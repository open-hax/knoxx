#!/usr/bin/env bash
# Native CMS -> desired intent -> reconciliation -> anonymous artwork/readback.
# Owns a disposable loopback process and every seeded record; never attaches to
# PM2, Mongo, a model provider, an existing CMS ledger or a deployed publication.
# --browser captures the public page. --serve allows a human/Cua tour until Ctrl-C.
set -euo pipefail
umask 077
media_repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
media_mode="${1:---check}"
case "$media_mode" in --check|--browser|--serve) ;; *) printf 'Usage: %s [--check|--browser|--serve]\n' "$0" >&2; exit 2 ;; esac
for media_tool in git node pnpm clojure python3 timeout rg; do command -v "$media_tool" >/dev/null; done
if [[ "$media_mode" == --browser ]]; then command -v agent-browser >/dev/null; fi
media_head="$(git -C "$media_repo" rev-parse --verify 'HEAD^{commit}')"
[[ "$media_head" =~ ^[0-9a-f]{40}$ ]] || { printf 'FAIL unresolved checkout commit identity\n' >&2; exit 1; }
media_git_root="$(git -C "$media_repo" rev-parse --show-toplevel)"
[[ "$(cd -P -- "$media_git_root" && pwd -P)" == "$media_repo" ]] || { printf 'FAIL source root is not this Git checkout\n' >&2; exit 1; }
media_art="${KNOXX_VERIFY_ART_FILE:?Set KNOXX_VERIFY_ART_FILE to the existing selected Sutured_Signal.svg asset.}"
media_frontend="${KNOXX_VERIFY_FRONTEND_DIST:-}"
if [[ -n "$media_frontend" && ! -f "$media_frontend/index.html" ]]; then printf 'FAIL optional frontend build is missing index.html\n' >&2; exit 1; fi
media_art_sha="889bdd17806386fd1915f190e0d3a09960c9b567d561b3f9eab78f402caeb009"
[[ -f "$media_art" && -d "$media_repo/backend/node_modules" ]] || { printf 'FAIL real selected art or installed backend dependencies are absent\n' >&2; exit 1; }
python3 - "$media_art" "$media_art_sha" <<'PY'
import hashlib, pathlib, sys
actual = hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest()
if actual != sys.argv[2]:
    raise SystemExit('FAIL supplied artwork differs from the selected real asset digest')
PY
printf 'Verifying physical checkout %s at %s; runtime node %s\n' "$media_repo" "$media_head" "$(node --version)"
media_run="$(mktemp -d "${TMPDIR:-/tmp}/knoxx-approved-media.XXXXXXXX")"
media_fixture="$media_run/fixture"
mkdir "$media_fixture"
media_pid=""
media_browser_started=0
media_session="approved-cms-media-${media_run##*.}"
ab() { NO_PROXY='*' no_proxy='*' HTTP_PROXY='' HTTPS_PROXY='' ALL_PROXY='' agent-browser --session "$media_session" "$@"; }
cleanup() {
  local media_code=$? media_cleanup_failed=0
  trap - EXIT INT TERM
  if [[ "$media_browser_started" == 1 ]]; then ab close >/dev/null 2>&1 || media_cleanup_failed=1; fi
  if [[ -n "$media_pid" ]]; then
    kill "$media_pid" 2>/dev/null || true
    for ((media_attempt=0; media_attempt<50; media_attempt++)); do
      kill -0 "$media_pid" 2>/dev/null || break
      sleep 0.1
    done
    if kill -0 "$media_pid" 2>/dev/null; then kill -KILL "$media_pid" 2>/dev/null || media_cleanup_failed=1; fi
    wait "$media_pid" 2>/dev/null || true
  fi
  rm -rf -- "$media_run" || media_cleanup_failed=1
  if [[ "$media_cleanup_failed" == 0 ]]; then printf 'PASS removed only this run-owned CMS, content, evidence and process: %s\n' "$media_run";
  else printf 'FAIL disposable runtime cleanup did not complete\n' >&2; [[ "$media_code" != 0 ]] || media_code=1; fi
  exit "$media_code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
python3 "$media_repo/scripts/verify-approved-cms-media.py" --source-proof >"$media_run/source-before.json"
media_config="{:main knoxx.backend.extern.approved-media-verifier/main :output-to \"$media_run/fixture.cjs\"}"
if ! (cd "$media_repo/backend" && timeout 240s pnpm exec shadow-cljs --force-spawn compile cms-history-tour --config-merge "$media_config") >"$media_run/build.log" 2>&1; then
  cat "$media_run/build.log"; printf 'FAIL fixture compile failed or exceeded four minutes\n' >&2; exit 1
fi
cat "$media_run/build.log"
if ! rg -q 'Build completed[.] .*0 warnings' "$media_run/build.log" || rg -q '\b[1-9][0-9]* warnings\b' "$media_run/build.log"; then
  printf 'FAIL nonempty compiler result with zero warnings was not observed\n' >&2; exit 1
fi
python3 "$media_repo/scripts/verify-approved-cms-media.py" --proof "$media_run/fixture.cjs" >"$media_run/proof.json"
python3 - "$media_run/source-before.json" "$media_run/proof.json" <<'PY'
import json,sys
before=json.load(open(sys.argv[1]))
after=json.load(open(sys.argv[2]))
if any(after.get(key) != value for key,value in before.items()):
    raise SystemExit('FAIL source or checkout identity changed during fixture compilation')
PY
NODE_PATH="$media_repo/backend/node_modules" CONTRACTS_DIR="$media_fixture/contracts" \
  node "$media_run/fixture.cjs" "$media_fixture" "$media_run/proof.json" "$media_art" "$media_art_sha" "$media_frontend" \
  >"$media_run/server.log" 2>&1 &
media_pid=$!
for ((media_attempt=0; media_attempt<200; media_attempt++)); do
  [[ -s "$media_fixture/receipt.json" ]] && break
  if ! kill -0 "$media_pid" 2>/dev/null; then cat "$media_run/server.log"; printf 'FAIL disposable native server stopped before ready\n' >&2; exit 1; fi
  sleep 0.1
done
[[ -s "$media_fixture/receipt.json" ]] || { cat "$media_run/server.log"; printf 'FAIL disposable server readiness timeout\n' >&2; exit 1; }
if ! python3 "$media_repo/scripts/verify-approved-cms-media.py" "$media_fixture" "$media_run/proof.json"; then
  cat "$media_run/server.log"; printf 'FAIL native approved-media checks did not pass\n' >&2; exit 1
fi
media_public_url="$(python3 - "$media_fixture/verification.json" <<'PY'
import json,sys
print(json.load(open(sys.argv[1]))['public-url'])
PY
)"
case "$media_mode" in
  --check) exit 0 ;;
  --serve) printf 'Public artwork/music document: %s\nOwned fixture receipt: %s\nStop with Ctrl-C to remove all fixture state.\n' "$media_public_url" "$media_fixture/receipt.json"; wait "$media_pid"; exit 0 ;;
esac
media_shots="${KNOXX_SHOT_DIR:-$media_repo/docs/verification/screenshots/approved-media-${media_run##*.}}"
mkdir -p "$media_shots"
media_browser_started=1
if [[ -n "$media_frontend" ]]; then
  media_start_url="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["start-url"])' "$media_fixture/receipt.json")"
  media_cms_origin="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["origin"])' "$media_fixture/receipt.json")"
  ab open "$media_start_url"
  ab open "$media_cms_origin/cms"
  ab wait --text 'Approved CMS art and music verification'
  ab find role button click --name 'Approved CMS art and music verification' --exact
  ab wait --fn 'document.querySelector("[aria-label=\"Document content\"]")?.value.includes("Sutured_Signal.svg")'
  ab screenshot "$media_shots/00-native-cms-selected-source.png"
fi
ab open "$media_public_url"
ab wait --text 'Selected artwork and music'
ab wait --fn 'document.querySelector("img")?.complete && document.querySelector("img").naturalWidth > 0'
ab screenshot "$media_shots/01-cms-published-art-and-music.png"
ab snapshot >"$media_shots/01-cms-published-art-and-music.txt"
ab reload
ab wait --fn 'document.querySelectorAll("img").length === 1 && document.querySelectorAll("iframe,script,audio,video").length === 0'
ab screenshot "$media_shots/02-metadata-tamper-keeps-approved-media.png"
printf 'PASS browser displayed real selected artwork and canonical song destination after metadata revision; screenshots: %s\n' "$media_shots"
printf 'WARN browser tour shows the public publication; private CMS editor preview, remote song playback and real translation review need separate live evidence.\n'
