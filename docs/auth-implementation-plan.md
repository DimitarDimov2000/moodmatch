# MoodMatch Auth Implementation Plan

## 1. Purpose

This document defines the implementation plan for adding real authentication to MoodMatch after the user ownership foundation has already been implemented.

The project target is a deployable university prototype where several private users can access the app through a web link, log in, and see only their own media, profile, candidates, matches, and future imports.

## 2. Current Baseline

Already implemented:

* `app_users` table.
* `media_items.user_id`.
* `AppUser` entity.
* `AuthProvider` enum.
* `CurrentUserProvider` abstraction.
* `LocalDemoCurrentUserProvider`.
* User-scoped media service behavior.
* User-scoped profile, candidates, and matches.
* Backend tests for user ownership.
* Separate frontend/backend deployment decision.

Current limitation:

* Real login is not implemented yet.
* All local dev requests still resolve to the temporary Local Demo User.
* Protected endpoints do not yet require a real bearer token.
* Frontend does not yet have login/logout or auth headers.

## 3. Chosen Auth Direction

Chosen implementation direction:

* Google/OIDC first.
* Separate frontend/backend deployment.
* Frontend handles login.
* Frontend sends `Authorization: Bearer <token>` to backend.
* Backend validates the token.
* Backend resolves or creates `AppUser`.
* Backend services use `CurrentUserProvider` to scope all private data.

Not part of this phase:

* phone OTP
* custom email-code login
* username/password storage
* TMDB integration
* import flow
* mobile redesign
* deployment hosting setup

## 4. Recommended Provider Choice

Recommended first implementation:

* Google Identity Services directly.

Reasoning:

* It fits the prototype scope.
* It avoids password storage.
* It avoids SMS/phone complexity.
* It works with a Vue single-page frontend.
* It can be validated by the Quarkus backend through token verification.
* It keeps the project understandable for university review.

Managed providers such as Auth0, Clerk, Firebase Auth, or Supabase Auth can still be evaluated later if the team wants easier dashboards or more login options.

## 5. Backend Plan

The backend should add real bearer-token validation while keeping local demo mode for development and tests.

Expected backend changes:

* Add Quarkus OIDC/JWT bearer-token support.
* Add configuration for issuer, audience/client ID, and auth mode.
* Add an authenticated current-user provider, for example `OidcCurrentUserProvider`.
* Extract token claims:

    * subject
    * email
    * display name if available
    * avatar URL if available
* Map token identity to `AppUser`.
* Create the `AppUser` if it does not exist yet.
* Reuse existing `AppUser` if it already exists.
* Keep `LocalDemoCurrentUserProvider` for dev/test only.
* Ensure production/deployed mode requires real authentication.

The backend must not trust user IDs from the frontend.

## 6. Frontend Plan

The frontend should add:

* login UI
* logout UI
* current-user state
* protected route handling
* authenticated API calls with `Authorization: Bearer <token>`
* `401 Unauthorized` handling
* loading state while auth is initialized

Frontend environment variables:

* `VITE_GOOGLE_CLIENT_ID`
* `VITE_API_BASE_URL`
* `VITE_AUTH_PROVIDER`

Frontend must not contain backend secrets.

## 7. Environment Variables

Backend variables:

* `OIDC_ISSUER_URL`
* `OIDC_CLIENT_ID`
* `OIDC_AUDIENCE`
* `AUTH_MODE`
* `CORS_ALLOWED_ORIGINS`
* `FRONTEND_BASE_URL`

Possible backend secret if required by the chosen setup:

* `OIDC_CLIENT_SECRET`

Frontend variables:

* `VITE_GOOGLE_CLIENT_ID`
* `VITE_API_BASE_URL`
* `VITE_AUTH_PROVIDER`

Rules:

* No secrets in Git.
* No backend secrets in frontend code.
* Frontend variables are public after build.
* Deployment hosts must store real secrets securely.
* Local development can use `.env.local` files that are ignored by Git.

## 8. Auth Modes

Recommended auth modes:

* `local-demo`
* `oidc`

`local-demo`:

* Used only for local development and tests.
* Resolves every request to the Local Demo User.
* Must not be used in production deployment.

`oidc`:

* Used for deployed or real authenticated mode.
* Requires valid bearer token.
* Resolves or creates `AppUser` from token claims.

## 9. Backend Endpoint Protection

Protected endpoints should include:

* `/api/media`
* `/api/profile`
* `/api/candidates`
* `/api/matches`
* `/api/external/search`
* future `/api/external/import`
* future `/api/swipe-decisions`

Public endpoints:

* health endpoint
* possibly static/public app metadata

External search should be protected because provider usage and future imports are user-facing features.

## 10. CORS Plan

Because frontend and backend are deployed separately, the backend must allow the deployed frontend origin.

Required behavior:

* Local frontend origin allowed in dev, for example `http://localhost:5173`.
* Deployed frontend origin allowed in production.
* Avoid wildcard CORS in production unless intentionally required.
* Allow `Authorization` header.

## 11. Testing Plan

Backend tests should cover:

* local demo mode still works in dev/test.
* unauthenticated request is rejected in OIDC mode.
* authenticated token resolves to an `AppUser`.
* first authenticated request creates an `AppUser`.
* repeated authenticated request reuses the same `AppUser`.
* user A cannot see user B's media.
* profile/candidates/matches remain user-scoped.

Frontend tests should cover:

* logged-out state.
* login button visible.
* API client adds bearer token when authenticated.
* `401` response handling.
* logout clears auth state.
* protected routes do not show private data before login.

E2E tests can be added later after auth is stable.

## 12. Implementation Order

Recommended implementation order:

1. Backend auth configuration and dependencies.
2. Backend auth mode configuration.
3. `OidcCurrentUserProvider`.
4. AppUser claim mapping logic.
5. Protected endpoint configuration.
6. Backend auth/user tests.
7. Frontend auth state module.
8. Frontend Google login UI.
9. API client bearer-token support.
10. Protected route behavior.
11. Frontend tests.
12. Manual local auth verification.
13. Documentation updates.

## 13. Manual Verification

After implementation:

Backend local demo mode should still work for development.

OIDC/auth mode should require a real token.

Manual checks:

* logged-out user cannot access private data.
* logged-in user can access own media.
* new Google user creates an `AppUser`.
* returning Google user reuses existing `AppUser`.
* user A cannot see user B's media.
* frontend includes bearer token in API calls.
* logout removes access to private data.

## 14. Risks

Main risks:

* confusing ID token and access token behavior.
* wrong token audience validation.
* CORS blocking frontend requests.
* frontend storing tokens unsafely.
* accidentally using local demo mode in production.
* exposing secrets in frontend env vars.
* breaking existing local dev flow.
* breaking tests that rely on local demo user.

Mitigation:

* Keep local demo mode explicit.
* Keep production auth mode strict.
* Keep secrets out of frontend.
* Add tests for auth and user isolation.
* Implement backend first, then frontend.
* Keep auth separate from TMDB/import work.

## 15. Recommended Next Phase

Recommended next phase:

* Phase 26: Implement backend auth foundation.

Phase 26 should implement backend authentication first, before frontend login UI.

It should not implement TMDB, import flow, mobile redesign, or deployment hosting.
