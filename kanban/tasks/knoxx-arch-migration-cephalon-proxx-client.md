---
uuid: "knoxx-arch-migration-cephalon-proxx-client"
title: "Architecture Migration — Cephalon Provider Wired into Proxx Client"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Architecture Migration — Cephalon Provider Wired into Proxx Client

> Parent epic: `knoxx-knowledge-ops-architecture-migration`
> Points: 3

## Purpose

Extend knoxx's backend proxx client to support the `auto:cephalon` model routing strategy, so agent runs can resolve cheapest/fastest/smartest models through the Cephalon provider-ordering logic already implemented in proxx's TypeScript layer.

## Scope

- Update `backend/src/cljs/knoxx/backend/infra/clients/proxx.cljs`: add a `chat-completions-cephalon!` variant (or extend `chat-completions!` to pass a `X-Routing-Strategy: cephalon` header) that signals proxx to apply cephalon provider ordering
- Update `backend/src/cljs/knoxx/backend/infra/agent/provider.cljs`: when the configured default model is `auto:cephalon`, `auto:cephalon:cheapest`, `auto:cephalon:fastest`, or `auto:cephalon:smartest`, use the cephalon-aware call path
- Add a test or doc comment confirming the model string is forwarded unchanged to proxx (which owns the resolution logic in `src/lib/provider-strategy/strategies/cephalon.ts`)
- Verify `pnpm lint` and shadow-cljs compile pass cleanly

## Definition of done

- Knoxx backend forwards `auto:cephalon` (and `:cheapest`/`:fastest`/`:smartest` variants) to proxx without transforming the model string
- Agent provider resolution in `infra/agent/provider.cljs` does not hard-fail when the configured model begins with `auto:cephalon`
- `pnpm lint` (clj-kondo) and `pnpm typecheck` (shadow-cljs compile) both exit zero with no new warnings

## Notes

Split from parent epic `knoxx-knowledge-ops-architecture-migration` on 2026-05-30.
