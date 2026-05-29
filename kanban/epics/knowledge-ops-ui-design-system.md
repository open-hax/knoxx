---
uuid: "knoxx-knowledge-ops-ui-design-system"
title: "Knowledge Ops — UI Design System"
status: accepted
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.393Z"
source: "specs/epics/knowledge-ops-ui-design-system.md"
points: null
category: epics
---
# Knowledge Ops — UI Design System

> Source: `specs/epics/knowledge-ops-ui-design-system.md`

> *Monokai. Spacemacs chords. Vim modality. Every action has a button, every button has a chord. Mouse users never feel lost, keyboard users never reach for the mouse.*

---

## Component Library

All UI implementations use `@open-hax/uxx` (or `@open-hax/uxx-reagent` for ClojureScript).

### Package structure

```
orgs/open-hax/uxx/
├── tokens/          ← @open-hax/uxx/tokens (colors, spacing, typography, keybindings)
│   └── src/
│       ├── colors.ts        ← Monokai palette + semantic aliases
