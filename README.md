# MoodMatch

MoodMatch is a university-project prototype for explainable media interest matching. It helps users collect titles they know, import new ones from external sources, build a taste profile from confirmed preferences, and compare what to watch, read, or play next.

## What Problem MoodMatch Solves

Many recommendation tools feel opaque, single-provider-bound, or focused only on one media category. MoodMatch treats films, series, books, games, audiobooks, podcasts, and videos as part of one personal interest space, then shows why a candidate fits instead of only returning a black-box ranking.

## Core User Workflow

1. Log in with a local email/password account for development or demo use.
2. Create media manually or import titles from supported external providers.
3. Confirm tags, ratings, and statuses so MoodMatch can build a meaningful profile.
4. Inspect candidates and match explanations in list-based views.
5. Use the swipe flow for quick local decisions.
6. Review matches, profile readiness, and explanation details.

## Features

- Local email/password authentication for prototype and demo use
- Protected routes for app areas such as dashboard, profile, media, candidates, swipe, matches, and external search
- Responsive Vue app shell
- Dark, light, and system theme support
- German and English UI language switching
- Media library with create, edit, status, rating, and tag flows
- Manual media creation
- External search, URL resolve, and import flows
- Candidate comparison and deterministic match explanations
- Swipe-style quick decision flow
- Profile readiness, signals, and interest-weight views
- Local development helper scripts
- Demo-user helper script for QA
- Frontend and backend automated test coverage
- Backend provider integrations with warning/fallback behavior

## Screenshots

Screenshots are optional for the source submission. Current desktop and mobile captures can be added later for a public showcase or deployment page.

## Tech Stack

### Frontend

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Vue I18n
- Vitest
- ESLint

### Backend

- Quarkus
- Java 21
- Maven
- Hibernate ORM Panache
- Flyway

### Database

- PostgreSQL for local development
- H2 for backend tests

## Architecture Overview

### Frontend

The `frontend/` app is a Vue 3 single-page application. It owns the UI flows for authentication, media management, external search/import, profile inspection, candidates, swipe decisions, and match explanations. Local development uses Vite on `http://localhost:5173`.

### Backend

The `backend/` service is a Quarkus REST API under `/api`. It owns validation, persistence, local auth flows, Flyway migrations, provider integrations, deterministic profile building, candidate loading, and match scoring/explanations.

### Database

PostgreSQL is the normal local-development datastore. Flyway applies the committed schema migrations on backend startup. Backend tests use H2 in PostgreSQL-compatibility mode so CI and local tests do not require a running PostgreSQL instance.

### Provider Integrations

External providers are backend-only integrations. The frontend never stores or calls provider secrets directly. Search/import responses are normalized into MoodMatch DTOs before they reach the UI, and imported titles become ordinary user-owned media items in the local library.

## Provider/API Overview

| Provider | Media type(s) | Needs API key? | Role in app | Notes / limitations |
| --- | --- | --- | --- | --- |
| `DEMO` | Film, series, book, game, audiobook, podcast, video | No | Offline fallback and local demo/test source | Uses placeholder or styled artwork rather than real provider assets |
| `TMDB` | Film, series | Yes | Real search/import source for films and series | Availability depends on API key, quota, network, and provider response quality |
| `OPEN_LIBRARY` | Book | No | Real search/import source for books | Public metadata quality can vary |
| `LIBRIVOX` | Audiobook | No | Real search/import source for public-domain audiobooks | Some searches can return handled provider-side technical errors |
| `RAWG` | Game | Yes | Real search/import source for games | Prototype/non-commercial use only; depends on API key and provider availability |
| `ANILIST` | Anime/manga mapped into film, series, or book | No | Supplements automatic search/import without adding new core media types | AniList results are mapped into existing media types rather than separate anime/manga enums |
| `PODCAST_INDEX` | Podcast | Yes | Real search/import source for podcast shows/feeds | Relevance can vary; podcast episodes are not imported as separate items |
| `YOUTUBE` | Video | Yes | Real query-search and URL-import source for videos | Titles may preserve provider HTML entities because external metadata is kept as provider data |

## Local Setup Quick Start

1. Copy the committed env examples:

```bash
cp .env.local.example .env.local
cp frontend/.env.local.example frontend/.env.local
```

2. Fill local-only secrets and database credentials in `.env.local`.
3. Start PostgreSQL.
4. Start the backend:

```bash
cd backend
./scripts/dev-local.sh
```

5. Start the frontend:

```bash
cd frontend
npm run dev
```

6. Create or verify the demo user from the repo root:

```bash
./scripts/dev-create-demo-user.sh
```

The full setup guide lives in [docs/local-setup.md](docs/local-setup.md).

## Testing

Backend:

```bash
cd backend
./scripts/test-clean.sh
```

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run build
```

## Demo QA Flow

1. Start PostgreSQL, backend, and frontend locally.
2. Run `./scripts/dev-create-demo-user.sh`.
3. Log in with `MOODMATCH_DEMO_EMAIL` and `MOODMATCH_DEMO_PASSWORD` from `.env.local`.
4. Verify protected routes such as dashboard, profile, external search, media, candidates, swipe, and matches.
5. Check German/English switching.
6. Check dark, light, and system theme behavior.
7. Confirm responsive behavior on desktop and mobile widths.

## Scoring And Readiness

MoodMatch scoring is deterministic and rule/tag-based, not a generative AI model.

- The interest profile is built from consumed media rated `4` or `5` with confirmed local tags.
- Ratings and favourites increase tag weights, and tag category weights shape the final profile.
- A candidate becomes meaningful only when it has confirmed local tags that overlap with the current profile.
- Readiness stays limited until enough profile-relevant media exists; fresh imports often need status updates, tags, and signals before Swipe or Matches become useful.
- Relative comparison scores are intentionally suppressed when the comparison would be misleading.

More detail is documented in [docs/scoring-and-matching.md](docs/scoring-and-matching.md).

## Authentication Note

The current prototype uses local email/password authentication for development and demo use. Protected app endpoints require a bearer token in normal `local-password` usage. A real deployment would need stronger session/token handling, HTTPS-first deployment decisions, and broader production hardening.

## Known Limitations

- Provider availability depends on API keys, quota, network access, and third-party response quality.
- LibriVox can return handled technical provider errors for some searches.
- YouTube and some other provider titles may preserve provider HTML entities because external metadata is kept as source data.
- Podcast Index relevance can vary.
- DEMO artwork is intentionally placeholder/styled.
- Fresh imports may need tags, ratings, and readiness-building signals before Swipe and Matches are meaningful.
- Scoring is deterministic and explainable, not AI-generated.
- Swipe behavior is prototype-level and focused on local decision flow rather than a finished persistence model.
- Deployment is not configured as a default public environment in this repository.
- Large brand SVG assets are acceptable for the prototype but could be optimized later.
- Some historical planning/checkpoint docs still contain older package names, roadmaps, or assumptions.

## Future Work

- Production-ready deployment setup and environment separation
- Stronger auth/session hardening for non-local deployment
- End-to-end browser testing for critical user flows
- Better provider resilience, metadata cleanup, and search relevance tuning
- Improved import/readiness UX so new titles become match-ready faster
- Clear persisted save/like semantics beyond the current swipe prototype behavior
- Asset optimization and final public showcase material such as screenshots

See [docs/future-roadmap.md](docs/future-roadmap.md) for the current roadmap note.

## Repository Hygiene And Security

- Do not commit `.env.local`, `frontend/.env.local`, or real API keys.
- Keep provider secrets backend-side only.
- Use `git archive` when you need a clean source submission zip without local build artifacts or secrets.
- The committed `.env.local.example` files are placeholders only and are safe to keep in Git.

## Project Status

MoodMatch is a demo-ready local prototype for a university project. Core implementation and final QA are complete, local development is supported, and repository cleanup is focused on documentation and submission readiness. Public deployment is optional future work unless configured separately later.

## Documentation Index

- [docs/local-setup.md](docs/local-setup.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/api-contract.md](docs/api-contract.md)
- [docs/scoring-and-matching.md](docs/scoring-and-matching.md)
- [docs/external-media-sources.md](docs/external-media-sources.md)
- [docs/testing-strategy.md](docs/testing-strategy.md)
- [docs/README.md](docs/README.md)
