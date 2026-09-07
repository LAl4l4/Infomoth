# Crawler PROJECT_SPEC

## Market prediction inputs
- `infomoth/market_input_scraper.py` acquires FRED SP500/VIX/VIX3M/Treasury/high-yield spread, Cboe equity put/call, AAII public survey, delayed public NAAIM history and CFTC E-mini leveraged-fund positions.
- The full cycle includes atomic, history-preserving `market_inputs.json` updates. `python main.py --market-inputs-only` runs these adapters independently; stock-only mode is unchanged.
- Preserve observation dates and UTC retrieval timestamps. Persist source attempt status even when access fails; do not synthesize missing values or treat delayed surveys as current.
- Contract, source restrictions and model integration: [MARKET_SIGNAL.md](../MARKET_SIGNAL.md).

## 1. Project Positioning
- **Type**: Python Scraping Tasks (Batch Processing)
- **Responsibilities**:
  - Scrape Tech News and International Politics News.
  - Scrape exchange rates for major currencies.
  - Scrape major US stock indices.
  - Output JSON files for Backend consumption.

## 2. Tech Stack & Frameworks
- **Language/Runtime**: Python `3.13.3`
- **Core Dependencies** (from `requirements.txt`):
  - `requests==2.31.0`: HTTP requests
  - `beautifulsoup4==4.12.2`: HTML parsing
  - `feedparser==6.0.11`: RSS/Atom parsing
  - `selenium==4.18.1`: Fallback for JavaScript-heavy dynamic pages
  - `yfinance==0.2.54`: US stock index market data
  - `python==3.13.3`
- **Standard Library Usage**:
  - `ThreadPoolExecutor`: improve performance via multi-thread execution

## 3. Module Structure
- `main.py`: Entry point for tasks, orchestrates execution and writes JSON outputs.
- `infomoth/base_scraper.py`: Abstract scraping workflow and shared capabilities:
  - User-Agent rotation.
  - Timeout and 403 error handling.
  - De-duplication logic (based on `title|link`).
  - Combined strategy: RSS → HTML → Selenium.
- `infomoth/tech_scraper.py`: Source configuration and implementation for tech news.
- `infomoth/politics_scraper.py`: Source configuration and implementation for political news.
- `infomoth/exchange_rate_scraper.py`: Exchange rate scraping (Frankfurter/ECB).
- `infomoth/us_stock_index_scraper.py`: US stock index scraping (Yahoo Finance via yfinance).

## 4. Input/Output Contracts
- **Execution Command**: `python main.py`
- **Setup Command**: `pip install -r requirements.txt`
- **Output Files** (the `shared.directory` configured in `Config/app-config.json`):
  - `tech_news.json`
  - `politics_news.json`
  - `exchangeRates.json`
  - `ai_skills_today.json`
  - `us_stock_indices.json`
- **News Item Fields**:
  - `title`, `link`, `summary`, `published_date`, `source`
- **Exchange Rate Item Fields**:
  - `base_currency`, `base_currency_name`
  - `quote_currency`, `quote_currency_name`
  - `rate`, `date`, `source`
- **US Stock Index Item Fields**:
  - `symbol`, `name`
  - `price`, `change`, `changePercent`
  - `date` (the Yahoo close series' actual trading date), `source`

## 5. Scraping Strategy Standards
- Prioritize RSS; if it fails, fallback to HTML parsing or Selenium based on the source type.
- All network requests must include a timeout to prevent indefinite hanging.
- Handle 403 responses by retrying with different User-Agents.
- Selenium should only be used for sources that strictly require JS rendering to minimize resource overhead.
- Source strategy:
  1. RSS/Atom with `feedparser` (first choice)
  2. Standard HTML with `requests` + `BeautifulSoup`
  3. Headless Selenium only for JS-rendered pages
- Selenium runtime prerequisites:
  - Install one of:
    - Chrome + ChromeDriver
    - Firefox + GeckoDriver
  - Driver version must match browser version and be available in `PATH`.
- Current implementation notes:
  - Chinese-specific sources and language-specific parsing have been removed.
  - HTTP calls use rotating custom User-Agents and explicit timeout handling.

## 6. AI Agent Development Guidelines (Crawler)
- **Source Expansion**: When adding news sources, only modify the source configuration and necessary parsing logic in the corresponding `*_scraper.py`. Do not break the `BaseNewsScraper` contract.
- **Field Consistency**: New fields must be compatible with Backend parsing (especially field naming in `exchangeRates.json`).
- **File Stability**: Maintain stable output filenames and write location (`shared.directory`); otherwise, the file paths in the Backend `DataService` will become invalid.
- **Trading Dates**: Stamp US stock snapshots with the latest close series' trading date, never the crawler's current calendar date. If a scrape is empty, retain the last valid trading-day payload.
- **Logic Abstraction**: When modifying scraping strategies, prioritize extending the common base class instead of duplicating logic across multiple modules.

## 7. Quick Start
1. Install dependencies:
   - `pip install -r requirements.txt`
2. Run crawler:
   - `python main.py`
3. Check generated files under the configured `shared.directory`:
   - `tech_news.json`
   - `politics_news.json`
   - `exchangeRates.json`
   - `ai_skills_today.json`
   - `us_stock_indices.json`

## 8. Selenium Driver Requirements
- Selenium is only used for sources that require JS rendering.
- Install one of:
  - Chrome + ChromeDriver
  - Firefox + GeckoDriver
- Keep the browser and driver versions compatible, and ensure the driver is available in `PATH`.

## 9. Operational Notes
- Chinese-specific sources and language-specific parsing have been removed.
- HTTP calls use rotating custom User-Agents and explicit timeout handling.
- 403 responses are handled with retries using alternate User-Agents.

## Partial cycles and snapshot writes
- Each source is saved independently. Exceptions and empty scrape results retain that source's previous snapshot; other successful sources are still written and synced. Only a cycle where all sources fail raises a fatal crawler error.
- JSON saves use a temporary file in the destination directory and atomic replacement, preserving existing file permissions.
- Exchange-rate updates merge actual captured base/quote directions, retaining missing pairs with their original source dates and rejecting older observations for the same pair. No reverse or cross rates are synthesized.
