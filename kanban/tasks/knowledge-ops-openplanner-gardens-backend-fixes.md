---
uuid: "knoxx-knowledge-ops-openplanner-gardens-backend-fixes"
title: "openplanner Gardens Backend Fixes"
status: review
priority: P2
labels: ["tasks"]
created_at: "2026-05-28T22:40:14.399Z"
source: "specs/tasks/knowledge-ops-openplanner-gardens-backend-fixes.md"
points: null
category: tasks
---

# openplanner Gardens Backend Fixes

> Source: `specs/tasks/knowledge-ops-openplanner-gardens-backend-fixes.md`

**Status:** Review (see triage note below)  
**Scope:** `src/routes/v1/gardens.ts`, `src/routes/v1/cms.ts`,
`src/lib/garden-renderer.ts`, `src/routes/v1/public.ts`  
**Priority:** P0 (data corruption), P1 (renderer breakage, latency), P2 (stale stats, HTTP semantics)

---

## Context

Post-merge review of the gardens publication system (openplanner) surfaced
seven issues across four files. Two are data-correctness bugs; the rest are
performance, reliability, or semantic issues.

---

**Triage 2026-05-28 (todo → review):** All four files exist and the gardens publication system is fully implemented — `gardens.ts` (≈406 lines, CRUD + stats + per-garden docs), `cms.ts` (≈554 lines, publish workflow + translation queueing), `garden-renderer.ts` (≈1012 lines, themed HTML rendering), `public.ts` (≈583 lines, public endpoints + language negotiation). However, a code read suggests a few of the seven issues may still be live: `cms.ts` publish creates translation jobs synchronously in a loop (latency); `public.ts getAvailableLanguages()` returns only the source language and doesn't query `translation_segments`; `gardens.ts` stats `last_published_at` can be undefined. Moving to review — the bulk landed, but the owner should confirm/close the two data-correctness bugs and the latency item specifically.

---

