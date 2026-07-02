# Testing Strategy

MoodMatch relies on deterministic, explainable behavior, so the current checkpoint keeps both backend and frontend checks in place.

The current suite is appropriate for the project checkpoint, but it should not be described as full production-grade coverage.

## Current Automated Coverage

### Backend

Current backend tests cover:

- REST resources
- media and tag services
- interest profile calculation
- deterministic matching
- auth mode behavior for local-demo and local-password
- local registration/login/logout and bearer-token protection
- user-isolation behavior for media/profile/candidates/matches
- DTO/entity mappers
- starter tag migration behavior
- DEMO external search provider behavior

The backend test suite uses Quarkus tests plus H2 in PostgreSQL compatibility mode and applies the committed Flyway migrations.

### Frontend

Current frontend tests cover:

- API client/config helpers
- auth store defaults, `localStorage` persistence, restore verification, and login/logout state transitions
- protected-route guard behavior in `local-demo` vs `local-password`, including pending restore timing
- login view and app-shell auth UI basics
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
- Current-user ownership rules prevent cross-user media access at the service layer
- Tag replacement continues to work
- Status and favourite rules stay enforced
- Flyway migrations remain valid
- Seeded starter tags stay deterministic
- Profile readiness and weighted contributions stay deterministic
- Match scoring and explanation states stay deterministic
- Profile, candidate, and match calculations stay scoped to the resolved current user
- External DEMO preview stays normalized and offline
- Local demo auth mode still resolves the demo user in backend tests
- Local-password mode rejects missing/invalid tokens on protected endpoints
- Local-password tokens resolve the backing `AppUser`
- Registration rejects duplicate email and login rejects wrong passwords
- Swipe mode semantics stay unchanged
- Frontend route-level views still build and render against the typed API layer
- Frontend auth mode state, bearer-header attachment, and route protection remain wired correctly without Google credentials
- Prototype refresh persistence keeps local email/password sessions usable during demos while still clearing invalid stored tokens on startup verification

## Prototype Session Note

For the university prototype, frontend auth persistence uses `localStorage` so refreshes keep the demo session alive until backend verification fails.

Production should replace this with secure HTTP-only cookies rather than leaving bearer tokens readable from browser JavaScript.

## Manual Checkpoint Smoke Test

For a presentation or final checkpoint pass:

1. Start PostgreSQL locally.
2. Start the backend with `./mvnw quarkus:dev`.
3. Start the frontend with `npm run dev`.
4. Create a MoodMatch account with email/password.
5. Log out and log back in with the same account.
6. Refresh the browser and confirm the session is restored without a login/dashboard flicker.
7. Open the dashboard and confirm the summary cards load.
8. Create or edit media, then confirm tags and status/favourite rules still behave as documented.
9. Open profile and matches to confirm profile readiness and score explanations.
10. Open swipe mode and verify local like/skip vs persistent reject.
11. Open external search and confirm DEMO preview results render without import behavior.

## Future Work

Possible future test expansion, not required for this checkpoint:

- dedicated end-to-end browser tests
- external import flow tests once import exists
- dedicated decision-mode filtering tests once a stable API/UI contract exists

End-to-end/browser coverage remains future work and is tracked as deferred roadmap work rather than current test coverage.
