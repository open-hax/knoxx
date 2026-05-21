# Knoxx Agent Runtime Shapes

Date: 2026-05-21
Status: done
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Define canonical Malli/data schemas for the agent runtime maps that currently drift across routes, session persistence, provider interop, stream handling, and run state.

## Scope

Create or extend focused shape namespaces, preferring vertical sub-namespaces over one giant file. A first acceptable home is:

- `backend/src/cljs/knoxx/backend/shape/agent_runtime.cljs`

If the file becomes too broad, split into:

- `shape/agent/spec.cljs`
- `shape/agent/turn.cljs`
- `shape/agent/session.cljs`
- `shape/agent/stream.cljs`
- `shape/agent/run.cljs`

## Deliverables

1. Schemas for runtime request/response shapes:
   - `AgentSpec`
   - `TurnRequest`
   - `DirectStartPayload`
   - `ControlRequest`
   - `RecoveryRequest`
   - `TurnResponse`
   - `AcceptedResponse`
2. Schemas for auth/policy/context shapes consumed by the runtime:
   - `AuthContext`
   - `ToolPolicy`
   - `ContextPolicy`
   - `PolicyConstraints`
   - `TemplateContext`
3. Schemas for message/content/provider shapes:
   - `StoredMessage`
   - `ProviderAgentMessage`
   - `ContentPart`
   - `EtaMuAttachment`
4. Schemas for session/run/stream persistence:
   - `ActiveSessionEntry`
   - `RuntimeSetup`
   - `VisibleSessionSignature`
   - `StreamState`
   - `ProviderStreamEvent`
   - `CanonicalRunEvent`
   - `TokenEvent`
   - `ToolReceipt`
   - `TraceBlock`
   - `KnoxxRun`
   - `SessionStoreRecord`
5. A status vocabulary decision for tool receipts and run completion:
   - either canonicalize to current runtime values such as `"running"`, `"completed"`, `"failed"`; or
   - preserve existing schema values such as `"running"`, `"done"`, `"error"` behind explicit translation helpers.
6. Non-throwing validation helpers for the most-used shapes.

## Non-goals

1. Rewriting runtime behavior.
2. Enforcing closed schemas on provider JS payloads at the boundary.
3. Migrating every caller to validation in this slice.
4. Changing stored Redis/session JSON format without a compatibility codec.

## Implementation notes

- Reuse existing `shape/agent.cljs` and `shape/session_persistence.cljs` where possible.
- External provider payload schemas should be permissive but document fields Knoxx consumes.
- Mixed camel/kebab/snake aliases must be acknowledged explicitly, not silently erased.
- Tests should use representative fixtures from current code paths rather than idealized maps only.

## Acceptance criteria

1. The listed schemas are defined and exported from an obvious shape namespace.
2. Existing real-ish fixtures for turn request, session store record, tool receipt, and stream event validate successfully.
3. Tool receipt status drift is documented with either a canonical enum or translation helpers.
4. No runtime route or agent behavior changes are required to land this spec.

## Implementation result

Completed on 2026-05-21.

Touched files:

- `backend/src/cljs/knoxx/backend/shape/agent.cljs`
- `backend/src/cljs/knoxx/backend/shape/agent/message.cljs`
- `backend/src/cljs/knoxx/backend/shape/agent/recovery.cljs`
- `backend/src/cljs/knoxx/backend/shape/agent/runtime.cljs`
- `backend/src/cljs/knoxx/backend/shape/session_persistence.cljs`
- `backend/test/cljs/knoxx/backend/shape_agent_schemas_test.cljs`

Notes:

- Added `shape.agent.runtime` as the aggregate runtime-boundary schema namespace.
- Preserved `AgentRequestSpec` while adding `AgentSpec` as the canonical alias.
- Added explicit schemas for direct start payloads, auth context, context policy, policy constraints, active sessions, runtime setup, visible session signatures, stream state, provider stream events, canonical run/token events, turn/accepted responses, and session store records.
- Added message schemas for stored messages, eta-mu attachments, and provider agent messages.
- Documented status drift by accepting current runtime tool receipt statuses `"running"`, `"completed"`, `"failed"` plus legacy `"done"`/`"error"` during migration.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
git diff --check -- backend/src/cljs/knoxx/backend/shape/agent.cljs backend/src/cljs/knoxx/backend/shape/agent/message.cljs backend/src/cljs/knoxx/backend/shape/agent/recovery.cljs backend/src/cljs/knoxx/backend/shape/session_persistence.cljs backend/test/cljs/knoxx/backend/shape_agent_schemas_test.cljs specs/knoxx-agent-runtime-shapes.md specs/knoxx-agent-service-protocol-split-epic.md specs/README.md
git diff --check --no-index /dev/null backend/src/cljs/knoxx/backend/shape/agent/runtime.cljs
```

Results:

- `compile test`: exit 0; 321 tests, 787 assertions, 0 failures, 0 errors; 221 existing warnings.
- `compile server`: exit 0; build completed; 302 existing warnings.
- `git diff --check`: passed.
