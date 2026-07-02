# Auth Implementation Plan

This document now reflects the implemented local email/password direction.

## Implemented Direction

MoodMatch uses local account authentication for the university prototype:

- users register with email and password
- users log in with email and password
- backend returns a bearer token and safe user info
- frontend stores the token in the auth store
- protected API requests send `Authorization: Bearer <token>`
- existing `AppUser` ownership remains the source of truth for private media/profile/candidates/matches

Google OAuth/OIDC is not the normal local path anymore. It was explored, but disabled Google Cloud project/client state made it too fragile for the prototype.

## Backend Pieces

- `V4__add_local_password_auth.sql`
- `password_hash` and `last_login_at` on `app_users`
- `auth_sessions` for hashed opaque bearer tokens
- local password hashing service
- local auth token service
- request filter for protected local-password endpoints
- `LocalPasswordCurrentUserProvider`
- `AuthResource` under `/api/auth`

## Frontend Pieces

- local auth API client
- Pinia auth store login/register/logout actions
- login/create-account UI in `LoginView`
- protected route behavior based on local-password token state
- bearer header attachment in the shared API client

## Deferred

- email verification
- password reset
- deployment/session hardening
- external import auth concerns
- removing old inactive OIDC reference code if the team wants a smaller codebase later
