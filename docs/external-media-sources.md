# External Media Sources

## Active Providers

- `TMDB` is the active real provider for `FILM` and `SERIES`.
- `OPEN_LIBRARY` is the active real provider for `BOOK`.
- `LIBRIVOX` is the active real provider for `AUDIOBOOK`.
- `RAWG` is the active real provider for `GAME`.
- `PODCAST_INDEX` is the active real provider for `PODCAST` podcast-show search and import.
- `ANILIST` is the active real provider for anime and manga metadata mapped into existing media types.
- `YOUTUBE` is the active real provider for `VIDEO` query search and URL import through the official YouTube Data API.
- `DEMO` remains available as an offline fallback and local test source.

Provider notes:

- `TMDB` requires backend-only configuration through `MOODMATCH_TMDB_API_KEY`, and the expected value is the TMDB v3 API key.
- `OPEN_LIBRARY` uses the public Search API and does not require a committed secret.
- `LIBRIVOX` uses the public catalog API and does not require a committed secret in the current implementation.
- `RAWG` requires backend-only configuration through `MOODMATCH_RAWG_API_KEY`.
- `PODCAST_INDEX` requires backend-only configuration through `MOODMATCH_PODCASTINDEX_KEY` and `MOODMATCH_PODCASTINDEX_SECRET`.
- `ANILIST` uses AniList GraphQL and does not require an API key.
- `YOUTUBE` requires backend-only configuration through `MOODMATCH_YOUTUBE_API_KEY`.
- LibriVox search is intentionally limited to public-domain audiobooks in the catalog.
- RAWG is used only for this non-commercial university prototype. Keep provider attribution/backlinks visible and review RAWG terms before any production or commercial deployment.
- The frontend never stores or sends provider secrets.
- Tests must not require live external APIs or real API keys.

## Package 2.12 Verification Notes

Live verification before the final Package 2.12 fixes showed:

- `RAWG` worked with a real backend key.
- `PODCAST_INDEX` worked with a real backend key and secret.
- `YOUTUBE` query search worked with a real backend key.
- `OPEN_LIBRARY` worked without a key.
- `LIBRIVOX` worked without a key.
- `ANILIST` worked without a key.
- `TMDB` initially failed because the backend property was not mapped from `MOODMATCH_TMDB_API_KEY`.
- `YOUTUBE` URL import initially failed in live verification and is now guarded by stricter backend metadata cleanup plus the supported URL parser.

Current code expectations after Package 2.12:

- TMDB reads `MOODMATCH_TMDB_API_KEY` on the backend only and uses it as the TMDB v3 `api_key` query parameter.
- YouTube query search and URL import both use the same backend-only `MOODMATCH_YOUTUBE_API_KEY` configuration.
- API responses must never include configured provider key values.

## Provider Plan

| Provider | Planned media coverage | Status |
| --- | --- | --- |
| `TMDB` | `FILM`, `SERIES` | Active |
| `OPEN_LIBRARY` | `BOOK` | Active |
| `LIBRIVOX` | `AUDIOBOOK` | Active |
| `RAWG` | `GAME` | Active |
| `PODCAST_INDEX` | `PODCAST` show/feed import only | Active |
| `ANILIST` | Anime and manga mapped into `FILM`, `SERIES`, or `BOOK` | Active |
| `YOUTUBE` | `VIDEO` query search and URL import | Active |
| `IGDB` | Backup/future game provider | Not active |
| Music providers | Music | Out of scope |

AniList does not introduce core `ANIME` or `MANGA` media types. Anime movies map to `FILM`, anime TV/OVA/ONA/special/short formats map to `SERIES`, and manga/light novel/novel/one-shot formats map to `BOOK`. The source stays `ANILIST`, and anime/manga meaning is preserved through provider metadata such as format, status, tags, source URL, and attribution.

## Fallback Behavior

- Automatic search means the `source` query parameter is omitted or set to `AUTOMATIC`. The response-level source is then `AUTOMATIC`, while each result keeps its real provider in `results[*].source`.
- Automatic `FILM` and `SERIES` searches query `TMDB` and `ANILIST` in that order. This allows regular film/series results and anime film/series results to appear together without adding `ANIME` as a core media type.
- Automatic `BOOK` searches query `OPEN_LIBRARY` and `ANILIST` in that order. This allows book results and manga/light novel results to appear together without adding `MANGA` as a core media type.
- Automatic `AUDIOBOOK` searches query `LIBRIVOX`.
- Automatic `GAME` searches query `RAWG` when `MOODMATCH_RAWG_API_KEY` is configured.
- Automatic `PODCAST` searches query `PODCAST_INDEX` when `MOODMATCH_PODCASTINDEX_KEY` and `MOODMATCH_PODCASTINDEX_SECRET` are configured.
- Automatic mode keeps explicit provider behavior unchanged, but applies the backend limit per provider before merging so one source does not dominate the mixed result set.
- Explicit provider search, such as `source=TMDB`, `source=OPEN_LIBRARY`, or `source=ANILIST`, searches only that provider and keeps the existing provider-specific error behavior.
- Explicit `source=PODCAST_INDEX` searches only Podcast Index podcast shows/feeds.
- Explicit `source=YOUTUBE` searches support `sort=relevance`, `sort=newest`, or `sort=most_viewed`.
- When a compatible provider is unconfigured in automatic mode, MoodMatch skips it, records a non-blocking warning, and returns partial results from other compatible providers when possible.
- When the RAWG key is missing, automatic `GAME` searches fall back to the offline `DEMO` provider and return a clear warning. Explicit `source=RAWG` searches return a provider configuration error instead of silently falling back.
- When all compatible real providers for `FILM`, `SERIES`, `BOOK`, `AUDIOBOOK`, or `GAME` are unavailable and `DEMO` can cover the media type, MoodMatch uses `DEMO` as the fallback.
- When Podcast Index credentials are missing, automatic `PODCAST` searches return an empty result set plus a clear warning instead of crashing or silently falling back.
- `VIDEO` still has no active automatic multi-provider search. Use explicit `source=YOUTUBE` for official YouTube query search, or the dedicated YouTube URL resolver when you already have a link or raw id.
- `DEMO` remains available for local development and tests.

## Search And Import Mapping

Search responses are normalized before they reach the frontend:

- `source`: the provider that actually answered the search
- `externalId`: provider-specific identifier
- `title`, `originalTitle`, `creatorNames`, `description`, `releaseYear`, `coverUrl`, `sourceUrl`
- `externalGenres`, `externalSubjects`
- `suggestedTags`: internal MoodMatch tag suggestions. Explicit `external_tag_mappings` are preferred; when no explicit mapping exists, the backend may add low-confidence fallback suggestions from normalized `externalGenres` and safe `externalSubjects`.

Provider metadata is preserved separately from tag suggestions. `externalGenres` and `externalSubjects` keep useful provider context for display/import auditing, while `suggestedTags` is the local normalized suggestion layer shown to the user. Confidence stays available internally and in the API payload, but the normal result-card UI renders suggested tags as clean pills without raw `LOW`, `MEDIUM`, or `HIGH` labels.

Provider overlap is expected in automatic mode. The UI should therefore rely on three separate ideas at once:

- `mediaType`: MoodMatch core type such as `FILM`, `SERIES`, `BOOK`, `GAME`, `AUDIOBOOK`, `PODCAST`, or `VIDEO`
- `source`: the actual provider that produced the row, such as `TMDB`, `OPEN_LIBRARY`, `ANILIST`, or `RAWG`
- subtype/display hint: provider-specific context like `Anime movie`, `Anime series`, `Manga`, or `Book`

These display hints help mixed result sets stay understandable without introducing new core media types.

Automatic provider matrix:

| Media type | Automatic providers |
| --- | --- |
| `FILM` | `TMDB`, `ANILIST`, then `DEMO` only if all compatible real providers are unavailable |
| `SERIES` | `TMDB`, `ANILIST`, then `DEMO` only if all compatible real providers are unavailable |
| `BOOK` | `OPEN_LIBRARY`, `ANILIST`, then `DEMO` only if all compatible real providers are unavailable |
| `AUDIOBOOK` | `LIBRIVOX`, then `DEMO` only if all compatible real providers are unavailable |
| `GAME` | `RAWG`, then `DEMO` only if all compatible real providers are unavailable |
| `PODCAST` | `PODCAST_INDEX` only; no automatic `DEMO` fallback |
| `VIDEO` | no active automatic search provider; use explicit `YOUTUBE` search or the dedicated `YOUTUBE` URL resolver/import flow |

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

Podcast Index podcast mapping decisions:

- `externalId`: Podcast Index numeric feed `id` when available, otherwise a stable feed URL
- `mediaType`: always `PODCAST`
- imported unit: one podcast show/feed only, never individual episodes
- `title`: Podcast Index feed `title`
- `originalTitle`: `null` in the current normalized mapping
- `creatorNames`: `author` and `ownerName` when available and distinct
- `description`: Podcast Index feed `description` with HTML stripped into plain text
- `releaseYear`: derived from `newestItemPubdate` only when it looks reliable
- `coverUrl`: prefer `artwork`, otherwise `image`
- `sourceUrl`: prefer website `link`, otherwise feed URL
- `externalGenres`: Podcast Index categories when available
- `externalSubjects`: compact provider context such as language, explicit flag, and feed type
- `attribution`: `Metadata from Podcast Index`

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

YouTube video mapping decisions:

- search API: official YouTube Data API `search.list` with `part=snippet`, `type=video`, a backend-side default of `maxResults=10`, and explicit sort support for `relevance`, `newest`, or `most_viewed`
- `most_viewed` maps to the official YouTube API `order=viewCount`; this is view count, not click count
- `externalId`: YouTube video id from `id.videoId` for query search, or resolved from a supported watch URL, short URL, Shorts URL, embed URL, or raw id for URL import
- `mediaType`: always `VIDEO`
- `title`: YouTube `snippet.title`
- `originalTitle`: `null`
- `creatorNames`: YouTube `snippet.channelTitle` when available
- `description`: sanitized YouTube `snippet.description`
- `releaseYear`: parsed from `snippet.publishedAt`
- `coverUrl`: best available official YouTube thumbnail URL, preferring maxres down to default
- `sourceUrl`: canonical watch URL, for example `https://www.youtube.com/watch?v=abc123XYZ_0`
- `externalGenres`: the resolved YouTube category label when available for URL import; query search keeps this empty because `search.list` snippet payloads do not include category metadata
- `externalSubjects`: YouTube tags plus compact channel/category context when useful for URL import; query search keeps compact channel context when available
- URL import accepts standard watch URLs, watch URLs with extra query parameters, `youtu.be` URLs, Shorts URLs, embed URLs, and raw 11-character video ids. The backend resolves metadata through the same configured YouTube gateway/API key path as query search, uses `videos.list` with only `part=snippet`, `id`, and `key`, and treats optional category-label lookup failures as non-fatal so preview/import can still continue.
- URL-import metadata is trimmed and capped to stay compatible with the backend import validation limits
- `attribution`: `Metadata from YouTube`
- only official YouTube Data API metadata is used; there is no scraping
- imported YouTube search results create normal user-owned `MediaItem` rows and preserve `external_source_name = YOUTUBE` plus `media_external_refs` metadata the same way URL imports do

Imports create a normal user-owned `media_items` row with:

- `source_type = EXTERNAL_SEARCH`
- `metadata_origin = IMPORTED`
- `external_source_name`, `external_source_id`, `external_source_url`
- one `media_external_refs` row storing the provider id, URL, attribution, and payload hash
- mapped local tags when provider values match `external_tag_mappings`

Tag mapping uses light normalization before matching provider metadata against `external_tag_mappings`:

- trims whitespace
- lowercases the internal comparison key
- collapses duplicates
- normalizes common punctuation and hyphen variants
- maps a small set of obvious synonyms such as `sci-fi` -> `science fiction`, `kids` -> `children`, and `tv` -> `television`
- drops obviously noisy subject labels such as provider status/format markers from tag matching

This normalization affects tag matching and import cleanup only. Original provider text still remains the display/source text returned by the provider, and MoodMatch does not automatically translate provider metadata. Full i18n and metadata translation remain future polish.

## Current Source Mapping

- `TMDB` search results map directly to `external_source_name = TMDB`.
- `OPEN_LIBRARY` search results map directly to `external_source_name = OPEN_LIBRARY`.
- `LIBRIVOX` search results map directly to `external_source_name = LIBRIVOX`.
- `RAWG` search results map directly to `external_source_name = RAWG`.
- `PODCAST_INDEX` search results map directly to `external_source_name = PODCAST_INDEX`.
- `ANILIST` search results map directly to `external_source_name = ANILIST`.
- `YOUTUBE` query-search imports and URL imports map directly to `external_source_name = YOUTUBE`.
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
- Do not import podcast episodes in the current provider plan.
- Do not build RSS crawling from Podcast Index feed URLs in this prototype.
- Do not add `ANIME` or `MANGA` as core media types.
- Do not build audio or video players as part of provider integration.
- Do not build podcast playback or progress tracking in this prototype.
- Do not add audiobook playback, progress tracking, chapter state, or streaming UI in this prototype.
- Keep provider-specific HTTP clients behind the backend adapter boundary.
