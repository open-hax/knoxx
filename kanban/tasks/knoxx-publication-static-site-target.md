---
uuid: knoxx-publication-static-site-target
title: Publication — the static-site target adapter
status: ready
priority: P1
points: 8
labels:
  - tasks
  - publication
  - adapters
  - website
  - has-parent
---

# Publication — the static-site target adapter

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

The first `IPublicationTarget` that writes bytes a person can load in a browser.
Everything above the effect boundary is already proven against a fake; this card
is where the protocol finds out whether it was honest.

## Dependencies

`knoxx-publication-artifact-contract`, `knoxx-publication-target-registry`, and
**`services-website-content-root`** — the transport this adapter uses is decided
by whether Knoxx and the website share a host. Do not start before that answer.

## Work

- Write an artifact to a content root at a path derived from document × locale ×
  concrete revision, and maintain a manifest that maps public paths to the
  artifact currently serving them.
- **The manifest is the published fact.** A file on disk that no manifest entry
  names is not public, so a partially written artifact cannot be served. Write
  the artifact first, then update the manifest as the commit point.
- Update the manifest atomically — write beside and rename — so a reader never
  observes a half-written manifest. A static file server has no read lock.
- Implement `observe!` by reading the manifest, keyed on `:publication/id`, not
  on the desired path. `publication-target-memory` documents exactly why: keying
  on path means that after a path move the caller cannot see the route it is
  replacing, `:previous` comes back nil, and both routes stay public.
- Implement `remove!` so the route leaves the manifest and the publication id is
  carried on the receipt — without it a publish-then-remove history still reports
  the old route as materialized, and a later republish of the same revision reads
  as `:noop` with nothing public.
- Reclaim orphaned artifact files, or state in the namespace docstring that
  content is retained deliberately and what bounds the growth.
- Implement `IIdempotencyStore` against the same root with the same atomic
  reservation contract: one operation claims the key, with no await between
  reading it and claiming it. A separate check-then-write can publish twice and
  the second publication is unrecoverable.
- Concurrency is real here in a way it is not in the memory target: two
  reconciler runs, or a run overlapping a deploy, touch the same directory.
- An empty content root is a valid initial state, not an error.

## Definition of Done

- A published document is fetchable at its manifest path with the expected bytes.
- Replay is a `:noop` and does not rewrite the artifact.
- A path move leaves exactly one public route.
- A removal makes the route stop serving and is visible to `observe!`.
- A crash between artifact write and manifest update leaves nothing public and
  the next run converges.
- Interrupted mid-write, the manifest is either the old one or the new one.
- The adapter contains no admissibility, gating, or planning logic.
