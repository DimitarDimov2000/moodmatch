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

### External Search And Import

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/external/search` | Search normalized external results |
| POST | `/external/import` | Save one normalized external result into the current user's media library |

Query parameters:

| Name | Required | Type | Notes |
| --- | --- | --- | --- |
| `query` | yes | string | Trimmed server-side |
| `mediaType` | yes | enum | `FILM`, `SERIES`, `BOOK`, `GAME`, `AUDIOBOOK`, `PODCAST`, `VIDEO` |
| `source` | no | enum | `AUTOMATIC`, `DEMO`, `TMDB`, `OPEN_LIBRARY`, `RAWG`, `LIBRIVOX`, `PODCAST_INDEX`, `ANILIST`, or `YOUTUBE`; omit or pass `AUTOMATIC` to search all compatible active providers |
| `limit` | no | integer | Positive integer, capped by backend safety rules |

Search behavior:

- Automatic `FILM` and `SERIES` searches query all compatible real providers in order: `TMDB`, then `ANILIST`.
- If TMDB is not configured during automatic search, the backend skips it, returns a warning, and still returns AniList results when available.
- Automatic `BOOK` searches query `OPEN_LIBRARY`, then `ANILIST`.
- Explicit `source=ANILIST` searches only AniList for `FILM`, `SERIES`, and `BOOK`; it uses AniList GraphQL and does not require an API key.
- Automatic `AUDIOBOOK` searches query `LIBRIVOX`, which does not require a secret in the current implementation.
- LibriVox results are limited to public-domain audiobooks in the LibriVox catalog.
- Automatic `GAME` searches query `RAWG` when `MOODMATCH_RAWG_API_KEY` is configured.
- If RAWG is not configured, automatic game search falls back to `DEMO` with warnings. Explicit `source=RAWG` returns `RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.`
- `PODCAST` and `VIDEO` have no active automatic search provider in this package and return a clear empty response.
- Anime movie results from AniList map to `FILM`; anime TV/OVA/ONA/special/short results map to `SERIES`; manga/light novel/novel/one-shot results map to `BOOK`.
- Automatic responses use response-level `"source": "AUTOMATIC"` and preserve the real provider on every result in `results[*].source`.
- In automatic mode, the capped `limit` is applied per provider before merging so one provider cannot hide another provider's results.
- Response-level `warnings` are non-blocking partial-result notices for skipped or unavailable providers.
- Future provider names are accepted by the enum contract, but requests fail with `Source is not available` until a provider bean exists.
- YouTube is planned only for later URL import, not search.
- Suggested tags are derived from `external_tag_mappings`.
- Requires bearer authentication in `local-password` mode.

Implemented provider/media-type combinations:

- `TMDB` -> `FILM`, `SERIES`
- `OPEN_LIBRARY` -> `BOOK`
- `LIBRIVOX` -> `AUDIOBOOK`
- `RAWG` -> `GAME`
- `ANILIST` -> `FILM`, `SERIES`, `BOOK`
- `DEMO` -> `FILM`, `SERIES`, `BOOK`, `GAME`, `AUDIOBOOK`, `PODCAST`, `VIDEO`

Example response shape:

```json
{
  "query": "spirited away",
  "mediaType": "FILM",
  "source": "AUTOMATIC",
  "warnings": [
    "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Provider skipped in automatic search."
  ],
  "results": [
    {
      "source": "ANILIST",
      "externalId": "199",
      "mediaType": "FILM",
      "title": "Sen to Chihiro no Kamikakushi",
      "originalTitle": "千と千尋の神隠し",
      "creatorNames": [],
      "description": "A normalized preview description.",
      "releaseYear": 2001,
      "coverUrl": "https://img.anilist.co/spirited-away-large.jpg",
      "sourceUrl": "https://anilist.co/anime/199",
      "externalGenres": ["Adventure", "Fantasy"],
      "externalSubjects": ["Format: MOVIE", "Status: FINISHED"],
      "suggestedTags": [],
      "attribution": "Metadata from AniList",
      "warnings": []
    }
  ]
}
```

Import request shape:

```json
{
  "source": "TMDB",
  "externalId": "11",
  "mediaType": "FILM",
  "title": "Arrival",
  "originalTitle": null,
  "creatorNames": [],
  "description": "First contact changes everything.",
  "releaseYear": 2016,
  "coverUrl": "https://image.tmdb.org/t/p/w342/poster.jpg",
  "sourceUrl": "https://www.themoviedb.org/movie/11",
  "externalGenres": ["Science Fiction"],
  "externalSubjects": [],
  "attribution": "Metadata from TMDB"
}
```

Book import request example:

```json
{
  "source": "OPEN_LIBRARY",
  "externalId": "OL82563W",
  "mediaType": "BOOK",
  "title": "Dune",
  "originalTitle": null,
  "creatorNames": ["Frank Herbert"],
  "description": "Book by Frank Herbert. First published in 1965.",
  "releaseYear": 1965,
  "coverUrl": "https://covers.openlibrary.org/b/id/987654-M.jpg",
  "sourceUrl": "https://openlibrary.org/works/OL82563W",
  "externalGenres": [],
  "externalSubjects": ["Politics", "Desert planets"],
  "attribution": "Metadata from Open Library"
}
```

Audiobook search response example:

```json
{
  "query": "pride",
  "mediaType": "AUDIOBOOK",
  "source": "LIBRIVOX",
  "warnings": [],
  "results": [
    {
      "source": "LIBRIVOX",
      "externalId": "253",
      "mediaType": "AUDIOBOOK",
      "title": "Pride and Prejudice",
      "originalTitle": null,
      "creatorNames": [
        "Author: Jane Austen",
        "Reader: Annie Coleman Rothenberg"
      ],
      "description": "Jane Austen's classic novel about wit, family, and first impressions.",
      "releaseYear": 1813,
      "coverUrl": "https://archive.org/covers/pride.jpg",
      "sourceUrl": "https://librivox.org/pride-and-prejudice-by-jane-austen/",
      "externalGenres": ["Romance"],
      "externalSubjects": ["English"],
      "suggestedTags": [],
      "attribution": "LibriVox public domain audiobook catalog",
      "warnings": []
    }
  ]
}
```

Audiobook import request example:

```json
{
  "source": "LIBRIVOX",
  "externalId": "253",
  "mediaType": "AUDIOBOOK",
  "title": "Pride and Prejudice",
  "originalTitle": null,
  "creatorNames": [
    "Author: Jane Austen",
    "Reader: Annie Coleman Rothenberg"
  ],
  "description": "Jane Austen's classic novel about wit, family, and first impressions.",
  "releaseYear": 1813,
  "coverUrl": "https://archive.org/covers/pride.jpg",
  "sourceUrl": "https://librivox.org/pride-and-prejudice-by-jane-austen/",
  "externalGenres": ["Romance"],
  "externalSubjects": ["English"],
  "attribution": "LibriVox public domain audiobook catalog"
}
```

Game search response example:

```json
{
  "query": "elden ring",
  "mediaType": "GAME",
  "source": "RAWG",
  "warnings": [],
  "results": [
    {
      "source": "RAWG",
      "externalId": "3498",
      "mediaType": "GAME",
      "title": "Elden Ring",
      "originalTitle": null,
      "creatorNames": [
        "Developer: FromSoftware",
        "Publisher: Bandai Namco Entertainment"
      ],
      "description": "Rise, Tarnished, and be guided by grace.",
      "releaseYear": 2022,
      "coverUrl": "https://media.rawg.io/media/games/elden-ring.jpg",
      "sourceUrl": "https://rawg.io/games/elden-ring",
      "externalGenres": ["Action", "RPG"],
      "externalSubjects": ["PC", "PlayStation 5", "Open World"],
      "suggestedTags": [],
      "attribution": "Metadata from RAWG. View source on RAWG for full provider details.",
      "warnings": []
    }
  ]
}
```

Game import request example:

```json
{
  "source": "RAWG",
  "externalId": "3498",
  "mediaType": "GAME",
  "title": "Elden Ring",
  "originalTitle": null,
  "creatorNames": [
    "Developer: FromSoftware",
    "Publisher: Bandai Namco Entertainment"
  ],
  "description": "Rise, Tarnished, and be guided by grace.",
  "releaseYear": 2022,
  "coverUrl": "https://media.rawg.io/media/games/elden-ring.jpg",
  "sourceUrl": "https://rawg.io/games/elden-ring",
  "externalGenres": ["Action", "RPG"],
  "externalSubjects": ["PC", "PlayStation 5", "Open World"],
  "attribution": "Metadata from RAWG. View source on RAWG for full provider details."
}
```

AniList anime series search response example:

```json
{
  "query": "attack on titan",
  "mediaType": "SERIES",
  "source": "ANILIST",
  "warnings": [],
  "results": [
    {
      "source": "ANILIST",
      "externalId": "16498",
      "mediaType": "SERIES",
      "title": "Shingeki no Kyojin",
      "originalTitle": "進撃の巨人",
      "creatorNames": ["Wit Studio"],
      "description": "Humanity fights titans beyond the walls.",
      "releaseYear": 2013,
      "coverUrl": "https://img.anilist.co/aot-large.jpg",
      "sourceUrl": "https://anilist.co/anime/16498",
      "externalGenres": ["Action", "Drama"],
      "externalSubjects": ["Format: TV", "Status: FINISHED", "Season: SPRING 2013", "Survival"],
      "suggestedTags": [],
      "attribution": "Metadata from AniList",
      "warnings": []
    }
  ]
}
```

AniList manga import request example:

```json
{
  "source": "ANILIST",
  "externalId": "30002",
  "mediaType": "BOOK",
  "title": "Berserk",
  "originalTitle": "ベルセルク",
  "creatorNames": ["Kentaro Miura"],
  "description": "A dark fantasy manga.",
  "releaseYear": 1989,
  "coverUrl": "https://img.anilist.co/berserk-large.jpg",
  "sourceUrl": "https://anilist.co/manga/30002",
  "externalGenres": ["Action", "Fantasy"],
  "externalSubjects": ["Format: MANGA", "Status: RELEASING", "Seinen"],
  "attribution": "Metadata from AniList"
}
```

Import response shape:

```json
{
  "created": true,
  "message": "Imported into your media library.",
  "media": {
    "id": "5b0f51ef-1de4-49fa-8453-bac3fd8b38fd",
    "title": "Arrival",
    "mediaType": "FILM",
    "consumptionStatus": "WANT_TO_CONSUME",
    "sourceType": "EXTERNAL_SEARCH",
    "commitmentLevel": "MEDIUM",
    "externalSourceName": "TMDB",
    "externalSourceId": "11",
    "externalSourceUrl": "https://www.themoviedb.org/movie/11",
    "metadataOrigin": "IMPORTED",
    "tags": [],
    "externalReferences": []
  }
}
```

Notes on book imports:

- `creatorNames` is part of the normalized external contract for display/import, but MoodMatch does not yet persist a dedicated author column on `media_items`.
- When an imported book has no provider description, the backend keeps a short fallback description so authorship and first-publish-year are not lost immediately after import.
- Imported LibriVox audiobooks stay `mediaType = AUDIOBOOK`; the prototype does not expose playback, chapters, streaming, or progress tracking.
- Imported RAWG games stay `mediaType = GAME`; this package does not add game-specific recommendation or matching logic.
- Imported AniList anime and manga stay in the existing model as `FILM`, `SERIES`, or `BOOK`; the API does not expose core `ANIME` or `MANGA` media types.

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
- External search results never affect scoring by themselves.
- Imported external media behaves like any other local media item after it is saved.

## Future Work Outside The Current Contract

The following are not part of the implemented API contract yet:

- `/api/dashboard`
- Dedicated decision-mode filter endpoints
- Persistent swipe-like/save endpoints
- YouTube integration

## Auth Mode Summary

- Public endpoint: `GET /api/health`
- Public for now: `GET /api/tags`
- Protected in `local-password` mode: `/api/media`, `/api/profile`, `/api/candidates`, `/api/matches`, `/api/external/*`, `/api/auth/me`, `/api/auth/logout`
- Backend tests still use `local-demo` where useful, while dedicated auth tests run `local-password`.

## Frontend Auth Expectations

- In `local-demo` frontend mode, private routes remain locally accessible and frontend API requests may omit `Authorization`.
- In `local-password` frontend mode, the SPA treats dashboard, media, profile, candidates, matches, swipe, and external search routes as protected.
- When the frontend has a MoodMatch token, it should call protected backend endpoints with `Authorization: Bearer <token>`.
- A backend `401 Unauthorized` should be treated as a login-required or session-expired state on the frontend.
