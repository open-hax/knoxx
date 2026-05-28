# Π Fork Tax Handoff

- Timestamp: `2026-05-28T16:30:34Z`
- Repository: `/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx`
- Branch: `pi/fork-tax/20260526T204054Z-knoxx-host-services`
- Remote: `origin` (`git@github.com:open-hax/knoxx.git`)
- Snapshot base HEAD before artifact refresh: `c066cf0b206961ef70d46bd869b7db006b314d6f`
- Dirty entries before artifact refresh: `95`
- Planned tag: `pi/fork-tax/20260528T163034Z/knoxx-lint-remediation`

## Scope

- User requested a careful full fork tax snapshot.
- All visible repo-relevant dirty paths were treated as snapshot scope after review: backend CLJS lint remediation, async test repairs, Docker/runtime metadata, contracts, specs, docs, and receipts.
- No unrelated path was deleted, reset, restored, cleaned, or unstaged.
- No Knoxx PM2 process was restarted.

## Verification

- Dirty text secret heuristic scan: passed; matches were code/test references to token/secret/password/api-key labels, not literal secret material.
- `pnpm -C backend exec clj-kondo --lint test/cljs/knoxx/backend/mcp_http_test.cljs`: passed with 0 errors / 0 warnings.
- `git diff --check`: passed.
- `pnpm -C backend exec shadow-cljs compile test`: passed; 449 tests, 1320 assertions, 0 failures, 0 errors, 0 warnings.
- `pnpm -C backend exec shadow-cljs compile server`: passed; 0 warnings.
- `pnpm -C backend run lint`: blocked as expected by remaining lint baseline; exits 3 with 11 function-length errors and 1749 warnings. Log: `/tmp/knoxx-lint.log`.

## Remaining Lint Errors

- `gw-start-voice-listener` spans 171 lines.
- `ensure-schema!` spans 294 lines.
- `start-document-ingestion!` spans 163 lines.
- `index-run-memory-legacy!` spans 181 lines.
- `register-routes!` spans 188 lines.
- `register-auth-routes` spans 184 lines.
- `register-model-routes!` spans 193 lines.
- `register-proxy-routes!` spans 263 lines.
- `register-translation-routes!` spans 229 lines.
- `register-user-admin-routes!` spans 214 lines.
- `register-voice-routes!` spans 190 lines.

## Residual / Blockers

- Concurrent dirt intentionally left out: none identified.
- Blocked/generated/runtime paths left unstaged: none identified before staging.
- Full backend lint remains red on known function-length remediation work; test and server compile gates are green.

## Status Snapshot Before Commit

```text
## pi/fork-tax/20260526T204054Z-knoxx-host-services...origin/pi/fork-tax/20260526T204054Z-knoxx-host-services
 M ".\316\267\316\274/\316\240_LAST.md"
 M ".\316\267\316\274/\316\240_STATE.sexp"
 M backend/.clj-kondo/config.edn
 M backend/.clj-kondo/hooks/defroute.clj
 M backend/.clj-kondo/hooks/promise_chain.clj
 M backend/Dockerfile
 M backend/package.json
 M backend/src/cljs/knoxx/backend/bootstrap.cljs
 M backend/src/cljs/knoxx/backend/domain/actor/scope.cljs
 M backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/resolve.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs
 M backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/session_mycology.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/content_codec.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/recovery.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/runner.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/turn.cljs
 M backend/src/cljs/knoxx/backend/infra/auth/session.cljs
 M backend/src/cljs/knoxx/backend/infra/config.cljs
 M backend/src/cljs/knoxx/backend/infra/core_memory.cljs
 M backend/src/cljs/knoxx/backend/infra/db/policy.cljs
 M backend/src/cljs/knoxx/backend/infra/document_state.cljs
 M backend/src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs
 M backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs
 M backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/pipeline_runner.cljs
 M backend/src/cljs/knoxx/backend/infra/registry/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/admin.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/app.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/auth.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/memory.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/workspace_media.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs
 M backend/src/cljs/knoxx/backend/infra/temp_memory.cljs
 M backend/src/cljs/knoxx/backend/infra/tooling.cljs
 M backend/src/cljs/knoxx/backend/shape/app_shapes.cljs
 M backend/src/cljs/knoxx/backend/shape/db/memberships.cljs
 M backend/src/cljs/knoxx/backend/shape/db/roles.cljs
 M backend/src/cljs/open_hax/contracts/policy/fulfillment.cljs
 M backend/src/cljs/open_hax/contracts/policy/gate.cljs
 M backend/test/cljs/knoxx/backend/agent_hydration_test.cljs
 M backend/test/cljs/knoxx/backend/agent_templates_test.cljs
 M backend/test/cljs/knoxx/backend/agent_turns_test.cljs
 M backend/test/cljs/knoxx/backend/agents/content_history_tooling_ports_test.cljs
 M backend/test/cljs/knoxx/backend/agents/content_test.cljs
 M backend/test/cljs/knoxx/backend/agents/recovery_policy_hydration_ports_test.cljs
 M backend/test/cljs/knoxx/backend/agents/session_registry_provider_test.cljs
 M backend/test/cljs/knoxx/backend/contracts/resolve_test.cljs
 M backend/test/cljs/knoxx/backend/mcp_http_test.cljs
 M backend/test/cljs/knoxx/backend/memory_routes_test.cljs
 M backend/test/cljs/knoxx/backend/openplanner_semantic_test.cljs
 M backend/test/cljs/knoxx/backend/openutau_test.cljs
 M backend/test/cljs/knoxx/backend/pipeline_runner_test.cljs
 M backend/test/cljs/knoxx/backend/policy_actor_test.cljs
 M backend/test/cljs/knoxx/backend/tooling_test.cljs
 M backend/test/cljs/knoxx/backend/tools/temp_memory_test.cljs
 M contracts/agents/page_defaults/knoxx_default.edn
 M contracts/policies/basic_user_chat_guardrails.edn
 M contracts/roles/basic_user.edn
 M docs/cljs-async-and-request-state.md
 M frontend/Dockerfile
 M frontend/src/cljs/knoxx/frontend/app_routes.cljs
 M frontend/src/lib/app-routes.ts
 M frontend/src/pages/AdminLayout.tsx
 M receipts.edn
 M specs/README.md
 M specs/epics/knoxx-backend-cljs-lint-remediation.md
 M specs/tasks/knoxx-backend-lint-function-length-extractions.md
 M specs/tasks/knoxx-backend-lint-hard-error-first-pass.md
 D specs/tasks/knoxx-backend-lint-promesa-migration-src.md
 M specs/tasks/knoxx-backend-lint-route-helper-symbols.md
 M specs/tasks/knoxx-backend-lint-test-boundaries.md
 M specs/tasks/knoxx-backend-lint-unused-and-final-warnings.md
 M specs/tasks/knoxx-backend-lint-zero-warning-gate.md
?? backend/deps.edn
?? backend/mutation/README.md
?? backend/mutation/knoxx/mutation_test.clj
?? backend/src/cljs/knoxx/backend/shape/memory_sessions.cljs
?? backend/src/cljs/knoxx/backend/shape/pipeline.cljs
?? backend/test/clj/knoxx/mutation_test_test.clj
?? backend/test/cljs/knoxx/backend/agents/context_test.cljs
?? backend/test/cljs/knoxx/backend/agents/policy_test.cljs
?? backend/test/cljs/knoxx/backend/agents/stream_sinks_test.cljs
?? backend/test/cljs/knoxx/backend/agents/tool_lifecycle_test.cljs
?? backend/test/cljs/knoxx/backend/agents/transcript_test.cljs
?? backend/test/cljs/knoxx/backend/config_test.cljs
?? backend/test/cljs/knoxx/backend/local_path_contract_test.cljs
?? backend/test/cljs/knoxx/backend/memory_routes_page_state_test.cljs
?? backend/test/cljs/knoxx/backend/open_hax_policy_test.cljs
?? backend/test/cljs/knoxx/backend/shape_db_users_sessions_invites_test.cljs
?? backend/test/cljs/knoxx/backend/workspace_path_test.cljs
?? specs/tasks/knoxx-backend-lint-async-workflows-src.md
?? specs/tasks/knoxx-backend-lint-coverage-characterization.md
```
