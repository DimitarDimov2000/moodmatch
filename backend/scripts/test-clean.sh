#!/usr/bin/env bash
set -euo pipefail

# Local test helper only. Runs backend tests without loading .env.local so
# developer secrets or auth overrides do not leak into the test profile.

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_dir="$(cd "${script_dir}/.." && pwd)"

vars_to_unset=(
  MOODMATCH_AUTH_MODE
  MOODMATCH_PRIVATE_ENDPOINT_POLICY
  MOODMATCH_DB_URL
  MOODMATCH_DB_USERNAME
  MOODMATCH_DB_PASSWORD
  MOODMATCH_TMDB_API_KEY
  MOODMATCH_RAWG_API_KEY
  MOODMATCH_PODCASTINDEX_KEY
  MOODMATCH_PODCASTINDEX_SECRET
  MOODMATCH_YOUTUBE_API_KEY
  QUARKUS_HTTP_CORS_ENABLED
  QUARKUS_HTTP_CORS_ORIGINS
  QUARKUS_HTTP_CORS_METHODS
  QUARKUS_HTTP_CORS_HEADERS
  MOODMATCH_OIDC_ENABLED
  MOODMATCH_OIDC_ISSUER_URL
  MOODMATCH_OIDC_CLIENT_ID
  MOODMATCH_OIDC_AUDIENCE
  QUARKUS_OIDC_ENABLED
  QUARKUS_OIDC_PROVIDER
  QUARKUS_OIDC_AUTH_SERVER_URL
  QUARKUS_OIDC_CLIENT_ID
  QUARKUS_OIDC_APPLICATION_TYPE
)

for var_name in "${vars_to_unset[@]}"; do
  unset "${var_name}" || true
done

cd "${backend_dir}"
echo "Running backend tests with a clean environment. Root .env.local is intentionally not loaded."
exec ./mvnw test
