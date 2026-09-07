# Changelog

- `09/08/2026`:
    - Added market-input crawlers for FRED, Cboe equity put/call, AAII sentiment, delayed public NAAIM exposure, and CFTC E-mini leveraged-fund positions, with atomic history retention, independent source status, and a `--market-inputs-only` command.
    - Persisted dated market observations and source attempts through additive MyBatis schemas, preserving first availability for unchanged values and preventing older captures from overwriting newer observations.
    - Added a replaceable linear mock model combining market indicators and existing FinBERT rolling z-scores, with explicit feature transforms, effective weights, contributions, freshness rules, and score suppression below 50% coverage.
    - Added the authenticated market-signal API and the 情绪预测 tab (ID 6), including selectable indicator history, source status, five-minute refresh, failed-refresh retention, and default-page settings support.
    - Moved prediction and response serialization into scheduled precomputation. The API reads one persisted JSON snapshot; rebuilds use at most 60 rows per raw indicator and run only when input, news, source, model, or UTC-date dependencies change. Failed rebuilds retain the previous response; initial missing snapshots return 503.
    - Added a transactional SHA-256 ingestion checkpoint that skips unchanged files across restarts. Changed files still scan the cumulative observations and insert or update individual rows; delta-only ingestion is not implemented.
    - Documented source access, data timestamps, model limitations, and snapshot behavior in `MARKET_SIGNAL.md`; verified 76 Crawler, 119 Backend, and 157 Frontend tests, plus frontend TypeScript checking and production build. Final HTTP-status adjustments passed the 12 affected backend tests.

- `09/07/2026`:
    - Hardened authentication: login and registration now accept JSON bodies, return a stable `success` field, hash new passwords with BCrypt, upgrade legacy plaintext passwords after successful login, and load the deployed JWT signing key from `JWT_SECRET`.
    - Added five-minute frontend data expiry, mounted-tab refresh polling, manual refresh controls, last-success timestamps, stale-value retention after failed refreshes, and retryable runtime configuration loading.
    - Made crawler snapshots resilient to partial source failures and interrupted writes with per-source saves, atomic JSON replacement, and date-aware exchange-rate merging that preserves missing pairs without redating them.
    - Reused one batched FinBERT analyser across both news sources, continued processing after one source fails, and expanded regression coverage for authentication, caching, refresh failures, partial crawls, and atomic writes.
    - Updated frontend terminology and documentation to describe rolling sentiment z-scores as 标准化 while retaining the existing API field names for compatibility.

- `09/05/2026`:
    - Persisted the latest politics and technology sentiment JSON SHA-256 fingerprints in a single-row `sentiment_file_state` checkpoint, so unchanged pipeline output is skipped across Backend restarts while storage stays O(1).
    - Added `scripts/fix_sentiment_duplicates.sql` to remove historical consecutive duplicate sentiment samples and rebuild their cumulative means, Bessel-corrected sample standard deviations, and counts.
    - Documented the repair procedure in `FIXES.md`.

- `09/04/2026`:
    - Added cumulative Bessel-corrected sample standard deviation to every persisted politics and technology sentiment sample alongside its raw FinBERT score, cumulative mean, and sample count; startup migration backfills the complete statistic sequence for existing rows.
    - Changed normalized sentiment to the rolling z-score `(raw - mean) / standard deviation`, changed `/data/sentiment` to return today's mean across normalized samples as `dailyAverage`, and adapted the sentiment and market-trend displays to the new scale.
    - Kept the market chart at seven calendar days while rebuilding each displayed `corr` from every completed overlapping persisted date, and clarified that full-history scope in the UI.
    - Routed the Pipeline Python base image through the shared configurable `IMAGE_PREFIX`, defaulting to `docker.1ms.run/library`, so pipeline builds do not depend on direct Docker Hub access.

- `09/03/2026`:
    - Reordered the primary navigation to 概览 / 市场情绪 / 汇率 / 美股 / 市场走势 / 更多, moved AI skill trends into the secondary More view, and reduced the ranking to five entries until expanded.
    - Removed AI skills from the Overview snapshot so its data loads only when More is opened. Breaking: `defaultPage` values were renumbered to match the new tab order without migrating previously saved preferences.

- `09/01/2026`:
    - Changed sentiment persistence to append one immutable politics and technology sample per run, storing the raw FinBERT score, the cumulative rolling average including that sample, and the cumulative sample count.
    - Normalized sentiment reads as the current raw score minus the rolling average that existed before it; `/data/sentiment` now returns `normalizedScore` and `rollingAverage`, and market-trend sentiment/correlation uses normalized daily observations.
    - Added a repeatable MySQL 8/H2 migration that initializes cumulative values for legacy rows, removes the old unique-date restriction, and renumbers legacy sample counts to match the append-only record sequence.
    - Updated the Overview and Market Sentiment UI to present normalized sentiment and the cross-date cumulative average, with backend mapper/service/schema and frontend regression coverage.
    - Switched US index acquisition from completed daily candles to 5-minute intraday candles, collapsing each trading day to its latest price so the current session's change remains relative to the previous trading-day close.
    - Added a lightweight five-minute stock-only Pipeline refresh and a separate five-minute Backend persistence schedule, leaving the full Crawler → Analyser pipeline on its hourly cadence.
    - Made the Market Trend tab refresh automatically every five minutes while open, so an in-progress US trading day can update without waiting for a page reload or market close.

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
