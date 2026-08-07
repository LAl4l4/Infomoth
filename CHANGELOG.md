# Changelog

- `08/08/2026`:
    - Migrated authentication to a 48-hour `HttpOnly` `authToken` Cookie with session restore, logout expiry, credentialed Axios requests, and no JWT storage in Redux or localStorage.
    - Preserved same-day US stock data when a scrape returns an empty value, while clearing stale data after the date changes.
    - Added daily US stock persistence, seven-day market trend aggregation, per-index sentiment correlation (`corr`), and a new 市场走势 tab with inline SVG charts.
    - Extended the existing `user_settings` table to persist the default home tab and both exchange-rate selector sides (`default_base_currency` / `default_quote_currency`).
    - Updated InfoMoth frontend branding assets and runtime metadata.

- `08/01/2026`:
    - Added a container-owned hourly Pipeline schedule that runs Crawler, Analyser, and rsync sequentially.
    - Removed Analyser's direct MySQL persistence; database writes are reserved for Backend.
    - Added staged, auto-starting `deployapp` and `deploypipe` Make targets for build, package, upload, and remote startup.
    - Removed FinBERT files from the Pipeline image and build context; `make submit-models` now uploads them independently to the Pipeline VM for read-only volume mounting.

- `07/26/2026`: 
    - Moved frontend, backend, crawler, analyser, and pipeline cross-service settings into runtime config; added deployable database schemas with automatic initialization.
