# Knoxx Backend Data Shapes Review

Date: 2026-05-21
Status: review
Scope: `backend/src/cljs/knoxx/backend/**`
Follow-up epic: `specs/knoxx-backend-law-shape-domain-epic.md`

## Purpose

Identify explicit, implied, and drift-prone data shapes used across the Knoxx ClojureScript backend, then define the target consolidation into `knoxx.backend.law.*` and `knoxx.backend.shape.*` domain namespaces.

## Review method

This review inspected:

- all 192 files under `backend/src/cljs/knoxx/backend/`
- existing explicit shape/law namespaces under `backend/src/cljs/knoxx/backend/{law,shape}/`
- policy/database DDL in `backend/src/cljs/knoxx/backend/infra/db/policy/schema.cljs`
- protocol definitions and store/client interfaces
- route normalizers and common map-returning functions
- namespaced keywords and wire keys used in domain, infra, extern, route, and store code

Commands used during inventory:

```bash
find backend/src/cljs/knoxx/backend -type f | sort
rg -n "CREATE TABLE|create table|insert into|from .*" backend/src/cljs/knoxx/backend/infra/db backend/src/cljs/knoxx/backend/shape/db -g '*.cljs'
python3 scripts/snippets-for-keyword-inventory # ad hoc keyword/def inventory in shell
```

## Executive summary

Knoxx already has the seed of a shape/law layer, but most runtime contracts remain implicit at route, client, store, and domain boundaries.

Current explicit type anchors:

- `knoxx.backend.law.contracts`: Malli schemas for disk/runtime contracts.
- `knoxx.backend.law.actions`: action contract and step schemas.
- `knoxx.backend.shape.session-persistence`: `KnoxxRun`, `ToolReceipt`, `TraceBlock`, `Message`, `ISessionStore`.
- `knoxx.backend.shape.app-shapes`: chat/control request normalization.
- `knoxx.backend.shape.db.*`: DB query helpers, but mostly no schemas.
- protocol interfaces in client/store/domain namespaces.

Main gaps:

1. The same semantic entities are expressed with multiple key styles: `:run_id`, `:run-id`, `runId`, `:session_id`, `:session-id`, `conversationId`, namespaced EDN keys, and DB snake_case.
2. Many response envelopes are repeated ad hoc: `{:status ...}`, `{:error ...}`, `{:ok? ...}`, `{:message ...}`, `{:data ...}`.
3. DB row shapes are defined only in SQL and HoneySQL accessors, not as reusable Malli schemas.
4. Tool definitions are data-oriented but not governed by a central `Tool`, `ToolCall`, `ToolResult`, or `ToolPolicy` shape namespace.
5. Agent turns, content parts, streaming events, transcript messages, and media payloads cross many namespaces without a single canonical shape.
6. External client protocols describe operations but not the maps they accept/return.
7. Authorization/session/auth context is scattered between `infra.auth.*`, route guards, DB rows, and app request bodies.

## Existing law namespaces

### `knoxx.backend.law.contracts`

Explicit Malli schemas:

- `ContractId`: string or keyword.
- `UserSurface`: UI/action surface metadata.
- `PolicyCheck`: policy invariant/required check record.
- `AgentSpec`: role/model/thinking hints.
- `ActorCapSpec`: capability vector wrapper.
- `HookMap`: before/after hook maps.
- `ContextPolicy`: context truncation policy.
- `RuntimeSourceRef`: runtime source reference by keyword/string/map.
- `UiAction`: UI-triggerable action metadata.
- `AgentContract`: agent contract record.
- `ActorContract`: actor contract record.
- `RoleContract`: role contract record.
- `CapabilityContract`: capability/tool/surface record.
- `PolicyContract`: policy contract record.
- `ModelFamilyContract`: model family registry record.
- `ModelContract`: model registry record.
- `SourceModeContract`: source-mode prompt/transform record.
- `RuntimeSourceContract`: runtime hydration source contract.
- `RuntimeFeatureContract`: runtime feature toggle contract.
- `IngestSourceContract`: ingestion source contract.
- `SubAgentContract`: sub-agent spawn contract.
- `ActionContract`: action handler contract.
- `PipelineStep`: pipeline step contract.
- `PipelineContract`: pipeline contract.
- `TriggerContract`: trigger contract.
- `CmsContract`: CMS registry/template contract.

Law behavior:

- `infer-contract-class` maps values to contract classes based on marker keys.
- `schema-for` maps a class to a Malli schema.
- `validate` emits `{:ok :value :errors}`.

Migration target:

- Keep these as law-level invariants.
- Split reusable nested shapes into `shape.contracts`, `shape.agent`, `shape.actor`, `shape.policy`, `shape.model`, `shape.source`, and let `law.contracts` compose/enforce them.

### `knoxx.backend.law.actions`

Explicit Malli schemas:

- `ActionKind`: enum including `:invoke/noop`, `:invoke/agent`, `:invoke/sub-agent`, `:invoke/http`, `:invoke/discord-post`, `:actions/start-agent`, `:actions/start-agent-session`, `:actions/stop-session`, `:actions/agent-send-steer-message`, `:actions/agent-queue-follow-up-message`.
- `ActionContract`: action invocation map with `:action/id`, `:action/kind`, optional `:action/label`, optional `:action/with`.
- `StepSpec`: `:uses` plus optional `:with`.

Migration target:

- Promote action runtime input/output and dispatch result shapes into `shape.action`.
- Keep allowed action kind, safety, and dispatch constraints in `law.actions`.

### `knoxx.backend.law.guards`

Implicit guard shapes:

- Fastify request-like object with session/user context.
- Session guard output is behavior-only today; failures are HTTP/authz side effects.

Migration target:

- Add `shape.auth/AuthContext`, `shape.http/FastifyRequestContext`, and `law.guards` validation functions.

### `knoxx.backend.law.url`

Value predicates:

- URL-looking string.
- media URL.
- data URL.

Migration target:

- Keep predicates in law, centralize URL/media locator shapes in `shape.media`.

## Existing shape namespaces

### `knoxx.backend.shape.agent`

Protocol:

- `IAgentSession`
  - `streaming?`
  - `current-turn`
  - `messages`
  - `follow-up!`
  - `steer!`
  - `set-thinking-level!`

Implied shapes:

- running agent session object, opaque JS turn object, eta-mu message history, steering/follow-up message.

Migration target:

- Add schemas for `AgentSessionRef`, `AgentMessage`, `AgentTurn`, `AgentTurnControl`, `AgentThinkingLevel`.
- Keep opaque implementation details external, but validate maps at boundaries.

### `knoxx.backend.shape.app-shapes`

Explicit normalizers:

- `normalize-chat-body` returns:
  - `:message`
  - `:conversation-id`
  - `:session-id`
  - `:run-id`
  - `:model`
  - `:thinking-level`
  - `:content-parts`
  - `:template-context`
  - `:mode`
  - `:agent-spec`
  - `:auth-context`
- `normalize-control-body` returns:
  - `:message`
  - `:conversation-id`
  - `:session-id`
  - `:run-id`
  - `:actor-id`
  - `:metadata`

Nested implied shapes:

- `ContentPart`: `{:type :text|:image|:audio|:video|:document, :text?, :url?, :data?, :mimeType?, :filename?, :size?}`.
- `AgentRequestSpec`: `:contract-id`, `:actor-id`, `:role`, `:system-prompt`, `:task-prompt`, `:model`, `:thinking-level`, `:tool-policies`, `:resource-policies`, `:sources`, `:memory-hydration`, `:context-policy`.
- `ToolPolicy`: `{:toolId string, :effect "allow"|"deny"}`.
- `ChatBody`: normalized HTTP chat request.
- `ControlBody`: normalized follow-up/steer/control request.

Migration target:

- Move schemas to `shape.app`, `shape.content`, `shape.agent`, `shape.tool-policy`; keep normalizers here or rename to `shape.app.normalize`.

### `knoxx.backend.shape.session-persistence`

Explicit Malli schemas:

- `RunStatus`: `"queued" | "running" | "completed" | "failed" | "waiting_input"`.
- `ToolReceipt`: `:id`, `:tool_name`, `:status`, optional `:started_at`, `:ended_at`, `:input_preview`, `:result_preview`.
- `TraceBlock`: `:id`, `:kind`, `:status`, optional `:at`.
- `Message`: `:role`, `:content`.
- `KnoxxRun`: `:run_id`, `:session_id`, `:conversation_id`, `:status`, `:created_at`, `:updated_at`, optional model/messages/tool_receipts/trace_blocks/answer/reasoning/error/has_active_stream/org_id/user_id.

Protocol:

- `ISessionStore`: `put-run!`, `get-run`, `patch-run!`, `list-active-runs`, `complete-run!`, `delete-run!`.

Migration target:

- Keep as canonical persistence shape but add compatibility schemas for Redis session-store records that currently overlap but are not always `KnoxxRun`.

### `knoxx.backend.shape.db.*`

Current state:

- Namespaces provide HoneySQL query helpers for DB tables.
- Most DB shape definitions are implied by SQL DDL in `infra.db.policy.schema` and by `insert`/`upsert` argument maps.

DB tables and row shapes:

1. `orgs`
   - columns: `id`, `slug`, `name`, `kind`, `is_primary`, `status`, `created_at`, `updated_at`
   - helper namespace: `shape.db.orgs`
2. `users`
   - columns: `id`, `email`, `display_name`, `auth_provider`, `external_subject`, `status`, `created_at`, `updated_at`
   - helper namespace: `shape.db.users`
3. `memberships`
   - columns: `id`, `user_id`, `org_id`, `actor_id`, `status`, `is_default`, `created_at`, `updated_at`
   - helper namespace: `shape.db.memberships`
4. `roles`
   - columns: `id`, `org_id`, `name`, `slug`, `scope_kind`, `built_in`, `system_managed`, `created_at`, `updated_at`
   - helper namespace: `shape.db.roles`
5. `role_permissions`
   - columns: `role_id`, `permission_code`, `effect`
6. `membership_roles`
   - columns: `membership_id`, `role_id`
7. `tool_definitions`
   - columns: `id`, `label`, `description`, `risk_level`
8. `role_tool_policies`
   - columns: `role_id`, `tool_id`, `effect`, `constraints_json`
9. `user_tool_policies`
   - columns: `membership_id`, `tool_id`, `effect`, `constraints_json`
10. `actor_credentials`
    - columns: `id`, `user_id`, `org_id`, `provider`, `kind`, `account_identifier`, `secret_json`, `status`, `created_at`, `updated_at`
11. `data_lakes`
    - columns: `id`, `org_id`, `name`, `slug`, `kind`, `config_json`, `status`, `created_at`, `updated_at`
12. `audit_events`
    - columns: `id`, `actor_user_id`, `actor_membership_id`, `org_id`, `action`, `resource_kind`, `resource_id`, `before_json`, `after_json`, `created_at`
13. `sessions`
    - columns: `id`, `user_id`, `membership_id`, `org_id`, `token_hash`, `token_prefix`, `salt`, `email`, `display_name`, `auth_provider`, `external_subject`, `ip_address`, `user_agent`, `expires_at`, `last_seen_at`, `created_at`
14. `invites`
    - columns: `id`, `org_id`, `code`, `email`, `inviter_membership_id`, `role_slugs`, `status`, `redeemed_by`, `redeemed_at`, `expires_at`, `created_at`
15. `knoxx_config`
    - columns: `key`, `value`, `updated_at`
16. `actor_mailbox_entries`
    - columns: `id`, `kind`, `status`, source fields, target fields, `delivery_mode`, `attempts`, scheduling timestamps, `content_ref_json`, `metadata_json`, `preview`, `last_error`, timestamps
    - enum constraints: status `pending|delivered|failed|expired|superseded|acknowledged`; delivery mode `steer|follow-up|event|inbox-only|direct-run`
17. `actor_mailbox_routes`
    - columns: `actor_id`, `conversation_id`, `session_id`, `run_id`, `contract_id`, `status`, `source_json`, `expires_at`, `last_seen_at`, `created_at`
    - enum constraints: status `active|inactive`
18. `studio_state`
    - columns: `id`, `user_id`, `org_id`, `kind`, `state_json`, timestamps
19. `studio_audio_assets`
    - columns: `id`, `audio_path`, `asset_type`, `image_data`, `mime_type`, `width`, `height`, `created_at`
    - enum constraint: asset_type `waveform|spectrogram`

Migration target:

- Add `shape.db.core` for common DB scalar conventions: UUID string, timestamp string/JS Date, JSONB map, status text.
- Add one Malli row schema per DB table.
- Add insert/update/select projection schemas where helper functions intentionally return joined/derived rows.

### `knoxx.backend.shape.number`, `shape.parse`, `shape.path`, `shape.url`

Current state:

- Small parse/normalization helpers.
- Implied shapes: positive integer parameter, truthy query parameter, devel path, basename/path-relative/path-absolute, resolved media URL, stripped data URL.

Migration target:

- Keep simple parsing helpers, add predicate schemas where used by routes.

## Domain-level implied shapes

### Action domain

Namespaces:

- `domain.action.dispatch`
- `domain.action.invoke_agent`
- `domain.action.invoke_sub_agent`
- `domain.action.loader`
- `domain.action.registry`
- `domain.action.run_pipeline`
- `domain.action.run_state`
- `domain.action.start_agent_session`

Shapes:

- `ActionInvocation`: `:action/kind`, `:action/with`, contract/action identity.
- `ActionDispatchResult`: success/error map from handlers.
- `RunState`: Redis-backed action/run event state with `:id`, `:status`, `:kind`, `:type`, `:content`, `:error`.
- `PipelineRunInput`: action `:with` plus `:event/payload`.
- `StartAgentSessionInput`: `:agent/id`, `:actor/id`, trigger context, event payload, optional message content.
- `SubAgentInvocation`: parent/child contract IDs, capabilities, mode, model, role, timeout, data.

Target namespaces:

- `shape.action`
- `shape.pipeline`
- `shape.run-state`
- `law.actions`
- `law.pipeline`

### Actor and auth domain

Namespaces:

- `domain.actor.credentials`
- `domain.actor.mailbox`
- `domain.actor.scope`
- `domain.actor.tools`
- `infra.auth.auth_session`
- `infra.auth.authz`
- `infra.auth.session`
- `law.guards`

Shapes:

- `ActorId`, `ActorScope`, `ActorRoleSet`.
- `ActorContract` from law.
- `AuthSession`: DB `sessions` row plus cookie/token fields.
- `AuthContext`: user, membership, org, roles, tool policies, actor ID.
- `ActorMailboxEntry`: namespaced map using `:mailbox/id`, `:mailbox/kind`, `:mailbox/status`, `:mailbox/source`, `:mailbox/target`, `:mailbox/delivery`, `:mailbox/content-ref`, timestamps, metadata, preview, last error.
- `ActorMailboxRoute`: actor to session/conversation/run routing row.

Target namespaces:

- `shape.actor`
- `shape.auth`
- `shape.mailbox`
- `law.actor`
- `law.authz`
- `law.mailbox`

### Agent runtime domain

Namespaces:

- `domain.agent.agent_context`
- `domain.agent.agent_templates`
- `domain.agent.content`
- `infra.agent.*`
- `extern.agent_*`

Shapes:

- `AgentMessage`: role/content plus multimodal content parts.
- `TranscriptMessage`: normalized transcript message.
- `TemplateContext`: runtime context map merged into prompts.
- `AgentRuntime`: in-process session/runtime state.
- `AgentTurn`: turn input, streaming state, output, error, trace/tool events.
- `AgentProviderRequest`: model, messages, tools, thinking, provider options.
- `AgentProviderResponse`: answer, reasoning, usage, finish reason.
- `RecoveryRecord`: stale/running session recovery state.

Target namespaces:

- `shape.agent`
- `shape.agent.message`
- `shape.agent.turn`
- `shape.agent.provider`
- `shape.agent.recovery`
- `law.agent`

### Content, media, and multimodal domain

Namespaces:

- `domain.agent.content`
- `domain.media`
- `domain.media.blaze*`
- `domain.media.multimodal`
- `domain.media.workspace`
- `extern.agent_turn_media`
- `infra.routes.multimodal`
- `infra.routes.workspace_media`
- `infra.svg_render`

Shapes:

- `ContentPart`: text/image/audio/video/document part.
- `MediaLocator`: URL, data URL, workspace path, remote media reference.
- `MediaPayload`: `:type`, `:url`, `:data`, `:path`, `:headers`, content metadata.
- `BlazeRequest` / `BlazeResponse`: image/media generation/editing payloads.
- `WorkspaceMediaFile`: path/name/type/content data.
- `SvgRenderRequest`: SVG/string plus render output type.

Target namespaces:

- `shape.content`
- `shape.media`
- `shape.media.blaze`
- `law.url`
- `law.media`

### Contract/policy/model/source domain

Namespaces:

- `domain.contracts.*`
- `domain.policy.*`
- `domain.models`
- `infra.db.policy`
- `infra.control_config`
- `infra.routes.contracts`
- `open_hax.contracts.*`

Shapes:

- Existing law contract shapes listed above.
- `ResolvedActor`: actor contract plus roles/capabilities/sources.
- `ResolvedRole`: role with permissions, prompts, sources.
- `ResolvedCapability`: tools and user surfaces.
- `PolicyStore` protocol maps for contract class/id/value.
- `ActorCredential`: provider/kind/account identifier/secret/status.
- `PolicyDecision`: allowed/denied reasons and call shape.
- `ModelDescriptor`: model id/provider/api/context/reasoning/input/default fields.
- `SourceDescriptor`: source ID/provider/hydration/render/filters/tools.

Target namespaces:

- `shape.contract`
- `shape.policy`
- `shape.model`
- `shape.source`
- `law.contracts`
- `law.policy`
- `law.models`

### Event, trigger, cron, and pipeline domain

Namespaces:

- `domain.event.*`
- `domain.trigger.normalize`
- `infra.trigger_runner`
- `infra.pipeline_runner`

Shapes:

- `Event`: `:event/id`, `:event/type`, `:event/types`, `:event/source`, `:event/actor`, `:event/payload`, `:event/timestamp`.
- `Trigger`: `:trigger/id`, `:trigger/kind`, `:trigger/events`, `:trigger/event-types`, `:trigger/action`, `:trigger/agent`, `:trigger/with`, `:trigger/source-kind`, `:trigger/enabled?`, `:trigger/condition`, `:trigger/context`, `:trigger/raw`.
- `CronSpec`: id/name/type/kind schedule details.
- `PipelineStep` and `PipelineContract` from law.
- `PipelineExecutionResult`: step status, errors, output data.

Target namespaces:

- `shape.event`
- `shape.trigger`
- `shape.pipeline`
- `law.trigger`
- `law.event`

### Tool domain

Namespaces:

- `domain.tools`
- `infra.agent.tools`
- `infra.registry.tools`
- `infra.routes.tools`
- `infra.routes.tools.proxy`
- many `domain.*.tools` namespaces

Shapes:

- `ToolDefinition`: name/id/description/parameters/execute function.
- `ToolParameters`: JSON-schema/TypeBox-like shape, often JS object.
- `ToolCall`: tool id/name, arguments, session/user/auth context.
- `ToolResult`: content/status/error/data envelope.
- `ToolPolicy`: allow/deny plus constraints.
- `ToolProxyRequest`: Fastify route proxy request to internal tool execution.
- `ToolReceipt`: already in session-persistence.

Target namespaces:

- `shape.tool`
- `shape.tool.policy`
- `shape.tool.receipt`
- `law.tools`

### HTTP route and response envelopes

Namespaces:

- `infra.http`
- `infra.http_server`
- all `infra.routes.*`

Repeated shapes:

- `HttpResponse`: `:status`, `:headers`, `:body`.
- `ApiSuccess`: `{:ok true, :data ...}` or route-specific success maps.
- `ApiError`: `{:error string, :message? string, :status? int}`.
- `HealthPayload`: per-service health/status URL maps.
- `RouteParams`: route params with path-derived IDs such as `:orgId`, `:roleId`, `:membershipId`, `:userId`, `:documentId`, `:runId`, `:triggerId`, `:jobId`.
- `Pagination/Query`: `:query`, truthy params, positive int params.

Target namespaces:

- `shape.http`
- `shape.api`
- `shape.route.params`
- `law.http`

### OpenPlanner / memory / graph domain

Namespaces:

- `infra.openplanner.memory`
- `infra.openplanner.semantic`
- `infra.openplanner.tools`
- `infra.clients.openplanner`
- `infra.clients.graph`
- `infra.core_memory`
- `infra.temp_memory`
- `infra.routes.memory`

Shapes:

- `MemoryNode`: id/kind/content/data/path/session/user metadata.
- `MemorySearchRequest`: `:query`, filters, session/user context.
- `MemorySearchResult`: node/result list plus status/error.
- `GraphQuery`: node id, direction, kind, limit.
- `SemanticQuery`: query text plus embedding/search options.
- `TempMemory`: id/type/params ephemeral record.

Target namespaces:

- `shape.memory`
- `shape.graph`
- `shape.semantic`
- `law.memory`

### Discord, Bluesky, Twitch, MCP, voice, music, OpenUtau, extension domains

These are vertical slices and should keep domain implementation local, but their boundary payloads should use shared shapes.

Shapes by slice:

- Discord: message, channel/guild/user IDs, REST request/response, source records, reaction label records, voice event payloads.
- Bluesky: ATProto request/response, post/chat payloads, DID/profile references.
- Twitch: IRC websocket message/user payload.
- MCP: JSON-RPC request/response, tool expose descriptor, bridge HTTP request.
- Voice: TTS/STT request/response, voice turn-control result, audio path/text payload.
- Music: AudD/AcoustID/MusicBrainz requests/responses, audio file analysis result.
- OpenUtau: project/track/note/voicebank tool inputs.
- Extensions: eta-mu extension runtime input/output.

Target namespaces:

- shared: `shape.external.http`, `shape.tool`, `shape.media`, `shape.message`
- domain-specific: `shape.discord`, `shape.bluesky`, `shape.twitch`, `shape.mcp`, `shape.voice`, `shape.music`, `shape.openutau`, `shape.extension`
- law-specific: only where Knoxx enforces local invariants, not upstream provider schemas.

## Protocol/interface types discovered

Protocols/interfaces should be backed by request/response shape docs:

- `shape.agent/IAgentSession`
- `shape.session-persistence/ISessionStore`
- `domain.policy.protocol/PolicyStore`
- `domain.policy.protocol/ActorCredentialStore`
- `domain.policy.protocol/ActorProjectionStore`
- `infra.stores.message-source/IMessageSource`
- `extern.fetch/IHttpClient`
- `domain.bluesky.client/IBlueskyClient`
- `domain.contracts.client/IContractLibrarianClient`
- `domain.discord.rest-client/IDiscordRestClient`
- `domain.mcp.client/IMcpHttpClient`
- `domain.media.blaze-client/IBlazeClient`
- `domain.media.remote-client/IRemoteMediaClient`
- `domain.music.audd-client/IAudDClient`
- `domain.voice.client/ITtsClient`
- `domain.voice.client/ISttClient`
- `infra.clients.github/IGitHubClient`
- `infra.clients.graph/IGraphClient`
- `infra.clients.health-probe/IHealthProbeClient`
- `infra.clients.ingestion/IIngestionClient`
- `infra.clients.knoxx-control/IKnoxxControlClient`
- `infra.clients.opencode/IOpenCodeClient`
- `infra.clients.openplanner/IOpenPlannerClient`
- `infra.clients.provider-catalog/IProviderCatalogClient`
- `infra.clients.proxx/IProxxClient`
- `infra.clients.shibboleth/IShibbolethClient`

## Canonical naming and conversion problems

Observed key styles:

- EDN namespaced: `:contract/id`, `:actor/id`, `:trigger/action`, `:mailbox/status`.
- EDN kebab: `:conversation-id`, `:session-id`, `:run-id`.
- EDN snake: `:conversation_id`, `:session_id`, `:run_id`.
- JS camel: `conversationId`, `sessionId`, `runId`, `contentParts`.
- DB snake: `conversation_id`, `token_hash`, `created_at`.
- External/provider JSON: mixed camel/snake depending on provider.

Recommended convention:

1. Internal domain maps use namespaced EDN keys when they model durable domain concepts.
2. Normalized app/request maps may use kebab keys, but should be converted immediately into domain shapes.
3. Persistence maps use DB snake_case only at DB/session-store edges.
4. Provider/client namespaces own provider-specific wire conversion.
5. `shape.*` namespaces define canonical maps and conversion helpers; `law.*` namespaces define invariants and policy checks.

## Proposed target namespace map

### Core shared shapes

- `knoxx.backend.shape.scalar`
  - IDs, timestamps, JSON maps, status strings, non-empty strings, positive ints.
- `knoxx.backend.shape.api`
  - success/error envelopes.
- `knoxx.backend.shape.http`
  - request/response/client result shapes.
- `knoxx.backend.shape.content`
  - content parts, messages, multimodal inputs.
- `knoxx.backend.shape.media`
  - URLs, data URLs, workspace paths, media refs.
- `knoxx.backend.shape.tool`
  - tool definitions, tool calls, tool results, tool receipts.

### Domain shapes

- `shape.agent`, `shape.agent.message`, `shape.agent.turn`, `shape.agent.provider`, `shape.agent.recovery`
- `shape.actor`, `shape.auth`, `shape.mailbox`
- `shape.contract`, `shape.policy`, `shape.model`, `shape.source`
- `shape.action`, `shape.event`, `shape.trigger`, `shape.pipeline`, `shape.run-state`
- `shape.memory`, `shape.graph`, `shape.semantic`
- `shape.discord`, `shape.bluesky`, `shape.twitch`, `shape.mcp`, `shape.voice`, `shape.music`, `shape.openutau`, `shape.extension`
- `shape.db.core`, `shape.db.orgs`, `shape.db.users`, `shape.db.memberships`, `shape.db.roles`, `shape.db.sessions`, `shape.db.invites`, `shape.db.audit`, `shape.db.mailbox`, `shape.db.studio`, `shape.db.tools`

### Law namespaces

- `law.contracts`: contract validation and class inference.
- `law.actions`: allowed action kinds and action dispatch constraints.
- `law.agent`: agent turn/session invariants.
- `law.authz`: authorization and role/tool policy constraints.
- `law.mailbox`: mailbox routing/status transitions.
- `law.event`: event normalization and admissibility.
- `law.trigger`: trigger kind/source/action constraints.
- `law.pipeline`: dependency and step execution constraints.
- `law.media`: allowed media source/path/url constraints.
- `law.tools`: tool call policy constraints.
- `law.db`: DB projection/row validation helpers for admin/debug boundaries.

## Prioritized migration order

1. Add shared scalar/API/HTTP/content/media/tool shapes.
2. Add DB row schemas from DDL without changing behavior.
3. Extract existing contract nested schemas from `law.contracts` into `shape.contract*`, then re-compose in `law.contracts`.
4. Add agent/session/turn/message schemas around existing app normalizers and session stores.
5. Add actor/auth/mailbox schemas and law transition checks.
6. Add event/trigger/action/pipeline schemas and wire validators into loaders/runners.
7. Add provider/client request/response shape docs as non-invasive schemas.
8. Add tests that validate representative fixtures and a small set of live normalizer outputs.

## Open questions

1. Should internal runtime shapes prefer fully namespaced keys everywhere, or allow request-normalized kebab keys in `infra.agent.*`?
2. Should DB schemas validate raw `pg` rows as JS-ish strings/dates, or normalized CLJS maps only?
3. Should external provider response shapes be strict enough to catch upstream drift, or permissive and only validate fields Knoxx consumes?
4. Should `law.*` throw, return `{:ok false}`, or both depending on boundary?
5. Should shape schemas live as pure Malli data only, or include `parse/normalize/assert` helpers per domain?

## Definition of reviewed

This report is an inventory and migration design, not a complete implementation. It should be used as the backing document for the epic and subtasks in `specs/knoxx-backend-law-shape-domain-epic.md`.
