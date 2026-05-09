# Frontend PROJECT_SPEC

## 1. Project Positioning
- **Type**: React Single Page Application (SPA)
- **Responsibilities**:
  - Homepage content display and interactive animations.
  - Login and Registration workflows.
  - Profile retrieval and updates.
  - Exchange rate data visualization (via Backend API).

## 2. Tech Stack & Frameworks
- **Language**: JavaScript (Non-TypeScript)
- **Framework**: React `19.2.0` (CRA/react-scripts)
- **Routing**: `react-router-dom` `7.x`
- **State Management**: Redux Toolkit + React Redux
- **Forms**: `react-hook-form`
- **Networking**: Axios (Unified instance + Interceptors)
- **Testing**: react-testing-library / jest-dom (CRA defaults)

## 3. Engineering Structure Standards
- `src/API/`: Backend interface wrappers (`auth.js`, `data.js`, `prof.js`)
- `src/Variable/`: Redux slices and store configuration
- `src/Main/`: Homepage and content area components
- `src/Account/`: Login, Registration, and Profile pages
- `src/App.js`: Routing entry point

## 4. Execution & Build
- **Install Dependencies**: `pnpm install`
- **Local Development**: `pnpm start`
- **Build**: `pnpm run build`
- **Test**: `pnpm test`

## 5. Frontend-Backend Interaction Conventions
- **Axios Base URL**: `http://localhost:8080`
- **Token Storage**:
  - localStorage key: `authToken`
  - Request interceptor automatically injects `Authorization: Bearer <token>`
  - Response interceptor automatically clears token and redirects to `/login` on `401` errors.
- **Current API Contracts**:
  - `POST /auth/login` (Query params: `username`, `pass`)
  - `POST /auth/register` (Query params: `username`, `pass`, `email`)
  - `GET /auth/pullProfiles`: Retrieve profile
  - `POST /auth/pushProfile`: Update profile (JSON body)
  - `GET /data/currencies`: List currencies
  - `GET /data/exchangerate?base=&quote=`: Fetch specific rate

## 6. State Management Conventions
- **Store Composition**: Composed of slices: `login`, `page`, `isOpen`, `profile`.
- **Async Logic**: Use `createAsyncThunk` for all asynchronous requests.
- **State Linkage**: Login/Logout actions must be linked with the profile state (e.g., clearing profile data upon logout).

## 7. AI Agent Development Guidelines (Frontend)
- **API Encapsulation**: When adding new APIs, encapsulate them in `src/API/` first, then call them within slices or pages. Avoid writing direct Axios calls inside components.
- **Contract Mapping**: Do not assume backend response field names will never change. If the backend changes, perform compatibility mapping within the API layer.
- **Routing**: New pages must be explicitly registered in `App.js` routes.
- **Authentication**: Any changes related to authentication must verify:
  - localStorage token persistence/removal.
  - Axios request/response interceptor logic.
  - Redux login state consistency.
