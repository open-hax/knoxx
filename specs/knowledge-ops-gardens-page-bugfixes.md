# Gardens Page Bug Fixes

**Status:** Ready  
**Scope:** `frontend/src/pages/GardensPage.tsx`  
**Depends on:** `simple-markdown-gardens.md`, openplanner `gardens.ts` route contract  
**Priority:** P0 (create form is broken), P1 (schema drift, status mismatch)

---

## Context

The `GardensPage` component was shipped with the garden management feature
(commit `908c3b9`). It covers create/edit/delete flows and theme selection.
A post-merge review surfaced six issues, two of which are functional blockers.

---

## Issues & Required Fixes

### 1. Create form always PATCHes — never creates (P0)

**Current behaviour:** `handleSave` always calls
`PATCH /api/openplanner/v1/gardens/:id` regardless of whether the form is in
create or edit mode. When `editingGarden` is `null` the endpoint does not
exist yet and the request 404s silently.

**Fix:** Branch on `editingGarden`:

```ts
if (editingGarden) {
  // PATCH existing
  await fetch(`/api/openplanner/v1/gardens/${encodeURIComponent(formGardenId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, description, theme, domain, status }),
  });
} else {
  // POST new
  await fetch('/api/openplanner/v1/gardens', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ garden_id: formGardenId, title, description, theme, domain, status }),
  });
}
```

**Exit criterion:** Creating a garden from a fresh form results in an HTTP 201
from the backend and the new card appears in the list without a page reload.

---

### 2. Deleted gardens reappear after reload (P0)

**Current behaviour:** `handleDelete` calls the backend `DELETE` route, which
performs a soft-delete (sets `status: "archived"`). The subsequent
`loadGardens()` call fetches all gardens including archived ones, so the card
reappears immediately.

**Fix:** Filter archived gardens out on the client side, or append
`?status=active,draft` to the fetch URL in `loadGardens` once the backend
supports status filtering on list. Until then, filter client-side:

```ts
const visible = body.gardens.filter(g => g.status !== 'archived');
setData({ ...body, gardens: visible });
```

**Exit criterion:** After confirming deletion, the garden card disappears and
does not return on `loadGardens`.

---

### 3. `domain` field renders "undefined" (P1)

**Current behaviour:** The `Garden` TypeScript type declares a `domain` field
and the card renders `Domain: {garden.domain}`. The openplanner garden schema
(post-migration) does not include `domain` in the API response, so the value
is always `undefined` at runtime.

**Fix:** Remove `domain` from the `Garden` type and from the card display. If
domain filtering is desired in future it should be added back to the openplanner
route contract first.

```diff
- domain: string;
```

```diff
- <span className="text-slate-600">
-   Domain: {garden.domain}
- </span>
```

**Exit criterion:** No card displays "Domain: undefined". TypeScript compiles
cleanly with `domain` removed.

---

### 4. Status dropdown offers invalid values (P1)

**Current behaviour:** The form `<select>` for status provides options
`"active"` and `"inactive"`. The openplanner backend accepts
`"draft" | "active" | "archived"`. `"inactive"` is not a recognised value and
will be stored as-is, breaking any backend filtering that relies on the enum.
`"draft"` is missing entirely.

**Fix:** Replace select options:

```tsx
<option value="draft">Draft</option>
<option value="active">Active</option>
```

Do not expose `"archived"` as a create/edit option — archiving is a delete
action, not a status selection.

**Exit criterion:** The status dropdown shows exactly `draft` and `active`.
A garden created with status `draft` is stored with `status: "draft"` in
MongoDB and returned correctly by `GET /gardens`.

---

### 5. `gardenLinks` is dead code (P2)

**Current behaviour:** A `gardenLinks` map (hardcoding localhost ports) is
declared at module level but never read. The "View Garden" button correctly
uses `gardenHtmlUrl` derived from the garden ID.

**Fix:** Delete the `gardenLinks` declaration entirely.

**Exit criterion:** `gardenLinks` does not appear in the file.

---

### 6. `confirm()` for delete is blocking UX (P2)

**Current behaviour:** `handleDelete` calls the native `confirm()` dialog,
which is a synchronous blocking call, fails silently in most iframe/embedded
contexts, and is stylistically inconsistent with the rest of the shell.

**Fix:** Replace with an inline confirmation state:

```ts
const [confirmingDelete, setConfirmingDelete] = useState<string | null>(null);
```

When the Delete button is clicked, set `confirmingDelete` to the `garden_id`.
Replace the Delete button with two inline buttons: **Confirm** and **Cancel**.
Only call the API on **Confirm**.

**Exit criterion:** No `confirm()` call exists in the file. Delete requires
two user interactions before the API call is made.

---

## Implementation Order

1. Fix #1 (create form POST) — unblocks all create testing
2. Fix #3 (remove `domain`) — prevents runtime noise
3. Fix #4 (status enum) — prevents bad data
4. Fix #2 (archived filter) — closes the delete loop
5. Fix #5 (dead code) — cleanup
6. Fix #6 (confirm UX) — polish

---

## Exit Criteria (full)

- [ ] New garden created via form results in 201 and card appears
- [ ] Edited garden saved via form results in 200 and card updates
- [ ] Deleted garden does not reappear after reload
- [ ] No "Domain: undefined" text rendered anywhere
- [ ] Status dropdown only shows `draft` / `active`
- [ ] No `gardenLinks` dead code in file
- [ ] No `confirm()` call in file; inline confirm UX present
- [ ] `tsc --noEmit` passes with no new errors
