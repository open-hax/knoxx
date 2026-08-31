#!/usr/bin/env bash
# Boot an isolated Knoxx backend/frontend and run the browser translation
# contract used by pull-request CI. The API key is ephemeral and never printed.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_TMP_DIR="$(mktemp -d)"
BACKEND_PID=""
FRONTEND_PID=""
BACKEND_URL="${KNOXX_BASE_URL:-http://127.0.0.1:8000}"
FRONTEND_URL="${KNOXX_FRONTEND_URL:-http://127.0.0.1:5173}"
KNOXX_SHOT_DIR="${KNOXX_SHOT_DIR:-${VERIFY_TMP_DIR}/screenshots}"
KNOXX_PUBLICATION_CONTENT_ROOT="${KNOXX_PUBLICATION_CONTENT_ROOT:-${VERIFY_TMP_DIR}/publication-content}"
GENERATED_API_KEY="$(node -e \
  "process.stdout.write(require('node:crypto').randomBytes(32).toString('base64url'))")"
KNOXX_API_KEY="${KNOXX_API_KEY:-$GENERATED_API_KEY}"
KNOXX_API_KEY_USER_EMAIL="${KNOXX_API_KEY_USER_EMAIL:-pi@open-hax.local}"

export KNOXX_API_KEY KNOXX_API_KEY_USER_EMAIL KNOXX_PUBLICATION_CONTENT_ROOT KNOXX_SHOT_DIR
export KNOXX_BASE_URL="$BACKEND_URL" KNOXX_FRONTEND_URL="$FRONTEND_URL"
export CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
export KNOXX_CONTRACTS_DIR="$CONTRACTS_DIR"

note() { printf '   %s\n' "$1"; }
die() { printf 'ABORT %s\n' "$1" >&2; exit 2; }

stop_group() {
  local pid="$1"
  [ -z "$pid" ] || kill -TERM -- "-$pid" >/dev/null 2>&1 || true
}

cleanup() {
  local code=$?
  stop_group "$FRONTEND_PID"
  stop_group "$BACKEND_PID"
  if [ "$code" -ne 0 ]; then
    printf '\nbackend log (tail)\n' >&2
    tail -n 80 "${VERIFY_TMP_DIR}/backend.log" 2>/dev/null >&2 || true
    printf '\nfrontend log (tail)\n' >&2
    tail -n 80 "${VERIFY_TMP_DIR}/frontend.log" 2>/dev/null >&2 || true
    printf '\nfailed-run logs preserved at %s\n' "$VERIFY_TMP_DIR" >&2
  else
    rm -rf -- "$VERIFY_TMP_DIR"
  fi
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

for tool in curl jq node pnpm setsid; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: $tool"
done
[ -n "${MONGODB_URI:-}" ] || die "MONGODB_URI is required"
[ -n "${MONGODB_DB:-}" ] || die "MONGODB_DB is required"
[ -f "${REPO_ROOT}/backend/dist/server.js" ] \
  || die "backend/dist/server.js is absent; run pnpm -C backend run typecheck"
mkdir -p "$KNOXX_PUBLICATION_CONTENT_ROOT" "$KNOXX_SHOT_DIR"

note "starting isolated backend and frontend"
setsid bash -c '
  pid_file=$1
  shift
  printf "%s\n" "$$" >"$pid_file"
  exec "$@"
' _ "${VERIFY_TMP_DIR}/backend.pid" \
  env NODE_ENV=test KNOXX_DISABLE_EVENT_RUNTIMES=true \
  pnpm -C "${REPO_ROOT}/backend" start >"${VERIFY_TMP_DIR}/backend.log" 2>&1 &
setsid bash -c '
  pid_file=$1
  shift
  printf "%s\n" "$$" >"$pid_file"
  exec "$@"
' _ "${VERIFY_TMP_DIR}/frontend.pid" \
  pnpm -C "${REPO_ROOT}/frontend" dev >"${VERIFY_TMP_DIR}/frontend.log" 2>&1 &

# util-linux setsid forks when its caller is already a process-group leader.
# Capture the process inside the new session rather than the transient launcher
# so liveness checks and cleanup address the real backend/frontend groups on
# both local shells and GitHub-hosted runners.
for _ in $(seq 1 50); do
  if [ -s "${VERIFY_TMP_DIR}/backend.pid" ] \
     && [ -s "${VERIFY_TMP_DIR}/frontend.pid" ]; then
    break
  fi
  sleep 0.1
done
IFS= read -r BACKEND_PID <"${VERIFY_TMP_DIR}/backend.pid" \
  || die "backend process group did not start"
IFS= read -r FRONTEND_PID <"${VERIFY_TMP_DIR}/frontend.pid" \
  || die "frontend process group did not start"

context=""
context_status="000"
frontend_ready="false"
for _ in $(seq 1 120); do
  kill -0 "$BACKEND_PID" >/dev/null 2>&1 \
    || die "backend exited before the authenticated context became ready"
  kill -0 "$FRONTEND_PID" >/dev/null 2>&1 \
    || die "frontend exited before its browser surface became ready"

  context_status="$(curl -q --noproxy '*' -sS --max-time 2 \
    -o "${VERIFY_TMP_DIR}/auth-context.json" -w '%{http_code}' \
    -H "x-api-key: ${KNOXX_API_KEY}" "${BACKEND_URL}/api/auth/context" 2>/dev/null || true)"
  context="$(cat "${VERIFY_TMP_DIR}/auth-context.json" 2>/dev/null || true)"
  if curl -q --noproxy '*' -fsS --max-time 2 -H 'Accept: text/html' \
       "$FRONTEND_URL" >/dev/null 2>&1; then
    frontend_ready="true"
  fi
  if [ "$context_status" = "200" ] \
     && printf '%s' "$context" | jq -e '.org.id and .user.email' >/dev/null 2>&1 \
     && [ "$frontend_ready" = "true" ]; then
    break
  fi
  sleep 1
done

VERIFY_ORG_ID="$(printf '%s' "$context" | jq -er '.org.id' 2>/dev/null)" \
  || {
    context_error="$(printf '%s' "$context" | jq -cr \
      '{detail:(.detail // null),error:(.error // null),code:(.code // null)}' 2>/dev/null \
      || printf '{"detail":"unavailable","error":null,"code":null}')"
    die "authenticated Knoxx context did not become ready (HTTP ${context_status}; ${context_error})"
  }
KNOXX_USER_EMAIL="$(printf '%s' "$context" | jq -er '.user.email' 2>/dev/null)" \
  || die "authenticated Knoxx user did not become ready"
KNOXX_ORG_SLUG="$(printf '%s' "$context" | jq -er '.org.slug' 2>/dev/null)" \
  || die "authenticated Knoxx organization did not become ready"
[ "$frontend_ready" = "true" ] || die "frontend browser surface did not become ready"
export VERIFY_ORG_ID KNOXX_USER_EMAIL KNOXX_ORG_SLUG

note "running the translation review browser contract"
"${REPO_ROOT}/scripts/verify-translation-split-review-tour.sh"
