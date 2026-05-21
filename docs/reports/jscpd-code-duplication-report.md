# jscpd code duplication report

Date: 2026-05-21

## Setup

- Added `.jscpd.json` at the Knoxx repo root.
- Added `pnpm run scan:duplication` to `package.json`.
- Scanner command: `pnpm dlx jscpd@4.2.3 --config .jscpd.json`.
- Raw machine report: `reports/jscpd/jscpd-report.json`.

## Scan scope

The configured scan targets hand-authored code and excludes generated/runtime folders such as `node_modules`, `dist`, `dist-dev`, `coverage`, `.shadow-cljs`, `.clj-kondo/.cache`, `.lsp`, `.cpcache`, timestamped Vite temp modules, and generated `.d.ts` files.

Configured roots:

- `backend/src/cljs`
- `backend/test/cljs`
- `backend/test/js`
- `backend/scripts`
- `backend/test-mcp.mjs`
- `frontend/src`
- `frontend/e2e/knoxx_e2e_support.cjs`
- selected frontend config files
- `scripts`
- `discord-bot/src`

Thresholds:

- `minLines`: 8
- `minTokens`: 80
- `maxLines`: 2000
- `maxSize`: 300kb
- total duplication gate: 3%
- comments skipped

## TDD refactors completed

### 1. Workspace-context frontend extraction

A first test-driven refactor extracted the duplicated chat/context workspace helpers into `frontend/src/components/workspace-context/`.

Red test first:

```bash
NODE_ENV=test pnpm -C frontend exec vitest run --config vitest.config.ts src/components/workspace-context/utils.test.ts
```

It failed because `./utils` did not exist. Then the shared module was implemented and both previous import surfaces were kept as compatibility wrappers:

- `frontend/src/components/workspace-context/types.ts`
- `frontend/src/components/workspace-context/utils.ts`
- `frontend/src/components/workspace-context/utils.test.ts`
- `frontend/src/components/chat-page/types.ts`
- `frontend/src/components/chat-page/utils.ts`
- `frontend/src/components/context-bar/types.ts`
- `frontend/src/components/context-bar/utils.ts`

Two pre-existing frontend test failures also blocked the full frontend suite. They were fixed while preserving current behavior:

- `frontend/src/components/chat-page/VoiceInputButton.test.tsx` now mocks `AudioContext`, matching the hook's real browser dependency.
- `frontend/src/components/admin-page/EventAgentsPanel.test.tsx` now expects the current loud CLJS integration error instead of the removed silent TS fallback.

### 2. OpenUtau backend core consolidation

A second test-driven refactor consolidated duplicated OpenUtau USTX/YAML generation into `backend/src/cljs/knoxx/backend/domain/openutau/openutau.cljs`.

Red signal first:

```bash
pnpm -C backend exec nbb -cp src/cljs:test/cljs -e "(require '[knoxx.backend.domain.openutau.openutau :as openutau]) ..."
```

It showed the canonical OpenUtau core did not yet provide render-safe normalization: one note stayed unpadded with lyric `kyo ukai` instead of becoming 12 notes with lyric `kaikyou`.

Implemented changes:

- `backend/src/cljs/knoxx/backend/domain/openutau/openutau.cljs` now owns:
  - singer registry and default singer resolution
  - render-safe lyric sanitation
  - minimum-note padding for OpenUtau headless rendering
  - USTX project construction
  - YAML emission
  - README text generation
- `backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs` is now a thin compatibility/runtime wrapper:
  - re-exports canonical OpenUtau core vars/functions
  - keeps only `render-script-path` and `render-ustx-to-wav`
- `backend/test/cljs/knoxx/backend/openutau_test.cljs` now asserts canonical render-safe normalization and singer defaults.

## Current result summary

`pnpm run scan:duplication` completes successfully. The total duplicated-line percentage is below the configured 3% gate.

| Metric | Initial scan | After workspace-context | After OpenUtau |
|---|---:|---:|---:|
| Files analyzed | 599 | 608 | 610 |
| Lines analyzed | 112,659 | 113,110 | 114,424 |
| Tokens analyzed | 1,162,308 | 1,165,488 | 1,180,921 |
| Clone groups | 87 | 80 | 77 |
| Duplicated lines | 2,200 | 1,804 | 1,551 |
| Duplicated line percentage | 1.95% | 1.59% | 1.36% |
| Duplicated tokens | 23,146 | 18,905 | 16,700 |
| Duplicated token percentage | 1.99% | 1.62% | 1.41% |

Net reduction from the initial scan:

- clone groups: -10
- duplicated lines: -649
- duplicated line percentage: -0.59 percentage points
- duplicated tokens: -6,446

## Current breakdown by format

| Format | Files | Lines | Clones | Duplicated lines | % lines | % tokens |
|---|---:|---:|---:|---:|---:|---:|
| clojure | 248 | 49,327 | 41 | 555 | 1.13% | 1.42% |
| tsx | 124 | 30,428 | 17 | 459 | 1.51% | 1.51% |
| javascript | 127 | 17,913 | 7 | 217 | 1.21% | 1.18% |
| typescript | 91 | 14,283 | 8 | 195 | 1.37% | 1.11% |
| css | 14 | 2,204 | 4 | 125 | 5.67% | 5.49% |
| bash | 6 | 269 | 0 | 0 | 0% | 0% |

## Current top duplicated files

| Duplicated lines | % | Clones | Format | File |
|---:|---:|---:|---|---|
| 236 | 63.1% | 3 | tsx | `frontend/src/components/layout/WorkbenchShell.tsx` |
| 152 | 15.17% | 4 | javascript | `frontend/src/pages/ContractsPage.tsx` |
| 130 | 19.82% | 10 | clojure | `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs` |
| 124 | 74.7% | 1 | tsx | `frontend/src/components/layout/ResizablePanel.tsx` |
| 122 | 37.31% | 6 | clojure | `backend/test/cljs/knoxx/backend/tools/blaze_music_generate_test.cljs` |
| 120 | 24.64% | 8 | clojure | `backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs` |
| 110 | 30.14% | 10 | clojure | `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` |
| 106 | 33.13% | 3 | css | `frontend/src/pages/CmsPage.module.css` |
| 102 | 76.12% | 3 | css | `frontend/src/pages/ContentEditorPage.module.css` |
| 100 | 52.91% | 8 | clojure | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs` |
| 93 | 17.82% | 7 | clojure | `backend/src/cljs/open_hax/contracts/schema.cljs` |
| 90 | 11.9% | 2 | typescript | `frontend/src/components/chat-page/useChatWorkspaceController.ts` |
| 81 | 45.76% | 2 | typescript | `frontend/src/components/chat-page/chat-page-persistence-suite.ts` |
| 71 | 17.15% | 5 | clojure | `backend/src/cljs/knoxx/backend/law/contracts.cljs` |
| 70 | 23.73% | 2 | javascript | `frontend/src/components/chat-page/MultimodalContent.tsx` |

## Largest remaining clone pairs

| Lines | Format | First | Second |
|---:|---|---|---|
| 125 | tsx | `frontend/src/components/layout/ResizablePanel.tsx:43-167` | `frontend/src/components/layout/WorkbenchShell.tsx:49-154` |
| 57 | tsx | `frontend/src/components/layout/Sidebar.tsx:34-90` | `frontend/src/components/layout/WorkbenchShell.tsx:49-112` |
| 57 | tsx | `frontend/src/components/layout/BottomPanel.tsx:32-88` | `frontend/src/components/layout/WorkbenchShell.tsx:245-308` |
| 56 | javascript | `frontend/src/pages/ContractsPage.tsx:1116-1171` | `frontend/src/pages/ContractsPage.tsx:1076-1116` |
| 52 | javascript | `frontend/src/components/layout/ResizablePanel.tsx:112-163` | `frontend/src/components/layout/Sidebar.tsx:99-150` |
| 51 | typescript | `frontend/src/components/chat-page/chat-page-persistence-suite.ts:65-115` | `frontend/src/components/chat-page/useChatWorkspaceController.ts:270-320` |
| 51 | css | `frontend/src/pages/CmsPage.module.css:73-123` | `frontend/src/pages/ContentEditorPage.module.css:49-99` |
| 41 | typescript | `frontend/src/components/chat-page/useChatWorkspaceController.ts:418-458` | `frontend/src/components/chat-page/workspace-actions.ts:212-252` |
| 36 | javascript | `frontend/src/components/chat-page/MultimodalContent.tsx:216-251` | `frontend/src/components/chat-page/MultimodalContent.tsx:130-165` |
| 34 | css | `frontend/src/pages/CmsPage.module.css:228-261` | `frontend/src/pages/ContentEditorPage.module.css:96-129` |
| 32 | tsx | `frontend/src/components/layout/AgentWorkbenchLayout.tsx:63-94` | `frontend/src/components/layout/WorkbenchLayout.tsx:83-114` |
| 32 | typescript | `frontend/src/components/chat-page/chat-page-persistence-suite.ts:13-44` | `frontend/src/components/chat-page/hooks.ts:247-278` |

## Investigation notes

### 1. Chat page and context bar utilities are now single-sourced

The previous top hotspot duplicated `frontend/src/components/chat-page/utils.ts`, `frontend/src/components/context-bar/utils.ts`, and their type files. Those modules now re-export from `frontend/src/components/workspace-context/`, which preserves existing imports while giving both surfaces one implementation for workspace/context helpers, memory-row conversion, trace event handling, hydration-source extraction, and shared browse/search types.

### 2. OpenUtau generation is now single-sourced

The previous largest clone pair between `backend/src/cljs/knoxx/backend/domain/openutau/openutau.cljs` and `backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs` is removed. The `tools.cljs` namespace remains as a compatibility import path plus the runtime render shell, while the pure generation and README logic now lives in the canonical OpenUtau core namespace.

### 3. Workbench layout components overlap

`frontend/src/components/layout/ResizablePanel.tsx` duplicates much of `WorkbenchPanel` in `WorkbenchShell.tsx`; `AgentWorkbenchLayout.tsx` and `WorkbenchLayout.tsx` also share layout structure. `rg` shows `ResizablePanel.tsx` is exported but not otherwise used in `frontend/src`, which makes it a candidate for removal or deprecation once downstream imports are checked.

Recommended fix: standardize on `WorkbenchShell` / `WorkbenchPanel` / `WorkbenchBottomPanel`, then remove the unused `ResizablePanel` export if no external bridge depends on it.

### 4. Backend route aliases intentionally duplicate behavior

Several CLJS route files duplicate route bodies to preserve old and new API vocabulary, especially event/discord control aliases in `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs` and proxy/status routes in `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs`.

Recommended fix: preserve route aliases, but extract the repeated handler bodies into private helper functions so alias routes remain explicit while behavior stays single-source.

### 5. CSS duplication is concentrated in page modules

`CmsPage.module.css`, `ContentEditorPage.module.css`, and `ReviewQueuePage.module.css` repeat page-shell, card, and form styling. This duplication is high by percentage but low absolute volume.

Recommended fix: extract shared layout primitives into a shared CSS module or migrate common page surface styles to design-system tokens/classes.

## Verification

Commands run for the frontend/workspace-context refactor:

```bash
NODE_ENV=test pnpm -C frontend exec vitest run --config vitest.config.ts src/components/workspace-context/utils.test.ts src/components/chat-page/utils.test.ts
NODE_ENV=test pnpm -C frontend exec vitest run --config vitest.config.ts src/components/chat-page/VoiceInputButton.test.tsx src/components/admin-page/EventAgentsPanel.test.tsx
NODE_ENV=test pnpm -C frontend test
pnpm -C frontend typecheck
```

Results:

- targeted shared/chat utility tests: passed, 11 tests
- targeted fixed frontend tests: passed, 4 tests
- full frontend Vitest suite: passed, 46 files, 206 tests passed, 41 todo
- frontend TypeScript typecheck: passed

Commands run for the OpenUtau refactor:

```bash
pnpm -C backend exec nbb -cp src/cljs:test/cljs -e "(require '[cljs.test :refer [run-tests]] '[knoxx.backend.openutau-test]) ..."
pnpm -C backend exec clj-kondo --lint src/cljs/knoxx/backend/domain/openutau test/cljs/knoxx/backend/openutau_test.cljs
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
pnpm run scan:duplication
```

Results:

- narrow OpenUtau CLJS test via nbb: passed, 3 tests, 14 assertions
- clj-kondo on touched OpenUtau namespaces/tests: passed, 0 errors, 0 warnings
- jscpd duplication scan: passed below 3% gate, 77 clones, 1,551 duplicated lines, 1.36% duplicated lines
- `shadow-cljs compile test` and `shadow-cljs compile server` are blocked by an unrelated pre-existing syntax error in `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:669:1`: `Unexpected EOF while reading item 39 of list, starting at line 118 and column 1`.

## Priority refactor queue

1. Fix the existing `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` syntax error so full backend Shadow builds can run again.
2. Remove or deprecate unused `ResizablePanel.tsx`, then collapse `WorkbenchLayout` / `AgentWorkbenchLayout` common structure if both remain needed.
3. Extract repeated backend route alias handlers after confirming old aliases are still contractually required.
4. Factor shared CSS only after functional duplication is handled; CSS clone count is visible but lower risk.

## Re-run instructions

```bash
pnpm run scan:duplication
```

The command updates `reports/jscpd/jscpd-report.json` and exits non-zero if total duplication reaches or exceeds 3%.
