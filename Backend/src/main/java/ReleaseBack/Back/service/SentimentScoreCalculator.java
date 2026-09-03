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
        if (sample.getSampleCount() == null || sample.getSampleCount() <= 1) {
            return 0.0;
        }
        if (sample.getRollingAverage() == null || !Double.isFinite(sample.getRollingAverage())) {
            return null;
        }

        double previousAverage = (
                (sample.getRollingAverage() * sample.getSampleCount())
                        - sample.getSentimentScore())
                / (sample.getSampleCount() - 1);
        return sample.getSentimentScore() - previousAverage;
    }

    static Double rollingAverage(SentimentAverage sample) {
        if (sample == null || sample.getRollingAverage() == null
                || !Double.isFinite(sample.getRollingAverage())) {
            return null;
        }
        return sample.getRollingAverage();
    }
}
