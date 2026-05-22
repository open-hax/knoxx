# Knoxx JS Boundary Inventory and Law Gate

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Source reports:

- `docs/notes/architecture/js-cljs-boundary-report.md`
- `docs/notes/architecture/js-cljs-boundary-domain-plan.md`

Story points: 3

## Purpose

Turn the boundary concern into a measurable rule before refactoring call sites. The first deliverable is not code movement; it is a reliable inventory and a gate that prevents new generic extern alias leaks.

## Problem

The current pattern is ambiguous:

```clojure
[knoxx.backend.extern.js :as xjs]
[knoxx.backend.extern.json :as xjson]
```

Those imports look like a boundary but still let any namespace decide how to interpret raw JS values. Without an explicit law/gate, migrations can simply replace one alias with another and preserve the same leak.

## Scope

Initial scan targets the files named in the user request plus adjacent direct conversion forms in those files:

- `domain/discord/tools.cljs`
- `domain/discord/rest_client.cljs`
- `domain/voice/client.cljs`
- `domain/tools.cljs`
- `infra/http.cljs`
- `infra/core_memory.cljs`
- `infra/stores/session_titles.cljs`
- `infra/clients/openplanner.cljs`
- `domain/bluesky/client.cljs`
- `infra/clients/knoxx_control.cljs`
- `infra/routes/voice.cljs`
- `infra/routes/multimodal.cljs`
- `infra/clients/proxx.cljs`
- `domain/contracts/client.cljs`

The final gate should scan the whole backend, but this child spec should preserve the initial smell inventory as the first migration baseline.

## Implementation results

Completed 2026-05-21.

Created:

- `backend/scripts/check-js-boundary.mjs` — reproducible scanner with `--check`, `--inventory`, and `--markdown` modes.
- `backend/config/js-boundary-allowlist.json` — temporary allow-list tying existing non-extern generic extern imports to child specs or source reports.
- `reports/js-boundary-inventory.md` — generated baseline inventory table.

Added package scripts:

```bash
pnpm -C backend boundary:check
pnpm -C backend boundary:inventory
```

Current gate behavior:

- Scans all `backend/src/cljs/knoxx/backend/**/*.cljs` files.
- Fails when a non-extern file imports `knoxx.backend.extern.js` or `knoxx.backend.extern.json` and that file is not in `backend/config/js-boundary-allowlist.json`.
- Still records broader boundary smells (`xjs/js-array-seq`, `xjson/stringify`, `xjson/parse-object`, direct `js->clj`, `clj->js`, `js/JSON`, `js/Object`, `Object.keys`, `#js`, `aget`, `aset`) in the inventory so child specs can burn down counts.
- Allows existing violations only as active migration debt; adding a new generic extern import requires explicitly editing the allow-list and naming a child spec.

## Deliverables

1. A generated inventory table with counts by file for:
   - `knoxx.backend.extern.js`
   - `knoxx.backend.extern.json`
   - `xjs/js-array-seq`
   - `xjson/stringify`
   - `xjson/parse-object`
   - direct `js->clj`, `clj->js`, `js/JSON`, `js/Object`, `Object.keys`, `#js`, `aget`, `aset`
2. A written allow-list policy for legitimate low-level wrappers:
   - `backend/src/cljs/knoxx/backend/extern/**`
   - `backend/src/cljs/knoxx/backend/domain/node/**`
   - selected `shape/**` schema constructors, if they are truly JS schema output boundaries
3. A script or test target that fails on new non-extern imports of generic `extern.js` / `extern.json`.
4. A migration checklist that each child spec must fill out:
   - JS value origin
   - first conversion point
   - canonical CLJS shape
   - final JS sink
   - regression fixture
5. A temporary allow-list file or section that records active violations by child spec, so the gate can tighten incrementally.

## Acceptance criteria

1. The baseline inventory is reproducible with one command.
2. The gate fails if a new non-extern namespace imports `knoxx.backend.extern.js` or `knoxx.backend.extern.json`.
3. Existing violations are explicitly tied to child specs rather than silently permitted.
4. The policy forbids generic alias replacement as a migration strategy.
5. The policy distinguishes conversion adapters from domain logic; adapters may convert, domain logic may not.

## Verification

```bash
pnpm -C backend boundary:check
pnpm -C backend boundary:inventory > /tmp/js-boundary-inventory.md
cmp -s /tmp/js-boundary-inventory.md reports/js-boundary-inventory.md
```

For later implementation children that change CLJS code:

```bash
pnpm -C backend exec shadow-cljs compile test
```
