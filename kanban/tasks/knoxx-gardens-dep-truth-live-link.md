---
uuid: "knoxx-gardens-dep-truth-live-link"
title: "Link Dependency Garden and Truth Workbench as live gardens"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Link Dependency Garden and Truth Workbench as live gardens

> Parent epic: `knoxx-knowledge-ops-gardens`
> Points: 3

## Purpose

Register the two existing operator surfaces — the Dependency Garden (`services/devel-deps-garden/`) and the Truth Workbench (`services/eta-mu-truth-workbench/`) — as named, navigable gardens inside the workbench Gardens index so that operators can launch them from a single surface.

## Scope

- Define garden registry entries for `devel-deps-garden` and `eta-mu-truth-workbench` (title, purpose, `garden_id`, target URL or embed route)
- Render both entries as clickable cards or list items on the `/gardens` index page
- Each card must link out to (or embed) the live service URL — no reimplementation of the underlying garden logic
- Confirm the two existing services are reachable from the workbench host (document any proxy or CORS config needed)

## Definition of done

- The `/gardens` index page shows both Dependency Garden and Truth Workbench as distinct entries
- Clicking each entry navigates to or opens the corresponding live service without error
- Garden registry entries follow the `garden_id / title / purpose / lakes / views / actions / outputs` schema from the parent spec

## Notes

Split from parent epic `knoxx-knowledge-ops-gardens` on 2026-05-30.
