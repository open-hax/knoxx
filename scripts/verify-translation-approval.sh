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
# run. It writes ONLY inside ${CONTRACTS_DIR}/_verify_translation_approval — the
# probe source file included, which is why the seeded document's source path
# points in there — and removes that one directory on exit, including on failure
# or Ctrl-C. Anything it seeds in Mongo it also removes.
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

# The receipt this run seeds so the successful approval path is reachable.
# Approval validates against a completed translation, and the only producer of
# one is the ingestion worker — so a live happy path needs the receipt seeded.
# Seeded directly in Mongo rather than through a route: a route that writes
# translation evidence is a route that can fabricate it, which is the one thing
# this whole seam exists to prevent.
MONGO_URL="${KNOXX_MONGO_URL:-mongodb://localhost:27017}"
MONGO_DB="${KNOXX_MONGO_DB:-knoxx}"
ORG_ID="${KNOXX_VERIFY_ORG_ID:-}"
PROJECT="${KNOXX_SESSION_PROJECT_NAME:-knoxx-session}"
# The stored form of a nullable project, mirroring
# `mongo-translation-evidence/scope-value`: an unset project is its own scope, so
# it is stored as a sentinel rather than left absent, and the query matches it.
if [ -n "$PROJECT" ]; then
  STORED_PROJECT="$PROJECT"
else
  STORED_PROJECT=$'\u0000none'
fi
SOURCE_REVISION="sha256-verifyapproval${RUN_ID}"
TRANSLATION_REVISION="${SOURCE_REVISION}+es@verify-${RUN_ID}"
DISPATCH_KEY="verify-approval-${RUN_ID}"
DOC_EDN=":${DOC_ID}"

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
mongo_eval() {
  mongosh "${MONGO_URL}/${MONGO_DB}" --quiet --eval "$1"
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
RECEIPT_SEEDED=0

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
  if [ "$RECEIPT_SEEDED" -eq 1 ]; then
    mongo_eval "db.knoxx_translation_receipts.deleteMany({dispatch_key: \"${DISPATCH_KEY}\"});
                db.knoxx_translation_approvals.deleteMany({document: \"${DOC_EDN}\"});" \
      >/dev/null 2>&1 \
      && note "torn down the seeded receipt and any approval of it" \
      || warn "could not remove seeded Mongo rows — see the cleanup command above"
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
fixture_write
# Inside the fixture directory, so the single teardown covers it.
printf '# Approval verification probe\n\nSeeded by scripts/verify-translation-approval.sh.\n' \
  > "$SOURCE_FILE"
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

# ── 5. The successful approval ─────────────────────────────────────────────
#
# Approval validates against a completed translation, and the only thing that
# produces one is the ingestion worker. So to let a reviewer watch this feature
# work, the script seeds one receipt itself — in Mongo, not through a route,
# because a route that writes translation evidence is a route that can fabricate
# it. It removes what it seeded on the way out.

step "5. a real approval, against a seeded translation"

if ! command -v mongosh >/dev/null 2>&1; then
  warn "mongosh is not installed, so the successful path cannot be exercised"
  note "Install mongosh, or set KNOXX_MONGO_URL to a reachable deployment, and"
  note "re-run to see the approval actually recorded."
elif [ -z "$ORG_ID" ]; then
  warn "KNOXX_VERIFY_ORG_ID is not set, so the successful path is skipped"
  note "An approval is tenant-scoped and inherits its organization from the"
  note "receipt, so the seeded receipt has to name the org the API key resolves"
  note "to. Find it with: GET /api/auth/session — then re-run with"
  note "KNOXX_VERIFY_ORG_ID=<that org id>"
elif ! mongo_eval 'db.runCommand({ping:1})' >/dev/null 2>&1; then
  warn "cannot reach ${MONGO_URL} — the successful path is skipped"
else
  receipt_edn="{:receipt/type :translation/completed"
  receipt_edn="${receipt_edn} :translation/document ${DOC_EDN}"
  receipt_edn="${receipt_edn} :translation/source-locale :en"
  receipt_edn="${receipt_edn} :translation/locale :es"
  receipt_edn="${receipt_edn} :translation/source-revision \"${SOURCE_REVISION}\""
  receipt_edn="${receipt_edn} :translation/revision \"${TRANSLATION_REVISION}\""
  receipt_edn="${receipt_edn} :translation/dispatch-key \"${DISPATCH_KEY}\""
  receipt_edn="${receipt_edn} :translation/org-id \"${ORG_ID}\""
  receipt_edn="${receipt_edn} :translation/project \"${PROJECT}\""
  receipt_edn="${receipt_edn} :translation/at \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\"}"

  # `org_id` and `project` are top-level columns, not just fields inside the EDN.
  # `read-receipts!` narrows through `scope-query`, which is field equality on
  # exactly those two — a receipt carrying them only in `receipt_edn` is
  # invisible to the query, and the approval would be refused 409 for a receipt
  # that is sitting right there.
  if mongo_eval "db.knoxx_translation_receipts.insertOne({
       dispatch_key: \"${DISPATCH_KEY}\",
       document: \"${DOC_EDN}\",
       locale: \"es\",
       source_revision: \"${SOURCE_REVISION}\",
       org_id: \"${ORG_ID}\",
       project: \"${STORED_PROJECT}\",
       receipt_edn: '${receipt_edn}'
     });" >/dev/null 2>&1; then
    RECEIPT_SEEDED=1
    pass "seeded one completed-translation receipt for this run"
    note "org ${ORG_ID}, project ${PROJECT:-<none>}, revision ${SOURCE_REVISION}"

    good="{\"document\":\"${DOC_ID}\",\"locale\":\"es\",\"revision\":\"${SOURCE_REVISION}\",\"translation_revision\":\"${TRANSLATION_REVISION}\"}"

    resp="$(http POST "$APPROVALS_URL" auth "$good")"
    if expect_status "POST approvals records the approval" "201" "$resp"; then
      expect_jq "the response says it was recorded" '.status == "recorded"' "$resp"
      expect_jq "the approval names the reviewed output revision" \
        "[.. | strings] | index(\"${TRANSLATION_REVISION}\")" "$resp"
      expect_jq "the approval is attributed to a principal the caller never sent" \
        '[.. | objects | select(has("user_email") or has("userEmail"))] | length > 0' "$resp" \
        || expect_jq "the approval carries a principal" \
             '[.. | strings] | any(test("@"))' "$resp"
      expect_jq "the tenant is inherited from the receipt, not the request" \
        "[.. | strings] | index(\"${ORG_ID}\")" "$resp"
    fi

    step "6. approving the same output twice is the same fact"

    again="$(http POST "$APPROVALS_URL" auth "$good")"
    if expect_status "the replay answers 200, not a conflict" "200" "$again"; then
      expect_jq "it is recognized as the approval already recorded" \
        '.status == "existing"' "$again"
      note "an honest double-click is not something a reviewer has to resolve"
    fi

    step "7. an approval cannot be transplanted onto another output"

    stale="$(printf '%s' "$good" | jq -c '.translation_revision = "sha256-someone-elses+es@b9"')"
    resp="$(http POST "$APPROVALS_URL" auth "$stale")"
    if expect_status "naming a different produced output is refused" "409" "$resp"; then
      expect_jq "the refusal names both sides" \
        "[.. | strings] | index(\"${TRANSLATION_REVISION}\")" "$resp"
      note "so a reviewer can see whether their request or the record was stale"
    fi
  else
    warn "could not seed a receipt, so the successful path is skipped"
    note "The API key's org may differ from KNOXX_VERIFY_ORG_ID, or the Mongo"
    note "user may lack write access to ${MONGO_DB}."
  fi
fi

# ── 8. Known gaps ──────────────────────────────────────────────────────────

step "8. what this run cannot reach"

warn "whether an approval unblocks a publication is the reconciler card's surface"
note "Approval makes a plan admissible; it must not itself publish, and a test"
note "pins that. The trigger that acts on it is"
note "knoxx-publication-reconciler-runtime, the next card."

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
