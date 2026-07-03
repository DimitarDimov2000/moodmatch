# MoodMatch Backend

This directory contains the Quarkus backend for MoodMatch.

## What The Backend Provides

- `GET /api/health`
- Media CRUD under `/api/media`
- Tag listing under `/api/tags`
- Interest profile calculation under `/api/profile`
- Candidate listing under `/api/candidates`
- Deterministic match scoring under `/api/matches`
- External search under `/api/external/search`
- External import under `/api/external/import`

The backend owns validation rules, persistence, Flyway migrations, DTO mapping, profile calculation, match scoring, and explanation messages. External search prefers TMDB for films and series when `MOODMATCH_TMDB_API_KEY` is configured, uses Open Library for books, and otherwise keeps the offline DEMO provider available. Imported media is stored as normal user-owned `MediaItem` data with external references, and the provider model is prepared for later games, audiobooks, podcasts, videos, and AniList-sourced anime/manga mappings.

## Development Setup

The local development workflow uses the root `.env.local` file plus the helper scripts in `backend/scripts/`.

Create your local env file from the committed example:

```bash
cp ../.env.local.example ../.env.local
```

The development profile uses PostgreSQL on:

- host: `localhost`
- port: `5432`
- database: `moodmatch`
- user: `moodmatch`
- password: `moodmatch`

Flyway runs automatically on backend startup and validates the schema before the app serves requests.

Start dev mode:

```bash
cd backend
./scripts/dev-local.sh
```

Default backend URL:

```text
http://localhost:8080
```

Health check:

```text
GET /api/health
```

## Tests

Run backend tests:

```bash
cd backend
./scripts/test-clean.sh
```

Tests use the Quarkus test profile with H2 in PostgreSQL compatibility mode plus Flyway migrations. The clean test helper intentionally avoids loading the root `.env.local` file and unsets common leaking auth/provider variables first.

For full local project setup, frontend commands, and checkpoint context, see:

- [`../docs/local-setup.md`](../docs/local-setup.md)
- [`../docs/checkpoint-phase-19.md`](../docs/checkpoint-phase-19.md)
