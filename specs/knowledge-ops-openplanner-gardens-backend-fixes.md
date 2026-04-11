# openplanner Gardens Backend Fixes

**Status:** Ready  
**Scope:** `src/routes/v1/gardens.ts`, `src/routes/v1/cms.ts`,
`src/lib/garden-renderer.ts`, `src/routes/v1/public.ts`  
**Priority:** P0 (data corruption), P1 (renderer breakage, latency), P2 (stale stats, HTTP semantics)

---

## Context

Post-merge review of the gardens publication system (openplanner) surfaced
seven issues across four files. Two are data-correctness bugs; the rest are
performance, reliability, or semantic issues.

---

## Issues & Required Fixes

### 1. `cms.ts` — `gardenPublications` array splice corrupts unrelated entries (P0)

**File:** `src/routes/v1/cms.ts` — publish endpoint  
**Current behaviour:** When re-publishing a document to a garden that already
has an entry in `gardenPublications`, the code finds the existing index via
`findIndex`, creates an updated publication object, then writes it using:

```ts
gardenPublications[gardenPublications.length - 1] = updatedPublication;
```

This always overwrites the **last** element of the array, not the element at
`existingPubIndex`. If the document has been published to multiple gardens,
a re-publish to garden A will silently overwrite garden B's publication entry.

**Fix:**

```ts
const targetIndex = existingPubIndex >= 0
  ? existingPubIndex
  : gardenPublications.length; // append

if (targetIndex === gardenPublications.length) {
  gardenPublications.push(updatedPublication);
} else {
  gardenPublications[targetIndex] = updatedPublication;
}
```

**Exit criterion:** A document published to gardens A and B, then re-published
to A, retains its B entry unchanged. Verified by reading `gardenPublications`
from MongoDB after the re-publish.

---

### 2. `cms.ts` — legacy `/cms/publish/:id` reads stale `metadata.garden_id` (P1)

**Current behaviour:** The legacy publish endpoint reads
`metadata.garden_id` (flat string) from the document. Any document created
under the new schema that stores publications in the `garden_publications` array
will silently produce an empty/wrong result with no error surfaced to the caller.

**Fix:** Return a clear 400 response:

```ts
return reply.status(400).send({
  error: 'legacy-endpoint-deprecated',
  message: 'Use POST /v1/cms/documents/:id/publish with a garden_id body parameter.',
});
```

**Exit criterion:** Any call to the legacy endpoint returns 400 with the
migration message. No existing callers in the knoxx frontend use this path.

---

### 3. `cms.ts` — `/cms/stats` full in-memory scan (P1)

**Current behaviour:** The stats endpoint loads up to 1000 documents into
memory to count them by visibility bucket. This is a linear scan; at scale it
will be slow and waste memory.

**Fix:** Replace with a MongoDB `$facet` aggregation:

```ts
const [result] = await app.mongo.documents.aggregate([
  { $match: { project: tenantId } },
  {
    $facet: {
      byVisibility: [
        { $group: { _id: '$visibility', count: { $sum: 1 } } },
      ],
      total: [{ $count: 'n' }],
    },
  },
]).toArray();

const counts = Object.fromEntries(
  result.byVisibility.map((b: { _id: string; count: number }) => [b._id, b.count])
);
return reply.send({ total: result.total[0]?.n ?? 0, byVisibility: counts });
```

**Exit criterion:** Stats endpoint returns correct counts without loading
documents into memory. Response time does not degrade linearly with document
count.

---

### 4. `cms.ts` — `published_by` hardcoded (P2)

**Current behaviour:** Every publish action sets `published_by` to the string
`"openplanner-cms"`. This is incorrect when operator identity or auth is added.

**Fix:** Accept an optional `published_by` field from the request body, with
`"openplanner-cms"` as fallback only:

```ts
const publishedBy = body.published_by ?? request.headers['x-operator-id'] ?? 'openplanner-cms';
```

**Exit criterion:** Publish requests that include `published_by` in the body
or `x-operator-id` header have that value stored in the document. Requests
without either fall back to `"openplanner-cms"`.

---

### 5. `garden-renderer.ts` — shiki regex `[^<]*` breaks on code containing `<` (P1)

**File:** `src/lib/garden-renderer.ts`  
**Current behaviour:** The async shiki post-processing pass uses a regex like:

```ts
/<pre class="shiki-placeholder"[^>]*><code[^>]*>([^<]*)<\/code><\/pre>/g
```

The capture group `[^<]*` stops at the first `<` character. Any code block
containing HTML entities or TypeScript generics (e.g. `Array<T>`, which the
markdown renderer may or may not escape as `Array&lt;T&gt;`) will be silently
truncated — only the portion before the first `<` is highlighted.

**Fix:** Replace the capture group with a non-greedy multi-line match, or
(preferred) accumulate code blocks in a pre-pass array indexed by placeholder
ID, then substitute by ID rather than by re-parsing the rendered HTML:

```ts
// Pre-pass: collect blocks
const blocks: Array<{ lang: string; code: string }> = [];
const html = baseHtml.replace(
  /<!--SHIKI:(\d+)-->/g,
  (_, idx) => `<shiki-slot id="${idx}" />`
);

// Highlight all blocks in parallel
const highlighted = await Promise.all(blocks.map(b => highlighter.codeToHtml(b.code, { lang: b.lang })));

// Substitute
return html.replace(/<shiki-slot id="(\d+)" \/>/g, (_, idx) => highlighted[Number(idx)]);
```

The exact pre-pass injection strategy depends on the existing remark/rehype
pipeline — adapt to match. The key invariant is: **code text must be captured
before HTML rendering, not extracted from HTML after**.

**Exit criterion:** A code block containing `Array<T>`, `<div>`, or
`a < b && b > c` is syntax-highlighted in full without truncation.

---

### 6. `garden-renderer.ts` — `renderGardenIndex` is synchronous (P2)

**Current behaviour:** `renderGardenIndex` returns `string` (synchronous).
`renderGardenPage` returns `Promise<string>`. This asymmetry means the index
page cannot use async syntax highlighting, and any future async rendering
needs applied to the index path will require a breaking signature change.

**Fix:** Make `renderGardenIndex` async now:

```ts
export async function renderGardenIndex(
  garden: GardenRecord,
  docs: DocumentRecord[]
): Promise<string> { ... }
```

Update all call sites in `public.ts` to `await` the result.

**Exit criterion:** Both render functions return `Promise<string>`. Index page
and document page rendering are symmetric.

---

### 7. `public.ts` — dynamic `import()` of garden-renderer adds cold-start latency (P1)

**Current behaviour:** `src/routes/v1/public.ts` uses a dynamic
`import("../../lib/garden-renderer.js")` inside a route handler. This defers
module load to the first HTTP request, adding 50–300ms latency to the first
`/html` hit and potentially throwing an unhandled rejection after the server
has already started if the import fails.

**Fix:** Convert to a static top-level import:

```ts
import { renderGardenPage, renderGardenIndex } from '../../lib/garden-renderer.js';
```

If the dynamic import was introduced to avoid circular dependencies, resolve
those dependencies structurally (move shared types to a `types.ts` module)
rather than by deferring the import.

**Exit criterion:** The first `/api/openplanner/v1/public/gardens/:id/html`
request has no module-load overhead. Server startup fails fast if
`garden-renderer.ts` has an import error.

---

### 8. `gardens.ts` — `DELETE` is a soft-delete, HTTP semantics mismatch (P2)

**Current behaviour:** `DELETE /gardens/:id` sets `status: "archived"` and
returns `{ status: "archived" }`. HTTP `DELETE` conventionally implies
irreversible removal; clients that check status codes will assume the resource
is gone, but a subsequent `GET /gardens/:id` will still return it (with
`status: archived`).

**Options (pick one):**

- **Option A (preferred):** Keep `DELETE` but filter archived gardens from all
  `GET /gardens` list responses by default (`?include_archived=true` to opt in).
  `GET /gardens/:id` on an archived garden returns 404.
- **Option B:** Rename to `POST /gardens/:id/archive` to match the pattern
  already used for `activate` and `stats`. Keep `DELETE` for actual hard-delete
  (admin only, or not exposed).

**Exit criterion (Option A):** `GET /gardens` never returns archived gardens
unless `include_archived=true` is passed. `GET /gardens/:id` for an archived
garden returns 404.

---

### 9. `gardens.ts` — stats always stale (P2)

**Current behaviour:** `GET /gardens/:id` returns `doc.stats` as stored in
MongoDB. Stats are only refreshed by explicitly calling
`POST /gardens/:id/stats`. There is no automatic recalculation on publish or
unpublish, so the count shown in the UI is always out of date.

**Fix:** Call `recalculateStats` (or inline the aggregation) at the end of
the publish and unpublish handlers in `cms.ts`:

```ts
// At end of publish handler:
await recalculateGardenStats(app, gardenId);
```

Alternatively, add `stats_stale: true` to the `GET /gardens/:id` response
so the UI can show a "refresh" button rather than silently displaying wrong
numbers.

**Exit criterion:** After publishing a document to a garden, the garden's
`doc_count` (or equivalent stats field) reflects the new count within the same
request cycle, without requiring a manual stats call.

---

## Implementation Order

1. Fix #1 (`gardenPublications` array corruption) — data correctness, P0
2. Fix #5 (shiki regex) — renderer correctness, P1
3. Fix #7 (static import) — reliability, P1
4. Fix #2 (legacy endpoint 400) — API hygiene, P1
5. Fix #3 (stats aggregation) — performance, P1
6. Fix #8 (DELETE semantics / archived filter) — HTTP correctness, P2
7. Fix #6 (`renderGardenIndex` async) — future-proofing, P2
8. Fix #9 (auto stats refresh) — UX polish, P2
9. Fix #4 (`published_by` passthrough) — auth readiness, P2

---

## Exit Criteria (full)

- [ ] Re-publishing to garden A does not overwrite garden B's publication entry
- [ ] Legacy `/cms/publish/:id` returns 400 with migration message
- [ ] `/cms/stats` uses `$facet` aggregation; no in-memory document scan
- [ ] Code blocks with `<` characters render fully highlighted
- [ ] `renderGardenIndex` is async; both render fns return `Promise<string>`
- [ ] `garden-renderer.ts` is statically imported in `public.ts`
- [ ] `GET /gardens` does not return archived gardens by default
- [ ] Garden stats update automatically after publish/unpublish
- [ ] `published_by` accepts caller-provided identity with fallback
