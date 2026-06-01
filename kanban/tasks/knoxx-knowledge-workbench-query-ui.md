---
uuid: "knoxx-knowledge-workbench-query-ui"
title: "Knowledge Workbench — Query UI"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Knowledge Workbench — Query UI

> Parent epic: `knoxx-knowledge-ops-clojure-backend-migration`
> Points: 3

## Purpose

Wire a minimal workbench query interface in the frontend that lets users select a role preset and lake, submit a search or answer request, and display results returned from the `kms-query` Clojure service.

## Scope

- Add a query panel component (role-preset selector, free-text query input, submit button, results list) to the existing frontend surface — drawing from `orgs/mojomast/ragussy/frontend/src/pages/IngestionPage.tsx` as a donor for layout patterns
- Bind the panel to `POST /api/query/search` and `POST /api/query/answer` endpoints provided by `kms-query`
- Fetch and populate the preset dropdown from `GET /api/query/presets`
- Display retrieved context passages and any synthesised answer from Proxx

## Definition of done

- The preset dropdown populates from `GET /api/query/presets` without errors
- Submitting a query against a live `kms-query` instance returns results and renders them in the UI
- The component passes frontend typechecks (`pnpm typecheck` in `frontend/`) with no new errors

## Notes

Split from parent epic `knoxx-knowledge-ops-clojure-backend-migration` on 2026-05-30.
