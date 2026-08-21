---
uuid: services-promethean-ingress-decommission
title: Services — decommission the Promethean nginx ingress
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - ingress
  - decommission
  - has-parent
---

# Services — decommission the Promethean nginx ingress

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`nginx-ingress` does not migrate. Caddy already owns host 80 and 443 on
DigitalOcean as the sole ingress — "Exactly one ingress owner, per #27" — so
moving nginx there would create a second owner of the same ports. This card ends
the old ingress rather than relocating it.

## Dependencies

Every service whose hostname is still served by it. This card completes **after**
each per-service migration and its DNS cutover, and it is the last thing running
on the old host besides the bridge.

## Work

- Enumerate what `promethean/nginx/promethean.conf` actually serves. Every
  `server_name` is a hostname that must have moved before nginx stops. Note that
  `services#19`'s branch adds two more (`open-hax.promethean.rest` and its
  staging peer) to this file — those never take effect, because the website is
  being built for the DigitalOcean lane instead.
- Translate each surviving route to a Caddy site block, or confirm it is served
  by one already. Do not translate what is dead; a route with no service behind
  it is deleted, not ported.
- The certbot arrangement goes with it. The old lane holds certificates as host
  state alongside nginx; Caddy issues and renews its own into its `/data` volume.
  Confirm nothing else on the host reads the old certificate paths.
- Sequence the stop against DNS, not against the calendar. nginx serves a
  hostname until that hostname's record moves; it stops only once no record
  points at 104.130.159.19.
- Keep the config file until `services-promethean-lane-deletion` removes it, so a
  rollback during cutover has something to restore.
- Record what the old host is for afterwards — nothing, presumably, which makes
  its own decommissioning a decision someone should take deliberately rather than
  by leaving a droplet running.

## Definition of Done

- Every hostname nginx served is served by Caddy or is deliberately gone.
- No certificate renewal runs on the old host.
- nginx is stopped, with the config retained for rollback until lane deletion.
- The fate of the old host itself is recorded.
