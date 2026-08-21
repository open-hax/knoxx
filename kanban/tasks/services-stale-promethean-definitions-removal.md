---
uuid: services-stale-promethean-definitions-removal
title: Services — remove definitions that describe nothing
status: ready
priority: P2
points: 1
labels:
  - tasks
  - deployment
  - hygiene
  - has-parent
---

# Services — remove definitions that describe nothing

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`promethean/services.yaml` defines `proxx-production` and `knoxx-production` on
the old host, and the DigitalOcean lane already deploys both. Those entries are
stale definitions, not second deployments. A file whose job is to say where
services run, saying it wrongly for the two most important ones, is worse than a
file that omits them.

Smallest card in the inventory and worth doing early: it removes the ambiguity
that makes every other migration card harder to read.

## Work

- Confirm before deleting. For each entry, check that the DigitalOcean lane is
  what actually serves its hostname — the deploy chain and the live record, not
  the file's own claim. A definition that turns out to be live is a migration,
  not a deletion.
- Remove the two stale service entries and any `routes` entries that point at
  them.
- Do the same for the `updated:` field at the top of the file, which reads
  `"2026-06-01"` and has been wrong for the entire period this drift accumulated.
- Leave `nginx-ingress`, the staging entries, axxium, openplanner and the bridge
  alone. Those describe things that are still running and are removed by their
  own cards.
- Add a header note stating that this file is the legacy lane, that it accepts no
  new services, and where the model lives. A reader who arrives here first should
  not have to infer that from an absence.

## Definition of Done

- Each removed entry was verified as stale against live state, not just against
  another file.
- `proxx-production` and `knoxx-production` and their routes are gone.
- The file carries a legacy notice and an accurate `updated:` date.
- Nothing that still runs was removed.
