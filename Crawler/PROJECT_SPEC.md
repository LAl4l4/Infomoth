# Crawler PROJECT_SPEC

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
- **Output Files** (`../Shared` directory):
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
  - `price`, `change`, `change_percent`
  - `date`, `source`

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
- **File Stability**: Maintain stable output filenames and write location (`../Shared`); otherwise, the file paths in the Backend `DataService` will become invalid.
- **Logic Abstraction**: When modifying scraping strategies, prioritize extending the common base class instead of duplicating logic across multiple modules.

## 7. Quick Start
1. Install dependencies:
   - `pip install -r requirements.txt`
2. Run crawler:
   - `python main.py`
3. Check generated files under `../Shared`:
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
