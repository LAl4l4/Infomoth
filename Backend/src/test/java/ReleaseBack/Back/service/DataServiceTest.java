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
        dataService = new DataService(sharedDir);
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
    void getSentimentScoreShouldReturnAverageWhenBothTablesHaveValues() {
        SentimentAverage politics = sentiment(1.0);
        SentimentAverage tech = sentiment(3.0);
        when(sentimentMapper.findLatestByTable("politics_average")).thenReturn(politics);
        when(sentimentMapper.findLatestByTable("tech_average")).thenReturn(tech);

        double score = dataService.getSentimentScore();

        assertEquals(2.0, score);
    }

    @Test
    void getSentimentScoreShouldReturnExistingValueWhenOneTableMissing() {
        SentimentAverage tech = sentiment(2.5);
        when(sentimentMapper.findLatestByTable("politics_average")).thenReturn(null);
        when(sentimentMapper.findLatestByTable("tech_average")).thenReturn(tech);

        double score = dataService.getSentimentScore();

        assertEquals(2.5, score);
    }

    @Test
    void getSentimentScoreShouldThrowWhenBothTablesMissing() {
        when(sentimentMapper.findLatestByTable("politics_average")).thenReturn(null);
        when(sentimentMapper.findLatestByTable("tech_average")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dataService.getSentimentScore());

        assertTrue(ex.getMessage().contains("No sentiment data available"));
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
