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

Nine open PRs carrying the whole contract-owned publication pipeline sit above a
pull request that is closed, unmerged, and — this is the part that matters —
**cannot be reopened**. Nothing in this epic or in
`knoxx-publication-runtime-follow-up` lands until the bottom of the ladder does.

## The state, precisely

`#230` (`feat/publication-intent-resolver`) merged to `main` and its branch was
deleted. `#232` (`feat/publication-state-migration` → `feat/publication-intent-resolver`)
is closed, unmerged, `mergeable_state: dirty`, `closed_at` 2026-08-21T17:38:55Z.

Both repair operations are refused by the API:

```text
PullRequest.base (invalid): Cannot change the base branch of a closed pull request.
PullRequest.state (custom): state cannot be changed.
                            The feat/publication-intent-resolver branch has been deleted.
```

**Do not generalize this into "GitHub closes PRs when their base is deleted."**
It does not. When a pull request merges and its head branch is deleted, GitHub
retargets open PRs that used it as a base onto the merged PR's own base — which
is exactly what should have happened here and demonstrably did not. Everything
above `#232` kept its base and stayed open; the fourteen other open PRs are fine.
What is established is the observed state and the two refusals above, not a rule
about the platform.

The rest of the ladder is intact at the branch level. `#233`'s base,
`feat/publication-state-migration`, still exists with all nine commits, which is
why this is a base problem and not lost work.

## Work

- **Done: `#247`** opens `feat/publication-state-migration` → `main`, same head
  commit, nothing rewritten. It carries `#232`'s review history by reference; that
  history is where the four fold-level findings and the answered-not-fixed entry
  point question live, and it should be read before merging.
- Verify the ladder still applies. Merged in PR order into current `main`, all
  eleven branches apply with **no conflicts** — re-run this rather than trusting
  it, since `main` moves:

  ```text
  publication-state-migration → translation-config-resource →
  translation-publication-gate → publication-reconcile-plan →
  publication-adapter-effects → publication-receipts-proof →
  cms-resource-backed-publication → openplanner-rest-retirement →
  contract-publication-e2e → publication-verification-artifact →
  fastify-null-prototype-params
  ```

- Merge in order, letting each merge retarget the next PR onto `main`. Confirm
  the retarget happened rather than assuming it; `#232` is the evidence that it
  can fail to.
- Expect CodeRabbit to review each PR for the first time as it retargets.
  `.coderabbit.yaml` disables auto review on non-default base branches, so the
  entire stack has run without it — findings will arrive late and in bulk.
- Do not rebase or force-push anything in the ladder. Branches are stacked on
  exact history and a rewrite invalidates every one above it.
- Delete a base branch only after the PR stacked on it has been retargeted and
  confirmed.

## Definition of Done

- `#232`'s content is merged to `main` via `#247`.
- Every remaining PR in the ladder has a base that exists, verified after each
  merge rather than assumed.
- The ladder is merged in order with each PR's own verification re-run.
- A process note records the observed failure and the two API refusals, without
  overstating them as platform behavior.
