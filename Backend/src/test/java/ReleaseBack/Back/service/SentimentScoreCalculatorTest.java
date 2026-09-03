package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import ReleaseBack.Back.entity.SentimentAverage;

class SentimentScoreCalculatorTest {

    @Test
    void normalizesTheRawScoreWithThePersistedRollingStandardDeviation() {
        SentimentAverage sample = sentiment(0.8, 0.5, 0.1, 4);

        assertEquals(3.0, SentimentScoreCalculator.normalizedScore(sample), 0.000001);
    }

    @Test
    void returnsZeroUntilTheRollingStandardDeviationIsNonZero() {
        assertEquals(0.0, SentimentScoreCalculator.normalizedScore(
                sentiment(0.7, 0.7, 0.0, 1)));
        assertEquals(0.0, SentimentScoreCalculator.normalizedScore(
                sentiment(0.8, 0.5, 0.0, 2)));
    }

    @Test
    void rejectsSamplesWithoutCompleteFiniteRollingStatistics() {
        SentimentAverage sample = sentiment(0.8, 0.5, 0.1, 4);
        sample.setRollingStandardDeviation(null);

        assertNull(SentimentScoreCalculator.normalizedScore(sample));
    }

    private SentimentAverage sentiment(
            double rawScore,
            double rollingAverage,
            double rollingStandardDeviation,
            int sampleCount) {
        SentimentAverage sample = new SentimentAverage();
        sample.setSentimentScore(rawScore);
        sample.setRollingAverage(rollingAverage);
        sample.setRollingStandardDeviation(rollingStandardDeviation);
        sample.setSampleCount(sampleCount);
        return sample;
    }
}
