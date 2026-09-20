---
name: pr-review-to-merge
description: Own an authorized pull request or ordered stack from review through verified merge, including fixes, current-head bot follow-up, CI, thread resolution, and durable continuation.
license: GPL-3.0-or-later
metadata:
  origin: User-requested workflow extracted from open-hax/knoxx PR 305
---

# PR review to merge

## Use this skill when

- The user authorizes merging a PR or advancing an ordered PR stack.
- The user asks you to resume an already authorized review-and-merge workflow.

## Do not use this skill when

- The request is only to review, split, task out, or prepare draft PRs. Those
  actions do not grant merge authorization.
- The user pauses or cancels merging, or the next PR is outside the authorized scope.

Follow the active harness, user instructions, project AGENTS.md, and applicable
canonical principles. Existing session authorization persists; do not ask again
merely because a turn ended, a reviewer replied, or a quota reset is needed.

For PRs that change deployment qualification, also apply the repository's
promotion workflow. This skill still owns their review and merge; it does not
replace any required deployment qualification or promotion gate.

## Inputs and durable state

Record the authorized scope, repository, ordered PR/task URLs, intended bases,
merge method, and required reviewers. For each PR retain its current head and
base SHAs, current-head review evidence, check results, unresolved thread URLs,
blockers, and the next concrete action. Use the project's existing tracker and
receipt location; do not create a competing queue or record credentials.

## Procedure

1. **Select one active PR.** Inspect live heads, bases, branch rules, required
   checks, review state, and dependency order. Keep later stack layers draft.
   Make the active PR ready for review once its actual prerequisites and base
   integration are present. Parallel workers may prepare independent fixes;
   one coordinator owns branch propagation, external review requests and merge.
   Inspect `autoMergeRequest` and ready-triggered repository automation. If a bot
   queues auto-merge before the review gate, wait for that job to finish, disable
   auto-merge on this PR, and verify it is absent before resolving the final
   conversations. Recheck this after readiness transitions; do not change global
   repository settings or assume a green bot status means its review completed.
2. **Make the active change mergeable.** Reproduce failures and repair code,
   tests, build configuration and integration conflicts within the authorized
   scope. Bring real prerequisites into the active dependency chain or repair
   the split boundary. A green cumulative tree does not prove an earlier layer
   builds. Preserve unrelated work; stage explicit paths, never amend or force
   push, and propagate stack changes with ordinary commits/merges.
3. **Close the review loop.** Read every actionable bot thread, including older
   threads still relevant to the current diff. Fix valid findings and add the
   appropriate regression evidence. Reply in each thread with the fix commit,
   exact tested context and result. If you disagree, explain the code path and
   evidence, request adjudication, and keep it open pending that decision.
   Linking a report to a task is intake, not resolution.
4. **Request current-head follow-up.** Supply an author walkthrough where the
   repository requires it. Request both CodeRabbit and Codex for Knoxx, or all
   reviewers required by the user's workflow elsewhere. Reuse an in-flight
   request for the same head; never duplicate it. A request acknowledgment,
   stale review or rate-limit reply is not completion. After each change,
   obtain each reviewer's completion for the latest head and relevant base.
5. **Resolve verified threads.** Resolve a finding only after the fix and
   relevant checks demonstrate it is addressed, or the reviewer adjudicates
   the documented disagreement. Reply with evidence before resolving. Do not
   resolve an actionable finding merely because it is outdated, copied to a
   task, or inconvenient. Inspect all conversations again after follow-up.
6. **Recheck the merge gate.** Fetch fresh remote state. Require no actionable
   findings, completion by every required reviewer on the current head, all
   required checks passing with applicable suites actually exercised, all
   review conversations resolved, branch protection satisfied, and no unresolved
   prerequisite or integration blocker. Missing/pending evidence is not a pass.
   Do not weaken checks, suppress failures, change protection, use administrator
   bypass, or merge an unqualified layer to advance the queue.
7. **Merge the verified head.** Use an API/CLI operation that asserts the exact
   reviewed head SHA and obeys the repository's merge method and protections.
   If the head/base changes or merge is refused, refresh evidence and return to
   the relevant gate. Queuing auto-merge is still pending work: observe the
   actual GitHub merged state and merge commit before declaring completion.
   Check actual PR state during review waits too. If another actor or automation
   merges early, record the missing evidence, finish the outstanding review and
   land any required follow-up before advancing. Do not report that the intended
   review gate passed merely because the PR merged.
8. **Advance the stack.** Record the merged PR and commit, retarget the next
   authorized layer to the correct surviving base, check its actual diff and
   dependencies, propagate changes without rewriting history, and repeat.
   A squash merge can leave predecessor commits in descendant history; repair
   that ancestry/diff with an ordinary integration commit before review.

## Quotas and continuation

A quota, pending CI run, or unavailable reviewer is a durable wait state, not
completion. Record its evidence, next eligible retry time, and the next action.
Use an available supported continuation mechanism when the user has authorized
continued work; keep its objective **fix, review, verify, merge, and advance**.
Never reduce the objective to collecting review comments or reporting status.
On waking, inspect current state first and resume the owning loop. Back off on
unchanged waits and notify only on meaningful progress, merge, failure or a
required user decision. If no continuation can actually be scheduled, state
that limitation and preserve a resumable handoff; do not promise background work.

## Completion and output

An individual merge task is complete only after GitHub confirms that PR merged.
An authorized stack is complete only after every in-scope PR merged, unless the
user changes the scope. A draft, review request, green suite, resolved-thread
list, or enabled auto-merge is an intermediate state. Report merged PR/commit
links and the next active PR, or the exact blocker and continuation state.

## Provenance and compatibility

This native skill was explicitly requested during the Knoxx PR #305 decomposition
after review collection had not yet delivered the authorized merge outcome. It
does not grant merge authorization by itself. Its activation/governance data is
in [CONTRACT.edn](CONTRACT.edn).

The canonical local catalog is `~/.agents/skills/pr-review-to-merge/`. A project
may carry a versioned copy for a self-contained checkout; keep that copy equal
to the canonical skill rather than editing two independent policies. OpenCode
may discover a project link under `.opencode/skill/pr-review-to-merge/`; legacy
Pi and Codex discovery directories may link to the canonical catalog. Installation
does not imply that an already-running harness has reloaded its skill list.
