# AI Instructions for InfoMoth

## Build, test, and lint commands

This repository has 3 runnable parts: `Backend` (Spring Boot), `Frontend` (React), and `Crawler` (Python scraper).

Detailed Specification is in `PROJECT_SPEC.md` in the root directory. This file is a high-level overview and reference for developers.

### Backend (`Backend/`)
- Run app: `./mvnw spring-boot:run`
- Build jar: `./mvnw package`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw -Dtest=BackApplicationTests test`

### Frontend (`Frontend/`)
- Install deps: `pnpm install` (lockfile present) or `npm install`
- Run dev server: `pnpm start`
- Build: `pnpm build`
- Run all tests once: `pnpm test --watchAll=false`
- Run a single test file: `pnpm test --watchAll=false --runTestsByPath src/App.test.js`

### Crawler (`Crawler/`)
- Install deps: `pip install -r requirements.txt`
- Run scraper: `python main.py`
- There is currently no dedicated automated test or lint command configured for this package.

### Root helper command
- `make dev` starts backend and frontend in separate macOS Terminal windows via `osascript`.

## High-level architecture

- **Crawler-first data pipeline**: `Crawler/main.py` orchestrates `TechNewsScraper`, `PoliticsNewsScraper`, and `ExchangeRateScraper`, then writes JSON artifacts (`tech_news.json`, `politics_news.json`, `exchangeRates.json`) into the `Crawler` directory.
- **Backend as API + file-backed data service**: `Backend` exposes auth/profile and exchange-rate APIs. `DataService` reads exchange rates from `../Crawler/exchangeRates.json` at request time, so crawler output format/path is part of the runtime contract.
- **Frontend as API-driven SPA**: `Frontend` calls backend APIs through `src/API/*` wrappers, with centralized Axios interceptors for JWT injection and 401 handling.
- **Auth flow spans frontend + backend**:
  - Frontend stores JWT in `localStorage` key `authToken`.
  - Axios request interceptor sends `Authorization: Bearer <token>`.
  - Backend `WebConfig` + `JwtInterceptor` protect all routes except `/auth/login` and `/auth/register`.

## Key conventions in this codebase

- **Crawler owns data acquisition**: All crawlers and data normalization must stay under `Crawler/`. The backend only reads crawler-produced JSON files and must not implement crawling or external data-fetch logic.
- **Backend persistence style is MyBatis XML-first**: SQL is defined in `src/main/resources/mapper/*.xml`, with mapper interfaces in `mapper/`. Do not assume JPA annotations drive persistence behavior.
- **Backend API layering is strict**: controller -> service -> mapper; DTO/VO classes are used for request/response shaping (`DTO/`, `VO/`).
- **Auth endpoints expect query params, not JSON body** for login/register (`@RequestParam` in `AuthController`), and frontend API helpers follow that contract.
- **Exchange-rate JSON schema is compatibility-sensitive**: backend `exchangeRateDTO` supports aliases (`base_currency`/`base`, `quote_currency`/`quote`). Keep scraper output backward-compatible when changing fields.
- **Frontend state flow**: API calls are wrapped in `src/API/`, then consumed by Redux Toolkit thunks/slices under `src/Variable/`; components generally interact with state/actions, not raw Axios.
- **CORS and local dev assumptions are explicit**:
  - Backend controllers are currently configured for `http://localhost:3000`.
  - Frontend Axios base URL is `http://localhost:8080`.
