---
uuid: "knoxx-frontend-app-bridge-retirement"
title: "Frontend: migrate bridge-exposed TS surfaces — retire vite.app-bridge.config.ts"
status: accepted
priority: P2
labels: ["tasks", "frontend", "helix", "vite-retirement", "has-parent"]
created_at: "2026-06-11T00:00:00Z"
points: 8
category: tasks
---
# Frontend: migrate bridge-exposed TS surfaces — retire vite.app-bridge.config.ts

> Parent epic: `knoxx-frontend-helix-migration-vite-retirement`

## Background

`vite.app-bridge.config.ts` builds `dist/bridge/knoxx-app-bridge.es.js` from
`src/bridge/app.ts` — the stable ESM surface over the legacy TS app that
shadow-cljs consumes via `:js-options :resolve {:target :file}` (router libs
kept external so CLJS and TS share one react-router context). It exists only
because legacy TS pages/components are still mounted from CLJS during the
migration.

This is the long-tail card: ~129 .tsx + ~76 .ts remain vs 22 .cljs.

## Approach

- Work the established patterns from the epic: loader shim for views, pure
  logic extracted to uxx-free namespaces with cljs.test, util + consumers
  migrated together, TS deleted as each slice lands.
- Inventory `src/bridge/app.ts` exports first; each export is a migration
  slice. Track per-slice progress as comments on this card (split child cards
  if a slice is >3 points — e.g. CodeMirror editor, Puck CMS, chart pages,
  webgl graph view are likely their own cards).
- Heavy npm-widget pages (codemirror, @measured/puck, chart.js,
  webgl-graph-view) stay as thin Helix interop wrappers — migrating the
  mounting/state code, not rewriting the widgets.

## Progress

- 2026-06-11 (TDD round 1): bridge shrunk from 20 to 16 exports.
  - `CollapsedPanelTab` → Helix
    (`knoxx.frontend.components.layout.collapsed-panel-tab`, tests written
    FIRST in `collapsed_panel_tab_test.cljs`); `workbench.cljs` consumes it
    natively; export deleted. TS copy stays for ChatPage/CmsPage/
    BroadcastStudioPage/WorkbenchShell.tsx until those migrate.
  - Layout block (`WorkbenchShell/Panel/Main/BottomPanel/AgentWorkbenchLayout`)
    and `ContextBar` exports deleted — verified dead from CLJS (pages use the
    native `knoxx.frontend.components.layout.workbench` ns).
  - Bonus same round (tests-first): `lib/storage.ts` → `knoxx.frontend.lib.storage`
    (quota-eviction contract incl. never-evict-target; mock localStorage via
    js/globalThis injection) — workbench.cljs's private duplicate helpers
    deleted in favor of the lib ns. `lib/edn.ts` contract →
    `knoxx.frontend.lib.edn` (cljs.reader; tests cover real view-contract
    shape incl. namespaced keywords the TS parser mangles). TS copies stay
    until WorkbenchShell.tsx / VisualCmsEditorPage migrate.
  - Gates: test:cljs 50 tests / 207 assertions green 0 warnings; vitest 217
    green; tsc clean; app-bridge rebuilt; shadow app compile 0 warnings.

- 2026-06-11 (TDD round 2): bridge 16 → 15 exports; **first full native-routed
  page migration with interaction tests**.
  - Deleted dead code: `src/lib/multimodal-utils.ts` (zero consumers),
    `src/App.tsx` + `src/main.tsx` (index.html loads only the shadow
    entrypoint; app.cljs owns routing — they were never executed).
  - **MailPage → Helix, no loader shim needed**: routed natively in app.cljs
    (`knoxx.frontend.pages.mail.view/mail-page`). Namespaces: `mail.logic`
    (pure helpers + mailbox normalizers from lib/api/runtime.ts),
    `mail.card` (router-free, :on-navigate prop), `mail.api` (js/fetch +
    x-knoxx auth headers via lib.storage), `mail.page` (bridge-free body),
    `mail.view` (useAuth/useNavigate wrapper). TSX + its vitest test deleted;
    `MailPage` export removed from app.ts.
  - **Interaction testing in node PROVEN** (closes the harness card's open
    question): `page_interaction_test.cljs` uses jsdom globals +
    @testing-library/react fireEvent/waitFor against the bridge-free page
    body, api ns mocked via set!. Ports MailPage.test.tsx's contract (filter
    refetch + ack/reload). GOTCHA: RTL waitFor retries while the callback
    THROWS — cljs.test `is` records instead of throwing, so wrap predicates
    and throw until true.
  - ui-backend-surface-matrix.ts repointed (owner cljs, implementedBy → cljs
    test). Helix `$` gotcha: non-literal props map is treated as a CHILD —
    use `{& props}`.
  - Deferred: promesa adoption for frontend (classpath change = shadow server
    restart; 65 pre-existing raw-chain warnings in frontend src).
  - Gates: test:cljs 63/280 green; vitest 215 green (46 files — MailPage's 2
    tests ported to cljs); tsc clean; app compile 0 warnings; app-bridge
    rebuilt.

- 2026-06-11 (TDD round 3): bridge 15 → 14 exports. **GardensPage → native
  Helix** (logic/api/view + shared `components/ui.cljs` button/card/input
  primitives standing in for uxx until uxx-helix-native). Interaction tests
  port GardensPage.test.tsx (create flow + delete-with-inline-confirmation);
  payload contract covered by pure build-save-request tests. TSX + vitest
  test deleted; matrix repointed.
  - **Two real bugs found by the harness this round:**
    1. React event feature-detection happens at react-dom MODULE LOAD — jsdom
       globals created in a test ns body are too late, and text-input
       onChange silently never fires (selects/clicks still work). Fix:
       `:prepend-js` on the :test build creates jsdom globals before any
       bundle code runs. Per-ns JSDOM setup blocks removed.
    2. `($ :<> ...)` is NOT a fragment in helix 0.2.2 — it createElements a
       literal "<>" tag → InvalidCharacterError on client render (static
       markup masks it). Fixed in gardens AND in production
       `source_doc/view.cljs` (zoom gallery Prev/Next would have crashed in
       the browser). Correct form: `helix.core/<>` macro.
  - clj-kondo function-length ERRORS (≥60 lines) on garden-form/gardens-page
    fixed by splitting (form-fields, gardens-body, run-load!/save!/delete!).
  - Gates: cljs 80/336 green; vitest 213/45 files; tsc clean; app compile 0
    warnings; bridge rebuilt (2715 kB).

- 2026-06-11 (TDD round 4): bridge 14 → 13 exports. **TranslationReviewPage +
  TranslationModelSection → native Helix** (both TSX deleted; model section
  had the page's only prior test coverage — its contract ported; the review
  page flows are NET-NEW interaction tests: list→detail→segment label submit
  with trimmed/omitted payload fields, document-level Approve All, pipeline
  config PATCH).
  - NEW shared `lib/api.cljs` — CLJS port of lib/api/core.ts `request`
    (x-knoxx auth headers from localStorage, credentials include, JSON
    bodies, error-text propagation). Tested directly by mocking GLOBAL fetch
    via set! js/globalThis.fetch — first real api-layer test coverage.
    mail/api refactored onto it. Future page migrations should use it.
  - 99 cljs tests / 395 assertions green ON FIRST RUN — the round-3 harness
    fixes (prepend-js jsdom) hold.
  - Gates: vitest 212/44; tsc clean; app compile 0 warnings; bridge 2685 kB
    (was 2753 at round start ~70 kB lighter).

- 2026-06-11 (TDD round 5): bridge 13 → 12 exports. **AgentAuditSessionList →
  native Helix** (`components/agent_audit/{logic,api,session_list}.cljs` +
  `ui/badge` primitive). The ~250-line pure merge/scoring/matching core got
  a full logic suite (contract matching, active-run→session, active-first
  sort, page dedup, status mapping, search text); both component tests
  ported (contract-scoped load + search + resume; 20-row infinite scroll
  with Object.defineProperties scroll geometry). agents.cljs consumes the
  native component with a JS controller object passed straight through.
  VisualCmsEditorPage scouted and DEFERRED: its VisualEditor wraps
  @measured/puck + @open-hax/garden-publication-components — npm resolution
  risk, treat as its own slice. Gates: cljs 109/442 first-run green; vitest
  207/43; tsc clean; app 0 warnings; bridge 2671 kB.

- 2026-06-11 (TDD round 6): bridge 12 → 11 exports. **Auth subtree → Helix**
  (`auth/{api,context,login,signup,boundary}.cljs`): AuthBoundary, useAuth,
  LoginPage, SignupPage TSX all deleted (4 files). KEY ARCHITECTURE: the
  React context INSTANCE stays shared — the bridge now exports
  `AuthContextInstance` (auth-context-instance.ts also hosts the
  AuthContext type now); app.cljs wires it via
  `auth-ctx/set-context-instance!` at load, so TS pages' useAuth reads the
  value the CLJS boundary provides. CLJS auth context value is a JS object
  for cross-language parity. node tests run on a lazily created local
  context (no bridge dep in the test path).
  - AuthContext.test.tsx ported: 401→login surface (+credentials include),
    invite redemption→context refetch→protected app, plus a new
    authenticated-consumer use-auth test.
  - All CLJS app/useAuth call sites swapped to auth-ctx/use-auth (app,
    mail, agents, events). GOTCHA: `^js` return hint needed on use-auth or
    every consumer gets infer warnings.
  - Gates: cljs 112/450 green; vitest 205/42; tsc clean; app 0 warnings.

- 2026-06-11 (TDD round 7): **SettingsPage → Helix via loader shim** (first
  shim of this loop — OpsRoot is TS-routed so the shim pattern applies:
  `pages/settings/{api,view}.cljs`, exposed at
  window.knoxx.frontend.pages.settings.view.settings_page, required from
  core.cljs; SettingsPage.tsx is now the standard loud-failure shim + 3-test
  vitest contract). Interaction tests cover config card + 4 status-row pings
  (OK/Unavailable) + config-failure tolerance.
  - Scouting notes: ContractsPage is entangled with the chat-workspace
    subtree (ChatWorkspacePane/useChatWorkspaceController) + EdnEditor
    (CodeMirror) + 67 uxx token usages — the chat workspace is the keystone
    and needs its own decomposition. VectorsPage blocked on GraphExplorer
    (webgl). SidebarOpsStatus needs the lib/ws port (small, good future
    slice). OpsRoot itself stays TS until its children are all CLJS.
  - NOTE: existing shim exports (source_doc, settings) rely on namespace
    object paths that :advanced release may munge — settings-page carries
    ^:export; source_doc's does NOT (pre-existing risk, verify on next
    release build).
  - Gates: cljs 114/460 green; vitest 208/43 (+3 shim tests); tsc clean;
    app 0 warnings.

- 2026-06-11 (TDD rounds 8–9): chat-workspace decomposition card created
  (`knoxx-frontend-chat-workspace-decomposition`). **DocumentsPage (Data
  Lakes) → Helix via loader shim**: `pages/documents/{logic,api,view,page}`
  — logic landed tests-first in round 8 (rate/ETA math, sample windowing,
  restart decisions); round 9 added the 13 sessionRequest API ports (incl.
  FormData upload + x-knoxx-session-id from sessionStorage), the split
  presentational view, the container (2s progress polling, lake CRUD with
  js/confirm, restart force-fresh flow), 5 interaction tests (load,
  select→ingest payload, lake creation payload, restart-no-active-run
  message, confirmed delete), the shim + 3-test vitest contract, and
  DocumentsPageView.tsx deletion. Test gotchas recorded: poll-driven
  assertions need waitFor {:timeout 4000}; target row checkboxes via
  (.closest el "tr") not getAllByRole index. Gates: cljs 126/500; vitest
  211/44; tsc clean; app 0 warnings; lint 0 errors.

## Definition of done

- `src/bridge/app.ts` has no exports; CLJS has no
  `@open-hax/knoxx-app-bridge` requires.
- `vite.app-bridge.config.ts` deleted; APP_BRIDGE watcher and
  `build:app-bridge` removed from scripts.
- App builds with 0 warnings; routes render in browser; ava e2e green.

## Proposed bounded slice: native WebSocket transport and its current consumers (3 points)

This is a proposed allocation within this existing 8-point bridge-retirement card. It is not a new card, a points/frontmatter change, completed migration progress, review approval or readiness. The current canonical card remains `accepted`. Before implementation, publish and review this concrete scoped body through canonical planning, settle its findings, and move this same card lawfully through `breakdown`, `ready`, `todo` and `in_progress` using Rheos. The last transition requires actual WIP admission. Historical progress on other slices does not approve this one.

### Outcome and ownership

The active chat and Ops TypeScript consumers use the Knoxx-owned native CLJS WebSocket transport. Retire `frontend/src/lib/ws.ts` only after its transport behavior, public API, native callers and regression assertions are preserved and pass. The regenerated canonical migration manifest must show the actual single-file retirement and a legacy surface reduction from 272 to 271, with no new legacy source path, application-bridge export or route regression.

Knoxx owns all source, tests and fixtures for this slice. It does not alter Services, providers, credentials, deployment admission or another repository's package policy. It does not migrate the entire chat hook, Ops component, API library or app bridge.

### Scope and implementation boundary

1. Keep `knoxx.frontend.lib.ws/connect-stream` as the native map-returning three-argument boundary used by the native Ops sidebar and its existing `set!` mock. Place token shape/validation and pure decisions in named `law/ws.cljc` and `domain/ws.cljc` namespaces; the existing `lib/ws.cljs` owns browser WebSocket/URL/timer/JSON interop. Support a configured base only at the effectful adapter, retaining the existing three-argument same-origin behavior.
2. Add a named `^:export` JS adapter in that existing native WS namespace and explicitly require it from `frontend/src/cljs/knoxx/frontend/core.cljs`. Its public return remains callable as React cleanup and exposes the exact `disconnect` and `setConversationId` methods. It accepts camelCase handlers and emits camelCase token metadata and string statuses. Use explicit stable property names under production optimization.
3. Reuse the existing API facade boundary (`frontend/src/lib/api/core.ts`, re-exported by `frontend/src/lib/api.ts`) for the stream types and a small typed synchronous, late-bound `connectStream` delegate. It passes the actual `API_BASE` to the native adapter; it owns no token parsing, URL construction, reconnect policy or fallback transport. Resolve the exported namespace when called, after the app has initialized, rather than at Vite/module load. Missing or malformed export is a clear integration error. This adapts the established exported-namespace loader pattern to an effectful client; it is not an async shim for synchronous pure utilities.
4. Repoint the two production consumers, `chat-runtime-effects.ts` and `SidebarOpsStatus.tsx`, and the two TS test consumers. Preserve all chat persistence, RAF ordering, correction, terminal hydration/Undo and run-association assertions. Move the exact transport-decoding assertions from the final WS block in `hooks.test.ts` to the real native `ws_test.cljs` before removing their TS copies; preserve every positive case, malformed override, UTF-16 offset and empty/repeated/whitespace assertion. The other `hooks.test.ts` blocks remain. Existing controller tests retain their intentional mocked stream boundary and receive only import/mock alignment.
5. Native tests call the actual native adapter; TS facade tests in an existing TS suite verify late delegation, configured-base propagation, public types and missing-export failure. A mock used to test delegation supplies no native transport or UI proof. Do not add a standalone build target, generic bridge/loader framework, resolver alias, source relocation, JS rename or fallback to the retired TS transport.

### Acceptance criteria

- Ordinary deltas remain literal, including repeats and whitespace. Valid replacement packets require string token, kind `assistant_message` or `reasoning`, `operation: replace`, and a nonnegative safe-integer offset no greater than the token's UTF-16 length. Empty replacements and surrogate-pair offsets remain valid. Unknown operations, malformed token/kind/offset and an offset without its operation emit the existing malformed-token diagnostic and never reach `onToken`.
- `run_id` and ordinary `kind` retain the existing TS string-only normalization; malformed optional metadata does not become authoritative run identity. The public API emits `runId`, `kind`, `operation` and `offset` with the existing meaning. The adapter preserves ordinary token coercion and the distinction between absent correction metadata and malformed correction metadata.
- Both the callable cleanup and `.disconnect()` dispose the owned connection. `.setConversationId()` retains its name, null clearing, same-ID no-op, open-socket control packet, closed-socket reconnection and rebinding on the next reconnect. Retain native `{:disconnect f :set-conversation-id f}` callers and mocks.
- Empty `API_BASE` derives ws/wss from the browser origin. A configured HTTP/HTTPS base retains its host/port, maps its scheme, sets `/ws/stream`, and replaces its query with URLSearchParams-encoded session/conversation IDs. Do not silently turn a configured base into same-origin routing.
- Preserve existing reconnect delay (one second, doubling to the 30-second cap and reset after open), owned-timer cancellation, disconnect and status/error semantics. Characterize disposal during connecting, after close before retry, repeated cleanup, stale socket close and conversation change during a pending retry. Both current implementations still deliver some callbacks after disposal and lack a current-socket generation guard; eliminating those inherited behaviors is outside parity. Repair only a bounded migration/parity defect established by actual causal RED. General stale-generation, double-retry or lifecycle hardening requires re-estimation under the stop boundary.
- Stats, formatted console, event and lounge packets retain their payloads and routes; malformed JSON remains diagnosed. The native Ops stats path keeps its expected JS payload shape. These assertions do not imply durable backend replay or provider behavior.
- The actual production compiled namespace export is callable, the app bridge resolves every remaining consumer, native and TS suites pass with zero warnings/errors, and a browser fixture demonstrates the compiled client/consumer path. Canonical migration:write/check verifies 272 to 271 and every existing non-growth/source-containment/route/native-floor guard without an infrastructure exemption.

### Causal RED and verification

RED begins only after canonical planning/readiness. Add failing laws/native tests first while retaining `ws.ts` and all original assertions. Against the actual predecessor, replacement metadata is discarded and malformed correction packets reach the token callback; those wrong results are causal RED neighbors. Keep tests on the existing fixed-arity native function where possible. A public export assertion may demonstrate the missing JS boundary, but no typo, invented compile arity or loader/config failure counts as transport RED. Do not delete or rewire the legacy runtime before recording this RED and its source identity.

Run the registered `pnpm -C frontend run test:cljs`; the existing guarded node-test runner checks real summaries rather than compiler exit alone. Run the retained TS suites with `pnpm -C frontend exec vitest run --config vitest.config.ts src/components/chat-page/hooks.test.ts src/components/chat-page/useChatWorkspaceController.test.ts` and then the registered full `pnpm -C frontend run test:coverage`. Facade assertions belong in an existing suite and must be included in that focused command if placed elsewhere.

After GREEN, run `pnpm -C frontend run typecheck`, the configured changed-CLJS lint with the actual base, and `pnpm -C frontend run build` (both governed Vite bridges, then optimized Shadow app). Verify the actual browser export and active chat/Ops use after the optimized build. Retire only `frontend/src/lib/ws.ts`; confirm no source/test import or mock still targets it. Run `pnpm -C frontend run migration:write`, then `KNOXX_MIGRATION_BASE_SHA=3409977bca4ba35e09967f9a99d50867a679a73c KNOXX_MIGRATION_INFRASTRUCTURE=false pnpm -C frontend run migration:check`. Rebind that base if the actual native PR base changes. Keep the registered CI job and all ratchets unchanged.

For the human artifact, reuse `frontend/e2e/knoxx_e2e_support.cjs`, its existing Puppeteer browser/server ownership and teardown; retain every AVA case. Its server destroys WebSocket upgrades, so its existing audit smoke tests supply no stream proof. The observed installed Playwright 1.58.2 runtime supports `routeWebSocket`, `send`, `onMessage` and `onClose`; the artifact may resolve and verify that existing pinned runtime, attach by CDP only to its own browser, and register a mock route before navigation/socket creation. It must not call `connectToServer`. Drive the real compiled native adapter and active chat/Ops consumers with fixture packets, show literal deltas then correction and an Ops stats update, capture screenshots, and observe close/rebinding/cleanup. No replacement decoder or backend/provider/WebSocket protocol service supplies the proof.

The thin `scripts/verify-frontend-ws-migration.sh` and `docs/verification/frontend-ws-migration.md` must name actual runtime resolution/version/hash, CDP attachment, checkout/head/diff/build and fixture/network/provider preconditions. Fail if the verified runtime is absent; a global agent-browser installation is not an implied frontend/e2e dependency or hosted-CI capability. The registered `pnpm -C frontend run test:e2e` retains and qualifies its existing cases; the explicitly scoped human packet artifact separately qualifies WS behavior. If reusing the existing runtime/owned browser requires a new harness, dependency or service, stop and re-estimate before implementation. Missing prerequisites or failed cases remain visible.

### Estimate and stop boundary

Allocate 1 point to native packet/lifecycle parity and causal tests, 1 point to the named JS adapter/typed facade plus current consumers/assertion transfer, and 1 point to manifest/build/browser qualification and human documentation. The parent remains 8 points and unfinished. This 3-point estimate is a proposed bounded judgment, not established effort or board metadata. If preserving assertions, optimized export/loading or browser proof requires a new build framework, provider service, broader hook/route migration or more than this scope, stop and re-estimate/decompose under the parent card's existing greater-than-3-point rule before proceeding.
