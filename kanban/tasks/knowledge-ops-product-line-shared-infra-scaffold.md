---
uuid: "knoxx-knowledge-ops-product-line-shared-infra-scaffold"
title: "Scaffold shared infrastructure packages for the product line"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---

# Scaffold shared infrastructure packages for the product line

> Parent epic: `knoxx-knowledge-ops-product-line`
> Points: 5

## Purpose

Create the three shared infrastructure packages (`packages/knowledge-lake`, `packages/chat-ui`, `packages/futuresight-kms`) that sit beneath all four products (Knowledge Ops, Exposure Monitor, The Lake, Shibboleth) and provide the provider abstraction layer, shared chat components, and schema/bridge clients.

## Scope

- `packages/knowledge-lake/` — provider abstraction layer with adapters for Azure, AWS, self-hosted, and local; define the core ingest/embed/search protocol
- `packages/chat-ui/` — shared React chat components (widget, message thread, input bar) extracted from or aligned with the existing frontend chat work
- `packages/futuresight-kms/` — schemas (TypeBox or edn), bridge client stubs, and UI shell for the multi-tenant KMS
- Each package needs `package.json`, `tsconfig.json` (or `shadow-cljs.edn`), a `src/` skeleton, and a README documenting its contract
- Wire packages into the pnpm workspace so they can be referenced by product packages

## Definition of done

- All three packages exist in the pnpm workspace and resolve correctly (`pnpm install` succeeds)
- `packages/knowledge-lake` exports a typed provider interface with at least one stub adapter
- `packages/chat-ui` exports at least one shared component consumable by both Knowledge Ops and Exposure Monitor frontends
- `packages/futuresight-kms` exports core schema types and a no-op bridge client
- No existing package build is broken after the scaffold is added

## Notes

Split from parent epic `knoxx-knowledge-ops-product-line` on 2026-05-30.
