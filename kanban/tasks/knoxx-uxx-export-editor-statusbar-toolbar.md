---
uuid: "knoxx-uxx-export-editor-statusbar-toolbar"
title: "Add status bar and toolbar exports to @open-hax/uxx"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Add status bar and toolbar exports to @open-hax/uxx

> Parent epic: `knoxx-knowledge-ops-ui-design-system`
> Points: 3

## Purpose

Every Knowledge Ops page requires a status bar at the bottom displaying mode, collection, model, and counts. This slice creates the `StatusBar` and `Toolbar` composite components in `@open-hax/uxx/react` and exports them from the package entry point.

## Scope

- Add `orgs/open-hax/uxx/react/src/composites/StatusBar.tsx` — renders `[ModeIndicator] [collection] [model] [counts]` slots
- Add `orgs/open-hax/uxx/react/src/composites/Toolbar.tsx` — horizontal row of `Button` actions, accepts `items` prop
- Export both from `orgs/open-hax/uxx/react/src/index.ts`
- Add Vitest unit tests for both composites (slot rendering, prop forwarding)

## Definition of done

- `StatusBar` renders `ModeIndicator`, collection name, model name, and counts slots via props
- `Toolbar` renders a list of `Button` items with correct variant and chord hints
- Both components are exported from `@open-hax/uxx` package entry
- Vitest tests pass for both components with at least two test cases each

## Notes

Split from parent epic `knoxx-knowledge-ops-ui-design-system` on 2026-05-30.
