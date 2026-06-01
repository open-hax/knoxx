---
uuid: "knoxx-uxx-chord-overlay-composite"
title: "Implement ChordOverlay composite in @open-hax/uxx"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Implement ChordOverlay composite in @open-hax/uxx

> Parent epic: `knoxx-knowledge-ops-ui-design-system`
> Points: 3

## Purpose

The Spacemacs-style chord overlay is the centerpiece of keyboard navigation. When `Space` is pressed the overlay appears bottom-left, narrows available chords on each keypress, fires the action on a complete match, and auto-closes after 3 seconds of inactivity. This slice implements or hardens `ChordOverlay` in `@open-hax/uxx/react`.

## Scope

- Verify or create `orgs/open-hax/uxx/react/src/composites/ChordOverlay.tsx`
- Accepts `chords: ChordDef[]` (from `keybindings.ts`), `activePrefix: string`, `onAction: (id: string) => void`, `onClose: () => void`
- Renders bottom-left overlay showing available next-key hints filtered by `activePrefix`
- Auto-closes after 3 s of inactivity via `useEffect` timeout
- Export `ChordOverlay` from `orgs/open-hax/uxx/react/src/index.ts`
- Add Vitest tests: initial render shows all root chords; prefix filters list; complete match calls `onAction`

## Definition of done

- Overlay renders at bottom-left with correct chord list filtered by current `activePrefix`
- `onAction` is called with chord `id` when a complete chord is entered
- Auto-close fires after 3 seconds with no further input
- Component is exported from the `@open-hax/uxx` package entry
- Vitest tests pass for filtering, action dispatch, and auto-close behaviour

## Notes

Split from parent epic `knoxx-knowledge-ops-ui-design-system` on 2026-05-30.
