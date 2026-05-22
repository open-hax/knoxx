# Knoxx JS Boundary — Stores and Core Memory Codecs

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Story points: 5

## Purpose

Remove generic JSON parsing and JS promise-array handling from store/memory code. Persistence and memory logic should consume CLJS row maps and CLJS result vectors; JSON row decoding and promise aggregation belong to codecs/adapters.

## Problem

Two smaller but important leaks remain in the requested inventory:

- `backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs`
  - imports `knoxx.backend.extern.json`
  - exposes `parse-json-object` as a thin alias to `xjson/parse-object`
- `backend/src/cljs/knoxx/backend/infra/core_memory.cljs`
  - imports `knoxx.backend.extern.js`
  - has direct `js/JSON.parse` / `js->clj` parsing in `parse-json-object`
  - uses `js/Promise.all (clj->js ...)` and `xjs/js-array-seq` in authorization filtering

These are not just mechanical smells. They hide persistence shape rules in arbitrary namespaces rather than declaring codecs for row extras, session title metadata, and memory authorization results.

## Target model

Create focused codecs/adapters:

```clojure
(session-title-codec/parse-row-extra value) ;; => CLJS map
(core-memory-codec/parse-row-extra value)   ;; => CLJS map
(promise/all-vec promises)                  ;; => Promise<vec<CLJS result>>
```

If a generic JSON parser remains, it should be internal to an adapter and named by the data being decoded at the call site.

## Deliverables

1. Replace `session_titles.cljs` `parse-json-object` alias with a data-specific row codec.
2. Replace `core_memory.cljs` direct JSON parsing with the same row-extra codec or a core-memory-specific codec.
3. Replace `js/Promise.all (clj->js ...)` + `xjs/js-array-seq` with `extern.promise/all-vec` or equivalent promise adapter returning a CLJS vector.
4. Document accepted row-extra shapes and key compatibility:
   - snake_case
   - kebab-case
   - camelCase legacy values where currently consumed
5. Add tests for:
   - invalid JSON returns `{}` or nil exactly as current callers expect
   - CLJS maps pass through unchanged
   - string JSON objects decode to keyword maps
   - non-object JSON is rejected or normalized according to current behavior
   - authorization result aggregation returns a CLJS set of allowed session ids

## File-specific notes

### `infra/stores/session_titles.cljs`

Keep storage behavior unchanged. The migration should only change where JSON parsing lives and how it is named.

### `infra/core_memory.cljs`

`row-extra-map`, session visibility, actor/contract extraction, and authorized-session filtering should operate only on CLJS values. Promise aggregation should resolve to CLJS vectors without local JS array conversion.

## Acceptance criteria

1. `infra/stores/session_titles.cljs` does not import `knoxx.backend.extern.json`.
2. `infra/core_memory.cljs` does not import `knoxx.backend.extern.js`.
3. `infra/core_memory.cljs` has no direct `js/JSON.parse`, `js->clj`, `js/Promise.all (clj->js ...)`, or `xjs/js-array-seq` for these flows.
4. Row-extra compatibility remains unchanged for current session visibility and actor/contract filters.
5. Backend test build reports zero failures and zero errors.

## Verification

```bash
rg -n "knoxx\.backend\.extern\.(js|json)|xjs/js-array-seq|xjson/parse-object|js/JSON|js->clj|js/Promise\.all|clj->js" backend/src/cljs/knoxx/backend/infra/core_memory.cljs backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs
pnpm -C backend boundary:check
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Latest verification (2026-05-21):

- Target grep returned no matches for generic JSON/JS boundary forms in `infra/core_memory.cljs` and `infra/stores/session_titles.cljs`.
- `infra/openplanner/memory.cljs` also no longer imports `knoxx.backend.extern.js` or performs local `Promise.all`/`js->clj` result aggregation for the touched flows.
- `pnpm -C backend boundary:check` passed with 2 remaining allow-listed non-extern generic extern import files.
- `pnpm -C backend exec shadow-cljs compile test` passed: 358 tests, 954 assertions, 0 failures, 0 errors.
- `pnpm -C backend exec shadow-cljs compile server` exited 0 with existing warnings.
- `git diff --check` passed for touched stores/memory codec files, tests, specs, inventory, and allow-list.
