CREATE TABLE IF NOT EXISTS user_settings (
    user_id INT NOT NULL PRIMARY KEY,
    default_page TINYINT NOT NULL DEFAULT 0,
    default_base_currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    default_quote_currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    background_color VARCHAR(7) NOT NULL DEFAULT '#0C101C',
    globe_glow_color VARCHAR(7) NOT NULL DEFAULT '#00FFC6',
    globe_point_color VARCHAR(7) NOT NULL DEFAULT '#FFFFFF',
    globe_marker_color VARCHAR(7) NOT NULL DEFAULT '#00E5FF'
);
