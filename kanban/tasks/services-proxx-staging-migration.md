---
uuid: services-proxx-staging-migration
title: Services — proxx staging slot on DigitalOcean
status: ready
priority: P1
points: 2
labels:
  - tasks
  - deployment
  - migration
  - proxx
  - has-parent
---

# Services — proxx staging slot on DigitalOcean

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`proxx-staging` exists only on the Promethean host. Proxx production already
deploys through the DigitalOcean lane, so as with Knoxx this is the missing
phase, and the promotion rule needs its record.

## Dependencies

`services-staging-slot-pattern`. Sequence before `services-knoxx-staging-migration`
if staging Knoxx is to point at staging Proxx.

## Work

- Apply the staging-slot pattern to `digitalocean/services/proxx/`.
- The container-name collision is recorded history here, not a hypothetical:
  `promethean/services.yaml` notes on both proxx entries that federation nginx
  must route to project-specific container names because bare service aliases
  collide with the other phase on the shared Docker network. Carry that forward
  explicitly.
- Credentials are the real constraint. Proxx brokers provider access, and the
  host contract lists `REQUESTY_API_KEY` under `providerSecrets`. Decide whether
  staging holds its own provider credentials or shares production's — sharing
  means staging traffic spends production quota and appears in production
  provider logs.
- The production entry notes that Proxx should broker OpenAI OAuth through the
  localhost bridge rather than storing local refresh tokens, and
  `local-proxx-bridge` is being decommissioned. Confirm whether staging needs an
  equivalent, or whether that arrangement dies with the old host.
- `verify.sh` runs unchanged against staging.

## Definition of Done

- Proxx staging deploys to DigitalOcean and its gate passes.
- Container names and aliases carry the phase; no cross-phase collision.
- The provider-credential posture for staging is recorded.
- The bridge question is answered rather than inherited.
