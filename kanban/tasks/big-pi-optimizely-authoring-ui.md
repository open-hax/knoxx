---
uuid: "big-pi-optimizely-authoring-ui"
title: "Provide a usable and accessible CMS authoring interface"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Provide a usable and accessible CMS authoring interface

> Parent epic: `big-pi-optimizely-compatibility`

As an editor, I can find content, inspect source and revision history, edit language drafts and see conflicts without needing to construct API requests.

**Acceptance**

- Support accessible keyboard and screen-reader journeys for content discovery, types, locales, editing and exact revision selection; show errors, draft status and stale or conflicted writes visibly.
- Preserve all unedited supported properties, refuse unsupported structured field mappings without loss, and clearly distinguish synthetic local data from connected tenant data.
- Provide a self-contained browser tour that seeds and cleans fixtures, verifies the checkout, captures screenshots, exercises failure and authorization cases, and names remaining gaps.

**Evidence**: browser and accessibility tests, screenshots, runnable verification script and manually reviewed editing and conflict journeys.
