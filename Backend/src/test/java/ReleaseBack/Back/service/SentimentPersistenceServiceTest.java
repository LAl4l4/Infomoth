package ReleaseBack.Back.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void insertsAveragesWhenTodayHasNoRows() throws IOException {
        write("politics_news.json", """
                [{"financeInfluence":{"sentiment_score":0.8}}, {"financeInfluence":{"sentiment_score":-0.2}}]
                """);
        write("tech_news.json", """
                [{"financeInfluence":{"sentiment_score":0.4}}, {"financeInfluence":{"sentiment_score":0.2}}]
                """);
        when(sentimentMapper.accumulatePoliticsAverage(any(), any(Double.class))).thenReturn(0);
        when(sentimentMapper.accumulateTechAverage(any(), any(Double.class))).thenReturn(0);

        service.persistTodayAverages();

        verify(sentimentMapper).insertPoliticsAverage(any(), doubleThat(score -> Math.abs(score - 0.3) < 0.000001));
        verify(sentimentMapper).insertTechAverage(any(), doubleThat(score -> Math.abs(score - 0.3) < 0.000001));
    }

    @Test
    void updatesExistingRowsAndIgnoresItemsWithoutScores() throws IOException {
        write("politics_news.json", """
                [{"financeInfluence":{"sentiment_score":0.5}}, {"title":"not analysed"}, {"financeInfluence":{"sentiment_score":"bad"}}]
                """);
        write("tech_news.json", "[]");
        when(sentimentMapper.accumulatePoliticsAverage(any(), any(Double.class))).thenReturn(1);

        service.persistTodayAverages();

        verify(sentimentMapper).accumulatePoliticsAverage(any(), eq(0.5));
        verify(sentimentMapper, never()).insertPoliticsAverage(any(), any(Double.class));
        verify(sentimentMapper, never()).accumulateTechAverage(any(), any(Double.class));
        verify(sentimentMapper, never()).insertTechAverage(any(), any(Double.class));
    }

    private void write(String fileName, String content) throws IOException {
        Files.writeString(tempDir.resolve(fileName), content);
    }
}
