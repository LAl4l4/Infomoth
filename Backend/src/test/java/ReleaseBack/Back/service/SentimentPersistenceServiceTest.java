package ReleaseBack.Back.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.mapper.SentimentMapper;

@ExtendWith(MockitoExtension.class)
class SentimentPersistenceServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SentimentMapper sentimentMapper;

    private SentimentPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new SentimentPersistenceService(tempDir, sentimentMapper);
    }

    @Test
    void insertsFirstRawSamplesAsTheirOwnRollingAverages() throws IOException {
        write("politics_news.json", """
                [{"financeInfluence":{"sentiment_score":0.8}}, {"financeInfluence":{"sentiment_score":-0.2}}]
                """);
        write("tech_news.json", """
                [{"financeInfluence":{"sentiment_score":0.4}}, {"financeInfluence":{"sentiment_score":0.2}}]
                """);
        when(sentimentMapper.findLatestByTable("politics_average")).thenReturn(null);
        when(sentimentMapper.findLatestByTable("tech_average")).thenReturn(null);

        service.persistTodayAverages();

        verify(sentimentMapper).insertPoliticsAverage(
                any(),
                doubleThat(score -> Math.abs(score - 0.3) < 0.000001),
                doubleThat(score -> Math.abs(score - 0.3) < 0.000001),
                eq(1));
        verify(sentimentMapper).insertTechAverage(
                any(),
                doubleThat(score -> Math.abs(score - 0.3) < 0.000001),
                doubleThat(score -> Math.abs(score - 0.3) < 0.000001),
                eq(1));
    }

    @Test
    void appendsFromThePreviousCumulativeAverageAndIgnoresMissingScores() throws IOException {
        write("politics_news.json", """
                [{"financeInfluence":{"sentiment_score":0.5}}, {"title":"not analysed"}, {"financeInfluence":{"sentiment_score":"bad"}}]
        """);
        write("tech_news.json", "[]");
        when(sentimentMapper.findLatestByTable("politics_average"))
                .thenReturn(sentiment(0.2, 4));

        service.persistTodayAverages();

        verify(sentimentMapper).insertPoliticsAverage(
                any(),
                eq(0.5),
                doubleThat(score -> Math.abs(score - 0.26) < 0.000001),
                eq(5));
        verify(sentimentMapper, never()).findLatestByTable("tech_average");
        verify(sentimentMapper, never()).insertTechAverage(
                any(), anyDouble(), anyDouble(), anyInt());
    }

    private void write(String fileName, String content) throws IOException {
        Files.writeString(tempDir.resolve(fileName), content);
    }

    private SentimentAverage sentiment(double rollingAverage, int sampleCount) {
        SentimentAverage sample = new SentimentAverage();
        sample.setId(1);
        sample.setDate(LocalDate.now().minusDays(1));
        sample.setSentimentScore(rollingAverage);
        sample.setRollingAverage(rollingAverage);
        sample.setSampleCount(sampleCount);
        return sample;
    }
}
