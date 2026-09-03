package ReleaseBack.Back.service;

import ReleaseBack.Back.entity.SentimentAverage;

final class SentimentScoreCalculator {
    private SentimentScoreCalculator() {
    }

    static Double normalizedScore(SentimentAverage sample) {
        if (sample == null || sample.getSentimentScore() == null
                || !Double.isFinite(sample.getSentimentScore())) {
            return null;
        }
        if (sample.getRollingAverage() == null || !Double.isFinite(sample.getRollingAverage())) {
            return null;
        }
        if (sample.getRollingStandardDeviation() == null
                || !Double.isFinite(sample.getRollingStandardDeviation())
                || sample.getRollingStandardDeviation() < 0) {
            return null;
        }
        if (sample.getRollingStandardDeviation() == 0) {
            return 0.0;
        }

        return (sample.getSentimentScore() - sample.getRollingAverage())
                / sample.getRollingStandardDeviation();
    }
}
