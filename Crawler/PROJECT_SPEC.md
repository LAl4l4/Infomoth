# Crawler PROJECT_SPEC

## 1. Project Positioning
- **Type**: Python Scraping Tasks (Batch Processing)
- **Responsibilities**:
  - Scrape Tech News and International Politics News.
  - Scrape exchange rates for major currencies.
  - Output JSON files for Backend consumption.

## 2. Tech Stack & Frameworks
- **Language/Runtime**: Python `3.13.3`
- **Core Dependencies**:
  - `requests`: HTTP requests
  - `beautifulsoup4`: HTML parsing
  - `feedparser`: RSS/Atom parsing
  - `selenium`: Fallback for JavaScript-heavy dynamic pages
  - `ThreadPoolExecutor`: Improve the performance by multi-thread

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

## 4. Input/Output Contracts
- **Execution Command**: `python main.py`
- **Output Files** (`../Shared` directory):
  - `tech_news.json`
  - `politics_news.json`
  - `exchangeRates.json`
  - `ai_skills_today.json`
- **News Item Fields**:
  - `title`, `link`, `summary`, `published_date`, `source`
- **Exchange Rate Item Fields**:
  - `base_currency`, `base_currency_name`
  - `quote_currency`, `quote_currency_name`
  - `rate`, `date`, `source`

## 5. Scraping Strategy Standards
- Prioritize RSS; if it fails, fallback to HTML parsing or Selenium based on the source type.
- All network requests must include a timeout to prevent indefinite hanging.
- Handle 403 responses by retrying with different User-Agents.
- Selenium should only be used for sources that strictly require JS rendering to minimize resource overhead.

## 6. AI Agent Development Guidelines (Crawler)
- **Source Expansion**: When adding news sources, only modify the source configuration and necessary parsing logic in the corresponding `*_scraper.py`. Do not break the `BaseNewsScraper` contract.
- **Field Consistency**: New fields must be compatible with Backend parsing (especially field naming in `exchangeRates.json`).
- **File Stability**: Maintain stable output filenames and write location (`../Shared`); otherwise, the file paths in the Backend `DataService` will become invalid.
- **Logic Abstraction**: When modifying scraping strategies, prioritize extending the common base class instead of duplicating logic across multiple modules.
