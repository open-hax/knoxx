---
uuid: "knoxx-qualified-id-resolution"
title: "Qualified-Id Resolution for Roles, Capabilities, Agents"
status: pending
priority: "P2"
labels: ["tasks", "5sp", "contract-runtime-deployment"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 5
category: "tasks"
---
# Qualified-Id Resolution for Roles, Capabilities, Agents

> Parent epic: `knoxx-contract-runtime-deployment`

## Context

Role/capability resolution (`actor-scope`, `tooling`) is slug-shaped
(`:role/contract-librarian` → `contract_librarian` → file lookup). Manifest
resources have qualified ids (`:ns/local`). Until resolution accepts both,
roles/caps/actors cannot migrate to manifests.

## Work

1. Resolution accepts qualified ids alongside legacy slugs everywhere a role,
   capability, agent, or actor is referenced.
2. Record lookup by `[class id]` tries qualified id, then legacy slug.
3. Keep the duplicate-slug dedup behavior intact (see contracts/AGENTS.md
   "Duplicate Slug Trap").
4. Tests for both id forms across the resolution chain.

## Definition of Done

- [ ] Roles/caps resolvable by qualified id and legacy slug
- [ ] No behavior change for existing slug references
- [ ] Tests pass

## Risks

- Load-bearing string handling across actor-scope/tooling
