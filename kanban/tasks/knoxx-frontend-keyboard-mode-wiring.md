---
uuid: "knoxx-frontend-keyboard-mode-wiring"
title: "Wire keyboard mode state and ChordOverlay into Knoxx frontend"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Wire keyboard mode state and ChordOverlay into Knoxx frontend

> Parent epic: `knoxx-knowledge-ops-ui-design-system`
> Points: 3

## Purpose

The `ModeIndicator` and `ChordOverlay` components are useful only when connected to global keyboard event handling. This slice adds a React context/hook that tracks the current modal mode, listens for `Space`/`Esc`/`i`/`v` transitions, and mounts `ChordOverlay` at the app root of the Knoxx frontend.

## Scope

- Add `frontend/src/keyboard/ModeContext.tsx` — React context + provider tracking `mode` and `activePrefix`
- Add `frontend/src/keyboard/useModeKeyboard.ts` — `useEffect` attaching `keydown` listener, dispatching mode transitions and chord prefix updates
- Mount `<ModeProvider>` at the frontend app root (`frontend/src/App.tsx` or equivalent)
- Render `<ChordOverlay>` and `<ModeIndicator>` inside `ModeProvider`, connected to context state
- Add Vitest tests for mode transition logic (normal→insert on `i`, any→normal on `Esc`, normal→command on `Space`)

## Definition of done

- Pressing `Space` in Normal mode opens `ChordOverlay` and switches to Command mode
- Pressing `Esc` from any mode returns to Normal mode and closes any open overlay
- `ModeIndicator` in the status bar reflects the current mode with the correct Monokai color
- Mode transitions are covered by at least three Vitest unit tests
- No existing frontend tests regress

## Notes

Split from parent epic `knoxx-knowledge-ops-ui-design-system` on 2026-05-30.
