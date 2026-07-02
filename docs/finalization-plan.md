# MoodMatch Finalization Plan

## 1. Current Baseline

MoodMatch is currently a working local full-stack MVP/checkpoint.

Implemented baseline:

- Quarkus backend.
- Vue 3, Vite, and TypeScript frontend.
- PostgreSQL for local development.
- Flyway database migrations.
- H2 database profile for backend tests.
- Media CRUD.
- Starter tags.
- Deterministic matching/scoring.
- Dashboard, profile, candidates, matches, and swipe mode.
- Offline demo external search provider.
- Documentation for local setup, architecture, API contract, scoring, testing, and roadmap.
- Environment-variable based local database credentials.
- Backend and frontend tests.

Current project shape:

- Frontend talks to the backend through `/api`.
- Backend owns database access and business logic.
- PostgreSQL stores media, tags, external references, and migration history.
- Flyway owns schema changes.
- External providers should stay behind backend adapter interfaces.
- The current app is mostly a single-user local MVP.

Important current limitations:

- No authentication/login yet.
- No user-owned media data yet.
- No deployed environment yet.
- No real external provider integration yet.
- No real import flow yet.
- No persisted like/skip swipe decisions yet.
- No production CI/deployment pipeline yet.
- No browser E2E tests yet.
- Mobile UI is functional but not final-polished.

## 2. Final Target

The final target is a deployable university prototype for several private daily users.

The target is not a full production consumer-grade application, but it should be serious enough that several people can access it through a web link, log in, manage their own media, import external media, and receive private matches.

Expected final qualities:

- Users can access MoodMatch through a web URL.
- Users can authenticate through email/OIDC/Google login.
- Each user has private media data.
- Each user gets their own profile, candidates, matches, and swipe experience.
- At least one real external provider is integrated.
- Imported external results default to `WANT_TO_CONSUME`.
- The mobile frontend is polished enough for daily use.
- Secrets and API keys are managed through environment variables.
- The project has tests, documentation, and deployment instructions.

## 3. Scope Classification

### Must-have

- Authentication/login.
- User ownership for media and matching data.
- Safe environment-variable handling for database/auth/API keys.
- At least one real external provider.
- Import flow from external search to local media library.
- Mobile-ready core user flow.
- Deployment plan and basic deployment configuration.
- Updated tests and documentation.

### Should-have

- TMDB movies/series integration.
- Swipe filters.
- Dashboard analytics.
- Better CI workflow.
- Improved mobile/Tinder-style UI polish.
- External source coverage in the dashboard.

### Could-have

- Audiobook provider integration if a viable source is confirmed.
- YouTube provider through the official YouTube API if quota, terms, and setup are acceptable.
- Playwright E2E tests.
- More visual dashboard charts.

### Future

- Phone OTP login.
- Rotten Tomatoes integration.
- Full production hardening.
- Full i18n.
- Advanced persistent swipe learning.
- Full accessibility audit.

## 4. Authentication Plan

Recommended path:

- Use email/OIDC/Google login first.
- Do not build custom phone OTP as the first authentication solution.
- Do not build SMS login unless explicitly required later.
- Prefer a provider or OIDC-based flow that can be deployed safely.

Reasoning:

- Phone OTP requires SMS provider setup, abuse protection, resend limits, expiration logic, rate limiting, and cost control.
- Email/OIDC/Google login is more realistic for a university prototype.
- Login must be connected to user-owned data; otherwise all users would still share the same media library.

Backend implications:

- Add authentication/security configuration.
- Add a way to identify the current user.
- Scope all media/profile/matching queries by user.
- Add tests for authenticated and unauthorized access.
- Store only the necessary user identity information.

Frontend implications:

- Add login/logout flow.
- Add protected routes.
- Show user state in the app shell.
- Handle unauthorized API responses.
- Redirect unauthenticated users to login.

Environment variables will be required for auth provider configuration. Exact variable names depend on the chosen provider.

## 5. User Ownership Plan

Authentication is not enough by itself. MoodMatch must also make user data private.

Required concept:

- Each media item belongs to one user.
- Profile calculation uses only that user’s consumed media.
- Candidates are selected only from that user’s media.
- Matches are calculated only for that user.
- Swipe decisions should be scoped to the user.
- Imported external media belongs to the user who imported it.

Recommended model:

- Keep starter tags global.
- Add user ownership to media items.
- Add custom user tags only as future work.
- Keep scores calculated live from user-owned data.
- Add persistent swipe decisions later, after the user model exists.

Database migration ideas:

- Add a `users` or `app_users` table if needed.
- Add a `user_id` column to `media_items`.
- Backfill local existing data to a demo/local user during migration if needed.
- Add indexes for user-scoped queries.
- Add constraints so user-owned data remains consistent.

Service/repository changes:

- All media queries must filter by current user.
- Matching/profile/candidate services must receive or resolve current user identity.
- External imports must create media for the current user.
- Tests must verify that one user cannot see another user’s data.

## 6. External Provider Roadmap

Current baseline:

- The app already has an external search adapter foundation.
- The existing provider is an offline demo provider.
- Real providers should plug into the existing backend provider boundary.

Provider priority:

1. TMDB for films and series.
2. Audiobook provider research and integration if viable.
3. YouTube only later if official API use is feasible and compliant.

Important terminology:

- Open source means source code is openly licensed.
- Open data means the data can be reused under open terms.
- Free API means usage may be free within limits.
- Public API means developers can access it, but terms and restrictions still apply.

Rules:

- Use official APIs where possible.
- Do not commit API keys.
- Do not expose provider keys in frontend code.
- Store provider keys in backend environment variables.
- Keep provider-specific logic behind backend adapter interfaces.
- Do not plan scraping unless the source explicitly allows it.
- Document provider limits and terms.

## 7. TMDB First-Provider Plan

TMDB should be the first real external provider because movies and series fit the current MoodMatch model well.

First TMDB feature set:

- Movie/series search.
- Title.
- Overview/description.
- Release date or year.
- Poster image URL if allowed.
- Provider genre IDs.
- Genre mapping suggestions to local MoodMatch tags.
- External source name and external ID.
- External URL or reference if useful.

Import behavior:

- Imported TMDB items default to `WANT_TO_CONSUME`.
- External tags are suggestions first.
- User should review/confirm tags before they affect matching.
- Store the TMDB external reference to prevent duplicates.

Duplicate detection:

- Same user.
- Same external source.
- Same external ID.

Testing:

- Provider adapter mapping tests.
- Service tests for provider selection.
- API tests for search results.
- Import tests once import flow exists.
- Error handling tests for provider failures.

## 8. Audiobook Plan

Audiobooks should be treated as their own media type.

Recommended media type:

- `AUDIOBOOK`

Reasoning:

- Audiobooks have different consumption behavior than books.
- Duration, narrator, and audio platform may matter.
- A user may like the book but not the audiobook version, or the opposite.

Candidate sources to research:

- Open Library.
- Internet Archive metadata.
- Google Books.
- Other legally usable audiobook metadata sources.

Important caution:

- Do not promise audiobook integration before source research is complete.
- Metadata quality may be inconsistent.
- Some audiobook sources may be commercial or restricted.
- Audiobook availability is not the same as book metadata availability.

## 9. YouTube Plan

YouTube should be deferred until after TMDB and audiobook research.

Recommended model:

- Add `VIDEO` as a generic media type.
- Treat YouTube as an external source for `VIDEO`.
- Do not add `YOUTUBE_VIDEO` as a media type unless there is a strong reason.

Rules:

- Use the official YouTube API only if feasible.
- Do not scrape YouTube.
- Do not commit YouTube API keys.
- Do not expose YouTube API keys in frontend code.
- Store any YouTube key as a backend environment variable.
- Respect quota limits and provider policies.
- If quota or terms are too restrictive, skip YouTube for the final prototype.

Possible YouTube metadata:

- Video title.
- Description.
- Channel name.
- Published date.
- Thumbnail.
- Duration if available.
- External YouTube video ID.
- External URL.

Important scoring rule:

- YouTube public metrics should not be mixed directly into the personal MoodMatch score.
- MoodMatch score should remain based on personal taste and confirmed local tags.
- External metrics can be displayed separately as metadata if allowed.

## 10. Media Type Expansion Plan

Current media types:

- `FILM`
- `SERIES`
- `BOOK`
- `GAME`

Proposed additions:

- `VIDEO`
- `AUDIOBOOK`

Reasoning:

- `VIDEO` can support YouTube and future video providers without tying the media type to one platform.
- `AUDIOBOOK` is meaningfully different from `BOOK`.

Required changes:

- Backend enum update.
- Flyway migration for database constraints.
- DTO/API documentation update.
- Frontend type update.
- Frontend media type options update.
- Tests for new types.
- Matching should continue to work without special-case scoring unless needed.

## 11. Import Flow Plan

External search should become useful through an import flow.

Recommended flow:

1. User searches an external provider.
2. Backend returns normalized external search results.
3. User opens a preview.
4. User reviews metadata and suggested tags.
5. User confirms import.
6. Backend creates a local media item for the current user.
7. Backend stores external reference.
8. Imported item defaults to `WANT_TO_CONSUME`.
9. Imported item appears in library and candidates/swipe if complete enough.

Important rules:

- Import should be user-confirmed.
- External tags should be suggestions, not automatically confirmed.
- Duplicate import should be prevented.
- Import errors should be clear in the UI.
- Provider failures should not break the whole app.

## 12. Swipe and Filter Plan

Current final gesture mapping should stay:

- Right = Like.
- Left = Reject / persist `NOT_INTERESTED`.
- Up = Skip.
- Down = Preview/details.

Recommended filters:

- Media type.
- Minimum match score.
- Complete candidates only.
- Tag filters.
- Provider/source filters later.
- Hide no-score candidates.

Decision persistence:

- Reject already persists as `NOT_INTERESTED`.
- Skip can remain local for now.
- Like/save should become persistent later, but not by reusing favourite.
- Favourite should remain for consumed media the user loved.

Future table idea:

- `swipe_decisions`
- user ID.
- media ID.
- decision type.
- timestamp.

This should come after user ownership exists.

## 13. Mobile-First UI Polish Plan

The final UI should be Tinder-inspired, not a direct copy.

Goals:

- Mobile-first swipe deck.
- Large media card.
- Poster/cover/thumbnail image area.
- Good fallback design when no image exists.
- Bottom action buttons.
- Smooth gesture feedback.
- Clear detail preview.
- Responsive dashboard.
- Responsive media library.
- Accessible buttons and keyboard alternatives.

Important:

- Do not copy Tinder branding or exact visuals.
- Use the interaction idea: swipeable recommendation cards.
- Keep the app usable on desktop too.

## 14. Dashboard Analytics Plan

Current dashboard is useful but can become more explanatory.

Possible analytics:

- Profile readiness.
- Number of consumed profile sources.
- Candidate completeness.
- Top weighted tags.
- Media type distribution.
- Status distribution.
- Average match score.
- Best current match.
- External source coverage.
- Swipe decision summary after persistence exists.

Future backend endpoint:

- `GET /api/dashboard`

This can aggregate dashboard data server-side once auth, users, and swipe persistence exist.

Scoring rule:

- MoodMatch score should remain separate from external ratings.
- External ratings are public metadata.
- MoodMatch score is personal fit.

## 15. External Ratings Plan

External ratings should be displayed as metadata first.

Do not directly mix external ratings into the MoodMatch score.

Reasoning:

- MoodMatch should recommend based on personal taste.
- Public ratings measure general popularity or reception.
- Mixing both too early makes explanations less clear.

Rotten Tomatoes:

- Research-only / optional.
- Do not make it a required feature unless access is confirmed.
- Prefer provider-native rating metadata first, such as TMDB rating metadata if allowed.

## 16. Testing Plan

Current testing baseline:

- Backend Quarkus/JUnit tests exist.
- Frontend Vitest tests exist.
- Frontend build and lint checks exist.

Future test needs:

- Auth tests.
- User scoping tests.
- Provider adapter tests.
- Import flow tests.
- Duplicate import tests.
- Swipe filter tests.
- Deployment smoke checks.
- Playwright E2E tests after auth and UI stabilize.

Recommended E2E flows later:

- Login.
- Import external media.
- Confirm tags.
- See candidates/matches.
- Swipe a candidate.
- Verify user isolation.

## 17. CI and Deployment Plan

Deployment target:

- Frontend deployed as static build.
- Backend deployed as Java service.
- Database hosted as PostgreSQL.
- Secrets configured through deployment environment variables.

Required deployment environment variables:

- Database URL.
- Database username.
- Database password.
- Auth provider settings.
- Provider API keys, such as TMDB API key.
- Frontend/backend URL settings.
- CORS allowed origin.

CI should eventually run:

- Backend tests.
- Frontend lint.
- Frontend tests.
- Frontend build.

Deployment considerations:

- No secrets in repo.
- Production CORS must not be accidentally open.
- Database migrations should run safely.
- Health checks should exist.
- A basic privacy note should explain what user data is stored.
- Optional Docker/Docker Compose can help local reproducibility.

## 18. Risks and Dependencies

Major risks:

- Auth provider complexity.
- User ownership migration complexity.
- API key and provider terms.
- TMDB rate limits or attribution requirements.
- Audiobook metadata uncertainty.
- YouTube quota and policy constraints.
- UI scope creep.
- Deployment setup complexity.
- Testing scope growing too late.

Mitigation:

- Plan first.
- Implement one phase at a time.
- Keep provider integrations behind backend adapters.
- Keep external metrics separate from MoodMatch scoring.
- Use environment variables for secrets.
- Keep tests passing after every phase.
- Document limitations clearly.

## 19. Recommended Implementation Order

Recommended phases:

1. Phase 20: Finalization plan.
2. Phase 21: Authentication architecture decision.
3. Phase 22: User ownership model.
4. Phase 23: Authentication implementation.
5. Phase 24: Media type expansion.
6. Phase 25: TMDB provider.
7. Phase 26: Import flow.
8. Phase 27: Audiobook research/provider decision.
9. Phase 28: Swipe filters.
10. Phase 29: Mobile UI polish.
11. Phase 30: Dashboard analytics.
12. Phase 31: CI/deployment/E2E.

If time becomes limited, reduce scope to:

1. Auth and user ownership.
2. TMDB provider.
3. Import flow.
4. Mobile UI polish.
5. Deployment documentation.

## 20. Recommended Next Implementation Phase

The next implementation phase after this plan should be an authentication architecture decision phase.

Before writing code, decide:

- Which auth provider or OIDC setup will be used.
- How the backend will identify the current user.
- How user ownership will be represented in the database.
- How local development and deployed environments will configure auth.
- Which tests are required to prove users cannot see each other’s media.

After that decision, implementation should begin with user ownership and authentication, before adding real external imports.