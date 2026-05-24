# Event, Trigger, Action, Actor, Agent Runtime

## Goal

Delete the legacy blended event/agent concept by replacing it with a resource-native runtime:

- Events are immutable declarations emitted by actors/generators while doing work.
- Generators produce events.
- Source resources are actor-owned instances of code drivers for systems whose event logic lives in code, such as Discord and eta-mu.
- Schedules are separate temporal agreements that emit synthetic events.
- Triggers subscribe to event types and decide whether an action should run.
- Actions are registered functions invoked against an actor context and optional event.
- Actors own identity, permissions, credentials, state, emitted events, received events, and allowed actions.
- Agents are executables that specify prompting, capabilities, roles, ownership, and policy; they are not generators, triggers, actions, or schedules.

## Resource Separation Constraints

- Do not normalize legacy mixed shapes into new runtime truth.
- Do not put source/source-mode fields on agents or triggers.
- Use source resources for driver-backed event generators whose logic lives in code; source resources declare actor, driver, and `:source/listens`, while driver code owns emitted event specs.
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
- `:event/generator` is event provenance and may point at a source resource when an external driver emitted the event.
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
- A trigger is not an action. It is an actor/listener agreement to request an action after observing a matching event.

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

- Agent session lifecycle is represented as actions, not as event runtime methods.
- `:actions/start-agent-session` accepts an actor context plus optional agent contract overlay.
- Legacy `:invoke/agent` maps to `:actions/start-agent-session` during migration, then disappears.
- An action resource advertises registered behavior and schemas. The action interpreter table is executable code; the EDN resource is discoverable data.

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

### Generator

A generator resource declares event provenance: a process, adapter, actor, or demo fixture that can emit events.

Example shape:

```clojure
{:contract/kind :generator
 :contract/id "hello_world_demo"
 :generator/kind :demo
 :generator/actor "system_admin"
 :generator/emits [:message/greeting]}
```

Rules:

- Generators produce events; they do not subscribe to events.
- Generators do not invoke actions.
- `:event/generator` on emitted events should identify the generator/provenance.

### Driver

A driver is ClojureScript code, not an EDN resource. It knows how to normalize one system's native signals into Knoxx events and how to bind one source instance.

Driver protocol shape:

```clojure
(driver-id driver)              ;; => :driver/discord
(driver-kind driver)            ;; => :discord
(driver-event-specs driver)     ;; code-owned event names and shapes
(start-source! driver context)  ;; context includes :source and :dispatch!
```

Rules:

- Drivers own emitted event names and shapes.
- Drivers are registered by code (`knoxx.backend.domain.driver.builtin` for built-ins), not loaded from `contracts/`.
- A driver does not decide which actor/account is active; a source resource does.
- A driver emits an event by calling the source runtime dispatch function, which attaches source actor/provenance and sends the event to the generic dispatcher.

### Source

A source resource is an actor-owned instance of a code driver. It exists because a system can have many credentialed actors/accounts, e.g. many Discord bots using the same Discord driver.

Example shape:

```clojure
{:contract/kind :source
 :contract/id "discord_gateway"
 :source/id :source/discord-gateway
 :source/type :event-generator
 :source/driver :driver/discord
 :source/actor "discord_automation"
 :source/listens [:discord.message
                  :discord.message.mention]}
```

Event path:

```clojure
;; Driver callback for the discord_automation actor:
(dispatch-driver-event! config
                        :driver/discord
                        "discord_automation"
                        {:event/type :discord.message
                         :event/payload {...}})

;; Source runtime checks that an enabled source exists:
{:source/driver :driver/discord
 :source/actor "discord_automation"
 :source/listens [:discord.message]}

;; Then it dispatches:
{:event/type :discord.message
 :event/actor "discord_automation"
 :event/generator {:kind :discord
                   :driver :driver/discord
                   :source :source/discord-gateway}
 :event/payload {...}}
```

Rules:

- Sources are resources; drivers are code.
- Source resources declare `:source/driver`, `:source/actor`, and `:source/listens`.
- Source resources do not declare `:source/emits` or event shapes; the driver implementation owns those.
- `:source/listens` is the admission filter: the source only dispatches driver events it selected.
- Triggers do not bind to source implementations. Triggers observe event types and predicates.
- Context-only sources may exist, but event-generating sources use `:source/type :event-generator` and `:source/listens`.

### Schedule

A schedule resource declares a temporal rule for producing a synthetic event.

Example shape:

```clojure
{:contract/kind :schedule
 :contract/id "hello_world_morning_tick"
 :schedule/rule "*/30 * * * *"
 :schedule/generator "hello_world_demo"
 :schedule/event {:event/type :message/greeting
                  :event/payload {:name "world"}}}
```

Rules:

- Schedules emit events through the event dispatcher.
- Schedules do not contain `:trigger/action`, `:trigger/agent`, or trigger predicates.
- A scheduled event may match zero, one, or many triggers.

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

### Concrete Resource Set: hello-world

The hello-world demo is intentionally split across four resources:

- `contracts/actions/hello-world.edn` — advertises the executable behavior `:actions/hello-world` and its `:message/greeting` input event.
- `contracts/generators/hello_world_demo.edn` — declares demo/manual/schedule provenance for `:message/greeting` events.
- `contracts/triggers/hello_world_inbox.edn` — lets `system_admin` request `:actions/hello-world` after observing `:message/greeting`.
- `contracts/schedules/hello_world_morning_tick.edn` — disabled-by-default temporal resource that emits a synthetic `:message/greeting` event.

This is the reference split:

```text
generator/schedule -> event -> trigger -> action
```

Do not put trigger maps, schedule maps, or generator maps inside the action file. Do not put action execution inside the schedule file.

### Phase 1: Define New Runtime Surface

- Add event normalization namespace.
- Add trigger contract normalization namespace.
- Add action registry names for `:actions/*` lifecycle actions.
- Add actor authorization helpers for event emit/receive/action checks.
- Keep compatibility reads for existing contracts, but convert them into the new shape immediately at load time.

Verification:

- Unit tests for event normalization, trigger normalization, predicate matching, and action key resolution.
- `pnpm -C backend exec shadow-cljs compile test` once the missing JS test stub is restored.

### Phase 2: Move Dispatch Out Of `legacy blended runtime namespace`

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
- Preserve endpoints only if they expose the new runtime vocabulary; no thin `event runtime` modules.

Verification:

- Server compile.
- Manual dispatch route emits an event; matching trigger contracts invoke actions through the same event path.

### Phase 4: Delete Legacy File

- Remove all requires of `knoxx.backend.event runtimes`.
- Delete `backend/src/cljs/knoxx/backend/legacy blended runtime namespace`.
- Remove legacy event runtime tool ids or mark them as explicit compatibility commands that call action/trigger APIs.
- Update `contracts/AGENTS.md` so new contracts do not mention event runtime authoring.

Verification:

- `pnpm -C backend build`.
- `pnpm -C backend exec shadow-cljs compile test`.
- Search confirms no `knoxx.backend.event runtimes` namespace and no runtime dependency on `event runtime-control`.
