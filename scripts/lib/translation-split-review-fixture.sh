#!/usr/bin/env bash
# Shared, run-scoped resource and durable-candidate fixture for translation
# review verification. The sourcing script owns traps and reporting.

# shellcheck shell=bash
# shellcheck disable=SC2034  # constants are consumed by the sourcing scripts

TRANSLATION_FIXTURE_NS="knoxx.verifysplit.r${RUN_ID}"
TRANSLATION_FIXTURE_DOC_LOCAL="doc01"
TRANSLATION_FIXTURE_DOC_ID="${TRANSLATION_FIXTURE_NS}/${TRANSLATION_FIXTURE_DOC_LOCAL}"
TRANSLATION_FIXTURE_GARDEN_ID="${TRANSLATION_FIXTURE_NS}/review-garden"
TRANSLATION_FIXTURE_PUBLICATION_ID="${TRANSLATION_FIXTURE_NS}/${TRANSLATION_FIXTURE_DOC_LOCAL}-es"
TRANSLATION_FIXTURE_PUBLICATION_PATH="/verify-split-review/${RUN_ID}/${TRANSLATION_FIXTURE_DOC_LOCAL}-es"
TRANSLATION_FIXTURE_TITLE_PREFIX="Split Review Fixture"
TRANSLATION_FIXTURE_DOCUMENT_TITLE="${TRANSLATION_FIXTURE_TITLE_PREFIX} 01"
TRANSLATION_FIXTURE_COUNT=18

TRANSLATION_FIXTURE_CONTRACTS_PARENT="$(cd "$(dirname "${CONTRACTS_DIR}")" && pwd)"
TRANSLATION_FIXTURE_CONTRACTS_NAME="$(basename "${CONTRACTS_DIR}")"
TRANSLATION_FIXTURE_SOURCE_REL="${TRANSLATION_FIXTURE_CONTRACTS_NAME}/$(basename "${FIXTURE_DIR}")/source-01.md"
TRANSLATION_FIXTURE_SOURCE_FILE="${TRANSLATION_FIXTURE_CONTRACTS_PARENT}/${TRANSLATION_FIXTURE_SOURCE_REL}"

translation_fixture_source_rel() {
  local index="$1"
  printf '%s/%s/source-%02d.md' \
    "$TRANSLATION_FIXTURE_CONTRACTS_NAME" "$(basename "$FIXTURE_DIR")" "$index"
}

translation_fixture_write() {
  mkdir -p "$FIXTURE_DIR"

  {
    printf '%s\n' ";; Throwaway fixture written by translation split-review verification."
    printf '{:namespace :%s\n :resources\n [' "$TRANSLATION_FIXTURE_NS"

    local index local_id source_rel
    for index in $(seq 1 "$TRANSLATION_FIXTURE_COUNT"); do
      local_id="$(printf 'doc%02d' "$index")"
      source_rel="$(translation_fixture_source_rel "$index")"
      printf '\n  {:document/id :%s\n' "$local_id"
      printf '   :document/title "%s %02d"\n' "$TRANSLATION_FIXTURE_TITLE_PREFIX" "$index"
      printf '   :document/source-locale :en\n'
      printf '   :document/visibility :public\n'
      printf '   :document/source {:path "%s"}}\n' "$source_rel"
    done

    printf '\n  {:garden/id :review-garden\n'
    printf '   :garden/title "Split Review Verification Garden"\n'
    printf '   :garden/status :active\n'
    printf '   :garden/locales [:en :es]}\n'

    for index in $(seq 1 "$TRANSLATION_FIXTURE_COUNT"); do
      local_id="$(printf 'doc%02d' "$index")"
      printf '\n  {:publication/id :%s-es\n' "$local_id"
      printf '   :publication/document :%s\n' "$local_id"
      printf '   :publication/garden :review-garden\n'
      printf '   :publication/target :open-hax.publication/static-site\n'
      printf '   :publication/locale :es\n'
      printf '   :publication/revision :source/current\n'
      printf '   :publication/state :published\n'
      printf '   :publication/path "/verify-split-review/%s/%s-es"\n' "$RUN_ID" "$local_id"
      printf '   :translation/review :required}'
    done
    printf ']}\n'
  } > "${FIXTURE_DIR}/resources.edn"

  printf '%s\n\n%s\n\n%s\n' \
    'First source paragraph for durable split review.' \
    'Second source paragraph for correction and rejection.' \
    'Third source paragraph for document-level review.' \
    > "$TRANSLATION_FIXTURE_SOURCE_FILE"

  local index source_file
  for index in $(seq 2 "$TRANSLATION_FIXTURE_COUNT"); do
    source_file="${TRANSLATION_FIXTURE_CONTRACTS_PARENT}/$(translation_fixture_source_rel "$index")"
    printf 'Source document %02d remains visible without candidate evidence.\n' "$index" \
      > "$source_file"
  done
}

translation_fixture_remove_files() {
  if [[ "$FIXTURE_DIR" == "${CONTRACTS_DIR}/_verify_translation_split_review"* ]] \
     && [ -d "$FIXTURE_DIR" ]; then
    rm -rf -- "$FIXTURE_DIR"
  fi
}

translation_fixture_dependency_classpath() {
  local raw_classpath dependency_dir entry
  local -a classpath_entries selected
  dependency_dir="$1"
  raw_classpath="$(cd "${REPO_ROOT}/backend" && clojure -Spath -M:cljs)" || return 1

  IFS=':' read -r -a classpath_entries <<< "$raw_classpath"
  selected=("${REPO_ROOT}/backend/scripts" "${REPO_ROOT}/backend/src/cljs" "$dependency_dir")
  for entry in "${classpath_entries[@]}"; do
    case "$entry" in
      *'/malli-'*.jar|*'/dynaload-'*.jar)
        unzip -qo "$entry" -d "$dependency_dir" || return 1
        ;;
      *.jar)
        ;;
      *)
        selected+=("$entry")
        ;;
    esac
  done
  local joined
  joined="$(IFS=:; printf '%s' "${selected[*]}")"
  printf '%s' "$joined"
}

translation_fixture_helper() {
  local action="$1" project_value dependency_dir helper_classpath output
  dependency_dir="${VERIFY_TMP_DIR}/cljs-deps"
  mkdir -p "$dependency_dir"
  helper_classpath="$(translation_fixture_dependency_classpath "$dependency_dir")" \
    || return 1
  project_value="${VERIFY_PROJECT:-__NONE__}"

  output="$(
    cd "${REPO_ROOT}/backend" || exit 1
    VERIFY_RUN_ID="$RUN_ID" \
    VERIFY_ORG_ID="$VERIFY_ORG_ID" \
    VERIFY_PROJECT="$project_value" \
    VERIFY_DOCUMENT_ID="$TRANSLATION_FIXTURE_DOC_ID" \
    VERIFY_GARDEN_ID="$TRANSLATION_FIXTURE_GARDEN_ID" \
    VERIFY_PUBLICATION_ID="$TRANSLATION_FIXTURE_PUBLICATION_ID" \
    VERIFY_PUBLICATION_PATH="$TRANSLATION_FIXTURE_PUBLICATION_PATH" \
    VERIFY_DOCUMENT_TITLE="$TRANSLATION_FIXTURE_DOCUMENT_TITLE" \
    VERIFY_TARGET_LOCALE="es" \
    VERIFY_SOURCE_FILE="$TRANSLATION_FIXTURE_SOURCE_FILE" \
    KNOXX_PUBLICATION_CONTENT_ROOT="$KNOXX_PUBLICATION_CONTENT_ROOT" \
    ./node_modules/.bin/nbb \
      --classpath "$helper_classpath" \
      "${REPO_ROOT}/backend/scripts/translation_split_review_fixture.cljs" "$action"
  )" || return 1
  printf '%s\n' "$output" | tail -n 1
}
