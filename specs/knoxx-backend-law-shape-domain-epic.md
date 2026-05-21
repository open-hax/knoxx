# Knoxx Backend Law/Shape Domain Epic

Date: 2026-05-21
Status: next
Parent review: `specs/knoxx-backend-data-shapes-review.md`
Story points: 34

## Purpose

Turn the implicit data contracts spread across `backend/src/cljs/knoxx/backend/**` into explicit, reusable domain namespaces under:

- `backend/src/cljs/knoxx/backend/shape/`
- `backend/src/cljs/knoxx/backend/law/`

## Problem

Knoxx backend data currently crosses many boundaries as plain maps with locally implied shape:

- HTTP route bodies and responses
- agent turn/session state
- tool definitions, tool calls, and tool receipts
- contracts, roles, actors, models, sources, triggers, pipelines
- database rows and derived projections
- external client/protocol request and response maps
- media/content/multimodal payloads

Some explicit Malli schemas exist, but they do not yet cover the full backend. The result is duplicated normalization, mixed key styles, weak runtime checks, and harder refactors.

## Goals

1. Create a coherent `shape.*` taxonomy for backend data maps.
2. Keep `law.*` focused on invariants, admissibility, transitions, and policy checks.
3. Preserve vertical domain slices; do not create a god namespace.
4. Avoid behavior changes during the first extraction pass.
5. Add representative tests proving schemas accept current real fixtures.
6. Document conversion boundaries between JS camelCase, DB snake_case, EDN kebab, and EDN namespaced keys.

## Non-goals

1. Rewriting all backend code to strict closed schemas in one pass.
2. Changing database schema or route API shape.
3. Replacing domain-specific external provider payloads with over-strict schemas.
4. Restarting PM2 or production processes.
5. Moving implementation helpers from vertical slices unless they are genuinely shared.

## Architecture rules

1. `shape.*` namespaces define data:
   - Malli schemas
   - scalar predicates
   - parse/normalize helpers where appropriate
   - non-throwing validators
2. `law.*` namespaces define constraints:
   - admissibility checks
   - transition rules
   - authorization/policy invariants
   - boundary assertions
3. Shared shapes are small and composable.
4. Domain shapes should live near their conceptual domain, not in one `types.cljs` file.
5. External provider schemas should be permissive at the edge and strict only for fields Knoxx consumes.
6. DB row schemas should mirror current DDL, while projection schemas should document joined/derived rows separately.

## Proposed file map

### Shared shapes

- `shape/scalar.cljs`
- `shape/api.cljs`
- `shape/http.cljs`
- `shape/content.cljs`
- `shape/media.cljs`
- `shape/tool.cljs`

### Agent/runtime shapes

- `shape/agent.cljs` existing, extend carefully
- `shape/agent/message.cljs`
- `shape/agent/turn.cljs`
- `shape/agent/provider.cljs`
- `shape/agent/recovery.cljs`

### Actor/auth/mailbox shapes

- `shape/actor.cljs`
- `shape/auth.cljs`
- `shape/mailbox.cljs`

### Contract/policy/model/source shapes

- `shape/contract.cljs`
- `shape/policy.cljs`
- `shape/model.cljs`
- `shape/source.cljs`

### Event/action/pipeline shapes

- `shape/action.cljs`
- `shape/event.cljs`
- `shape/trigger.cljs`
- `shape/pipeline.cljs`
- `shape/run_state.cljs`

### Memory and external integration shapes

- `shape/memory.cljs`
- `shape/graph.cljs`
- `shape/semantic.cljs`
- `shape/discord.cljs`
- `shape/bluesky.cljs`
- `shape/twitch.cljs`
- `shape/mcp.cljs`
- `shape/voice.cljs`
- `shape/music.cljs`
- `shape/openutau.cljs`
- `shape/extension.cljs`

### DB shapes

- `shape/db/core.cljs`
- extend existing `shape/db/audit.cljs`, `invites.cljs`, `memberships.cljs`, `orgs.cljs`, `roles.cljs`, `sessions.cljs`, `users.cljs`
- add `shape/db/tools.cljs`
- add `shape/db/mailbox.cljs`
- add `shape/db/studio.cljs`
- add `shape/db/config.cljs`

### Law namespaces

- keep and extend `law/actions.cljs`, `law/contracts.cljs`, `law/guards.cljs`, `law/url.cljs`
- add `law/agent.cljs`
- add `law/authz.cljs`
- add `law/mailbox.cljs`
- add `law/event.cljs`
- add `law/trigger.cljs`
- add `law/pipeline.cljs`
- add `law/media.cljs`
- add `law/tools.cljs`
- add `law/db.cljs`

## Subtasks

### 1. Shared scalar/API/HTTP foundation

Status: todo
Story points: 3

Create foundational schemas used by every other namespace.

Deliverables:

- `shape.scalar`
  - `NonEmptyString`
  - `UuidString`
  - `IsoTimestampString`
  - `JsonMap`
  - `JsonValue`
  - `PositiveInt`
  - `StatusText`
- `shape.api`
  - `ApiSuccess`
  - `ApiError`
  - `ApiEnvelope`
  - `ValidationError`
- `shape.http`
  - `HttpMethod`
  - `HttpHeaders`
  - `HttpRequest`
  - `HttpResponse`
  - `FetchResult`

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 2. Content/media/tool shared shapes

Status: todo
Story points: 5

Codify shapes already implied by `shape.app-shapes`, media domains, and tool factories.

Deliverables:

- `shape.content/ContentPartType`
- `shape.content/ContentPart`
- `shape.content/MessageRole`
- `shape.content/Message`
- `shape.media/MediaLocator`
- `shape.media/DataUrl`
- `shape.media/WorkspaceMediaRef`
- `shape.tool/ToolDefinition`
- `shape.tool/ToolCall`
- `shape.tool/ToolResult`
- `shape.tool/ToolPolicy`
- `shape.tool/ToolReceipt`

Integration:

- Make `shape.app-shapes` use these schemas in tests without changing route behavior.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 3. DB row schemas from current DDL

Status: todo
Story points: 5

Mirror `infra.db.policy.schema` table definitions as Malli schemas.

Deliverables:

- Row schemas for:
  - orgs
  - users
  - memberships
  - roles
  - role permissions
  - membership roles
  - tool definitions
  - role tool policies
  - user tool policies
  - actor credentials
  - data lakes
  - audit events
  - sessions
  - invites
  - knoxx config
  - actor mailbox entries
  - actor mailbox routes
  - studio state
  - studio audio assets
- Separate insert/update schemas where helper functions do not require full rows.
- Projection schemas for joined admin views.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 4. Contract schema extraction without behavior changes

Status: todo
Story points: 5

Refactor reusable schema data out of `law.contracts` into `shape.contract`, `shape.actor`, `shape.policy`, `shape.model`, and `shape.source`, while keeping public validation behavior stable.

Deliverables:

- `shape.contract/ContractId`
- `shape.contract/ContractHeader`
- `shape.actor/ActorContract`
- `shape.policy/PolicyCheck`
- `shape.model/ModelFamilyContract`, `ModelContract`
- `shape.source/RuntimeSourceRef`, `RuntimeSourceContract`, `SourceModeContract`, `IngestSourceContract`
- `law.contracts` composes imported schemas and still exposes `validate`.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 5. Agent/session/turn schemas

Status: done
Story points: 5

Make agent runtime state and persisted run state explicit.

Implementation note 2026-05-21: schema namespaces and representative tests have been added. Full `pnpm -C backend exec shadow-cljs compile test` and `pnpm -C backend exec shadow-cljs compile server` are green after clearing the blocking Proxx response normalization and hyphenated role-slug actor default issues.

Deliverables:

- Preserve `shape.session-persistence/KnoxxRun` as persistence baseline.
- Add `shape.agent/AgentRequestSpec`.
- Add `shape.agent.message/AgentMessage`, `TranscriptMessage`, `TemplateContext`.
- Add `shape.agent.turn/AgentTurnInput`, `AgentTurnState`, `AgentTurnResult`, `StreamEvent`.
- Add `shape.agent.provider/ProviderRequest`, `ProviderResponse`.
- Add `shape.agent.recovery/RecoveryRecord`.
- Add tests for `normalize-chat-body`, `normalize-control-body`, and representative session-store records.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 6. Actor/auth/mailbox schemas and laws

Status: todo
Story points: 3

Unify actor/session/auth context and mailbox record shapes.

Deliverables:

- `shape.auth/AuthContext`, `AuthSession`, `SessionCookieContext`.
- `shape.mailbox/MailboxEntry`, `MailboxRoute`, `MailboxDeliveryMode`, `MailboxStatus`.
- `law.authz` for role/tool-policy admissibility checks.
- `law.mailbox` for allowed status and delivery transitions.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 7. Event/trigger/action/pipeline schemas and laws

Status: todo
Story points: 3

Codify the event automation runtime.

Deliverables:

- `shape.event/Event`
- `shape.trigger/Trigger`
- `shape.action/ActionInvocation`, `ActionResult`
- `shape.pipeline/PipelineStep`, `PipelineRun`, `PipelineResult`
- `shape.run-state/RunEvent`, `RunStateSnapshot`
- `law.event`, `law.trigger`, `law.pipeline` with non-invasive validators.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 8. Memory/graph/semantic shapes

Status: todo
Story points: 2

Document the OpenPlanner/memory boundary maps.

Deliverables:

- `shape.memory/MemoryNode`, `MemorySearchRequest`, `MemorySearchResult`.
- `shape.graph/GraphNode`, `GraphEdge`, `GraphQuery`.
- `shape.semantic/SemanticQuery`, `SemanticResult`.
- Representative tests around route/client fixture maps.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 9. External integration boundary shape docs

Status: todo
Story points: 3

Add permissive consumed-field schemas for vertical integration slices.

Deliverables:

- `shape.discord`
- `shape.bluesky`
- `shape.twitch`
- `shape.mcp`
- `shape.voice`
- `shape.music`
- `shape.openutau`
- `shape.extension`

Rule:

- Do not overfit full third-party API schemas. Validate only fields Knoxx reads/writes.

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
```

### 10. Boundary tests and migration docs

Status: todo
Story points: 3

Create fixtures and documentation that make future shape work safe.

Deliverables:

- `backend/test/cljs/knoxx/backend/shape/*_test.cljs` fixtures for core shapes.
- A short namespace README or docstring convention explaining shape vs law.
- A key-style conversion table:
  - JS camelCase at HTTP/provider edges
  - DB snake_case at persistence edges
  - EDN namespaced keys for durable domain concepts
  - EDN kebab keys for normalized route request bodies only when intentionally local

Verification:

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

## Suggested implementation sequence

1. Implement subtasks 1–2 first because they unblock most downstream schemas.
2. Implement subtask 3 next because DB rows are stable and can be validated without runtime changes.
3. Implement subtask 4 carefully to avoid breaking contract validation.
4. Implement subtasks 5–7 around active runtime flows.
5. Implement subtasks 8–9 as boundary documentation and consumed-field checks.
6. Finish with subtask 10 and run full backend verification.

## Risks

1. Strict schemas may reject current tolerated payloads.
   - Mitigation: default to `{:closed false}` during first pass.
2. Namespace explosion could make discoverability worse.
   - Mitigation: keep names aligned with existing vertical domain slices and add a top-level README/docstring index.
3. Contract refactor may break disk contract validation.
   - Mitigation: preserve `law.contracts/validate` API and add fixtures before moving schemas.
4. DB rows may differ between Postgres JS driver output and normalized CLJS maps.
   - Mitigation: separate raw row schemas from normalized domain projection schemas.

## Definition of done

- Review inventory is checked in at `specs/knoxx-backend-data-shapes-review.md`.
- Shape/law namespace taxonomy exists and is documented.
- Existing explicit schemas are preserved or re-exported compatibly.
- DB row schemas cover all current DDL tables.
- Agent/session/tool/content/action/event/trigger/pipeline/auth/mailbox maps have explicit schemas.
- Representative fixtures validate under backend tests.
- `pnpm -C backend exec shadow-cljs compile test` completes with zero failures and zero errors.
- `pnpm -C backend exec shadow-cljs compile server` completes for production backend confidence.
