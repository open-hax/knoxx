# Restore the sandbox bundle's clj-kondo

The sandbox workflow packages clj-kondo 2025.07.28 for Linux x86-64, including
the upstream static ZIP, its pinned SHA-256, and a standalone Node restore
helper. The pin comes from the [upstream release asset](https://github.com/clj-kondo/clj-kondo/releases/tag/v2025.07.28):
`d6449daf243516fdc703f0629cb077ddfbe1981a5e21a3d1421708f22d1b6ce7`.
Collection rejects bytes that differ from this digest. The bundle's manifest
records the version, host architecture, archive, digest, restore script, and
separate collection/restoration outcomes; `SHA256SUMS` includes all these files.

This closes the gap in the original PR #305 review: installing clj-kondo in
the CI runner did not put it in the exported bundle. The old bundle could not
reliably run its mandatory hook tests after restoration.

After downloading the bundle and extracting its source, use the following in
one shell from the Knoxx source root. Set `KNOXX_SANDBOX_BUNDLE` to the absolute
directory containing the downloaded `manifest.json` and `toolchain/`:

```sh
set -euo pipefail
: "${KNOXX_SANDBOX_BUNDLE:?Set the absolute extracted bundle directory}"
(cd "$KNOXX_SANDBOX_BUNDLE" && sha256sum --check --strict SHA256SUMS)
sandbox_kondo_bin="$(node "$KNOXX_SANDBOX_BUNDLE/toolchain/sandbox-clj-kondo.mjs" \
  restore "$KNOXX_SANDBOX_BUNDLE" "$KNOXX_SANDBOX_BUNDLE/restored-toolchain")"
export PATH="$sandbox_kondo_bin:$PATH"
clj-kondo --version
node --test backend/test/js/defroute-lint-hook.test.mjs \
  backend/test/js/kondo-file-size-hook.test.mjs
```

The helper requires an existing Node 22+ and `unzip`. It supports Linux x86-64
and rejects a different OS or architecture. It checks the archive digest and
contents, executes the extracted binary to check its version, and then installs
it under the chosen destination. A repeated restore replaces only that
version's executable. No preinstalled clj-kondo is used as a fallback. The
restore itself is offline; the bundle's existing dependency setup still needs
its other toolchain prerequisites and network access, as the manifest states.

The workflow rehearses restoration with PATH containing only Node and unzip,
first asserting that clj-kondo is absent. It then restores the actual downloaded
archive and runs all five defroute/file-size hook tests using the restored
binary. The temporary bootstrap and installation directories are removed on
exit. Collection or restoration failure remains a job failure after evidence
upload, even though each step continues so the bundle can record the failure.

Local evidence for this repair: the real upstream archive passes all five
restored-binary hook tests; eight unit tests cover repeat installation, corrupt
bytes, wrong host, extra archive entries, wrong executable version, absent
archive, HTTP failure, and a tampered download. Run those unit checks with
`node --test backend/test/js/sandbox-clj-kondo.test.mjs`; the existing backend
Node test globs also run them; all 67 Node smoke tests pass on the repair tree.
`actionlint .github/workflows/sandbox-bundle.yml`
passes with zero diagnostics. A fresh hosted workflow run remains required;
this local check does not claim full bundle coverage or application startup.
