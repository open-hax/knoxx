# Knoxx Backend Lint — Function Length Extractions

Date: 2026-05-27
Status: in_progress
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Bring functions over the configured length budget back under lint limits by extracting readable, named helpers that preserve behavior and architecture boundaries.

## Problem

The hard-error first pass left 33 function-length errors at the `>=60` line threshold and 134 function-length warnings at the `>=30` line threshold. Long route registration functions and large orchestration functions make symbol drift, async cleanup, and coverage characterization harder.

After the first twenty extraction slices, lint reports 12 function-length errors and 1773 total warnings.

## Goals

1. Fix all function-length errors at or above 60 lines.
2. Prefer pure helper extraction for decision logic and shape transformations.
3. Keep effectful route, DB, network, and file-system work in `infra.*` or named extern adapters.
4. Avoid new `utils` namespaces.
5. Leave warnings under 60 lines for the final warning task unless they are touched naturally.

## Non-goals

1. Solving every 30-line warning in the first pass.
2. Changing route behavior or response envelopes.
3. Moving domain policy into `infra.*` while extracting.
4. Suppressing the length checker.

## Initial error set

Representative functions above the error threshold include:

- `start-http!` in `bootstrap.cljs`
- `eval-list-call` in `domain/agent/agent_templates.cljs`
- `gw-start-voice-listener` and `createDiscordGatewayManager` in `domain/discord/gateway.cljs`
- `make-execute-fn` in `domain/session_mycology.cljs`
- `resume-recovered-session!` in `infra/agent/recovery.cljs`
- `normalize-agent-spec` in `infra/agent/runner.cljs`
- `create-initial-run!`, `finalize-turn-success!`, and `send-agent-turn!` in `infra/agent/turn.cljs`
- `cfg` in `infra/config.cljs`
- `ensure-schema!` in `infra/db/policy/schema.cljs`
- `start-document-ingestion!` and `priority-ingest-workspace-files!` in `infra/document_state.cljs`
- `run-eta-mu-session-ingest` in `infra/eta_mu_session_ingester.cljs`
- `index-run-memory-legacy!` in `infra/openplanner/memory.cljs`
- route registration functions in `infra/routes/{admin,app,auth,memory,models,tools/proxy,translation,users/admin,voice,workspace_media}.cljs`
- `run->events` in `infra/stores/openplanner_session_store.cljs`
- `start-session-title-backfill!` in `infra/stores/session_titles.cljs`
- `normalize-agent-spec` in `shape/app_shapes.cljs`
- `make-invoke-mcp-post!` in `test/cljs/knoxx/backend/mcp_http_test.cljs`

## Progress — 2026-05-27

Completed the thirteenth extraction slice:

- Extracted `finalize-run-record!`, `extract-turn-answer-and-reasoning`, `build-turn-completed-response`, `resolve-turn-model`, `resolve-turn-thinking-level`, `emit-hydration-event!`, and `process-hydration-results-and-start-turn!` from `infra/agent/turn.cljs`.
- `finalize-turn-success!` is now under 60 lines (down from 95).
- `send-agent-turn!` is now under 60 lines (down from 97).
- `create-initial-run!` remains at 56 lines (extracted earlier).
- Full lint improved from 21 to 19 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/agent/turn.cljs
# 0 errors

cd backend && pnpm run lint
# errors: 19, warnings: 1769

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the twelfth extraction slice:

- Extracted `build-initial-run` from `infra/agent/turn.cljs::create-initial-run!` to separate the pure run-map construction from the side-effectful persistence and event emission.
- `create-initial-run!` is now 56 lines (down from 83), under the error threshold.
- Full lint improved from 22 to 21 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/agent/turn.cljs
# 2 remaining errors (finalize-turn-success!, send-agent-turn!), warnings only

cd backend && pnpm run lint
# errors: 21, warnings: 1762

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the eleventh extraction slice:

- Split `infra/stores/session_titles.cljs::start-session-title-backfill!` into named helpers: `session-ids-from-response`, `init-backfill-state!`, `complete-backfill!`, `record-backfill-error!`, and `backfill-one-session!`.
- `start-session-title-backfill!` is now 36 lines (down from 73), under the error threshold.
- Full lint improved from 23 to 22 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/stores/session_titles.cljs
# 0 errors, 34 warnings

cd backend && pnpm run lint
# errors: 22, warnings: 1760

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the tenth extraction slice:

- Extracted `session-status-running-response` from `infra/routes/app.cljs::handle-session-status` to pull the 14-field response map out of the nested `.then` chain.
- `handle-session-status` is now 50 lines (down from 63), under the error threshold.
- Full lint improved from 24 to 23 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/routes/app.cljs
# 1 error (register-routes!, separate function), warnings only

cd backend && pnpm run lint
# errors: 23, warnings: 1760

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the ninth extraction slice:

- Extracted `spec-value` private helper from `infra/openplanner/memory.cljs::run-scope-extra` to collapse repetitive keyword-alternative string normalization.
- `run-scope-extra` is now 34 lines (down from 65), under the error threshold.
- Full lint improved from 25 to 24 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/openplanner/memory.cljs
# 1 error (index-run-memory-legacy!), 15 warnings

cd backend && pnpm run lint
# errors: 24, warnings: 1759

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the eighth extraction slice:

- Extracted a `spec-value` helper from `infra/agent/runner.cljs::normalize-agent-spec` to collapse repetitive keyword-alternative string normalization.
- `normalize-agent-spec` is now 50 lines (down from 136), under the error threshold.
- The helper is private and scoped to the runner namespace; it mirrors the same pattern used in `shape/app_shapes.cljs`.
- Full lint improved from 26 to 25 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/agent/runner.cljs
# 0 errors, 5 warnings (existing)

cd backend && pnpm run lint
# errors: 25, warnings: 1758

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the seventh extraction slice:

- Extracted `spec-value` and `spec-value-raw` private helpers from `shape/app_shapes.cljs::normalize-agent-spec` to collapse the repetitive keyword-alternative string normalization pattern.
- `normalize-agent-spec` is now 34 lines (down from 63); the helpers are reusable across other normalization functions in the same file.
- Full lint improved from 27 to 26 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/shape/app_shapes.cljs
# 0 errors, 4 warnings (pre-existing)

cd backend && pnpm run lint
# errors: 26, warnings: 1757

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the sixth extraction slice:

- Split `infra/stores/openplanner_session_store.cljs::run->events` into named helpers: `normalize-content-part-for-event`, `user-event-extra`, and `run-event-list`.
- `run->events` is now 19 lines; `run-event-list` is 30 lines (under the error threshold).
- Behavioral note: the event `:id` format is now derived as `(str run_id ":" kind ":" role)`, which produces the same id strings as the original hardcoded patterns for all event kinds (`knoxx.message`, `knoxx.run`, `knoxx.reasoning`, `knoxx.error`, `knoxx.tool_receipt`) and roles (`user`, `system`, `assistant`).
- Full lint improved from 28 to 27 errors; one new warning appeared (`run-event-list` at 30 lines); remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs
# 0 errors, 1 warning (run-event-list spans 30 lines)

cd backend && pnpm run lint
# errors: 27, warnings: 1756

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the fifth extraction slice:

- Split `infra/config.cljs::cfg` into thematic config subsection helpers: `base-server-config`, `workspace-config`, `provider-config`, `integration-config`, `voice-config`, `agent-config`, `sandbox-config`, and `mcp-config`.
- `cfg` now merges the subsection maps; all keys are preserved and ordering does not matter for a flat config map.
- Full lint improved from 29 to 28 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint src/cljs/knoxx/backend/infra/config.cljs
# 0 errors, 0 warnings

cd backend && pnpm run lint
# errors: 28, warnings: 1755

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the fourth extraction slice:

- Split `bootstrap.cljs::start-http!` startup orchestration into named helpers for request debug hooks, WS route registration, optional session hook installation, HTTP route registration, Redis/session persistence startup, and post-listen app registration.
- Reconciled the tooling authorization helper so both selected custom agent contract precedence and `knoxx_default` non-admin policy clamping tests pass.
- Full lint improved from 30 to 29 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint \
  src/cljs/knoxx/backend/bootstrap.cljs \
  src/cljs/knoxx/backend/infra/tooling.cljs \
  test/cljs/knoxx/backend/tooling_test.cljs
# 0 errors; existing warnings only

cd backend && pnpm run lint
# errors: 29, warnings: 1755

pnpm -C backend exec shadow-cljs compile test
# 446 tests / 1311 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the third extraction slice:

- Split `infra/agent/tools.cljs::tool-args->markdown-preview` into named tool-name, argument coercion, argument lookup, bash-preview, and read-preview helpers.
- Added a bash preview characterization test through the public `tool-call-input-preview` seam.
- Full test compile initially surfaced a current `tooling/allowed-tool-id-set` regression where selected agent contract tools were intersected with a narrower auth policy; restored contract precedence and removed the unused `clojure.set` require.
- Full lint improved from 31 to 30 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint \
  src/cljs/knoxx/backend/infra/agent/tools.cljs \
  src/cljs/knoxx/backend/infra/tooling.cljs \
  test/cljs/knoxx/backend/agent_turns_test.cljs \
  test/cljs/knoxx/backend/tooling_test.cljs
# 0 errors; existing warnings only

cd backend && pnpm run lint
# errors: 30, warnings: 1758

pnpm -C backend exec shadow-cljs compile test
# 445 tests / 1306 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Completed the second extraction slice:

- Split `domain/agent/agent_templates.cljs::eval-list-call` logical `and`/`or` branches into named helpers.
- Added a template-runtime characterization test for `and`/`or`/`not` composition through JSON-round-tripped vector forms.
- Full lint improved from 32 to 31 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint \
  src/cljs/knoxx/backend/domain/agent/agent_templates.cljs \
  test/cljs/knoxx/backend/agent_templates_test.cljs
# 0 errors; existing warnings only

cd backend && pnpm run lint
# errors: 31, warnings: 1757

pnpm -C backend exec shadow-cljs compile test
# 441 tests / 1294 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

Global `git diff --check` is currently blocked by unrelated existing dirt in `contracts/policies/basic_user_chat_guardrails.edn`; path-scoped diff check for this slice passed.

Completed the first route extraction slice:

- Split `infra/routes/memory.cljs::memory-sessions-route!` from a 109-line `defroute` body into named route workflow helpers.
- Moved pure list query/page/response shape helpers into `knoxx.backend.shape.memory-sessions` so `memory.cljs` stays under the 800-line file-length error threshold.
- Added `memory-routes-page-state-test` to characterize page window and response metadata shape.
- Full lint improved from 33 to 32 errors; remaining errors are still function-length only.

Verification:

```bash
cd backend && pnpm exec clj-kondo --lint \
  src/cljs/knoxx/backend/infra/routes/memory.cljs \
  src/cljs/knoxx/backend/shape/memory_sessions.cljs \
  test/cljs/knoxx/backend/memory_routes_test.cljs \
  test/cljs/knoxx/backend/memory_routes_page_state_test.cljs
# 0 errors; existing warnings only

cd backend && pnpm run lint
# errors: 32, warnings: 1752

pnpm -C backend exec shadow-cljs compile test
# 440 tests / 1293 assertions / 0 failures / 0 errors / 0 warnings

pnpm -C backend exec shadow-cljs compile server
# 0 warnings
```

## Extraction rules

1. Extract intent-revealing helpers close to the owning namespace first.
2. Move reusable shape-only logic to `shape.*` and invariant checks to `law.*` only when reuse is real.
3. Do not create junk-drawer namespaces.
4. Keep helper functions small enough that the next lint run does not create new length warnings unnecessarily.
5. When extracting async code, prefer named `^:async` workflow functions with `await` instead of preserving raw chains.

## Verification

For each touched namespace:

```bash
pnpm -C backend exec clj-kondo --lint <touched-file-1> <touched-file-2>
```

After the batch:

```bash
pnpm -C backend lint
pnpm -C backend exec shadow-cljs compile test
```

Run server compile too if production startup or route registration behavior changes:

```bash
pnpm -C backend exec shadow-cljs compile server
```

## Definition of done

- No `spans ... (error >=60)` diagnostics remain.
- Extracted helpers have names that explain their role.
- No broad lint suppressions are added.
- Relevant compile/test gates pass or any unrelated blocker is recorded exactly.
