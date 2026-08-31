#!/usr/bin/env bash
# Boot an isolated Knoxx backend/frontend and run the browser translation
# contract used by pull-request CI. The API key is ephemeral and never printed.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_TMP_DIR="$(mktemp -d)"
BACKEND_PID=""
FRONTEND_PID=""
BACKEND_PORT="${KNOXX_BROWSER_BACKEND_PORT:-$(node -e '
  const server = require("node:net").createServer();
  server.unref();
  server.listen(0, "127.0.0.1", () => {
    process.stdout.write(String(server.address().port));
    server.close();
  });
')}"
BACKEND_URL="http://127.0.0.1:${BACKEND_PORT}"
FRONTEND_PORT="${KNOXX_BROWSER_FRONTEND_PORT:-5173}"
FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}"
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

stop_process() {
  local pid="$1"
  [ -z "$pid" ] || kill -TERM -- "$pid" >/dev/null 2>&1 || true
}

cleanup() {
  local code=$?
  stop_process "$FRONTEND_PID"
  stop_process "$BACKEND_PID"
  if [ "$code" -ne 0 ]; then
    # pnpm and Node can finish writing redirected diagnostics just after the
    # watched process exits; give the file descriptors a moment to drain.
    sleep 0.25
    printf '\nbackend log (tail)\n' >&2
    tail -n 80 "${VERIFY_TMP_DIR}/backend.log" 2>/dev/null >&2 || true
    printf '\nbackend diagnostic (tail)\n' >&2
    tail -n 120 "${VERIFY_TMP_DIR}/backend.diagnostic.log" 2>/dev/null >&2 || true
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

case "$FRONTEND_PORT" in
  ''|*[!0-9]*) die "KNOXX_BROWSER_FRONTEND_PORT must be an integer" ;;
esac
if [ "$FRONTEND_PORT" -lt 1 ] || [ "$FRONTEND_PORT" -gt 65535 ]; then
  die "KNOXX_BROWSER_FRONTEND_PORT must be between 1 and 65535"
fi

for tool in curl jq node pnpm; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: $tool"
done
[ -n "${MONGODB_URI:-}" ] || die "MONGODB_URI is required"
[ -n "${MONGODB_DB:-}" ] || die "MONGODB_DB is required"
[ -f "${REPO_ROOT}/backend/dist/server.js" ] \
  || die "backend/dist/server.js is absent; run pnpm -C backend run typecheck"
mkdir -p "$KNOXX_PUBLICATION_CONTENT_ROOT" "$KNOXX_SHOT_DIR"

note "building the browser frontend once before starting runtime processes"
{
  pnpm -C "${REPO_ROOT}/frontend" run build:bridge
  pnpm -C "${REPO_ROOT}/frontend" run build:app-bridge
  # Vite copies frontend/public (including compatibility CLJS output) into
  # dist. Run it before the authoritative compile so that stale development
  # artifacts cannot replace the browser-contract bundle.
  pnpm -C "${REPO_ROOT}/frontend" exec vite build
  pnpm -C "${REPO_ROOT}/frontend" exec shadow-cljs compile app \
    --config-merge '{:devtools {:enabled false}}'
  pnpm -C "${REPO_ROOT}/frontend" exec tailwindcss \
    -c tailwind.config.ts -i src/index.css -o dist/app.css
} >"${VERIFY_TMP_DIR}/frontend.log" 2>&1

# A hosted runner may keep a Shadow server JVM alive after a compile even
# though this contract only needs the emitted files. Reclaim it before loading
# the large backend module graph; otherwise the kernel can terminate Node before
# its first JavaScript instruction and leave no application diagnostic.
if [ "${CI:-}" = "true" ]; then
  pnpm -C "${REPO_ROOT}/frontend" exec shadow-cljs stop >/dev/null 2>&1 || true
  pnpm -C "${REPO_ROOT}/backend" exec shadow-cljs stop >/dev/null 2>&1 || true
fi

note "starting isolated backend on ${BACKEND_URL}"
(
  cd "${REPO_ROOT}/backend"
  exec env NODE_ENV=test KNOXX_DISABLE_EVENT_RUNTIMES=true PORT="$BACKEND_PORT" \
    KNOXX_BROWSER_BACKEND_DIAGNOSTIC_PATH="${VERIFY_TMP_DIR}/backend.diagnostic.log" \
    node "${REPO_ROOT}/scripts/start-browser-contract-backend.mjs"
) >"${VERIFY_TMP_DIR}/backend.log" 2>&1 &
BACKEND_PID=$!

context=""
context_status="000"
for _ in $(seq 1 120); do
  if ! kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    set +e
    wait "$BACKEND_PID"
    backend_status=$?
    set -e
    die "backend exited before the authenticated context became ready (status ${backend_status})"
  fi

  context_status="$(curl -q --noproxy '*' -sS --max-time 2 \
    -o "${VERIFY_TMP_DIR}/auth-context.json" -w '%{http_code}' \
    -H "x-api-key: ${KNOXX_API_KEY}" "${BACKEND_URL}/api/auth/context" 2>/dev/null || true)"
  context="$(cat "${VERIFY_TMP_DIR}/auth-context.json" 2>/dev/null || true)"
  if [ "$context_status" = "200" ] \
     && printf '%s' "$context" | jq -e '.org.id and .user.email' >/dev/null 2>&1; then
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
export VERIFY_ORG_ID KNOXX_USER_EMAIL KNOXX_ORG_SLUG

note "backend is ready; starting isolated frontend on ${FRONTEND_URL}"
(
  cd "${REPO_ROOT}/frontend"
  exec env VITE_KNOXX_BACKEND_URL="$BACKEND_URL" \
    node node_modules/vite/bin/vite.js preview \
      --host 127.0.0.1 --port "$FRONTEND_PORT" --strictPort
) >>"${VERIFY_TMP_DIR}/frontend.log" 2>&1 &
FRONTEND_PID=$!

frontend_ready="false"
for _ in $(seq 1 120); do
  if ! kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    set +e
    wait "$BACKEND_PID"
    backend_status=$?
    set -e
    die "backend exited while the frontend was starting (status ${backend_status})"
  fi
  if ! kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    set +e
    wait "$FRONTEND_PID"
    frontend_status=$?
    set -e
    die "frontend exited before its browser surface became ready (status ${frontend_status})"
  fi
  if curl -q --noproxy '*' -fsS --max-time 2 -H 'Accept: text/html' \
       "$FRONTEND_URL" >/dev/null 2>&1; then
    frontend_ready="true"
    break
  fi
  sleep 1
done
[ "$frontend_ready" = "true" ] || die "frontend browser surface did not become ready"

note "running the translation review browser contract"
"${REPO_ROOT}/scripts/verify-translation-split-review-tour.sh"
