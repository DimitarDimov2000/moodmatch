# MoodMatch User Ownership Plan

> Historical planning document / checkpoint, kept for traceability. User ownership is implemented now; see [architecture.md](architecture.md), [data-model.md](data-model.md), and the root [README](../README.md) for the final behavior.

## 1. Purpose

MoodMatch is moving from a local single-user MVP to a deployable university prototype for several private daily users.

Authentication alone is not enough. If users can log in but all users still share the same media library, then the app is not private.

This document defines how MoodMatch data should become user-owned before implementation starts.

## 2. Current Baseline

The current MoodMatch app has:

- Quarkus backend.
- Vue 3/Vite frontend.
- PostgreSQL development database.
- Flyway database migrations.
- Media CRUD.
- Global starter tags.
- Media-tag relationships.
- Profile calculation.
- Candidate calculation.
- Match scoring.
- Swipe mode.
- Offline demo external search provider.
- Documentation and tests.

Current limitation:

- There is no `app_users` table yet.
- Media items are not assigned to a user.
- Profile calculation uses all local media.
- Candidate calculation uses all local media.
- Match calculation uses all local media.
- Swipe state is not fully persisted per user.
- External imports do not exist yet.

This is acceptable for the current local MVP, but not for a deployed multi-user prototype.

## 3. Target Behavior

In the deployed prototype:

- Each user logs in with email/OIDC/Google.
- Each user has a private media library.
- Each user has their own profile calculation.
- Each user has their own candidates.
- Each user has their own matches.
- Each user has their own future swipe decisions.
- Each user imports external media into their own account.
- One user must never see or modify another user's media.

Example:

```txt
User A creates "Dune" as WANT_TO_CONSUME.
User B logs in.
User B must not see User A's "Dune".
```

## 4. Core Design Decision

MoodMatch should introduce an internal user table.

Recommended table name:

```txt
app_users
```

The app should not rely only on raw provider data from Google/OIDC. Instead, the backend should map the external authenticated identity to a local app user.

This gives MoodMatch a stable internal user ID that can be referenced by media items, imports, and future swipe decisions.

## 5. Proposed `app_users` Table

Suggested fields:

```txt
id
provider
provider_subject
email
display_name
avatar_url
created_at
updated_at
```

Field meaning:

- `id`: internal MoodMatch user ID.
- `provider`: authentication provider, for example `GOOGLE` or `OIDC`.
- `provider_subject`: stable external user identifier from the auth provider.
- `email`: user's email address for display and recognition.
- `display_name`: optional user display name.
- `avatar_url`: optional profile image URL.
- `created_at`: creation timestamp.
- `updated_at`: update timestamp.

Important rule:

```txt
provider + provider_subject should identify a user uniquely.
```

Email is useful, but it should not be the only identity key because emails can change or may not be stable across providers.

## 6. Media Ownership

The main required ownership change is:

```txt
media_items.user_id
```

Every media item should belong to exactly one app user.

This means:

- A media item cannot be global.
- A media item cannot be visible to every user.
- All media queries must filter by the current authenticated user.
- The frontend must not send arbitrary `user_id` values.
- The backend must get the current user from the authenticated security context.

Recommended rule:

```txt
The backend decides the current user. The frontend never decides which user owns data.
```

## 7. Global Data vs User-Owned Data

### Global data

These should remain global:

- Starter tags.
- Tag categories.
- Tag definitions.
- External source definitions.
- Media type definitions.
- Database migrations.

Reason:

- Starter tags are shared vocabulary.
- They should not be duplicated for every user.
- They make the matching system consistent.

### User-owned data

These should be user-owned:

- Media items.
- Media status.
- Ratings.
- Favourite flags.
- Notes/metadata added by the user.
- Media-tag assignments through the user's media item.
- Future imported media.
- Future swipe decisions.

### Future optional user-owned data

Possible future additions:

- Custom user-created tags.
- Personalized tag aliases.
- Persistent swipe likes/skips.
- Saved searches.
- User preferences/settings.

These are not required for the first user ownership implementation.

## 8. Media Tags and Ownership

The current media-tag relation connects media items and tags.

If media items become user-owned, then `media_tags` are indirectly user-owned through `media_items`.

Recommended rule:

```txt
A user can only add or remove tags on their own media items.
```

There is no need to add `user_id` directly to `media_tags` if access is always checked through the owned media item.

## 9. Profile, Candidates, and Matches

All recommendation logic must become user-scoped.

### Profile calculation

Current concept:

```txt
Profile is built from consumed media with rating >= 4 and confirmed tags.
```

Future user-scoped concept:

```txt
Profile is built only from the current user's consumed media with rating >= 4 and confirmed tags.
```

### Candidate calculation

Current concept:

```txt
Candidates are media items with WANT_TO_CONSUME status.
```

Future user-scoped concept:

```txt
Candidates are only the current user's media items with WANT_TO_CONSUME status.
```

### Match calculation

Current concept:

```txt
Matches compare candidates against the profile.
```

Future user-scoped concept:

```txt
Matches compare only the current user's candidates against the current user's profile.
```

Important privacy rule:

```txt
No matching endpoint may use another user's media in the calculation.
```

## 10. External Imports

Future external imports must be user-owned.

Import flow after user ownership:

1. Authenticated user searches external provider.
2. User previews result.
3. User confirms import.
4. Backend creates local media item for the current user.
5. Backend stores external reference.
6. Imported media defaults to `WANT_TO_CONSUME`.
7. Imported media can appear in that user's candidates/swipe flow.

Important rule:

```txt
External imports must never create global media items.
```

## 11. Future Swipe Decisions

Current swipe behavior:

- Reject persists as `NOT_INTERESTED`.
- Like/skip are mostly local session decisions.
- Details opens/preview details.

Future persistent swipe model can be added after user ownership.

Possible future table:

```txt
swipe_decisions
```

Suggested fields:

```txt
id
user_id
media_item_id
decision
created_at
updated_at
```

Possible decisions:

```txt
LIKE
SKIP
REJECT
DETAILS
```

Recommended first step:

- Do not add `swipe_decisions` immediately.
- First implement `app_users` and `media_items.user_id`.
- Add persistent swipe decisions later if needed.

## 12. Database Migration Strategy

User ownership should be implemented with Flyway migrations.

Likely migration steps:

1. Create `app_users`.
2. Add `user_id` column to `media_items`.
3. Create a local/demo user for existing local data if needed.
4. Backfill existing media items to that local/demo user.
5. Make `media_items.user_id` not nullable.
6. Add foreign key from `media_items.user_id` to `app_users.id`.
7. Add index on `media_items.user_id`.
8. Add unique constraints where needed for provider identity.

Possible migration constraint:

```txt
unique(provider, provider_subject)
```

This prevents duplicate app users for the same external identity.

## 13. Existing Local Data

Because the current app already has local media data, a migration must decide what happens to existing rows.

Recommended development strategy:

- Create a demo/local app user.
- Assign all existing media items to that demo/local user.
- Use this only as a migration/backfill strategy for existing local data.
- Future created media should use the authenticated current user.

Possible demo user values:

```txt
provider = LOCAL
provider_subject = local-demo-user
email = local-demo@example.local
display_name = Local Demo User
```

This preserves existing local demo data during migration.

## 14. Backend Service Changes

The backend should stop using global media queries for private data.

Services that likely need user scoping:

- `MediaService`
- `InterestProfileService`
- `CandidateService`
- `MatchingService`
- `ExternalSearchService` later for imports
- future `SwipeDecisionService`

Repository/service methods should receive a current user or user ID.

Example conceptual change:

```txt
findAllMedia()
```

becomes:

```txt
findAllMediaForUser(currentUser)
```

Important rule:

```txt
Do not accept user IDs from frontend request bodies for ownership.
```

The backend should resolve the current user from authentication.

## 15. REST API Changes

Most API routes can keep the same external paths.

Example:

```txt
GET /api/media
```

should still exist, but after user ownership it should return only the current user's media.

This is better than exposing user IDs in URLs.

Recommended API behavior:

- `GET /api/media` returns current user's media.
- `POST /api/media` creates media for current user.
- `GET /api/media/{id}` returns the item only if it belongs to current user.
- `PUT /api/media/{id}` updates the item only if it belongs to current user.
- `DELETE /api/media/{id}` deletes the item only if it belongs to current user.
- Profile/candidate/match endpoints use only current user's data.

Unauthorized or cross-user access should return an appropriate error.

## 16. Frontend Implications

The frontend should not need to know user IDs for media ownership.

Future frontend behavior:

- User logs in.
- Frontend calls `/api/media`.
- Backend returns only that user's media.
- Frontend displays current user's private data.
- If unauthorized, frontend shows login or session-expired state.

Frontend should not send:

```txt
userId
```

when creating or updating media.

The backend should attach ownership automatically.

## 17. Testing Requirements

Backend tests should prove user isolation.

Required test areas:

### Media tests

- User A can create media.
- User A can list own media.
- User B cannot see User A's media.
- User B cannot edit User A's media.
- User B cannot delete User A's media.

### Profile tests

- User A's profile uses only User A's consumed media.
- User B's profile uses only User B's consumed media.
- User A's profile does not include User B's tags.

### Candidate tests

- User A's candidates include only User A's `WANT_TO_CONSUME` media.
- User B's candidates include only User B's `WANT_TO_CONSUME` media.

### Match tests

- User A's matches are calculated only from User A's profile and candidates.
- User B's media does not affect User A's match scores.

### Import tests later

- Imported media belongs to current user.
- Duplicate detection is user-scoped.

## 18. Development Without Full Auth

There may be a short transition phase where user ownership exists before full OIDC login is implemented.

For local development, the backend can temporarily resolve a default demo user.

This must be clearly temporary.

Possible temporary strategy:

```txt
If no real auth is active in dev, use Local Demo User.
```

Important:

- This should not become production behavior.
- It should be documented.
- Production profile should require real authentication.
- Tests should still be able to simulate different users.

## 19. Deployment Implications

For deployed multi-user use:

- User ownership is mandatory.
- Authentication is mandatory.
- Database rows must be private by user.
- External imports must be attached to current user.
- Secrets must stay in environment variables.
- CORS and auth redirect URLs must be configured.
- A basic privacy note should explain stored user preference data.

## 20. Risks

Main risks:

- Implementing login without user-owned data.
- Accidentally returning all users' media.
- Letting frontend choose `user_id`.
- Breaking existing matching logic during user scoping.
- Making starter tags user-owned unnecessarily.
- Overcomplicating swipe persistence too early.
- Mixing user ownership implementation with external provider work.

Mitigation:

- Implement user ownership before external imports.
- Keep starter tags global.
- Add cross-user isolation tests.
- Keep API routes mostly stable.
- Resolve current user in backend.
- Avoid frontend-controlled ownership.

## 21. Recommended Implementation Order

Recommended next implementation order:

1. Add `app_users` concept.
2. Add `media_items.user_id`.
3. Backfill existing local media to a demo/local user.
4. Add backend current-user resolution abstraction.
5. Scope media service/repository methods by current user.
6. Scope profile/candidate/matching services by current user.
7. Add user-isolation backend tests.
8. Add frontend auth-aware behavior later.
9. Add real OIDC/Google authentication.
10. Add external imports after user ownership is stable.

## 22. Recommended Next Phase

The next phase after this document should be a carefully scoped implementation phase for user ownership foundation.

Recommended Phase 23:

```txt
Add user ownership foundation
```

It should include:

- `app_users` database migration.
- `AppUser` entity.
- `media_items.user_id` migration.
- local/demo user backfill.
- current-user resolver abstraction.
- user-scoped media/profile/candidate/matching logic.
- backend tests for user isolation.

It should not include:

- real Google/OIDC login yet.
- TMDB provider yet.
- import flow yet.
- swipe decision persistence yet.
- frontend redesign yet.
