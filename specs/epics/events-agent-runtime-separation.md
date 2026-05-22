# Events / Agents Runtime Separation

Date: 2026-05-06
Status: draft
Repo: `packages/agents/knoxx`

## Goal

Separate the generic event system from the agent runtime so Knoxx has:

1. one agent runtime,
2. one generic event bus / scheduler / trigger layer,
3. no special "event agent" execution path.

## Problem

Today `backend/src/cljs/knoxx/backend/event_agents.cljs` mixes at least five concerns:

1. event bus behavior,
2. cron/ticker behavior,
3. source adapters (Discord etc),
4. trigger matching / schedule state,
5. agent dispatch.

That creates an architectural split-brain:

1. chat/runtime agents go through `send-agent-turn!` and the Knoxx chat runtime,
2. so-called "event agents" go through a second orchestration surface.

As long as those stay entangled, Knoxx effectively has two agent runtimes.

## Target Model

### 1. Events are generic infrastructure

Create an `events` subsystem that owns:

1. normalized event envelope,
2. append/dispatch semantics,
3. source adapters,
4. subscriptions,
5. cron/ticker scheduling,
6. persisted event/timer state.

### 2. Agents are just agents

Agents do not "understand events" via a separate runtime.

Instead:

1. events produce a normalized event envelope,
2. trigger rules decide what should happen,
3. matched actions dispatch a normal agent run through the same Knoxx runtime used by chat.

### 3. Triggering is separate from execution

Introduce a trigger layer whose only job is:

1. subscribe to events,
2. match rules / schedules,
3. invoke actions.

It should not contain agent orchestration logic.

## Namespace / Surface Split

### Old → New

1. `knoxx.backend.event-agents`
   - split into:
     - `knoxx.backend.events.bus`
     - `knoxx.backend.events.state`
     - `knoxx.backend.events.cron`
     - `knoxx.backend.events.sources.discord`
     - `knoxx.backend.events.triggers`

2. `knoxx.backend.discord-cron`
   - becomes compatibility shim over `knoxx.backend.events.cron`
   - eventual name target: `knoxx.backend.events.cron`

3. `knoxx.backend.triggers.trigger-runner`
   - fold or align into `knoxx.backend.events.triggers`
   - one trigger engine only

4. `event agent` jobs
   - become trigger definitions plus dispatched actions
   - not a separate agent runtime class

## Contract Model

### Current unwanted concept

`event-agent job`

This concept currently mixes:

1. trigger definition,
2. source subscription,
3. dispatch action,
4. agent template.

### Target contract split

1. `:event-source`
   - source adapter config
   - e.g. Discord gateway, webhook input, cron ticker

2. `:trigger`
   - matching logic
   - binds source/event kinds to actions

3. `:action` or `:dispatch`
   - what happens when triggered
   - examples:
     - dispatch normal agent run
     - emit follow-up event
     - execute pipeline

4. existing `:agent`
   - unchanged conceptually
   - remains a normal Knoxx agent contract

## Tool Surface Rewrite

### Remove event-agent-centric tools

Deprecate these semantics:

1. `event_agents.run_job`
2. `event_agents.upsert_job`
3. `schedule_event_agent`

### Replace with two primitives

1. `events.dispatch`
   - publish a normalized event onto the event bus
   - agents or admin surfaces use this to stimulate triggers

2. `agents.spawn`
   - launch a one-off normal agent run directly
   - no scheduler/job abstraction required

Optional supporting read/admin tools:

1. `events.status`
2. `events.subscriptions`
3. `events.replay`
4. `events.reset`

## Routing / UI Rename

### Backend routes

Current `/api/admin/config/event-agents/*` should evolve toward `/api/admin/config/events/*`.

Suggested mapping:

1. `/api/admin/config/events`
2. `/api/admin/config/events/runtime/start`
3. `/api/admin/config/events/runtime/stop`
4. `/api/admin/config/events/runtime/reset`
5. `/api/admin/config/events/dispatch`
6. `/api/admin/config/events/triggers`

### Frontend

Agents page should stop owning scheduler semantics.

Target UI split:

1. `Agents`
   - agent catalog
   - audit logs
   - one-off spawn / inspect

2. `Events`
   - event bus status
   - sources
   - trigger review
   - cron schedule review
   - event replay / reset / dispatch

## Dispatch Boundary

Normal agent invocation from events must route through one boundary only.

Target boundary:

1. `events.*` decides *that* an action should happen,
2. `actions.dispatch` decides *what kind* of action it is,
3. `agents.runner` performs normal Knoxx agent invocation.

That means no event subsystem code should build bespoke agent runtime payloads except through the shared runner.

## Migration Phases

### Phase 1 — Name the seam, no behavior change

1. create `agents.runner` and move shared normal agent invocation there,
2. make event-triggered agent runs call `agents.runner`,
3. keep old routes/tools as compatibility shims.

Definition of done:

1. event-triggered agent launches and chat launches both flow through the same runner,
2. `event_agents.cljs` no longer owns the canonical agent invocation path.

### Phase 2 — Extract generic events runtime

1. move event envelope + state into `events.bus` / `events.state`,
2. move cron ticker into `events.cron`,
3. move Discord event adaptation into `events.sources.discord`,
4. move matching into `events.triggers`.

Definition of done:

1. no agent-specific code remains in the generic event bus/ticker layer.

### Phase 3 — Reframe tools and routes

1. introduce `events.dispatch` and `agents.spawn`,
2. keep old `event_agents.*` tools as deprecated aliases initially,
3. add `/events` admin surface and move schedule review there.

Definition of done:

1. human/admin vocabulary is "events" and "agents", not "event agents".

### Phase 4 — Contract cleanup

1. migrate trigger-bearing pseudo-agent jobs into explicit trigger/action composition,
2. reduce `:trigger-kind` on regular agents to descriptive metadata, not scheduler ownership,
3. convert cron/event orchestration to event/trigger contracts.

Definition of done:

1. regular agents are regular agents,
2. scheduling and subscriptions are owned by events/triggers only.

## Compatibility Plan

Short-term compatibility shims are acceptable:

1. `knoxx.backend.discord-cron` → delegates to `events.cron`
2. `event_agents.dispatch` → delegates to `events.dispatch`
3. `event_agents.run_job` → delegates to either `events.dispatch` or `agents.spawn` depending on payload
4. `/event-agents` UI route → redirects to `/events`

## Risks

1. contract migration could break existing cron/event-driven Discord behaviors if done all at once,
2. circular dependencies can reappear if `events.*` imports `agents.*` directly instead of going through dispatch/runner boundaries,
3. admin surfaces may temporarily need dual labels while compatibility shims exist.

## Immediate Next Slice

Implement Phase 1 first:

1. introduce `agents.runner` as the only normal agent launch path,
2. make current event-triggered runs delegate to it,
3. leave the rest of the event scheduler intact for one pass.

This is the minimum slice that collapses the "two runtimes" problem before the broader events refactor.
