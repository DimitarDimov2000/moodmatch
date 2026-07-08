# Future Roadmap

This document lists work that is intentionally outside the final local-demo prototype scope.

## Deployment And Operations

Current state:

- the repository is optimized for local development, demo use, and source submission

Future work:

- deployment environment setup
- environment separation for dev/staging/prod
- monitoring, logging, and operational runbooks
- backup/recovery considerations

## Auth And Security Hardening

Current state:

- local email/password auth supports the prototype and demo workflow
- bearer-token handling is suitable for local/dev use

Future work:

- stronger production session/token strategy
- HTTPS-first deployment assumptions
- secret-management improvements
- broader security review for public deployment

## End-To-End Test Coverage

Current state:

- frontend lint/unit-test/build checks exist
- backend automated tests exist
- final QA also relied on manual browser verification

Future work:

- browser-level E2E coverage for login, import, profile readiness, matches, and swipe flows
- richer CI reporting around UI regression checks

## Provider Resilience And Metadata Quality

Current state:

- provider integrations work at prototype level with warnings/fallback behavior
- provider quality still depends on API keys, quota, network, and third-party metadata quality

Future work:

- better metadata cleanup and normalization
- stronger retry/error handling where appropriate
- relevance tuning for provider-specific search behavior
- additional public showcase polish around provider attribution and edge cases

## Import Readiness UX

Current state:

- imported titles become local data, but they may still need tags, ratings, and other signals before scoring becomes meaningful

Future work:

- faster onboarding from import to match-ready state
- clearer guidance around missing tags/signals
- optional workflows for confirming or refining suggested tags

## Swipe Persistence Semantics

Current state:

- swipe is intentionally prototype-level
- reject persists as `NOT_INTERESTED`
- like and skip remain local to the active flow

Future work:

- explicit persisted save/like behavior
- clearer long-term relationship between swipe decisions, favourites, and candidate management

## Asset And Showcase Polish

Current state:

- the app is demo-ready, but some branding assets and repository presentation elements remain prototype-grade

Future work:

- optimize large SVG assets where worthwhile
- add curated screenshots or short demo media to the repository
- tighten final public-facing presentation material
