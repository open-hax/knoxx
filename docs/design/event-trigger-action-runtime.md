# Event, Trigger, Action, Actor, Agent Runtime

## Goal

Delete the legacy blended event/agent concept by replacing it with a resource-native runtime:

- Events are immutable declarations emitted by actors/generators while doing work.
- Generators produce events.
- Schedules are separate temporal agreements that emit synthetic events.
- Triggers subscribe to event types and decide whether an action should run.
- Actions are registered functions invoked against an actor context and optional event.
- Actors own identity, permissions, credentials, state, emitted events, received events, and allowed actions.
- Agents are executables that specify prompting, capabilities, roles, ownership, and policy; they are not generators, triggers, actions, or schedules.

## Resource Separation Constraints

- Do not normalize legacy mixed shapes into new runtime truth.
- Do not use a generic runtime `source` concept; use generator contracts for event producers and event provenance on emitted events.
- Agent resources do not define triggers, actions, schedules, generators, event filters, or source/source-mode fields.
- Trigger resources are agreements by actors to take an action after observing an event that meets a condition.
- Action resources define registered behavior and schemas.
- Schedule resources define temporal rules that emit synthetic events in the future.
- One actor may be responsible for many agent sessions.

## Domain Model

### Event

An event is a signal emitted during an action taken by an actor.

Required shape:

```clojure
{:event/id "evt_..."
 :event/type :discord.message/mention
 :event/actor "discord_automation"
 :event/generator {:kind :discord :message-id "..."}
 :event/payload {...}
 :event/timestamp "2026-05-18T...Z"}
```

Rules:

- `:event/type` replaces legacy `:eventKind` and `:eventKinds`.
- `:event/generator` is event provenance; do not reintroduce a runtime `source` concept.
- Event fan-out is handled by the dispatcher matching multiple trigger contracts, not by mutating a single event with multiple kinds.
- Event dedup lives in the event dispatcher, keyed by `:event/id`.

### Trigger

A trigger resource defines when an event may initiate work.

Required concepts:

- Triggering event types.
- Emitter actor contract capable of emitting those event types.
- Optional predicate that must pass.
- Handler action valid for the triggering event type.
- Optional agent contract applied to the listener actor context before invoking the action.
- Listener actor context capable of taking the initiated action.

Proposed shape:

```clojure
{:contract/kind :trigger
 :contract/id "ussyverse_social_replies_event"
 :enabled true
 :trigger/events [:discord.message/mention :discord.message/keyword]
 :trigger/emitter "discord_automation"
 :trigger/listener "discord_automation"
 :trigger/predicate {:predicate/kind :discord.message/filters
                     :channels ["1494137016303095828"]
                     :keywords ["frankie" "yap"]}
 :trigger/action :actions/start-agent-session
 :trigger/agent "ussyverse_social_replies"}
```

Rules:

- `:trigger/target` becomes transitional only; new triggers use `:trigger/action`.
- `:trigger/source` is invalid for new runtime truth.
- `:trigger/schedule`, `:trigger/kind :cron`, and `:trigger/kind :manual` are invalid for new runtime truth.
- `:data {:filters ...}` becomes `:trigger/predicate`.
- Triggers always respond to events. Schedules emit events; one scheduled event may fan out to many triggers.

### Action

An action is a function called on an actor context and optional event.

Runtime signature:

```clojure
(action actor-context event)
```

Action resources and trigger action refs resolve to entries in the action interpreter table. Resource discovery is handled by the separate registry protocol.

Required built-ins:

- `:actions/start-agent-session`
- `:actions/agent-send-steer-message`
- `:actions/agent-queue-follow-up-message`
- `:actions/stop-session`
- `:actions/noop`

Rules:

- Agent session lifecycle is represented as actions, not as event-agent runtime methods.
- `:actions/start-agent-session` accepts an actor context plus optional agent contract overlay.
- Legacy `:invoke/agent` maps to `:actions/start-agent-session` during migration, then disappears.

### Action Registry

A map/multimethod of functions registered under names usable from contracts.

Current seam:

- `knoxx.backend.domain.action.registry/run-action!`

Required change:

- Dispatch by action key/name, not `:action/kind` only.
- Accept actor-context-first shape.
- Keep implementation data-oriented: action maps in, result maps out.

### Actor

An actor is stable identity plus state.

Current actor responsibilities:

- Permissions.
- Credentials.
- Roles.
- Optional prompt augmentation.
- State.

New actor responsibilities:

- `:actor/actions` actions this actor may take.
- `:actor/events/emits` event types this actor may emit.
- `:actor/events/receives` event types this actor may receive.
- Discord bot tokens live on the `discord_automation` actor identity.

Rules:

- Actor permissions gate both direct user actions and triggered actions.
- Trigger listener actor must be allowed to receive the event and take the action.
- Trigger emitter actor must be allowed to emit the event.

### Agent

An agent is a prompt object plus an actor context constraint.

Agent responsibilities:

- Prompt object.
- Actor context the agent is valid for.
- Additional actions valid while this agent context is active.
- Additional emitted/received events valid while this agent context is active.
- Model/thinking/context/tool policy as agent session settings.

Rules:

- Agent resources do not carry source, trigger, or event trigger fields.
- The initiating event supplies generator/provenance context to the action.
- If an agent contract is available in an actor context, agent lifecycle actions are available to that context.

## Migration Plan

### Phase 1: Define New Runtime Surface

- Add event normalization namespace.
- Add trigger contract normalization namespace.
- Add action registry names for `:actions/*` lifecycle actions.
- Add actor authorization helpers for event emit/receive/action checks.
- Keep compatibility reads for existing contracts, but convert them into the new shape immediately at load time.

Verification:

- Unit tests for event normalization, trigger normalization, predicate matching, and action key resolution.
- `pnpm -C backend exec shadow-cljs compile test` once the missing JS test stub is restored.

### Phase 2: Move Dispatch Out Of `event_agents.cljs`

- Move event matching into `knoxx.backend.domain.event.dispatch`.
- Move job/run state into `knoxx.backend.domain.trigger.state` if still needed.
- Move Discord event emission into `knoxx.backend.domain.discord.events`.
- Move Discord synthesis source gathering into a Discord-specific action or event context builder.

Verification:

- `pnpm -C backend exec shadow-cljs compile server` after each moved slice.

### Phase 3: Replace Event-Agent Control

- Stop synthesizing `:jobs` as the runtime truth in `triggers/control_config.cljs`.
- Load trigger resources directly as trigger records.
- Convert admin Events UI responses from job-centric snapshots to resource-centric snapshots.
- Preserve endpoints only if they expose the new runtime vocabulary; no thin `event-agent` modules.

Verification:

- Server compile.
- Manual dispatch route emits an event; matching trigger contracts invoke actions through the same event path.

### Phase 4: Delete Legacy File

- Remove all requires of `knoxx.backend.event-agents`.
- Delete `backend/src/cljs/knoxx/backend/event_agents.cljs`.
- Remove legacy event-agent tool ids or mark them as explicit compatibility commands that call action/trigger APIs.
- Update `contracts/AGENTS.md` so new contracts do not mention event-agent authoring.

Verification:

- `pnpm -C backend build`.
- `pnpm -C backend exec shadow-cljs compile test`.
- Search confirms no `knoxx.backend.event-agents` namespace and no runtime dependency on `event-agent-control`.
