---
uuid: knoxx-http-event-runtime-lifecycle-separation
title: HTTP / Event Runtime Lifecycle Separation
status: ready
priority: P1
points: 5
labels:
  - tasks
  - publication
  - events
  - has-parent
---

# HTTP / Event Runtime Lifecycle Separation

> Parent epic: `knoxx-publication-runtime-follow-up`

## Purpose

Make backend HTTP startup independently usable for verification without connecting gateways, schedules, triggers, generators, or dispatching agent/event work.

## Work

- Identify the current boot seam where HTTP startup implicitly starts event runtimes.
- Split HTTP server lifecycle from event-runtime lifecycle with an explicit composition point.
- Define and test `start`, `stop`, `reset`, and `status` semantics, including repeated calls and disabled mode.
- Preserve fail-closed behavior: disabled/failed lifecycle mutations cannot return false success.
- Add an HTTP-only verification boot mode with zero Discord/gateway/schedule/trigger/generator/agent effects.
- Once authoritative, retire or narrow `KNOXX_DISABLE_EVENT_RUNTIMES` so there is one lifecycle model rather than two competing controls.

## Definition of Done

- HTTP server can start and serve required routes without starting event runtimes.
- Tests prove HTTP-only startup produces zero event effects.
- Lifecycle operations have explicit tested idempotency/error semantics.
- Disabled `start`/`reset` remain truthful failures rather than no-op success.
- Existing full-runtime startup still starts intended event runtimes exactly once.
- Backend test/lint/typecheck gates pass.
