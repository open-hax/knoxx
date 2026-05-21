# Knoxx Agent Recovery, Policy, and Hydration Ports

Date: 2026-05-21
Status: todo
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Expose recovery coordination, policy evaluation, and hydration as explicit service ports instead of embedding their orchestration inside `turn.cljs`, `session.cljs`, `recovery.cljs`, `resume.cljs`, and `hydration.cljs` call chains.

## Problem

Turn startup and recovery currently mix:

- auth/conversation access checks
- chat policy validation
- model/resource/tool policy constraints
- RAG/semantic hydration
- memory hydration
- session recovery at startup/periodic/shutdown moments
- resume request/result handling
- final memory indexing and cleanup hooks

These are lifecycle services with different dependencies, retry behavior, and tests.

## Target ports

```clojure
(defprotocol IPolicyEngine
  (authorize-turn [engine turn-request])
  (resolve-model-policy [engine auth-context requested-model])
  (resolve-tool-policy [engine auth-context agent-spec])
  (resolve-resource-policy [engine auth-context agent-spec]))

(defprotocol IHydrationSource
  (hydrate [source hydration-request]))

(defprotocol IRecoveryCoordinator
  (recover-startup! [coordinator])
  (recover-session! [coordinator recovery-request])
  (resume-turn! [coordinator recovery-request])
  (shutdown-recovery! [coordinator]))
```

Exact method names may change, but the separation should remain.

## Deliverables

1. Policy engine boundary around existing access/model/tool/resource checks.
2. Hydration source abstraction for semantic/RAG and memory hydration with a common result shape.
3. Recovery coordinator boundary for startup, periodic, shutdown, and explicit resume flows.
4. Tests using fake policy engines, hydration sources, and recovery coordinators.
5. Transitional wrappers for existing public functions such as `validate-chat-policy!` and `resume-recovered-session!` where still needed.

## Non-goals

1. Replacing existing policy semantics with a full contract DSL in this slice.
2. Changing source databases, vector search, or memory indexing implementation.
3. Rewriting scheduler/startup process management.
4. Changing authz outcomes except to preserve or document existing behavior.

## Implementation notes

- The report notes that `policy.cljs` wants to evolve toward a contract DSL; this spec only creates the seam.
- Hydration result schemas should cover both semantic/RAG and memory hydration variants.
- Recovery should remain operationally conservative: do not restart PM2 processes as part of implementation.

## Acceptance criteria

1. Turn orchestration can call policy, hydration, and recovery through ports/fakes in tests.
2. Existing access and policy validation behavior is preserved for representative allowed and denied fixtures.
3. Existing hydration result payloads are preserved or translated through explicit codecs.
4. Existing recovery/resume behavior is preserved for representative active and missing session cases.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```
