---
original_name: "2026.05.20.16.22.49.md"
title: "Action Registry Intent"
summary: "Intent note for formalizing an action registry shared by APIs, tools, and websockets."
category: "architecture"
created: "2026-05-20"
---

# Intent

formalize an action registry system, and that is how we describe our exposed behaviors through the api, tools, websockets.
Tools become wrappers around actions
our /api become a wrapper around actions.

We can define actions throught a  data as interpreter pattern.

We called them contracts, for lack of a better word initally.
but I may have overloaded that a bit.

The EDN files themselves are not contracts. They are resource definitions. They must satisfy contracts at the boundary: Malli schemas, admissibility checks, and policy rules.

Focusing on one part of it at a time creates drift from one to the other, so registry, resource, schedule, trigger, and action language must be kept distinct.

## Registry

A registry is a dictionary mapping a keyword/id to a resource of one kind. It advertises resource availability to actors while enacting policy.

A registry is distinct from an action. An action is executable behavior; a registry is an index/catalog protocol for discovering resources.

### Knoxx Resource Registries
- actions
- rules
- triggers
- actors
- users
- agent
- capabilities
- roles
- workflows
- pipelines
- schedules

### Registry Protocol

All resource registries implement the same small protocol:

```clojure
(registry-id registry)                         ;; => :registry/actions
(registry-resource-kind registry)              ;; => :action
(registered-resource-ids registry config)      ;; => ["spawn_session" ...]
(registry-resource registry config resource-id) ;; => resource record | nil
(registry-catalog registry config)             ;; => {:catalog/resources {:action [...]}}
```

Initial implementation namespace:

```clojure
knoxx.backend.domain.registry.resource
```

Resource definitions are loaded through the resource facade:

```clojure
knoxx.backend.domain.resources.loader
```

The older `domain.contracts.loader` namespace remains only as a compatibility/parser layer until callers move to resource language.


## Catalog

A map of of vectors of keywords of registered resources suitable for a specific intent.
A data type used to declare required or available resources 

```edn
{:catalog/resources
 {:agent-spec [:agent-spec/ussyverse-creative-social
               :agent-spec/system-admin]
  :service [:service/eta-mu]
  :action [:action/spawn-session
           :action/get-session
           :action/pause-session]
  :event [:event/session-started
          :event/session-idle
          :event/session-error
          :event/session-paused]
  :actor [:actor/system-admin
          :actor/discord-connected]}}
```

Catalogs may also be embedded inside resource definitions to describe required or available resources for a specific intent:

```edn
{:some-resource-type/id :foobar
 :some-resource-type/need-stuff
 {:agent-spec [:agent-spec/ussyverse-creative-social
               :agent-spec/system-admin]
  :service [:service/eta-mu]
  :action [:action/spawn-session
           :action/get-session
           :action/pause-session]
  :event [:event/session-started
          :event/session-idle
          :event/session-error
          :event/session-paused]
  :actor [:actor/system-admin
          :actor/discord-connected]}}
```



## Resource

An identity, the registry that owns it, and a common shape of data. Resource definitions may be sparse components over shared IDs, but they are still plain EDN data.

## Actor
A persistent entity with a stable identity
and internal state capable of taking actions in response to events
to effect changes in the state of the world



## Action

A registered behavior
an input/output schema

Action resource definitions are not subscriptions. They advertise behavior that
the action interpreter table can execute. A trigger may request an action; a
schedule may cause an event that a trigger hears; the action resource itself does
not watch time or events.

Example:

```edn
{:contract/kind :action
 :contract/id "hello-world"
 :action/id :actions/hello-world
 :action/kind :actions/hello-world
 :action/handler "knoxx.backend.domain.action.registry/run-action!"
 :action/responds-to [:message/greeting]
 :action/result :message/send.expectation}
```

## Event

An immutable declaration that a state transition has occurred
at a specific coordinate in space-time.

## Message

The fundamental unit of communication between actors
Has a sender, and recipient

```clojure
;; A Message is an epistemic envelope. It makes no claims about the truth
;; of its contents, only about the legitimacy of its transport.
(def MessageSchema
  [:map
   [:id :uuid]
   [:sender :keyword]
   [:recipient :keyword]
   [:payload :any]
   [:meta [:map-of :keyword :any]]])
```

## Policy


## Permission

## Generator

Abstractly an entity with a policy that produces events

Generators are provenance resources. They identify event-producing adapters,
processes, actors, or fixtures. They do not contain action execution policy.

## Schedule

A resource mapping a temporal rule to the generation of a synthetic event. A schedule does not implement a trigger path and does not call actions. It emits an event, and any number of triggers may respond.



## Trigger

An agreement to take an action by an actor in response to an event that meets a condition. Triggers always respond to events; they do not contain schedules.

Example:

```edn
{:contract/kind :trigger
 :contract/id "hello_world_inbox"
 :trigger/kind :event
 :trigger/listener "system_admin"
 :trigger/events [:message/greeting]
 :trigger/action :actions/hello-world}
```
## User

An actor identity representing a human user


## Driver

A recursive loop that examines a process and decides what action
to take next and if it is allowed by policy

## Agent

A driver/runner

## Tool

An action bundled  with text instructions explaining the tools use.


## Capability

A catalog of tools suitable for a specific intent with a description
## Role


## Route

## Process

A sovereign boundry of state over time



## Workflow

A map of states
