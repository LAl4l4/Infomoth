ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS default_base_currency VARCHAR(10) NOT NULL DEFAULT 'USD';

ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS default_quote_currency VARCHAR(10) NOT NULL DEFAULT 'CNY';
