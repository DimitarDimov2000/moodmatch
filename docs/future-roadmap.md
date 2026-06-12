# Future Roadmap

This document describes future work that is intentionally not implemented yet. It is a roadmap note, not part of the current shipped behavior.

## i18n / German-English Copy Consistency

Current state:

- The current app mixes English and German labels, route titles, and explanatory copy.

Intended future goal:

- Introduce a deliberate internationalization strategy and make the UI copy consistent across routes, components, and documentation.

Why it is deferred:

- The current checkpoint prioritizes documenting and stabilizing behavior rather than changing copy architecture or adding i18n runtime support.

## Real External API Providers

Current state:

- External search uses an offline deterministic `DEMO` provider only.

Intended future goal:

- Add real provider integrations behind the existing backend-only adapter boundary.

Why it is deferred:

- The project currently focuses on the explainable local core and avoids introducing API-key handling, network variability, and provider-specific operational concerns.

## External Import Flow Into Local Media/Candidates

Current state:

- External search can preview normalized results and suggested tags, but it does not import anything into the local library.

Intended future goal:

- Let users review external preview data and import it into local media or candidate records with explicit confirmation of what becomes local data.

Why it is deferred:

- Import adds persistence decisions, duplicate handling, tag-confirmation UX, and broader validation flows that are intentionally outside the current checkpoint.

## Persistent Swipe-Like/Save Behavior

Current state:

- Swipe `like` and `skip` are local to the current round.
- Swipe `reject` persists by setting the media item to `NOT_INTERESTED`.

Intended future goal:

- Add a clear persisted save/like behavior that updates local data without confusing it with the existing domain `favourite` field.

Why it is deferred:

- The current implementation keeps swipe semantics simple and avoids overloading the meaning of `favourite` or introducing new persistence rules prematurely.

## E2E / Browser Tests

Current state:

- The project relies on backend tests plus frontend unit/component/view tests.
- No browser-level E2E suite is installed yet.

Intended future goal:

- Add end-to-end/browser coverage for core user flows such as media creation, scoring visibility, swipe behavior, and external preview.

Why it is deferred:

- The current checkpoint avoids adding new tooling and keeps validation focused on the existing backend/frontend test layers.

## Production Hardening

Current state:

- The project is runnable locally and is better documented after the checkpoint phases.
- The app is not presented as production-ready.

Intended future goal:

- Harden configuration, secrets handling, deployment setup, monitoring, operational safeguards, and environment separation for a real deployment posture.

Why it is deferred:

- Production readiness requires deployment-specific decisions that are intentionally outside the scope of the current academic checkpoint.

Possible future hardening topics:

- secret management
- separate production datasource settings
- deployment-specific logging and monitoring
- reverse-proxy and CORS review
- backup and restore considerations
- operational startup and recovery guidance
