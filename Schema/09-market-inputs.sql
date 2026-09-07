-- Raw observations retain source dates; timestamps are UTC epoch milliseconds.
CREATE TABLE IF NOT EXISTS market_observation (
    indicator VARCHAR(40) NOT NULL,
    observed_on DATE NOT NULL,
    observed_value DOUBLE NOT NULL,
    source VARCHAR(32) NOT NULL,
    fetched_at BIGINT NOT NULL,
    available_at BIGINT NOT NULL,
    PRIMARY KEY (indicator, observed_on)
);

CREATE TABLE IF NOT EXISTS market_input_source (
    source VARCHAR(32) PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    message VARCHAR(255) NOT NULL,
    attempted_at BIGINT NOT NULL
);
