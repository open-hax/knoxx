---
uuid: "knoxx-events-agent-runtime-separation"
title: "Events, Triggers, Actions, Schedules, and Agents Runtime Separation"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-06T00:00:00Z"
source: "specs/epics/events-agent-runtime-separation.md"
points: null
category: "epics"
---

# Events, Triggers, Actions, Schedules, and Agents Runtime Separation

> Source: `specs/epics/events-agent-runtime-separation.md`

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

---
Triage 2026-05-29: Clear architectural goal — separate event dispatch, trigger/action/schedule/generator resources, and agent execution into distinct concerns with no blended runtime class. The task body is incomplete (Problem section is empty), but the goal is unambiguous and strongly supported by the existing codebase: `infra/event-runtime.cljs` is already a composition shell delegating to `domain/trigger/runtime`, `domain/schedule/runtime`, and `domain/source/runtime`; `infra/trigger-runner.cljs` is an explicit deprecated facade. The separation is in progress but not complete — no external blockers, directly actionable in the knoxx backend. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban

Breakdown 2026-05-29: Code inspection confirms separation is partially complete but four concrete gaps remain across multiple subsystems.

DONE: infra/event_runtime.cljs (80 lines) is a clean composition shell starting domain/trigger/runtime, domain/schedule/runtime, and domain/source/runtime with no blending. domain/control/catalog.cljs (348 lines) enforces the conceptual separation via a violation framework. infra/trigger_runner.cljs is already a pure delegation facade with a deprecation notice and no logic.

GAPS FOUND:
(1) domain/generator/runtime.cljs does not exist — generator resources are defined in law/contracts.cljs and open_hax/contracts/schema.cljs (schema: :generator/kind, :generator/driver, :generator/emits) and validated in domain/control/catalog.cljs (generator-violations), but no runtime starts them. The event-runtime lifecycle never calls a generator start!.
(2) domain/action/start_agent_session.cljs line 5 imports knoxx.backend.infra.agent.runner directly — a domain-to-infra boundary crossing. The action domain should receive a spawn! fn via context, not import infra directly.
(3) infra/trigger_runner.cljs deprecated facade still exists (39 lines). Only one file references it at all (discord_io.cljs docstring, not a require). Facade can be deleted once confirmed safe.
(4) domain/discord/discord_io.cljs docstring line 2 still names trigger-runner as a consumer — stale after the facade became a pure delegation shim.

Total scope: 13sp. Four independent streams, each bounded. Original epic → icebox. Split into 4 subtasks. --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
