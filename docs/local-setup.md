# Local setup

This guide describes the supported local development, test, and demo workflow for the final MoodMatch repository.

## Prerequisites

- Java 21
- Maven, or use the included `backend/mvnw` wrapper
- Node.js and npm
- PostgreSQL
- `curl` for the demo-user helper

## 1. Prepare PostgreSQL

Create a local database and user using credentials of your choice. For example, use your normal PostgreSQL administration tool or `psql`; do not copy personal credentials into Git. The default database name expected by the example is `moodmatch`, on `localhost:5432`.

Put the resulting JDBC URL, username, and password in the uncommitted root `.env.local`.

## 2. Create local env files

From the repository root:

```bash
cp .env.local.example .env.local
cp frontend/.env.local.example frontend/.env.local
```

The committed reference files are:

- `.env.local.example` — backend database, local auth, CORS, optional provider keys, and demo account placeholders
- `backend/.env.example` — backend variable reference with provider URL overrides
- `frontend/.env.local.example` — Vite API/auth settings for local development
- `frontend/.env.example` — frontend variable reference

Never commit `.env.local`, `frontend/.env.local`, real provider keys, personal passwords, or bearer tokens.

## 3. Configure `.env.local`

Required for the normal local-password flow:

```dotenv
MOODMATCH_DB_URL=jdbc:postgresql://localhost:5432/moodmatch
MOODMATCH_DB_USERNAME=<your-local-database-user>
MOODMATCH_DB_PASSWORD=<your-local-database-password>
MOODMATCH_AUTH_MODE=local-password
QUARKUS_HTTP_CORS_ENABLED=true
QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173
```

Recommended CORS values are already present in the example. Provider keys are optional:

- `MOODMATCH_TMDB_API_KEY`
- `MOODMATCH_RAWG_API_KEY`
- `MOODMATCH_PODCASTINDEX_KEY`
- `MOODMATCH_PODCASTINDEX_SECRET`
- `MOODMATCH_YOUTUBE_API_KEY`

For the local demo helper, set `MOODMATCH_DEMO_EMAIL`, `MOODMATCH_DEMO_PASSWORD`, and optionally `MOODMATCH_DEMO_DISPLAY_NAME` to values that exist only in your local env file. `MOODMATCH_LOCAL_BACKEND_URL` is optional and defaults to `http://localhost:8080`.

## 4. Configure the frontend

The committed local example uses:

```dotenv
VITE_AUTH_MODE=local-password
VITE_AUTH_PROVIDER=local-password
VITE_API_BASE_URL=http://localhost:8080
```

The frontend also supports the Vite `/api` proxy when `VITE_API_BASE_URL=/api` is used. The explicit local backend URL in the example makes the runtime boundary clear and matches the CORS setup.

## 5. Start the backend

From the repository root:

```bash
cd backend
./scripts/dev-local.sh
```

The script loads the root `.env.local`, exports it for the process, and starts Quarkus dev mode through `./mvnw`. The default backend URL is `http://localhost:8080`.

Check the health endpoint:

```bash
curl http://localhost:8080/api/health
```

Flyway applies and validates migrations during startup.

## 6. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

The default frontend URL is `http://localhost:5173`.

## 7. Create or verify a demo user

After the backend is running, from the repository root:

```bash
./scripts/dev-create-demo-user.sh
```

The helper reads the local demo values, calls the normal register endpoint, and if the account already exists verifies the credentials through the normal login endpoint. It does not bypass auth or seed a production account.

Open `http://localhost:5173` and sign in with the local values from `.env.local`.

## 8. Suggested local demo flow

1. Login.
2. Review Dashboard.
3. Search/import a title.
4. Open Media Library and Media Detail.
5. Review Profile readiness and signals.
6. Inspect Candidates.
7. Use Swipe.
8. Explain a result in Matches.
9. Switch German/English and dark/light/system theme if demonstrating UI preferences.

## 9. Verification commands

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run build
```

Backend:

```bash
cd backend
./scripts/test-clean.sh
```

`test-clean.sh` intentionally does not load `.env.local` and unsets database, auth, CORS, OIDC, and provider overrides before running `./mvnw test`. This keeps local secrets and live provider configuration out of automated tests.

## Provider setup notes

- TMDB: films/series; requires `MOODMATCH_TMDB_API_KEY`.
- Open Library: books; no secret required by the current public search integration.
- RAWG: games; requires `MOODMATCH_RAWG_API_KEY`.
- AniList: anime/manga mapped into existing media types; no key required.
- Podcast Index: podcast shows/feeds; requires `MOODMATCH_PODCASTINDEX_KEY` and `MOODMATCH_PODCASTINDEX_SECRET`.
- LibriVox: public-domain audiobooks; no key required.
- YouTube Data API: video search and URL import; requires `MOODMATCH_YOUTUBE_API_KEY`.
- DEMO: offline fallback/test source; no key required.

Provider quality and availability vary. Missing optional keys should not block the core application or clean test suite.

## Troubleshooting

- **Backend does not start:** confirm PostgreSQL is running and the three `MOODMATCH_DB_*` values point to an accessible local database.
- **Frontend cannot reach the API:** confirm the backend is on port 8080 and `VITE_API_BASE_URL` matches the chosen direct or proxy setup.
- **Demo helper fails:** confirm the backend is running and the local demo email/password variables are set.
- **Live search is unavailable:** configure the relevant backend-only provider key, or use DEMO/fallback behavior for local testing.
- **CORS errors with a direct frontend URL:** confirm `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`.
