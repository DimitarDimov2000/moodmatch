# Data Model

This document describes the planned database model for MoodMatch. Database migrations are not implemented yet.

MoodMatch will use PostgreSQL to store a locally closed media dataset. The planned model keeps media items, tags, and their relationships explicit so deterministic, explainable, rule-based matching can be calculated from local data.

## Tables

### media_items

Stores media entries that the user has consumed, wants to consume, rejected, abandoned, or is considering.

Planned fields include:

| Field | Purpose |
| --- | --- |
| `id` | Stable identifier for the media item. |
| `title` | Display title. |
| `media_type` | Type of media, such as film, series, book, or game. |
| `consumption_status` | User's current relationship to the item. |
| `source_type` | Where the item came from or how it was discovered. |
| `commitment_level` | Expected time or attention commitment. |
| `favorite` | Whether the item is marked as a favorite. |
| `rating` | Optional user rating or positive signal for profile calculation. |
| `created_at` | Creation timestamp. |
| `updated_at` | Last update timestamp. |

### tags

Stores the controlled tag vocabulary used for profile and matching calculations.

Planned fields include:

| Field | Purpose |
| --- | --- |
| `id` | Stable identifier for the tag. |
| `name` | Human-readable tag name. |
| `category` | Optional grouping for similar tags. |

### media_tags

Stores the many-to-many relationship between media items and tags.

Planned fields include:

| Field | Purpose |
| --- | --- |
| `media_item_id` | Reference to a media item. |
| `tag_id` | Reference to a tag. |

## Enums

### media_type

| Value |
| --- |
| `FILM` |
| `SERIES` |
| `BOOK` |
| `GAME` |

### consumption_status

| Value |
| --- |
| `CONSUMED` |
| `WANT_TO_CONSUME` |
| `NOT_INTERESTED` |
| `ABANDONED` |

### source_type

| Value |
| --- |
| `FRIEND` |
| `SOCIAL_MEDIA` |
| `ARTICLE` |
| `PLATFORM` |
| `MANUAL` |
| `UNKNOWN` |

### commitment_level

| Value |
| --- |
| `SHORT` |
| `MEDIUM` |
| `LONG` |
| `UNKNOWN` |
