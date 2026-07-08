# Local Setup

This guide describes the current local development, testing, and demo-QA workflow for MoodMatch.

The goal is repeatability:

- keep secrets out of Git
- use the committed helper scripts instead of ad-hoc shell setup
- make protected-route QA easy for local manual checks

## Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL
- `curl`

## 1. Start PostgreSQL

The backend expects a PostgreSQL database reachable from your local machine.

Typical local defaults:

- host: `localhost`
- port: `5432`
- database: `moodmatch`

If you need a simple local setup:

```bash
psql postgres
```

```sql
CREATE USER moodmatch WITH PASSWORD 'moodmatch';
CREATE DATABASE moodmatch OWNER moodmatch;
```

If you prefer different local credentials, keep them local and place them in the root `.env.local`.

## 2. Create Local Env Files

Copy the committed examples:

```bash
cp .env.local.example .env.local
cp frontend/.env.local.example frontend/.env.local
```

Files that must stay local only:

- `.env.local`
- `frontend/.env.local`

Committed placeholder/reference files:

- `.env.local.example`
- `frontend/.env.local.example`
- `backend/.env.example`

## 3. Fill The Root `.env.local`

The root `.env.local` is the source used by the backend dev script and the demo-user helper.

Required local values:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`
- `MOODMATCH_AUTH_MODE=local-password`
- `QUARKUS_HTTP_CORS_ENABLED=true`
- `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`

Recommended CORS values:

- `QUARKUS_HTTP_CORS_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS`
- `QUARKUS_HTTP_CORS_HEADERS=Accept,Authorization,Content-Type,Origin,X-Requested-With`

Optional provider keys:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_RAWG_API_KEY`
- `MOODMATCH_PODCASTINDEX_KEY`
- `MOODMATCH_PODCASTINDEX_SECRET`
- `MOODMATCH_YOUTUBE_API_KEY`

Optional local demo-user values:

- `MOODMATCH_DEMO_EMAIL`
- `MOODMATCH_DEMO_PASSWORD`
- `MOODMATCH_DEMO_DISPLAY_NAME`
- `MOODMATCH_LOCAL_BACKEND_URL` if you are not using `http://localhost:8080`

Safety rules:

- Do not commit `.env.local`.
- Do not commit `frontend/.env.local`.
- Do not commit real API keys.
- Do not commit personal credentials.

## 4. Fill `frontend/.env.local`

The committed example is valid for normal local development:

```dotenv
VITE_AUTH_MODE=local-password
VITE_API_BASE_URL=http://localhost:8080
```

The frontend also has a Vite `/api` proxy for local development, but the committed local example keeps the backend URL explicit and matches the current helper docs.

## 5. Start The Backend

From `backend/`:

```bash
./scripts/dev-local.sh
```

This script:

- reads the root `.env.local`
- exports those variables into the backend process
- starts Quarkus dev mode

Default backend URL:

```text
http://localhost:8080
```

Quick health check:

```bash
curl http://localhost:8080/api/health
```

Flyway migrations run automatically on backend startup.

## 6. Start The Frontend

If dependencies are not installed yet:

```bash
cd frontend
npm install
```

Start the frontend:

```bash
cd frontend
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

## 7. Create Or Verify The Demo User

From the repo root:

```bash
./scripts/dev-create-demo-user.sh
```

The helper:

- reads `MOODMATCH_DEMO_EMAIL`, `MOODMATCH_DEMO_PASSWORD`, and `MOODMATCH_DEMO_DISPLAY_NAME` from the root `.env.local`
- talks to `MOODMATCH_LOCAL_BACKEND_URL` or `http://localhost:8080` by default
- sends `POST /api/auth/register`
- if the user already exists, verifies the credentials with `POST /api/auth/login`

This is local QA only:

- no auth bypass
- no seeded production/demo user
- no backend behavior change

## 8. Local Demo QA Flow

After the helper succeeds:

1. Open `http://localhost:5173`.
2. Log in with `MOODMATCH_DEMO_EMAIL` and `MOODMATCH_DEMO_PASSWORD`.
3. Verify protected routes:
   `Dashboard`, `Profile`, `External Search`, `Media Library`, `Candidates`, `Swipe`, `Matches`
4. Check the German/English switch.
5. Check dark, light, and system theme behavior.
6. Check desktop and mobile widths.

## 9. Frontend Checks

From `frontend/`:

```bash
npm run lint
npm run test
npm run build
```

Optional extra check:

```bash
npm run typecheck
```

## 10. Clean Backend Test

From `backend/`:

```bash
./scripts/test-clean.sh
```

This helper:

- does not load `.env.local`
- unsets common auth, DB, CORS, OIDC, and provider variables
- runs `./mvnw test`

## 11. Provider Notes

- `TMDB` for `FILM` and `SERIES` uses `MOODMATCH_TMDB_API_KEY` when configured.
- `OPEN_LIBRARY` for `BOOK` does not need a secret in the current integration.
- `LIBRIVOX` for `AUDIOBOOK` does not need a secret in the current integration.
- `RAWG` for `GAME` uses `MOODMATCH_RAWG_API_KEY` when configured.
- `PODCAST_INDEX` for `PODCAST` uses `MOODMATCH_PODCASTINDEX_KEY` and `MOODMATCH_PODCASTINDEX_SECRET` when configured.
- `ANILIST` does not need a secret in the current integration.
- `YOUTUBE` search and URL import use `MOODMATCH_YOUTUBE_API_KEY` on the backend only.
- Missing provider keys must not block local core app development or automated tests.

## 12. Security Reminder

Never commit:

- `.env.local`
- `frontend/.env.local`
- real API keys
- real personal credentials
- real bearer tokens
