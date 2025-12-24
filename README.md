# DxVision

Full-stack training platform with Spring Boot backend (JWT + JPA) and Vite/React frontend.

## How to run locally

1. **Backend (local profile with seed data)**
   - Ensure MySQL is running (or start `docker-compose` from `docker/docker-compose.yml`).
   - From `backend/`:
     ```bash
     ./gradlew bootRun
     ```
   - Defaults: `SPRING_PROFILES_ACTIVE=local`, DB `jdbc:mysql://localhost:3306/dxvision` (username `root`, password `password`). Seeds create `admin@example.com` / `user@example.com` with password `Password123!`.

2. **Frontend**
   - From `frontend/`:
     ```bash
     npm install
     npm run dev
     ```
   - Vite dev server proxies `/api` to `http://localhost:8080`. API calls default to relative `/api/v1`.

3. **Tests**
   - Backend (H2): `cd backend && ./gradlew test`
   - Frontend build check: `cd frontend && npm run build`

## How to deploy

1. **Backend**
   - Use profile `prod`:
     ```
     SPRING_PROFILES_ACTIVE=prod
     DB_URL=jdbc:mysql://<host>:3306/dxvision
     DB_USERNAME=...
     DB_PASSWORD=...
     JWT_SECRET=<32+ chars>
     JWT_EXPIRATION_MS=3600000
     CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
     ```
   - Run with `./gradlew bootRun` or package with `./gradlew bootJar` and deploy the jar.
   - `/api` endpoints are CORS-configured via `CORS_ALLOWED_ORIGINS`.

2. **Frontend**
   - Build: `cd frontend && npm run build`
   - Serve `frontend/dist` behind the same domain as the backend for simplest CORS (reverse proxy `/api` → backend `:8080`).
   - If hosting separately, set `VITE_API_BASE_URL` to the backend base (e.g., `https://api.example.com/api/v1`), and ensure CORS allows that origin.

3. **Recommended architecture**
   - Single domain with reverse proxy:
     - `/` → static files from `frontend/dist`
     - `/api/*` → backend service
   - Configure HTTPS and environment variables above.
