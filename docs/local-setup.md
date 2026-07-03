# Local Setup

This guide describes the standard local development and authenticated demo-QA setup for MoodMatch.

The goal is repeatability:

- keep real secrets out of Git
- stop manually exporting backend variables for every session
- make protected-route QA easy for Codex and manual browser checks

## Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL
- `curl`

## 1. Start PostgreSQL Locally

The local backend expects PostgreSQL on:

- host: `localhost`
- port: `5432`
- database: `moodmatch`

If you still need a local database and user, one simple setup is:

```bash
psql postgres
```

```sql
CREATE USER moodmatch WITH PASSWORD 'moodmatch';
CREATE DATABASE moodmatch OWNER moodmatch;
```

If you already use different local credentials, keep them local and put them into your root `.env.local`.

## 2. Create Local Env Files

Copy the committed examples first:

```bash
cp .env.local.example .env.local
cp frontend/.env.local.example frontend/.env.local
```

Files to keep local only:

- `.env.local`
- `backend/.env.local`
- `frontend/.env.local`

Example files are safe to commit because they contain placeholders only:

- `.env.local.example`
- `frontend/.env.local.example`
- `backend/.env.example`

## 3. Fill Local Env Vars Safely

Edit the new root `.env.local` and set your real local-only values.

Core backend values:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`
- `MOODMATCH_AUTH_MODE=local-password`
- `MOODMATCH_PRIVATE_ENDPOINT_POLICY=authenticated`
- `QUARKUS_HTTP_CORS_ENABLED=true`
- `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`
- `QUARKUS_HTTP_CORS_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS`
- `QUARKUS_HTTP_CORS_HEADERS=Accept,Authorization,Content-Type,Origin,X-Requested-With`

Optional backend-only provider keys:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_RAWG_API_KEY`
- `MOODMATCH_PODCASTINDEX_KEY`
- `MOODMATCH_PODCASTINDEX_SECRET`
- `MOODMATCH_YOUTUBE_API_KEY`

Optional local demo-QA account values used by the helper script:

- `MOODMATCH_DEMO_EMAIL`
- `MOODMATCH_DEMO_PASSWORD`
- `MOODMATCH_DEMO_DISPLAY_NAME`

The frontend local file should normally stay as:

```dotenv
VITE_AUTH_MODE=local-password
VITE_API_BASE_URL=http://localhost:8080
```

Important safety rules:

- Do not commit `.env.local`.
- Do not commit `frontend/.env.local`.
- Do not commit real API keys.
- Do not commit personal credentials.

## 4. Start The Backend

The backend local helper reads the root `.env.local`, exports the variables for the current process, and starts Quarkus dev mode.

```bash
cd backend
./scripts/dev-local.sh
```

Default backend URL:

```text
http://localhost:8080
```

Quick smoke check:

```bash
curl http://localhost:8080/api/health
```

Flyway migrations still run automatically on backend startup.

## 5. Start The Frontend

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

## 6. Create Or Check The Local Demo User

Use the local helper from the project root:

```bash
./scripts/dev-create-demo-user.sh
```

What it does:

- reads `MOODMATCH_DEMO_EMAIL`, `MOODMATCH_DEMO_PASSWORD`, and `MOODMATCH_DEMO_DISPLAY_NAME` from the root `.env.local`
- talks to the local backend at `http://localhost:8080` by default
- sends `POST /api/auth/register`
- if the user already exists, checks the configured credentials with `POST /api/auth/login`

This is local QA only:

- no bypass login
- no backend auth changes
- no seeded demo user migration

## 7. Demo QA Login Instructions

After the helper succeeds:

1. Open `http://localhost:5173`.
2. Log in with `MOODMATCH_DEMO_EMAIL` and `MOODMATCH_DEMO_PASSWORD` from your root `.env.local`.
3. Continue with the authenticated verification flow below.

## 8. Standard Codex / Manual Browser QA Workflow

Use this exact order for reliable authenticated QA:

1. PostgreSQL is running.
2. Backend is running on `http://localhost:8080`.
3. Frontend is running on `http://localhost:5173`.
4. Demo user exists.
5. Log in with the local demo account.
6. Verify protected routes:
   `Dashboard`, `Profile`, `External Search`, `Media Library`, `Candidates`, `Swipe`, `Matches`
7. Check the DE/EN language switch.
8. Check the dark/light/system theme switch.
9. Check both desktop and mobile widths.

## 9. Run Backend Tests Cleanly

Use the clean backend helper instead of a shell that may still contain local exports:

```bash
cd backend
./scripts/test-clean.sh
```

This script:

- does not load `.env.local`
- unsets common auth, DB, CORS, OIDC, and provider variables before running tests
- runs `./mvnw test`

## 10. Run Frontend Checks

Run the standard frontend checks:

```bash
cd frontend
npm run lint && npm run test && npm run build
```

Useful extra check:

```bash
cd frontend
npm run typecheck
```

## Provider Notes

Provider behavior is unchanged. Local secrets stay backend-only.

Current provider notes:

- TMDB for `FILM` and `SERIES` uses `MOODMATCH_TMDB_API_KEY` when configured.
- Open Library for `BOOK` does not need a secret in the current integration.
- LibriVox for `AUDIOBOOK` does not need a secret in the current integration.
- RAWG for `GAME` uses `MOODMATCH_RAWG_API_KEY` when configured.
- Podcast Index for `PODCAST` uses `MOODMATCH_PODCASTINDEX_KEY` and `MOODMATCH_PODCASTINDEX_SECRET` when configured.
- AniList does not need a secret in the current integration.
- YouTube search and URL import use `MOODMATCH_YOUTUBE_API_KEY` on the backend only.

If provider keys are missing, the existing application behavior stays the same. This package does not change provider logic.

## Security Reminder

Never commit any of the following:

- `.env.local`
- `backend/.env.local`
- `frontend/.env.local`
- real API keys
- real personal credentials
