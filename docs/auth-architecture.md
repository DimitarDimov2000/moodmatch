# MoodMatch Authentication Architecture

## 1. Purpose

MoodMatch is moving from a local single-user MVP toward a deployable university prototype for several private daily users.

Authentication is required so that each user can access MoodMatch through a web link, log in, and see only their own media library, profile, matches, candidates, and swipe state.

This document defines the authentication direction before implementation.

## 2. Current Baseline

The current app has:

- Quarkus backend.
- Vue 3/Vite frontend.
- PostgreSQL dev database.
- Flyway migrations.
- Media CRUD.
- Tags.
- Profile, candidates, matches, and swipe mode.
- Offline demo external search provider.
- Environment-variable based database credentials.
- Backend and frontend tests.

Current limitations:

- No login yet.
- No local user model yet.
- Media items are not user-owned yet.
- Profile and matches are not user-scoped yet.
- Swipe likes/skips are not persistently user-scoped yet.
- External imports do not exist yet.

## 3. Target

The target is a deployable university prototype, not a full production consumer app.

Users should be able to:

- Open MoodMatch through a deployed web link.
- Log in through email/OIDC/Google.
- Manage their own private media library.
- Receive their own profile and match calculations.
- Swipe through their own candidates.
- Import external media into their own account later.

## 4. Chosen Authentication Direction

Recommended direction:

- Use OIDC / Google-first authentication.
- Do not implement phone OTP first.
- Do not implement custom email-code login first.
- Do not build custom password storage.

Reasoning:

- OIDC is a standard authentication layer on top of OAuth 2.0.
- Google/OIDC login is realistic for a university prototype.
- Quarkus has official OIDC support.
- Custom OTP requires extra security work such as expiration, hashing, resend limits, brute-force protection, rate limiting, and email/SMS provider setup.
- Phone login introduces SMS cost and abuse risk.

## 5. Preferred Implementation Shape

Preferred approach:

- Backend-managed OIDC login.
- The backend integrates with an OIDC provider.
- The provider authenticates the user.
- The backend resolves the authenticated user identity.
- Backend services use the current user identity to scope all private data.

Alternative approach:

- Vue frontend handles login.
- Frontend sends bearer token to backend.
- Backend validates token for every protected API request.

Decision:

- Prefer backend-managed OIDC if deployment routing supports it cleanly.
- Use bearer-token SPA approach only if the deployment architecture makes backend-managed redirects difficult.

## 6. Internal User Model

Even with external login, MoodMatch needs an internal user record.

Proposed table:

```txt
app_users