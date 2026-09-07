# Market signal inputs and mock model

## Run and inspect

From the repository root, run `rtk python3 Crawler/main.py --market-inputs-only`.
The normal hourly crawler cycle also includes these inputs; `--stocks-only`
continues to update only the existing intraday stock file.

The crawler atomically merges `Shared/market_inputs.json`. Backend automatically
creates the additive tables in `Schema/09-market-inputs.sql` and
`Schema/10-market-signal-state.sql`, imports the file
five seconds after startup, and checks it every five minutes. Configure
`MARKET_INPUTS_ENABLED`, `MARKET_INPUTS_INITIAL_DELAY_MS`, and
`MARKET_INPUTS_FIXED_DELAY_MS` to control this independent scheduler.

Start Backend and Frontend normally, sign in, and select **情绪预测**. The tab is
appended as ID 6, preserving existing saved tab IDs. It can also be saved as the
default page in settings. The authenticated endpoint is `GET /data/market-signal`.
The tab refreshes every five minutes and provides manual refresh; API refresh
does not trigger external crawling or model computation. Failed refreshes preserve the previous view.

## Acquisition contract

| Source | Raw indicator codes | Data/access behavior |
| --- | --- | --- |
| [FRED CSV](https://fred.stlouisfed.org/graph/fredgraph.csv) | `sp500`, `vix`, `vix3m`, `treasury10y`, `hy_spread` | SP500, VIXCLS, VXVCLS, DGS10, BAMLH0A0HYM2; request 400 calendar days, independently per series; skip missing values |
| [Cboe daily statistics](https://www.cboe.com/markets/us/options/market-statistics/daily) | `put_call` | Equity-only put/call ratio; use the page's selected trading date |
| [AAII public survey](https://www.aaii.com/sentimentsurvey) | `aaii_bull`, `aaii_bear` | Current publicly displayed percentages and survey date; no member-only history access; site may reject automated requests |
| [NAAIM public chart](https://index.naaim.org/embeddable/chart) | `naaim` | Dated public series, currently delayed three months; never label it current or substitute the undated headline |
| [CFTC historical files](https://www.cftc.gov/MarketReports/CommitmentsofTraders/HistoricalCompressed/index.htm) | `cot_long`, `cot_short`, `cot_oi` | Annual TFF futures-only ZIP, contract 13874A (E-mini S&P 500), leveraged-money long/short and total open interest; prior year included near January |

An observation contains `indicator`, `date`, numeric `value`, `source`, and an
ISO-8601 UTC `fetchedAt`. The envelope also has per-source `status`, `message`,
and `attemptedAt`. Source statuses are `ok`, `partial`, `delayed`, or `error`.
Failures do not erase observations. FRED partial success keeps the successful
series. No reverse engineering of subscriptions, fabricated data, or additional
API credentials is used.

News inputs reuse the existing persisted technology/politics rolling z-scores;
this change does not run FinBERT in Backend or duplicate its storage. ETF fund
flows, paid positioning feeds, and full AAII history are not part of this v1.

## Storage and time semantics

`market_observation` has one row per `(indicator, observed_on)`, with the raw
value, source, latest successful `fetched_at`, and `available_at` as UTC epoch
milliseconds. `market_input_source` separately persists the last attempt status.
Ingestion uses a transaction and parameterized MyBatis XML queries. Repeated
imports do not add duplicates; older captures cannot overwrite newer values.
Unchanged values retain first availability. Corrections update the value and
reset availability to the time Backend receives the correction.

Observation date, retrieval time, and availability are different concepts.
Historical backfills are first available to this system when imported, not on
their historical dates. CFTC report dates are position dates, not release dates.
These tables keep the latest revision of each observation, **not all historical
vintages**, and are not a point-in-time backtesting dataset. Legacy news rows
only have dates; their missing timestamps remain null. No historical prediction
performance is claimed. The latest mock response is persisted as a replaceable
snapshot, not an append-only forecast history.

## Precomputation and bounded memory

The scheduled path is `ingest and commit -> rebuild snapshot if needed -> commit snapshot`. The HTTP path is
`GET /data/market-signal -> SELECT payload WHERE id = 1 -> return JSON`.
`market_signal_state` contains one row with the imported file's SHA-256, the
prediction dependency signature, and a serialized response (`MEDIUMTEXT`). No
large JVM cache is retained. Snapshot reads do not query raw observations,
read files, calculate features, or deserialize and reserialize the response.

The importer streams the file through SHA-256 using a bounded buffer. If the
persisted hash matches, it skips JSON parsing and all observation queries, even
after a restart. Changed files still use the existing validated per-row merge;
their checkpoint commits in the same transaction as the observations. An open
file descriptor keeps hashing and parsing on the same atomically replaced file.

Every five minutes the producer checks the input checkpoint, two latest news
samples, source statuses, model version/catalog, and UTC date. Only a change
rebuilds the snapshot. Each of the 12 raw indicators uses its primary-key index
to read at most 60 rows within the existing 400-day window: at most 720 raw rows
plus two news samples. Derived features, standardized values, effective weights,
contributions, total score, source status, and chart history are serialized once
and saved together. Existing FinBERT rolling statistics are reused; no new model
or normalization formula is introduced by this optimization.

News changes and calendar expiry become visible on the next scheduled check,
normally within five minutes. `generatedAt` is the successful computation time,
not the HTTP request time. A failed rebuild retains the previous snapshot and
signature for retry. Before the first successful rebuild the API returns 503;
it never falls back to expensive request-time computation. With the scheduler
disabled, existing snapshots remain readable but do not refresh. When changing
model logic or parameters, bump `MarketPredictionModel.cacheVersion()`.

## Linear mock v1

`MarketPredictionModel` is the replacement interface. `LinearMarketPredictionModel`
is its current Spring bean. Replace that bean with a trained model implementation
while retaining the `MarketSignalDTO` response contract. The input catalog and
feature transforms are in `MarketInputCatalog`.

Each weighted feature uses `clip((x - center) / scale, -1, 1)`:

| Feature | Weight | Center | Scale |
| --- | ---: | ---: | ---: |
| Technology news z-score | 10% | 0 | 3 |
| Politics news z-score | 10% | 0 | 3 |
| VIX | 15% | 20 | -15 |
| Equity put/call | 10% | 0.7 | -0.4 |
| AAII bullish minus bearish, percentage points | 10% | 0 | 40 |
| NAAIM exposure | 10% | 50 | 50 |
| CFTC `(long - short) / open interest * 100` | 10% | 0 | 30 |
| S&P 500 five-session price return, percent | 10% | 0 | 5 |
| High-yield spread, percentage points | 10% | 4 | -3 |
| Ten-year Treasury yield, percent | 5% | 4 | -2 |

These are arbitrary, reviewable demonstration parameters, not estimated effects.
Daily market observations expire after five calendar days, weekly surveys and
CFTC after fourteen, and news after three. Derived survey/CFTC inputs require
matching report dates. Momentum needs six price observations within twelve
calendar days. VIX3M, price levels and underlying survey/position components
remain visible as context with zero additional weight, avoiding double counting.

`coverage` is the sum of available fixed weights, not statistical confidence.
Missing, stale, nonfinite or future-available inputs are excluded. Available
weights are divided by coverage, and `score = 100 * sum(effectiveWeight * feature)`.
Coverage below 50% returns a null score and `insufficient_data`. The nominal
five-session target is uncalibrated: the -100..100 score is neither a probability
nor an expected return. Each response includes transforms, weights, individual
contributions, source dates and the most recent 60 observations per raw series.

## Verification

- `rtk python3 -m pytest Crawler/tests -q` (from repository root)
- `rtk ./mvnw test` (from Backend)
- `rtk pnpm test --runInBand`, `rtk pnpm exec tsc --noEmit`, `rtk pnpm build` (from Frontend)

Tests cover source parsing, delayed dates, contract selection, partial failures,
atomic history retention, real-schema MyBatis mapping and idempotent corrections,
mock math/coverage/date alignment, endpoint authentication, and frontend refresh
failure, retry and timer cleanup. External live-source availability is verified
separately; unit tests do not depend on the network.

Verified on 2026-09-07: 76 Crawler tests, 119 Backend tests, 157 Frontend tests,
TypeScript checking and the production build passed. The live collection produced
1,541 observations across 12 raw indicators, verified in local MySQL. AAII access
was intermittent; its successful September 2 survey was retained after subsequent
failures. NAAIM's latest public chart observation was May 27 and remains stale.

Snapshot tests additionally verify bounded-history equivalence, persistent skip
behavior across service recreation, news/input/model invalidation, date expiry,
failed rebuild retention/retry, and a one-query request path. These establish
bounded work; deployment swap usage and latency have not been benchmarked.
