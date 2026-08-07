CREATE TABLE IF NOT EXISTS user_settings (
    user_id INT NOT NULL PRIMARY KEY,
    default_page TINYINT NOT NULL DEFAULT 0,
    default_base_currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    default_quote_currency VARCHAR(10) NOT NULL DEFAULT 'CNY'
);
