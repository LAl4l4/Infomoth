# Changelog

- `08/10/2026`:
    - Reshaped `us_stock_indices` to one row per trading date, with persisted price and captured percentage-change columns for all four tracked indices; added a startup migration that preserves both values from complete legacy weekday rows and discards legacy weekend rows.
    - Changed market-trend `corr` to use the crawler-captured percentage change directly, so each stored trading day contributes one sample; price lines bridge weekend gaps and label those calendar dates as `Weekend` without creating weekend market observations.
    - Changed the US stock crawler to stamp snapshots from Yahoo's actual close-series trading date and retain the latest trading-day JSON when a scrape is empty.
    - Reduced exchange-rate request concurrency, switched to Frankfurter's direct v1 request format, and retained the more complete prior JSON payload after partial crawler failures.
    - Unified relative `shared.directory` resolution against `Config/app-config.json` across Crawler, Analyser, and Backend, so local runs consistently use `InfoMoth/Shared` while deployed absolute paths remain unchanged.

- `08/09/2026`:
    - Changed market-trend `corr` to compare each day's sentiment with the stock percentage change from its preceding available price, including across non-consecutive trading dates without filling missing days.
    - Reduced the market-sentiment chart display scale from `×100` to `×10` while keeping correlation calculations on unscaled sentiment values.
    - Added dedicated top and bottom safe spacing to the home tab content shell so long panels clear the fixed tab bar and retain bottom scrolling room on desktop and mobile.

- `08/08/2026`:
    - Migrated authentication to a 48-hour `HttpOnly` `authToken` Cookie with session restore, logout expiry, credentialed Axios requests, and no JWT storage in Redux or localStorage.
    - Preserved same-day US stock data when a scrape returns an empty value, while clearing stale data after the date changes.
    - Added daily US stock persistence, seven-day market trend aggregation, per-index sentiment correlation (`corr`), and a new 市场走势 tab with inline SVG charts.
    - Extended the existing `user_settings` table to persist the default home tab and both exchange-rate selector sides (`default_base_currency` / `default_quote_currency`).
    - Combined the default base-currency and quote-currency selectors into one responsive Settings card, keeping the two choices side by side on larger screens.
    - Updated the Overview exchange quick lookup to use the same saved currency pair as the Exchange Rate tab, with USD/CNY as the fallback.
    - Clarified market-trend correlation behavior: `corr` uses every available same-date sentiment/price pair in the seven-day window, including non-consecutive dates, without filling missing days; added regression coverage for six available days.
    - Updated InfoMoth frontend branding assets and runtime metadata.
    - Fixed MySQL 8 startup failure by making the existing `user_settings` currency-column migration metadata-driven and repeatable.
    - Added disposable Testcontainers MySQL 8 Schema integration coverage for repeatable fresh-schema initialization and legacy `user_settings` migration, without touching the local database; made test connection and query-count values explicitly non-null.

- `08/01/2026`:
    - Added a container-owned hourly Pipeline schedule that runs Crawler, Analyser, and rsync sequentially.
    - Removed Analyser's direct MySQL persistence; database writes are reserved for Backend.
    - Added staged, auto-starting `deployapp` and `deploypipe` Make targets for build, package, upload, and remote startup.
    - Removed FinBERT files from the Pipeline image and build context; `make submit-models` now uploads them independently to the Pipeline VM for read-only volume mounting.

- `07/26/2026`: 
    - Moved frontend, backend, crawler, analyser, and pipeline cross-service settings into runtime config; added deployable database schemas with automatic initialization.
