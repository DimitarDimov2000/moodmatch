# MoodMatch

MoodMatch is a component-based media discovery application for finding films, series, books, games, audiobooks, podcasts, and videos that fit a user's personal taste. It addresses the problem of opaque, single-category recommendations by building an explainable taste profile from the user's own media library and showing why a candidate matches.

## Project idea and goal

Users build a taste profile from media they have consumed, rated, favourited, and tagged. MoodMatch combines that profile with candidate media, external metadata providers, and a dating-app-like swipe workflow. Recommendations are deterministic and explainable: the application exposes the matching tags, readiness state, and score instead of hiding the decision behind a black box.

The project implements the submitted idea as a full-stack university prototype: a Vue frontend for the user workflows, a Quarkus REST backend for application rules and integrations, and PostgreSQL persistence managed through Flyway migrations.

## Main functionality

- Local email/password registration, login, logout, protected routes, and user-owned data
- Dashboard overview with library, profile, candidate, and match signals
- External search, URL resolution where supported, and import into the personal library
- Manual media creation and editing
- Media library, media detail, status, rating, favourite, and tag management
- Profile readiness, interest weights, contributions, and explanatory signals
- Candidate list with completeness and match-readiness information
- Swipe-style candidate decisions with keyboard, button, and gesture controls
- Matches with overlap explanations, score display, and suppressed-score explanations when comparison is not meaningful
- German/English interface language switching
- Dark, light, and system theme preferences

## Component architecture

MoodMatch is organized as a monorepo with a clear frontend/backend boundary:

- **Frontend components and views:** Vue views compose reusable cards, forms, tag chips, artwork, score, profile, and swipe components. API modules and typed DTOs keep HTTP concerns separate from presentation.
- **Backend resources and services:** Quarkus REST resources expose the API; services own validation, user ownership, profile calculation, candidate selection, scoring, and import orchestration.
- **Repositories and persistence:** Panache repositories access PostgreSQL entities. Flyway migrations in `backend/src/main/resources/db/migration/` define the database evolution.
- **External provider layer:** provider gateways and normalized adapter interfaces isolate TMDB, Open Library, RAWG, AniList, Podcast Index, LibriVox, YouTube, and the offline DEMO provider from business logic.
- **Matching/scoring service:** interest-profile and matching services calculate deterministic weighted tag overlap and return explanations.
- **Auth and ownership:** local password auth resolves the current user; backend queries scope private media and matching data to that user.
- **Tests and QA:** frontend Vitest tests, backend Quarkus tests, lint/build checks, helper scripts, and manual browser QA cover the main grading workflows.

### Architecture overview

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
Application services and provider adapters
       /                         \\
      v                           v
Panache repositories        External provider gateways
      |
      v
PostgreSQL + Flyway migrations
```

The REST API is the integration boundary between frontend and backend. Business rules stay in backend services, persistence is isolated behind repositories, provider access is abstracted behind gateways, and PostgreSQL is the normal local datastore.

## Important backend components

- **Auth/local password flow:** `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, and `/api/auth/logout` manage local accounts and opaque bearer sessions for local/demo use.
- **User ownership:** `AppUser`, `CurrentUserProvider`, auth filters, and user-scoped repositories prevent one account from reading or changing another account's media and matching data.
- **Media management:** `MediaResource` and `MediaService` handle manual CRUD, statuses, favourites, ratings, and confirmed tags.
- **External search/import:** `ExternalSearchResource`, `ExternalSearchService`, `ExternalImportService`, and provider adapters normalize search results and turn selected results into local media.
- **Tag normalization:** external metadata is mapped to local tag categories and normalized before it is presented or stored.
- **Interest profile:** `InterestProfileService` calculates weighted contributions from consumed, rated, tagged media and reports readiness.
- **Candidates and matching:** `CandidateService` selects `WANT_TO_CONSUME` media; `MatchingService` calculates deterministic overlap, relative scores, and explanations.
- **Database evolution:** Flyway migrations create the schema, starter tags, ownership/auth support, and external-reference model.

## Important frontend components

- `AppShell`, navigation, theme preference, language preference, and auth store provide the application shell.
- Dashboard, External Search, Media Library, Media Detail/Form, Profile, Candidates, Swipe, and Matches views implement the main workflows.
- Shared `MediaCard`, `MediaArtwork`, `TagChip`, `MatchScoreDisplay`, `MatchExplanation`, and swipe components keep repeated UI behavior reusable.
- Artwork fallback keeps cards usable when external providers do not return an image.

## Technology stack

- Java 21, Quarkus, Hibernate ORM Panache, REST/JSON
- PostgreSQL for local development, H2 for backend tests, Flyway for migrations
- Maven and the Maven wrapper
- Vue 3, Vite, TypeScript, Pinia, Vue Router, Vue I18n
- Vitest, ESLint, Vue Test Utils, and `vue-tsc`
- External provider APIs behind backend-only gateways
- GitHub Actions CI for frontend lint/test/build and backend tests

## External data sources and providers

| Provider | Media category | API key | Important limitation |
| --- | --- | --- | --- |
| TMDB | Films and series | Required | Quota, network, and metadata quality depend on TMDB configuration. |
| Open Library | Books | Not required for the current public search integration | Public metadata quality varies. |
| RAWG | Games | Required | Used for this non-commercial university prototype; availability and terms should be reviewed before production use. |
| AniList | Anime/manga mapped to existing film, series, or book types | Not required | Results are mapped into MoodMatch's existing media types. |
| Podcast Index | Podcast shows/feeds | Key and secret required | Relevance and provider availability can vary. |
| LibriVox | Public-domain audiobooks | Not required | Catalog coverage is limited to public-domain audiobooks. |
| YouTube Data API | Videos and URL imports | Required | Search quota and provider metadata affect availability. |
| DEMO | Offline fallback across media types | Not required | Uses deterministic demo data and fallback artwork; it is useful for tests and local demos. |

Provider keys are backend-only. Missing optional keys should not prevent backend tests or core local development.

## Setup and installation

### Prerequisites

- Java 21
- Maven (or use the included Maven wrapper)
- Node.js and npm
- PostgreSQL
- `curl` for the demo-user helper

### Local run

```bash
git clone <github-repository-url>
cd moodmatch

cp .env.local.example .env.local
cp frontend/.env.local.example frontend/.env.local
```

Create a local PostgreSQL database and user, then put the local JDBC URL, username, and password in `.env.local`. Keep all provider keys optional unless you want to exercise the corresponding live provider. The normal local auth mode is `MOODMATCH_AUTH_MODE=local-password`.

In separate terminals:

```bash
# Terminal 1
cd backend
./scripts/dev-local.sh

# Terminal 2
cd frontend
npm install
npm run dev

# Terminal 3, from the repository root
./scripts/dev-create-demo-user.sh
```

Open `http://localhost:5173` and sign in with the local demo account values from `.env.local`. The backend runs at `http://localhost:8080`; Flyway applies migrations during startup. `backend/.env.example` documents backend variables, but `backend/scripts/dev-local.sh` intentionally loads the root `.env.local`.

### Useful commands

```bash
cd frontend && npm run lint && npm run test && npm run build
cd backend && ./scripts/test-clean.sh
```

The clean backend script deliberately does not load `.env.local`, so tests do not depend on PostgreSQL credentials or live provider keys. `scripts/dev-create-demo-user.sh` registers or verifies the local QA account through the normal API; it does not seed a bypass user.

## Testing

Frontend checks cover typed API/config helpers, auth and route protection, dashboard, search/import UI, media flows, profile/matches, swipe behavior, themes, language switching, and reusable components.

Backend checks cover REST resources, services, ownership isolation, local auth, mappers, Flyway starter tags, profile readiness, deterministic matching, and provider adapters. The backend test profile uses H2 in PostgreSQL compatibility mode and committed migrations, so CI does not need a running database or provider credentials.

Run the expected submission checks:

```bash
cd frontend && npm run lint && npm run test && npm run build
cd backend && ./scripts/test-clean.sh
```

See [docs/testing-strategy.md](docs/testing-strategy.md) for coverage focus and [docs/api-contract.md](docs/api-contract.md) for the implemented REST surface.

## Project structure

```text
backend/                 Quarkus API, services, providers, entities, tests, migrations
frontend/                Vue 3/Vite application, components, views, API client, tests
docs/                    Current reference docs, API contract, ADRs, and labelled history
scripts/                 Repository-level local QA helpers
.github/workflows/       GitHub Actions CI
.env.local.example       Root local backend/demo configuration placeholders
backend/.env.example     Backend variable reference placeholders
frontend/.env*.example   Frontend local configuration placeholders
```

Generated output (`node_modules`, `frontend/dist`, `backend/target`), IDE files, OS files, local environment files, and agent context are ignored and are not part of the source submission.

## Known limitations

- External provider quality, quotas, rate limits, network access, and configuration affect live search.
- Some providers require API keys; the DEMO provider and artwork fallback keep local testing usable without them.
- Missing artwork uses a deterministic fallback rather than guaranteeing provider imagery.
- Local demo data can include manually created media and is not a production seed dataset.
- Local-password auth is intended for local/demo use; production deployment would need stronger session, transport, account-recovery, and secret-management hardening.
- The repository does not guarantee a public deployment or hosted database.
- Deterministic matching is explainable by design, but it is not a learned recommendation model.
- Swipe decisions are designed for the current local decision flow; persistent recommendation feedback is future work.

## Future work

- Production deployment, hosted database, environment separation, and CI/CD deployment
- OAuth/full account management, secure cookies, account recovery, and broader security hardening
- Richer artwork resolution and caching with more provider backups
- Stronger recommendation learning and relevance tuning
- Better demo-data reset/admin tooling
- Browser-level end-to-end tests and more operational observability

See [docs/future-roadmap.md](docs/future-roadmap.md) for the distinction between current limitations and future enhancements.

## Grading criteria alignment

| Criterion | Evidence in MoodMatch |
| --- | --- |
| Komponentenarchitektur & Softwaredesign | Vue component/view separation, Quarkus resources/services/repositories, provider abstraction, DTOs, and Flyway persistence boundary. |
| Umsetzung & Vollständigkeit | Auth, dashboard, library, external import, profile, candidates, swipe, matches, explanations, themes, and languages are implemented as the core workflow. |
| Codequalität & Best Practices | Typed frontend API layer, normalized provider DTOs, service-layer rules, user ownership checks, reusable UI components, and explicit error handling. |
| Testing | Frontend lint/test/build plus clean backend tests for resources, services, auth, ownership, matching, migrations, and providers. |
| Dokumentation | This README, current architecture/setup/API/scoring/provider/testing docs, and clearly labelled historical records. |
| Abschlusspräsentation & Live-Demo | The suggested demo and code walkthrough below cover the end-to-end user journey and the most important architectural decisions. |

## Suggested demo flow

1. Login with the local demo account.
2. Show the Dashboard overview.
3. Search/import a title from External Search.
4. Open the Media Library and a media detail page.
5. Show Profile readiness and interest signals.
6. Inspect Candidates and their completeness.
7. Use Swipe to make decisions.
8. Open Matches and explain the score/overlap or readiness explanation.

Suggested code walkthrough:

- `backend/src/main/java/com/moodmatch/service/MatchingService.java` for deterministic scoring and explanations
- `backend/src/main/java/com/moodmatch/external/adapter/` for the provider abstraction and normalized results
- `frontend/src/views/SwipeView.vue`, `frontend/src/views/MatchesView.vue`, and `frontend/src/views/ProfileView.vue` for the main interactive frontend flows
- `backend/src/main/resources/db/migration/` and the entity/repository packages for the persistence model and evolution

## Submission information

- GitHub repository: `<add repository link>`
- Live application: `Not deployed yet` or `<add live application link>`
- Presentation files are submitted separately.
- This README and the linked `docs/` files serve as the implementation documentation.

## Repository hygiene

Do not commit `.env.local`, `frontend/.env.local`, real provider credentials, personal passwords, bearer tokens, generated build output, or IDE metadata. Placeholder examples are intentionally committed so a grader can understand the required configuration without receiving secrets.
