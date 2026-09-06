package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.OptionalDouble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.entity.SentimentFileState;
import ReleaseBack.Back.mapper.SentimentFileStateMapper;
import ReleaseBack.Back.mapper.SentimentMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SentimentPersistenceService {
    private static final String POLITICS_FILE = "politics_news.json";
    private static final String TECH_FILE = "tech_news.json";

    private final SentimentMapper sentimentMapper;
    private final SentimentFileStateMapper sentimentFileStateMapper;
    private final SentimentFileReader sentimentFileReader;
    private final Path sharedDir;

    @Autowired
    public SentimentPersistenceService(
            AppConfigProvider appConfigProvider,
            SentimentMapper sentimentMapper,
            SentimentFileReader sentimentFileReader,
            SentimentFileStateMapper sentimentFileStateMapper) {
        this(
                appConfigProvider.getSharedDirectory(),
                sentimentMapper,
                sentimentFileReader,
                sentimentFileStateMapper);
    }

    SentimentPersistenceService(
            Path sharedDir,
            SentimentMapper sentimentMapper,
            SentimentFileReader sentimentFileReader,
            SentimentFileStateMapper sentimentFileStateMapper) {
        this.sentimentMapper = sentimentMapper;
        this.sentimentFileStateMapper = sentimentFileStateMapper;
        this.sentimentFileReader = sentimentFileReader;
        this.sharedDir = sharedDir;
    }

    @Transactional
    public void persistTodayAverages() {
        SentimentFileState state = sentimentFileStateMapper.find();
        if (state == null) {
            sentimentFileStateMapper.insert();
            state = new SentimentFileState();
            state.setId(1);
        }
        persistAverage(
                POLITICS_FILE,
                "politics",
                "politics_average",
                state.getPoliticsSha256(),
                sentimentMapper::insertPoliticsAverage,
                sentimentFileStateMapper::updatePoliticsSha256,
                state::setPoliticsSha256);
        persistAverage(
                TECH_FILE,
                "tech",
                "tech_average",
                state.getTechSha256(),
                sentimentMapper::insertTechAverage,
                sentimentFileStateMapper::updateTechSha256,
                state::setTechSha256);
    }

    private void persistAverage(
            String fileName,
            String category,
            String tableName,
            String previousFingerprint,
            AverageWriter writer,
            FingerprintWriter fingerprintWriter,
            FingerprintStateUpdater stateUpdater) {
        try {
            SentimentFileReader.Snapshot snapshot =
                    sentimentFileReader.readSnapshot(sharedDir.resolve(fileName));
            if (snapshot.fingerprint().equals(previousFingerprint)) {
                log.debug("Skipping unchanged {} sentiment file", category);
                return;
            }

            OptionalDouble average = snapshot.average();
            if (average.isEmpty()) {
                log.warn("No valid sentiment scores in {}; skipping the {} sample", fileName, category);
                return;
            }

            double rawScore = average.getAsDouble();
            SentimentAverage previous = sentimentMapper.findLatestByTable(tableName);
            int sampleCount = nextSampleCount(previous);
            RollingStatistics statistics = nextRollingStatistics(previous, rawScore, sampleCount);

            if (writer.write(
                    LocalDate.now(),
                    rawScore,
                    statistics.average(),
                    statistics.standardDeviation(),
                    sampleCount) != 1) {
                throw new IllegalStateException("Sentiment sample insert did not affect one row");
            }
            if (fingerprintWriter.write(snapshot.fingerprint()) != 1) {
                throw new IllegalStateException("Sentiment file state row is missing");
            }
            stateUpdater.update(snapshot.fingerprint());
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
        double previousM2 = previousCount <= 1
                ? 0.0
                : Math.pow(previous.getRollingStandardDeviation(), 2) * (previousCount - 1);
        double nextM2 = previousM2 + delta * (rawScore - nextAverage);
        double nextVariance = Math.max(0.0, nextM2 / (sampleCount - 1));
        return new RollingStatistics(nextAverage, Math.sqrt(nextVariance));
    }

    @FunctionalInterface
    private interface AverageWriter {
        int write(
                LocalDate date,
                double score,
                double rollingAverage,
                double rollingStandardDeviation,
                int sampleCount);
    }

    @FunctionalInterface
    private interface FingerprintWriter {
        int write(String sha256);
    }

    @FunctionalInterface
    private interface FingerprintStateUpdater {
        void update(String sha256);
    }

    private record RollingStatistics(double average, double standardDeviation) {
    }

}
