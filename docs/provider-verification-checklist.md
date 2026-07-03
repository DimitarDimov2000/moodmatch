# Provider Verification Checklist

Use this checklist for the Package 2.12 closeout pass and for local deployment smoke tests.

## Backend Startup

- Export local password auth environment variables.
- Export `QUARKUS_HTTP_CORS_ENABLED=true`.
- Export `QUARKUS_HTTP_CORS_ORIGINS=http://localhost:5173`.
- Keep all provider keys and secrets in the backend environment only.
- Start the backend with `cd backend && ./mvnw quarkus:dev`.

## Frontend Flow

- Log in through the frontend with local email/password auth.
- Confirm dashboard and profile still load.
- Confirm provider keys are not visible in browser code or network responses.

## Provider Checks

- `FILM` + `TMDB` + `Dune`
- `SERIES` + `TMDB` + `Dark`
- `GAME` + `RAWG` + `Elden Ring`
- `PODCAST` + `PODCAST_INDEX` + `Lex Fridman`
- `VIDEO` + `YOUTUBE` + `AI tutorial`
- Confirm explicit YouTube sort behavior for `relevance`, `newest`, or `most_viewed`
- Copy a `sourceUrl` from a YouTube search result and paste that exact URL into YouTube URL import; it must resolve to a preview with title, channel, and thumbnail.
- Resolve and import a standard YouTube watch URL
- Resolve and import a `youtu.be` short URL
- Resolve and import a Shorts URL if available
- `BOOK` + `OPEN_LIBRARY` + `Pride and Prejudice`
- `AUDIOBOOK` + `LIBRIVOX` + `Pride and Prejudice`
- `SERIES` + `ANILIST` + `Demon Slayer`
- `BOOK` + `ANILIST` + `Berserk`

## Import Checks

- Import at least one item from each provider used in the session.
- Confirm imported items appear in the media library.
- Confirm YouTube imports preserve `coverUrl`, `creatorNames`, and `externalSourceName = YOUTUBE`.
- Confirm suggested tags appear when useful provider genres or safe subjects exist, and that noisy provider context such as status, format, explicit flags, feed type, channels, and raw URLs is not suggested as a tag.
- Confirm suggested tags render as clean tag pills in the normal UI and do not show raw `LOW`, `MEDIUM`, or `HIGH` confidence labels.

## Security Checks

- Do not paste real keys into the repository, docs, or tests.
- Confirm API errors mention environment variable names only, not secret values.
- Confirm provider responses never include configured key values.
