---
uuid: knoxx-publication-lossless-edn-state-edits
title: Publication — Lossless EDN State Edits
status: ready
priority: P1
points: 3
labels:
  - tasks
  - publication
  - has-parent
---

# Publication — Lossless EDN State Edits

> Parent epic: `knoxx-publication-runtime-follow-up`

## Purpose

Replace value-level whole-manifest rewrites with syntax-preserving targeted edits for human-maintained publication/resource EDN.

## Work

- Locate publication/CMS state mutation paths that parse EDN and rewrite with `pr-str` or equivalent whole-value serialization.
- Introduce a syntax-preserving edit seam using `rewrite-clj` or an equivalent zipper representation.
- Preserve comments, unrelated resources, ordering, and surrounding formatting where practical.
- Fail closed on malformed/unreadable state; never reconstruct a partial manifest from a failed read.
- Add fixture and property/round-trip tests covering comments, sibling resources, documents, gardens, and targeted state transitions.

## Definition of Done

- A targeted publication state mutation changes only the intended semantic node.
- Comments and unrelated resource syntax survive the edit.
- Malformed state produces no write.
- Existing publication identity/manifest-preservation tests remain green.
- Backend test/lint/typecheck gates for touched code pass.
