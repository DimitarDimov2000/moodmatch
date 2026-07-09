# Phase 19 Checkpoint

> Historical planning document / checkpoint, kept for traceability. Use the current reference docs and root README for the final repository state.

This document freezes the current project state after Phase 18 so MoodMatch is understandable, runnable, testable, and presentable without changing application behavior.

Phase 19.1 improves checkpoint and reviewer-readiness posture, but it does not make the application production-ready.

Related reference docs:

- [Scoring And Matching](scoring-and-matching.md)
- [Future Roadmap](future-roadmap.md)

## Current Implemented Features

- Media CRUD through backend and frontend flows
- Full confirmed-tag replacement for media items
- Status and favourite validation rules
- Seeded starter tags through Flyway
- Interest profile calculation from local consumed media
- Candidate listing for `WANT_TO_CONSUME` items
- Deterministic match scoring with explanation details
- Dashboard assembled from existing media, profile, candidates, and matches APIs
- Swipe mode with explicit local-vs-persistent actions
- External search preview through an offline DEMO provider

## Current Limitations

- No real external API calls yet
- No external import into the media library yet
- No dedicated `/api/dashboard` endpoint
- No persistent swipe-like/save action yet
- Swipe reject persists as `NOT_INTERESTED`
- Swipe like and skip are local to the current swipe round
- Favourite is separate from swipe semantics and only valid for consumed media rated 4 or 5

## Swipe Decision Semantics

- `like`: local to the current swipe round only
- `skip`: local to the current swipe round only
- `reject`: persisted by updating the media item to `NOT_INTERESTED`
- `favourite`: domain field on a media item, unrelated to swipe save/like behavior

## External Search Scope

The external search feature is currently a foundation/preview feature:

- uses the backend-only `DEMO` source
- runs against an offline deterministic catalog
- returns normalized preview results
- can show suggested local tags
- does not call real external APIs yet
- does not import anything into the local media library yet

## Suggested Demo Flow

1. Start PostgreSQL locally.
2. Start the backend.
3. Start the frontend.
4. Open the dashboard and review the project summary.
5. Create or edit media in the local library.
6. Assign or replace confirmed tags.
7. Open profile and matches to verify readiness, scoring, and explanations.
8. Use swipe mode and explain local like/skip versus persistent reject.
9. Open external search preview and show the offline DEMO provider flow.

## Presentable Story For Reviewers

MoodMatch already demonstrates the full local rule-based loop:

- maintain a local collection
- mark consumed and candidate items
- confirm local tags
- build an explainable profile
- compare candidates deterministically
- inspect results in both list and swipe-oriented interfaces

The current checkpoint intentionally stops before live provider integrations and external import so the deterministic local core stays clear and testable.

## Safe Cleanup Included In Phase 19

- Stale scaffold-era wording removed from public project docs
- Stale scaffold wording removed from the backend README
- Not-found and candidates UI text aligned with the current implemented routes and swipe scope
- Small frontend wording cleanup limited to clearly contradictory scaffold-era text only

## Future Work Left Out On Purpose

These are intentionally not part of the current checkpoint:

- real external provider integrations
- media import from external search results
- dedicated dashboard backend endpoint
- persistent swipe-like/save behavior
- broader route/component renaming
- behavior changes to scoring, swipe handling, or migrations
