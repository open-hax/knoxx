---
category: "tasks"
labels: ["tasks", "3sp", "resources", "http", "addressing"]
points: "3"
title: "Address namespace-qualified resource ids safely in the admin CRUD routes"
priority: "P3"
status: "incoming"
uuid: "knoxx-resource-route-qualified-id-addressing"
created_at: "2026-08-13T00:00:00Z"
---

# Address namespace-qualified resource ids safely in the admin CRUD routes

> Split out of `knoxx-publication-resource-contracts` on 2026-08-13. Recorded
> there as explicitly out of scope for that card's Malli shapes.

## Purpose

`document`, `garden`, and `publication` are the first resource kinds whose ids
are namespace-qualified keywords (`:knoxx.docs/translation-pipeline`). Every
other resource kind uses an unnamespaced id. The admin CRUD route surface was
built for the unnamespaced case and silently misbehaves for the new kinds.

## Observed gaps

Both in `backend/src/cljs/knoxx/backend/infra/routes/resources.cljs`:

- `parsed-resource-id` has no case for `"documents"` / `"gardens"` /
  `"publications"`, so the route's record-id-vs-route-id agreement guard
  silently no-ops for these kinds — a payload whose record id disagrees with
  the route id is accepted.
- `safe-resource-id!` rejects any `/`, so a qualified id like `:tenant/bar`
  cannot be used as the route's resource-id segment as-is. There is currently
  no encoding convention for putting a qualified id in a path segment.

## Scope

- Decide one path-safe addressing convention for namespace-qualified resource
  ids and apply it consistently to read, write, and delete.
- Extend `parsed-resource-id` to cover the three new kinds so the agreement
  guard actually fires.
- Keep the convention consistent with the resource **wire** identity
  convention already fixed by the publication cards: a qualified keyword
  serializes as `namespace/name` with no EDN leading colon.
- Decide deliberately whether the path segment carries the qualified id
  percent-encoded, or whether namespace and name become two segments.

## Non-goals

- Do not change the Malli resource contracts in `law/publication.cljs`.
- Do not couple this to the publication facade. The CMS cutover card
  (`knoxx-cms-resource-backed-publication-ui`) reads and writes through
  `/api/publications/...`, not admin CRUD, so that card is not blocked by
  this one.

## Done when

- A qualified id round-trips through the admin CRUD route path without
  ambiguity, and a test proves the exact encoded form.
- A payload whose record id disagrees with the route id is rejected for
  `documents`, `gardens`, and `publications`, with a test per kind.
- Two ids differing only by namespace (`:tenant-a/foo` vs `:tenant-b/foo`)
  address distinct records through the route surface.

---

Split from `knoxx-publication-resource-contracts` 2026-08-13 while closing that
card. Not on the `knoxx-contract-owned-publication-pipeline` critical path;
sized P3 for that reason. Origin is a Codex review finding on PR #227.
