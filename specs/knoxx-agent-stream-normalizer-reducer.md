# Knoxx Agent Stream Normalizer and Reducer

Date: 2026-05-21
Status: todo
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Split provider JS stream event parsing and stream lifecycle reduction out of `stream.cljs` so stream semantics can be tested as pure data transformations.

## Problem

`stream.cljs` currently mixes:

- provider JS event shape parsing
- cumulative/replay text delta suppression
- `<think>` reasoning routing
- tool lifecycle start/update/end normalization
- death-spiral detection and abort decisions
- run-state mutation
- session-store mutation
- WebSocket broadcasting
- subscribe handler composition

The semantic parts should be pure; mutation and broadcasting should be sinks.

## Target modules

1. `knoxx.backend.infra.agent.stream.provider_events`
   - parse provider JS events into canonical CLJS event maps.
2. `knoxx.backend.domain.agent.text_delta`
   - `diff-appended-text` and replay/cumulative suppression.
3. `knoxx.backend.domain.agent.reasoning`
   - `<think>` tag routing shared with message normalization.
4. `knoxx.backend.domain.agent.tool_lifecycle`
   - normalize tool start/update/end event lifecycle.
5. `knoxx.backend.domain.agent.turn_guards`
   - death spiral detection and abort decision data.
6. `knoxx.backend.infra.agent.stream.reducer`
   - pure reducer from stream state + canonical event to effects/events.

## Deliverables

1. Canonical stream event shape for provider events:
   - `message_update`
   - `message_end`
   - `tool_execution_start`
   - `tool_execution_update`
   - `tool_execution_end`
   - `turn_end`
   - `agent_end`
2. `IStreamEventNormalizer` or equivalent function set for JS event parsing.
3. Pure stream reducer that returns updated state plus emitted semantic events/effects.
4. Tests for:
   - cumulative text replay suppression
   - appended token extraction
   - reasoning split/routing
   - tool lifecycle start/update/end
   - death-spiral guard decision
5. Thin compatibility wrapper in `stream.cljs` that wires provider subscription to reducer and sinks.

## Non-goals

1. Rewriting run-state, Redis, or WebSocket sinks in this slice.
2. Changing UI token event format except through explicit compatibility mapping.
3. Changing provider subscription behavior.
4. Hiding provider parse errors as successful stream events.

## Implementation notes

- Provider event normalizer should be tolerant at the boundary, but reducer input should be canonical.
- Keep reducer output data-oriented: no direct Redis, WS, or run-state mutation in reducer tests.
- If `<think>` handling is already duplicated in message code, extract the shared logic once.

## Acceptance criteria

1. Provider JS event parsing is isolated from stream lifecycle reduction.
2. Core text/reasoning/tool lifecycle behavior has pure tests.
3. `stream.cljs` becomes thinner composition code, or at minimum delegates semantic decisions to extracted functions.
4. Existing streaming output behavior is preserved for representative fixtures.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
```
