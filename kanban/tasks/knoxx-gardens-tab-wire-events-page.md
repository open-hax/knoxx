---
uuid: "knoxx-gardens-tab-wire-events-page"
title: "Wire Gardens tab into the workbench events page"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Wire Gardens tab into the workbench events page

> Parent epic: `knoxx-knowledge-ops-gardens`
> Points: 3

## Purpose

Add a Gardens tab to the workbench navigation alongside Chat, Query, CMS, Ingestion, Labels, and Synthesize so that operators can switch into a garden surface from any workbench page.

## Scope

- Add a `Gardens` entry to the workbench tab/nav component (frontend, likely under `frontend/src/`)
- Wire a route (e.g. `/gardens`) that renders a gardens index or placeholder page
- Emit a navigation event when the Gardens tab is selected so downstream components can react
- No garden implementations are required in this slice — only the tab presence, routing, and event wiring

## Definition of done

- A Gardens tab is visible in the workbench navigation and does not break existing tabs
- Clicking the tab routes to `/gardens` (or equivalent) without a full page reload
- A navigation event is emitted on tab selection, observable in the browser dev console or event bus

## Notes

Split from parent epic `knoxx-knowledge-ops-gardens` on 2026-05-30.
