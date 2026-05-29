---
uuid: "knoxx-knowledge-ops-gardens-page-bugfixes"
title: "Gardens Page Bug Fixes"
status: review
priority: P2
labels: ["tasks", "has-parent"]
created_at: "2026-05-28T22:40:14.398Z"
source: "specs/tasks/knowledge-ops-gardens-page-bugfixes.md"
points: null
category: tasks
---

# Gardens Page Bug Fixes

> Source: `specs/tasks/knowledge-ops-gardens-page-bugfixes.md`
> Parent: `**`

**Status:** Review (see triage note below)  
**Scope:** `frontend/src/pages/GardensPage.tsx`  
**Depends on:** `simple-markdown-gardens.md`, openplanner `gardens.ts` route contract  
**Priority:** P0 (create form is broken), P1 (schema drift, status mismatch)

---

## Context

The `GardensPage` component was shipped with the garden management feature
(commit `908c3b9`). It covers create/edit/delete flows and theme selection.
A post-merge review surfaced six issues, two of which are functional blockers.

---

**Triage 2026-05-28 (todo → review):** `frontend/src/pages/GardensPage.tsx` (≈530 lines) is fully built — create/edit/delete flows, theme selector, and a status selector are all present. The two functional blockers appear addressed: the create form wires every schema field in `handleSave()` (≈lines 181–235), POSTs to `/api/openplanner/v1/gardens`, and the status field is included in the update payload. Moving to review rather than done because I verified the page exists and the create/schema paths are wired, but did not independently confirm each of the six listed issues. Owner should tick off the remaining four P1/P2 items (schema drift, status mismatch, etc.) and close.

---

