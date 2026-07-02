# Auth Provider Decision

MoodMatch no longer uses Google OAuth/OIDC as the active prototype authentication path.

Google OAuth was explored first, but it introduced external Google Cloud project/client fragility. The OAuth client/project was disabled, and that made the login path too brittle for the university prototype. The final prototype direction is an internal local email/password account system.

## Decision

- Use local email/password authentication for the prototype.
- Store users in MoodMatch `app_users`.
- Store password hashes only.
- Issue MoodMatch bearer tokens from the backend.
- Keep media/profile/candidates/matches scoped through the existing `AppUser` ownership model.
- Keep `local-demo` only as a development/test fallback where useful.

## Not In Scope

- Google OAuth
- Google Identity Services
- Google Cloud dependency
- email verification
- password reset
- TMDB/import/YouTube/deployment auth work

## Current Shape

Frontend:

- `/login` offers login and create-account forms.
- Successful login/register stores the returned bearer token in the auth store.
- API requests include `Authorization: Bearer <token>` when a token exists.
- Protected routes redirect signed-out users to `/login`.

Backend:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- Protected app endpoints resolve the current `AppUser` from the bearer token.

## Security Notes

- Passwords are never stored or returned in plaintext.
- Password hashes are never returned by the API.
- Login failure returns `401` without saying whether email or password was wrong.
- Duplicate email returns `409`.
- Tokens are prototype bearer sessions; use HTTPS and stronger session controls before real deployment.
