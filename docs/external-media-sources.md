# External Media Sources

## Active Providers

- `TMDB` is the active real provider for `FILM` and `SERIES`.
- `OPEN_LIBRARY` is the active real provider for `BOOK`.
- `DEMO` remains available as an offline fallback and local test source.

Provider notes:

- `TMDB` requires backend-only configuration through `MOODMATCH_TMDB_API_KEY`.
- `OPEN_LIBRARY` uses the public Search API and does not require a committed secret.
- The frontend never stores or sends provider secrets.

## Fallback Behavior

- When `MOODMATCH_TMDB_API_KEY` is configured, film and series searches default to `TMDB`.
- When the key is missing, MoodMatch falls back to the offline `DEMO` provider and returns a clear warning in the search response.
- `BOOK` searches default to `OPEN_LIBRARY`.
- `GAME` searches still use `DEMO` for now.
- `DEMO` remains available for local development and tests.

## Search And Import Mapping

Search responses are normalized before they reach the frontend:

- `source`: the provider that actually answered the search (`TMDB`, `OPEN_LIBRARY`, or `DEMO`)
- `externalId`: provider-specific identifier
- `title`, `originalTitle`, `creatorNames`, `description`, `releaseYear`, `coverUrl`, `sourceUrl`
- `externalGenres`, `externalSubjects`
- `suggestedTags`: local tag suggestions derived from `external_tag_mappings`

Open Library book mapping decisions:

- `title`: Open Library `title`
- `creatorNames`: Open Library `author_name`
- `releaseYear`: Open Library `first_publish_year`
- `coverUrl`: Open Library cover id or edition OLID cover URL when available
- `externalId`: prefer work key, otherwise first matching edition key
- `sourceUrl`: prefer the Open Library work page, otherwise the edition page
- `description`: Open Library `first_sentence` when present, otherwise a short fallback such as `Book by Frank Herbert. First published in 1965.`

Imports create a normal user-owned `media_items` row with:

- `source_type = EXTERNAL_SEARCH`
- `metadata_origin = IMPORTED`
- `external_source_name`, `external_source_id`, `external_source_url`
- one `media_external_refs` row storing the provider id, URL, attribution, and payload hash
- mapped local tags when provider values match `external_tag_mappings`

## Current Source Mapping

- `TMDB` search results map directly to `external_source_name = TMDB`
- `OPEN_LIBRARY` search results map directly to `external_source_name = OPEN_LIBRARY`
- `DEMO` imports preserve `external_source_name = DEMO`
- `DEMO` tag suggestions still reuse the existing mapping sources:
  - `FILM` and `SERIES` -> `TMDB`
  - `BOOK` -> `OPEN_LIBRARY`
  - `GAME` -> `RAWG`

## Future Work

- Add more real providers behind the same backend adapter boundary.
- An optional third provider for a later package could target music or anime, for example `MusicBrainz` or `Jikan`.
- Keep YouTube as future work only. It is not part of the active scope for this package.
