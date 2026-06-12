# MoodMatch

MoodMatch is a deterministic, explainable media decision assistant for choosing what to watch, read, or play next.

## Phase 19 Checkpoint

This repository is frozen as a project checkpoint after Phase 18. The current app is already runnable and testable with a Vue 3 frontend, a Quarkus backend, PostgreSQL in local development, Flyway migrations, and automated frontend/backend tests.

MoodMatch manages a local media library, calculates an interest profile from consumed and positively rated items, scores `WANT_TO_CONSUME` candidates with deterministic tag-based matching, and shows the reasoning behind each result. It also includes a preview-only external search flow backed by an offline DEMO provider and a swipe mode with explicit local-vs-persistent decision semantics.

## What Works Now

- Media CRUD through the backend and frontend
- Tag replacement for media items
- Consumption-status and favourite validation rules
- Seeded starter tag taxonomy via Flyway
- Interest profile calculation from local consumed media
- Candidate listing for `WANT_TO_CONSUME` items
- Deterministic match scoring with explanations
- Dashboard assembled from existing APIs
- Swipe mode for local round-based decisions
- External search preview through an offline DEMO provider

## Current Limitations

- No real external API calls yet
- No external import into the media library yet
- No dedicated `/api/dashboard` endpoint
- No persistent swipe-like/save action yet
- Swipe reject persists as `NOT_INTERESTED`
- Swipe like and skip remain local to the current swipe round
- Favourite is separate from swipe semantics and only valid for consumed media rated 4 or 5

## Monorepo Structure

| Path | Purpose |
| --- | --- |
| `frontend/` | Vue 3 + TypeScript app for dashboard, media management, profile, candidates, matches, swipe mode, and external search preview |
| `backend/` | Quarkus REST API with validation, persistence, Flyway migrations, deterministic profile/matching logic, and the DEMO external search provider |
| `docs/` | Project documentation, API contract, setup notes, checkpoint summary, testing notes, and ADRs |

## Quick Start

Project setup and verification:

- [Local Setup](docs/local-setup.md)
- [Phase 19 Checkpoint](docs/checkpoint-phase-19.md)

Core reference docs:

- [Architecture](docs/architecture.md)
- [API Contract](docs/api-contract.md)
- [Data Model](docs/data-model.md)
- [Scoring And Matching](docs/scoring-and-matching.md)
- [Testing Strategy](docs/testing-strategy.md)
- [Future Roadmap](docs/future-roadmap.md)
- [ADR 0001: Use a Monorepo](docs/adr/0001-monorepo.md)

## License

The project license will be decided later.
