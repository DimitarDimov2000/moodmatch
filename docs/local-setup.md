# Local Setup

This guide describes the current local development setup for the Phase 19 checkpoint.

## Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL

## PostgreSQL For The Dev Profile

The backend development profile expects PostgreSQL on:

- host: `localhost`
- port: `5432`
- database: `moodmatch`
- user: `moodmatch`
- password: `moodmatch`

Example `psql` setup:

```sql
CREATE USER moodmatch WITH PASSWORD 'moodmatch';
CREATE DATABASE moodmatch OWNER moodmatch;
```

If your local PostgreSQL setup requires it, connect as a superuser first:

```bash
psql postgres
```

## Flyway

Flyway runs automatically when the backend starts. It validates the committed migrations and applies them before the app serves requests.

Current migrations:

- `V1__init_schema.sql`
- `V2__seed_starter_tags.sql`

## Start The Backend

```bash
cd backend
./mvnw quarkus:dev
```

Default backend URL:

```text
http://localhost:8080
```

Quick smoke check:

```bash
curl http://localhost:8080/api/health
```

## Run Backend Tests

```bash
cd backend
./mvnw test
```

Notes:

- Backend tests use the Quarkus test profile with H2 in PostgreSQL compatibility mode.
- Resource tests start a local Quarkus HTTP server during the test run.

## Frontend Setup And Run

Install dependencies:

```bash
cd frontend
npm install
```

Start the frontend:

```bash
cd frontend
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

## Frontend API Configuration

The frontend uses `/api` for backend requests.

In local Vite development:

- `frontend/vite.config.ts` proxies `/api` to `http://localhost:8080`

Optional override:

- `VITE_API_BASE_URL` can point the frontend at a different backend base URL
- if set, the frontend normalizes it to an `/api` base path

## Frontend Checks

```bash
cd frontend
npm run lint
npm run test
npm run build
```

Useful additional check:

```bash
cd frontend
npm run typecheck
```

## Quick End-To-End Demo Startup

1. Start PostgreSQL locally.
2. Start the backend with `cd backend && ./mvnw quarkus:dev`.
3. Start the frontend with `cd frontend && npm run dev`.
4. Open the frontend and inspect the dashboard, media flows, profile, matches, swipe mode, and external search preview.
