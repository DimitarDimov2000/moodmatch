# MoodMatch architecture

MoodMatch is a monorepo full-stack application with a Vue 3 frontend, a Quarkus REST backend, and PostgreSQL persistence. Its central design goal is explainable media matching: external metadata supports discovery, while deterministic profile and scoring rules operate on confirmed local data.

## Repository responsibilities

| Area | Responsibility |
| --- | --- |
| `frontend/` | Vue views, reusable components, routing, auth state, language/theme preferences, and typed API access |
| `backend/` | REST resources, validation, auth, ownership, application services, provider adapters, DTOs, entities, repositories, and tests |
| `backend/src/main/resources/db/migration/` | Flyway schema evolution and starter data |
| `docs/` | Current architecture/setup/API/testing references plus labelled historical records |
| `scripts/` | Repository-level local demo-user helper |

## System boundary

```text
Vue 3 / Vite views and components
          |
          v
Typed frontend API client
          |
          v
Quarkus REST resources (/api)
          |
          v
Application services
       /             \\
      v               v
Repositories      Provider adapters/gateways
      |               |
      v               v
PostgreSQL       TMDB, Open Library, RAWG, AniList,
 + Flyway         Podcast Index, LibriVox, YouTube, DEMO
```

The frontend/backend separation is enforced by the REST/JSON boundary. The frontend never receives provider secrets. Backend services own business rules, repositories own persistence access, and provider gateways translate third-party responses into stable MoodMatch DTOs.

## Frontend components

The Vue application is organized around views and reusable components:

- `App.vue`, `AppShell`, auth store, router, theme composable, and language preference controls form the application shell.
- Dashboard, External Search, Media Library, Media Detail/Form, Profile, Candidates, Swipe, and Matches views represent the main user workflows.
- Shared media cards, artwork fallback, tag chips, score displays, match explanations, profile contribution cards, and swipe controls keep repeated UI behavior component-based.
- API modules (`frontend/src/api/`) and typed models (`frontend/src/types/`) isolate HTTP details from views.

## Backend components

- **Resources:** `AuthResource`, `MediaResource`, `TagResource`, `ProfileResource`, `CandidateResource`, `MatchingResource`, `ExternalSearchResource`, and `HealthResource` expose the API.
- **Services:** media/tag rules, current-user resolution, local auth, external search/import, tag normalization, profile calculation, candidate generation, and matching/scoring.
- **Provider layer:** `external/adapter/` defines normalized contracts; provider-specific gateways implement TMDB, Open Library, RAWG, AniList, Podcast Index, LibriVox, YouTube, and DEMO behavior.
- **DTOs and mappers:** request/response DTOs keep the API contract stable while mappers separate transport shape from entities and provider responses.
- **Repositories/entities:** Panache repositories and JPA entities represent users, auth sessions, media, tags, external references, and ownership relationships.

## Auth and ownership

The normal prototype mode is `local-password`. Registration and login create/verify local accounts, and the backend issues opaque bearer sessions. `CurrentUserProvider` resolves the authenticated `AppUser`; private media, profile, candidate, and match calculations use that user as their scope. The frontend may hold a demo token, but it never chooses the owner of a record.

## Persistence and migrations

PostgreSQL is used for local development. Flyway applies the committed migrations at startup, including the initial media/tag schema, starter tags, ownership/auth support, and external-reference model. Backend tests use H2 in PostgreSQL compatibility mode with the same migration set.

## Main data flows

### Manual media and profile flow

```text
Media view/form
 -> /api/media
 -> MediaResource / MediaService
 -> user-scoped repository
 -> PostgreSQL
 -> InterestProfileService
 -> profile, candidates, matches, and dashboard DTOs
```

### External search/import flow

```text
External Search view
 -> /api/external/search or /api/external/resolve-url
 -> provider adapter and gateway
 -> normalized external result DTO
 -> /api/external/import
 -> user-owned MediaItem + external reference
```

### Matching flow

```text
Consumed, rated, tagged user media
 -> interest profile and readiness
 -> WANT_TO_CONSUME candidates
 -> weighted tag overlap and precision
 -> score/explanation DTOs
 -> Candidates, Swipe, Matches, Dashboard
```

## Design boundaries and limitations

- Matching is deterministic and rule-based, not generative AI.
- External metadata does not directly become a user's profile; imported media must become confirmed local data.
- Provider availability can be degraded by missing keys, quota, network, or third-party metadata quality.
- Local-password auth and bearer tokens are suitable for the university prototype and local demo, not a production identity platform.
- The repository does not include a default public deployment or hosted database.
