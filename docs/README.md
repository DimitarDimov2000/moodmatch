# MoodMatch documentation

This folder separates current implementation references from historical planning and checkpoint material. If a historical document conflicts with the code, this index, the root [README](../README.md), and the current reference docs take precedence.

## Current reference documentation

- [Local setup](local-setup.md) — prerequisites, env files, PostgreSQL, startup, demo user, and verification commands
- [Architecture](architecture.md) — final frontend/backend/provider/persistence responsibilities and data flows
- [API contract](api-contract.md) — implemented REST endpoints and payload expectations
- [Data model](data-model.md) — entities and migration-aligned persistence model
- [Scoring and matching](scoring-and-matching.md) — deterministic profile, readiness, candidate, and score rules
- [External media sources](external-media-sources.md) — active providers, keys, mapping, and limitations
- [Testing strategy](testing-strategy.md) — automated coverage, commands, and manual demo checks
- [Auth provider decision](auth-provider-decision.md) — current local-password decision and scope
- [Monorepo ADR](adr/0001-monorepo.md) — rationale for keeping frontend, backend, and docs together
- [Future roadmap](future-roadmap.md) — known limitations separated from realistic future enhancements

## Historical planning and checkpoint records

The following documents are intentionally retained for traceability. Each is labelled at the top as a historical planning document or checkpoint and may describe an earlier state:

- [Authentication architecture](auth-architecture.md)
- [Authentication implementation plan](auth-implementation-plan.md)
- [Phase 19 checkpoint](checkpoint-phase-19.md)
- [Finalization plan](finalization-plan.md)
- [Google/OIDC setup reference](google-auth-setup.md)
- [Provider verification checklist](provider-verification-checklist.md)
- [User ownership plan](user-ownership-plan.md)

These files should not be used as the primary setup instructions for the final submission.
