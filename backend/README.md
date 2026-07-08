# MoodMatch Backend

This directory contains the Quarkus backend for MoodMatch. It provides the REST API, local authentication flow, persistence, provider integrations, Flyway migrations, and deterministic profile/matching logic used by the frontend.

## Stack

- Quarkus
- Java 21
- Maven
- Hibernate ORM Panache
- Flyway
- PostgreSQL for local development
- H2 for backend tests

## Backend Purpose

The backend owns:

- local email/password auth endpoints
- bearer-token validation for protected app endpoints in `local-password` mode
- media CRUD and validation
- tag listing and replacement
- interest profile calculation
- candidate selection
- deterministic match scoring and explanation payloads
- external provider search, URL resolve, and import normalization
- Flyway-based schema setup and validation

## Required Local Environment

Local development uses the root `.env.local` file, loaded by `backend/scripts/dev-local.sh`.

Required values:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`
- `MOODMATCH_AUTH_MODE=local-password`
- `QUARKUS_HTTP_CORS_ENABLED=true`
- `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`

Common optional local values:

- `QUARKUS_HTTP_CORS_METHODS`
- `QUARKUS_HTTP_CORS_HEADERS`
- `MOODMATCH_DEMO_EMAIL`
- `MOODMATCH_DEMO_PASSWORD`
- `MOODMATCH_DEMO_DISPLAY_NAME`

Reference examples:

- `../.env.local.example`
- `./.env.example`

## Provider Environment Variables

Optional backend-only provider keys:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_RAWG_API_KEY`
- `MOODMATCH_PODCASTINDEX_KEY`
- `MOODMATCH_PODCASTINDEX_SECRET`
- `MOODMATCH_YOUTUBE_API_KEY`

Optional provider base URL overrides:

- `MOODMATCH_TMDB_BASE_URL`
- `MOODMATCH_TMDB_IMAGE_BASE_URL`
- `MOODMATCH_TMDB_WEBSITE_BASE_URL`
- `MOODMATCH_LIBRIVOX_BASE_URL`
- `MOODMATCH_PODCASTINDEX_BASE_URL`
- `MOODMATCH_ANILIST_BASE_URL`
- `MOODMATCH_RAWG_BASE_URL`
- `MOODMATCH_RAWG_WEBSITE_BASE_URL`

## Auth Mode Notes

- Normal prototype development uses `local-password`.
- In `local-password` mode, private app endpoints are authenticated endpoints and require a valid bearer token.
- Protected paths include `/api/media`, `/api/profile`, `/api/candidates`, `/api/matches`, `/api/external/*`, `/api/auth/me`, and `/api/auth/logout`.
- Backend tests still use the test profile and do not require a running local account setup.

## Local Run

From the `backend/` directory:

```bash
./scripts/dev-local.sh
```

What the script does:

- loads the root `.env.local`
- exports the variables for the backend process
- starts Quarkus dev mode with `./mvnw quarkus:dev`

Default local backend URL:

```text
http://localhost:8080
```

Health check:

```text
GET /api/health
```

## Clean Backend Test

From the `backend/` directory:

```bash
./scripts/test-clean.sh
```

This helper intentionally:

- does not load the root `.env.local`
- unsets common DB, auth, CORS, OIDC, and provider variables first
- runs `./mvnw test`

Tests use H2 in PostgreSQL-compatibility mode plus the committed Flyway migrations, so CI and local test runs do not require PostgreSQL or live provider credentials.

## Flyway And Persistence

- Flyway migrations live in `src/main/resources/db/migration/`.
- Local startup validates the schema and migrates on backend startup.
- PostgreSQL is the normal local-development database.
- Backend tests exercise the same migration set against H2 compatibility mode.

## Provider Integration Overview

The backend integrates providers behind a normalized adapter layer:

- `DEMO`: offline fallback/demo data
- `TMDB`: films and series
- `OPEN_LIBRARY`: books
- `LIBRIVOX`: public-domain audiobooks
- `RAWG`: games
- `ANILIST`: anime/manga mapped into existing media types
- `PODCAST_INDEX`: podcast shows/feeds
- `YOUTUBE`: video query search and URL resolve/import

Provider keys stay server-side only. Missing keys must not break backend tests or CI.

## Troubleshooting

### PostgreSQL Is Not Running

- Start PostgreSQL locally before running `./scripts/dev-local.sh`.
- Verify `MOODMATCH_DB_URL` points to the expected local database.

### Wrong Database Credentials

- Recheck `MOODMATCH_DB_USERNAME` and `MOODMATCH_DB_PASSWORD` in the root `.env.local`.
- Confirm the configured database/user actually exists in PostgreSQL.

### Missing Provider Keys

- Real provider search/import may warn, partially degrade, or fail depending on the provider.
- Backend tests and CI should still pass without real provider keys.

### CORS Issues

- Confirm `QUARKUS_HTTP_CORS_ENABLED=true`.
- Confirm `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173` for the normal Vite dev origin.

### Datasource URL Missing

- `./scripts/dev-local.sh` expects the root `.env.local`.
- If `MOODMATCH_DB_URL` is missing, copy `../.env.local.example` to `../.env.local` and fill it first.

## Security Notes

- Keep API keys on the backend only.
- Do not commit `.env.local`, `frontend/.env.local`, or real secrets.
- Do not commit personal passwords or real bearer tokens.
- Prototype bearer-token auth is suitable for local demo/dev use, not a finished production deployment model.

For full setup and repo-level guidance, see [../docs/local-setup.md](../docs/local-setup.md) and [../README.md](../README.md).
