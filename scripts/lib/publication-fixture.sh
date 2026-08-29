#!/usr/bin/env bash
#
# Shared throwaway fixture for the publication verification scripts.
#
# Both scripts/verify-publication-epic.sh and scripts/verify-publication-tour.sh
# source this so they seed byte-identical resources. If they drifted, the API
# run and the browser run would be proving things about different topologies.
#
# Everything written lives under ${FIXTURE_DIR}. Removing that one directory is
# the entire teardown, including the file the PATCH step rewrites.

# shellcheck shell=bash
# shellcheck disable=SC2034  # every FIXTURE_* value is read by the sourcing script

# The seeded namespace. Deliberately not a real one, so nothing here can collide
# with an actual document, garden, or publication.
FIXTURE_NS="knoxx.verify"
FIXTURE_DOC_ID="${FIXTURE_NS}/probe"
FIXTURE_GARDEN_ID="${FIXTURE_NS}/probe-garden"
FIXTURE_PUB_ID="${FIXTURE_NS}/probe-es"
FIXTURE_DOC_TITLE="Publication Verification Probe"
FIXTURE_GARDEN_TITLE="Verification Garden"

fixture_write_valid() {
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/probe.edn" <<'EDN'
;; Throwaway fixture written by the publication verification scripts.
;; A namespace manifest: the namespace is declared once and each entry writes
;; its LOCAL id, which the loader canonicalizes to a qualified keyword BEFORE
;; validation — the Malli shapes require qualified ids, so validating first
;; would drop every one of these.
{:namespace :knoxx.verify
 :resources
 [{:document/id :probe
   :document/title "Publication Verification Probe"
   :document/source-locale :en
   :document/source {:path "docs/verify-probe.md"}}

  ;; :garden/locales is REQUIRED — `law.publication/Garden` has demanded a
   ;; non-empty LocaleCatalog since #250 (publication-locale-catalog), and this
   ;; fixture was not updated with it. A garden without one is dropped by the
   ;; loader, which made step 0 abort with "the backend cannot see the fixture"
   ;; and sent the reader off to check pm2. It must contain the source locale
   ;; and every locale the fixture publishes into.
  {:garden/id :probe-garden
   :garden/title "Verification Garden"
   :garden/status :active
   :garden/locales [:en :es :fr]}

  {:publication/id :probe-es
   :publication/document :probe
   :publication/garden :probe-garden
   :publication/locale :es
   :publication/revision "rev-verify-1"
   :publication/state :withheld
   :publication/path "/verify/probe-es"
   :translation/review :required}]}
EDN
}

fixture_write_invalid() {
  # A publication path with no leading slash. PublicationPath rejects it, so the
  # loader drops the record. The projection must report it as a blocker rather
  # than serving a topology with this intent quietly missing.
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/invalid.edn" <<'EDN'
{:namespace :knoxx.verify
 :resources
 [{:publication/id :probe-broken
   :publication/document :probe
   :publication/garden :probe-garden
   :publication/locale :fr
   :publication/revision "rev-verify-1"
   :publication/state :published
   :publication/path "verify/no-leading-slash"
   :translation/review :none}]}
EDN
}

fixture_write_collision() {
  # Same canonical id as probe.edn's publication, different payload. First-wins
  # dedup would silently keep whichever file the filesystem enumerated first;
  # the projection must call it a conflict instead.
  mkdir -p "$FIXTURE_DIR"
  cat > "${FIXTURE_DIR}/collision.edn" <<'EDN'
{:namespace :knoxx.verify
 :resources
 [{:publication/id :probe-es
   :publication/document :probe
   :publication/garden :probe-garden
   :publication/locale :es
   :publication/revision "rev-verify-CONFLICTING"
   :publication/state :published
   :publication/path "/verify/probe-es-conflicting"
   :translation/review :none}]}
EDN
}

fixture_state_on_disk() {
  sed -n 's/.*:publication\/state[[:space:]]*:\([a-z]*\).*/\1/p' "${FIXTURE_DIR}/probe.edn" 2>/dev/null | head -1
}

fixture_remove() {
  [ -n "${FIXTURE_DIR:-}" ] && [ -d "$FIXTURE_DIR" ] && rm -rf "$FIXTURE_DIR"
}
