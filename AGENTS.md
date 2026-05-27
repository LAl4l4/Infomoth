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

# Tools for token-efficiency

## Project Context (OpenWolf)
This project uses OpenWolf for context management.
- At the start of every session, read `.wolf/anatomy.md` to understand the project structure.
- Before generating or modifying code, check `.wolf/cerebrum.md` for project conventions and preferences.
- If you are unsure about file organization, refer to `.wolf/anatomy.md` first instead of scanning many files.

### For Claude Code
- Claude Code already integrates OpenWolf natively via project-level hooks. It automatically reads `.wolf/anatomy.md` and `.wolf/cerebrum.md` according to its own lifecycle. **Do not manually re-read these files or add extra OpenWolf steps.**

## Token-Efficient Commands (RTK)
This project uses RTK (Rust Token Killer) to reduce token consumption from command output.
- **Golden Rule:** Always prefix shell commands with `rtk`.  
  Example: `rtk git status`, `rtk npm test`, `rtk mvn test`
- For command chains, prefix each subcommand:
  ```bash
  rtk git add . && rtk git commit -m "message" && rtk git push

# After this is the project details

## Build, test, and lint commands

This repository has 4 runnable parts: `Backend` (Spring Boot), `Frontend` (React), `Crawler` (Python scraper), and `Analyser` (Python analysis task).

Detailed specification files:
- Frontend: `Frontend/PROJECT_SPEC.md`
- Backend: `Backend/PROJECT_SPEC.md`
- Crawler: `Crawler/PROJECT_SPEC.md`
- Analyser: `Analyser/PROJECT_SPEC.md`

### Backend (`Backend/`)
- Run app: `cd Backend && ./mvnw spring-boot:run`
- Build jar: `cd Backend && ./mvnw package`
- Run all tests: `cd Backend && ./mvnw test`
- Run a single test class: `cd Backend && ./mvnw -Dtest=BackApplicationTests test`
- Requires MySQL database `blog` on `localhost:3306`, user `root`, empty password (see `Backend/src/main/resources/application.yaml`).

### Frontend (`Frontend/`)
- Install deps: `cd Frontend && pnpm install` (lockfile present) 
- Run dev server: `cd Frontend && pnpm start`
- Build: `cd Frontend && pnpm build`
- Run all tests once: `cd Frontend && pnpm test --watchAll=false`
- Run a single test file: `cd Frontend && pnpm test --watchAll=false --runTestsByPath src/App.test.js`

### Crawler (`Crawler/`)
- Install deps: `cd Crawler && pip install -r requirements.txt`
- Run scraper: `cd Crawler && python main.py`
- There is currently no dedicated automated test or lint command configured for this package.

### Analyser (`Analyser/`)
- Install deps: ensure `transformers` and `torch` are available in your Python environment.
- Run analyser: `cd Analyser && python main.py`
- First run downloads the FinBERT model (~500MB) to `models/` if not cached.
- There is currently no dedicated automated test or lint command configured for this package.

### Root helper commands
- `make dev` — starts backend and frontend in separate macOS Terminal windows via `osascript`.
- `make dev-backend` — opens a Terminal window for the backend only.
- `make dev-frontend` — opens a Terminal window for the frontend only.

## High-level architecture

### Data flow (order matters)

```
Crawler/main.py
  ├── TechNewsScraper      → Shared/tech_news.json
  ├── PoliticsNewsScraper  → Shared/politics_news.json
  ├── ExchangeRateScraper  → Shared/exchangeRates.json
  └── AiSkillScraper       → Shared/ai_skills_today.json

Analyser/main.py  (runs AFTER Crawler)
  ├── Reads Shared/tech_news.json & politics_news.json
  ├── FinBERT sentiment analysis (ProsusAI/finbert, cached in models/)
  └── Writes back to the SAME files, appending financeInfluence to each item

Backend (Spring Boot, port 8080)
  ├── Reads Shared/exchangeRates.json at request time (no caching)
  ├── Reads Shared/ai_skills_today.json (via aiSkillDTO)
  ├── MySQL database "blog" (tables: user, profiles, + dynamic sentiment tables)
  └── JWT-protected REST API

Frontend (React, port 3000)
  ├── Axios → Backend API (localhost:8080)
  └── Redux Toolkit state management
```

The pipeline must run Crawler before Analyser. The Analyser mutates source JSON in-place — do not run it while the Crawler is writing.

### Component details

- **Crawler scraping strategy**: `base_scraper.py` implements a 3-tier fallback per source: RSS feed → static HTML parse → headless Selenium (only when JS rendering is required). UA rotation and dedup are built into the base class.
- **Backend as API + file-backed data service**: `DataService` reads exchange rates from `../Shared/exchangeRates.json` at request time, so crawler output format/path is part of the runtime contract.
- **Frontend as API-driven SPA**: `Frontend` calls backend APIs through `src/API/*` wrappers, with centralized Axios interceptors for JWT injection and 401 handling.
- **Auth flow spans frontend + backend**:
  - Frontend stores JWT in `localStorage` key `authToken`.
  - Axios request interceptor sends `Authorization: Bearer <token>`.
  - Backend `WebConfig` + `JwtInterceptor` protect all routes except `/auth/login` and `/auth/register`.
- **Config directory**: `Config/app-config.json` is a centralized config reference (MySQL, API base URL, CORS origins), but backend currently reads from its own `application.yaml`.

## Key conventions in this codebase

- **Crawler owns data acquisition**: All crawlers and data normalization must stay under `Crawler/`. The backend only reads crawler-produced JSON files and must not implement crawling or external data-fetch logic.
- **Backend persistence style is MyBatis XML-first**: SQL is defined in `src/main/resources/mapper/*.xml`, with mapper interfaces in `mapper/`. Do not assume JPA annotations drive persistence behavior.
- **SentimentMapper uses dynamic table names**: `${table}` in `SentimentMapper.xml` is string interpolation (not a prepared-statement parameter). Callers control the table name — sanitize if ever exposing to user input.
- **Backend API layering is strict**: controller -> service -> mapper; DTO/VO classes are used for request/response shaping (`DTO/`, `VO/`).
- **Auth endpoints expect query params, not JSON body** for login/register (`@RequestParam` in `AuthController`), and frontend API helpers follow that contract.
- **Exchange-rate JSON schema is compatibility-sensitive**: backend `exchangeRateDTO` supports aliases (`base_currency`/`base`, `quote_currency`/`quote`). Keep scraper output backward-compatible when changing fields.
- **Frontend state flow**: API calls are wrapped in `src/API/`, then consumed by Redux Toolkit thunks/slices under `src/Variable/`; components generally interact with state/actions, not raw Axios.
- **CORS and local dev assumptions are explicit**:
  - Backend controllers are currently configured for `http://localhost:3000`.
  - Frontend Axios base URL is `http://localhost:8080`.
- **Shared/ is gitignored**: JSON artifacts in `Shared/` are not versioned. They must be generated by running the Crawler (and optionally Analyser) before the backend can serve exchange-rate or AI-skill data.
- **Analyser mutates source files in-place**: It reads news JSON from `Shared/`, adds `financeInfluence` to each item, and writes back. Do not assume the news JSON is read-only after the Crawler runs.
