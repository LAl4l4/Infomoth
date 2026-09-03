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
            RollingStatistics statistics = nextRollingStatistics(previous, rawScore, sampleCount);

            writer.write(
                    LocalDate.now(),
                    rawScore,
                    statistics.average(),
                    statistics.standardDeviation(),
                    sampleCount);
            log.info(
                    "Persisted {} sentiment sample: raw={}, rollingAverage={}, "
                            + "rollingStandardDeviation={}, sampleCount={}",
                    category,
                    rawScore,
                    statistics.average(),
                    statistics.standardDeviation(),
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

    private RollingStatistics nextRollingStatistics(
            SentimentAverage previous,
            double rawScore,
            int sampleCount) {
        if (previous == null || previous.getRollingAverage() == null
                || !Double.isFinite(previous.getRollingAverage())
                || previous.getRollingStandardDeviation() == null
                || !Double.isFinite(previous.getRollingStandardDeviation())
                || previous.getRollingStandardDeviation() < 0
                || sampleCount == 1) {
            return new RollingStatistics(rawScore, 0.0);
        }

        int previousCount = sampleCount - 1;
        double previousAverage = previous.getRollingAverage();
        double delta = rawScore - previousAverage;
        double nextAverage = previousAverage + delta / sampleCount;
        double previousM2 = Math.pow(previous.getRollingStandardDeviation(), 2) * previousCount;
        double nextM2 = previousM2 + delta * (rawScore - nextAverage);
        double nextVariance = Math.max(0.0, nextM2 / sampleCount);
        return new RollingStatistics(nextAverage, Math.sqrt(nextVariance));
    }

    @FunctionalInterface
    private interface AverageWriter {
        void write(
                LocalDate date,
                double score,
                double rollingAverage,
                double rollingStandardDeviation,
                int sampleCount);
    }

    private record RollingStatistics(double average, double standardDeviation) {
    }
}
