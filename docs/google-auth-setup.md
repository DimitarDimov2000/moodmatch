# Google/OIDC Setup Guide

## 1. Purpose

This guide explains how to activate real Google/OIDC authentication in MoodMatch based on the auth foundation that already exists in the current codebase.

- MoodMatch already includes backend and frontend auth foundations.
- This document focuses on activating real Google/OIDC login for local testing and later deployment preparation.
- The existing `local-demo` mode remains available for normal local development, presentations, and most day-to-day feature work.

## 2. Current Implemented Baseline

### Backend

The backend already includes the current auth foundation:

- `local-demo` mode and `oidc` mode are both supported.
- A mode-aware current-user provider switches between local demo behavior and OIDC behavior.
- In `oidc` mode, the backend expects a bearer token on protected endpoints.
- The backend can resolve or create an `AppUser` from the authenticated identity.
- The backend uses the provider subject as the stable identity key and can also sync optional profile data such as email, display name, and avatar URL.
- Real Google credentials are not committed in the repository.

### Frontend

The frontend already includes the current auth foundation:

- an auth store with login/logout state
- a `LoginView` for auth-related UI
- a Google Identity Services provider boundary under `frontend/src/auth/`
- protected-route behavior for `oidc` mode
- bearer-token API support that sends `Authorization: Bearer <token>` when a token exists

Current frontend behavior:

- the Google credential token is stored in memory only
- `local-demo` mode keeps the app accessible without Google setup
- real Google client IDs are expected from local or deployed environment variables, not from committed source files

## 3. Google Account / Project Setup

Use a dedicated MoodMatch project email or Google account if your team wants shared ownership of the prototype setup. This is optional, but it can make later handoff easier.

### Create or reuse a Google Cloud project

1. Open Google Cloud Console.
2. Create a new project for MoodMatch or reuse an existing one.
3. Open the Google Auth Platform / OAuth setup area for that project.

### Configure OAuth branding / consent

Set up the basic OAuth branding or consent information for the project.

Typical items include:

- application name
- support email
- branding details

For local development, keep this lightweight. You do not need to treat full public publishing as a blocker for local activation.

### Create an OAuth 2.0 client ID

Create an OAuth 2.0 client ID with:

- Application type: `Web application`

Google requires a client ID before Sign in with Google can be configured.

### Authorized JavaScript origins for local development

Add these local origins:

- `http://localhost`
- `http://localhost:5173`

Later, also add the real deployed frontend origin.

### Redirect URI note

The current MoodMatch frontend foundation uses a callback-style Google Identity Services flow. In that setup, redirect URIs may not be needed.

Only add redirect URIs if your team later switches to a redirect-based flow.

### Deployed-use note

For a deployed prototype, Google OAuth branding or consent may require some additional information depending on the app publishing state, for example:

- support email
- app name
- authorized domain
- homepage URL
- privacy-policy URL

Treat these as later deployment checklist items, not as blockers for local development.

## 4. Frontend Local Environment Setup

Create a local file at:

- `frontend/.env.local`

Do not commit this file.

Example placeholder values:

```bash
VITE_AUTH_MODE=oidc
VITE_AUTH_PROVIDER=google
VITE_GOOGLE_CLIENT_ID=replace-with-google-client-id
VITE_API_BASE_URL=http://localhost:8080
```

Important rules:

- `VITE_` variables are public in frontend builds.
- Frontend environment variables must not contain secrets.
- Do not place a Google client secret in the frontend.
- Use placeholder values in documentation and shared screenshots.

## 5. Backend Local Environment Setup

### Current local-demo mode

In the current codebase:

- the base/default backend configuration is `oidc`
- the `%dev` profile defaults to `local-demo`
- the `%dev` profile also defaults to OIDC disabled and protected endpoints permitted

This means normal local development still works without Google credentials unless you deliberately activate real OIDC mode.

### Real local OIDC mode

For real local Google/OIDC testing, use the actual current environment variable names from `backend/src/main/resources/application.properties`.

Database variables:

```bash
MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch
MOODMATCH_DB_USERNAME=replace-with-local-database-user
MOODMATCH_DB_PASSWORD=replace-with-local-database-password
```

Auth variables:

```bash
MOODMATCH_AUTH_MODE=oidc
MOODMATCH_OIDC_ENABLED=true
MOODMATCH_PRIVATE_ENDPOINT_POLICY=authenticated
MOODMATCH_OIDC_ISSUER_URL=https://accounts.google.com
MOODMATCH_OIDC_CLIENT_ID=replace-with-google-client-id
MOODMATCH_OIDC_AUDIENCE=replace-with-google-client-id
```

Optional current variable:

```bash
MOODMATCH_OIDC_PROVIDER=GOOGLE
```

Notes:

- `MOODMATCH_OIDC_PROVIDER` is optional. If omitted, the current backend default is `OIDC`.
- Setting `MOODMATCH_OIDC_PROVIDER=GOOGLE` can be useful if you want the stored `app_users.provider` value to reflect Google explicitly.
- Keep any future backend-only secrets out of the frontend.

### Planning names vs current active names

Some planning documents mention names such as:

- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_BASE_URL`

These are useful deployment planning ideas, but they are not currently wired as active local startup variables in the checked-in `application.properties`.

For Phase 29 local setup, use the actual active backend variable names listed above.

## 6. Running `local-demo` Mode

`local-demo` mode is still the easiest way to run MoodMatch locally without Google credentials.

### Start PostgreSQL

Create a local database and user if needed:

```sql
CREATE USER moodmatch WITH PASSWORD 'replace-with-local-database-password';
CREATE DATABASE moodmatch OWNER moodmatch;
```

If your PostgreSQL setup requires a superuser session first:

```bash
psql postgres
```

### Set backend database variables

```bash
export MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch
export MOODMATCH_DB_USERNAME=moodmatch
export MOODMATCH_DB_PASSWORD=replace-with-local-database-password
```

### Keep local-demo auth mode

The current `%dev` profile already defaults to `local-demo`, but you can set it explicitly:

```bash
export MOODMATCH_AUTH_MODE=local-demo
export MOODMATCH_OIDC_ENABLED=false
export MOODMATCH_PRIVATE_ENDPOINT_POLICY=permit
```

### Start the backend

```bash
cd backend
./mvnw quarkus:dev
```

### Start the frontend

```bash
cd frontend
npm run dev
```

### Result

- open `http://localhost:5173`
- no Google client ID is required
- no Google login is required
- protected frontend routes remain available for local development

## 7. Running Local OIDC / Google Mode

Use this checklist when you want to test the real auth path locally.

### Activation checklist

- Start PostgreSQL locally.
- Set backend database variables:

```bash
export MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch
export MOODMATCH_DB_USERNAME=replace-with-local-database-user
export MOODMATCH_DB_PASSWORD=replace-with-local-database-password
```

- Set backend auth variables:

```bash
export MOODMATCH_AUTH_MODE=oidc
export MOODMATCH_OIDC_ENABLED=true
export MOODMATCH_PRIVATE_ENDPOINT_POLICY=authenticated
export MOODMATCH_OIDC_ISSUER_URL=https://accounts.google.com
export MOODMATCH_OIDC_CLIENT_ID=replace-with-google-client-id
export MOODMATCH_OIDC_AUDIENCE=replace-with-google-client-id
export MOODMATCH_OIDC_PROVIDER=GOOGLE
```

- Create `frontend/.env.local` with placeholder structure:

```bash
VITE_AUTH_MODE=oidc
VITE_AUTH_PROVIDER=google
VITE_GOOGLE_CLIENT_ID=replace-with-google-client-id
VITE_API_BASE_URL=http://localhost:8080
```

- Start the backend:

```bash
cd backend
./mvnw quarkus:dev
```

- Start the frontend:

```bash
cd frontend
npm run dev
```

- Open the frontend at `http://localhost:5173`.
- Open the login screen and start Google sign-in.
- Log in with a Google account allowed by your Google project setup.
- In the browser Network tab, confirm that protected API requests send an `Authorization` header with a bearer token.
- Confirm that the backend resolves the current `AppUser`.

### How to confirm `AppUser` resolution

The current API contract does not provide a dedicated "who am I" endpoint, so the simplest manual check is the database.

Example query:

```sql
SELECT id, provider, provider_subject, email, display_name
FROM app_users
ORDER BY updated_at DESC;
```

What to expect:

- the first authenticated request may create a new `app_users` row
- later authenticated requests for the same Google identity should reuse that row
- the stable identity key is the provider subject, not only the email address

## 8. Deployment Setup Later

When the prototype is deployed later, update the auth setup accordingly.

### Google side

- Add the deployed frontend URL to Google Authorized JavaScript origins.
- If a redirect-based flow is introduced later, add the matching redirect URIs too.
- For deployed or public use, complete any required OAuth branding or consent items such as support email, authorized domain, homepage URL, and privacy-policy URL.

### Backend side

- Store backend database and auth environment variables securely in the deployment environment.
- Do not use `local-demo` mode in production or deployment.
- The backend must allow the deployed frontend origin through CORS when CORS support is implemented or configured.

### Frontend host side

- Store `VITE_GOOGLE_CLIENT_ID` on the frontend host.
- Store `VITE_API_BASE_URL` on the frontend host.
- Remember that frontend `VITE_` variables are public values, not secrets.

## 9. Security Rules

- Do not commit secrets to Git.
- Do not commit `frontend/.env.local`.
- Do not put a client secret in the frontend.
- Do not paste credentials into documentation, issues, or screenshots.
- Use the provider subject as the stable user identity, not only email.
- Use HTTPS for deployed environments.
- The frontend sends the token.
- The backend validates the token.
- The backend maps the token subject to `AppUser`.
- The backend scopes private data by `AppUser`.

## 10. Troubleshooting

### Missing Google client ID

- Symptom: the login view says Google setup is still required.
- Check that `frontend/.env.local` contains `VITE_GOOGLE_CLIENT_ID=replace-with-google-client-id` replaced with a real local value.
- Restart the frontend dev server after changing environment variables.

### Origin mismatch

- Symptom: Google sign-in does not start or Google rejects the request.
- Check Google Authorized JavaScript origins.
- For local use, confirm both `http://localhost` and `http://localhost:5173` are configured.

### CORS blocked `Authorization` header

- Symptom: login appears to work, but protected API requests fail across different origins.
- This is mainly a later deployed-environment concern.
- When backend CORS support is implemented or configured, the deployed frontend origin must be allowed and the `Authorization` header must be accepted.

### Backend still running in `local-demo` mode

- Symptom: protected endpoints still behave like demo mode even though Google login was prepared.
- Check that `MOODMATCH_AUTH_MODE=oidc` is set.
- In the current dev profile, also check `MOODMATCH_OIDC_ENABLED=true` and `MOODMATCH_PRIVATE_ENDPOINT_POLICY=authenticated`.

### Backend running in `oidc` mode without token

- Symptom: protected API calls return `401 Unauthorized`.
- This is expected if the frontend has not completed login yet or no bearer token is being sent.
- Confirm the request includes `Authorization: Bearer <token>`.

### Wrong audience or client ID

- Symptom: login succeeds in the browser, but backend token validation fails.
- Check that `MOODMATCH_OIDC_CLIENT_ID` and `MOODMATCH_OIDC_AUDIENCE` match the Google web client ID used by the frontend.

### Login visible but API returns `401`

- Check whether the frontend stored a credential token in memory.
- Check the browser Network tab for the `Authorization` header.
- Check whether the backend is actually in `oidc` mode and OIDC is enabled.
- Check for client ID or audience mismatch.

### IntelliJ / PostgreSQL not running

- Symptom: backend startup fails or database connection fails.
- Check that PostgreSQL is running locally.
- Check `MOODMATCH_DB_URL`, `MOODMATCH_DB_USERNAME`, and `MOODMATCH_DB_PASSWORD`.
- If you start the backend from IntelliJ, make sure the same environment variables are set in the Run configuration.

### GIS popup or One Tap issues

- Symptom: the Google popup does not appear, appears blank, or One Tap behaves unexpectedly.
- Check the browser console for Google Identity Services errors.
- Confirm the client ID and allowed origins first.
- For later deployment, Google Identity Services popup or One Tap behavior may require CSP or COOP header adjustments.
- Do not implement those headers in Phase 29; treat them as future deployment or debugging concerns.

## 11. Manual Verification Checklist

- `local-demo` mode still works without Google credentials.
- `oidc` mode requires login before protected routes are usable.
- A logged-in protected API request sends a bearer token.
- The backend creates or reuses the correct `AppUser`.
- Logout removes token-backed access to protected routes.
- User data remains scoped to the resolved current user.

## 12. Next Phase Recommendation

Phase 30 should depend on whether a real Google client ID is available.

- If the Google client ID is ready, Phase 30 should activate real local Google login and complete manual verification.
- If the Google client ID is not ready yet, Phase 30 should focus on backend and frontend auth hardening around the existing foundation.

This keeps the project aligned with the current codebase while avoiding premature deployment-only work.
