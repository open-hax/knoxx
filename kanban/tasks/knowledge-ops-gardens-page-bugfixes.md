---
uuid: "knoxx-knowledge-ops-gardens-page-bugfixes"
title: "Gardens Page Bug Fixes"
status: done
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

**Status:** Done (see implementation note below)  
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

**Implemented 2026-05-29 (review → done):** Fixed all six issues in `frontend/src/pages/GardensPage.tsx`, cross-checked against the openplanner gardens contract (`openplanner/src/routes/v1/gardens.ts`):

1. **Schema drift — `domain`:** The garden contract has no top-level `domain` field (it only exists nested under `source_filter.domain`; the `GardenResponse` never returns a top-level `domain`, and both POST/PATCH ignore it). Removed `domain` from the `Garden` type, the `formDomain` state, the `startEdit` assignment, both `handleSave` request bodies, the form's Domain `<Input>`, and the card's `Domain: {garden.domain}` line (which had been rendering `undefined`).
2. **Dead code:** Removed the unused `gardenLinks` constant.
3. **Status mismatch:** The POST `CreateGardenPayload` does not accept `status` — the backend always creates gardens as `"draft"` and only PATCH/activate can change status. So sending `status` on create is a no-op that misleads the operator. Made the Status `<select>` edit-only (rendered when `editingGarden` is set), kept `status` in the PATCH body, and added the contract's `archived` option. POST body no longer carries an ineffective `status`/`domain`.
4. **Unsafe `confirm()`:** Replaced the blocking `window.confirm()` in `handleDelete` with `[confirmingDelete, setConfirmingDelete]` state and inline Confirm Delete / Cancel buttons on each card.
5. Updated `GardensPage.test.tsx` to match the corrected behavior (removed `domain` from fixtures and the POST assertion; deletion test now clicks the inline Confirm Delete button instead of stubbing `confirm`).

**Verification:**
- `pnpm -C frontend typecheck` (tsc --noEmit) => exit 0, no errors.
- `NODE_ENV=test vitest run src/pages/GardensPage.test.tsx` => 2 passed (create posts payload without domain; delete requires inline confirmation). (Note: running vitest without `NODE_ENV=test` fails with React's "act(...) not supported in production builds" — that is an environment/shell issue, not a code issue.)

---

