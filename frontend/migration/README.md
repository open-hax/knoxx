# Frontend CLJS migration ratchet

This directory records the monotone boundary for finishing the Knoxx frontend migration to ClojureScript/Helix.

The baseline is revision-bound to `fb08a10a8aa32a594cc97ae11b820113de4cf386` and intentionally records only the scalar limits that CI must preserve. The detailed inventory is a generated projection, not a second hand-maintained source of truth.

At that revision the frontend contains:

- 117 `.tsx` files under `frontend/src`;
- 76 `.ts` files under `frontend/src`;
- 65 `.cljs` files under `frontend/src`;
- 11 exports from the application bridge;
- 19 exports from the frontend/UXX bridge; and
- 8 route registrations in `app.cljs` that still mount application-bridge components.

`frontend/scripts/check-cljs-migration-ratchet.mjs` walks the live checkout and can emit a newline-delimited EDN manifest with one record for every legacy TypeScript source, legacy TypeScript test suite, bridge export, and bridge-owned route. Records are deterministic and sorted.

The ratchet enforces:

1. TypeScript and TSX file counts may only decrease.
2. CLJS source count may not decrease.
3. Neither bridge may gain exports.
4. Shadow-owned routing may not gain bridge-owned route components.
5. When a base ref is supplied, a pull request may not add a new production `.ts` or `.tsx` path even if another TypeScript file is deleted in the same change.

The Malli record contract is `knoxx.frontend.law.migration-manifest` in `frontend/src/cljs` so the manifest shape is portable CLJC data rather than a Node-only convention.

Local checks:

```sh
node frontend/scripts/check-cljs-migration-ratchet.mjs --self-test
node frontend/scripts/check-cljs-migration-ratchet.mjs --check
node frontend/scripts/check-cljs-migration-ratchet.mjs \
  --manifest frontend/migration/frontend-surface.ndedn
```

CI additionally supplies a Git base ref, so path-level and bridge-name regressions are rejected. The generated ND-EDN file is uploaded as a workflow artifact for review and archaeology; it is not committed because the checkout itself is authoritative and the projection can always be regenerated exactly.
