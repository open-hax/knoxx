#!/usr/bin/env bash
#
# Manual verification for translation work dispatch.
#
# Walks the dispatch seam against a LIVE Knoxx: seed a publication intent that
# needs translating -> ask Knoxx to dispatch derived work -> watch it become a
# recorded, revision-bound dispatch -> ask again and watch it collapse to a
# duplicate instead of enqueueing a second batch.
#
# Then it walks the failure modes the card is about: an unauthenticated caller
# must be refused, an unknown-shaped body must be refused rather than
# reinterpreted as a whole-corpus sweep, an unqualified document id must be
# refused rather than silently sweeping nothing, and a dispatch must not happen
# at all for an intent that needs no translation.
#
# WHAT THIS CANNOT PROVE, and why (printed as WARN every run, not hidden):
#
#   * The worker actually translating. Dispatch hands a batch to the ingestion
#     worker; whether a translation comes back depends on OpenPlanner and a
#     model being reachable, which is not this card's surface. When the worker
#     boundary is absent the dispatch is recorded as FAILED — that is a real,
#     correct, observable outcome and the script asserts it as one rather than
#     pretending it is success.
#   * The completion half end to end. Minting a receipt requires a real batch to
#     reach `complete`/`partial`, which needs the worker above. The join, the
#     refusals, and the receipt are covered by
#     backend/test/cljs/knoxx/backend/infra/translation_dispatch_test.cljs.
#     Note that minting evidence also requires a SYSTEM-ADMIN worker principal:
#     an org admin can update batch status but cannot make Knoxx believe a
#     translation exists. That check is covered by
#     backend/test/cljs/knoxx/backend/infra/routes/translation_worker_principal_test.cljs.
#
# The fixture is created and destroyed by this script. It writes ONLY inside
# ${CONTRACTS_DIR}/_verify_translation_dispatch and removes that directory on
# exit, including on failure or Ctrl-C, so a killed run leaves nothing behind.
#
# Usage:
#   scripts/verify-translation-dispatch.sh
#   KNOXX_BASE_URL=http://localhost:8000 KNOXX_API_KEY=... scripts/verify-translation-dispatch.sh
#
# Exit code is 0 only when every check passed.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${KNOXX_BASE_URL:-http://localhost:8000}"
API_KEY="${KNOXX_API_KEY:-}"
CONTRACTS_DIR="${KNOXX_CONTRACTS_DIR:-${REPO_ROOT}/contracts}"
FIXTURE_DIR="${CONTRACTS_DIR}/_verify_translation_dispatch"

# Unique per run. A fixed identity made every run after the first reuse the
# previous run's dispatch claim: the claim is durable, so the second run got
# `dispatch/duplicate` and verified nothing. A fresh document and revision per
# run means each run exercises a genuinely fresh dispatch.
RUN_ID="$(date -u +%Y%m%d%H%M%S)$$"
NS="knoxx.verifydispatch"
DOC_LOCAL="probe${RUN_ID}"
DOC_ID="${NS}/${DOC_LOCAL}"
PUB_TRANSLATED_ID="${NS}/${DOC_LOCAL}-es"
PUB_NATIVE_ID="${NS}/${DOC_LOCAL}-en"
SOURCE_REL="docs/verify-dispatch-probe-${RUN_ID}.md"
SOURCE_FILE="${REPO_ROOT}/${SOURCE_REL}"
PINNED_REVISION="rev-verify-dispatch-${RUN_ID}"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0
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

# A known gap prints every run so it stays visible, but does not fail the run:
# a permanently red verification script stops being read.
warn() {
  WARN_COUNT=$((WARN_COUNT + 1))
  printf '%s   WARN%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"
}

die() { printf '\n%sABORT%s %s\n\n' "$C_RED$C_BOLD" "$C_RESET" "$1" >&2; exit 2; }

# ── HTTP ───────────────────────────────────────────────────────────────────

# Emits "<status>\n<body>". Body may be empty.
http() {
  local method="$1" path="$2" authorized="$3" body="${4:-}"
  local args=(-s -o /dev/stdout -w $'\n%{http_code}' -X "$method" --max-time 60)
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

expect_jq() {
  local label="$1" filter="$2" response="$3"
  local payload; payload="$(body_of "$response")"
  if printf '%s' "$payload" | jq -e "$filter" >/dev/null 2>&1; then
    pass "$label"
    return 0
  fi
  fail "$label — jq filter did not hold: ${filter}" \
    "$(printf '%s' "$payload" | jq -c . 2>/dev/null | head -c 400 || printf '%s' "$payload" | head -c 400)"
  return 1
}

# ── Fixture ────────────────────────────────────────────────────────────────
#
# Two publication intents on one document, and the contrast between them is the
# point. `probe-es` wants Spanish from an English source, so translation is
# required and work is derivable. `probe-en` wants English from an English
# source, so `translation-required?` is false and no work may ever be derived
# for it — a dispatch that queued it would be translating a document into the
# language it is already written in.
#
# The revision is PINNED rather than :source/current, so this script does not
# depend on the source file's digest being resolvable from the backend's working
# directory. The digest path has its own coverage in
# backend/test/cljs/knoxx/backend/infra/publication_source_revision_test.cljs.

FIXTURE_OWNED=0
SOURCE_OWNED=0

fixture_write() {
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/probe.edn" <<EDN
;; Throwaway fixture written by scripts/verify-translation-dispatch.sh.
{:namespace :${NS}
 :resources
 [{:document/id :${DOC_LOCAL}
   :document/title "Translation Dispatch Verification Probe"
   :document/source-locale :en
   :document/source {:path "${SOURCE_REL}"}}

  {:garden/id :probe-garden
   :garden/title "Dispatch Verification Garden"
   :garden/status :active
   :garden/locales [:en :es]}

  {:publication/id :${DOC_LOCAL}-es
   :publication/document :${DOC_LOCAL}
   :publication/garden :probe-garden
   :publication/locale :es
   :publication/revision "${PINNED_REVISION}"
   :publication/state :published
   :publication/path "/verify-dispatch/${DOC_LOCAL}-es"
   :translation/review :required}

  {:publication/id :${DOC_LOCAL}-en
   :publication/document :${DOC_LOCAL}
   :publication/garden :probe-garden
   :publication/locale :en
   :publication/revision "${PINNED_REVISION}"
   :publication/state :published
   :publication/path "/verify-dispatch/${DOC_LOCAL}-en"
   :translation/review :none}]}
EDN
}

cleanup() {
  local code=$?
  local signalled="${1:-}"
  # On EXIT, $? is the script's real status. On a signal it is not — it is
  # whatever the last command returned — so a run killed mid-check would tear
  # down and then report success. The signal handlers pass 128+n explicitly.
  if [ -n "$signalled" ]; then code="$signalled"; fi
  if [ "$FIXTURE_OWNED" -eq 1 ] && [ -d "$FIXTURE_DIR" ]; then
    rm -rf "$FIXTURE_DIR"
    note "torn down ${FIXTURE_DIR#$REPO_ROOT/}"
  fi
  # Only remove the source file if this run created it. A checkout that already
  # had one is not this script's to delete.
  if [ "$SOURCE_OWNED" -eq 1 ] && [ -f "$SOURCE_FILE" ]; then
    rm -f "$SOURCE_FILE"
    note "torn down ${SOURCE_REL}"
  fi
  exit "$code"
}
trap cleanup EXIT
trap 'trap - EXIT; cleanup 130' INT
trap 'trap - EXIT; cleanup 143' TERM

# ── Preflight ──────────────────────────────────────────────────────────────

printf '%s\n' "${C_BOLD}Knoxx translation work dispatch — live verification${C_RESET}"
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
  die "no Knoxx backend answering at ${BASE_URL}. Start it from THIS checkout (see docs/verification/translation-dispatch.md) and retry."
fi
note "backend is reachable"

# ── 0. the running backend serves this checkout ────────────────────────────
#
# Without this, every check below is being run against somebody else's code.

step "0. the running backend serves this checkout"
FIXTURE_OWNED=1
if [ ! -f "$SOURCE_FILE" ]; then
  SOURCE_OWNED=1
  mkdir -p "$(dirname "$SOURCE_FILE")"
  printf '# Translation dispatch verification probe\n\nSeeded by scripts/verify-translation-dispatch.sh.\n' \
    > "$SOURCE_FILE"
fi
fixture_write
sleep 1

probe="$(http GET "/api/publications/documents" auth)"
if [ "$(status_of "$probe")" = "000" ]; then
  die "backend did not answer /api/publications/documents"
fi
if body_of "$probe" | jq -e --arg id "$DOC_ID" '[.documents[].document.id] | index($id)' >/dev/null 2>&1; then
  pass "seeded fixture is visible to the running backend"
else
  printf '%s   FAIL%s  the running backend cannot see %s\n' "$C_RED" "$C_RESET" "${FIXTURE_DIR#$REPO_ROOT/}"
  note "It is probably serving a different checkout. Check: pm2 describe knoxx-backend | grep cwd"
  note "Response was: $(body_of "$probe" | head -c 300)"
  die "cannot verify against a backend that is not running this code"
fi

# ── 1. the dispatch route refuses an unauthenticated caller ────────────────
#
# Dispatch mutates a shared worker queue. A route that answers an anonymous
# caller lets anyone enqueue translation work for the whole corpus.

step "1. the dispatch route refuses an unauthenticated caller"

expect_status "POST /api/publications/translations/dispatch          is refused" "401 403" \
  "$(http POST "/api/publications/translations/dispatch" anon '{}')"
expect_status "POST /api/publications/translations/dispatch (scoped) is refused" "401 403" \
  "$(http POST "/api/publications/translations/dispatch" anon "{\"document\":\"${DOC_ID}\"}")"

# ── 2. a malformed request is refused, not reinterpreted ───────────────────
#
# The body's only field is optional, which makes an unrecognized field
# dangerous: silently ignored, `{"documnet": "..."}` becomes a whole-corpus
# sweep the caller never asked for.

step "2. a malformed request is refused, not reinterpreted"

expect_status "an unrecognized body field is refused" "400" \
  "$(http POST "/api/publications/translations/dispatch" auth "{\"documnet\":\"${DOC_ID}\"}")"
expect_status "an unqualified document id is refused" "400" \
  "$(http POST "/api/publications/translations/dispatch" auth '{"document":"probe"}')"
note "an unqualified id would sweep nothing while reporting success"

# ── 3. derived work is dispatched, and bound to the concrete revision ──────

step "3. derived work is dispatched and revision-bound"

resp="$(http POST "/api/publications/translations/dispatch" auth "{\"document\":\"${DOC_ID}\"}")"
dispatch_status="$(status_of "$resp")"

if [ "$dispatch_status" = "503" ]; then
  warn "translation evidence persistence is unavailable (MongoDB not connected)"
  note "The route refuses rather than accepting a dispatch whose revision binding"
  note "would be lost on restart. Start MongoDB and re-run to verify sections 3-5."
elif [ "$dispatch_status" = "200" ]; then
  pass "POST /api/publications/translations/dispatch ${C_DIM}(200)${C_RESET}"

  # Wire shape: `clj->js` renders a map KEY with `name`, so :dispatch/outcome
  # arrives as "outcome"; `encode-wire-values` keeps keyword VALUES qualified,
  # so :dispatch/accepted arrives as "dispatch/accepted".
  expect_jq "both intents on the document were considered" \
    '.considered == 2' "$resp"

  expect_jq "exactly one intent derived translation work" \
    '(.dispatched | length) == 1' "$resp"

  expect_jq "the derived work is the intent whose locale differs from the source" \
    "[.. | strings] | index(\"${PUB_TRANSLATED_ID}\")" "$resp"

  expect_jq "no work was derived for the same-locale intent" \
    "[.. | strings] | index(\"${PUB_NATIVE_ID}\") | not" "$resp"
  note "probe-en asks for English from an English source, so no work is derivable for it"

  outcome="$(body_of "$resp" | jq -r '.dispatched[0].outcome // "unknown"' 2>/dev/null)"
  case "$outcome" in
    dispatch/accepted)
      pass "the worker accepted the batch ${C_DIM}(dispatch/accepted)${C_RESET}"
      expect_jq "the accepted dispatch is bound to the pinned concrete revision" \
        "[.. | strings] | index(\"${PINNED_REVISION}\")" "$resp"
      ;;
    dispatch/failed)
      # A correct, observable outcome — not a pass and not a silent skip.
      warn "the worker boundary refused the batch (dispatch/failed)"
      note "OpenPlanner is probably not reachable from this backend. The dispatch was"
      note "still claimed and recorded, which is what section 4 checks."
      ;;
    *)
      fail "unrecognized dispatch outcome" "$outcome"
      ;;
  esac
else
  fail "POST /api/publications/translations/dispatch — expected 200 or 503, got ${dispatch_status}" \
    "$(body_of "$resp" | head -c 400)"
fi

# ── 4. asking twice does not translate twice ───────────────────────────────
#
# The claim is taken before the worker is called, so a second ask has to collapse
# to a duplicate. This is the check that would have caught a dispatch that
# called the worker first and recorded afterwards.

step "4. asking twice does not translate twice"

if [ "$dispatch_status" = "200" ]; then
  again="$(http POST "/api/publications/translations/dispatch" auth "{\"document\":\"${DOC_ID}\"}")"
  expect_status "POST /api/publications/translations/dispatch (again)" "200" "$again"
  repeat_outcome="$(body_of "$again" | jq -r '.dispatched[0].outcome // "none"' 2>/dev/null)"
  case "$outcome" in
    dispatch/accepted)
      # The first attempt is still in flight, so the only correct answer is
      # duplicate. A fresh accept here would mean a second batch translating the
      # same revision, and the second translation cannot be withdrawn.
      if [ "$repeat_outcome" = "dispatch/duplicate" ]; then
        pass "the second ask reused its dispatch identity ${C_DIM}(dispatch/duplicate)${C_RESET}"
      else
        fail "an in-flight claim must collapse to duplicate" "$repeat_outcome"
      fi
      ;;
    dispatch/failed)
      # A failed attempt is retriable BY DESIGN: no translation came of it, so
      # the work still needs doing. Reporting duplicate here would strand this
      # source revision forever — the gate would keep saying the translation is
      # missing while every pass answered duplicate.
      if [ "$repeat_outcome" = "dispatch/accepted" ]; then
        pass "a failed attempt was replaced by a fresh one ${C_DIM}(retriable)${C_RESET}"
      elif [ "$repeat_outcome" = "dispatch/duplicate" ]; then
        fail "a failed attempt was treated as terminal — this revision can never be translated" \
          "$repeat_outcome"
      else
        warn "second ask reported ${repeat_outcome} after a failed first attempt"
      fi
      ;;
    *)
      warn "second ask reported ${repeat_outcome}"
      ;;
  esac
else
  warn "skipped — section 3 did not dispatch"
fi

# ── 5. nothing was published, and no translation was fabricated ────────────
#
# Dispatch asks for a translation. It must not, on its own, make anything public
# or claim a translation exists.

step "5. dispatch alone publishes nothing and fabricates no translation"

if [ "$dispatch_status" = "200" ]; then
  expect_jq "the dispatch response claims no completed translation" \
    '[.. | strings] | index("translation/completed") | not' "$resp"
  expect_jq "the dispatch response claims no materialization" \
    '[.. | strings] | index("publication/materialized") | not' "$resp"
else
  warn "skipped — section 3 did not dispatch"
fi

cms="$(http GET "/api/cms/publications/documents" auth)"
if [ "$(status_of "$cms")" = "200" ]; then
  expect_jq "the seeded document is still not materialized anywhere" \
    "[.. | objects | select(.observed != null)] | length == 0" "$cms"
else
  warn "GET /api/cms/publications/documents answered $(status_of "$cms") — could not check"
fi

warn "the worker actually translating is out of this card's scope (see header)"
warn "the completion half needs a real batch reaching complete/partial (see header)"

# ── Durable residue ────────────────────────────────────────────────────────
#
# What this script CANNOT tear down, stated every run rather than left for
# somebody to discover in a collection.

if [ "$dispatch_status" = "200" ]; then
  warn "left behind: one Knoxx dispatch record in ${C_BOLD}knoxx_translation_dispatches${C_RESET}"
  note "identity: ${DOC_ID} @ ${PINNED_REVISION}"
  note "There is no delete surface for a dispatch claim, and inventing one for a"
  note "verification script would be a worse trade than the residue: a route that"
  note "erases translation evidence is a route that can erase real evidence."
  note "The identity is unique per run, so nothing is reused and nothing collides."
  if [ "$outcome" = "dispatch/accepted" ]; then
    warn "left behind: one OpenPlanner translation batch on the shared worker queue"
    note "It will be claimed, attempt one document, and terminate on its own."
  fi
fi

# ── Summary ────────────────────────────────────────────────────────────────

printf '\n%s%s%s\n' "$C_BOLD" "$(printf '═%.0s' $(seq 1 62))" "$C_RESET"
printf '%s  %s passed%s' "$C_GREEN" "$PASS_COUNT" "$C_RESET"
[ "$WARN_COUNT" -gt 0 ] && printf '%s, %s known gaps%s' "$C_YELLOW" "$WARN_COUNT" "$C_RESET"
if [ "$FAIL_COUNT" -gt 0 ]; then
  printf '%s, %s FAILED%s\n' "$C_RED" "$FAIL_COUNT" "$C_RESET"
  for f in "${FAILURES[@]}"; do
    printf '%s    - %s%s\n' "$C_RED" "$f" "$C_RESET"
  done
  printf '\n'
  exit 1
fi
printf '\n\n'
exit 0
