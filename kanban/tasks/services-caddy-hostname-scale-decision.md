---
uuid: services-caddy-hostname-scale-decision
title: Services — reopen the certificate decision before hostnames grow
status: ready
priority: P1
points: 2
labels:
  - tasks
  - deployment
  - ingress
  - has-parent
---

# Services — reopen the certificate decision before hostnames grow

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`digitalocean/services/caddy/compose.yaml`'s header records a decision and its
reasoning:

> Chosen over the legacy nginx path […] because that arrangement needs Cloudflare
> API credentials resident on the host and a wildcard certificate held as host
> state. **Three hostnames and HTTP-01 need neither.**

Consolidating every service onto this host — plus a staging slot per service,
plus the website — takes the count well past three. The decision was correct at
three and should be re-made explicitly at its new number, once, rather than
eroded one hostname at a time by whoever adds the next service.

## Work

- Count the end state honestly: production and staging hostnames for every
  service that survives the migration, including the website.
- Weigh the two options against that number:
  - **Stay on HTTP-01 per hostname.** Costs a Caddyfile site block and an
    environment placeholder per hostname, and each new hostname needs its DNS
    record before its first issuance. No credentials on the host.
  - **Wildcard via DNS-01.** One certificate, no per-hostname record ordering —
    but Cloudflare API credentials resident on the host and a wildcard held as
    host state, which is exactly what was rejected.
- Note the rate limit either way: Let's Encrypt is per-hostname-per-week, and the
  Caddy state volume comment already warns that losing `/data` means re-issuing.
  A migration that re-issues many hostnames in one week is a real risk.
- Record the answer **in `compose.yaml`'s header**, replacing the "three
  hostnames" sentence, so the next reader sees the current reasoning and not the
  superseded one.
- If the answer is to stay on HTTP-01, say what the ceiling is and what triggers
  revisiting.

## Definition of Done

- The end-state hostname count is written down.
- One option is chosen with its costs stated.
- `caddy/compose.yaml`'s header states the current decision, not the old one.
- If HTTP-01 stays, the re-issuance risk during migration has a mitigation.
