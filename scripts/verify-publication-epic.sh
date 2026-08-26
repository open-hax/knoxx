#!/usr/bin/env bash
#
# Manual verification for the contract-owned publication epic.
#
# Walks the whole journey against a LIVE Knoxx: seed resources on disk ->
# projection -> CMS view -> authorized state change -> the change lands back in
# the resource file. Then it walks the failure modes the reviews were about:
# a schema-invalid resource must block rather than vanish, a colliding
# canonical identity must be a conflict rather than a coin flip, and every
# surface must refuse an unauthenticated caller.
#
# The fixture is created and destroyed by this script. It writes ONLY inside
# ${CONTRACTS_DIR}/_verify and removes that directory on exit, including on
# failure or Ctrl-C, so a killed run leaves no resource behind.
#
# Usage:
#   scripts/verify-publication-epic.sh
#   KNOXX_BASE_URL=http://localhost:8000 KNOXX_API_KEY=... scripts/verify-publication-epic.sh
#
# Exit code is 0 only when every check passed.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${KNOXX_BASE_URL:-http://localhost:8000}"
API_KEY="${KNOXX_API_KEY:-}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify"

# shellcheck source=lib/publication-fixture.sh
. "${REPO_ROOT}/scripts/lib/publication-fixture.sh"

NS="$FIXTURE_NS"
DOC_ID="$FIXTURE_DOC_ID"
PUB_ID="$FIXTURE_PUB_ID"

PASS_COUNT=0
FAIL_COUNT=0
FAILURES=()

# ── Output ─────────────────────────────────────────────────────────────────

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_DIM=$'\033[2m'; C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'; C_RED=$'\033[31m'; C_YELLOW=$'\033[33m'; C_CYAN=$'\033[36m'
else
  C_RESET=""; C_DIM=""; C_BOLD=""; C_GREEN=""; C_RED=""; C_YELLOW=""; C_CYAN=""
fi

step() { printf '\n%s── %s %s%s\n' "$C_BOLD$C_CYAN" "$1" "$(printf '─%.0s' $(seq 1 $((60 - ${#1}))))" "$C_RESET"; }
note() { printf '%s   %s%s\n' "$C_DIM" "$1" "$C_RESET"; }

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf '%s   PASS%s  %s\n' "$C_GREEN" "$C_RESET" "$1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAILURES+=("$1")
  printf '%s   FAIL%s  %s\n' "$C_RED" "$C_RESET" "$1"
  if [ -n "${2:-}" ]; then
    printf '%s         got: %s%s\n' "$C_DIM" "$2" "$C_RESET"
  fi
}

die() { printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2; exit 2; }

# ── HTTP ───────────────────────────────────────────────────────────────────

# Emits "<status>\n<body>". Body may be empty.
http() {
  local method="$1" path="$2" authorized="$3" body="${4:-}"
  local args=(-s -o /dev/stdout -w $'\n%{http_code}' -X "$method" --max-time 20)
  [ "$authorized" = "auth" ] && args+=(-H "X-API-Key: ${API_KEY}")
  if [ -n "$body" ]; then
    args+=(-H "Content-Type: application/json" -d "$body")
  fi
  local raw
  raw="$(curl "${args[@]}" "${BASE_URL}${path}" 2>/dev/null)"
  local status="${raw##*$'\n'}"
  local payload="${raw%$'\n'*}"
  printf '%s\n%s' "$status" "$payload"
}

status_of() { printf '%s' "${1%%$'\n'*}"; }
body_of()   { printf '%s' "${1#*$'\n'}"; }

# Assert a request returns one of a set of statuses.
expect_status() {
  local label="$1" expected="$2" response="$3"
  local status; status="$(status_of "$response")"
  if [[ " $expected " == *" $status "* ]]; then
    pass "$label ${C_DIM}(${status})${C_RESET}"
    return 0
  fi
  fail "$label — expected ${expected// /|}, got ${status}" "$(body_of "$response" | head -c 400)"
  return 1
}

# Assert a jq expression over the response body is true.
expect_jq() {
  local label="$1" filter="$2" response="$3"
  local payload; payload="$(body_of "$response")"
  if printf '%s' "$payload" | jq -e "$filter" >/dev/null 2>&1; then
    pass "$label"
    return 0
  fi
  fail "$label — jq filter did not hold: ${filter}" "$(printf '%s' "$payload" | jq -c . 2>/dev/null | head -c 400 || printf '%s' "$payload" | head -c 400)"
  return 1
}

# ── Fixture ────────────────────────────────────────────────────────────────

# Removing the whole fixture directory is the entire teardown: every file this
# script writes lives inside it, including the file the PATCH step rewrites.
#
# It removes the directory ONLY when this run is the one that created it.
# The trap has to be armed before the fixture exists — a run killed mid-seed
# must still clean up — so an unguarded teardown would delete a fixture left by
# a killed earlier run, or one a concurrent run is actively using, on any early
# exit such as a missing tool. Losing someone else's data while reporting a
# usage error is a worse failure than leaving a stale directory behind.
FIXTURE_OWNED=0

cleanup() {
  local code=$?
  local signalled="${1:-}"
  # On EXIT, $? is the script's real status. On a signal it is not: it is
  # whatever the last command happened to return, so a run killed mid-check
  # would tear down and then report success. The signal handlers pass their
  # conventional 128+n status explicitly.
  if [ -n "$signalled" ]; then code="$signalled"; fi
  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    fixture_remove
    note "torn down ${FIXTURE_DIR#$REPO_ROOT/}"
  fi
  exit "$code"
}
# Each signal handler disarms EXIT first, so teardown runs once and the status
# it names is the one that leaves the process.
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

# ── Preflight ──────────────────────────────────────────────────────────────

printf '%s\n' "${C_BOLD}Knoxx contract-owned publication — live verification${C_RESET}"
note "base url       ${BASE_URL}"
note "contracts dir  ${CONTRACTS_DIR}"

for tool in curl jq; do
  command -v "$tool" >/dev/null 2>&1 || die "missing required tool: ${tool}"
done

[ -n "$API_KEY" ] || die "KNOXX_API_KEY is not set. Use the same value the running backend was started with."
[ -d "$CONTRACTS_DIR" ] || die "contracts directory not found: ${CONTRACTS_DIR}"

if [ -e "$FIXTURE_DIR" ]; then
  die "fixture directory already exists: ${FIXTURE_DIR} — a previous run may have been killed. Remove it and retry."
fi

if ! curl -s -o /dev/null --max-time 5 "${BASE_URL}/health" 2>/dev/null; then
  die "no Knoxx backend answering at ${BASE_URL}. Start it from THIS checkout (see docs/verification/publication-epic.md) and retry."
fi
note "backend is reachable"

# The running backend must be serving this checkout's contracts, or the fixture
# is invisible to it and every projection check below is meaningless.
step "0. the running backend serves this checkout"
FIXTURE_OWNED=1
fixture_write_valid
sleep 1
probe="$(http GET "/api/publications/documents" auth)"
if [ "$(status_of "$probe")" = "000" ]; then
  die "backend did not answer /api/publications/documents"
fi
if body_of "$probe" | jq -e --arg id "$DOC_ID" '[.documents[].document.id] | index($id)' >/dev/null 2>&1; then
  pass "seeded fixture is visible to the running backend"
else
  # A blocker naming the fixture file is PROOF the backend read it. Reporting
  # that as "cannot see it" is what sent a reader to check pm2 while the real
  # answer — the fixture no longer satisfies a schema — sat in the response.
  if body_of "$probe" | jq -e --arg d "$FIXTURE_DIR" \
       '[.error.blockers[]? | select(.["file-path"] | tostring | startswith($d))] | length > 0' \
       >/dev/null 2>&1; then
    printf '%s   FAIL%s  the backend READ the fixture and rejected it\n' "$C_RED" "$C_RESET"
    note "This is a fixture/schema drift, not a wrong checkout. The blocker names the file and its kind:"
    note "$(body_of "$probe" | jq -c '.error.blockers' 2>/dev/null | head -c 300)"
    note "Update scripts/lib/publication-fixture.sh to satisfy the current law.publication shapes."
    die "the verification fixture is stale"
  fi
  printf '%s   FAIL%s  the running backend cannot see %s\n' "$C_RED" "$C_RESET" "${FIXTURE_DIR#$REPO_ROOT/}"
  note "It is probably serving a different checkout. Check: pm2 describe knoxx-backend | grep cwd"
  note "Response was: $(body_of "$probe" | head -c 300)"
  die "cannot verify against a backend that is not running this code"
fi

# ── 1. Authorization ───────────────────────────────────────────────────────
#
# Every surface is checked unauthenticated FIRST. A route that answers 200 to an
# anonymous caller is not a working route even though it responds, and the
# projection reads document titles, garden membership, and publication paths off
# the filesystem — so an open route is an enumeration leak.

step "1. every surface refuses an unauthenticated caller"

expect_status "GET  /api/publications/documents            is refused" "401 403" \
  "$(http GET "/api/publications/documents" anon)"
expect_status "GET  /api/publications/documents/:id        is refused" "401 403" \
  "$(http GET "/api/publications/documents/${NS}%2Fprobe" anon)"
expect_status "GET  /api/cms/publications/documents        is refused" "401 403" \
  "$(http GET "/api/cms/publications/documents" anon)"
expect_status "PATCH /api/cms/publications/intents/:id     is refused" "401 403" \
  "$(http PATCH "/api/cms/publications/intents/${NS}%2Fprobe-es" anon '{"state":"published"}')"
expect_status "GET  /api/translations/config               is refused" "401 403" \
  "$(http GET "/api/translations/config" anon)"
expect_status "PATCH /api/translations/config              is refused" "401 403" \
  "$(http PATCH "/api/translations/config" anon '{"model":"anything"}')"

# ── 2. Projection ──────────────────────────────────────────────────────────

step "2. the desired topology resolves from resources alone"

resp="$(http GET "/api/publications/documents" auth)"
expect_status "GET /api/publications/documents" "200" "$resp"
expect_jq "the seeded document is in the topology" \
  "[.documents[].document.id] | index(\"${DOC_ID}\") != null" "$resp"
expect_jq "the seeded garden is in the topology" \
  "[.gardens[].id] | index(\"${NS}/probe-garden\") != null" "$resp"
expect_jq "qualified identity survives JSON (namespace intact, no leading colon)" \
  "[.documents[].document.id] | any(startswith(\"${NS}/\"))" "$resp"

note "identity check matters: clj->js renders a keyword with 'name', which would"
note "have collapsed ${NS}/probe and any-other-ns/probe onto \"probe\"."

resp="$(http GET "/api/publications/documents/${NS}%2Fprobe" auth)"
expect_status "GET /api/publications/documents/:documentId" "200" "$resp"
expect_jq "it returns exactly the seeded publication" \
  ".publications | length == 1 and .[0].id == \"${PUB_ID}\"" "$resp"

expect_status "an unknown document is a 404, not an empty 200" "404" \
  "$(http GET "/api/publications/documents/${NS}%2Fno-such-document" auth)"

# ── 3. CMS view ────────────────────────────────────────────────────────────

step "3. the CMS separates desired intent from observed evidence"

resp="$(http GET "/api/cms/publications/documents" auth)"
expect_status "GET /api/cms/publications/documents" "200" "$resp"
expect_jq "desired state comes from the resource (:withheld)" \
  "[.documents[].publications[] | select(.id == \"${PUB_ID}\")] | .[0].desired == \"withheld\"" "$resp"
expect_jq "observed is null — nothing has been materialized" \
  "[.documents[].publications[] | select(.id == \"${PUB_ID}\")] | .[0].observed == null" "$resp"

note "desired and observed are separate wire fields on purpose. Their disagreement"
note "is drift, and the UI has to be able to show both rather than pick one."

# ── 4. The write ───────────────────────────────────────────────────────────

step "4. an authorized state change reaches the resource file"

resp="$(http PATCH "/api/cms/publications/intents/${NS}%2Fprobe-es" auth '{"state":"published"}')"
expect_status "PATCH intents/:publicationId {state: published}" "200" "$resp"
expect_jq "the response reports the new desired state" '.desired == "published"' "$resp"

sleep 1
if grep -q ':publication/state :published' "${FIXTURE_DIR}/probe.edn" 2>/dev/null; then
  pass "the EDN resource on disk was rewritten — the graph is the authority"
else
  fail "the resource file on disk still says :withheld" "$(grep ':publication/state' "${FIXTURE_DIR}/probe.edn" 2>/dev/null || echo '<file unreadable>')"
fi

resp="$(http GET "/api/cms/publications/documents" auth)"
expect_jq "re-reading the CMS shows the new desired state" \
  "[.documents[].publications[] | select(.id == \"${PUB_ID}\")] | .[0].desired == \"published\"" "$resp"

# Identity is immutable through this surface. A caller that contrives to send
# identity fields must not be able to move the publication.
resp="$(http PATCH "/api/cms/publications/intents/${NS}%2Fprobe-es" auth \
  '{"state":"withheld","path":"/verify/moved","locale":"de","revision":"rev-hijack","garden":"knoxx.verify/other"}')"
expect_status "PATCH carrying identity fields is accepted for state only" "200 422" "$resp"

# Checked field by field, and every field the PATCH above tried to move is
# checked — asserting only path and locale would have let a garden or revision
# hijack through while still reporting "identity did not move". Flattened first
# because the writer is free to line-break between a key and its value.
# shellcheck disable=SC2002  # `< missing` makes the shell itself write to stderr
on_disk_flat="$(cat "${FIXTURE_DIR}/probe.edn" 2>/dev/null | tr '\n' ' ')"
identity_moved=""
for expected in \
  ':publication/path[[:space:]]+"/verify/probe-es"' \
  ':publication/locale[[:space:]]+:es' \
  ':publication/garden[[:space:]]+:probe-garden' \
  ':publication/revision[[:space:]]+"rev-verify-1"'
do
  printf '%s' "$on_disk_flat" | grep -qE -- "$expected" \
    || identity_moved="${identity_moved}${identity_moved:+, }${expected}"
done

if [ -z "$identity_moved" ]; then
  pass "identity did not move — path, locale, garden and revision are unchanged on disk"
else
  fail "identity moved through the state-patch surface" "no longer on disk: ${identity_moved}"
fi

expect_status "PATCH on an unknown publication is a 404" "404" \
  "$(http PATCH "/api/cms/publications/intents/${NS}%2Fno-such-publication" auth '{"state":"published"}')"

# ── 5. Failure modes ───────────────────────────────────────────────────────
#
# These are the two review findings that were unreachable through the real load
# path until they were fixed. Both are about a resource that should stop the
# projection but could instead disappear from it.

step "5. a bad resource blocks the projection instead of vanishing from it"

fixture_write_invalid
sleep 1
resp="$(http GET "/api/publications/documents" auth)"
expect_status "a schema-invalid resource makes the projection fail closed" "409" "$resp"
expect_jq "and it says WHICH resource was rejected" \
  '(.error.blockers // .detail.blockers // []) | length > 0' "$resp"

note "the failure mode being prevented: the loader drops an invalid record, so"
note "without this the projection would 200 with the intent silently absent."

rm -f "${FIXTURE_DIR}/invalid.edn"
sleep 1
expect_status "removing it restores the projection" "200" \
  "$(http GET "/api/publications/documents" auth)"

step "6. a colliding canonical identity is a conflict, not a coin flip"

fixture_write_collision
sleep 1
resp="$(http GET "/api/publications/documents" auth)"
expect_status "two files claiming ${PUB_ID} conflict" "409" "$resp"
# Keyed on :detail, because every adapter on this surface now builds its body
# through knoxx.backend.law.error-body — the message in :detail, the ex-data in
# :error. Before that the two adapters disagreed about which key held which,
# and this had to match the whole body as text to pass on both routes.
expect_jq "and the conflict is reported, not resolved by directory order" \
  '.detail | tostring | test("conflict")' "$resp"

note "first-wins dedup would have kept whichever file readdir returned first,"
note "making the topology depend on filesystem enumeration order."
note ""
note "Enforced by publication-identity-conflicts in domain/publication_resolver.cljs."
note "publication-conflicts alone was not enough: it keys on the RELATION"
note "(document x garden x locale x revision), so two files declaring the same"
note ":publication/id with different revisions both landed in the index and every"
note "lookup by id — including set-publication-state! — took whichever came first."
note "The identity check now rejects the pair outright."

rm -f "${FIXTURE_DIR}/collision.edn"
sleep 1
expect_status "removing it restores the projection" "200" \
  "$(http GET "/api/publications/documents" auth)"

# ── 7. Translation config ──────────────────────────────────────────────────

step "7. translation config resolves with no hosted backend"

resp="$(http GET "/api/translations/config" auth)"
expect_status "GET /api/translations/config" "200" "$resp"
expect_jq "it returns a resolved model" '(.model // .["model"]) != null' "$resp"

note "this is the surface the JVM ingestion worker reads too, so no consumer"
note "resolves model precedence for itself."

# ── 8. Retired authority ───────────────────────────────────────────────────
#
# Repo-wide, unlike the backend test, which walks an explicit list of 8 files.
#
# law/publication_surface.cljs is excluded: it is the file that DECLARES the
# retired paths, so it must name them literally. The backend suite asserts that
# exception positively — the declaration must be data and never a call.

sweep() {
  grep -rn --fixed-strings "$1" \
    "${REPO_ROOT}/frontend/src" "${REPO_ROOT}/backend/src" "${REPO_ROOT}/ingestion/src" \
    2>/dev/null \
    | grep -v '\.test\.\|_test\.\|/test/\|ui-backend-surface-matrix\|law/publication_surface\.cljs' \
    || true
}

step "8. the CMS and translation paths call no retired authority"

# The scope this epic actually claims: CMS publication state and translation
# pipeline config. A caller here is a regression and fails the run.
for retired in "/api/openplanner/v1/translations/config" "/v1/translations/config"; do
  hits="$(sweep "$retired")"
  if [ -z "$hits" ]; then
    pass "no shipped caller of ${retired}"
  else
    fail "${retired} still has a shipped caller" "$(printf '%s' "$hits" | head -3)"
  fi
done

hits="$(sweep "KNOXX_EXPECT_OPENPLANNER_REST")"
if [ -z "$hits" ]; then
  pass "the conditional-skip deploy flag is gone from shipped source"
else
  fail "KNOXX_EXPECT_OPENPLANNER_REST still appears in shipped source" "$(printf '%s' "$hits" | head -3)"
fi

step "8b. the translation producer — an agent actor, not a worker"

# The check whose absence caused the failure this section verifies is fixed.
#
# Every other publication surface in this script can pass while nothing is able
# to produce a translation: the gate derives work, a claim is taken, and the
# batch goes to an ingestion worker this deployment does not run. The four
# localized intents then stay blocked forever and no surface says why. So what is
# asserted here is the *producer*, not the plumbing.
#
# The composition is three existing contracts plus one that was missing:
#   contracts/roles/translator.edn            the role      (already existed)
#   contracts/capabilities/cap_translation.edn the tool      (already existed)
#   contracts/namespaces/publication.edn       the trigger   (added)
#   contracts/agents/publication_translator.edn the agent    (added)

response="$(http GET /api/admin/config/events auth)"
if expect_status "the event runtime surface answers an authorized caller" "200" "$response"; then
  # Matched on `.id`, not `.events`: `encode-wire-values` renders an event
  # keyword with `name`, so :publication/translation-needed reaches the wire as
  # "translation-needed" and the namespace is only recoverable from the id.
  trigger="$(body_of "$response" | jq -c '
    [.runtime.triggers[]?
     | select(((.id // "") | tostring | test("translation-needed"))
              or (((.events // []) | map(tostring) | any(test("translation-needed")))))]
    | first // empty' 2>/dev/null)"

  if [ -z "$trigger" ] || [ "$trigger" = "null" ]; then
    fail "a trigger subscribes to publication/translation-needed" \
         "$(body_of "$response" | jq -c '[.runtime.triggers[]?.id]' 2>/dev/null)"
    note "with no such trigger, every localized intent stays blocked and nothing reports why"
  else
    pass "a trigger subscribes to publication/translation-needed"

    if [ "$(printf '%s' "$trigger" | jq -r '.enabled')" = "true" ]; then
      pass "that trigger is enabled"
    else
      fail "that trigger is enabled" "$trigger"
    fi

    # The wire renders :actions/start-agent-session with `name`, so the
    # namespace is gone by the time it gets here. Match the tail.
    if [ "$(printf '%s' "$trigger" | jq -r '.action')" = "start-agent-session" ]; then
      pass "its action starts an agent session, rather than posting to a worker"
    else
      fail "its action starts an agent session" "$(printf '%s' "$trigger" | jq -r '.action')"
    fi

    agent_id="$(printf '%s' "$trigger" | jq -r '.agent // empty')"
    if [ -z "$agent_id" ]; then
      fail "the trigger names an agent contract" "$trigger"
    else
      pass "the trigger names agent contract '${agent_id}'"

      # The file being on disk proves nothing. A contract only starts a session
      # if it resolves through role, capability and actor scope, and the catalog
      # is the one view that has done all three.
      #
      # Asked AS THE TRIGGER'"'"'S LISTENER, because the catalog is actor-scoped: the
      # default actor is chat_primary and the translator belongs to pi, so a bare
      # lookup reports "does not resolve" for a contract that resolves fine for
      # the actor the session actually runs as.
      listener="$(printf '%s' "$trigger" | jq -r '.listener // empty')"
      response="$(http GET "/api/knoxx/agents/catalog${listener:+?actorId=$listener}" auth)"
      if expect_status "the agent catalog answers an authorized caller" "200" "$response"; then
        if printf '%s' "$(body_of "$response")" \
             | jq -e --arg id "$agent_id" '[.agents[]? | select((.id // "") == $id)] | length > 0' \
             >/dev/null 2>&1; then
          pass "'${agent_id}' resolves in the deployed catalog"
        else
          fail "'${agent_id}' resolves in the deployed catalog" \
               "$(body_of "$response" | jq -c '[.agents[]?.id]' 2>/dev/null)"
          note "a trigger naming an unresolvable contract fails exactly like having no trigger"
        fi
      fi
    fi
  fi
fi

# The role and capability halves are contract data, so they are checked on disk:
# they are what makes `save_translation` reachable at all, and a deployment that
# lost either would resolve the agent and then hand it no way to submit.
if [ -f "${CONTRACTS_DIR}/roles/translator.edn" ]; then
  pass "the translator role contract is present"
else
  fail "the translator role contract is present" "${CONTRACTS_DIR}/roles/translator.edn"
fi

if grep -q "save_translation" "${CONTRACTS_DIR}/capabilities/cap_translation.edn" 2>/dev/null; then
  pass "cap/translation still grants save_translation"
else
  fail "cap/translation still grants save_translation" \
       "${CONTRACTS_DIR}/capabilities/cap_translation.edn"
fi

# The four links that were each individually broken, and are each individually
# checkable. Every one of them failed silently before — no surface reported any
# of it — which is why they are asserted rather than assumed.
step "8c. the four links the translation path needs"

# 1. Derived work must actually reach dispatch. `referenced-documents` used to
#    return [] for every input, so `:current-source-revision` was nil for every
#    document and every intent short-circuited on :publication-revision-unresolved.
response="$(http POST /api/publications/translations/dispatch auth '{}')"
if expect_status "dispatch answers an authorized caller" "200" "$response"; then
  expect_jq "the runner is the agent, not the absent ingestion worker" \
    '.runner == "agent"' "$response"
  # `dispatched` empty while `admissible` is non-zero is the exact shape the
  # `referenced-documents` bug produced, and it reads like "nothing to do".
  expect_jq "admissible intents actually derive work" \
    '(.admissible // 0) == 0 or ((.dispatched // []) | length) > 0' "$response"
fi

# 2. The pin must reach the tool. Carried by the event, forwarded by
#    :actions/start-agent-session, and put on the turn's auth context by
#    `auth-context-for-agent-turn` — the last of which was missing, so
#    save_translation fell through to the OpenPlanner segment path.
# 3. The tool schema must accept a submission that omits what the pin supplies.
# 4. The organization must come from the claim, not from a request context a
#    triggered session does not have.
#
# All three show up the same way: a claim that stays in flight with no receipt.
note "links 2-4 are observable only after an agent run completes; see"
note "docs/verification/publication-epic.md for the live walkthrough"

# KNOWN GAP — a WARN every run, never a FAIL. See
# knoxx.backend.infra.translation-agent-dispatch/known-gap.
printf '%s   WARN%s  an agent-dispatched claim whose session dies mid-run stays in flight\n' \
  "$C_YELLOW" "$C_RESET"
note "there is no session read that can settle it, so that revision needs an operator"
note "the worker path recovers this from batch state; the agent path has no equivalent"

step "9. advisory — OpenPlanner surfaces still live outside this epic's scope"

# NOT a failure. law.publication-surface declares that retired paths must have
# "NO shipped caller", but this epic only retired the CMS and translation
# authority. The Gardens page still reads gardens from OpenPlanner REST, and the
# backend test that guards the claim walks an explicit 8-file list that does not
# include it. Printed here so the gap is visible during review rather than
# discovered in production.
hits="$(sweep "/api/openplanner/v1/gardens")"
if [ -z "$hits" ]; then
  pass "no shipped caller of /api/openplanner/v1/gardens either"
else
  printf '%s   WARN%s  /api/openplanner/v1/gardens still has %s shipped caller(s), outside this epic:\n' \
    "$C_YELLOW" "$C_RESET" "$(printf '%s' "$hits" | wc -l | tr -d ' ')"
  printf '%s' "$hits" | sed "s|${REPO_ROOT}/|         |" | head -5
  note "the backend guard does not cover these files; see docs/verification/publication-epic.md"
fi

# ── Summary ────────────────────────────────────────────────────────────────

printf '\n%s%s%s\n' "$C_BOLD" "$(printf '═%.0s' $(seq 1 64))" "$C_RESET"
if [ "$FAIL_COUNT" -eq 0 ]; then
  printf '%s%d passed, 0 failed%s\n\n' "$C_GREEN$C_BOLD" "$PASS_COUNT" "$C_RESET"
  exit 0
fi

printf '%s%d passed, %d FAILED%s\n\n' "$C_RED$C_BOLD" "$PASS_COUNT" "$FAIL_COUNT" "$C_RESET"
for f in "${FAILURES[@]}"; do
  printf '  %s•%s %s\n' "$C_RED" "$C_RESET" "$f"
done
printf '\n'
exit 1
