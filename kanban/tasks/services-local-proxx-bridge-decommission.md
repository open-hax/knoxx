---
uuid: services-local-proxx-bridge-decommission
title: Services — decommission the local proxx bridge
status: ready
priority: P2
points: 2
labels:
  - tasks
  - deployment
  - decommission
  - proxx
  - has-parent
---

# Services — decommission the local proxx bridge

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

`local-proxx-bridge` is declared with `host: localhost` — an arrangement tied to
the old machine, not a service that runs somewhere else. It exists so production
Proxx can broker OpenAI OAuth through a localhost bridge rather than storing
local refresh tokens. It does not migrate; it either dies or is replaced by
something the DigitalOcean lane can express.

## Dependencies

`services-proxx-staging-migration` shares the same question and should agree with
whatever this card decides.

## Work

- Establish what still depends on it. The trust relationship is visible in the
  ingress config: Caddy strips `x-open-hax-bridge-auth` on every proxied request,
  with the comment that Proxx grants owner-level auth to requests carrying that
  header from a trusted local address. That header is the bridge's mechanism, and
  the stripping is what keeps it from arriving from outside.
- Decide the disposition:
  - **Dead** — the OAuth flow it brokered is no longer used, or Proxx now holds
    credentials directly. Remove the definition and confirm nothing sends the
    header.
  - **Replaced** — the flow is still needed, and a DigitalOcean-side equivalent
    must be declared as a real service with a gate.
- Whichever way, **the header stripping stays**. It is a defence against an
  externally supplied owner-level auth header, and its value does not depend on
  the bridge existing. Removing it alongside the bridge would be the kind of
  cleanup that opens a hole.
- Verify after: a request carrying `x-open-hax-bridge-auth` from outside must not
  receive owner-level auth. Assert it, rather than reasoning about it.

## Definition of Done

- The bridge's remaining dependents are established, not assumed.
- Its disposition is recorded and acted on.
- The header stripping survives, with a test proving an external header is not
  honoured.
