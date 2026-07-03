#!/usr/bin/env bash
set -euo pipefail

# Local development helper only. Loads the uncommitted root .env.local file
# and starts Quarkus dev mode for the backend.

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_dir="$(cd "${script_dir}/.." && pwd)"
repo_root="$(cd "${backend_dir}/.." && pwd)"
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

cd "${backend_dir}"
echo "Starting MoodMatch backend in local development mode with ${env_file}."
exec ./mvnw quarkus:dev
