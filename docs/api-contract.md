# API Contract

This document describes the currently implemented REST API for MoodMatch. It reflects the current code and tests, not future aspirations.

Base path: `/api`

## Implemented Endpoints

### Health

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/health` | Basic backend health response |

Example response:

```json
{
  "status": "UP"
}
```

### Auth

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/auth/register` | Create a local email/password account |
| POST | `/auth/login` | Log in with email/password |
| GET | `/auth/me` | Return the current authenticated user |
| POST | `/auth/logout` | Revoke the current bearer token |

Register request:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "Mood Student"
}
```

Login request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Auth response:

```json
{
  "token": "opaque-bearer-token",
  "user": {
    "id": "00000000-0000-0000-0000-000000000001",
    "email": "user@example.com",
    "displayName": "Mood Student"
  }
}
```

Notes:

- Email is normalized to lowercase.
- Password must be at least 8 characters.
- Duplicate email returns `409`.
- Invalid login returns `401` without revealing whether email or password was wrong.
- Password hashes are never returned.

### Media

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/media` | List the current user's media items |
| GET | `/media/{id}` | Get one current-user media item by id |
| POST | `/media` | Create a media item for the current user |
| PUT | `/media/{id}` | Replace one current-user media item |
| DELETE | `/media/{id}` | Delete one current-user media item |
| PATCH | `/media/{id}/status` | Update only the consumption status for one current-user media item |
| PATCH | `/media/{id}/favorite` | Update only the favourite flag for one current-user media item |
| PUT | `/media/{id}/tags` | Replace the full confirmed tag list for one current-user media item |

Notes:

- `PUT /media/{id}` replaces the full editable media resource.
- `PUT /media/{id}/tags` replaces the full confirmed tag set for the item.
- `PATCH /media/{id}/status` may also clear rating and favourite when leaving `CONSUMED`.
- `PATCH /media/{id}/favorite` updates only `isFavourite`.
- The frontend does not send a `user_id`; the backend resolves the current user internally.
- In `local-demo` mode, the backend resolves the Local Demo User internally for development and tests.
- In `local-password` mode, these endpoints require a valid bearer token and the backend resolves the current `AppUser` from the local session.

### Tags

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/tags` | List the available starter and user-created tags |

### Profile

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/profile` | Return the calculated interest profile |

Profile behavior:

- Uses only the current user's consumed media with rating 4 or 5 and at least one confirmed local tag.
- Returns readiness information and explanation text.
- Suppresses meaningful matching until enough profile-relevant media exist.
- Requires bearer authentication in `local-password` mode.

### Candidates

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/candidates` | Return `WANT_TO_CONSUME` media plus matching completeness |

Candidate behavior:

- Candidates are the current user's items with `consumptionStatus = WANT_TO_CONSUME`.
- Each candidate includes `isCompleteForMatching`.
- Requires bearer authentication in `local-password` mode.

### Matches

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/matches` | Return deterministic match results with explanation details |

Match behavior:

- Scores are based on deterministic weighted tag overlap plus precision adjustment over the current user's profile and candidates.
- `relativeScore` may be `null` when comparisons are not meaningful yet.
- `scoresSuppressed` may be `true` when the profile is not ready.
- Explanations distinguish incomplete candidates from no-overlap candidates.
- Requires bearer authentication in `local-password` mode.

### External Search Preview

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/external/search` | Search normalized preview results from the offline DEMO provider |

Query parameters:

| Name | Required | Type | Notes |
| --- | --- | --- | --- |
| `query` | yes | string | Trimmed server-side |
| `mediaType` | yes | enum | `FILM`, `SERIES`, `BOOK`, `GAME` |
| `source` | no | enum | Only `DEMO` is currently supported |
| `limit` | no | integer | Positive integer, capped by backend safety rules |

Preview behavior:

- Uses an offline deterministic DEMO provider only.
- Does not call real external APIs yet.
- Does not import anything into the local media library.
- Suggested tags are suggestions only and do not become local media tags automatically.
- Requires bearer authentication in `local-password` mode.

Example response shape:

```json
{
  "query": "arrival",
  "mediaType": "FILM",
  "source": "DEMO",
  "results": [
    {
      "source": "DEMO",
      "externalId": "demo-film-arrival",
      "mediaType": "FILM",
      "title": "Arrival",
      "originalTitle": null,
      "description": "A normalized preview description.",
      "releaseYear": 2016,
      "coverUrl": "https://demo.moodmatch.local/covers/arrival.jpg",
      "sourceUrl": "https://demo.moodmatch.local/items/demo-film-arrival",
      "externalGenres": ["Science-Fiction", "Drama"],
      "externalSubjects": ["Zeit", "Entdeckung"],
      "suggestedTags": [],
      "attribution": "MoodMatch Demo Provider (offline)",
      "warnings": []
    }
  ],
  "warnings": []
}
```

## Validation And Error Shape

Handled errors return structured JSON:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed.",
  "details": [
    {
      "field": "query",
      "message": "must not be blank"
    }
  ]
}
```

Current error codes include:

- `VALIDATION_ERROR`
- `BUSINESS_RULE_VIOLATION`
- `RESOURCE_NOT_FOUND`
- `REQUEST_ERROR`
- `INTERNAL_SERVER_ERROR`

## Important Domain Semantics

- Favourite is a persisted domain flag, not a swipe/save action.
- Favourite is only valid for consumed media with rating 4 or 5.
- Candidate matching uses confirmed local tags only.
- External preview results never affect scoring by themselves.

## Future Work Outside The Current Contract

The following are not part of the implemented API contract yet:

- `/api/dashboard`
- External import endpoints
- Dedicated decision-mode filter endpoints
- Persistent swipe-like/save endpoints

## Auth Mode Summary

- Public endpoint: `GET /api/health`
- Public for now: `GET /api/tags`
- Protected in `local-password` mode: `/api/media`, `/api/profile`, `/api/candidates`, `/api/matches`, `/api/external/search`, `/api/auth/me`, `/api/auth/logout`
- Backend tests still use `local-demo` where useful, while dedicated auth tests run `local-password`.

## Frontend Auth Expectations

- In `local-demo` frontend mode, private routes remain locally accessible and frontend API requests may omit `Authorization`.
- In `local-password` frontend mode, the SPA treats dashboard, media, profile, candidates, matches, swipe, and external search routes as protected.
- When the frontend has a MoodMatch token, it should call protected backend endpoints with `Authorization: Bearer <token>`.
- A backend `401 Unauthorized` should be treated as a login-required or session-expired state on the frontend.
