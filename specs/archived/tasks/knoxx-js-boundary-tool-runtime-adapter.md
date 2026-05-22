# Knoxx JS Boundary — Tool Runtime Adapter

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Story points: 5

## Purpose

Create a real boundary for agent tool definitions, tool parameter ingress, custom tool sanitation, and tool update callback payloads. Domain tool code should work with CLJS maps, not JS tool objects and raw `params` objects.

## Problem

`backend/src/cljs/knoxx/backend/domain/tools.cljs` currently owns both domain semantics and JS runtime object mechanics:

- `create-tool-obj` returns a `#js` tool object.
- `->params` calls `clj->js` on Malli JSON schema output.
- `maybe-tool-update!` builds a raw JS callback payload.
- `sanitize-custom-tool-name` reads and mutates tool JS objects with `aget` / `aset`.
- `sanitize-custom-tools` and `filter-custom-tools-by-allow-set` convert JS arrays with `xjs/js-array-seq`.
- `json-parse` parses JSON directly with `js/JSON`.

Downstream domain tools then preserve the leak by accepting raw JS `params` and using `aget` locally.

## Target model

Introduce an explicit tool runtime boundary, for example:

```clojure
(knoxx.backend.extern.tools/tool-definition
  {:name name
   :label label
   :description description
   :prompt-snippet prompt
   :prompt-guidelines prompt-guidelines
   :parameters schema
   :execute execute-fn})

(knoxx.backend.extern.tools/normalize-params params)
(knoxx.backend.extern.tools/send-update! on-update {:content [{:type "text" :text text}]})
```

Domain code receives:

- CLJS tool definition maps before final runtime export
- CLJS params maps in execute functions
- CLJS update payload maps
- CLJS vectors of custom tools for filtering/sanitation

The extern adapter owns final JS object construction and callback invocation shape.

## Deliverables

1. Add a narrow tool runtime adapter namespace, likely `knoxx.backend.extern.tools`.
2. Refactor `domain/tools.cljs` so it no longer imports `extern.js` and no longer constructs raw JS objects directly.
3. Convert execute wrappers so tool params are normalized once at the boundary.
4. Replace wrapper tool `#js` param remapping with CLJS map remapping.
5. Move custom tool JS mutation into the adapter or convert tool objects to CLJS maps before sanitation.
6. Add fixtures for:
   - Malli schema to runtime params conversion
   - prompt guideline sanitation
   - original/canonical tool name preservation
   - update callback payload shape
   - params keywordization and camel/snake compatibility

## Migration notes

1. Do this before the Discord adapter, because Discord tools currently depend on `create-tool-obj`, `maybe-tool-update!`, and raw `params` conventions.
2. The initial compatibility wrapper may still expose JS tool objects to the eta-mu runtime, but all JS construction must be inside the adapter.
3. Do not change public tool names or schemas in this child.

## Acceptance criteria

1. `domain/tools.cljs` does not import `knoxx.backend.extern.js`.
2. `domain/tools.cljs` has no `#js`, `clj->js`, `js->clj`, `js/JSON`, `aget`, or `aset` except documented temporary compatibility forms owned by this child.
3. Domain execute functions can be migrated to CLJS params without changing tool behavior.
4. Tests prove the exported runtime tool object is equivalent for representative fixtures.
5. Backend test build reports zero failures and zero errors.

## Verification

```bash
rg -n "knoxx\.backend\.extern\.js|xjs/js-array-seq|#js|clj->js|js->clj|js/JSON|aget|aset" backend/src/cljs/knoxx/backend/domain/tools.cljs
pnpm -C backend boundary:check
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Verified 2026-05-21:

- Added `knoxx.backend.extern.tools` for runtime tool object construction, parameters conversion, update callback payloads, custom tool sanitation/filtering, and JSON parsing.
- Refactored `domain/tools.cljs` to delegate generic JS mechanics to the adapter and removed its direct `extern.js` import and raw JS conversion forms.
- Added `extern_tools_test.cljs` coverage for representative tool object shape, update payloads, custom-tool original-name preservation, allow-set filtering, and JSON parsing.
- Removed `backend/src/cljs/knoxx/backend/domain/tools.cljs` from the JS boundary allow-list; boundary check passes with 9 remaining allow-listed non-extern generic extern import files.
- Verification passed: `pnpm -C backend boundary:check`, `pnpm -C backend exec shadow-cljs compile test` (344 tests, 899 assertions, 0 failures, 0 errors), and `pnpm -C backend exec shadow-cljs compile server` (build completed with existing warnings).
