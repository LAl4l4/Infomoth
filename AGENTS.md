# AI Instructions for InfoMoth

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## After this is the project details

## Build, test, and lint commands

This repository has 4 runnable parts: `Backend` (Spring Boot), `Frontend` (React), `Crawler` (Python scraper), and `Analyser` (Python analysis task).

Detailed specification files:
- Root overview: `PROJECT_SPEC.md`
- Backend: `Backend/PROJECT_SPEC.md`
- Crawler: `Crawler/PROJECT_SPEC.md`
- Analyser: `Analyser/PROJECT_SPEC.md`

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

### Analyser (`Analyser/`)
- Install deps: ensure `transformers` and runtime backend (`torch`) are available in your Python environment.
- Run analyser: `python main.py`
- There is currently no dedicated automated test or lint command configured for this package.

### Root helper command
- `make dev` starts backend and frontend in separate macOS Terminal windows via `osascript`.

## High-level architecture

- **Crawler-first data pipeline**: `Crawler/main.py` orchestrates `TechNewsScraper`, `PoliticsNewsScraper`, and `ExchangeRateScraper`, then writes JSON artifacts (`tech_news.json`, `politics_news.json`, `exchangeRates.json`) into the `Shared` directory.
- **Backend as API + file-backed data service**: `Backend` exposes auth/profile and exchange-rate APIs. `DataService` reads exchange rates from `../Shared/exchangeRates.json` at request time, so crawler output format/path is part of the runtime contract.
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
