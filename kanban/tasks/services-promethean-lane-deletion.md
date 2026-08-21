---
uuid: services-promethean-lane-deletion
title: Services — delete the Promethean lane
status: ready
priority: P2
points: 2
labels:
  - tasks
  - deployment
  - decommission
  - has-parent
---

# Services — delete the Promethean lane

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

The last card. Until the second lane is gone from the repository it remains a
choice, and the model's rule that every service is declared for the DigitalOcean
lane is enforced by nothing but a sentence in a document.

## Dependencies

Every other child of `services-promethean-lane-retirement`.

## Work

- Confirm the lane is empty first. `promethean/services.yaml` must define nothing
  that still runs, and no hostname may resolve to the old host. If an entry
  remains, it gets a written reason and a date — not a deletion.
- Remove `deploy-promethean.yml`, `promethean/scripts/*`,
  `promethean/nginx/promethean.conf`, `promethean/env/*.example`, and
  `promethean/services.yaml`. Preserve `promethean/docs/promotion-flow.md` and
  `drift-notes.md` if they carry reasoning the new model does not — move it
  rather than losing it.
- Check for references before deleting, not after: other repositories' workflows
  may still call this one. Knoxx's `deploy-staging.yml` and `deploy-testing.yml`
  both reach into the Promethean deploy module today, and
  `services-knoxx-staging-migration` is supposed to have repointed them. Verify
  rather than assume.
- Update `ROADMAP.md`. Its §2 and §3 describe the two-lane world, and §3 in
  particular narrates the deploy-authorization change as a contrast between them.
  Rewrite for one lane rather than deleting the reasoning — *why* deployment
  authorization derives from an immutable merge payload is worth keeping.
- Update `README.md`'s boundary section and `docs/deployment-model.md` §1, which
  currently exists to explain the difference between two lanes. It becomes a
  description of one.
- Decide the old droplet's fate explicitly. A machine that serves nothing and is
  still running is an expense and an attack surface, and leaving it up "just in
  case" without a date is how it stays up for a year.

## Definition of Done

- `promethean/` and `deploy-promethean.yml` are gone.
- No workflow in any repository references the deleted module.
- Reasoning worth keeping was moved, not deleted.
- `ROADMAP.md`, `README.md` and the deployment model describe one lane.
- The old host has a recorded disposition and a date.
