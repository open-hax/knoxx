---
uuid: services-axxium-digitalocean-migration
title: Services — migrate axxium to DigitalOcean
status: ready
priority: P1
points: 5
labels:
  - tasks
  - deployment
  - migration
  - axxium
  - has-parent
---

# Services — migrate axxium to DigitalOcean

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`axxium-production` and `axxium-staging` are defined only on the Promethean host,
deployed by `deploy-promethean.yml` through `promethean/scripts/deploy-axxium.sh`
with an rsynced source root. It is the largest genuine migration in the
inventory — a service that runs, with two phases, and no presence on
DigitalOcean at all.

## Dependencies

`services-caddy-hostname-scale-decision` for its hostnames.
`services-staging-slot-pattern` for the staging phase.

## Work

- Read `promethean/scripts/deploy-axxium.sh` and `promethean/services.yaml`'s two
  axxium entries first, and write down what the service actually needs: ports,
  state paths, required env names, and any host-local assumption. A migration
  that discovers a dependency after cutover is a rollback.
- Add axxium to `build-images.yml`. It currently offers `proxx`, `openplanner`,
  `knoxx-backend`, `knoxx-frontend` — axxium is a fifth. The image is built from
  `open-hax/axxium`; if that repo has no Dockerfile, adding one is part of this
  card and should be called out as app-repo work.
- Add `digitalocean/services/axxium/` with `compose.yaml`, `env.template`,
  `service.yaml` and `verify.sh`, and add `axxium` to the host's `roles`.
- Carry over state. Anything under the old `runtimeRoot` that is durable moves to
  the DigitalOcean host contract's `stateRoot`, and the move is rehearsed before
  the DNS record changes.
- Decide whether axxium joins the proxx → knoxx → caddy chain or deploys
  independently. It should be independent unless it has a real ordering
  dependency; a longer chain means one concurrency slot held for longer.
- `verify.sh` per the gate contract: bounded probes, enumerated acceptable
  statuses, anonymous probes for anything guarded, and proof of which revision is
  under test.
- Do not delete the Promethean definition in this card. It is removed by
  `services-promethean-lane-deletion`, after the DNS cutover has held.

## Definition of Done

- An axxium image builds in CI and is pushed to GHCR.
- Both phases deploy to DigitalOcean through the gated lane.
- `verify.sh` is required and passes against a real deploy.
- Durable state is on the new host and the move was rehearsed.
- The old definition is still present but no longer the deployed one, with a note
  saying so.
