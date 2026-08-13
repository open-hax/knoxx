---
uuid: "knoxx-publication-duplicate-identity"
title: "Duplicate publication ids are never detected; lookups resolve by file enumeration order"
status: incoming
priority: P1
labels: ["tasks", "3sp", "has-parent", "publication", "resources", "correctness"]
created_at: "2026-08-13T00:00:00Z"
points: 3
category: tasks
epic: "knoxx-contract-owned-publication-pipeline"
---

# Duplicate publication ids are never detected; lookups resolve by file enumeration order

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## The gap

Documents and gardens are indexed through `index-canonical!`
(`backend/src/cljs/knoxx/backend/domain/publication_resolver.cljs`), which
throws when two files declare the same canonical id with different payloads.

Publications are not. `index-one` conjes every intent onto a vector, and the
only uniqueness check is `publication-conflicts`, which groups by
**`publication-key`** — the relation `document × garden × locale × revision`.

Two files can therefore declare the same `:publication/id` with different
revisions, and both land in the index. Nothing reports it. The projection
answers `200`.

Every lookup by id then takes the first match. In
`infra/routes/cms_publication.cljs`:

```clojure
(->> (:publications index)
     (filter #(= publication-id (:publication/id %)))
     first)
```

Which one that is depends on the order the filesystem enumerated the files.

## Why this matters more than it looks

`infra/routes/publications.cljs` deliberately reads the **undeduped** record
list, and its docstring says why:

> `load-all-resources!` applies first-wins `[kind id]` dedup, which would
> collapse two files declaring the same canonical id with different payloads
> into whichever the filesystem enumerated first — making the resolver's
> deterministic identity-conflict detection unreachable and the topology
> dependent on directory order.

For documents and gardens that is accurate. For publications there is no
identity-conflict detection to reach, so the undeduped list buys nothing and
the stated failure mode happens anyway — one layer further in.

A `PATCH` to a duplicated id writes state to whichever intent sorted first,
while the CMS may be displaying the other.

## Found by

`scripts/verify-publication-epic.sh` §6, run against a live backend on
2026-08-13. That section seeds two files claiming `knoxx.verify/probe-es` with
different revisions and expects `409`. It gets `200`. **The check is currently
red on purpose** and should go green with this card.

## Outcome

Two resources claiming the same canonical publication id is an error, reported
the same way a duplicate document or garden id is.

## Scope

- Detect duplicate `:publication/id` across the resource graph and fail the
  projection with a conflict, the way `index-canonical!` already does for
  documents and gardens.
- Decide whether identical duplicate payloads collapse silently (as
  `index-canonical!` allows for documents) or are also an error.
- Correct the `resource-records!` docstring so its claim matches what the
  resolver actually enforces.
- Keep relation conflicts (`publication-conflicts`) as a separate, still-valid
  check — these are two different invariants, not one.

## Non-goals

- Changing `publication-key`. Two intents targeting different revisions of the
  same document/garden/locale remain distinct relations; that is correct.
- Reworking dedup in the shared contract loader.

## Acceptance criteria

- Two files declaring the same `:publication/id` with different payloads make
  the projection fail with a conflict naming that id.
- The conflict is reported in a stable order, independent of file enumeration,
  as the existing conflict reporting already guarantees.
- Relation conflicts continue to be detected independently.
- `scripts/verify-publication-epic.sh` §6 passes.
- A unit test covers duplicate id with differing payloads, and one covers
  duplicate id with byte-identical payloads under whichever rule is chosen.

## Verification

- Run `scripts/verify-publication-epic.sh` against a live backend; §6 goes
  green with no other section regressing.
