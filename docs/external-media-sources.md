# External Media Sources

## Active Provider

- `TMDB` is the first real external provider.
- Scope today: `FILM` and `SERIES`.
- Backend-only configuration: `MOODMATCH_TMDB_API_KEY`.
- Frontend never stores or sends the TMDB API key.

## Fallback Behavior

- When `MOODMATCH_TMDB_API_KEY` is configured, film and series searches default to `TMDB`.
- When the key is missing, MoodMatch falls back to the offline `DEMO` provider and returns a clear warning in the search response.
- `BOOK` and `GAME` searches still use `DEMO` for now.
- `DEMO` remains available for local development and tests.

## Search And Import Mapping

Search responses are normalized before they reach the frontend:

- `source`: the provider that actually answered the search (`TMDB` or `DEMO`)
- `externalId`: provider-specific identifier
- `title`, `originalTitle`, `description`, `releaseYear`, `coverUrl`, `sourceUrl`
- `externalGenres`, `externalSubjects`
- `suggestedTags`: local tag suggestions derived from `external_tag_mappings`

Imports create a normal user-owned `media_items` row with:

- `source_type = EXTERNAL_SEARCH`
- `metadata_origin = IMPORTED`
- `external_source_name`, `external_source_id`, `external_source_url`
- one `media_external_refs` row storing the provider id, URL, attribution, and payload hash
- mapped local tags when provider values match `external_tag_mappings`

## Current Source Mapping

- `TMDB` search results map directly to `external_source_name = TMDB`
- `DEMO` imports preserve `external_source_name = DEMO`
- `DEMO` tag suggestions still reuse the existing mapping sources:
  - `FILM` and `SERIES` -> `TMDB`
  - `BOOK` -> `OPEN_LIBRARY`
  - `GAME` -> `RAWG`

## Future Work

- Add more real providers behind the same backend adapter boundary.
- Expand beyond film and series once a stable provider choice exists.
- Keep YouTube as future work only. It is not part of the active scope for this package.
