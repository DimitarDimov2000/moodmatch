# Data Model

This document describes the currently implemented MoodMatch persistence model. It matches the committed Flyway migrations in `backend/src/main/resources/db/migration/`.

## Database Setup

Local development uses PostgreSQL. Flyway runs automatically on backend startup, validates migration naming, and applies the schema before the app serves requests.

Implemented migrations:

- `V1__init_schema.sql`
- `V2__seed_starter_tags.sql`
- `V3__add_app_users_and_media_ownership.sql`
- `V4__add_local_password_auth.sql`
- `V5__add_external_import_support.sql`
- `V6__expand_external_provider_model.sql`

## Tables

### `media_items`

Stores local media entries across films, series, books, games, audiobooks, podcasts, and videos. Each media item belongs to exactly one app user.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `user_id` | Required FK to `app_users` |
| `title` | Required display title |
| `original_title` | Optional original title |
| `description` | Optional description |
| `media_type` | Required enum-like string |
| `consumption_status` | Required enum-like string |
| `is_favourite` | Required boolean, default `false` |
| `rating` | Optional integer 1-5 |
| `source_type` | Required, default `UNKNOWN` |
| `source_note` | Optional note |
| `commitment_level` | Required, default `UNKNOWN` |
| `release_year` | Optional year |
| `cover_url` | Optional cover URL |
| `external_source_name` | Optional convenience source name |
| `external_source_id` | Optional convenience source id |
| `external_source_url` | Optional convenience source URL |
| `metadata_origin` | Required, default `MANUAL` |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

Important constraint:

- `is_favourite = true` is only valid when `consumption_status = CONSUMED` and `rating >= 4`
- `user_id` is required so media/profile/candidate/match data can be scoped per user

Index:

- `idx_media_items_user_id`

### `app_users`

Stores MoodMatch's internal user records. This is the ownership anchor for private media data.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `provider` | Required auth provider key; local password users use `LOCAL` |
| `provider_subject` | Required stable subject; local password users use normalized email |
| `email` | Optional email address, unique when present |
| `display_name` | Optional display name |
| `avatar_url` | Optional avatar URL |
| `password_hash` | Optional hash for local password users; never plaintext |
| `last_login_at` | Optional timestamp of last successful local-password login |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

Unique constraints:

```text
(provider, provider_subject)
email
```

Current behavior:

- the migration ensures a local/demo user exists with `provider = LOCAL` and `provider_subject = local-demo-user`
- local email/password users are also stored in `app_users`
- media/profile/candidates/matches remain scoped by `media_items.user_id`

### `auth_sessions`

Stores local password bearer sessions.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `user_id` | Required FK to `app_users` |
| `token_hash` | Unique SHA-256 hash of the opaque token |
| `expires_at` | Required expiration timestamp |
| `last_used_at` | Optional timestamp updated when a token is accepted |
| `revoked_at` | Optional timestamp set on logout |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

Raw bearer tokens are returned once to the frontend and are not stored in the database.

### `tags`

Stores the local MoodMatch tag vocabulary.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `name` | Required |
| `category` | Required enum-like string |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

Unique constraint:

```text
(name, category)
```

### `media_tags`

Stores confirmed media-to-tag assignments.

| Column | Notes |
| --- | --- |
| `media_id` | FK to `media_items` |
| `tag_id` | FK to `tags` |
| `created_at` | Required timestamp |

Primary key:

```text
(media_id, tag_id)
```

Ownership note:

- `media_tags` stay indirectly user-owned through `media_items.user_id`
- starter tags remain global and `tags` does not gain a `user_id` column in this phase

### `media_external_refs`

Stores external identities linked to local media.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `media_id` | FK to `media_items` |
| `source_name` | Required enum-like string |
| `external_id` | Required external id |
| `external_url` | Optional provider URL |
| `attribution_text` | Optional attribution |
| `source_payload_hash` | Optional hash/debug field |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

Unique constraint:

```text
(media_id, source_name, external_id)
```

Ownership note:

- external reference access is enforced through the owning `media_items` row
- the same external id can now be imported by different users without cross-user collisions
- imported Open Library books therefore store `OPEN_LIBRARY` in both `media_items.external_source_name` and `media_external_refs.source_name`
- imported LibriVox audiobooks store `LIBRIVOX` in both `media_items.external_source_name` and `media_external_refs.source_name`
- imported RAWG games store `RAWG` in both `media_items.external_source_name` and `media_external_refs.source_name`
- imported AniList anime/manga store `ANILIST` in both `media_items.external_source_name` and `media_external_refs.source_name`, while `media_items.media_type` remains `FILM`, `SERIES`, or `BOOK`
- imported Podcast Index podcast shows store `PODCAST_INDEX` in both `media_items.external_source_name` and `media_external_refs.source_name`, while `media_items.media_type` remains `PODCAST`
- imported YouTube videos store `YOUTUBE` in both `media_items.external_source_name` and `media_external_refs.source_name`, while `media_items.media_type` remains `VIDEO`

### `external_tag_mappings`

Stores provider-value to local-tag mappings for the external search and import foundation.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
| `source_name` | Required enum-like string |
| `external_field` | Required field label like `genre` or `subject` |
| `external_value` | Required provider value |
| `tag_id` | FK to `tags` |
| `confidence` | Required confidence enum-like string |
| `created_at` | Required timestamp |
| `updated_at` | Required timestamp |

## Enums And Allowed Values

### `media_type`

```text
FILM
SERIES
BOOK
GAME
AUDIOBOOK
PODCAST
VIDEO
```

### `consumption_status`

```text
CONSUMED
WANT_TO_CONSUME
NOT_INTERESTED
ABANDONED
```

### `source_type`

```text
FRIEND
SOCIAL_MEDIA
ARTICLE
PLATFORM
MANUAL
EXTERNAL_SEARCH
UNKNOWN
```

### `commitment_level`

```text
SHORT
MEDIUM
LONG
UNKNOWN
```

### `metadata_origin`

```text
MANUAL
IMPORTED
IMPORTED_AND_EDITED
```

### `external_source_name`

```text
DEMO
TMDB
OPEN_LIBRARY
RAWG
LIBRIVOX
PODCAST_INDEX
ANILIST
YOUTUBE
WIKIDATA
IGDB
GOOGLE_BOOKS
TVMAZE
```

Transport-level note:

- The normalized external search/import API now includes optional `creatorNames` for multi-source book metadata, but there is still no dedicated persisted author column on `media_items`.
- The same `creatorNames` transport field is also reused for audiobook author and reader summaries from LibriVox.
- The same `creatorNames` transport field is also reused for podcast author/owner metadata from Podcast Index.
- RAWG game imports also reuse `creatorNames` for developer and publisher summaries; no dedicated developer or publisher columns are persisted.
- AniList demonstrates the source/provider versus media-type distinction: the source can be `ANILIST`, but the core media type is still one of the existing values. Anime movies are `FILM`, anime series-style formats are `SERIES`, and manga/light novels are `BOOK`; there are no core `ANIME` or `MANGA` enum values.
- YouTube imports reuse `creatorNames` for the channel title. There is no dedicated persisted channel column on `media_items`.

### `tag_category`

```text
GENRE
THEME
SETTING
TONE
EXPERIENCE
```

### `tag_mapping_confidence`

```text
HIGH
MEDIUM
LOW
```

## Seeded Starter Tags

The committed starter migration seeds these tags:

### `GENRE`

- `Sci-Fi`
- `Drama`
- `Fantasy`
- `Mystery`
- `Romance`

### `THEME`

- `Space`
- `Survival`
- `Family`
- `Identity`
- `Politics`

### `TONE`

- `Thoughtful`
- `Emotional`
- `Dark`
- `Light`
- `Epic`

### `SETTING`

- `Future`
- `Historical`
- `Urban`
- `Nature`

### `EXPERIENCE`

- `Relaxing`
- `Intense`
- `Challenging`
- `Comfort`

## Current Behavioral Rules Reflected In The Model

- Media title, media type, and consumption status are required.
- Ratings must stay in the 1-5 range when present.
- Ratings are only valid for consumed media.
- Favourite requires consumed media with rating 4 or 5.
- Confirmed local tags are the tags used for profile calculation and matching.
- External suggested tags remain preview data until a future import flow exists.
