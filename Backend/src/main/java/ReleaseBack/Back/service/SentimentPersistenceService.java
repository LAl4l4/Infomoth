package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.OptionalDouble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.entity.SentimentAverage;
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

    @Transactional
    public void persistTodayAverages() {
        persistPoliticsAverage();
        persistTechAverage();
    }

    private void persistPoliticsAverage() {
        persistAverage(
                POLITICS_FILE,
                "politics",
                "politics_average",
                sentimentMapper::insertPoliticsAverage);
    }

    private void persistTechAverage() {
        persistAverage(
                TECH_FILE,
                "tech",
                "tech_average",
                sentimentMapper::insertTechAverage);
    }

    private void persistAverage(
            String fileName,
            String category,
            String tableName,
            AverageWriter writer) {
        try {
            OptionalDouble average = sentimentFileReader.readAverage(sharedDir.resolve(fileName));
            if (average.isEmpty()) {
                log.warn("No valid sentiment scores in {}; skipping the {} sample", fileName, category);
                return;
            }

            double rawScore = average.getAsDouble();
            SentimentAverage previous = sentimentMapper.findLatestByTable(tableName);
            int sampleCount = nextSampleCount(previous);
            double rollingAverage = nextRollingAverage(previous, rawScore, sampleCount);

            writer.write(LocalDate.now(), rawScore, rollingAverage, sampleCount);
            log.info(
                    "Persisted {} sentiment sample: raw={}, rollingAverage={}, sampleCount={}",
                    category,
                    rawScore,
                    rollingAverage,
                    sampleCount);
        } catch (IOException e) {
            log.warn("Cannot read {} sentiment data; it will be retried on the next run", category, e);
        }
    }

    private int nextSampleCount(SentimentAverage previous) {
        if (previous == null || previous.getSampleCount() == null || previous.getSampleCount() < 1) {
            return 1;
        }
        return previous.getSampleCount() + 1;
    }

    private double nextRollingAverage(
            SentimentAverage previous,
            double rawScore,
            int sampleCount) {
        if (previous == null || previous.getRollingAverage() == null
                || !Double.isFinite(previous.getRollingAverage()) || sampleCount == 1) {
            return rawScore;
        }
        return ((previous.getRollingAverage() * (sampleCount - 1)) + rawScore) / sampleCount;
    }

    @FunctionalInterface
    private interface AverageWriter {
        void write(LocalDate date, double score, double rollingAverage, int sampleCount);
    }
}
