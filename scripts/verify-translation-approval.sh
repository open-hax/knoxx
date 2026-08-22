#!/usr/bin/env bash
#
# Manual verification for the translation approval surface.
#
# Walks the approval surface against a LIVE Knoxx: every way of getting an
# approval wrong, then the one way of getting it right — which requires a
# completed translation to exist, and therefore cannot be driven from HTTP alone.
#
# WHAT THIS PROVES, entirely from the outside:
#
#   * An unauthenticated caller cannot record review evidence.
#   * A caller cannot supply its own principal, timestamp, tenant or project —
#     the request contract refuses every one of those fields.
#   * A malformed, blank, unqualified or selector-shaped identifier is refused.
#   * Approving a translation that does not exist is refused, and refused as a
#     conflict rather than a not-found: the request is well formed and the system
#     simply is not in a state where approving means anything yet.
#
# WHAT THIS CANNOT PROVE, printed as WARN every run:
#
#   * A successful approval. Recording one requires a completed-translation
#     receipt in the durable store, which requires the ingestion worker to have
#     actually translated something — not this card's surface, and not reachable
#     from HTTP. The happy path, the idempotent double-approval, the
#     tenant/project isolation and the re-translation supersession are covered by
#     backend/test/cljs/knoxx/backend/infra/routes/translation_review_test.cljs
#     and .../extern/fastify/translation_review_test.cljs.
#   * That an approval unblocks a publication. That is the reconciler runtime's
#     surface (`knoxx-publication-reconciler-runtime`), the next card.
#
# The fixture is created and destroyed by this script, with a unique identity per
# run. It writes ONLY inside ${CONTRACTS_DIR}/_verify_translation_approval and
# removes that directory on exit, including on failure or Ctrl-C.
#
# Usage:
#   scripts/verify-translation-approval.sh
#   KNOXX_BASE_URL=http://localhost:8000 KNOXX_API_KEY=... scripts/verify-translation-approval.sh
#
# Exit code is 0 only when every check passed.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${KNOXX_BASE_URL:-http://localhost:8000}"
API_KEY="${KNOXX_API_KEY:-}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify_translation_approval"

RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
NS="knoxx.verifyapproval"
DOC_LOCAL="probe${RUN_ID}"
DOC_ID="${NS}/${DOC_LOCAL}"
# The garden the publication intent names, canonicalized the same way the
# resolver canonicalizes it. An approval is scoped to one garden: the same
# document translated into the same locale for two gardens is two different
# outputs, so review evidence that named no garden would authorize bytes the
# reviewer never read.
GARDEN_ID="${NS}/probe-garden"
# Both derived from the *configured* contract root, never from `REPO_ROOT`.
# `KNOXX_CONTRACTS_DIR` may point at a different checkout, and a fixture whose
# EDN lives under that root while its source lives under this one declares a
# path the running backend cannot read — so the digest never resolves and
# teardown leaves the source behind. Same defect the dispatch script was
# reviewed for.
CONTRACTS_PARENT="$(cd "$(dirname "${CONTRACTS_DIR}")" && pwd)"
CONTRACTS_NAME="$(basename "${CONTRACTS_DIR}")"
SOURCE_REL="${CONTRACTS_NAME}/_verify_translation_approval/probe-${RUN_ID}.md"
SOURCE_FILE="${CONTRACTS_PARENT}/${SOURCE_REL}"
PINNED_REVISION="rev-verify-approval-${RUN_ID}"
APPROVALS_URL="/api/publications/translations/approvals"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0
FAILURES=()

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_DIM=$'\033[2m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_YELLOW=$'\033[33m'; C_CYAN=$'\033[36m'
else
  C_RESET=""; C_DIM=""; C_BOLD=""; C_GREEN=""; C_RED=""; C_YELLOW=""; C_CYAN=""
fi

step() { printf '\n%s── %s %s%s\n' "$C_BOLD$C_CYAN" "$1" "$(printf '─%.0s' $(seq 1 $((58 - ${#1}))))" "$C_RESET"; }
note() { printf '%s   %s%s\n' "$C_DIM" "$1" "$C_RESET"; }
pass() { PASS_COUNT=$((PASS_COUNT + 1)); printf '%s   PASS%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1)); FAILURES+=("$1")
  printf '%s   FAIL%s  %s\n' "$C_RED" "$C_RESET" "$1"
  [ -n "${2:-}" ] && printf '%s         got: %s%s\n' "$C_DIM" "$2" "$C_RESET"
}
warn() { WARN_COUNT=$((WARN_COUNT + 1)); printf '%s   WARN%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
die() { printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2; exit 2; }

http() {
  local method="$1" path="$2" authorized="$3" body="${4:-}"
  local args=(-s -o /dev/stdout -w $'\n%{http_code}' -X "$method" --max-time 30)
  [ "$authorized" = "auth" ] && args+=(-H "X-API-Key: ${API_KEY}")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local raw; raw="$(curl "${args[@]}" "${BASE_URL}${path}" 2>/dev/null)"
  printf '%s\n%s' "${raw##*$'\n'}" "${raw%$'\n'*}"
}
status_of() { printf '%s' "${1%%$'\n'*}"; }
body_of()   { printf '%s' "${1#*$'\n'}"; }

expect_status() {
  local label="$1" expected="$2" response="$3"
  local status; status="$(status_of "$response")"
  if [[ " $expected " == *" $status "* ]]; then
    pass "$label ${C_DIM}(${status})${C_RESET}"; return 0
  fi
  fail "$label — expected ${expected// /|}, got ${status}" "$(body_of "$response" | head -c 300)"
  return 1
}

expect_jq() {
  local label="$1" filter="$2" response="$3"
  if printf '%s' "$(body_of "$response")" | jq -e "$filter" >/dev/null 2>&1; then
    pass "$label"; return 0
  fi
  fail "$label — jq filter did not hold: ${filter}" "$(body_of "$response" | head -c 300)"
  return 1
}

# ── Fixture ────────────────────────────────────────────────────────────────

FIXTURE_OWNED=0
SOURCE_OWNED=0

fixture_write() {
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/probe.edn" <<EDN
;; Throwaway fixture written by scripts/verify-translation-approval.sh.
{:namespace :${NS}
 :resources
 [{:document/id :${DOC_LOCAL}
   :document/title "Translation Approval Verification Probe"
   :document/source-locale :en
   :document/source {:path "${SOURCE_REL}"}}

  {:garden/id :probe-garden
   :garden/title "Approval Verification Garden"
   :garden/status :active
   :garden/locales [:en :es]}

  {:publication/id :${DOC_LOCAL}-es
   :publication/document :${DOC_LOCAL}
   :publication/garden :probe-garden
   :publication/locale :es
   :publication/revision "${PINNED_REVISION}"
   :publication/state :published
   :publication/path "/verify-approval/${DOC_LOCAL}-es"
   :translation/review :required}]}
EDN
}

cleanup() {
  local code=$?
  local signalled="${1:-}"
  [ -n "$signalled" ] && code="$signalled"
  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    rm -rf "$FIXTURE_DIR"; note "torn down ${FIXTURE_DIR#$REPO_ROOT/}"
  fi
  if [ "$SOURCE_OWNED" -eq 1 ] && [ -f "$SOURCE_FILE" ]; then
    rm -f "$SOURCE_FILE"; note "torn down ${SOURCE_REL}"
  fi
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

# ── Preflight ──────────────────────────────────────────────────────────────

printf '%s\n' "${C_BOLD}Knoxx translation approval surface — live verification${C_RESET}"
note "base url       ${BASE_URL}"
note "run id         ${RUN_ID}"

for tool in curl jq; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: ${tool}"
done
[ -n "$API_KEY" ] || die "KNOXX_API_KEY is not set. Use the same value the running backend was started with."
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: ${CONTRACTS_DIR}"
[ -e "$FIXTURE_DIR" ] && die "fixture directory already exists: ${FIXTURE_DIR} — remove it and retry."

curl -s -o /dev/null --max-time 5 "${BASE_URL}/health" 2>/dev/null \
  || die "no Knoxx backend answering at ${BASE_URL}. Start it from THIS checkout and retry."
note "backend is reachable"

step "0. the running backend serves this checkout"
FIXTURE_OWNED=1
if [ ! -f "$SOURCE_FILE" ]; then
  SOURCE_OWNED=1
  mkdir -p "$(dirname "$SOURCE_FILE")"
  printf '# Approval verification probe\n\nSeeded by scripts/verify-translation-approval.sh.\n' > "$SOURCE_FILE"
fi
fixture_write
sleep 1

probe="$(http GET "/api/publications/documents" auth)"
if body_of "$probe" | jq -e --arg id "$DOC_ID" '[.documents[].document.id] | index($id)' >/dev/null 2>&1; then
  pass "seeded fixture is visible to the running backend"
else
  note "It is probably serving a different checkout. Check: pm2 describe knoxx-backend | grep cwd"
  die "cannot verify against a backend that is not running this code"
fi

# ── 1. Authorization ───────────────────────────────────────────────────────

step "1. review evidence cannot be manufactured anonymously"

body="{\"document\":\"${DOC_ID}\",\"garden\":\"${GARDEN_ID}\",\"locale\":\"es\",\"revision\":\"${PINNED_REVISION}\",\"translation_revision\":\"${PINNED_REVISION}+es@b1\"}"
expect_status "POST approvals is refused unauthenticated" "401 403" \
  "$(http POST "$APPROVALS_URL" anon "$body")"
note "an open route here would let anyone produce the evidence a gate waits on"

# ── 2. Attribution is the server's, not the caller's ───────────────────────

step "2. a caller cannot attribute an approval to someone else"

for field in principal at org_id project; do
  smuggled="$(printf '%s' "$body" | jq -c --arg f "$field" '. + {($f): "forged"}')"
  expect_status "a body carrying '${field}' is refused" "400" \
    "$(http POST "$APPROVALS_URL" auth "$smuggled")"
done
note "the principal and timestamp come from the auth context and the clock"

# ── 3. Malformed identifiers ───────────────────────────────────────────────

step "3. malformed identifiers are refused, not reinterpreted"

expect_status "an unrecognized field is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c '. + {documnet: "x"}')")"
expect_status "an unqualified document is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c '.document = "probe"')")"
expect_status "an unqualified garden is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c '.garden = "probe-garden"')")"
expect_status "an approval naming no garden is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c 'del(.garden)')")"
expect_status "a blank revision is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c '.revision = ""')")"
expect_status "a missing field is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c 'del(.locale)')")"
expect_status "a selector revision is refused" "400" \
  "$(http POST "$APPROVALS_URL" auth "$(printf '%s' "$body" | jq -c '.revision = "source/current"')")"
note "a selector gives a stable-looking identity to a moving target"

# ── 4. Approving nothing ───────────────────────────────────────────────────

step "4. approving a translation that does not exist is refused"

resp="$(http POST "$APPROVALS_URL" auth "$body")"
approve_status="$(status_of "$resp")"

if [ "$approve_status" = "503" ]; then
  fail "translation evidence persistence is unavailable" "MongoDB is a required precondition"
  note "The route refuses rather than recording an approval that would vanish on"
  note "restart — the gate would admit a publication today and block it tomorrow."
  note "That refusal is correct; a verification run that never reached the refusal"
  note "path it exists to check has proved nothing, so this is a failure here."
elif [ "$approve_status" = "409" ]; then
  pass "POST approvals refuses with a conflict ${C_DIM}(409)${C_RESET}"
  expect_jq "the refusal is typed, not a message string" \
    '.refusal.type == "translation-receipt-missing" or (.refusal | tostring | test("receipt-missing"))' "$resp" \
    || expect_jq "the response is marked refused" '.refused == true' "$resp"
  note "409 not 404: the request is well formed and the document exists — the"
  note "system simply is not in a state where approving means anything yet"
else
  fail "POST approvals — expected 409 or 503, got ${approve_status}" \
    "$(body_of "$resp" | head -c 300)"
fi

# ── 5. Known gaps ──────────────────────────────────────────────────────────

step "5. what this run cannot reach"

warn "the successful-approval path needs a completed translation in the store"
note "That needs the ingestion worker to have translated something, which is"
note "knoxx-translation-work-dispatch's surface, not this card's. The happy path,"
note "the idempotent double-approval, tenant/project isolation, and"
note "re-translation supersession are covered by the CLJS suites named in the"
note "script header."
warn "whether an approval unblocks a publication is the reconciler card's surface"

printf '\n%s%s%s\n' "$C_BOLD" "$(printf '═%.0s' $(seq 1 60))" "$C_RESET"
printf '%s  %s passed%s' "$C_GREEN" "$PASS_COUNT" "$C_RESET"
[ "$WARN_COUNT" -gt 0 ] && printf '%s, %s known gaps%s' "$C_YELLOW" "$WARN_COUNT" "$C_RESET"
if [ "$FAIL_COUNT" -gt 0 ]; then
  printf '%s, %s FAILED%s\n' "$C_RED" "$FAIL_COUNT" "$C_RESET"
  for f in "${FAILURES[@]}"; do printf '%s    - %s%s\n' "$C_RED" "$f" "$C_RESET"; done
  printf '\n'; exit 1
fi
printf '\n\n'
exit 0
