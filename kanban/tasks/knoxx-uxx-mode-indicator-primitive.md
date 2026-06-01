---
uuid: "knoxx-uxx-mode-indicator-primitive"
title: "Implement ModeIndicator primitive in @open-hax/uxx"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Implement ModeIndicator primitive in @open-hax/uxx

> Parent epic: `knoxx-knowledge-ops-ui-design-system`
> Points: 2

## Purpose

Every page must display the current modal mode (Normal, Insert, Command, Visual) in the status bar using a consistent color-coded indicator. This slice implements or hardens the `ModeIndicator` primitive in `@open-hax/uxx/react` so it correctly maps modes to Monokai accent colors.

## Scope

- Verify or create `orgs/open-hax/uxx/react/src/primitives/ModeIndicator.tsx`
- Accept `mode: 'normal' | 'insert' | 'command' | 'visual'` prop
- Map modes to palette from `keybindings.ts`: normal=#a6e22e, insert=#fd971f, command=#66d9ef, visual=#ae81ff
- Export `ModeIndicator` from `orgs/open-hax/uxx/react/src/index.ts`
- Add Vitest tests for all four mode variants

## Definition of done

- `ModeIndicator` renders the correct mode label and background/text color for all four modes
- Component is exported from `@open-hax/uxx` package entry point
- Vitest tests assert correct rendered output for each mode value
- Colors are sourced from `@open-hax/uxx/tokens` (no hardcoded hex in component)

## Notes

Split from parent epic `knoxx-knowledge-ops-ui-design-system` on 2026-05-30.
