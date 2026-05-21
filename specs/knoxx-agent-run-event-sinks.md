# Knoxx Agent Run Event Sinks

Date: 2026-05-21
Status: todo
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Separate run-state mutation, session-store updates, OpenPlanner/event recording, and WebSocket broadcasting from stream semantics.

## Problem

`stream.cljs` and `turn.cljs` currently combine semantic stream handling with side effects:

- run-state mutation
- trace block updates
- tool receipt persistence
- Redis/session-store updates
- active streaming flags
- WebSocket token/event broadcasting
- OpenPlanner or graph/event sinks
- final answer/session finalization

This makes stream reducer behavior hard to test and makes sink failures hard to isolate.

## Target port

```clojure
(defprotocol IRunEventSink
  (emit-run-event! [sink run-event])
  (emit-token-event! [sink token-event])
  (update-run-state! [sink run-id update])
  (update-session-record! [sink session-id update])
  (finalize-run! [sink result])
  (record-run-error! [sink error-event]))
```

Exact method names may change. Implementations may be split if one port becomes too broad.

## Deliverables

1. Focused sink namespace for run-state/session-store/WS side effects.
2. Data shapes for sink inputs aligned with runtime shape schemas.
3. Tests using fake sinks to prove stream reducer/composition emits the expected sink calls.
4. Compatibility wiring from existing `stream.cljs` subscribe handler to sink implementation.
5. Error handling policy for partial sink failures:
   - what is fatal to the turn;
   - what is logged and continued;
   - what is retried or marked degraded.

## Non-goals

1. Rewriting the persistent run/session storage format in this slice.
2. Changing frontend WebSocket event contracts.
3. Introducing a new durable event bus.
4. Moving generic event-agent work into this slice.

## Implementation notes

- If `IRunEventSink` becomes too broad, split into `IRunStateSink`, `ISessionStateSink`, and `IClientEventSink` while keeping composition thin.
- Sink implementations may remain infra-level; domain reducers should only return data describing desired effects.
- Preserve current trace block and tool receipt payloads unless shape work has already introduced explicit compatibility translation.

## Acceptance criteria

1. Stream semantic code can be tested with fake sinks and no Redis/WS mutation.
2. Existing WebSocket token and run event behavior is preserved for representative stream fixtures.
3. Sink failure behavior is documented and covered by at least one test.
4. `stream.cljs` clearly separates reducer invocation from sink side effects.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
```
