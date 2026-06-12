# Architecture

MoodMatch is a small monorepo application with three active layers:

1. A Vue 3 + TypeScript frontend
2. A Quarkus + Java backend
3. A PostgreSQL database for local development

The app is deterministic, explainable, locally grounded, and rule-based. External preview data may help users discover metadata, but it does not drive MoodMatch scoring.

## Current Repository Structure

| Path | Purpose |
| --- | --- |
| `frontend/` | Route-based UI for dashboard, media management, profile, candidates, matches, swipe mode, and external search preview |
| `backend/` | REST API, validation, persistence, deterministic profile/matching logic, external search provider abstraction, and Flyway migrations |
| `docs/` | Setup notes, API contract, data model, testing notes, checkpoint docs, and ADRs |

## Frontend

The frontend is a Vite-powered Vue 3 app with typed API modules and route-level views.

Current user-facing areas:

- Dashboard
- Media library
- Media create/edit/detail flows
- Profile view
- Candidate view
- Matches view
- Swipe mode
- External search preview

The frontend talks to the backend through `/api`. In local development, Vite proxies `/api` to `http://localhost:8080`. The API base can also be overridden through `VITE_API_BASE_URL`.

The dashboard is currently assembled in the frontend from existing media, profile, candidates, and matches endpoints. There is no dedicated `/api/dashboard` backend endpoint yet.

## Backend

The backend exposes REST endpoints under `/api` and owns:

- request validation
- structured error responses
- entity persistence
- Flyway-based schema setup
- starter tag seeding
- media CRUD rules
- status and favourite validation
- interest profile calculation
- candidate listing
- deterministic match scoring and explanations
- preview-only external search normalization

The external search path is backend-only. The frontend never calls external providers directly.

## Database

Local development uses PostgreSQL. The backend dev profile expects:

- database: `moodmatch`
- user: `moodmatch`
- password: `moodmatch`
- port: `5432`

Flyway runs automatically on startup and applies the committed migrations before the backend serves requests.

Backend tests use H2 in PostgreSQL compatibility mode together with the same Flyway migrations.

## Current Data Flow

### Media Management

```text
Frontend view/form
→ frontend API module
→ /api/media
→ MediaResource
→ MediaService
→ repositories/entities
→ PostgreSQL
```

### Profile And Matching

```text
Frontend profile/matches/dashboard views
→ /api/profile, /api/candidates, /api/matches
→ backend services
→ confirmed local media + tags
→ deterministic weights and scores
→ explanation-focused DTOs
```

### External Search Preview

```text
ExternalSearchView
→ /api/external/search
→ ExternalSearchService
→ DemoExternalSearchProvider
→ normalized preview DTOs
→ read-only frontend cards
```

### Swipe Mode

```text
SwipeView
→ /api/candidates and /api/matches for queue context
→ local like/skip round state in the frontend
→ reject persists via PATCH /api/media/{id}/status to NOT_INTERESTED
```

## Important Semantics

- Favourite is a persisted domain field, not a swipe action.
- Swipe like and skip are local to the active swipe round.
- Swipe reject persists as `NOT_INTERESTED`.
- Matching uses confirmed local tags only.
- External preview results do not import automatically and do not affect scores automatically.

## Current Boundaries

Implemented now:

- media CRUD and tag replacement
- seeded tags and schema migrations
- profile calculation
- candidate listing
- deterministic matches
- swipe UI over existing APIs
- offline DEMO external search preview

Not implemented yet:

- external import into the local library
- real provider integrations
- dedicated dashboard endpoint
- dedicated decision-mode filter API
- persistent swipe-like/save behavior
