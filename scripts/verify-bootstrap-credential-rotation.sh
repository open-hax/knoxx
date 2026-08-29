#!/usr/bin/env bash
#
# Live verification for bootstrap administrator credential reconciliation.
#
# The verifier starts this checkout's built Knoxx server against two uniquely
# named throwaway databases on a transaction-capable Mongo deployment. It
# proves rotation, blank-password revocation, and rollback on replacement
# failure, then drops both databases and removes all local evidence on exit.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MONGO_URI="${KNOXX_BOOTSTRAP_VERIFY_MONGODB_URI:-${MONGODB_URI:-}}"
RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
ROTATION_DB="knoxx_bootstrap_verify_rotation_${RUN_ID}"
FAILURE_DB="knoxx_bootstrap_verify_failure_${RUN_ID}"
EVIDENCE_DIR="$(mktemp -d "${TMPDIR:-/tmp}/knoxx-bootstrap-rotation.XXXXXX")"
BUILD_ROOT="${EVIDENCE_DIR}/reviewed-checkout"
SERVER_ENTRY="${BUILD_ROOT}/backend/dist/server.js"

OLD_EMAIL="bootstrap-old-${RUN_ID}@open-hax.local"
NEW_EMAIL="bootstrap-new-${RUN_ID}@open-hax.local"
OLD_PASSWORD="Verify-old-${RUN_ID}!"
NEW_PASSWORD="Verify-new-${RUN_ID}!"
SESSION_SECRET="knoxx-bootstrap-verifier-session-secret-${RUN_ID}"

PASS_COUNT=0
FAIL_COUNT=0
FAILURES=()
SERVER_PID=""
SERVER_PORT=""
SERVER_LOG=""
TEARDOWN_FAILED=0

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf 'PASS  %s\n' "$1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAILURES+=("$1")
  printf 'FAIL  %s\n' "$1"
  if [ -n "${2:-}" ]; then printf '      got: %s\n' "$2"; fi
}

die() {
  printf 'ABORT %s\n' "$1" >&2
  exit 2
}

mongo_eval() {
  local database="$1" expression="$2"
  mongosh "$MONGO_URI" --quiet --eval \
    "const target = db.getSiblingDB('${database}'); ${expression}"
}

free_port() {
  node -e '
    const net = require("node:net");
    const server = net.createServer();
    server.listen(0, "127.0.0.1", () => {
      console.log(server.address().port);
      server.close();
    });
  '
}

stop_server() {
  if [ -z "$SERVER_PID" ]; then return; fi
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    kill -TERM "$SERVER_PID" 2>/dev/null || true
    local attempt
    for attempt in $(seq 1 50); do
      if ! kill -0 "$SERVER_PID" 2>/dev/null; then break; fi
      sleep 0.1
    done
    if kill -0 "$SERVER_PID" 2>/dev/null; then
      kill -KILL "$SERVER_PID" 2>/dev/null || true
    fi
  fi
  wait "$SERVER_PID" 2>/dev/null || true
  SERVER_PID=""
}

drop_fixture_db() {
  local database="$1"
  case "$database" in
    knoxx_bootstrap_verify_rotation_[0-9]*|knoxx_bootstrap_verify_failure_[0-9]*) ;;
    *) fail "refused unsafe fixture database teardown" "$database"; return ;;
  esac
  mongo_eval "$database" 'target.dropDatabase();' >/dev/null 2>&1 \
    || {
      fail "drop throwaway database ${database}"
      TEARDOWN_FAILED=1
    }
}

cleanup() {
  local code=$?
  local signalled="${1:-}"
  if [ -n "$signalled" ]; then code="$signalled"; fi
  stop_server
  if [ -n "$MONGO_URI" ] && command -v mongosh >/dev/null 2>&1; then
    drop_fixture_db "$ROTATION_DB"
    drop_fixture_db "$FAILURE_DB"
  fi
  if [ -d "$EVIDENCE_DIR" ]; then rm -rf "$EVIDENCE_DIR"; fi
  if [ "$TEARDOWN_FAILED" -ne 0 ] && [ "$code" -eq 0 ]; then code=1; fi
  exit "$code"
}

trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

start_server() {
  local database="$1" email="$2" password="$3" previous_emails="$4" label="$5"
  stop_server
  SERVER_PORT="$(free_port)"
  SERVER_LOG="${EVIDENCE_DIR}/${label}.log"
  (
    cd "$BUILD_ROOT" || exit 1
    exec env \
      NODE_ENV=production \
      HOST=127.0.0.1 \
      PORT="$SERVER_PORT" \
      KNOXX_BASE_URL="http://127.0.0.1:${SERVER_PORT}" \
      KNOXX_PUBLIC_BASE_URL="http://127.0.0.1:${SERVER_PORT}" \
      MONGODB_URI="$MONGO_URI" \
      MONGODB_DB="$database" \
      KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL="$email" \
      KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME="Bootstrap verification administrator" \
      KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD="$password" \
      KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PREVIOUS_EMAILS="$previous_emails" \
      KNOXX_LOCAL_PASSWORD_AUTH_ENABLED=true \
      KNOXX_SESSION_SECRET="$SESSION_SECRET" \
      KNOXX_DISABLE_EVENT_RUNTIMES=true \
      KNOXX_SHUTDOWN_GRACE_MS=1000 \
      MCP_ENABLED=false \
      CONTRACTS_DIR="$BUILD_ROOT/contracts" \
      node "$SERVER_ENTRY"
  ) >"$SERVER_LOG" 2>&1 &
  SERVER_PID=$!
}

wait_for_ready() {
  local label="$1" attempt
  for attempt in $(seq 1 240); do
    if curl -q --noproxy '*' -fsS --max-time 1 \
      "http://127.0.0.1:${SERVER_PORT}/health" \
      >/dev/null 2>&1; then
      pass "$label starts this checkout and answers /health"
      return 0
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then break; fi
    sleep 0.25
  done
  fail "$label did not become ready" "$(tail -n 12 "$SERVER_LOG" | tr '\n' ' ')"
  return 1
}

expect_startup_failure() {
  local label="$1" attempt status
  for attempt in $(seq 1 240); do
    if curl -q --noproxy '*' -fsS --max-time 1 \
      "http://127.0.0.1:${SERVER_PORT}/health" \
      >/dev/null 2>&1; then
      fail "$label must not bind an HTTP listener"
      stop_server
      return 1
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      wait "$SERVER_PID" 2>/dev/null
      status=$?
      SERVER_PID=""
      if [ "$status" -ne 0 ]; then
        pass "$label exits nonzero before readiness"
        return 0
      fi
      fail "$label exited zero despite failed policy initialization"
      return 1
    fi
    sleep 0.25
  done
  fail "$label neither exited nor became ready" "$(tail -n 12 "$SERVER_LOG" | tr '\n' ' ')"
  stop_server
  return 1
}

login_response() {
  local email="$1" password="$2" label="$3"
  local body="${EVIDENCE_DIR}/${label}.json"
  local payload
  payload="$(jq -cn --arg email "$email" --arg password "$password" \
    '{email:$email,password:$password}')"
  curl -q --noproxy '*' -sS --max-time 10 -o "$body" -w '%{http_code}' \
    -H 'content-type: application/json' \
    --data "$payload" \
    "http://127.0.0.1:${SERVER_PORT}/api/auth/local/login" 2>/dev/null \
    || printf '000'
}

expect_login() {
  local label="$1" email="$2" password="$3" expected="$4" status
  status="$(login_response "$email" "$password" "$label")"
  if [ "$status" = "$expected" ]; then
    if [ "$expected" = 200 ]; then
      if jq -e --arg email "$email" \
        '.ok == true and .user.email == $email' \
        "${EVIDENCE_DIR}/${label}.json" >/dev/null 2>&1; then
        pass "$label authenticates the expected principal"
      else
        fail "$label returned 200 without the expected principal" \
          "$(head -c 300 "${EVIDENCE_DIR}/${label}.json")"
      fi
    else
      pass "$label is refused with HTTP ${expected}"
    fi
  else
    fail "$label expected HTTP ${expected}" "$status"
  fi
}

expect_count() {
  local label="$1" database="$2" filter="$3" expected="$4" actual
  actual="$(mongo_eval "$database" \
    "print(target.knoxx_actor_credentials.countDocuments(${filter}));" \
    | tail -n 1 | tr -d '\r')"
  if [ "$actual" = "$expected" ]; then
    pass "$label"
  else
    fail "$label expected ${expected}" "$actual"
  fi
}

printf 'Knoxx bootstrap credential rotation — live verification\n'
printf 'run id: %s\n' "$RUN_ID"

for tool in bash curl git jq mongosh node pnpm tar; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: ${tool}"
done
[ -n "$MONGO_URI" ] \
  || die 'set KNOXX_BOOTSTRAP_VERIFY_MONGODB_URI to an isolated, transaction-capable Mongo deployment'

REVIEWED_HEAD="$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null)" \
  || die "cannot resolve the reviewed checkout revision"
if [ -n "$(git -C "$REPO_ROOT" status --porcelain --untracked-files=normal)" ]; then
  die 'the checkout has uncommitted source files; commit or remove them before collecting revision-bound evidence'
fi
printf 'reviewed head: %s\n' "$REVIEWED_HEAD"

mkdir -p "$BUILD_ROOT" \
  || die "cannot create private reviewed-checkout directory"
if git -C "$REPO_ROOT" archive "$REVIEWED_HEAD" -- backend contracts shared \
  | tar -x -C "$BUILD_ROOT"; then
  pass "materialized reviewed head ${REVIEWED_HEAD} inside private build directory"
else
  die "could not materialize reviewed head ${REVIEWED_HEAD}"
fi
[ -d "$REPO_ROOT/backend/node_modules" ] \
  || die 'backend/node_modules is missing; install the reviewed dependencies first'
ln -s "$REPO_ROOT/backend/node_modules" "$BUILD_ROOT/backend/node_modules" \
  || die 'could not link reviewed dependencies into the private build directory'

if mongosh "$MONGO_URI" --quiet --eval '
  const hello = db.adminCommand({hello: 1});
  if (!(hello.setName || hello.msg === "isdbgrid")) quit(42);
' >/dev/null 2>&1; then
  pass "Mongo deployment advertises replica-set or sharded topology"
else
  die 'Mongo deployment is standalone or unreachable; atomic verification requires transactions'
fi

if pnpm -C "$BUILD_ROOT/backend" build >"${EVIDENCE_DIR}/build.log" 2>&1; then
  pass "rebuilt temporary backend/dist/server.js from reviewed head ${REVIEWED_HEAD}"
else
  die "backend build failed: $(tail -n 12 "${EVIDENCE_DIR}/build.log" | tr '\n' ' ')"
fi
[ -f "$SERVER_ENTRY" ] \
  || die "reviewed build did not produce ${SERVER_ENTRY}"

printf '\nRotation and revocation\n'
start_server "$ROTATION_DB" "$OLD_EMAIL" "$OLD_PASSWORD" "" seed-old
if wait_for_ready "prior credential bootstrap"; then
  expect_login old-before-rotation "$OLD_EMAIL" "$OLD_PASSWORD" 200
  expect_count "prior credential is active before rotation" "$ROTATION_DB" \
    "{account_identifier:'${OLD_EMAIL}',status:'active'}" 1
fi
stop_server

start_server "$ROTATION_DB" "$NEW_EMAIL" "$NEW_PASSWORD" "$OLD_EMAIL" rotate
if wait_for_ready "rotated credential bootstrap"; then
  expect_login old-after-rotation "$OLD_EMAIL" "$OLD_PASSWORD" 401
  expect_login new-after-rotation "$NEW_EMAIL" "$NEW_PASSWORD" 200
  expect_count "prior credential is inactive after rotation" "$ROTATION_DB" \
    "{account_identifier:'${OLD_EMAIL}',status:'active'}" 0
  expect_count "replacement credential is active after rotation" "$ROTATION_DB" \
    "{account_identifier:'${NEW_EMAIL}',status:'active'}" 1
fi
stop_server

start_server "$ROTATION_DB" "$NEW_EMAIL" "" "$OLD_EMAIL" revoke
if wait_for_ready "blank-password revocation bootstrap"; then
  expect_login old-after-revocation "$OLD_EMAIL" "$OLD_PASSWORD" 401
  expect_login new-after-revocation "$NEW_EMAIL" "$NEW_PASSWORD" 401
  expect_count "blank password leaves no managed active credential" "$ROTATION_DB" \
    "{account_identifier:{\$in:['${OLD_EMAIL}','${NEW_EMAIL}']},status:'active'}" 0
fi
stop_server

printf '\nReplacement failure rollback\n'
start_server "$FAILURE_DB" "$OLD_EMAIL" "$OLD_PASSWORD" "" failure-seed-old
if wait_for_ready "rollback fixture bootstrap"; then
  expect_login old-before-failure "$OLD_EMAIL" "$OLD_PASSWORD" 200
fi
stop_server

if mongo_eval "$FAILURE_DB" "
  const result = target.runCommand({
    collMod: 'knoxx_actor_credentials',
    validator: {account_identifier: {\$ne: '${NEW_EMAIL}'}},
    validationLevel: 'strict',
    validationAction: 'error'
  });
  if (!result.ok) quit(43);
" >/dev/null 2>&1; then
  pass "installed an isolated validator that rejects only the replacement"
else
  fail "install replacement-failure fixture validator"
fi

start_server "$FAILURE_DB" "$NEW_EMAIL" "$NEW_PASSWORD" "$OLD_EMAIL" replacement-failure
expect_startup_failure "replacement failure"
expect_count "failed replacement rolls the prior deactivation back" "$FAILURE_DB" \
  "{account_identifier:'${OLD_EMAIL}',status:'active'}" 1
expect_count "failed replacement does not leave a new active credential" "$FAILURE_DB" \
  "{account_identifier:'${NEW_EMAIL}',status:'active'}" 0

printf '\nSummary: %s passed, %s failed\n' "$PASS_COUNT" "$FAIL_COUNT"
if [ "$FAIL_COUNT" -ne 0 ]; then
  printf 'Failed checks:\n'
  printf '  - %s\n' "${FAILURES[@]}"
  exit 1
fi

exit 0
