---
uuid: "knoxx-knoxx-session-lake-graph-and-memory"
title: "Knoxx Session Lake + Graph Memory + Active/Passive RAG Alignment"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.395Z"
source: "specs/epics/knoxx-session-lake-graph-and-memory.md"
points: null
category: epics
---

# Knoxx Session Lake + Graph Memory + Active/Passive RAG Alignment

> Source: `specs/epics/knoxx-session-lake-graph-and-memory.md`

## Status
- state: working spec / implementation note
- date: 2026-04-04
- scope: Knoxx session isolation, session graph projection, graph query tool, OpenPlanner lake cleanup, and ingestion backpressure

## Problem

Knoxx session memory is being cross-contaminated by non-session data because historical Knoxx events and workspace data were sharing broad project/lake boundaries.

At the same time, the system already has the right organs for a better shape:

- `OpenPlanner` as canonical lake
- `Knoxx` as agent/runtime/session producer
- `kms-ingestion` as canonical `devel` producer
- `Myrmex + ShuvCrawl` as canonical `web` producer
