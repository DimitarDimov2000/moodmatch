# Architecture

MoodMatch is a monorepo prototype with a Vue frontend, a Quarkus backend, and a relational datastore for local development.

The core principle is explainability:

- matching stays deterministic
- provider data is normalized but not treated as the source of truth for scoring
- imported titles become local user-owned media before they meaningfully influence recommendations

## Repository Structure

| Path | Purpose |
| --- | --- |
| `frontend/` | Vue 3 SPA for auth, media management, external search/import, profile, candidates, swipe, and matches |
| `backend/` | Quarkus API for auth, persistence, provider integrations, profile logic, matching logic, and Flyway migrations |
| `docs/` | Setup, architecture, scoring, API, provider, testing, and historical project notes |
| `scripts/` | Repo-level helper scripts such as demo-user creation |

## Frontend

The frontend is a Vite-powered Vue 3 single-page application.

It owns:

- login and local account flows
- protected route navigation
- media library screens
- manual media create/edit/detail flows
- external search, URL resolve, and import UI
- profile/readiness views
- candidate and match views
- swipe-style decision flow
- theme and language switching

API behavior:

- the frontend can use `VITE_API_BASE_URL`
- the default local Vite dev server also proxies `/api` to `http://localhost:8080`
- provider secrets never live in the frontend

## Backend

The backend exposes REST endpoints under `/api` and owns:

- local email/password registration and login
- bearer-token validation for protected routes in `local-password` mode
- current-user scoping for media and matching data
- media CRUD and validation
- Flyway schema migration and validation
- tag listing and external-tag suggestion mapping
- interest profile calculation
- candidate loading
- deterministic match scoring and explanations
- external provider search, URL resolve, and import normalization

## Database

Local development uses PostgreSQL. The backend expects the datasource from the root `.env.local`.

Flyway behavior:

- migrations run on backend startup
- schema validation stays enabled
- committed migrations are the source of truth for local database shape

Backend tests use H2 in PostgreSQL-compatibility mode so automated tests and CI do not require a running PostgreSQL instance.

## Provider Architecture

External provider access is backend-only.

Current providers:

- `DEMO`
- `TMDB`
- `OPEN_LIBRARY`
- `LIBRIVOX`
- `RAWG`
- `ANILIST`
- `PODCAST_INDEX`
- `YOUTUBE`

Key rules:

- the frontend never sends provider secrets directly
- provider responses are normalized into shared API DTOs
- imports create normal local `MediaItem` data plus external reference metadata
- provider availability can add warnings or fallback behavior, but should not require real keys for tests

## High-Level Data Flow

### Local Media Flow

```text
Vue views/components
-> frontend API client
-> /api/media
-> backend resource/service layer
-> persistence layer
-> PostgreSQL
```

### Profile And Matching Flow

```text
Current user's consumed/rated/tagged media
-> interest profile calculation
-> WANT_TO_CONSUME candidate loading
-> deterministic weighted tag overlap
-> explanation-focused DTOs
-> profile/candidates/matches/swipe UI
```

### External Search And Import Flow

```text
External Search view
-> /api/external/search or /api/external/resolve-url
-> backend provider adapter(s)
-> normalized external DTOs
-> user chooses import
-> /api/external/import
-> imported title becomes local user-owned media
```

## Scoring Boundary

MoodMatch does not use a generative model for ranking. Matching is driven by persisted local tags, ratings, favourites, and readiness rules. External metadata can help discovery and import, but it is not the final authority for scoring until it has become confirmed local data.

## Current Prototype Boundaries

Implemented now:

- local email/password auth
- protected frontend routes
- multi-provider external search/import
- profile readiness and deterministic match explanations
- swipe-oriented local decision flow
- German/English UI switching
- dark/light/system theming

Not finished as production work:

- hardened deployment/auth/session infrastructure
- end-to-end browser automation
- a fully finalized persisted swipe-like/save model
- public deployment as a default repo outcome
