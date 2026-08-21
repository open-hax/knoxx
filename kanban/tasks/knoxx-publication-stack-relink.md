---
uuid: knoxx-publication-stack-relink
title: Publication — relink the stranded PR stack onto main
status: ready
priority: P0
points: 2
labels:
  - tasks
  - publication
  - process
  - has-parent
---

# Publication — relink the stranded PR stack onto main

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Nine open PRs carrying the whole contract-owned publication pipeline are stacked
behind a pull request that is closed and unmerged. Nothing in this epic — or in
`knoxx-publication-runtime-follow-up` — can land until the chain is relinked.

## The state, precisely

`#230` (`feat/publication-intent-resolver`) merged to `main`, and its branch was
deleted. GitHub closes any open PR whose base branch is deleted, so `#232`
(`feat/publication-state-migration` → `feat/publication-intent-resolver`) was
auto-closed at 2026-08-21T17:38:55Z, seconds after that merge. It is closed,
unmerged, and `mergeable_state: dirty`.

`#233` is based on `feat/publication-state-migration`. So the chain

```text
#233 -> #234 -> #235 -> #236 -> #237 -> #239 -> #240 -> #241 -> #242, #243
```

now stands on a branch whose own PR no longer exists. Each of those PRs still
reports `mergeable_state: clean` against its immediate base, which is why this is
invisible from any single PR's page: every link is green and the bottom of the
ladder is gone.

`#232` is 1341 additions across 9 files and is the last P0 of the parent epic —
the migration the CMS cutover waits on. It is not abandoned work.

## Work

- Reopen `#232` and retarget its base to `main`, or open a replacement PR from
  `feat/publication-state-migration` to `main` if the closed PR cannot be
  reopened.
- Resolve the `dirty` state against current `main` by merging `main` into the
  branch. Do not rebase or force-push: several branches above it are stacked on
  this exact history and a rewrite invalidates every one.
- Verify each subsequent PR's base still names an existing branch, walking
  upward, and retarget any other link the deletion broke.
- Re-run the stack's own gates on the relinked bottom before merging upward.
- Land in order. Every PR in the chain documents that its diff only reads
  correctly after its base.
- Delete branches only after the PR stacked on them has been retargeted.

## Definition of Done

- `#232`'s content has an open PR against a branch that exists, or is merged.
- Every open PR in the chain has a base branch that exists.
- The chain merges to `main` in order with each PR's own verification re-run.
- A note in the repo's process docs records that deleting a base branch
  auto-closes stacked PRs, so the next stack does not lose its bottom silently.
