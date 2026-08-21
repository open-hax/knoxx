---
uuid: services-promethean-lane-retirement
title: Services — retire the Promethean lane onto DigitalOcean
status: breakdown
priority: P1
points: 0
labels:
  - tasks
  - deployment
  - migration
  - roll-up
  - has-parent
---

# Services — retire the Promethean lane onto DigitalOcean

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`
> Coordination card. The work is the twelve children below, totalling **34sp**.

## Purpose

Everything deploys to DigitalOcean. Two lanes exist today and only one has a
health gate, a host contract, merge-time deploy authorization, and ordered
deployment. Leaving the second alive means every future service faces the choice
this epic just had to make, and the answer decays.

Retiring a lane is not one change. Each service migrates or is decommissioned on
its own evidence, and three cross-cutting concerns — hostname scale, DNS, and the
final deletion — sequence around them.

## The inventory

Defined only on Promethean:

```text
openplanner-production   axxium-production   proxx-staging    nginx-ingress
openplanner-staging      axxium-staging      knoxx-staging    local-proxx-bridge
```

`proxx-production` and `knoxx-production` also appear in
`promethean/services.yaml`, but the DigitalOcean lane already deploys both and
both hostnames resolve there. Those entries are stale definitions, not second
deployments — with one live exception: Knoxx's own workflows still *call* the
legacy module on every push, which is card 0.

Resolved 2026-08-21, rather than read off `services.yaml`:

```text
knoxx.promethean.rest          157.245.125.134   done
proxx.promethean.rest          157.245.125.134   done
openplanner.promethean.rest    104.130.159.19
axxium.promethean.rest         104.130.159.19
staging-knoxx.promethean.rest  104.130.159.19
open-hax.promethean.rest       104.130.159.19    website, unbuilt
```

Four hostnames to move, not the whole constellation.

## Child breakdown

**First, because it is in the way**

0. **P0 / ready / 3sp** `services-knoxx-deploy-path-unification` — every push to
   Knoxx `main` deploys to the legacy host. The publication ladder is ten such
   pushes.

**Disposition — decide before moving anything**

1. **P1 / ready / 5sp** `services-openplanner-lane-disposition` — migrate or
   retire. Everything else is cheaper once this is answered.
2. **P1 / ready / 2sp** `services-caddy-hostname-scale-decision` — reopen the
   HTTP-01-versus-wildcard decision before the hostname count grows past it.

**Per service**

3. **P1 / ready / 5sp** `services-axxium-digitalocean-migration`
4. **P1 / ready / 3sp** `services-staging-slot-pattern` — what a staging slot is
   on a single host, established once.
5. **P1 / ready / 3sp** `services-knoxx-staging-migration`
6. **P1 / ready / 2sp** `services-proxx-staging-migration`
7. **P1 / ready / 3sp** `services-promethean-ingress-decommission` — nginx and
   its certbot sidecar; not a migration, Caddy already owns 80/443.
8. **P2 / ready / 2sp** `services-local-proxx-bridge-decommission`
9. **P2 / ready / 1sp** `services-stale-promethean-definitions-removal`

**Closing**

10. **P1 / ready / 3sp** `services-promethean-dns-cutover` — record before
    deploy before certificate, per hostname.
11. **P2 / ready / 2sp** `services-promethean-lane-deletion` — last, and only
    when nothing is defined there.

## Order

```text
knoxx-deploy-path-unification (first, blocks the publication ladder)

openplanner-lane-disposition ─┐
caddy-hostname-scale-decision ┴─> axxium-migration ──────┐
                                  staging-slot-pattern   │
                                    -> knoxx-staging     ├─> dns-cutover
                                    -> proxx-staging     │      │
                                  ingress-decommission ──┘      │
                                  bridge-decommission           │
                                  stale-definitions-removal     │
                                                                v
                                                        lane-deletion
```

DNS is per hostname, not one flag day: each service's record moves as that
service becomes ready on DigitalOcean, so a failure rolls back one hostname
rather than the constellation.

## Done when

Every child is complete, `promethean/` and `deploy-promethean.yml` are gone, and
`ROADMAP.md` describes one lane.
