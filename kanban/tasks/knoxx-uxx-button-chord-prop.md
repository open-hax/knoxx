---
uuid: "knoxx-uxx-button-chord-prop"
title: "Add chord prop to @open-hax/uxx Button primitive"
status: ready
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Add chord prop to @open-hax/uxx Button primitive

> Parent epic: `knoxx-knowledge-ops-ui-design-system`
> Points: 2

## Purpose

The design system mandates that every button exposes its keyboard shortcut hint inline. This slice adds the optional `chord` prop to the existing `Button` primitive so callers can render a muted chord badge next to button labels.

## Scope

- Edit `orgs/open-hax/uxx/react/src/primitives/Button.tsx` — add optional `chord?: string` prop
- When `chord` is provided, render a `<kbd>` element styled in Monokai muted tone (`#75715e`) to the right of the button label
- Update `orgs/open-hax/uxx/contracts/button.edn` to include `chord` field
- Add Vitest tests confirming `chord` renders when provided and is absent when omitted

## Definition of done

- `Button` accepts and renders a `chord` prop as a styled `<kbd>` element
- Existing Button tests continue to pass (no regression)
- `button.edn` contract includes the `chord` field definition
- New Vitest test cases cover the chord-present and chord-absent render paths

## Notes

Split from parent epic `knoxx-knowledge-ops-ui-design-system` on 2026-05-30.
