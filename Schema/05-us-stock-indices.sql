CREATE TABLE IF NOT EXISTS us_stock_indices (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    sp500_price DOUBLE NOT NULL,
    sp500_change_percent DOUBLE NOT NULL,
    dow_jones_price DOUBLE NOT NULL,
    dow_jones_change_percent DOUBLE NOT NULL,
    nasdaq_price DOUBLE NOT NULL,
    nasdaq_change_percent DOUBLE NOT NULL,
    russell_2000_price DOUBLE NOT NULL,
    russell_2000_change_percent DOUBLE NOT NULL,
    UNIQUE (date)
);
