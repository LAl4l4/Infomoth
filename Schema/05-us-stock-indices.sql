CREATE TABLE IF NOT EXISTS us_stock_indices (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    price DOUBLE NOT NULL,
    change_value DOUBLE NOT NULL,
    change_percent DOUBLE NOT NULL,
    date DATE NOT NULL,
    source VARCHAR(128),
    UNIQUE (symbol, date)
);
