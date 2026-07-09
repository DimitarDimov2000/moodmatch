# Testing strategy

MoodMatch uses automated frontend and backend checks around deterministic, explainable behavior. The suite supports the university submission and local demo; it is not presented as production-grade end-to-end coverage.

## Automated coverage

### Backend

Backend tests cover REST resources, media and tag services, interest profile calculation, deterministic matching, local-demo and local-password auth modes, registration/login/logout and bearer-token protection, user-isolation behavior, DTO/entity mappers, starter-tag migrations, and external provider adapters.

The suite uses Quarkus tests with H2 in PostgreSQL compatibility mode and the committed Flyway migrations. It does not require a running PostgreSQL instance, live provider APIs, or real credentials.

### Frontend

Frontend tests cover API client/config helpers, auth state and route protection, dashboard, external search/result cards, media detail/form flows, match score display, swipe behavior, tags, theme and language preferences, application routing, and key shared components.

## Verification commands

Run the expected submission checks:

```bash
cd frontend && npm run lint && npm run test && npm run build
cd backend && ./scripts/test-clean.sh
```

The clean backend helper deliberately does not load `.env.local`; it unsets local database, auth, CORS, OIDC, and provider variables before running `./mvnw test`.

Optional frontend type-only check:

```bash
cd frontend
npm run typecheck
```

## What the checks protect

- media CRUD, tag replacement, status, rating, and favourite validation
- user ownership and protected endpoint behavior
- Flyway migration validity and starter tags
- profile readiness, weighted contributions, matching, score suppression, and explanations
- normalized external provider behavior and missing-key fallbacks
- frontend route/view behavior against the typed API layer
- local-password token attachment, refresh restore, logout, and invalid-token cleanup
- swipe interaction and responsive presentation states

## Manual demo smoke test

1. Start PostgreSQL, backend, and frontend using [local-setup.md](local-setup.md).
2. Create or verify a local demo account.
3. Log in, refresh, and confirm the protected app remains available.
4. Open Dashboard, Media Library, Profile, Candidates, Swipe, and Matches.
5. Create or import media, then add tags, status, rating, and favourite signals.
6. Confirm readiness and match explanations update as expected.
7. Check external search with a configured provider or the DEMO fallback.
8. Switch German/English and dark/light/system theme preferences.
9. Check desktop and mobile viewport behavior.

Browser-level E2E automation, hosted deployment checks, and live-provider reliability monitoring remain future work; they are separated from the current unit/resource/service test scope in [future-roadmap.md](future-roadmap.md).
