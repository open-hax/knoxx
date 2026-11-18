#!/usr/bin/env bash
set -euo pipefail
big_pi_repo="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
big_pi_package="$big_pi_repo/packages/big-pi-optimizely"
if [[ ! -x "$big_pi_package/node_modules/.bin/nbb" ]]; then
  printf 'Install the local package first: npm ci --ignore-scripts --prefix %q\n' "$big_pi_package" >&2
  exit 1
fi
cd "$big_pi_package"
exec ./node_modules/.bin/nbb -cp src -m big-pi.optimizely.main "$@"
