CREATE TABLE IF NOT EXISTS sentiment_file_state (
    ID TINYINT PRIMARY KEY,
    politicsSha256 CHAR(64),
    techSha256 CHAR(64),
    CONSTRAINT sentiment_file_state_single_row CHECK (ID = 1)
);
