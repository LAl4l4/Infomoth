package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.mapper.SentimentMapper;
import ReleaseBack.Back.DTO.SentimentScoreDTO;

@ExtendWith(MockitoExtension.class)
class DataServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SentimentMapper sentimentMapper;

    private DataService dataService;

    @BeforeEach
    void setUp() throws IOException {
        Path sharedDir = tempDir.resolve("Shared");
        Files.createDirectories(sharedDir);
        dataService = new DataService(sharedDir, sentimentMapper);
        ReflectionTestUtils.setField(dataService, "sentimentMapper", sentimentMapper);
    }

    @AfterEach
    void tearDown() {
        
    }

    @Test
    void getExchangeRateShouldReturnMatchingRate() throws IOException {
        writeSharedFile(
                "exchangeRates.json",
                """
                [
                  {"base":"USD","quote":"AUD","rate":1.5},
                  {"base":"EUR","quote":"USD","rate":1.1}
                ]
                """
        );

        double rate = dataService.getExchangeRate("USD", "AUD");

        assertEquals(1.5, rate);
    }

    @Test
    void getExchangeRateShouldThrowWhenPairMissing() throws IOException {
        writeSharedFile(
                "exchangeRates.json",
                """
                [
                  {"base":"USD","quote":"AUD","rate":1.5}
                ]
                """
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dataService.getExchangeRate("USD", "JPY")
        );

        assertTrue(ex.getMessage().contains("Exchange rate not found"));
    }

    @Test
    void getCurrenciesShouldReturnUniqueBaseAndQuoteCodes() throws IOException {
        writeSharedFile(
                "exchangeRates.json",
                """
                [
                  {"base":"USD","quote":"AUD","rate":1.5},
                  {"base":"USD","quote":"JPY","rate":155.2},
                  {"base":"EUR","quote":"USD","rate":1.1}
                ]
                """
        );

        Set<String> currencies = dataService.getCurrencies();

        assertEquals(Set.of("USD", "AUD", "JPY", "EUR"), currencies);
    }

    @Test
    void getPopularAISkillsShouldReadSharedJson() throws IOException {
        writeSharedFile(
                "ai_skills_today.json",
                """
                [
                  {"rank":1,"skill":"RAG","mentions":12,"date":"2026-05-25","source":"HN","sample_post_title":"RAG in prod"}
                ]
                """
        );

        var result = dataService.getPopularAISkills();

        assertEquals(1, result.size());
        assertEquals("RAG", result.getFirst().getSkill());
        assertEquals("RAG in prod", result.getFirst().getSamplePostTitle());
    }

    @Test
    void getUsStockIndicesShouldReadSharedJson() throws IOException {
        writeSharedFile(
                "us_stock_indices.json",
                """
                [
                  {"symbol":"^GSPC","name":"S&P 500","price":7483.23,"change":-16.13,"changePercent":-0.22,"date":"2026-07-02","source":"Yahoo Finance"},
                  {"symbol":"^DJI","name":"Dow Jones","price":52305.24,"change":-13.96,"changePercent":-0.03,"date":"2026-07-02","source":"Yahoo Finance"}
                ]
                """
        );

        var result = dataService.getUsStockIndices();

        assertEquals(2, result.size());
        assertEquals("^GSPC", result.get(0).getSymbol());
        assertEquals("S&P 500", result.get(0).getName());
        assertEquals(7483.23, result.get(0).getPrice());
        assertEquals(-0.22, result.get(0).getChangePercent());
    }

    @Test
    void getSentimentScoreShouldReturnInstantAndTodaysRollingAverage() throws IOException {
        writeSharedFile(
                "politics_news.json",
                """
                [{"financeInfluence":{"sentiment_score":0.8}}, {"financeInfluence":{"sentiment_score":0.2}}]
                """
        );
        writeSharedFile(
                "tech_news.json",
                """
                [{"financeInfluence":{"sentiment_score":-0.2}}, {"financeInfluence":{"sentiment_score":0.4}}]
                """
        );
        SentimentAverage politics = sentiment(1.0);
        SentimentAverage tech = sentiment(3.0);
        when(sentimentMapper.findByDate("politics_average", LocalDate.now())).thenReturn(politics);
        when(sentimentMapper.findByDate("tech_average", LocalDate.now())).thenReturn(tech);

        SentimentScoreDTO score = dataService.getSentimentScore();

        assertEquals(0.3, score.getInstant(), 0.000001);
        assertEquals(2.0, score.getDailyAverage());
    }

    @Test
    void getSentimentScoreShouldReturnAvailableValuesWhenOneCategoryIsMissing() throws IOException {
        writeSharedFile(
                "tech_news.json",
                """
                [{"financeInfluence":{"sentiment_score":0.5}}]
                """
        );
        SentimentAverage tech = sentiment(2.5);
        when(sentimentMapper.findByDate("politics_average", LocalDate.now())).thenReturn(null);
        when(sentimentMapper.findByDate("tech_average", LocalDate.now())).thenReturn(tech);

        SentimentScoreDTO score = dataService.getSentimentScore();

        assertEquals(0.5, score.getInstant());
        assertEquals(2.5, score.getDailyAverage());
    }

    @Test
    void getSentimentScoreShouldNotReuseYesterdayAsTodaysAverage() throws IOException {
        writeSharedFile(
                "politics_news.json",
                """
                [{"financeInfluence":{"sentiment_score":-0.4}}]
                """
        );
        when(sentimentMapper.findByDate("politics_average", LocalDate.now())).thenReturn(null);
        when(sentimentMapper.findByDate("tech_average", LocalDate.now())).thenReturn(null);

        SentimentScoreDTO score = dataService.getSentimentScore();

        assertEquals(-0.4, score.getInstant());
        assertEquals(null, score.getDailyAverage());
    }

    private void writeSharedFile(String fileName, String content) throws IOException {
        Path sharedFile = tempDir.resolve("Shared").resolve(fileName);
        Files.writeString(sharedFile, content);
    }

    private SentimentAverage sentiment(double score) {
        SentimentAverage average = new SentimentAverage();
        average.setId(1);
        average.setDate(LocalDate.now());
        average.setSentimentScore(score);
        return average;
    }
}
