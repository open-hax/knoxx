#!/usr/bin/env bash
# CMS resource policy plus native Fastify HTTP creation from this checkout.
# Isolated fixtures only; no credentials, providers or deployed content change.
set -euo pipefail
verify_repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v pnpm >/dev/null
command -v clojure >/dev/null
command -v node >/dev/null
command -v timeout >/dev/null
test -f "$verify_repo/backend/test/cljs/knoxx/backend/cms_source_locale_review_test.cljs"
test -d "$verify_repo/backend/node_modules"
if ! verify_revision="$(git -C "$verify_repo" rev-parse --verify 'HEAD^{commit}')"; then
  printf '%s\n' 'FAIL cannot resolve this checkout to a Git commit.' >&2
  exit 1
fi
if [[ ! "$verify_revision" =~ ^[0-9a-f]{40}$ ]]; then
  printf '%s\n' 'FAIL Git did not return a full commit identity for this checkout.' >&2
  exit 1
fi
printf 'Verifying checkout %s at %s\n' "$verify_repo" "$verify_revision"
verify_fixture="$(mktemp -d "${TMPDIR:-/tmp}/knoxx-source-locale-review.XXXXXX")"
trap 'rm -rf -- "$verify_fixture"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
cd "$verify_repo/backend"
NODE_OPTIONS="$(node --input-type=module -e 'import { testEnvironment } from "./scripts/shadow-test-environment.mjs"; process.stdout.write(testEnvironment().NODE_OPTIONS);')"
export NODE_OPTIONS
verify_log="$verify_fixture/results.log"
verify_config="{:ns-regexp \"^knoxx\\\\.backend\\\\.cms-source-locale-review-test$\" :output-to \"$verify_fixture/test.cjs\"}"
if ! timeout 240s pnpm exec shadow-cljs compile test --config-merge "$verify_config" >"$verify_log" 2>&1; then
  cat "$verify_log"
  printf '%s\n' 'FAIL compilation exited nonzero or exceeded four minutes.' >&2
  exit 1
fi
cat "$verify_log"
validate_log() {
node --input-type=module - "$1" <<'JS'
import { readFileSync } from 'node:fs';
import { testCountersExitCode } from './scripts/run-shadow-tests-ci.mjs';

const output = readFileSync(process.argv[2], 'utf8');
if (testCountersExitCode(output) !== 0 || /\b[1-9][0-9]* warnings?\b/.test(output)) {
  console.error('FAIL a nonempty guarded test run with zero warnings did not pass.');
  process.exit(1);
}
JS
}
validate_log "$verify_log"
verify_http_log="$verify_fixture/http-results.log"
if ! timeout 240s bash "$verify_repo/scripts/verify-cms-history.sh" >"$verify_http_log" 2>&1; then
  cat "$verify_http_log"
  printf '%s\n' 'FAIL native CMS HTTP verification exited nonzero or exceeded four minutes.' >&2
  exit 1
fi
cat "$verify_http_log"
validate_log "$verify_http_log"
verify_publication_log="$verify_fixture/publication-results.log"
verify_publication_config="{:ns-regexp \"^knoxx\\\\.backend\\\\.infra\\\\.publication-target-static-site-test$\" :output-to \"$verify_fixture/publication-test.cjs\"}"
if ! timeout 240s pnpm exec shadow-cljs compile test --config-merge "$verify_publication_config" >"$verify_publication_log" 2>&1; then
  cat "$verify_publication_log"
  printf '%s\n' 'FAIL native filesystem publication verification exited nonzero or exceeded four minutes.' >&2
  exit 1
fi
cat "$verify_publication_log"
validate_log "$verify_publication_log"
printf '%s\n' \
  'PASS native Fastify CMS HTTP creation persists English :none and Spanish :required review policies.' \
  'PASS the generated document source path resolves to actual saved bytes for publication admission.' \
  'PASS emitted English source intent is admitted for its concrete source digest without translation evidence.' \
  'PASS withheld English and every locale without a resolved source remain inadmissible.' \
  'PASS Spanish remains blocked without a content-bound candidate and exact source/output approval.' \
  'PASS another source, output, content digest, or later English edit cannot reuse the Spanish approval.' \
  'PASS corrected target bytes at the same source revision remain blocked until their renewed approval.' \
  'PASS approved output B replaces output A in the native filesystem manifest/artifact with a distinct publish key.' \
  'PASS historical source-only keys remain readable and previous completion records survive.' \
  'PASS repeated approved-output restorations reserve deterministic generations, while identical current output converges without rewriting.' \
  'WARN HTTP principals are seeded at the auth-context seam; deployed checkout/image and password login remain unverified.' \
  'WARN provider output, persisted split-review mutations, browser UI and deployed materialization need separate live evidence.' \
  'WARN use the deployed CMS/translation walkthrough in docs/verification/cms-source-locale-review.md for live proof.'
