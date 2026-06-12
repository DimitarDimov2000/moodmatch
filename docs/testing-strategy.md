# Testing Strategy

MoodMatch relies on deterministic, explainable behavior, so the current checkpoint keeps both backend and frontend checks in place.

## Current Automated Coverage

### Backend

Current backend tests cover:

- REST resources
- media and tag services
- interest profile calculation
- deterministic matching
- DTO/entity mappers
- starter tag migration behavior
- DEMO external search provider behavior

The backend test suite uses Quarkus tests plus H2 in PostgreSQL compatibility mode and applies the committed Flyway migrations.

### Frontend

Current frontend tests cover:

- API client/config helpers
- dashboard view behavior
- external search view and result card
- media detail flow
- media form behavior
- match score display
- swipe view behavior
- tag utilities
- top-level app shell routing

These tests focus on current rendered behavior and route/API integration boundaries without changing the backend contract.

## Verification Commands

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend
npm run lint
npm run test
npm run build
```

Useful additional frontend check:

```bash
cd frontend
npm run typecheck
```

## What The Current Checkpoint Verifies

- Media CRUD remains stable
- Tag replacement continues to work
- Status and favourite rules stay enforced
- Flyway migrations remain valid
- Seeded starter tags stay deterministic
- Profile readiness and weighted contributions stay deterministic
- Match scoring and explanation states stay deterministic
- External DEMO preview stays normalized and offline
- Swipe mode semantics stay unchanged
- Frontend route-level views still build and render against the typed API layer

## Manual Checkpoint Smoke Test

For a presentation or final checkpoint pass:

1. Start PostgreSQL locally.
2. Start the backend with `./mvnw quarkus:dev`.
3. Start the frontend with `npm run dev`.
4. Open the dashboard and confirm the summary cards load.
5. Create or edit media, then confirm tags and status/favourite rules still behave as documented.
6. Open profile and matches to confirm profile readiness and score explanations.
7. Open swipe mode and verify local like/skip vs persistent reject.
8. Open external search and confirm DEMO preview results render without import behavior.

## Future Work

Possible future test expansion, not required for this checkpoint:

- dedicated end-to-end browser tests
- external import flow tests once import exists
- dedicated decision-mode filtering tests once a stable API/UI contract exists
