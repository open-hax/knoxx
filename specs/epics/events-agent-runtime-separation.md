# Events, Triggers, Actions, Schedules, and Agents Runtime Separation

Date: 2026-05-06
Status: in-progress
Repo: `packages/agents/knoxx`

## Goal

Separate event reaction from agent execution so Knoxx has:

1. one agent runtime,
2. one event dispatch path,
3. explicit trigger/action/schedule/generator resources,
4. no blended runtime class that pretends scheduling, subscription, action, and agent execution are one thing.

## Problem

The legacy blended runtime mixed at least five concerns:

1. event bus behavior,
2. cron/ticker behavior,
3. adapter reads such as Discord,
4. trigger matching and schedule state,
5. agent dispatch.

That created an architectural split-brain: normal chat agents used the Knoxx agent runtime, while scheduled/reactive work used a second orchestration surface.

## Target Model

### Events are generic infrastructure

The event subsystem owns:

1. normalized event envelopes,
2. append/dispatch semantics,
3. generator provenance,
4. subscriptions through trigger matching,
5. schedule emission state.

### Agents are just agents

Agents do not carry triggers, schedules, generators, or adapter/source fields.

Instead:

1. generators and schedules emit normalized events,
2. trigger resources decide whether an action should happen,
3. action resources dispatch behavior,
4. agent-session actions launch normal agent runs through the shared Knoxx runtime.

### Triggering is separate from execution

A trigger resource only:

1. observes event types,
2. checks a condition/predicate,
3. requests an action as an actor/listener agreement.

It does not own agent orchestration or schedule timing.

## Current Resource Split

Required runtime resources:

1. `:generator` — event provenance / producer declaration,
2. `:schedule` — temporal rule that emits a synthetic event,
3. `:trigger` — actor agreement to act after observing a matching event,
4. `:action` — registered executable behavior,
5. `:agent` — executable prompt/model/capability spec.

Reference path:

```text
generator/schedule -> event -> trigger -> action -> optional agent run
```

## Tool Surface

Use current primitives:

1. `events.status`
2. `events.dispatch`
3. `triggers.fire`
4. `agents.spawn`
5. `actors.send-message`

Do not add job-centric trigger tools. Creating or changing behavior should mean editing resources or dispatching events.

## Routing / UI

Target admin surfaces:

1. `/api/admin/resources` for EDN resource CRUD,
2. `/api/admin/config/events` for runtime status/control,
3. `/api/admin/config/events/dispatch` for manual event dispatch,
4. `/api/admin/config/events/triggers/:triggerId/fire` for trigger exercise.

Frontend split:

1. `Agents` — agent catalog, audit logs, one-off spawn/inspect,
2. `Events` — event runtime status, generator/schedule/trigger review, event replay/dispatch.

## Migration Status

Completed slices:

1. `hello-world` split into action, trigger, schedule, and generator resources.
2. Cron trigger resources split into schedules that emit events plus triggers that react.
3. Runtime capabilities moved to `:cap/event-runtime` and `:cap/schedule-runtime`.
4. Old job-centric tool ids removed from public capability grants.
5. Resource docs and schemas now treat EDN files as resources guarded by contracts, not contract objects.

Remaining slices:

1. Migrate frontend/admin clients from legacy route names to resources/events routes.
2. Remove compatibility route aliases after clients migrate.
3. Continue deleting legacy source/source-mode fields from agent resources as dedicated generator resources take over provenance.
4. Replace generated prompt instructions that mention job payloads with `agents.spawn` and resource-edit instructions.

## Invariants

1. Schedules emit events; they never call actions directly.
2. Triggers hear events; they never contain schedule rules.
3. Actions execute behavior; they do not subscribe or schedule themselves.
4. Generators provide provenance; they do not decide reactions.
5. Agents are executable prompt/model/capability specs only.
