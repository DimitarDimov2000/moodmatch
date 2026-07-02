# MoodMatch Auth Provider Decision

## 1. Purpose

MoodMatch is moving toward a deployable university prototype for several private daily users.

The project has already introduced the user ownership foundation:

* `app_users`
* `media_items.user_id`
* user-scoped media/profile/candidate/matching logic
* temporary local demo user resolution

This document decides the authentication provider direction and deployment authentication shape before implementation.

## 2. Deployment Shape

Chosen deployment shape:

* Separate frontend and backend deployment.

Planned architecture:

* Browser
* Deployed Vue frontend
* Deployed Quarkus backend API
* Hosted PostgreSQL database

This means the backend should behave like a protected API. The frontend and backend may run on different URLs.

## 3. Chosen Auth Flow

Chosen direction:

* Frontend login
* Backend bearer-token validation

Flow:

1. User opens the deployed Vue frontend.
2. User logs in through Google/OIDC.
3. Frontend receives a token from the auth provider.
4. Frontend sends API requests to the backend with an `Authorization: Bearer <token>` header.
5. Backend validates the token.
6. Backend resolves or creates the matching `AppUser`.
7. Backend services return only that user's private data.

## 4. Why This Flow Fits MoodMatch

This flow fits the project because:

* The frontend and backend will be deployed separately.
* The frontend is a Vue single-page application.
* The backend is a Quarkus REST API.
* The backend already has a `CurrentUserProvider` abstraction.
* The database already has `app_users` and `media_items.user_id`.
* User-owned data is already prepared internally.
* The frontend can handle login UI without backend redirect complexity.

## 5. Provider Direction

Recommended first provider:

* Google/OIDC first.

Not planned for the first implementation:

* phone OTP
* custom email-code login
* username/password storage
* custom password reset
* SMS login

Reasoning:

* Google/OIDC is realistic for a university prototype.
* Custom OTP requires extra security controls.
* Phone OTP introduces SMS cost and abuse risk.
* MoodMatch should not store user passwords.
* User identity should come from a trusted external provider.

## 6. Provider Options Considered

### Option A: Google Identity Services directly

Possible approach:

* Vue frontend uses Google login.
* Backend validates the Google/OIDC token.
* Backend maps the token subject to `AppUser`.

Pros:

* Direct Google login.
* Good for prototype scope.
* No custom password storage.
* Clear user identity source.

Cons:

* Requires Google Cloud/client setup.
* Requires correct frontend origin configuration.
* Requires backend token validation configuration.
* Requires careful frontend token handling.

### Option B: Managed auth provider

Examples:

* Auth0
* Clerk
* Firebase Auth
* Supabase Auth

Pros:

* Often easier login UI.
* Can support more login methods.
* Helpful dashboards and user management.

Cons:

* Extra third-party dependency.
* Provider-specific SDKs.
* Free-tier limits.
* More vendor-specific setup.

### Option C: Self-hosted Keycloak

Pros:

* Strong OIDC story.
* Self-hostable.
* Professional architecture.

Cons:

* More infrastructure.
* More setup than needed for the prototype.
* Harder deployment.

## 7. Decision

Decision for now:

* Use Google/OIDC-first bearer-token architecture.

Implementation can start with either:

1. Google Identity Services directly.
2. A managed provider if the team decides it is easier.

The backend design should stay provider-neutral where possible:

* Use OIDC subject as stable identity.
* Store `provider` and `provider_subject` in `app_users`.
* Keep `CurrentUserProvider` as the backend boundary.
* Do not hardcode frontend user IDs into API requests.
* Do not let the frontend decide data ownership.

## 8. Backend Design

Future real-auth implementation should replace or extend `LocalDemoCurrentUserProvider` with something like `OidcCurrentUserProvider`.

Responsibilities:

* Read the authenticated token identity.
* Extract provider.
* Extract provider subject.
* Extract email if available.
* Extract display name if available.
* Extract avatar URL if available.
* Find or create the matching `AppUser`.
* Return the current `AppUser` to backend services.

Important rule:

* The backend must not trust user IDs sent by the frontend.
* The authenticated token determines the current user.

## 9. Frontend Design

The frontend will need:

* login page or login component
* logout action
* current-user state
* route protection
* unauthorized/session-expired handling
* API client support for the `Authorization` header

The frontend must not store provider client secrets.

Frontend environment variables are public after build, so they may contain public values like a Google client ID, but never backend secrets.

## 10. API Client Changes

Current frontend API calls do not include auth headers.

Future API client behavior:

* If authenticated, include `Authorization: Bearer <token>`.
* If not authenticated, do not call protected endpoints or handle `401 Unauthorized` responses.

Backend protected endpoints will eventually return `401 Unauthorized` if no valid token is present.

## 11. Backend Endpoint Security

Eventually protected endpoints should include:

* `/api/media`
* `/api/profile`
* `/api/candidates`
* `/api/matches`
* future `/api/external/import`
* future `/api/swipe-decisions`

Potential public endpoints:

* health endpoint
* login/auth-related metadata if needed
* static/public app information if needed

External search should probably require authentication because provider usage and future imports are user-facing features.

## 12. Environment Variables

Expected future backend variables:

* `OIDC_ISSUER_URL`
* `OIDC_CLIENT_ID`
* `OIDC_AUDIENCE`
* `CORS_ALLOWED_ORIGINS`
* `FRONTEND_BASE_URL`

If the backend also requires a provider secret:

* `OIDC_CLIENT_SECRET`

Expected future frontend variables:

* `VITE_AUTH_PROVIDER`
* `VITE_GOOGLE_CLIENT_ID`
* `VITE_API_BASE_URL`

Rules:

* No secrets in Git.
* No backend secrets in frontend env vars.
* Frontend variables are public after build.
* Deployment platforms must store real secrets/environment variables securely.
* Local development should use local environment variables or IntelliJ run configuration variables.

## 13. Local Development Strategy

Until real auth is implemented, local development can continue using the temporary local demo user.

Possible transition:

* Dev profile may allow local demo user mode.
* Test profile may use test current-user provider.
* Production/deployed profile must require real authentication.

Important rule:

* Local demo user mode must not be used as production authentication.

## 14. Testing Strategy

Backend tests should eventually cover:

* unauthenticated request rejected
* authenticated request accepted
* token subject maps to correct `AppUser`
* new user is created on first authenticated access
* returning user is reused
* user A cannot access user B data
* profile/candidates/matches remain user-scoped

Frontend tests should eventually cover:

* logged-out state
* login button visible
* authenticated API calls include token
* `401` handling
* logout clears user state
* protected routes redirect or show login state

E2E tests should be added after auth and UI stabilize.

## 15. Deployment Considerations

Separate deployment requires:

* correct backend CORS configuration
* frontend URL allowed by backend
* auth provider configured with frontend origin
* backend configured with issuer/audience/client ID
* HTTPS in deployed environment
* environment variables configured on both frontend and backend hosts

## 16. Risks

Main risks:

* frontend token handling mistakes
* CORS misconfiguration
* wrong token audience validation
* relying on email instead of provider subject
* exposing secrets in frontend
* using local demo user in production
* auth provider free-tier limitations
* deployment origin mismatch

Mitigation:

* Keep `provider + provider_subject` as the identity key.
* Use backend token validation.
* Store secrets only in backend/deployment env vars.
* Keep local demo mode limited to dev/test.
* Add auth tests before deployment.
* Document provider setup clearly.

## 17. Recommended Next Phase

Recommended next phase:

* Phase 25: Auth implementation preparation.

Before writing full auth code, decide:

* Google Identity Services directly or managed provider.
* Exact frontend package/API.
* Exact backend Quarkus OIDC configuration.
* Exact environment variable names.
* How local demo user mode is enabled/disabled.
* What tests will be written first.

After that, implement real authentication in a controlled phase.
