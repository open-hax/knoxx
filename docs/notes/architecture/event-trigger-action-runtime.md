# Event, Trigger, Action, Actor, Agent Runtime

## Goal

Delete `knoxx.backend.event-agents` by replacing the event-agent concept with a contract-native runtime:

- Events are normalized signals emitted by actors while doing work.
- Triggers subscribe to event types and decide whether an action should run.
- Actions are registered functions invoked against an actor context and optional event.
- Actors own identity, permissions, credentials, state, emitted events, received events, and allowed actions.
- Agents are prompt/context overlays on actor identities, not event sources or schedulers.

## Domain Model

### Event

An event is a signal emitted during an action taken by an actor.

Required shape:

```clojure
{:event/id "evt_..."
 :event/type :discord.message/mention
 :event/actor "discord_automation"
 :event/source {:kind :discord :message-id "..."}
 :event/payload {...}
 :event/timestamp "2026-05-18T...Z"}
```

Rules:

- `:event/type` replaces legacy `:eventKind` and `:eventKinds`.
- `:event/source` is event provenance, not a job source field.
- Event fan-out is handled by the dispatcher matching multiple trigger contracts, not by mutating a single event with multiple kinds.
- Event dedup lives in the event dispatcher, keyed by `:event/id`.

### Trigger

A trigger contract defines when an event may initiate work.

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
- `:trigger/source` becomes transitional only; the event is the source.
- `:data {:filters ...}` becomes `:trigger/predicate`.
- Cron triggers emit a cron event and then use the same trigger/action path.

### Action

An action is a function called on an actor context and optional event.

Runtime signature:

```clojure
(action actor-context event)
```

Action contracts and trigger action refs resolve to entries in the action registry.

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

- Agent contracts do not carry source, trigger, or event trigger fields.
- The initiating event supplies source context to the action.
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
- Load trigger contracts directly as trigger records.
- Convert admin Events UI responses from job-centric snapshots to trigger-centric snapshots.
- Preserve endpoints only if they expose the new runtime vocabulary; no thin `event-agent` modules.

Verification:

- Server compile.
- Manual dispatch route fires a trigger and invokes `:actions/start-agent-session`.

### Phase 4: Delete Legacy File

- Remove all requires of `knoxx.backend.event-agents`.
- Delete `backend/src/cljs/knoxx/backend/event_agents.cljs`.
- Remove legacy event-agent tool ids or mark them as explicit compatibility commands that call action/trigger APIs.
- Update `contracts/AGENTS.md` so new contracts do not mention event-agent authoring.

Verification:

- `pnpm -C backend build`.
- `pnpm -C backend exec shadow-cljs compile test`.
- Search confirms no `knoxx.backend.event-agents` namespace and no runtime dependency on `event-agent-control`.
