CREATE TABLE IF NOT EXISTS market_correlation (
    symbol VARCHAR(16) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    corr DOUBLE,
    sample_size INT NOT NULL
);
