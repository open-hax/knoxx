# Backend CLJS lint issue report — 2026-05-21
## Command
```bash
pnpm -C backend lint
```
- Exit code: `3`
- Source log: `/tmp/pi-bash-2c1d4c91531f7be9.log`
- Parsed diagnostics: `3517` across `159` files
- clj-kondo summary: errors `180`, warnings `3335`, info `2`
## Executive summary
The lint failure is not one bug; it is a backlog spike from stricter local rules plus several real correctness blockers. The highest-value first fixes are syntax/macro-scope breakage and unresolved symbols, because those can cause large false-positive cascades. After that, reduce function length errors. Promise-chain and unused-binding warnings dominate volume but should be batched by namespace once hard errors are gone.
## Severity breakdown
| Severity | Count |
|---|---:|
| error | 180 |
| warning | 3335 |
| info | 2 |

## Top diagnostic categories
| Category | All diagnostics | Errors only |
|---|---:|---:|
| Unused binding. | 1605 | 0 |
| Promise chains: replace `.then`/`.catch` with `p/let` or `^:async`/`await`. | 1474 | 0 |
| Length budget: function exceeds configured clj-kondo line threshold. | 167 | 34 |
| Unresolved symbol: missing binding/require/macro hook or syntax broke lexical scope. | 127 | 127 |
| Unused private vars. | 43 | 0 |
| Unused requires/refers. | 43 | 0 |
| Tests reaching into private vars. | 15 | 15 |
| Redundant expression/call. | 8 | 0 |
| Unresolved var: referenced public var not known to clj-kondo. | 6 | 0 |
| Unresolved namespace: missing `:require` alias. | 5 | 0 |
| Missing else branch. | 4 | 0 |
| Incomplete protocol mocks/reify records in tests. | 4 | 0 |
| redundant do | 2 | 0 |
| Arity mismatch. | 2 | 2 |
| Syntax/paren balance error. | 2 | 2 |

## Hotspot files by diagnostic count
| Count | File | Notes |
|---:|---|---|
| 739 | `src/cljs/knoxx/backend/infra/routes/app.cljs` | 8 errors; large route namespace; also arity/unresolved issues |
| 387 | `src/cljs/knoxx/backend/infra/routes/tools.cljs` | 21 errors; defroute expansion unresolved cascade |
| 230 | `src/cljs/knoxx/backend/infra/routes/studio.cljs` | 14 errors; defroute expansion unresolved cascade likely |
| 200 | `src/cljs/knoxx/backend/infra/routes/mcp.cljs` | 19 errors; defroute expansion unresolved cascade likely |
| 193 | `src/cljs/knoxx/backend/infra/routes/documents.cljs` | 14 errors; defroute expansion unresolved cascade likely |
| 148 | `src/cljs/knoxx/backend/infra/routes/memory.cljs` | 12 errors; defroute expansion unresolved cascade likely |
| 126 | `src/cljs/knoxx/backend/infra/db/policy.cljs` | 1 errors; many unused bindings |
| 68 | `src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs` | 13 errors |
| 65 | `src/cljs/knoxx/backend/infra/routes/actors.cljs` | 14 errors |
| 54 | `src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs` |  |
| 52 | `src/cljs/knoxx/backend/domain/discord/tools.cljs` |  |
| 47 | `src/cljs/knoxx/backend/infra/auth/session.cljs` |  |
| 45 | `src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 2 errors |
| 45 | `src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 1 errors |
| 42 | `test/cljs/knoxx/backend/node/fs_test.cljs` |  |
| 39 | `src/cljs/knoxx/backend/infra/routes/translation.cljs` | 11 errors |
| 36 | `src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs` | 1 errors |
| 36 | `src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 1 errors |
| 33 | `src/cljs/knoxx/backend/domain/discord/voice_tools.cljs` |  |
| 31 | `src/cljs/knoxx/backend/domain/sandbox_container.cljs` |  |

## Hard blockers to fix first
1. **Syntax break:** `src/cljs/knoxx/backend/infra/routes/multimodal.cljs` has an unclosed form around `request-parts-promise` (`line 85` opened, expected close at `line 240`). This can invalidate downstream analysis of the namespace.
2. **Macro/hook or expansion cascade:** `defroute` forms in `routes/{actors,documents,mcp,memory,studio,studio/discord_scan,tools}.cljs` report unresolved route-context names (`request`, `reply`, `ctx`, `route!`, `json-response!`, `with-request-context!`, etc.). `backend/.clj-kondo/config.edn` does register `knoxx.backend.macros/defroute`, so either the hook does not match the current macro shape or the macro call sites now use an unsupported argument pattern.
3. **Direct unresolved symbols:** examples include `prewarm-sdk-runtime!`, `basename`, `normalize-devel-path`, `ensure-bootstrap-allowlist-users!`, `openplanner-graph-export!`, `lounge-messages*`, `list-active-runs`, and `match-params`. These likely need requires, public protocol additions, renamed symbols, or typo fixes.
4. **Arity/private API test failures:** `ensure-session-id` is called with two args where one is expected; tests call private vars in `pipeline-runner`, `policy`, and `temp-memory`. Either expose supported public seams or move those assertions to behavior-level tests.
5. **Function length errors:** multiple production and test fns exceed the error threshold (`>=60` lines). Extract pure helpers first, especially from route registration fns and schema/bootstrap blocks.

## Full error inventory
| Location | Error |
|---|---|
| `src/cljs/knoxx/backend/bootstrap.cljs:81:1` | start-http! spans 67 lines (error >=60) |
| `src/cljs/knoxx/backend/domain/agent/agent_templates.cljs:255:1` | eval-list-call spans 60 lines (error >=60) |
| `src/cljs/knoxx/backend/domain/discord/gateway.cljs:655:1` | gw-start-voice-listener spans 171 lines (error >=60) |
| `src/cljs/knoxx/backend/domain/discord/gateway.cljs:832:1` | createDiscordGatewayManager spans 83 lines (error >=60) |
| `src/cljs/knoxx/backend/domain/session_mycology.cljs:217:1` | make-execute-fn spans 83 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/recovery.cljs:91:1` | resume-recovered-session! spans 93 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/runner.cljs:33:1` | normalize-agent-spec spans 93 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/tools.cljs:7:1` | tool-args->markdown-preview spans 60 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/turn.cljs:92:1` | create-initial-run! spans 76 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/turn.cljs:170:1` | finalize-turn-success! spans 95 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/agent/turn.cljs:482:1` | send-agent-turn! spans 97 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/config.cljs:42:1` | cfg spans 127 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/control_config.cljs:395:1` | normalize-event-agent-job spans 112 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/control_config.cljs:509:1` | event-agent-control-config spans 62 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/core.cljs:132:13` | Unresolved symbol: prewarm-sdk-runtime! |
| `src/cljs/knoxx/backend/infra/core_memory.cljs:65:12` | Unresolved symbol: basename |
| `src/cljs/knoxx/backend/infra/core_memory.cljs:72:15` | Unresolved symbol: normalize-devel-path |
| `src/cljs/knoxx/backend/infra/db/policy.cljs:1163:39` | Unresolved symbol: ensure-bootstrap-allowlist-users! |
| `src/cljs/knoxx/backend/infra/db/policy/schema.cljs:7:1` | ensure-schema! spans 294 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/document_state.cljs:239:1` | start-document-ingestion! spans 163 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/document_state.cljs:418:1` | priority-ingest-workspace-files! spans 88 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs:355:1` | run-eta-mu-session-ingest spans 88 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/openplanner/memory.cljs:540:1` | index-run-memory-legacy! spans 181 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/actors.cljs:68:1` | 13 unresolved symbols at same form (with-request-context!, fetch-json, route!, ctx, request, request-query-string, ensure-permission!, json-response!, … +5 more) |
| `src/cljs/knoxx/backend/infra/routes/actors.cljs:71:4` | Unresolved symbol: session-guard |
| `src/cljs/knoxx/backend/infra/routes/admin.cljs:4:1` | register-admin-routes! spans 117 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:453:62` | Unresolved symbol: config |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:509:20` | knoxx.backend.infra.agent.turn/ensure-session-id is called with 2 args but expects 1 |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:566:20` | knoxx.backend.infra.agent.turn/ensure-session-id is called with 2 args but expects 1 |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:670:1` | handle-session-status spans 63 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:735:1` | 3 unresolved symbols at same form (request, reply, ctx) |
| `src/cljs/knoxx/backend/infra/routes/app.cljs:1404:1` | register-routes! spans 188 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/auth.cljs:6:1` | register-auth-routes spans 165 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/documents.cljs:83:1` | 13 unresolved symbols at same form (with-request-context!, fetch-json, route!, ctx, request, request-query-string, ensure-permission!, json-response!, … +5 more) |
| `src/cljs/knoxx/backend/infra/routes/documents.cljs:102:1` | Unresolved symbol: openplanner-graph-export! |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:451:1` | 13 unresolved symbols at same form (with-request-context!, fetch-json, ctx, request, request-query-string, ensure-permission!, json-response!, send-fetch-response!, … +5 more) |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:473:1` | Unresolved symbol: redis-guard |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:488:1` | Unresolved symbol: browser-auth-guard |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:506:1` | 2 unresolved symbols at same form (code-ttl, token-ttl) |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:606:1` | Unresolved symbol: bearer-token-guard |
| `src/cljs/knoxx/backend/infra/routes/mcp.cljs:636:1` | Unresolved symbol: policy-db |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: with-request-context! |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: fetch-json |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: route! |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: ctx |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: request |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: request-query-string |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: send-fetch-response! |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: bearer-headers |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: clip-text |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | Unresolved symbol: reply |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:461:1` | memory-sessions-route! spans 103 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/memory.cljs:672:41` | Unresolved symbol: lounge-messages* |
| `src/cljs/knoxx/backend/infra/routes/models.cljs:164:1` | register-model-routes! spans 193 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/multimodal.cljs:85:1` | Found an opening ( with no matching ) |
| `src/cljs/knoxx/backend/infra/routes/multimodal.cljs:240:1` | Expected a ) to match ( from line 85 |
| `src/cljs/knoxx/backend/infra/routes/studio.cljs:53:1` | 14 unresolved symbols at same form (with-request-context!, fetch-json, route!, ctx, request, request-query-string, ensure-permission!, json-response!, … +6 more) |
| `src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs:426:8` | Unresolved symbol: json-response! |
| `src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs:436:1` | 12 unresolved symbols at same form (with-request-context!, fetch-json, route!, ctx, request, request-query-string, ensure-permission!, send-fetch-response!, … +4 more) |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:98:1` | 15 unresolved symbols at same form (tool-catalog, with-request-context!, fetch-json, route!, ctx, request, request-query-string, ensure-permission!, … +7 more) |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:101:4` | Unresolved symbol: optional-session-guard |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:114:4` | Unresolved symbol: session-guard |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:132:21` | Unresolved symbol: err |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:165:1` | Unresolved symbol: resolve-workspace-path |
| `src/cljs/knoxx/backend/infra/routes/tools.cljs:234:1` | 2 unresolved symbols at same form (replace-first, count-occurrences) |
| `src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs:119:1` | register-proxy-routes! spans 263 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:28:1` | register-translation-routes! spans 96 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:126:11` | Unresolved symbol: app |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:128:35` | Unresolved symbol: config |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:129:10` | Unresolved symbol: json-response! |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:130:10` | Unresolved symbol: with-request-context! |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:130:32` | Unresolved symbol: runtime |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:132:24` | Unresolved symbol: ensure-permission! |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:134:64` | Unresolved symbol: ctx-org-id |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:137:38` | Unresolved symbol: error-response! |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:185:64` | Unresolved symbol: ctx-user-id |
| `src/cljs/knoxx/backend/infra/routes/translation.cljs:186:67` | Unresolved symbol: ctx-user-email |
| `src/cljs/knoxx/backend/infra/routes/users/admin.cljs:5:1` | register-user-admin-routes! spans 185 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/voice.cljs:170:1` | register-voice-routes! spans 190 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/routes/workspace_media.cljs:191:1` | register-audio-library-routes! spans 91 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/stores/composite_session_store.cljs:43:6` | Unresolved symbol: list-active-runs |
| `src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs:14:1` | run->events spans 65 lines (error >=60) |
| `src/cljs/knoxx/backend/infra/stores/session_titles.cljs:429:1` | start-session-title-backfill! spans 73 lines (error >=60) |
| `src/cljs/knoxx/backend/shape/app_shapes.cljs:78:1` | normalize-agent-spec spans 63 lines (error >=60) |
| `src/cljs/open_hax/contracts/policy/fulfillment.cljs:45:18` | Unresolved symbol: match-params |
| `src/cljs/open_hax/contracts/policy/gate.cljs:43:18` | Unresolved symbol: match-params |
| `test/cljs/knoxx/backend/mcp_http_test.cljs:168:1` | make-invoke-mcp-post! spans 92 lines (error >=60) |
| `test/cljs/knoxx/backend/pipeline_runner_test.cljs:14:29` | #'knoxx.backend.infra.pipeline-runner/dependency-order is private |
| `test/cljs/knoxx/backend/pipeline_runner_test.cljs:18:29` | #'knoxx.backend.infra.pipeline-runner/dependency-order is private |
| `test/cljs/knoxx/backend/pipeline_runner_test.cljs:24:20` | #'knoxx.backend.infra.pipeline-runner/interpolate-value is private |
| `test/cljs/knoxx/backend/pipeline_runner_test.cljs:30:20` | #'knoxx.backend.infra.pipeline-runner/interpolate-value is private |
| `test/cljs/knoxx/backend/pipeline_runner_test.cljs:36:20` | #'knoxx.backend.infra.pipeline-runner/interpolate-map is private |
| `test/cljs/knoxx/backend/policy_actor_test.cljs:7:27` | #'knoxx.backend.infra.db.policy/default-membership-actor-id is private |
| `test/cljs/knoxx/backend/policy_actor_test.cljs:9:27` | #'knoxx.backend.infra.db.policy/default-membership-actor-id is private |
| `test/cljs/knoxx/backend/policy_actor_test.cljs:11:29` | #'knoxx.backend.infra.db.policy/default-membership-actor-id is private |
| `test/cljs/knoxx/backend/policy_actor_test.cljs:13:29` | #'knoxx.backend.infra.db.policy/default-membership-actor-id is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:11:20` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:13:20` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:15:18` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:17:17` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:21:27` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |
| `test/cljs/knoxx/backend/tools/temp_memory_test.cljs:23:27` | #'knoxx.backend.infra.temp-memory/parse-ttl-ms is private |

## Progress update — multimodal syntax blocker
- `backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs` paren balance was fixed by closing `request-parts-promise` and splitting `register-multimodal-routes!` into smaller route-registration helpers.
- Targeted check: `pnpm -C backend exec clj-kondo --lint src/cljs/knoxx/backend/infra/routes/multimodal.cljs` now reports `0` errors and `14` Promise-chain warnings.
- Full rerun: `pnpm -C backend lint` now reports `178` errors, `3347` warnings, and `2` info diagnostics. The previous `multimodal.cljs` syntax errors are gone; the suite still fails on the remaining backlog.
- Updated rerun log: `/tmp/knoxx-backend-lint-after-multimodal.log`.

## Recommended remediation order
1. Fix or update the `defroute` clj-kondo hook/macro call pattern; rerun lint and compare unresolved-symbol count.
2. Address remaining concrete unresolved symbols and arity mismatches namespace-by-namespace.
3. Split length-error functions over 60 lines, starting with route registration functions that also carry other diagnostics.
4. Batch warning cleanup: promise-chain conversion, unused bindings, unused requires, redundant expressions, and incomplete test protocol mocks.

## Verification plan
- Primary gate: `pnpm -C backend lint` must return exit code `0` with zero warnings/errors per Knoxx zero-warning policy.
- After code changes affecting backend behavior, run `pnpm -C backend exec shadow-cljs compile test`; for production paths, also run `pnpm -C backend exec shadow-cljs compile server`.
