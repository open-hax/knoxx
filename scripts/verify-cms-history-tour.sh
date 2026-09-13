#!/usr/bin/env bash
# Builds this checkout and serves real CMS routes with a disposable identity and
# garden topology. No deployment credentials or hand-authored document needed.
# Password login, publication execution and unrelated services are outside this tour.
set -euo pipefail
umask 077
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode="${1:-browser}"
case "$mode" in browser|--check|--serve) ;; *) printf 'Usage: %s [--check|--serve]\n' "$0" >&2; exit 2 ;; esac
for tool in pnpm clojure bb curl rg node sha256sum; do command -v "$tool" >/dev/null; done
if [[ "$mode" == browser ]]; then command -v agent-browser >/dev/null; fi
cd "$repo_root"
run_root="$(mktemp -d -t knoxx-cms-tour.XXXXXXXX)"
fixture_root="$run_root/fixture"
mkdir "$fixture_root"
server_pid=""
session="cms-history-tour-${run_root##*.}"
browser_started=0
cleanup() {
  local code=$?
  trap - EXIT INT TERM
  if [[ "$browser_started" == 1 ]]; then ab close >/dev/null 2>&1 || true; fi
  if [[ -n "$server_pid" ]]; then kill "$server_pid" 2>/dev/null || true; wait "$server_pid" 2>/dev/null || true; fi
  rm -rf -- "$run_root"
  printf 'PASS removed disposable fixture %s\n' "$run_root"
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
ab() { NO_PROXY='*' no_proxy='*' HTTP_PROXY='' HTTPS_PROXY='' ALL_PROXY='' agent-browser --session "$session" "$@"; }
build() {
  local name="$1"; shift
  if ! "$@" >"$run_root/$name.log" 2>&1; then cat "$run_root/$name.log"; return 1; fi
  if rg -q '[1-9][0-9]* warnings|^FAIL|^ERROR' "$run_root/$name.log"; then cat "$run_root/$name.log"; return 1; fi
}
head="$(git rev-parse HEAD)"
diff_sha="$(git diff HEAD -- | sha256sum | cut -d ' ' -f 1)"
printf 'Building CMS browser fixture from %s at %s (diff %s)\n' "$repo_root" "$head" "$diff_sha"
build backend pnpm -C backend exec shadow-cljs compile cms-history-tour
build frontend pnpm -C frontend build
CONTRACTS_DIR="$repo_root/backend/test/fixtures/empty-contracts" \
  node backend/target/cms-history-tour.cjs "$fixture_root" "$repo_root/frontend/dist" "$repo_root" "$head" "$diff_sha" \
  >"$run_root/server.log" 2>&1 &
server_pid=$!
receipt="$fixture_root/.ημ/tour.edn"
for ((attempt=0; attempt<200; attempt++)); do
  [[ -s "$receipt" ]] && break
  if ! kill -0 "$server_pid" 2>/dev/null; then cat "$run_root/server.log"; exit 1; fi
  sleep 0.1
done
[[ -s "$receipt" ]] || { cat "$run_root/server.log"; exit 1; }
field() { bb -e '(require (quote [clojure.edn :as edn])) (print (get (edn/read-string (slurp (first *command-line-args*))) (keyword (second *command-line-args*))))' "$receipt" "$1"; }
origin="$(field origin)"
title="$(field title)"
doc_id="$(field document-id)"
initial="$(field initial-revision)"
[[ "$(field head)" == "$head" && "$(field checkout)" == "$repo_root" && "$(field diff-sha)" == "$diff_sha" ]]
curl --noproxy '*' --fail --silent --show-error --cookie-jar "$run_root/cookies" "$(field start-url)" >/dev/null
[[ "$(curl --noproxy '*' --silent --output /dev/null --write-out '%{http_code}' "$origin/api/cms/documents")" == 403 ]]
curl --noproxy '*' --fail --silent --show-error --cookie "$run_root/cookies" "$origin/api/cms/documents/$doc_id/history" >"$run_root/history.json"
bb -e '(require (quote [cheshire.core :as json])) (let [h (json/parse-string (slurp (first *command-line-args*)) true)] (assert (= #{"First fixture revision" "Second fixture revision"} (set (map :content (:revisions h)))))) (println "PASS real CMS routes retain both fixture revisions and reject anonymous reads")' "$run_root/history.json"
printf 'WARN identity and garden topology are seeded fixtures; password login, publication and unrelated services are not exercised.\n'
case "$mode" in
  --check) exit 0 ;;
  --serve) printf 'Fixture receipt: %s\nStop with Ctrl-C to clean up.\n' "$receipt"; wait "$server_pid"; exit 0 ;;
esac
shot_dir="${KNOXX_SHOT_DIR:-$repo_root/docs/verification/screenshots/cms-history-${run_root##*.}}"
mkdir -p "$shot_dir"
browser_started=1
ab open "$(field start-url)"
ab set media dark
ab wait --text "$title"
ab snapshot -i >"$run_root/browser.txt"
ab find role button click --name "$title"
ab wait --fn 'document.querySelector("[aria-label=\"Document content\"]")?.value === "Second fixture revision"'
ab get value '[aria-label="Document content"]' >"$run_root/current.txt"
rg -q 'Second fixture revision' "$run_root/current.txt"
ab screenshot "$shot_dir/01-document.png"
ab find role button click --name History --exact
ab select '[aria-label="History version"]' "$initial"
ab wait --text 'First fixture revision'
ab snapshot >"$run_root/history-ui.txt"
rg -q 'First fixture revision' "$run_root/history-ui.txt"
ab screenshot "$shot_dir/02-history.png"
printf 'PASS browser opened the unique fixture and displayed its retained initial revision. Screenshots: %s\n' "$shot_dir"
