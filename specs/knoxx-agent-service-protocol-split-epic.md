# Knoxx Agent Service Protocol Split Epic

Date: 2026-05-21
Status: draft
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 35

## Purpose

Split the implicit Knoxx agent session/runtime surface into a small consumer-facing service facade, replaceable effectful ports, canonical Malli/data shapes, and pure stream/content reducers.

## Problem

Agent execution currently leaks across `session.cljs`, `turn.cljs`, and `stream.cljs`. Consumers import internal helpers directly, while effectful concerns such as eta-mu provider interop, active-session caching, tool policy, content materialization, stream normalization, Redis/run-state mutation, WebSocket broadcasting, recovery, hydration, and policy validation are braided together.

The current de facto public surface includes too many implementation helpers:

- `send-agent-turn!`
- `spawn-direct!`
- `queue-agent-control!`
- `active-agent-session`
- `ensure-session-id`
- `ensure-conversation-access!`
- `validate-chat-policy!`
- `resume-recovered-session!`

This makes internal refactors risky and allows shape drift between route inputs, runtime maps, provider JS payloads, stored sessions, run state, tool receipts, and stream events.

## Target model

Everything outside `knoxx.backend.infra.agent` should primarily call a tiny service facade:

```clojure
(knoxx.backend.infra.agent.service/start-turn! svc req)
(knoxx.backend.infra.agent.service/queue-turn! svc req)
(knoxx.backend.infra.agent.service/control-turn! svc req)
(knoxx.backend.infra.agent.service/resume-turn! svc req)
(knoxx.backend.infra.agent.service/active-turn svc conversation-id)
```

The facade delegates to explicit ports and pure reducers:

- `IAgentProviderAdapter`
- `IActiveSessionRegistry`
- `IMessageHistory` / `ITranscriptCodec`
- `IToolCatalog` / `IToolPolicyResolver`
- `IContentCodec` / `IMediaMaterializer`
- `IStreamEventNormalizer`
- `IStreamReducer`
- `IRunEventSink`
- `IPolicyEngine`
- `IHydrationSource`
- `IRecoveryCoordinator`

Malli/data shapes live under `knoxx.backend.shape.*`; protocols are reserved for replaceable behavior, not every map or helper.

## Goals

1. Establish one small agent runtime service surface for external consumers.
2. Move effectful dependencies behind explicit ports that are easy to mock in tests.
3. Extract stream/content/message logic into pure reducers/codecs where possible.
4. Canonicalize high-value runtime shapes and status vocabularies.
5. Keep existing runtime behavior unchanged during the first extraction pass.
6. Preserve Knoxx vertical domain slicing; avoid a new god namespace.
7. Add regression tests around existing behavior before redirecting call sites.

## Non-goals

1. Replacing eta-mu/proxx provider behavior.
2. Rewriting all agent routes or UI flows in one pass.
3. Strictly closing every external provider payload schema.
4. Changing Redis/session-store persistence format except through explicit compatibility codecs.
5. Restarting PM2 processes without explicit operator approval.
6. Moving unrelated event-agent or graph-memory work into this epic.

## Architecture rules

1. Consumer code imports the service facade, not `session.cljs`/`turn.cljs` internals.
2. Protocols represent effectful, replaceable behavior.
3. Maps are documented with Malli schemas and pure normalization helpers, not protocols.
4. JS provider payload parsing happens once at the boundary.
5. Stream reducers are pure where feasible; sinks own mutation and broadcasting.
6. Existing mixed key styles are handled at codec boundaries, not spread into new code.
7. Compatibility shims may remain temporarily, but must delegate into the new facade/ports.

## Child specs

- `knoxx-agent-runtime-shapes.md` (5 pts, done) — canonical runtime schemas and status vocabulary.
- `knoxx-agent-service-facade.md` (5 pts) — `IAgentService` facade and compatibility exports.
- `knoxx-agent-session-registry-provider-ports.md` (5 pts) — active-session registry and provider adapter extraction.
- `knoxx-agent-content-history-tooling-ports.md` (5 pts) — content/media, transcript/history, and tool catalog/policy ports.
- `knoxx-agent-stream-normalizer-reducer.md` (5 pts) — provider event normalization and pure stream reducer extraction.
- `knoxx-agent-run-event-sinks.md` (5 pts) — run-state/session-store/WS sinks separated from stream semantics.
- `knoxx-agent-recovery-policy-hydration-ports.md` (5 pts) — recovery, policy, and hydration as lifecycle/service ports.

## Suggested sequence

1. Runtime shapes.
2. Stream normalizer/reducer.
3. Run event sinks.
4. Session registry/provider ports.
5. Content/history/tooling ports.
6. Recovery/policy/hydration ports.
7. Service facade and call-site migration.

The facade is listed last in execution order because it should wrap stabilized ports and shapes, even though it is the conceptual target.

## Acceptance criteria

1. External consumers can start, queue, control, resume, and inspect active turns through `knoxx.backend.infra.agent.service`.
2. Existing route and direct-spawn behavior is covered by tests before and after facade migration.
3. `session.cljs`, `turn.cljs`, and `stream.cljs` are thinner composition namespaces with major concerns moved into focused files.
4. Tool receipt statuses and stream/run event statuses have one canonical vocabulary or explicit translation codec.
5. Provider JS events are normalized once before reducer/sink code sees them.
6. Backend test build reports zero failures and zero errors.

## Verification

For each implementation child touching backend CLJS:

```bash
pnpm -C backend exec shadow-cljs compile test
```

For production backend behavior changes, also run:

```bash
pnpm -C backend exec shadow-cljs compile server
```
