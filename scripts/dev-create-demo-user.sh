#!/usr/bin/env bash
set -euo pipefail

# Local QA helper only. Uses the normal auth register/login endpoints against
# the local backend and never changes backend auth logic or database migrations.

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
env_file="${repo_root}/.env.local"

if [[ ! -f "${env_file}" ]]; then
  echo "Error: Missing ${env_file}." >&2
  echo "Copy .env.local.example to .env.local and fill in your local-only values first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${env_file}"
set +a

backend_base_url="${MOODMATCH_LOCAL_BACKEND_URL:-http://localhost:8080}"
demo_email="${MOODMATCH_DEMO_EMAIL:-}"
demo_password="${MOODMATCH_DEMO_PASSWORD:-}"
demo_display_name="${MOODMATCH_DEMO_DISPLAY_NAME:-Local Demo User}"

if [[ -z "${demo_email}" || -z "${demo_password}" ]]; then
  echo "Error: MOODMATCH_DEMO_EMAIL and MOODMATCH_DEMO_PASSWORD must be set in ${env_file}." >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl is required for local demo-user setup." >&2
  exit 1
fi

json_escape() {
  local value="${1}"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "${value}"
}

register_payload="$(
  printf '{"email":"%s","password":"%s","displayName":"%s"}' \
    "$(json_escape "${demo_email}")" \
    "$(json_escape "${demo_password}")" \
    "$(json_escape "${demo_display_name}")"
)"

login_payload="$(
  printf '{"email":"%s","password":"%s"}' \
    "$(json_escape "${demo_email}")" \
    "$(json_escape "${demo_password}")"
)"

register_body="$(mktemp)"
login_body="$(mktemp)"
trap 'rm -f "${register_body}" "${login_body}"' EXIT

register_status="$(
  curl \
    --silent \
    --show-error \
    --write-out '%{http_code}' \
    --output "${register_body}" \
    --header 'Content-Type: application/json' \
    --data "${register_payload}" \
    "${backend_base_url}/api/auth/register"
)" || {
  echo "Error: Backend is not reachable at ${backend_base_url}." >&2
  echo "Start it first with: cd backend && ./scripts/dev-local.sh" >&2
  exit 1
}

case "${register_status}" in
  200|201)
    echo "Local demo user created successfully."
    ;;
  409)
    echo "Local demo user already exists. Verifying the configured credentials with /api/auth/login."
    login_status="$(
      curl \
        --silent \
        --show-error \
        --write-out '%{http_code}' \
        --output "${login_body}" \
        --header 'Content-Type: application/json' \
        --data "${login_payload}" \
        "${backend_base_url}/api/auth/login"
    )"

    if [[ "${login_status}" != "200" && "${login_status}" != "201" ]]; then
      echo "Warning: The user exists, but login with MOODMATCH_DEMO_PASSWORD failed (HTTP ${login_status})." >&2
      echo "Update .env.local to match the existing local account or reset that account in your local database." >&2
      exit 1
    fi

    echo "Existing local demo user credentials are valid."
    ;;
  *)
    echo "Error: Demo-user registration failed with HTTP ${register_status}." >&2
    cat "${register_body}" >&2
    exit 1
    ;;
esac

echo
echo "Next steps:"
echo "- Open http://localhost:5173"
echo "- Log in with the local demo account from ${env_file}:"
echo "  email: ${demo_email}"
echo "  password: MOODMATCH_DEMO_PASSWORD from .env.local"
