package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.OptionalDouble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.mapper.SentimentMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SentimentPersistenceService {
    private static final String POLITICS_FILE = "politics_news.json";
    private static final String TECH_FILE = "tech_news.json";

    private final SentimentMapper sentimentMapper;
    private final SentimentFileReader sentimentFileReader;
    private final Path sharedDir;

    @Autowired
    public SentimentPersistenceService(
            AppConfigProvider appConfigProvider,
            SentimentMapper sentimentMapper,
            SentimentFileReader sentimentFileReader) {
        this(appConfigProvider.getSharedDirectory(), sentimentMapper, sentimentFileReader);
    }

    SentimentPersistenceService(Path sharedDir, SentimentMapper sentimentMapper) {
        this(sharedDir, sentimentMapper, new SentimentFileReader(new ObjectMapper()));
    }

    SentimentPersistenceService(
            Path sharedDir,
            SentimentMapper sentimentMapper,
            SentimentFileReader sentimentFileReader) {
        this.sentimentMapper = sentimentMapper;
        this.sentimentFileReader = sentimentFileReader;
        this.sharedDir = sharedDir;
    }

    public void persistTodayAverages() {
        persistPoliticsAverage();
        persistTechAverage();
    }

    private void persistPoliticsAverage() {
        persistAverage(POLITICS_FILE, "politics", (date, score) -> {
            if (sentimentMapper.accumulatePoliticsAverage(date, score) == 0) {
                sentimentMapper.insertPoliticsAverage(date, score);
            }
        });
    }

    private void persistTechAverage() {
        persistAverage(TECH_FILE, "tech", (date, score) -> {
            if (sentimentMapper.accumulateTechAverage(date, score) == 0) {
                sentimentMapper.insertTechAverage(date, score);
            }
        });
    }

    private void persistAverage(String fileName, String category, AverageWriter writer) {
        try {
            OptionalDouble average = sentimentFileReader.readAverage(sharedDir.resolve(fileName));
            if (average.isEmpty()) {
                log.warn("No valid sentiment scores in {}; keeping the existing {} average", fileName, category);
                return;
            }

            writer.write(LocalDate.now(), average.getAsDouble());
            log.info("Persisted {} sentiment average: {}", category, average.getAsDouble());
        } catch (IOException e) {
            log.warn("Cannot read {} sentiment data; it will be retried on the next run", category, e);
        }
    }

    @FunctionalInterface
    private interface AverageWriter {
        void write(LocalDate date, double score);
    }
}
