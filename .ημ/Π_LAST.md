# Π Fork Tax Handoff

- Timestamp: 2026-05-20T23:29:55Z
- Repo: /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx
- Branch: chore/great-renamspacing
- Base HEAD: b08439cb5132c53b9e391465973e42f938f40e4e
- Scope: all visible repo changes in Knoxx working tree requested by user.
- Concurrent dirt intentionally left untouched: none classified; ignored .env was not staged.

## Verification

- PASS: `pnpm -C backend exec shadow-cljs compile test` (exit 0; warnings present; log sha256 3844314700ac98f2fa0a4c41fd33ec9ef94728d64998a245d8dcb7a910d848ec)
- PASS: `pnpm -C backend exec shadow-cljs compile server` (exit 0; warnings present; log sha256 b9ded89fa18f43bc4208069905a89f8a87712fdf16aec437eda8a7e9c94b5645)

## Pre-commit status

```text
## chore/great-renamspacing...origin/chore/great-renamspacing
 M backend/src/cljs/knoxx/backend/bootstrap.cljs
 M backend/src/cljs/knoxx/backend/domain/action/invoke_sub_agent.cljs
 M backend/src/cljs/knoxx/backend/domain/action/run_pipeline.cljs
 M backend/src/cljs/knoxx/backend/domain/action/run_state.cljs
 M backend/src/cljs/knoxx/backend/domain/action/start_agent_session.cljs
 M backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs
 M backend/src/cljs/knoxx/backend/domain/actor/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/resolve.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/roles.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/sources.cljs
 M backend/src/cljs/knoxx/backend/domain/contracts/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/discord_io.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/discord_reaction_labels.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/source.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/discord/voice_tools.cljs
 M backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs
 M backend/src/cljs/knoxx/backend/domain/event/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs
 M backend/src/cljs/knoxx/backend/domain/mcp/mcp_expose.cljs
 M backend/src/cljs/knoxx/backend/domain/media.cljs
 M backend/src/cljs/knoxx/backend/domain/media/blaze.cljs
 M backend/src/cljs/knoxx/backend/domain/media/multimodal.cljs
 M backend/src/cljs/knoxx/backend/domain/media/workspace.cljs
 M backend/src/cljs/knoxx/backend/domain/models.cljs
 M backend/src/cljs/knoxx/backend/domain/music.cljs
 M backend/src/cljs/knoxx/backend/domain/node/crypto.cljs
 M backend/src/cljs/knoxx/backend/domain/node/fs.cljs
 M backend/src/cljs/knoxx/backend/domain/node/path.cljs
 M backend/src/cljs/knoxx/backend/domain/nrepl.cljs
 M backend/src/cljs/knoxx/backend/domain/openutau/openutau.cljs
 M backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs
 M backend/src/cljs/knoxx/backend/domain/policy/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/realtime.cljs
 M backend/src/cljs/knoxx/backend/domain/sandbox_container.cljs
 M backend/src/cljs/knoxx/backend/domain/session_mycology.cljs
 M backend/src/cljs/knoxx/backend/domain/time.cljs
 M backend/src/cljs/knoxx/backend/domain/tools.cljs
 M backend/src/cljs/knoxx/backend/domain/twitch.cljs
 M backend/src/cljs/knoxx/backend/domain/voice/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/message.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/policy.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/provider.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/recovery.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/resume.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/runner.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/runtime.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/session.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/stream.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/transcript.cljs
 M backend/src/cljs/knoxx/backend/infra/agent/turn.cljs
 M backend/src/cljs/knoxx/backend/infra/auth/auth_session.cljs
 M backend/src/cljs/knoxx/backend/infra/auth/authz.cljs
 M backend/src/cljs/knoxx/backend/infra/auth/session.cljs
 M backend/src/cljs/knoxx/backend/infra/config.cljs
 M backend/src/cljs/knoxx/backend/infra/control_config.cljs
 M backend/src/cljs/knoxx/backend/infra/core.cljs
 M backend/src/cljs/knoxx/backend/infra/core_memory.cljs
 M backend/src/cljs/knoxx/backend/infra/db/policy.cljs
 M backend/src/cljs/knoxx/backend/infra/defaults.cljs
 M backend/src/cljs/knoxx/backend/infra/document_state.cljs
 M backend/src/cljs/knoxx/backend/infra/graceful_shutdown.cljs
 M backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs
 M backend/src/cljs/knoxx/backend/infra/openplanner/semantic.cljs
 M backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/pipeline_runner.cljs
 M backend/src/cljs/knoxx/backend/infra/registry/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/app.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/auth.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/documents.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/memory.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/models.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/studio.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/tools.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs
 M backend/src/cljs/knoxx/backend/infra/routes/workspace_media.cljs
 M backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/composite_session_store.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/redis_session_store.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/session_store.cljs
 M backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs
 M backend/src/cljs/knoxx/backend/infra/svg_render.cljs
 M backend/src/cljs/knoxx/backend/infra/temp_memory.cljs
 M backend/src/cljs/knoxx/backend/infra/tooling.cljs
 M backend/src/cljs/knoxx/backend/infra/trigger_runner.cljs
 M backend/src/cljs/knoxx/backend/law/guards.cljs
 M backend/src/cljs/knoxx/backend/shape/contracts.cljs
 M backend/src/cljs/knoxx/backend/shape/parse.cljs
 M backend/src/cljs/knoxx/backend/shape/path.cljs
 M backend/src/cljs/knoxx/backend/shape/session_persistence.cljs
 M backend/src/cljs/knoxx/backend/tools/mcp.cljs
 M backend/test/cljs/knoxx/backend/actions/invoke_sub_agent_test.cljs
 M backend/test/cljs/knoxx/backend/agent_hydration_test.cljs
 M backend/test/cljs/knoxx/backend/agent_runtime_test.cljs
 M backend/test/cljs/knoxx/backend/agent_turns_test.cljs
 M backend/test/cljs/knoxx/backend/agents/runner_test.cljs
 M backend/test/cljs/knoxx/backend/agents/stream_test.cljs
 M backend/test/cljs/knoxx/backend/auth_session_test.cljs
 M backend/test/cljs/knoxx/backend/authz_test.cljs
 M backend/test/cljs/knoxx/backend/contract_validator_test.cljs
 M backend/test/cljs/knoxx/backend/contracts/loader_test.cljs
 M backend/test/cljs/knoxx/backend/contracts/resolve_test.cljs
 M backend/test/cljs/knoxx/backend/contracts/sources_test.cljs
 M backend/test/cljs/knoxx/backend/node/fs_test.cljs
 M backend/test/cljs/knoxx/backend/node/path_crypto_test.cljs
 M backend/test/cljs/knoxx/backend/openutau_test.cljs
 M backend/test/cljs/knoxx/backend/pipeline_runner_test.cljs
 M backend/test/cljs/knoxx/backend/runtime_models_test.cljs
 M backend/test/cljs/knoxx/backend/session_store_test.cljs
 M backend/test/cljs/knoxx/backend/session_titles_test.cljs
 M backend/test/cljs/knoxx/backend/svg_render_test.cljs
 M backend/test/cljs/knoxx/backend/tooling_test.cljs
 M backend/test/cljs/knoxx/backend/tools/blaze_music_generate_test.cljs
 M backend/test/cljs/knoxx/backend/tools/blaze_music_integration_test.cljs
 M backend/test/cljs/knoxx/backend/tools/events_test.cljs
 M backend/test/cljs/knoxx/backend/tools/music_test.cljs
 M backend/test/cljs/knoxx/backend/tools/registry_test.cljs
 D backend/test/cljs/knoxx/backend/tools/resolution_test.cljs
 M backend/test/cljs/knoxx/backend/tools/shared_test.cljs
 M backend/test/cljs/knoxx/backend/tools/temp_memory_test.cljs
 M backend/test/cljs/knoxx/backend/triggers/control_config_test.cljs
 M backend/test/cljs/knoxx/backend/triggers/trigger_runner_test.cljs
 M docs/notes/2026.05.19.19.30.02.md
 M receipts.edn
?? CLAUDE.md
?? backend/src/cljs/knoxx/backend/domain/agent/agent_context.cljs
?? backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs
?? backend/src/cljs/knoxx/backend/domain/agent/content.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_message.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_runner.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_turn_media.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_turn_node.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_turn_prompt.cljs
?? backend/src/cljs/knoxx/backend/extern/agent_turn_result.cljs
?? backend/src/cljs/knoxx/backend/extern/eta_mu.cljs
?? backend/src/cljs/knoxx/backend/extern/js.cljs
?? backend/src/cljs/knoxx/backend/extern/json.cljs
?? backend/src/cljs/knoxx/backend/extern/promise.cljs
?? backend/src/cljs/knoxx/backend/extern/proxx.cljs
?? backend/src/cljs/knoxx/backend/infra/stores/composite_message_source.cljs
?? backend/src/cljs/knoxx/backend/infra/stores/message_source.cljs
?? backend/src/cljs/knoxx/backend/infra/stores/openplanner_message_source.cljs
?? backend/src/cljs/knoxx/backend/infra/stores/redis_message_source.cljs
?? backend/src/cljs/knoxx/backend/runtime/roles.cljs
?? backend/src/cljs/knoxx/backend/runtime/state.cljs
?? backend/src/cljs/knoxx/backend/shape/agent.cljs
?? backend/test/cljs/knoxx/backend/composite_session_store_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_message_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_runner_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_turn_media_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_turn_node_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_turn_prompt_test.cljs
?? backend/test/cljs/knoxx/backend/extern_agent_turn_result_test.cljs
?? backend/test/cljs/knoxx/backend/extern_proxx_test.cljs
?? backend/test/cljs/knoxx/backend/message_source_test.cljs
?? docs/notes/2026.05.20.08.59.16.md
?? docs/notes/2026.05.20.16.22.49.md
?? docs/notes/2026.05.20.17.23.12.cljs
?? docs/notes/2026.05.20.extern-boundary.md
```

## Diff stat

```text
 backend/src/cljs/knoxx/backend/bootstrap.cljs      |  20 +-
 .../backend/domain/action/invoke_sub_agent.cljs    |   6 +-
 .../knoxx/backend/domain/action/run_pipeline.cljs  |   2 +-
 .../knoxx/backend/domain/action/run_state.cljs     |  11 +-
 .../backend/domain/action/start_agent_session.cljs |   2 +-
 .../knoxx/backend/domain/actor/credentials.cljs    |   2 +-
 .../src/cljs/knoxx/backend/domain/actor/tools.cljs |   6 +-
 .../cljs/knoxx/backend/domain/bluesky/bluesky.cljs |   8 +-
 .../knoxx/backend/domain/contracts/loader.cljs     |   2 +-
 .../knoxx/backend/domain/contracts/resolve.cljs    |  10 +-
 .../cljs/knoxx/backend/domain/contracts/roles.cljs |   6 +-
 .../knoxx/backend/domain/contracts/sources.cljs    |   4 +-
 .../cljs/knoxx/backend/domain/contracts/tools.cljs |   6 +-
 .../knoxx/backend/domain/discord/discord_io.cljs   |   6 +-
 .../domain/discord/discord_reaction_labels.cljs    |   4 +-
 .../cljs/knoxx/backend/domain/discord/source.cljs  |   2 +-
 .../cljs/knoxx/backend/domain/discord/tools.cljs   |  10 +-
 .../knoxx/backend/domain/discord/voice_tools.cljs  |   8 +-
 .../cljs/knoxx/backend/domain/event/dispatch.cljs  |  18 +-
 .../src/cljs/knoxx/backend/domain/event/tools.cljs |   6 +-
 .../cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs  |   2 +-
 .../cljs/knoxx/backend/domain/mcp/mcp_expose.cljs  |   8 +-
 backend/src/cljs/knoxx/backend/domain/media.cljs   |   2 +-
 .../src/cljs/knoxx/backend/domain/media/blaze.cljs |   8 +-
 .../knoxx/backend/domain/media/multimodal.cljs     |   8 +-
 .../cljs/knoxx/backend/domain/media/workspace.cljs |   8 +-
 backend/src/cljs/knoxx/backend/domain/models.cljs  |   6 +-
 backend/src/cljs/knoxx/backend/domain/music.cljs   |   8 +-
 .../src/cljs/knoxx/backend/domain/node/crypto.cljs |   4 +-
 backend/src/cljs/knoxx/backend/domain/node/fs.cljs |   4 +-
 .../src/cljs/knoxx/backend/domain/node/path.cljs   |   4 +-
 backend/src/cljs/knoxx/backend/domain/nrepl.cljs   |   6 +-
 .../knoxx/backend/domain/openutau/openutau.cljs    |   2 +-
 .../cljs/knoxx/backend/domain/openutau/tools.cljs  |   2 +-
 .../knoxx/backend/domain/policy/edn_adapter.cljs   |   4 +-
 .../cljs/knoxx/backend/domain/policy/tools.cljs    |   4 +-
 .../src/cljs/knoxx/backend/domain/realtime.cljs    |   2 +-
 .../knoxx/backend/domain/sandbox_container.cljs    |   6 +-
 .../knoxx/backend/domain/session_mycology.cljs     |   6 +-
 backend/src/cljs/knoxx/backend/domain/time.cljs    |   2 +-
 backend/src/cljs/knoxx/backend/domain/tools.cljs   |   2 +-
 backend/src/cljs/knoxx/backend/domain/twitch.cljs  |   6 +-
 .../src/cljs/knoxx/backend/domain/voice/tools.cljs |   8 +-
 .../cljs/knoxx/backend/infra/agent/hydration.cljs  |  45 +-
 .../cljs/knoxx/backend/infra/agent/message.cljs    | 129 +----
 .../src/cljs/knoxx/backend/infra/agent/policy.cljs |   4 +-
 .../cljs/knoxx/backend/infra/agent/provider.cljs   |   6 +
 .../cljs/knoxx/backend/infra/agent/recovery.cljs   |  12 +-
 .../src/cljs/knoxx/backend/infra/agent/resume.cljs |  25 +-
 .../src/cljs/knoxx/backend/infra/agent/runner.cljs |  86 +---
 .../cljs/knoxx/backend/infra/agent/runtime.cljs    | 237 +--------
 .../cljs/knoxx/backend/infra/agent/session.cljs    | 566 ++++++++++-----------
 .../src/cljs/knoxx/backend/infra/agent/stream.cljs |  16 +-
 .../src/cljs/knoxx/backend/infra/agent/tools.cljs  |   2 +-
 .../cljs/knoxx/backend/infra/agent/transcript.cljs |  17 +-
 .../src/cljs/knoxx/backend/infra/agent/turn.cljs   | 208 +++-----
 .../knoxx/backend/infra/auth/auth_session.cljs     |   4 +-
 .../src/cljs/knoxx/backend/infra/auth/authz.cljs   |   4 +-
 .../src/cljs/knoxx/backend/infra/auth/session.cljs |   2 +-
 backend/src/cljs/knoxx/backend/infra/config.cljs   |   2 +-
 .../cljs/knoxx/backend/infra/control_config.cljs   |  10 +-
 backend/src/cljs/knoxx/backend/infra/core.cljs     |  17 +-
 .../src/cljs/knoxx/backend/infra/core_memory.cljs  |   4 +-
 .../src/cljs/knoxx/backend/infra/db/policy.cljs    |   6 +-
 backend/src/cljs/knoxx/backend/infra/defaults.cljs |   4 +-
 .../cljs/knoxx/backend/infra/document_state.cljs   |   6 +-
 .../knoxx/backend/infra/graceful_shutdown.cljs     |   8 +-
 .../knoxx/backend/infra/openplanner/memory.cljs    |  39 +-
 .../knoxx/backend/infra/openplanner/semantic.cljs  |  12 +-
 .../knoxx/backend/infra/openplanner/tools.cljs     |  10 +-
 .../cljs/knoxx/backend/infra/pipeline_runner.cljs  |   8 +-
 .../cljs/knoxx/backend/infra/registry/tools.cljs   |   2 +-
 .../src/cljs/knoxx/backend/infra/routes/app.cljs   |  58 +--
 .../src/cljs/knoxx/backend/infra/routes/auth.cljs  |   2 +-
 .../cljs/knoxx/backend/infra/routes/contracts.cljs |   8 +-
 .../cljs/knoxx/backend/infra/routes/documents.cljs |   4 +-
 .../src/cljs/knoxx/backend/infra/routes/mcp.cljs   |   4 +-
 .../cljs/knoxx/backend/infra/routes/memory.cljs    |  12 +-
 .../cljs/knoxx/backend/infra/routes/models.cljs    |   8 +-
 .../knoxx/backend/infra/routes/multimodal.cljs     |   4 +-
 .../cljs/knoxx/backend/infra/routes/studio.cljs    |   2 +-
 .../backend/infra/routes/studio/discord_scan.cljs  |   4 +-
 .../src/cljs/knoxx/backend/infra/routes/tools.cljs |   8 +-
 .../knoxx/backend/infra/routes/tools/proxy.cljs    |   2 +-
 .../backend/infra/routes/workspace_media.cljs      |   2 +-
 .../infra/source/opencode_session_ingester.cljs    |   2 +-
 .../infra/stores/composite_session_store.cljs      |  49 +-
 .../infra/stores/openplanner_session_store.cljs    |   6 +-
 .../backend/infra/stores/redis_session_store.cljs  |   4 +-
 .../knoxx/backend/infra/stores/session_store.cljs  |   2 +-
 .../knoxx/backend/infra/stores/session_titles.cljs |  58 +--
 .../src/cljs/knoxx/backend/infra/svg_render.cljs   |   2 +-
 .../src/cljs/knoxx/backend/infra/temp_memory.cljs  |   2 +-
 backend/src/cljs/knoxx/backend/infra/tooling.cljs  |  12 +-
 .../cljs/knoxx/backend/infra/trigger_runner.cljs   |   8 +-
 backend/src/cljs/knoxx/backend/law/guards.cljs     |   2 +-
 .../src/cljs/knoxx/backend/shape/contracts.cljs    |   2 +-
 backend/src/cljs/knoxx/backend/shape/parse.cljs    |   2 +-
 backend/src/cljs/knoxx/backend/shape/path.cljs     |  28 +-
 .../knoxx/backend/shape/session_persistence.cljs   |   2 +-
 backend/src/cljs/knoxx/backend/tools/mcp.cljs      |   6 +-
 .../backend/actions/invoke_sub_agent_test.cljs     |   2 +-
 .../cljs/knoxx/backend/agent_hydration_test.cljs   |   2 +-
 .../cljs/knoxx/backend/agent_runtime_test.cljs     |  79 ++-
 .../test/cljs/knoxx/backend/agent_turns_test.cljs  |  56 +-
 .../cljs/knoxx/backend/agents/runner_test.cljs     |  24 +-
 .../cljs/knoxx/backend/agents/stream_test.cljs     |   4 +-
 .../test/cljs/knoxx/backend/auth_session_test.cljs |   2 +-
 backend/test/cljs/knoxx/backend/authz_test.cljs    |   2 +-
 .../knoxx/backend/contract_validator_test.cljs     |   2 +-
 .../cljs/knoxx/backend/contracts/loader_test.cljs  |   2 +-
 .../cljs/knoxx/backend/contracts/resolve_test.cljs |   6 +-
 .../cljs/knoxx/backend/contracts/sources_test.cljs |   4 +-
 backend/test/cljs/knoxx/backend/node/fs_test.cljs  |   6 +-
 .../cljs/knoxx/backend/node/path_crypto_test.cljs  |   6 +-
 backend/test/cljs/knoxx/backend/openutau_test.cljs |   2 +-
 .../cljs/knoxx/backend/pipeline_runner_test.cljs   |   2 +-
 .../cljs/knoxx/backend/runtime_models_test.cljs    |   2 +-
 .../cljs/knoxx/backend/session_store_test.cljs     |   2 +-
 .../cljs/knoxx/backend/session_titles_test.cljs    |   8 +-
 .../test/cljs/knoxx/backend/svg_render_test.cljs   |   2 +-
 backend/test/cljs/knoxx/backend/tooling_test.cljs  |   2 +-
 .../backend/tools/blaze_music_generate_test.cljs   |   6 +-
 .../tools/blaze_music_integration_test.cljs        |   6 +-
 .../test/cljs/knoxx/backend/tools/events_test.cljs |   2 +-
 .../test/cljs/knoxx/backend/tools/music_test.cljs  |   2 +-
 .../cljs/knoxx/backend/tools/registry_test.cljs    |   2 +-
 .../cljs/knoxx/backend/tools/resolution_test.cljs  | 203 --------
 .../test/cljs/knoxx/backend/tools/shared_test.cljs |   2 +-
 .../cljs/knoxx/backend/tools/temp_memory_test.cljs |  12 +-
 .../backend/triggers/control_config_test.cljs      |   4 +-
 .../backend/triggers/trigger_runner_test.cljs      |   2 +-
 docs/notes/2026.05.19.19.30.02.md                  |  16 +
 receipts.edn                                       |   8 +
 134 files changed, 975 insertions(+), 1554 deletions(-)
```
