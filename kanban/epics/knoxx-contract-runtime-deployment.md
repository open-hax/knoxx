---
uuid: "knoxx-contract-runtime-deployment"
title: "Knoxx as a Deployment of the Contract Runtime"
status: in_progress
priority: "P1"
labels: "[\"epics\",\"resources\",\"manifests\",\"decomposition\",\"contract-runtime\"]"
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: null
category: "epics"
---
# Knoxx as a Deployment of the Contract Runtime

> Source: `docs/design/resource-architecture.md` (Resource Grammar + Deployment sections)
> Builds on: `knoxx-action-scope-and-pipeline-collapse` (done)

Date: 2026-06-10
Status: in-progress
Repo: `packages/agents/knoxx`

## Goal

Knoxx is not an application with contracts bolted on — **Knoxx is a deployment
of the contract runtime**, the prototype deployment of this system inside
OpenPlanner. As much of Knoxx as possible becomes contracts (namespace
manifests), or decomposes into drivers, protocols, libraries, and packages.

## The Grammar (implemented 2026-06-10)

The resource manifest grammar covers every kind:

1. `:K/id` registers a resource of kind K (qualified as `:namespace/local-id`).
2. Registration is optional — `:K/*` without `:K/id` is an **anonymous facet**
   owned by the entry's registered kinds (the `:action/fn` pattern,
   generalized). The loader records `:resource/anonymous-facets`.
3. Composite entries register several kinds; interpreters read only their keys.
4. References live in the owner's namespace (`:model/family`, never a second
   `:K/id`).

Implemented in `domain/resources/namespace_file.cljs` (`kind-id-keys` covers
all 17 kinds). Exemplars: `contracts/namespaces/ussyverse.edn`,
`contracts/namespaces/hello_world.edn` (full generator + trigger demo,
individual files deleted).

## What Remains

- **Manifest migration** — move remaining individual contract files into
  namespace manifests, domain by domain (fork-tales, broadcast-studio, muses,
  devel, knoxx-session, sources, models, roles/caps last).
- **Anonymous facet adoption** — interpreters honor anonymous facets per kind;
  first: `:actions/start-agent-session` accepts an inline `:agent/*` facet
  (with explicit capability gating — anonymous agents must not escalate).
- **Qualified-id resolution** — role/capability/agent resolution
  (`actor-scope`, `tooling`) accepts qualified ids so roles and caps can live
  in manifests without breaking slug-based lookup.
- **Runtime decomposition** — inventory `knoxx.backend` into manifest /
  driver / protocol / library; extract the contract runtime core (loader,
  schema, law, interpreter, safe-eval, stores) as a reusable package; what
  stays is the deployment: manifests, driver bindings, HTTP/WS surface.

## Risks

- Identity migration changes ids (`hello_world_inbox` → `:hello-world/inbox`):
  anything persisting old ids (session names, audit rows) sees new names.
- Anonymous agent facets bypass contract-resolution paths — capability gating
  must be designed before adoption, not after.
- Role/cap slug resolution is load-bearing and string-shaped; qualified-id
  support must keep legacy slugs working.
