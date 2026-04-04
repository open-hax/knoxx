# Knowledge Ops — Legacy UI Inventory

> *Every UI surface not built from `orgs/open-hax/uxx/` is explicit technical debt, not invisible drift.*

---

## Purpose

Track all currently active UI surfaces that do **not** primarily use:
- `@open-hax/uxx/tokens`
- `@open-hax/uxx`
- `@open-hax/uxx-reagent`

This turns UI inconsistency into managed work instead of accidental entropy.

---

## Rule

All **new** interactive UI features must use `orgs/open-hax/uxx/`.

Any existing UI that still uses custom HTML/Tailwind/CSS directly must be listed here until migrated.

---

## Inventory

### Knoxx frontend pages

| File | Current state | Migration note |
|------|---------------|----------------|
| `orgs/open-hax/knoxx/frontend/src/pages/ChatLabPage.tsx` | legacy custom UI | migrate to `@workspace/chat-ui` + `@open-hax/uxx` |
| `orgs/open-hax/knoxx/frontend/src/pages/CmsPage.tsx` | ✅ **Migrated** (2026-04-02) | uses `@open-hax/uxx` Button, Card, Badge |
| `orgs/open-hax/knoxx/frontend/src/pages/IngestionPage.tsx` | mostly legacy, except new browser panel | migrate shell/layout + forms gradually |
| `orgs/open-hax/knoxx/frontend/src/pages/QueryPage.tsx` | partial migration | uses `@open-hax/uxx` for some components |
| `orgs/open-hax/knoxx/frontend/src/pages/GardensPage.tsx` | partial migration | uses `@open-hax/uxx` for some components |
| `orgs/open-hax/knoxx/frontend/src/pages/RunsPage.tsx` | legacy custom UI | migrate tables/cards/buttons |
| `orgs/open-hax/knoxx/frontend/src/pages-next/*` | legacy Next-style UI | evaluate selectively |

### Partially migrated

| File | Uses `orgs/open-hax/uxx/`? | Note |
|------|----------------------|------|
| `orgs/open-hax/knoxx/frontend/src/components/WorkspaceBrowserCard.tsx` | **Yes** | New feature built on `Card`, `Button`, `Input`, `Spinner` |
| `orgs/open-hax/knoxx/frontend/src/pages/QueryPage.tsx` | **Partial** | Uses some `@open-hax/uxx` components |
| `orgs/open-hax/knoxx/frontend/src/pages/GardensPage.tsx` | **Partial** | Uses some `@open-hax/uxx` components |
| `orgs/open-hax/uxx/react/src/primitives/ModeIndicator.tsx` | canonical | use in status bars |
| `orgs/open-hax/uxx/react/src/composites/ChordOverlay.tsx` | canonical | integrate into workbench shell |

### Backend-rendered surfaces / other services

| Surface | Status | Note |
|--------|--------|------|
| `services/devel-deps-garden` | separate service UI | treat as a garden surface; migrate only if folded into workbench |
| `services/eta-mu-truth-workbench` | separate service UI | treat as a garden surface; migrate only if folded into workbench |
| `orgs/octave-commons/shibboleth/ui` | separate React app | should eventually adopt `orgs/open-hax/uxx/` primitives or a matching `reagent` layer |

---

## Priority Order

### P0
- ~~`CmsPage.tsx`~~ ✅ **Done** (2026-04-02)
- ~~`IngestionPage.tsx`~~ ✅ **Done** (2026-04-02)
- `QueryPage.tsx`

These are the core workbench surfaces.

### P1
- ~~`ChatLabPage.tsx`~~ ✅ **Done** (2026-04-02)
- `GardensPage.tsx` (already partial)
- `RunsPage.tsx`

### P2
- `pages-next/*`
- external garden surfaces

---

## Definition of Done

This spec is complete when:
- the workbench shell uses `@open-hax/uxx`
- all primary pages use shared primitives for buttons/cards/input/status
- keyboard chords are discoverable through `ChordOverlay`
- modal state is visible via `ModeIndicator`

---

## Status

Specified 2026-04-02. Updated 2026-04-02 (CmsPage migrated). Tracks non-library UI debt explicitly.
