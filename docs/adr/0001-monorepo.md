# 0001: Use a Monorepo

## Status

Accepted

## Context

MoodMatch is a small academic project with a planned Vue 3 and TypeScript frontend, a Quarkus and Java backend, and shared documentation. The project is deterministic, explainable, locally closed, and rule-based, so the frontend, backend, API contract, and architecture notes are tightly related.

Splitting the project across multiple repositories would add coordination overhead before the project needs that complexity.

## Decision

MoodMatch will use a monorepo with the following top-level structure:

| Path | Purpose |
| --- | --- |
| `frontend/` | Planned Vue 3 and TypeScript frontend. |
| `backend/` | Planned Quarkus and Java backend. |
| `docs/` | Shared architecture, API contract, data model, testing strategy, and ADR documentation. |

## Consequences

The monorepo keeps project setup simple and makes it easier to evolve the frontend, backend, and documentation together.

Shared documentation can describe cross-cutting decisions, API boundaries, and rule-based matching behavior in one place.

As the project grows, CI can be expanded to run frontend and backend checks from the same repository workflow.
