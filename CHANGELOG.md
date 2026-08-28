# Changelog

- `08/29/2026`:
    - Added per-account Display settings for the main background, globe glow, globe point grid, and city marker colors, with live preview, explicit save status, and a persisted Restore Default action.
    - Added JWT-protected `GET /settings/display` and `PUT /settings/display` endpoints, `#RRGGBB` validation, portable MyBatis persistence, and additive MySQL/H2 migration of the four display columns in `user_settings`.
    - Updated the home shell to load the saved palette and apply it to the page background and cobe renderer; kept the original dark background, green glow, white point grid, and cyan markers as shared defaults.
    - Moved market-trend Pearson correlations out of request-time calculation into the persisted `market_correlation` cache, with application-start warm-up, nightly completed-day recomputation, concurrent-insert recovery, and full-history samples.
    - Changed market-trend stock lines to plot Yahoo Finance's captured daily percentage changes directly instead of normalizing prices against the first visible point, while retaining weekend gap bridging and labels.
    - Made Backend/Frontend build bases and the deployed MySQL image use the same configurable `IMAGE_PREFIX`, defaulting to `docker.1ms.run/library` to avoid unexpected Docker Hub fallbacks.
    - Added frontend interaction/build coverage plus H2, MySQL 8, mapper, service, controller, schema, and authenticated end-to-end regression coverage for display preferences and persisted correlation data.

- `08/15/2026`:
    - Split `/data/sentiment` into the current news `instant` score and the current day's persisted `dailyAverage`, with null-safe handling when either source is unavailable.
    - Changed daily sentiment persistence to a same-day, sample-count-weighted rolling average; added the additive `sampleCount` schema migration and regression coverage for date rollover and legacy tables.
    - Updated the sentiment UI to show both metrics, using the instantaneous score for the Overview snapshot and the daily average for the main reading.
    - Added left/right arrow-key and horizontal trackpad-swipe tab navigation, with swipe-only slide transitions and regression coverage for boundaries, pauses, and vertical scrolling.
    - Refreshed today's data: 61 technology stories, 100 politics stories, and 110 exchange-rate pairs were analysed; no AI skills matched, and Yahoo Finance rate limiting left the latest valid stock snapshot at `2026-08-07`.

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
