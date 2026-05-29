---
uuid: "knoxx-knowledge-ops-workbench-ui"
title: "Knowledge Ops — Unified Knowledge Workbench UI Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.393Z"
source: "specs/epics/knowledge-ops-workbench-ui.md"
points: null
category: epics
---

# Knowledge Ops — Unified Knowledge Workbench UI Spec

> Source: `specs/epics/knowledge-ops-workbench-ui.md`

## Current Canonical Reading

- state: current UI direction, partially updated to Knoxx vocabulary
- **IMPORTANT (2026-04-12)**: This spec originally described the workbench as a "unified replacement interface." This interpretation is INCORRECT. The workbench is ONE workplace among many in the larger Knoxx app. See `workbench/0.1-shell-layout.md` for the corrected architecture.
- canonical Knoxx workbench terms today:
  - left rail = **Context Bar**
  - center = **Agent Runtime** (or Main Canvas)
  - right rail = **Scratchpad** (or Inspection Panel)
- interpret older `Canvas` language in this doc as the predecessor of the current Knoxx **Scratchpad** surface

> *One interface. File explorer ingestion, collection chat, labeling, and synthesis canvas. Every panel shares the same data sources
