---
uuid: "knoxx-cms-contract-validation"
title: "Exercise and validate the CMS contracts"
status: incoming
priority: P3
labels: ["tasks", "5sp", "has-parent", "cms", "validation"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Exercise and validate the CMS contracts

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The CMS contracts have never been tested. The CMS surface is also the one part of
Knoxx whose deploy check is *conditionally skipped*, so it has been shipping
unverified by construction.

## Verified as of 2026-08-04

The Knoxx deploy health gate (`digitalocean/services/knoxx/verify.sh`) probes
`/api/openplanner/v1/cms/documents?limit=1` **only when a host OpenPlanner API is
reachable**. `deploy-stack.yml` sets `KNOXX_EXPECT_OPENPLANNER_REST=false` and
deploys no OpenPlanner, so on every production deploy the gate logs:

```
knoxx: CMS surface skipped — no host OpenPlanner API at http://host.docker.internal:7777
```

So the CMS path is neither exercised in tests nor in the deploy gate.

## Scope

- Establish what the CMS contracts assert, and whether the runtime honours them
  (`contracts/` + the CMS compatibility routes).
- Decide the CMS's actual dependency story: it currently reaches a host
  OpenPlanner HTTP service over `host.docker.internal:7777` that production does
  not run. Either deploy that service, port the CMS path to the in-process Mongo
  client the rest of Knoxx already uses, or drop the surface.
- Once it has a dependency that exists in production, make the health gate
  require it unconditionally instead of skipping.

## Note

This is the last REST-only OpenPlanner dependency in the deployed stack; chat,
sessions, search and translation all run in-process through the SDK. Resolving it
removes the `KNOXX_EXPECT_OPENPLANNER_REST` flag and a whole conditional branch
from the deploy gate.

## Done when

- The CMS contracts have tests that fail when the runtime violates them.
- The CMS surface has a dependency that exists in production.
- The deploy gate asserts the CMS surface unconditionally.

## Prior art on this board

- **`knoxx-arch-migration-cms-routes-retirement`** (status: breakdown) — *Retire
  Legacy CMS Backend Routes*. A retirement direction already exists, so "drop the
  surface" is not a fresh option to weigh here: check that card first and treat
  this one as validating whatever survives it. If retirement lands, most of this
  card evaporates and the deploy gate's conditional CMS branch goes with it.
- **`knoxx-cms-backend-routes`** (accepted) — the CRUD/publish endpoints this
  would be validating.
