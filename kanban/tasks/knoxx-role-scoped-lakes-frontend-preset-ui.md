---
uuid: "knoxx-role-scoped-lakes-frontend-preset-ui"
title: "Role-Scoped Lakes: Frontend lake-aware search preset selector"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 5
category: tasks
---

# Role-Scoped Lakes: Frontend lake-aware search preset selector

> Parent epic: `knoxx-knowledge-ops-role-scoped-lakes`
> Points: 5

## Purpose

Add a role/lake preset selector to the Knoxx search UI so that users can switch between the five canonical lake views (Docs, Code, Config, Data, Mixed/All) and have the active preset reflected in every subsequent search query.

## Scope

- Knoxx frontend (`orgs/open-hax/knoxx/frontend/`) — add a `LakePresetSelector` component (React/CLJS) rendered in the search header or sidebar
- Presets to expose: `knowledge_worker` (Docs), `developer` (Code + Docs), `devsecops` (Config + Code + Docs + Data), `data_analyst` (Data + Events + Docs), `org_admin` (All lakes / cross-lake synthesis)
- Active preset is stored in component or app state; selecting a preset passes `role=<name>` (or equivalent lake filter params) on the next search request
- The search result list must display an indicator of the active lake filter (e.g. a badge or label showing "Code + Docs")
- Vitest unit tests covering preset resolution and the selector component render

## Definition of done

- Preset selector renders in the search UI with all five role options
- Switching preset immediately filters results to the correct lakes on the next search
- Active preset is visually indicated in the search results area
- Frontend typechecks cleanly (`pnpm typecheck` from `frontend/`)
- Vitest tests pass for the new component and preset logic (`pnpm test`)
- No regressions in existing search behaviour when no preset is selected (defaults to current behaviour)

## Notes

Split from parent epic `knoxx-knowledge-ops-role-scoped-lakes` on 2026-05-30.
