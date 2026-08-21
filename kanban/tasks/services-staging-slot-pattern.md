---
uuid: services-staging-slot-pattern
title: Services — what a staging slot is on a single host
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - lifecycle
  - has-parent
---

# Services — what a staging slot is on a single host

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

Staging on Promethean was a second set of compose projects on the same machine —
`proxx-staging`, `knoxx-staging`, `openplanner-staging`, `axxium-staging`, each
with its own runtime root, project name, published port and `staging-` hostname.
That shape is worth carrying across rather than reinventing per service, and it
must exist before the promotion rule can be enforced: rule 3 requires a staging
deployment record to compare production against, and `services#44` measured
`main` at seventy commits ahead of the last thing staging ever saw.

Establish the pattern once; `services-knoxx-staging-migration` and
`services-proxx-staging-migration` then apply it.

## Dependencies

`services-caddy-hostname-scale-decision` — a staging slot per service roughly
doubles the hostname count, which is most of why that decision needs re-making.

## Work

- Define the slot in the service descriptor rather than as a parallel directory:
  one service definition, a phase parameter, and everything phase-varying derived
  from it — compose project name, state path under `stateRoot`, published port,
  and hostname. Two directories per service is two things to keep in sync.
- Name the collision hazards explicitly, because the old lane hit them: bare
  service aliases collide across projects on a shared Docker network — the reason
  `promethean/services.yaml` records that federation nginx must route to
  project-specific container names. Every container name and network alias must
  carry the phase.
- Decide the database posture per phase and write it down. `services#44` lists
  this as the question that shapes everything else and could not decide it:
  ephemeral-per-phase is honest and the most work; shared makes phases interfere.
- Confirm the deployment record is produced. GitHub populates the Deployments API
  for any job declaring `environment:`, so the staging record the promotion rule
  reads already exists for free — verify that against a real staging deploy
  rather than assuming it.
- State what staging is *for* per service. A static site with no backend has
  little to stage; that is an argument for a cheap slot, not for skipping the
  record the promotion rule needs.

## Definition of Done

- A service declares a staging phase by parameter, not by a duplicate directory.
- Container names, aliases, ports, state paths and hostnames all carry the phase.
- The database posture per phase is recorded.
- A staging deploy produces a Deployments API record the promotion check can read.
