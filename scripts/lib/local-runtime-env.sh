#!/usr/bin/env bash

# Local Knoxx runtime environment discovery.
#
# This library exports connection settings but never prints credential values.
# Explicit environment variables always win. The discovery fallbacks target the
# local OpenPlanner/Proxx stack used by this workspace and can be overridden with
# KNOXX_OPENPLANNER_CONTAINER or PROXX_ENV_FILE.

knoxx_local_error() {
  printf 'ERROR  %s\n' "$*" >&2
}

knoxx_env_file_value() {
  local env_file="$1"
  local env_key="$2"
  node --env-file="$env_file" -e \
    'process.stdout.write(process.env[process.argv[1]] || "")' "$env_key"
}

knoxx_mongo_uri_from_container() {
  local container_name="$1"
  local container_uri

  container_uri="$(docker inspect "$container_name" \
    --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | sed -n 's/^MONGODB_URI=//p' \
    | head -n 1)"

  if [[ -z "$container_uri" ]]; then
    knoxx_local_error "container $container_name does not expose MONGODB_URI"
    return 1
  fi

  KNOXX_CONTAINER_MONGODB_URI="$container_uri" node -e '
    const uri = new URL(process.env.KNOXX_CONTAINER_MONGODB_URI);
    uri.hostname = "127.0.0.1";
    uri.port = "27017";
    uri.searchParams.set("directConnection", "true");
    process.stdout.write(uri.toString());
  '
}

configure_knoxx_local_runtime() {
  local script_dir knoxx_dir default_workspace proxx_env_file mongo_container
  local discovered_mongodb_uri discovered_proxx_token

  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  knoxx_dir="$(cd "$script_dir/../.." && pwd)"
  default_workspace="$(cd "$knoxx_dir/.." && pwd)"

  # Use a launcher-specific override so an unrelated shell/harness
  # WORKSPACE_ROOT cannot silently point Knoxx at a different checkout.
  export WORKSPACE_ROOT="${KNOXX_LOCAL_WORKSPACE_ROOT:-$default_workspace}"
  export WORKSPACE_PROJECT_NAME="${WORKSPACE_PROJECT_NAME:-$(basename "$WORKSPACE_ROOT")}"
  export CONTRACTS_DIR="${CONTRACTS_DIR:-$knoxx_dir/contracts}"
  export KNOXX_AGENT_DIR="${KNOXX_AGENT_DIR:-/tmp/knoxx-agent-local}"
  export KNOXX_DISABLE_EVENT_RUNTIMES="${KNOXX_DISABLE_EVENT_RUNTIMES:-true}"

  export OPENPLANNER_BASE_URL="${OPENPLANNER_BASE_URL:-http://127.0.0.1:7777}"
  export PROXX_BASE_URL="${PROXX_BASE_URL:-http://127.0.0.1:8789}"
  export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://127.0.0.1:11434}"
  export OLLAMA_DEFAULT_MODEL="${OLLAMA_DEFAULT_MODEL:-gemma4:e4b}"
  export MONGODB_DB="${MONGODB_DB:-openplanner}"

  if [[ -z "${MONGODB_URI:-}" ]]; then
    mongo_container="${KNOXX_OPENPLANNER_CONTAINER:-openplanner-openplanner-1}"
    if ! command -v docker >/dev/null 2>&1; then
      knoxx_local_error "MONGODB_URI is unset and Docker is unavailable for local discovery"
      return 1
    fi
    discovered_mongodb_uri="$(knoxx_mongo_uri_from_container "$mongo_container")"
    export MONGODB_URI="$discovered_mongodb_uri"
  fi

  if [[ -z "${PROXX_AUTH_TOKEN:-}" ]]; then
    proxx_env_file="${PROXX_ENV_FILE:-$(cd "$WORKSPACE_ROOT/.." && pwd)/proxx/.env}"
    if [[ ! -f "$proxx_env_file" ]]; then
      knoxx_local_error "PROXX_AUTH_TOKEN is unset and Proxx env file was not found: $proxx_env_file"
      knoxx_local_error "set PROXX_AUTH_TOKEN directly or set PROXX_ENV_FILE"
      return 1
    fi
    discovered_proxx_token="$(knoxx_env_file_value "$proxx_env_file" PROXY_AUTH_TOKEN)"
    export PROXX_AUTH_TOKEN="$discovered_proxx_token"
    if [[ -z "$PROXX_AUTH_TOKEN" ]]; then
      knoxx_local_error "PROXY_AUTH_TOKEN is missing from $proxx_env_file"
      return 1
    fi
  fi
}
