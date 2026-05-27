# InfoMoth

InfoMoth is a multi-module project for collecting market/news data, analyzing sentiment, and serving it to a web app/pages.

## Core Functions
- Crawl and aggregate:
  - Technology news
  - Global politics news
  - Exchange rates
  - AI skill trends
  - US stock index snapshots
- Analyze financial sentiment on crawled news with FinBERT.
- Store sentiment daily averages in MySQL (analyser side).
- Expose backend APIs for auth, profile, exchange rates, sentiment, and AI skills.
- Provide a frontend SPA for login, profile, and data display.

## Project Structure
- `Crawler/`: Batch data collection, outputs JSON files into `Shared/`.
- `Analyser/`: Reads `Shared/` news files, adds sentiment results, writes back.
- `Backend/`: Spring Boot API service, reads crawler/analyser outputs.
- `Frontend/`: React app consuming backend APIs.
- `Shared/`: Cross-module JSON data artifacts.
- `Config/`: Shared runtime configuration.

## Typical Data Flow
1. Run `Crawler` to generate data files under `Shared/`.
2. Run `Analyser` to enrich news with finance sentiment and persist averages.
3. Run `Backend` to serve APIs backed by database + `Shared/` files.
4. Run `Frontend` to interact with backend APIs in the browser.
