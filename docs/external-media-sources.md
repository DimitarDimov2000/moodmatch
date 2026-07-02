# External Media Sources

## Active Providers

- `TMDB` is the active real provider for `FILM` and `SERIES`.
- `OPEN_LIBRARY` is the active real provider for `BOOK`.
- `LIBRIVOX` is the active real provider for `AUDIOBOOK`.
- `RAWG` is the active real provider for `GAME`.
- `ANILIST` is the active real provider for anime and manga metadata mapped into existing media types.
- `DEMO` remains available as an offline fallback and local test source.

Provider notes:

- `TMDB` requires backend-only configuration through `MOODMATCH_TMDB_API_KEY`.
- `OPEN_LIBRARY` uses the public Search API and does not require a committed secret.
- `LIBRIVOX` uses the public catalog API and does not require a committed secret in the current implementation.
- `RAWG` requires backend-only configuration through `MOODMATCH_RAWG_API_KEY`.
- `ANILIST` uses AniList GraphQL and does not require an API key.
- LibriVox search is intentionally limited to public-domain audiobooks in the catalog.
- RAWG is used only for this non-commercial university prototype. Keep provider attribution/backlinks visible and review RAWG terms before any production or commercial deployment.
- The frontend never stores or sends provider secrets.
- Tests must not require live external APIs or real API keys.

## Provider Plan

| Provider | Planned media coverage | Status |
| --- | --- | --- |
| `TMDB` | `FILM`, `SERIES` | Active |
| `OPEN_LIBRARY` | `BOOK` | Active |
| `LIBRIVOX` | `AUDIOBOOK` | Active |
| `RAWG` | `GAME` | Active |
| `ANILIST` | Anime and manga mapped into `FILM`, `SERIES`, or `BOOK` | Active |
| `PODCAST_INDEX` | `PODCAST` | Planned, without episode import for now |
| `YOUTUBE` | `VIDEO` URL import only | Planned, no YouTube search |
| `IGDB` | Backup/future game provider | Not active |
| Music providers | Music | Out of scope |

AniList does not introduce core `ANIME` or `MANGA` media types. Anime movies map to `FILM`, anime TV/OVA/ONA/special/short formats map to `SERIES`, and manga/light novel/novel/one-shot formats map to `BOOK`. The source stays `ANILIST`, and anime/manga meaning is preserved through provider metadata such as format, status, tags, source URL, and attribution.

## Fallback Behavior

- When `MOODMATCH_TMDB_API_KEY` is configured, film and series searches default to `TMDB`.
- When the key is missing, MoodMatch falls back to the offline `DEMO` provider and returns a clear warning in the search response.
- `BOOK` searches default to `OPEN_LIBRARY`.
- Explicit `source=ANILIST` searches are available for `FILM`, `SERIES`, and `BOOK`.
- `AUDIOBOOK` searches default to `LIBRIVOX`.
- `GAME` searches prefer `RAWG` when `MOODMATCH_RAWG_API_KEY` is configured.
- When the RAWG key is missing, automatic `GAME` searches fall back to the offline `DEMO` provider and return a clear warning. Explicit `source=RAWG` searches return a provider configuration error instead of silently falling back.
- `PODCAST` and `VIDEO` currently default to `DEMO` until their real providers are implemented.
- `DEMO` remains available for local development and tests.

## Search And Import Mapping

Search responses are normalized before they reach the frontend:

- `source`: the provider that actually answered the search
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

LibriVox audiobook mapping decisions:

- `externalId`: LibriVox audiobook `id`
- `title`: LibriVox `title`
- `creatorNames`: normalized author and reader summary strings, for example `Author: Jane Austen` and `Reader: Annie Coleman Rothenberg`
- `description`: LibriVox `description` with HTML stripped into plain text when present
- `releaseYear`: parsed from LibriVox `copyright_year` when numeric
- `coverUrl`: prefer `coverart_jpg`, otherwise `coverart_thumbnail`
- `sourceUrl`: LibriVox `url_librivox`
- `externalGenres`: LibriVox `genres`
- `externalSubjects`: current implementation preserves audiobook language, for example `English`
- `attribution`: `LibriVox public domain audiobook catalog`

RAWG game mapping decisions:

- `externalId`: RAWG numeric `id` when available, otherwise the stable `slug`
- `title`: RAWG `name`
- `originalTitle`: `null` because RAWG does not expose a separate original-title field in this search mapping
- `creatorNames`: detail-page developers and publishers when available, for example `Developer: FromSoftware` and `Publisher: Bandai Namco Entertainment`
- `description`: RAWG `description_raw` from the game detail payload when available
- `releaseYear`: parsed from RAWG `released`
- `coverUrl`: RAWG `background_image`
- `sourceUrl`: RAWG game page, for example `https://rawg.io/games/elden-ring`
- `externalGenres`: RAWG `genres`
- `externalSubjects`: a capped, de-duplicated list of platforms followed by tags so the UI can show platform/tag context without noisy payloads
- `attribution`: `Metadata from RAWG. View source on RAWG for full provider details.`

AniList anime/manga mapping decisions:

- `externalId`: AniList media `id`
- `mediaType`: anime `MOVIE` -> `FILM`; anime `TV`, `TV_SHORT`, `OVA`, `ONA`, `SPECIAL`, or `SHORT` -> `SERIES`; manga `MANGA`, `NOVEL`, or `ONE_SHOT` -> `BOOK`
- `title`: prefer AniList romaji title, otherwise English title
- `originalTitle`: AniList native title when available and distinct from the preferred title
- `creatorNames`: main studios for anime; staff/author names for manga and novels when available
- `description`: AniList description with HTML stripped into plain text
- `releaseYear`: AniList `startDate.year`
- `coverUrl`: prefer `coverImage.large`, otherwise `coverImage.medium`
- `sourceUrl`: AniList `siteUrl`
- `externalGenres`: AniList `genres`
- `externalSubjects`: capped list containing format, status, season/year when available, then non-spoiler AniList tags
- `attribution`: `Metadata from AniList`

Imports create a normal user-owned `media_items` row with:

- `source_type = EXTERNAL_SEARCH`
- `metadata_origin = IMPORTED`
- `external_source_name`, `external_source_id`, `external_source_url`
- one `media_external_refs` row storing the provider id, URL, attribution, and payload hash
- mapped local tags when provider values match `external_tag_mappings`

## Current Source Mapping

- `TMDB` search results map directly to `external_source_name = TMDB`.
- `OPEN_LIBRARY` search results map directly to `external_source_name = OPEN_LIBRARY`.
- `LIBRIVOX` search results map directly to `external_source_name = LIBRIVOX`.
- `RAWG` search results map directly to `external_source_name = RAWG`.
- `ANILIST` search results map directly to `external_source_name = ANILIST`.
- `DEMO` imports preserve `external_source_name = DEMO`.
- `DEMO` tag suggestions reuse provider-specific mapping sources:
  - `FILM` and `SERIES` -> `TMDB`
  - `BOOK` -> `OPEN_LIBRARY`
  - `GAME` -> `RAWG`
  - `AUDIOBOOK` -> `LIBRIVOX`
  - `PODCAST` -> `PODCAST_INDEX`
  - `VIDEO` -> `YOUTUBE`

## Guardrails

- Do not implement music providers.
- Do not implement IGDB unless the provider plan changes later.
- Do not implement YouTube search; keep YouTube scoped to future URL import.
- Do not import podcast episodes in the current provider plan.
- Do not add `ANIME` or `MANGA` as core media types.
- Do not build audio or video players as part of provider integration.
- Do not add audiobook playback, progress tracking, chapter state, or streaming UI in this prototype.
- Keep provider-specific HTTP clients behind the backend adapter boundary.
