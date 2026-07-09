# Future roadmap

MoodMatch's core university-project workflows are implemented. This document separates truthful current limitations from enhancements that are intentionally outside the local/demo submission scope.

## Known limitations today

- Live provider quality depends on third-party APIs, network access, quotas, rate limits, metadata quality, and optional local keys.
- Missing artwork uses a fallback; provider imagery is not guaranteed.
- Local-password bearer-token auth is appropriate for the local/demo prototype, not a production account platform.
- The repository does not provide a guaranteed public deployment or hosted database.
- Matching is deterministic and explainable rather than learned from a large behavioural dataset.
- Swipe decisions support the current local decision flow; long-term persisted recommendation feedback is not part of the current scope.
- Browser-level end-to-end automation and operational monitoring are not included in the current test/operations setup.

## Future enhancements

### Deployment and operations

- production deployment and environment separation
- hosted PostgreSQL, secret management, monitoring, logging, and backup/recovery runbooks
- a CI/CD deployment pipeline after a hosting target is chosen

### Authentication and account management

- OAuth or another production identity provider where appropriate
- secure HTTP-only session cookies, account recovery, email verification, and broader security hardening

### Recommendation quality

- richer recommendation learning and relevance tuning
- improved tag confirmation/onboarding from import to match-ready state
- more provider backups, artwork resolution, caching, and metadata cleanup

### Product and QA support

- explicit persisted save/like semantics for swipe feedback
- better demo-data reset/admin tooling
- browser-level E2E tests for login, import, profile readiness, matches, and swipe flows
- additional accessibility and visual regression coverage
