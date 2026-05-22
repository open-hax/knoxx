# Knoxx Backend Lint — Function Length Extractions

Date: 2026-05-21
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Bring functions over the configured length budget back under lint limits by extracting readable, named helpers that preserve behavior and architecture boundaries.

## Problem

The lint run reports 32 function-length errors at the `>=60` line threshold and 136 function-length warnings at the `>=30` line threshold. Long route registration functions and large orchestration functions make symbol drift and promise-chain cleanup harder.

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

## Extraction rules

1. Extract intent-revealing helpers close to the owning namespace first.
2. Move reusable shape-only logic to `shape.*` and invariant checks to `law.*` only when reuse is real.
3. Do not create junk-drawer namespaces.
4. Keep helper functions small enough that the next lint run does not create new length warnings unnecessarily.
5. When extracting async code, prefer `p/let` or `^:async` + `await` instead of preserving raw chains.

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
