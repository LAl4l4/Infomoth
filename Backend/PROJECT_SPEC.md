# Backend PROJECT_SPEC

## 1. Project Positioning
- **Type**: REST API Service
- **Responsibilities**:
  - User authentication and profile management (`/auth/*`)
  - Exchange rate data retrieval (`/data/*`)
  - Seven-day sentiment/US stock trend data backed by full-history correlation calculation
- **External Dependencies**: Reads `exchangeRates.json` from the `shared.directory` in `Config/app-config.json`.

## 2. Tech Stack & Frameworks
- **Language/Runtime**: Java 25
- **Build Tool**: Maven (`pom.xml`)
- **Core Frameworks**:
  - Spring Boot `3.5.13`
  - Spring MVC (REST Controllers)
  - Spring Actuator
  - Springdoc OpenAPI UI `2.5.0`
- **Data Access**:
  - MyBatis Spring Boot Starter `3.0.5`
  - MySQL Connector/J (runtime)
  - Mapper XML (`src/main/resources/mapper/*.xml`)
- **Authentication**:
  - JWT (`io.jsonwebtoken:jjwt:0.12.5`)
  - Global Interceptor: `JwtInterceptor`
- **Other**:
  - Lombok
  - Spring Boot Test / MyBatis Test

## 3. Directory & Layering Standards
- `controller/`: HTTP Interface Layer
- `service/`: Business Logic Layer
- `mapper/` + `resources/mapper/*.xml`: MyBatis Data Access Layer
- `entity/`: Persistence Objects
- `DTO/`: Request/Intermediate Data Transfer Objects
- `VO/`: View Objects (Response Objects)
- `security/`: JWT utilities and interceptors
- `config/`: Web configuration (interceptor registration)
- `exception/`: Custom exceptions and global exception handling

> **Note**: When adding new features, prioritize following the existing layering. Do not write SQL or File I/O directly in Controllers.

## 4. Configuration & Execution
- **Runtime Configuration**: `Config/app-config.json`
  - Default Port: `8080`
  - MySQL host, port, user, password, and database: read from the root `mysql` object (`blog` locally)
  - Shared data directory: read from `shared.directory`
- **Execution**:
  - Run: `./mvnw spring-boot:run`
- **Common Commands**:
  - Run Tests: `./mvnw test`
  - Package: `./mvnw package`

## 5. API & Authentication Conventions
- **Public Endpoints** (No JWT required):
  - `POST /auth/login`
  - `POST /auth/register`
  - `GET /auth/session`
  - `POST /auth/logout`
- **Protected Endpoints**: All other endpoints are protected by default.
- **JWT Transmission**:
  - `HttpOnly` Cookie: `authToken`, `Path=/`, `SameSite=Lax`, `Max-Age=172800` seconds
  - Success Response: Sets the Cookie and returns `TokenVO { success, result }`; the JWT is not returned in the body
- **Key Endpoints**:
  - `GET /auth/pullProfiles`: Retrieve user profile
  - `POST /auth/pushProfile`: Update user profile (JSON Body: `ProfileDTO`)
  - `GET /data/currencies`: List available currencies
  - `GET /data/exchangerate?base=USD&quote=CNY`: Fetch specific exchange rate
  - `GET /data/sentiment`: Return the latest normalized sentiment and today's mean across all normalized samples as `{ normalizedScore, dailyAverage }`
  - `GET /data/market-trends`: Return seven calendar days of sentiment, persisted US stock prices and captured daily percentage changes, plus per-index full-history `corr`
  - `GET /settings/general`: Return `defaultPage`, `defaultBaseCurrency`, and `defaultQuoteCurrency`
  - `PUT /settings/general`: Persist the home tab plus both exchange-rate selector sides

## 6. Data & Contract Conventions
- User queries support both username and email (`findByNameEmail`).
- Profile and User entities are linked via `user_id`.
- `user_settings` stores both exchange-rate sides (`default_base_currency` and `default_quote_currency`) alongside `default_page`; missing values default to USD/CNY.
- Each immutable sentiment row stores its raw FinBERT score, cumulative rolling mean, cumulative sample standard deviation with Bessel correction, and cumulative sample count. The sample variance uses `M2 / (n - 1)` for `n > 1`; normalized sentiment is the z-score `(raw - mean) / standardDeviation`, with zero standard deviation mapped to zero.
- The sentiment persistence check runs every 15 minutes, but appends a category row only when the corresponding analysed news JSON content has changed. This keeps effective sentiment samples aligned with the hourly full Pipeline update while retaining retry checks between cycles.
- The latest SHA-256 for both analysed news files is stored in the single-row `sentiment_file_state` checkpoint, so change detection survives Backend restarts with O(1) state. Historical scheduler duplicates can be repaired with `scripts/fix_sentiment_duplicates.sql`.
- `us_stock_indices` stores one row per trading date. Each row contains the price and captured daily percentage change for S&P 500, Dow Jones, NASDAQ, and Russell 2000. The scheduled Backend persistence job groups the four snapshots in `Shared/us_stock_indices.json` by their source trading date and upserts that daily row.
- Market-trend `corr` uses every completed overlapping persisted date across the full available history and each persisted `change_percent` value directly; it is not limited to the seven dates returned for chart display and does not derive another return from adjacent stored prices.
- Exchange rate JSON parsing uses `exchangeRateDTO` with field compatibility:
  - `base_currency` / `base`
  - `quote_currency` / `quote`

## 7. AI Agent Development Guidelines (Backend)
- **Style Consistency**: Maintain current patterns when adding APIs:
  - Controllers only handle parameter reception and response returning.
  - Services encapsulate business logic.
  - Mappers + XML files maintain SQL queries.
- **Stability**: Keep response structures stable (especially login and profile endpoints) to avoid breaking frontend integrations.
- **Security**: Explicitly define whether new authenticated endpoints should be added to `excludePathPatterns`.
- **Logic**: Be aware that exchange rate logic depends on the local JSON file path generated by the crawler service.

## Authentication hardening (2026-09-06)
- `/auth/login` and `/auth/register` accept JSON bodies (`username`, `pass`, plus `email` for registration); query-only credential requests are no longer supported. Deploy the frontend and backend together.
- Login, registration, and session responses use `{ success, result }`. Localized result text is presentation only.
- New passwords use salted BCrypt (cost 12); successful legacy plaintext logins upgrade that user's password in the same service transaction. Existing hashes fit the current VARCHAR(100) column. BCrypt accepts at most 72 UTF-8 bytes; longer credentials are rejected.
- `JWT_SECRET` supplies the signing key (at least 32 bytes; use a cryptographically random value). Deployed Compose sets `AUTH_REQUIRE_SECRET=true` and refuses backend startup without a key. Unconfigured local development uses an ephemeral random key, so restarting logs users out.
- Set a persistent `JWT_SECRET` in the App VM's repository `.env` before deploying. Never commit this file. Key rotation invalidates existing sessions; users must log in again.
