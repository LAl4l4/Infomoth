# Frontend PROJECT_SPEC

## 1. Project Positioning
- **Type**: React Single Page Application (SPA)
- **Responsibilities**:
  - Homepage content display and interactive animations.
  - Login and Registration workflows.
  - Profile retrieval and updates.
  - Exchange rate data visualization (via Backend API).
  - Seven-day market trend visualization and sentiment/US stock correlation display.

## 2. Tech Stack & Frameworks
- **Language**: TypeScript / TSX
- **Framework**: React `19.2.0` (CRA/react-scripts)
- **Routing**: `react-router-dom` `7.x`
- **State Management**: Redux Toolkit + React Redux
- **Forms**: `react-hook-form`
- **Networking**: Axios (Unified instance + Interceptors)
- **Testing**: React Testing Library / jest-dom, standalone Jest 30 with Babel TypeScript transform

## 3. Engineering Structure Standards
- `src/API/`: Backend interface wrappers (`auth.ts`, `data.ts`, `prof.ts`)
- `src/Variable/`: Redux slices and store configuration
- `src/Main/`: Homepage and content area components
- `src/Account/`: Login, Registration, Profile, and Settings pages
- `src/App.tsx`: Routing entry point

## 4. Execution & Build
- **Install Dependencies**: `pnpm install`
- **Local Development**: `pnpm start`
- **Build**: `pnpm run build`
- **Test**: `pnpm test`

## 5. Frontend-Backend Interaction Conventions
- **Axios Base URL**: runtime `frontend.apiBaseUrl` from `Config/app-config.json` (locally `http://localhost:8080`)
- **Token Storage**:
  - The backend sets a 48-hour `HttpOnly` `authToken` Cookie; the frontend and Redux state never store the JWT.
  - Axios uses `withCredentials: true` so the browser sends the Cookie automatically.
  - The response interceptor redirects to `/login` on `401` errors; `/auth/logout` asks the backend to expire the Cookie.
- **Current API Contracts**:
  - `POST /auth/login` (JSON body: `username`, `pass`)
  - `POST /auth/register` (JSON body: `username`, `pass`, `email`)
  - `GET /auth/session`: Check whether the server-managed session Cookie is valid
  - `POST /auth/logout`: Expire the server-managed session Cookie
  - `GET /auth/pullProfiles`: Retrieve profile
  - `POST /auth/pushProfile`: Update profile (JSON body)
  - `GET /data/currencies`: List currencies
  - `GET /data/exchangerate?base=&quote=`: Fetch specific rate
  - `GET /data/sentiment`: Fetch the latest rolling-standard-deviation z-score plus today's mean across standardized samples as `{ normalizedScore, dailyAverage }`
  - `GET /data/market-trends`: Fetch seven calendar days of sentiment, persisted stock prices, captured daily percentage changes, and `corr` values. Price lines bridge missing weekend points while the axis labels those dates as `Weekend`; correlation uses all completed persisted history but only actual trading-day observations.
  - `GET /settings/general` / `PUT /settings/general`: Read and save the default tab plus both exchange-rate selector currencies

## 6. State Management Conventions
- **Store Composition**: Composed of slices: `login`, `page`, `isOpen`, `profile`.
- **Async Logic**: Use `createAsyncThunk` for all asynchronous requests.
- **State Linkage**: Login/Logout actions must be linked with the profile state (e.g., clearing profile data upon logout).

## 7. AI Agent Development Guidelines (Frontend)
- **API Encapsulation**: When adding new APIs, encapsulate them in `src/API/` first, then call them within slices or pages. Avoid writing direct Axios calls inside components.
- **Contract Mapping**: Do not assume backend response field names will never change. If the backend changes, perform compatibility mapping within the API layer.
- **Routing**: New pages must be explicitly registered in `App.js` routes.
- **Authentication**: Any changes related to authentication must verify:
  - Cookie credentials and server-side Cookie expiry/removal.
  - Axios request/response interceptor logic.
  - Redux login state consistency without storing the JWT.

## Data freshness and authentication
- Login, registration, and session endpoints return `{ success: boolean, result: string }`; use `success` for branching and `result` only for display. Credentials are sent in JSON bodies, never URL parameters.
- Data caches expire after five minutes. Mounted data views poll every five minutes; detail views expose manual refresh and the last successful fetch time. The fetch time is not the source publication time; stock cards and the overview show the source trading date.
- Failed refreshes retain cached values and their timestamp, with an error indicator. Exchange-rate freshness is tracked independently per currency pair.
- Runtime configuration failures clear the in-flight promise so subsequent requests can retry.
- Type checking: `pnpm exec tsc --noEmit`; tests: `pnpm test --runInBand`; build: `pnpm build`.
