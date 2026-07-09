# MoodMatch Backend

The `backend/` directory contains the Quarkus REST API for MoodMatch. It owns local authentication, user ownership, media and tag management, profile readiness, candidate selection, deterministic matching, external provider integration, and database migrations.

## Stack and runtime

- Java 21 and Quarkus
- REST/JSON resources with Hibernate ORM Panache
- PostgreSQL for local development
- H2 in PostgreSQL compatibility mode for tests
- Flyway migrations
- Maven via the included `mvnw` wrapper

## Package/component overview

- `resource/` — REST endpoints under `/api`
- `service/` — application rules, current-user resolution, local auth, profile, candidates, matching, media, tags, and external orchestration
- `auth/` — local password hashing, session tokens, and bearer-token filtering
- `external/` — provider gateways and normalized adapter contracts for external metadata
- `dto/` — request/response contracts exchanged with the frontend
- `entity/` — persistence model and enums
- `repository/` — Panache repositories and user-scoped data access
- `mapper/` — entity/provider to DTO mapping
- `src/main/resources/db/migration/` — committed Flyway schema migrations
- `src/test/` — Quarkus resource/service/provider tests and migration checks

Important REST areas are `/api/auth`, `/api/media`, `/api/tags`, `/api/profile`, `/api/candidates`, `/api/matches`, `/api/external`, and `/api/health`. The complete implemented endpoint contract is documented in [../docs/api-contract.md](../docs/api-contract.md).

## Local configuration

The normal development script loads the repository root `.env.local`:

```bash
cp ../.env.local.example ../.env.local
./scripts/dev-local.sh
```

Required local values include:

- `MOODMATCH_DB_URL`
- `MOODMATCH_DB_USERNAME`
- `MOODMATCH_DB_PASSWORD`
- `MOODMATCH_AUTH_MODE=local-password`
- `QUARKUS_HTTP_CORS_ENABLED=true`
- `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`

Provider keys are optional and backend-only:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_RAWG_API_KEY`
- `MOODMATCH_PODCASTINDEX_KEY`
- `MOODMATCH_PODCASTINDEX_SECRET`
- `MOODMATCH_YOUTUBE_API_KEY`

`backend/.env.example` is a backend variable reference. It contains placeholders only; it is not loaded automatically by the scripts. Optional provider base URL overrides are listed there.

## Authentication modes

- `local-password` is the normal local/demo mode. Users register and log in through `/api/auth/register` and `/api/auth/login`; protected requests use a bearer token.
- `local-demo` remains available as a development/test fallback where configured, but it is not the normal submission setup.
- OIDC/Google configuration is not required for the current prototype.

Auth sessions and media ownership are backed by the committed migrations. The backend resolves the current user server-side; the frontend does not submit arbitrary ownership identifiers.

## PostgreSQL and Flyway

PostgreSQL is the local runtime database. Flyway applies and validates the committed migrations during startup. They cover the initial media/tag schema, starter tags, user ownership, local password auth, and external provider references.

Do not edit an applied migration for local experiments; add a new migration when the schema changes. This submission does not require or include a schema change.

## Scripts

From `backend/`:

```bash
./scripts/dev-local.sh     # loads root .env.local and starts Quarkus dev mode
./scripts/test-clean.sh    # clears local override variables and runs ./mvnw test
```

`test-clean.sh` intentionally does not load `.env.local`, PostgreSQL credentials, or live provider keys. It is the command used by CI and by the repository README for reproducible backend checks.

## Tests

```bash
./scripts/test-clean.sh
```

The tests cover REST resources, services, local auth, ownership isolation, DTO/entity mapping, profile readiness, matching/scoring, migrations, and provider adapters. H2 and test profiles keep the suite independent of a running PostgreSQL instance and real provider credentials.

## Provider behavior

Provider gateways are normalized behind `external/adapter/`. The active integrations are TMDB, Open Library, RAWG, AniList, Podcast Index, LibriVox, YouTube, and the offline DEMO fallback. Missing optional keys are reported through controlled warnings/errors and must not make the clean test suite depend on external services.

For local setup, provider limitations, and security reminders, see [../docs/local-setup.md](../docs/local-setup.md), [../docs/external-media-sources.md](../docs/external-media-sources.md), and the [root README](../README.md).
