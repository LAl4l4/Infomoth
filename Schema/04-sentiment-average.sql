CREATE TABLE IF NOT EXISTS politics_average (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    sentimentScore DOUBLE NOT NULL,
    rollingAverage DOUBLE NOT NULL,
    rollingStandardDeviation DOUBLE NOT NULL DEFAULT 0,
    sampleCount INT NOT NULL DEFAULT 1,
    sampleVarianceCorrected BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tech_average (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    sentimentScore DOUBLE NOT NULL,
    rollingAverage DOUBLE NOT NULL,
    rollingStandardDeviation DOUBLE NOT NULL DEFAULT 0,
    sampleCount INT NOT NULL DEFAULT 1,
    sampleVarianceCorrected BOOLEAN NOT NULL DEFAULT TRUE
);
