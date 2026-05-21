# Knoxx Agent Service Facade

Date: 2026-05-21
Status: todo
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Create the small consumer-facing agent service surface that external routes, tools, and orchestration code can depend on instead of importing internals from `session.cljs`, `turn.cljs`, or `stream.cljs`.

## Target API

Add `knoxx.backend.infra.agent.service` with a minimal facade:

```clojure
(defprotocol IAgentService
  (start-turn! [svc turn-request])
  (queue-turn! [svc turn-request])
  (control-turn! [svc control-request])
  (resume-turn! [svc recovery-request])
  (active-turn [svc conversation-id]))
```

Also expose namespace functions that call the default service instance where existing code expects simple functions:

```clojure
(start-turn! req)
(queue-turn! req)
(control-turn! req)
(resume-turn! req)
(active-turn conversation-id)
```

## Deliverables

1. `knoxx.backend.infra.agent.service` namespace.
2. `IAgentService` protocol and default implementation map/record.
3. Default service constructor that receives explicit dependencies for provider, registry, tool policy, content/history, stream, sinks, policy, hydration, and recovery.
4. Compatibility functions for the existing external concepts:
   - `send-agent-turn!` delegates to `start-turn!` or `queue-turn!` as appropriate.
   - `spawn-direct!` delegates to `start-turn!` after payload normalization.
   - `queue-agent-control!` delegates to `control-turn!`.
   - `resume-recovered-session!` delegates to `resume-turn!`.
   - `active-agent-session` delegates to `active-turn` or registry port.
5. Route call-site migration for the highest-risk direct imports.
6. Tests proving compatibility wrappers call the facade and preserve existing response shapes.

## Non-goals

1. Deleting all old function names in the first slice.
2. Rewriting eta-mu provider behavior.
3. Replacing all internals before dependent ports exist.
4. Changing HTTP route response shape.

## Implementation notes

- Land this after enough ports/shapes exist to avoid a facade that merely re-exports the old gravity wells.
- Keep compatibility wrappers small and mark them as transitional in comments.
- If call-site migration is too large, migrate one route surface first and leave a follow-up TODO list in the epic.

## Acceptance criteria

1. External consumers have one documented service namespace to import.
2. Existing function names used by routes continue to work through compatibility wrappers.
3. At least one route or direct-spawn path is migrated to the service facade.
4. Tests cover start, queue, control, resume, and active-turn delegation at the facade level.
5. No PM2 restart is required as part of implementation.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```
