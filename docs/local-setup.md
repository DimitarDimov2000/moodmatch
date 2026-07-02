# Local Setup

This guide describes the current local development setup for MoodMatch.

## Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL

## PostgreSQL For The Dev Profile

The backend development profile expects PostgreSQL on:

- host: `localhost`
- port: `5432`
- database: `moodmatch`
- user: `moodmatch`
- password: `moodmatch`

Optional local overrides:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`

The current backend dev profile expects these variables to be present. For the same local database as this guide, set them to:

- `MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch`
- `MOODMATCH_DB_USERNAME=moodmatch`
- `MOODMATCH_DB_PASSWORD=moodmatch`

Example terminal setup before starting the backend:

```bash
export MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch
export MOODMATCH_DB_USERNAME=moodmatch
export MOODMATCH_DB_PASSWORD=moodmatch
```

Example `psql` setup:

```sql
CREATE USER moodmatch WITH PASSWORD 'moodmatch';
CREATE DATABASE moodmatch OWNER moodmatch;
```

If your local PostgreSQL setup requires it, connect as a superuser first:

```bash
psql postgres
```

## Flyway

Flyway runs automatically when the backend starts. It validates the committed migrations and applies them before the app serves requests.

Current migrations:

- `V1__init_schema.sql`
- `V2__seed_starter_tags.sql`
- `V3__add_app_users_and_media_ownership.sql`
- `V4__add_local_password_auth.sql`
- `V5__add_external_import_support.sql`
- `V6__expand_external_provider_model.sql`

## Optional TMDB Provider Setup

TMDB is the first real external provider. It is currently used for `FILM` and `SERIES` searches when the backend has a valid API key.

Optional local variables:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_TMDB_BASE_URL`
- `MOODMATCH_TMDB_IMAGE_BASE_URL`
- `MOODMATCH_TMDB_WEBSITE_BASE_URL`

Example terminal setup:

```bash
export MOODMATCH_TMDB_API_KEY=your_tmdb_api_key
```

If `MOODMATCH_TMDB_API_KEY` is missing, automatic film/series searches skip TMDB, return a warning, and still use other compatible providers such as AniList when available. If no compatible real provider can run, MoodMatch keeps the offline `DEMO` provider active as a fallback. No real API key is committed in this repository, and the frontend never receives the TMDB key directly.

## Open Library Provider Setup

Open Library is the active real provider for `BOOK` searches in Package 2.

Notes:

- No secret or paid API key is required for the implemented public search flow.
- Do not commit any personal contact header or experimental credentials to the repository.
- If you need endpoint overrides for debugging, they should stay local-only and out of committed secrets.

## LibriVox Provider Setup

LibriVox is the active real provider for `AUDIOBOOK` searches in Package 2.6.

Optional local variables:

- `MOODMATCH_LIBRIVOX_BASE_URL`

Notes:

- No secret or paid API key is required for the implemented public catalog integration.
- The LibriVox catalog only covers public-domain audiobooks, so missing modern titles are expected.
- The current prototype imports metadata only. It does not build playback, chapters, streaming, or progress tracking.

## Optional RAWG Provider Setup

RAWG is the active real provider for `GAME` searches in Package 2.7.

Backend-only local variable:

- `MOODMATCH_RAWG_API_KEY`

Example terminal setup:

```bash
export MOODMATCH_RAWG_API_KEY=your_rawg_api_key
```

If `MOODMATCH_RAWG_API_KEY` is missing, automatic game searches use the offline `DEMO` fallback and return a warning. Explicit `source=RAWG` searches return a clear provider configuration error. Do not commit a real RAWG key; the frontend never receives the key directly.

RAWG is included for non-commercial university prototype usage. Review RAWG attribution and usage terms before any production deployment.

## AniList Provider Setup

AniList is the active real provider for anime/manga metadata in Package 2.8.

Optional local variable:

- `MOODMATCH_ANILIST_BASE_URL`

Notes:

- No secret or API key is required.
- AniList can be selected explicitly and is also included automatically for compatible `FILM`, `SERIES`, and `BOOK` searches.
- Anime movies import as `FILM`, anime TV/OVA/ONA/special/short formats import as `SERIES`, and manga/light novel/novel/one-shot formats import as `BOOK`.
- MoodMatch does not add core `ANIME` or `MANGA` media types.

## Future Provider Configuration

These names are reserved for later provider packages. They are not required for the current app and should only be set once the matching provider is implemented:

- `MOODMATCH_PODCAST_INDEX_API_KEY`
- `MOODMATCH_PODCAST_INDEX_API_SECRET`
- `MOODMATCH_PODCAST_INDEX_BASE_URL`
- `MOODMATCH_YOUTUBE_API_KEY`
- `MOODMATCH_YOUTUBE_BASE_URL`

Provider scope notes:

- Podcast Index is planned for `PODCAST`; podcast episode import is out of scope.
- YouTube is planned for `VIDEO` URL import only, not search.
- IGDB is a backup/future game provider and is not active.
- Music is out of scope.

## Optional IntelliJ / PostgreSQL Inspection

This is optional and not required to run the app.

If you start the backend from IntelliJ IDEA instead of a terminal, you can set the same variables in the Run/Debug configuration environment:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`

For the local setup in this guide, use the same values shown above.

If you want to inspect the local database in IntelliJ IDEA, the Database tool window can connect with:

- host: `localhost`
- port: `5432`
- database: `moodmatch`
- user: `moodmatch`
- password: `moodmatch`

Useful tables to inspect:

- `media_items`
- `tags`
- `media_tags`
- `media_external_refs`
- `external_tag_mappings`
- `flyway_schema_history`

If IntelliJ shows an SQL warning because no data source is configured yet, that only means IntelliJ is not connected to a database for SQL assistance. It does not mean Flyway is broken or that the app failed to apply migrations.

## Start The Backend

```bash
cd backend
./mvnw quarkus:dev
```

Default backend URL:

```text
http://localhost:8080
```

Quick smoke check:

```bash
curl http://localhost:8080/api/health
```

## Run Backend Tests

```bash
cd backend
./mvnw test
```

Notes:

- Backend tests use the Quarkus test profile with H2 in PostgreSQL compatibility mode.
- Resource tests start a local Quarkus HTTP server during the test run.

## Frontend Setup And Run

Install dependencies:

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

## Frontend API Configuration

The frontend uses `/api` for backend requests.

In local Vite development:

- `frontend/vite.config.ts` proxies `/api` to `http://localhost:8080`

Optional override:

- `VITE_API_BASE_URL` can point the frontend at a different backend base URL
- if set, the frontend normalizes it to an `/api` base path

## Frontend Checks

```bash
cd frontend
npm run lint
npm run test
npm run build
```

Useful additional check:

```bash
cd frontend
npm run typecheck
```

## Quick End-To-End Demo Startup

1. Start PostgreSQL locally.
2. Start the backend with `cd backend && ./mvnw quarkus:dev`.
3. Start the frontend with `cd frontend && npm run dev`.
4. Open the frontend and inspect the dashboard, media flows, profile, matches, swipe mode, and external search preview.
5. Log in, search films/series and books, import one result, and verify it appears in the media library.
