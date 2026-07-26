CREATE TABLE IF NOT EXISTS user_settings (
    user_id INT NOT NULL PRIMARY KEY,
    default_page TINYINT NOT NULL DEFAULT 0
);
