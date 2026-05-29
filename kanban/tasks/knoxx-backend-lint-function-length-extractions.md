---
uuid: "knoxx-knoxx-backend-lint-function-length-extractions"
title: "Knoxx Backend Lint — Function Length Extractions"
status: review
priority: P1
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-function-length-extractions.md"
points: 5
category: tasks
---
# Knoxx Backend Lint — Function Length Extractions

> Source: `specs/tasks/knoxx-backend-lint-function-length-extractions.md`
> Points: 5

Date: 2026-05-27
Status: review
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Bring functions over the configured length budget back under lint limits by extracting readable, named helpers that preserve behavior and architecture boundaries.

## Problem

The hard-error first pass left 33 function-length errors at the `>=60` line threshold and 134 function-length warnings at the `>=30` line threshold. Long route registration functions and large orchestration functions make symbol drift, async cleanup, and coverage characterization harder.

After the first twenty extraction slices, lint reports 12 function-length errors and 1773 total warnings.

---

**Triage 2026-05-28 (in_progress → review):** Lint now reports **0 function-length errors** (the `>=60` budget is fully satisfied) and total warnings are down to 1461. Only ~7 function-length *warnings* (`>=30`) remain — e.g. `normalize-agent-spec`, `walk-audio-files!`, `value->preview-text`, `upsert-openplanner-document!`, `update-resource-id-in-edn-text`. The hard goal is met; moving to review for a final pass on the handful of remaining warning-level long functions before closing.

