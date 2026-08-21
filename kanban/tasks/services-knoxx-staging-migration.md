---
uuid: services-knoxx-staging-migration
title: Services — knoxx staging slot on DigitalOcean
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - migration
  - knoxx
  - has-parent
---

# Services — knoxx staging slot on DigitalOcean

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`knoxx-staging` exists only on the Promethean host. Knoxx production already
deploys through the DigitalOcean lane, so this is the phase that is missing —
and it is the one the promotion rule depends on. Without it, every production
deploy of the busiest service in the constellation ships code that has run
nowhere but CI.

## Dependencies

`services-staging-slot-pattern`. Also interacts with `knoxx`'s own
`deploy-staging.yml` and the label-gated `deploy-testing.yml`, which currently
target the shared Promethean staging slot.

## Work

- Apply the staging-slot pattern to `digitalocean/services/knoxx/`: phase-derived
  compose project, state path, ports, container names and `staging-` hostname.
- Knoxx is two containers plus a health dependency — the backend's `/health`
  returns 503 until Proxx is reachable and its healthcheck gates the frontend. A
  staging Knoxx therefore needs a Proxx to talk to. Decide whether it points at
  staging Proxx or production Proxx, and say why; pointing at production means
  staging traffic reaches production inference.
- Settle the data boundary. Knoxx runs the OpenPlanner data plane in-process
  against Atlas; staging must not write into production collections. This is the
  database-posture question from the pattern card, and Knoxx is where it bites.
- Repoint knoxx's own `deploy-staging.yml` at the new slot, and decide what
  `deploy-testing.yml`'s label-gated PR deploys do — it currently shares the
  staging slot by design and says so ("the slot is shared — a real staging merge
  deploy will overwrite it"). That trade may or may not survive the move.
- `verify.sh` runs unchanged against staging. The gate is phase-agnostic; if it
  is not, that is a defect in the gate.

## Definition of Done

- Knoxx staging deploys to DigitalOcean and its gate passes.
- Its inference and data dependencies are declared, with staging isolated from
  production data.
- `deploy-staging.yml` targets the new slot, and the testing slot's behavior is
  decided.
- A staging deployment record exists for the promotion check to read.
