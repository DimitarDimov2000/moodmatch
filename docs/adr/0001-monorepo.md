# 0001: Use a Monorepo

## Status

Accepted

## Context

MoodMatch is a small academic project with an active Vue 3 + TypeScript frontend, a Quarkus + Java backend, and shared documentation in one repository. The project is deterministic, explainable, locally grounded, and rule-based, so the frontend, backend, API contract, and project docs evolve together.

Splitting the project across multiple repositories would add coordination overhead without clear benefit at the current scope.

## Decision

MoodMatch uses a monorepo with the following top-level structure:

| Path | Purpose |
| --- | --- |
| `frontend/` | Vue 3 + TypeScript application |
| `backend/` | Quarkus + Java backend |
| `docs/` | Shared documentation, API notes, setup notes, and ADRs |

## Consequences

The monorepo keeps setup simple and makes it easier to evolve the frontend, backend, migrations, tests, and documentation together.

Shared documentation can describe cross-cutting decisions, API boundaries, and deterministic matching behavior in one place.

As the project grows, CI can continue validating both applications from the same repository workflow.
