# Data Model

This document describes the currently implemented MoodMatch persistence model. It matches the committed Flyway migrations in `backend/src/main/resources/db/migration/`.

## Database Setup

Local development uses PostgreSQL. Flyway runs automatically on backend startup, validates migration naming, and applies the schema before the app serves requests.

Implemented migrations:

- `V1__init_schema.sql`
- `V2__seed_starter_tags.sql`

## Tables

### `media_items`

Stores local media entries across films, series, books, and games.

| Column | Notes |
| --- | --- |
| `id` | UUID primary key |
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
(source_name, external_id)
```

### `external_tag_mappings`

Stores provider-value to local-tag mappings for the external preview foundation.

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
TMDB
OPEN_LIBRARY
RAWG
WIKIDATA
IGDB
GOOGLE_BOOKS
TVMAZE
```

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
