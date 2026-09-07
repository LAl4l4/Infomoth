-- Keep the ingestion checkpoint and precomputed response across process restarts.
CREATE TABLE IF NOT EXISTS market_signal_state (
    id INT PRIMARY KEY,
    input_sha VARCHAR(64) NOT NULL,
    signature VARCHAR(64),
    payload MEDIUMTEXT
);
INSERT INTO market_signal_state (id, input_sha)
SELECT 1, '' WHERE NOT EXISTS (SELECT 1 FROM market_signal_state WHERE id = 1);
