---
uuid: services-promethean-dns-cutover
title: Services — move every hostname to the DigitalOcean host
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - dns
  - migration
  - has-parent
---

# Services — move every hostname to the DigitalOcean host

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

Four hostnames still resolve to 104.130.159.19 and must resolve to
157.245.125.134:

```text
openplanner.promethean.rest    axxium.promethean.rest
staging-knoxx.promethean.rest  open-hax.promethean.rest   (website, unbuilt)
```

`knoxx.promethean.rest` and `proxx.promethean.rest` already answer from
DigitalOcean, so production is not part of this cutover — which makes it far
lower risk than the card's title suggests, and means every remaining move is a
service that is either being retired, is staging, or does not exist yet.

Ordering is still not cosmetic: records are deliberately DNS-only rather than
proxied so that ACME HTTP-01 reaches the origin directly, which means the record
must move **before** the new host can obtain a certificate for that hostname.

## Dependencies

Each hostname's own service migration. This card owns the sequencing and the
verification, not the migrations themselves.

## The per-hostname sequence

```text
1. the service is deployed on DigitalOcean and its verify.sh passes
2. its Caddy site block and host placeholder exist
3. the DNS record moves          <- traffic follows here
4. Caddy obtains the certificate <- cannot happen before step 3
5. verify from outside: TLS, the served surface, and the old host no longer serving
```

One hostname at a time. A failure then rolls back one record rather than the
constellation, and rollback is a record change, which is why the old host's
services stay running until the end.

## Work

- Re-resolve every hostname before starting; the inventory above is a snapshot.
  `promethean/nginx/promethean.conf` is the authority on what the old host is
  configured to answer for, and DNS is the authority on what actually reaches it.
  Where they disagree, believe DNS.
- Lower TTLs ahead of each move so rollback is minutes rather than hours. Do this
  as a separate, earlier step — a TTL reduction only helps if it has already
  propagated when the move happens.
- Move records one at a time, in the order services became ready. Do not batch.
- Watch the certificate rate limit. Let's Encrypt is per-hostname-per-week and
  Caddy's state volume comment already warns that losing `/data` means re-issuing.
  Spreading the moves is a mitigation, not a nicety; if
  `services-caddy-hostname-scale-decision` chose a wildcard, this constraint
  changes and this card follows it.
- Verify each move from outside the hosts: certificate issued for the right name,
  the expected surface served, and the old host no longer answering for it.
- Do not stop the old service on cutover. It stays running until its hostname has
  held for an agreed period, so a rollback has somewhere to go.
- Record each move with its time and outcome, so the sequence is reconstructable
  if a later one behaves differently.

## Definition of Done

- No `promethean.rest` hostname resolves to 104.130.159.19.
- Every moved hostname has a valid certificate issued on the new host.
- Each move was verified from outside and recorded.
- Rollback was possible at every point until the hold period elapsed.
