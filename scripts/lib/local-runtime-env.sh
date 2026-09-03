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

knoxx_require_node_timeout_ms() {
  local env_name="$1"
  local env_value="${!env_name:-}"
  local max_timeout_ms=2147483647
  local LC_ALL=C

  if [[ ! "$env_value" =~ ^[1-9][0-9]*$ ]] \
    || (( ${#env_value} > ${#max_timeout_ms} )) \
    || { (( ${#env_value} == ${#max_timeout_ms} )) \
         && [[ "$env_value" > "$max_timeout_ms" ]]; }; then
    knoxx_local_error "$env_name must be an integer between 1 and $max_timeout_ms"
    return 1
  fi
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
  export KNOXX_GENERATED_CONTRACTS_DIR="${KNOXX_GENERATED_CONTRACTS_DIR:-$HOME/.local/state/knoxx/generated-contracts}"
  export KNOXX_DISABLE_EVENT_RUNTIMES="${KNOXX_DISABLE_EVENT_RUNTIMES:-true}"

  export OPENPLANNER_BASE_URL="${OPENPLANNER_BASE_URL:-http://127.0.0.1:7777}"
  export PROXX_BASE_URL="${PROXX_BASE_URL:-http://127.0.0.1:8789}"
  export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://127.0.0.1:11434}"
  export OLLAMA_DEFAULT_MODEL="${OLLAMA_DEFAULT_MODEL:-gemma4:e2b}"
  export KNOXX_AGENT_MODEL_OVERRIDES="${KNOXX_AGENT_MODEL_OVERRIDES:-publication_translator=gemma4:e2b,publication_post_drafter=gemma4:e2b}"
  export KNOXX_AGENT_THINKING_OVERRIDES="${KNOXX_AGENT_THINKING_OVERRIDES:-publication_translator=off,publication_post_drafter=off}"
  export KNOXX_TRANSLATION_RUNNER="${KNOXX_TRANSLATION_RUNNER:-agent}"
  export KNOXX_EVENT_AGENT_CONCURRENCY="${KNOXX_EVENT_AGENT_CONCURRENCY:-1}"
  export KNOXX_EVENT_AGENT_QUEUE_LIMIT="${KNOXX_EVENT_AGENT_QUEUE_LIMIT:-256}"
  export KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS="${KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS-300000}"
  export EMBED_PROVIDER_BASE_URL="${EMBED_PROVIDER_BASE_URL:-$OLLAMA_BASE_URL}"
  export EMBED_PROVIDER_API_KEY="${EMBED_PROVIDER_API_KEY:-}"
  export EMBED_PROVIDER_MODEL="${EMBED_PROVIDER_MODEL:-nomic-embed-text}"
  export EMBED_PROVIDER_DIMENSIONS="${EMBED_PROVIDER_DIMENSIONS:-768}"
  export MONGODB_DB="${MONGODB_DB:-openplanner}"

  # Reject an invalid deployment liveness bound before credential discovery or
  # network access. Event workers must not silently start with an unbounded
  # stalled-provider slot.
  knoxx_require_node_timeout_ms KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS || return 1

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
