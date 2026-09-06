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

`frontend/scripts/check-cljs-migration-ratchet.mjs` walks the live checkout and emits a newline-delimited EDN manifest with one record for every legacy TypeScript source, legacy TypeScript test suite, bridge export, and bridge-owned route. Records are deterministic and sorted. `knoxx.frontend.infra.migration-manifest` then reads that projection and validates every record against the portable Malli contract in `knoxx.frontend.law.migration-manifest`.

The ratchet enforces:

1. TypeScript and TSX file counts may only decrease.
2. CLJS source count may not decrease.
3. Neither bridge may gain exports.
4. Shadow-owned routing may not gain bridge-owned route components.
5. When a base ref is supplied, a pull request may not add a new production `.ts` or `.tsx` path even if another TypeScript file is deleted in the same change.

Local checks:

```sh
pnpm -C frontend run migration:check
pnpm -C frontend run migration:manifest
```

`migration:manifest` writes `frontend/dist/migration/frontend-surface.ndedn`, compiles the CLJS validator, and Malli-validates the complete generated projection. CI writes the same projection into the runner temporary directory and uploads it as `knoxx-frontend-migration-manifest` for review and archaeology.

The generated ND-EDN file is deliberately not committed: the checkout is authoritative and the projection can always be regenerated exactly. The revision-bound scalar baseline is the CI contract; the manifest is evidence about the exact revision being tested.
