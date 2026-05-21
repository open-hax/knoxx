# Backend Normalization References Report

Generated: 2026-05-20

## Scope

Searched `backend/` for case-insensitive references to `normalization` or `normalize`.

Generated/build/vendor output under paths such as `backend/dist`, `backend/dist-dev`, `backend/.shadow-cljs`, `backend/coverage`, `backend/target`, and ignored vendored `node_modules` was treated as noise for the source-count summary.

## Summary

Source/test/script/pseudo references total:

- 89 matching files
- 625 matching lines

Main contract-related surface:

- `backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs`
- `backend/src/cljs/knoxx/backend/domain/contracts/sources.cljs`
- `backend/src/cljs/knoxx/backend/domain/contracts/roles.cljs`
- `backend/src/cljs/knoxx/backend/domain/contracts/resolve.cljs`
- `backend/src/cljs/knoxx/backend/domain/actor/scope.cljs`
- `backend/src/cljs/knoxx/backend/domain/models.cljs`
- `backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs`
- `backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs`
- `backend/src/cljs/knoxx/backend/infra/tooling.cljs`
- `backend/test/cljs/knoxx/backend/contracts/*`
- `backend/test/cljs/knoxx/backend/contract_validator_test.cljs`

## Contract-Related Findings

| Area | Files | What normalization does |
|---|---|---|
| Contract class identity | `domain/contracts/loader.cljs`, `infra/routes/contracts.cljs`, tests | `normalize-contract-class` canonicalizes aliases like `agent`, `contract`, `source-mode`, `runtime-source`, `model-family`, `policy` into canonical class dirs/labels like `agents`, `source_modes`, `sources`, `model_families`, `policies`; unknown classes throw. |
| Contract parsing identity | `domain/contracts/loader.cljs` | `extract-contract-identity` prefers structural keys such as `:actor/id`, `:role/id`, `:model/id`, `:source-mode/id` before raw `:contract/kind`, then normalizes class and validates. Comment explicitly says this avoids namespaced/dashed kinds reaching class normalization too early. |
| Contract lookup/dedup | `domain/contracts/loader.cljs` | Sync/async loaders parse all EDN files, normalize class, validate, dedupe by `[contractClass id]`, and route individual reads through normalized class + identity instead of trusting folders. |
| Agent/actor contract scope | `domain/actor/scope.cljs`, `domain/contracts/resolve.cljs`, `infra/routes/contracts.cljs`, tests | Normalizes actor claims, wildcard `:*`, legacy `:contract/actor` into `:contract/actors`, auto-wildcards `knoxx_default`, and resolves actor permission/effective actor from normalized claims. |
| Runtime source contracts | `domain/contracts/sources.cljs`, tests | Normalizes source refs to canonical `:source/<slug>` keywords, maps them to contract IDs, resolves source contracts, and composes actor → role → agent → run source refs with deep-merge/dedupe by source id. |
| Roles/capabilities/tools | `domain/contracts/roles.cljs`, `infra/tooling.cljs` | Normalizes role IDs and capability refs, bridges underscore/dash/cap aliases, and normalizes tool IDs before enforcing role/tool permissions. |
| Model contracts | `domain/models.cljs` | Normalizes model-family/model contract fields: provider, API, boolean flags, prefixes, thinking levels, input kinds, and model family matching. |
| Policy adapter | `domain/policy/edn_adapter.cljs` | EDN policy store delegates contract-class normalization to loader, normalizes actor contracts by defaulting missing `:actor/kind` to `:agent`, and normalizes tool IDs from capability contracts. |
| UI/action contract render | `domain/contracts/resolve.cljs`, `infra/routes/contracts.cljs` | Normalizes UI actions into wire-safe maps and uses normalized class routing for metadata, version, enabled status, paths, and API outputs. |

## Detailed Source Count By File

```text
backend/src/cljs/knoxx/backend/domain/action/invoke_sub_agent.cljs: 1
backend/src/cljs/knoxx/backend/domain/action/registry.cljs: 1
backend/src/cljs/knoxx/backend/domain/action/run_pipeline.cljs: 1
backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs: 2
backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs: 8
backend/src/cljs/knoxx/backend/domain/actor/scope.cljs: 25
backend/src/cljs/knoxx/backend/domain/actor/tools.cljs: 5
backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs: 1
backend/src/cljs/knoxx/backend/domain/agent/content.cljs: 4
backend/src/cljs/knoxx/backend/domain/condition/registry.cljs: 2
backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs: 10
backend/src/cljs/knoxx/backend/domain/contracts/resolve.cljs: 6
backend/src/cljs/knoxx/backend/domain/contracts/roles.cljs: 6
backend/src/cljs/knoxx/backend/domain/contracts/sources.cljs: 11
backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs: 6
backend/src/cljs/knoxx/backend/domain/discord/source.cljs: 1
backend/src/cljs/knoxx/backend/domain/discord/tools.cljs: 5
backend/src/cljs/knoxx/backend/domain/event/cron.cljs: 9
backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs: 5
backend/src/cljs/knoxx/backend/domain/event/normalize.cljs: 2
backend/src/cljs/knoxx/backend/domain/event/tools.cljs: 3
backend/src/cljs/knoxx/backend/domain/label/quality.cljs: 4
backend/src/cljs/knoxx/backend/domain/media/blaze.cljs: 7
backend/src/cljs/knoxx/backend/domain/media/multimodal.cljs: 2
backend/src/cljs/knoxx/backend/domain/media/workspace.cljs: 1
backend/src/cljs/knoxx/backend/domain/media.cljs: 14
backend/src/cljs/knoxx/backend/domain/models.cljs: 60
backend/src/cljs/knoxx/backend/domain/music.cljs: 4
backend/src/cljs/knoxx/backend/domain/node/path.cljs: 3
backend/src/cljs/knoxx/backend/domain/openutau/openutau.cljs: 10
backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs: 10
backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs: 8
backend/src/cljs/knoxx/backend/domain/policy/sql_adapter.cljs: 2
backend/src/cljs/knoxx/backend/domain/policy/tools.cljs: 2
backend/src/cljs/knoxx/backend/domain/sandbox_container.cljs: 3
backend/src/cljs/knoxx/backend/domain/session_mycology.cljs: 2
backend/src/cljs/knoxx/backend/domain/trigger/normalize.cljs: 5
backend/src/cljs/knoxx/backend/domain/voice/tools.cljs: 8
backend/src/cljs/knoxx/backend/extern/agent_runner.cljs: 1
backend/src/cljs/knoxx/backend/extern/proxx.cljs: 2
backend/src/cljs/knoxx/backend/infra/agent/runner.cljs: 6
backend/src/cljs/knoxx/backend/infra/agent/session.cljs: 3
backend/src/cljs/knoxx/backend/infra/agent/turn.cljs: 4
backend/src/cljs/knoxx/backend/infra/auth/authz.cljs: 1
backend/src/cljs/knoxx/backend/infra/auth/session.cljs: 11
backend/src/cljs/knoxx/backend/infra/control_config.cljs: 19
backend/src/cljs/knoxx/backend/infra/core_memory.cljs: 9
backend/src/cljs/knoxx/backend/infra/db/actors.cljs: 10
backend/src/cljs/knoxx/backend/infra/db/policy.cljs: 42
backend/src/cljs/knoxx/backend/infra/document_state.cljs: 4
backend/src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs: 2
backend/src/cljs/knoxx/backend/infra/openplanner/semantic.cljs: 1
backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs: 6
backend/src/cljs/knoxx/backend/infra/pipeline_runner.cljs: 1
backend/src/cljs/knoxx/backend/infra/registry/tools.cljs: 3
backend/src/cljs/knoxx/backend/infra/routes/actors.cljs: 2
backend/src/cljs/knoxx/backend/infra/routes/app.cljs: 11
backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs: 20
backend/src/cljs/knoxx/backend/infra/routes/documents.cljs: 4
backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs: 2
backend/src/cljs/knoxx/backend/infra/routes/memory.cljs: 18
backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs: 1
backend/src/cljs/knoxx/backend/infra/routes/studio.cljs: 6
backend/src/cljs/knoxx/backend/infra/routes/voice.cljs: 2
backend/src/cljs/knoxx/backend/infra/routes/workspace_media.cljs: 9
backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs: 10
backend/src/cljs/knoxx/backend/infra/tooling.cljs: 16
backend/src/cljs/knoxx/backend/infra/trigger_runner.cljs: 1
backend/src/cljs/knoxx/backend/runtime/roles.cljs: 1
backend/src/cljs/knoxx/backend/shape/app_shapes.cljs: 15
backend/src/cljs/knoxx/backend/shape/path.cljs: 6
backend/test/cljs/knoxx/backend/actions/invoke_sub_agent_test.cljs: 7
backend/test/cljs/knoxx/backend/actor_mailbox_test.cljs: 6
backend/test/cljs/knoxx/backend/agent_turns_test.cljs: 2
backend/test/cljs/knoxx/backend/agents/runner_test.cljs: 4
backend/test/cljs/knoxx/backend/app_shapes_test.cljs: 16
backend/test/cljs/knoxx/backend/contract_validator_test.cljs: 7
backend/test/cljs/knoxx/backend/contracts/loader_test.cljs: 31
backend/test/cljs/knoxx/backend/contracts/sources_test.cljs: 5
backend/test/cljs/knoxx/backend/extern_agent_message_test.cljs: 2
backend/test/cljs/knoxx/backend/extern_agent_turn_media_test.cljs: 1
backend/test/cljs/knoxx/backend/openutau_test.cljs: 3
backend/test/cljs/knoxx/backend/policy_actor_test.cljs: 1
backend/test/cljs/knoxx/backend/session_titles_test.cljs: 3
backend/test/cljs/knoxx/backend/tools/blaze_music_generate_test.cljs: 3
backend/test/cljs/knoxx/backend/tools/registry_test.cljs: 8
backend/scripts/synthesize-music.mjs: 3
backend/pseudo/dead-tool-reasolution.cljs: 9
backend/pseudo/dead-tools-dispatch.cljs: 6
```

## Commands Used

```bash
rg -n -i "normalization|normalize" backend
rg -l -i "normalization|normalize" backend | sort
python3 ... # counted source/test/script/pseudo references, excluding generated/vendor/build noise
rg -n -i "normalization|normalize" \
  backend/src/cljs/knoxx/backend/domain/contracts \
  backend/src/cljs/knoxx/backend/domain/actor/scope.cljs \
  backend/src/cljs/knoxx/backend/domain/models.cljs \
  backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs \
  backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs \
  backend/src/cljs/knoxx/backend/infra/tooling.cljs \
  backend/test/cljs/knoxx/backend/contracts \
  backend/test/cljs/knoxx/backend/contract_validator_test.cljs
```

## Notes

- The raw `rg backend` result includes generated files under `dist`, `dist-dev`, `.shadow-cljs`, coverage, target bundles, and ignored vendored `node_modules`; these were excluded from the summarized source report to avoid duplicate/build-artifact inflation.
- “Contract-related” is broader than `domain/contracts/*`; important contract behavior lives in actor scope, model contracts, policy adapter, route adapter, and tooling namespaces.
- This was a read-only investigation plus documentation save; no tests were run.
