---
uuid: "knoxx-arch-migration-ragussy-ui-sunset"
title: "Architecture Migration — Sunset Ragussy UI in Favour of Knoxx Frontend"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Architecture Migration — Sunset Ragussy UI in Favour of Knoxx Frontend

> Parent epic: `knoxx-knowledge-ops-architecture-migration`
> Points: 2

## Purpose

Decommission the standalone Ragussy frontend UI (`orgs/ussyverse/ragussy/frontend`) as the canonical knowledge-ops interface, ensuring all actively used UI surfaces (ingestion, CMS, query panel) are served exclusively from the knoxx frontend going forward.

## Scope

- Identify pages in `orgs/ussyverse/ragussy/frontend/src/pages/` that have functional equivalents in `orgs/open-hax/openplanner/packages/agents/knoxx/frontend/src/pages/` (e.g. `CmsPage.tsx`, `IngestionPage.tsx` pattern)
- For each page with a knoxx equivalent: document the mapping in a `ragussy/handoff.md` update (or a comment in the relevant knoxx page) so future agents know where features migrated to
- Add a top-level notice to `orgs/ussyverse/ragussy/README.md` marking the frontend as sunset and pointing to the knoxx frontend URL
- Confirm no PM2 process or nginx vhost is actively routing user traffic to the Ragussy UI port (`:5174` / `:3001`) that isn't already handled by knoxx

## Definition of done

- `orgs/ussyverse/ragussy/README.md` contains a clearly marked sunset notice with a pointer to the replacement knoxx UI
- Every Ragussy UI page that had a knoxx counterpart is documented in the handoff notes with the knoxx route path
- No active nginx routing sends user-facing requests to the Ragussy frontend; any proxy rules that did so are removed or redirected

## Notes

Split from parent epic `knoxx-knowledge-ops-architecture-migration` on 2026-05-30.
