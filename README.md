# MoodMatch

MoodMatch is a deterministic, explainable media decision assistant for choosing what to watch, read, or play next.

## Overview

MoodMatch helps users manage a personal collection of movies, series, books, and games, then compares candidate media against an interest profile built from consumed and positively rated items.

Unlike traditional recommendation systems, MoodMatch does not use artificial intelligence, machine learning, collaborative filtering, or external recommendation APIs. Its matching process is rule-based, deterministic, and transparent, so users can understand why each item was ranked.

## Key Features

* Personal media collection management
* Interest profile generation
* Cross-media matching (movies, series, books, games)
* Explainable recommendations
* Decision Mode filtering
* Rule-based scoring

## Tech Stack

* Vue 3
* TypeScript
* Quarkus
* Java
* PostgreSQL
* REST/JSON

## Architecture

MoodMatch uses a monorepo structure:

| Path | Purpose |
| --- | --- |
| `frontend/` | Planned Vue 3 and TypeScript frontend. |
| `backend/` | Planned Quarkus and Java backend. |
| `docs/` | Shared architecture, API, data model, testing, and ADR documentation. |

The frontend will be a Vue 3 and TypeScript application responsible for collection management, candidate browsing, filtering, and explanation-focused result views.

The backend will be a Quarkus and Java REST API that owns media data, interest profile generation, deterministic scoring rules, and recommendation explanations.

The database will be PostgreSQL, storing the locally closed media dataset, user collection state, predefined tags, ratings, and matching metadata.

## Documentation

* [Architecture](docs/architecture.md)
* [API Contract](docs/api-contract.md)
* [Data Model](docs/data-model.md)
* [Testing Strategy](docs/testing-strategy.md)
* [ADR 0001: Use a Monorepo](docs/adr/0001-monorepo.md)

## Project Status

Initial monorepo architecture setup. The backend has an initial Quarkus scaffold. Vue, database migrations, and matching logic have not been scaffolded yet.

## Backend Setup

The backend is a Quarkus Maven project located in `backend/`.

Requirements:

* Java 21 or newer
* Maven Wrapper from the backend project

Start the backend locally:

```powershell
cd backend
.\mvnw.cmd quarkus:dev
```

The minimal startup verification endpoint is available at:

```text
GET http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

## License

The project license will be decided later.
