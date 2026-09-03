---
uuid: "knoxx-event-runtime-boot-coupling"
title: "Decouple event runtimes from HTTP boot so Knoxx can run locally without live side effects"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "events", "discord", "runtime", "verification"]
created_at: "2026-08-13T00:00:00Z"
points: 5
category: tasks
epic: "knoxx-events-agent-runtime-separation"
---

# Decouple event runtimes from HTTP boot so Knoxx can run locally without live side effects

> Parent epic: `knoxx-events-agent-runtime-separation`

## Why this exists

Starting a Knoxx backend to serve HTTP also, unconditionally:

- arms every schedule in the contract tree,
- registers every trigger,
- reads `discord_bot` credentials out of the shared policy DB and **connects
  live Discord gateways for every actor that has one**.

There is no way to boot the HTTP surface without that. `start-background-services!`
in `backend/src/cljs/knoxx/backend/infra/core.cljs` runs `event-runtime/start!`
and `discord-source/bind-gateways!` as part of the same sequence that brings up
the app.

This is not theoretical. On **2026-08-13**, a local backend started purely to
run `scripts/verify-publication-epic.sh` came up as:

```
[discord-gateway] ready as OpenHax#8539 in 3 guilds
[schedule-domain] armed fork-tales/creative-director every 1800000 ms
[schedule-domain] armed ussyverse-social/creative every 1800000 ms
```

and, before it was shut down, consumed a real Discord message and dispatched a
real agent turn:

```
[prompt-and-await!] User request: Event: discord.message
Trigger: ussyverse/social-replies ... Channel ID: 1515955913792688
```

A developer verifying a CMS change caused a bot to answer someone in a real
guild. The two are unrelated concerns that share a boot path.

## The stopgap already in place

`KNOXX_DISABLE_EVENT_RUNTIMES` (added alongside this card) skips
`event-runtime/start!` and `bind-gateways!`, prints a boxed banner at boot, and
nags every 60s so a process cannot sit in that mode unnoticed.

**It is a mitigation, not the fix.** It is one global on/off switch bolted onto
the existing coupling. It cannot express "schedules yes, Discord no", it does
not stop a *second* process from binding the same bot, and its correctness
depends on a developer remembering to set it.

## Outcome

Bringing up the HTTP surface does not start event runtimes. Event runtimes are
started explicitly, by something that means to start them, and can be started
independently of each other.

## Scope

- Separate HTTP serving from event-runtime startup so neither implies the other.
- Make schedules, triggers, and Discord gateways independently startable rather
  than one all-or-nothing block.
- Decide and document the ownership rule: which process is allowed to hold a
  Discord actor gateway. Today any process with policy-DB credentials will take
  one, so two backends against the same Mongo both connect the same bot.
- Give the local-development path a supported, discoverable way to run the HTTP
  surface with no outbound event effects — not an env var a developer has to
  know about.
- Retire `KNOXX_DISABLE_EVENT_RUNTIMES`, or demote it to an alias of whatever
  the real mechanism becomes.

## Non-goals

- Changing what any individual trigger, schedule, or Discord handler *does*.
- Reworking the agent runtime itself; that is the parent epic's business.
- Multi-host gateway leasing or failover. Naming the ownership rule is in
  scope; building a distributed lease is not.

## Acceptance criteria

- A backend can serve `/api/**` with **zero** outbound event effects: no
  gateway connection, no armed schedule, no registered trigger — and this is
  the default for local development rather than an opt-in.
- Schedules, triggers, and Discord gateways can each be started without the
  other two.
- Starting a second backend against the same policy DB does not silently
  connect a second gateway for an actor that already has one; the behavior is
  defined and logged either way.
- `scripts/verify-publication-epic.sh` and `scripts/verify-publication-tour.sh`
  can be run against a local backend without any Discord connection, without
  setting an env var by hand.
- A test asserts that HTTP startup alone binds no gateway and arms no schedule.

## Verification

- Boot the backend in the local-development configuration; assert the log
  contains no `[discord-gateway] ready`, no `[schedule-domain] armed`, and no
  `[trigger-domain] registered`.
- Boot it in full-runtime configuration; assert all three appear.
- Run both verification scripts end to end against the local configuration and
  confirm they pass with no gateway in the log.
- With two backends pointed at one policy DB, confirm the defined
  single-owner behavior and that it is logged.

## Notes

Found while running the human verification artifact required by
**AGENTS.md → Human Verification Artifact**. That requirement is currently in
tension with this coupling: the guide asks every agent to run the app locally,
and running the app locally has live consequences. This card closes that gap.

Related: `knoxx-contract-owned-publication-pipeline` (the epic whose
verification surfaced it).
