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

The edn files themselves are not really contracts, but they must satisfy contracts.


Focusing on one part of it at a time... creates drift from one to the other.


## Actor
A persistent entity with a stable identity
and internal state capable of taking actions in response to events
to effect changes in the state of the world

- policy
- identity
- catalog
- state
- inbox
- outbox

## Registry
A dictionary mapping a keyword to resource of a given type used to advertise
the availablity of a resource to actors while enacting a policy

### Knoxx Registries
- actions
- rules
- triggers
- actors
- users
- agents
- capabilities
- roles
- workflows
- pipelines
- catalogs

## Catalog

A map of of vectors of keywords of registered resources suitable for a specific intent
## Action

A registered behavior
an input/output schema

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

## Schedule
A contract mapping a temporal shape (a rule) to the spontaneous generation of a synthetic Event.
Implements the generator shape



## Trigger

An agreement to take an action by an actor in response to an event that meets a condition
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
