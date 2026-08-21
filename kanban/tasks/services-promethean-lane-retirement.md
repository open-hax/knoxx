---
uuid: services-promethean-lane-retirement
title: Services — retire the Promethean lane onto DigitalOcean
status: ready
priority: P1
points: 8
labels:
  - tasks
  - deployment
  - migration
  - has-parent
---

# Services — retire the Promethean lane onto DigitalOcean

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

Everything deploys to DigitalOcean. Two lanes exist today and only one has a
health gate, a host contract, merge-time deploy authorization, and ordered
deployment. Leaving the second one alive means every future service faces the
same choice this epic just had to make, and the answer decays.

This card is scoped to the *decision and the definitions*, not to migrating every
service in one change. Each service migrates or is decommissioned on its own; this
card owns the inventory, the order, and the point at which `deploy-promethean.yml`
is deleted.

## Dependencies

`services:docs/deployment-model.md` §1 and §7. Independent of the publication work
— included in this epic because the website is the first service the decision
binds, not because publication waits on it.

## The inventory

Defined only on Promethean:

```text
nginx-ingress          openplanner-production   axxium-production   local-proxx-bridge
proxx-staging          openplanner-staging      axxium-staging
knoxx-staging
```

`proxx-production` and `knoxx-production` are already deployed by the
DigitalOcean lane; their `promethean/services.yaml` entries are stale
definitions, not second deployments, and should be removed rather than migrated.

## Work

- **Settle OpenPlanner first.** `digitalocean/hosts/production.yaml` lists
  `openplanner` in `roles` with no `digitalocean/services/openplanner/` behind
  it, and `deploy-stack.yml` records that its HTTP service is deliberately absent
  because Knoxx runs the data plane in-process against Atlas. Decide migrate or
  retire. Migrating means a service directory and a gate; retiring means removing
  the role and the remaining REST callers — which is
  `knoxx-gardens-openplanner-rest-decoupling`'s scope. Everything else in the
  inventory is cheaper once this is answered.
- **Decide what staging means on one host.** `proxx-staging` and `knoxx-staging`
  become second compose projects and second hostnames on the DigitalOcean host,
  not a second machine. The promotion rule needs a staging record to exist, so
  this cannot simply be dropped.
- **`local-proxx-bridge` and `nginx-ingress` do not migrate.** Caddy already owns
  80/443 on the DigitalOcean host as its sole ingress; the bridge is a localhost
  arrangement tied to the old host. Both are decommissioned, and the Promethean
  nginx config and certbot sidecar go with them.
- **Reopen the Caddy hostname decision explicitly.** `caddy/compose.yaml`'s
  header records that three hostnames on HTTP-01 was the reason to reject a
  wildcard and keep Cloudflare API credentials off the host. Consolidating every
  service plus staging slots raises that count enough to be a different question.
  Answer it once, in that file's header, rather than by accident one hostname at
  a time.
- **Sequence the DNS.** Every hostname on 104.130.159.19 moves to
  157.245.125.134; records stay DNS-only so ACME HTTP-01 reaches the origin.
  Record first, deploy second, certificate third, per hostname.
- **Delete the lane last.** `deploy-promethean.yml`, `promethean/scripts/*`, and
  `promethean/services.yaml` are removed only when nothing is defined there.
  Until then, mark the file as legacy at its head so a reader does not treat it
  as current.
- Update `ROADMAP.md`, whose §2 and §3 still describe the two-lane world.

## Definition of Done

- OpenPlanner is either declared as a DigitalOcean service with a gate, or
  removed from `roles` with its remaining callers named.
- Staging exists on the DigitalOcean host for every service that declares it.
- No hostname resolves to 104.130.159.19.
- The Caddy hostname-count decision is recorded in `caddy/compose.yaml`'s header.
- `promethean/` and `deploy-promethean.yml` are deleted, or every remaining entry
  has a written reason and a date.
- `ROADMAP.md` describes one lane.
