# Owning a PR from review to merge

Once the user authorizes merging, the agent owns that outcome. The reusable
[skill](../../.agents/skills/pr-review-to-merge/SKILL.md) defines the procedure;
[AGENTS.md](../../AGENTS.md#pr-review-and-merge-ownership) makes it repository
policy. A review-only request, PR decomposition, or skill installation does not
authorize a merge. Keep previously granted authorization across turns.

## Keep one active merge candidate

Use the existing epic/task tracker to record the ordered PRs and their intended
bases. The first candidate must contain its real prerequisites and integrate
with its actual target branch. Make that candidate ready for review and keep
later slices draft. Parallel fixes may be prepared in separate worktrees; one
coordinator propagates commits, requests reviews and advances the stack.

Inspect `autoMergeRequest` when making a PR ready. Knoxx's Auto Merge workflow
can queue a squash merge on `ready_for_review`; required CI and resolved threads
alone do not prove both requested bots completed review. Wait for its enabling
job to finish, use `gh pr merge --disable-auto` for the active PR, and verify the
request is absent before resolving the final conversations. Check again after
any later readiness transition. Preserve the repository's global settings.

Distinguish a source defect from a missing prerequisite in an intermediate
slice, but repair either before that slice merges. Full-stack green results
cannot qualify a smaller tree with unresolved namespaces or incompatible APIs.
Repair a split boundary or land a genuine prerequisite rather than treating
the dependency note as a waiver. Use ordinary commits and merges, explicit
staging, and isolated worktrees. Preserve unrelated work and remote history.

## Review, fix, and follow up

Inspect current GitHub heads, required checks, branch rules, reviewer completions,
and every review conversation. Provide the author's walkthrough required by
AGENTS.md, then request both CodeRabbit and Codex on the current head. An accepted
request, running summary, rate-limit response or old-head review is not a
completed current-head review. Do not duplicate an in-flight request.

For each actionable finding, reproduce and fix it or explain why the claim does
not apply. Reply in its review thread with the commit, relevant test command,
result and exact tested tree. Task comments retain intake and dependencies;
they do not replace thread replies. Ask for reviewer adjudication when disputing
a finding. Resolve only after evidence verifies the fix or the reviewer accepts
the disagreement. An outdated line alone is insufficient evidence.

Any new head needs current-head follow-up from both bots and renewed applicable
checks. A changed base also needs its actual diff and integration rechecked.
Preserve repo-required backend tests, production compilation, warning gates,
and live/browser verification for user-reachable changes. Missing tools, skipped
tests and compile-only runs must remain visible; they cannot stand in for a
required passing check.

## The merge gate

Immediately before merge, confirm all of these against fresh GitHub state:

| Evidence | Requirement |
| --- | --- |
| Authorization | The PR and intended target remain within the user's scope. |
| Head and base | The reviewed head and qualified base are unchanged. |
| Both bots | CodeRabbit and Codex completed review for the current head. |
| Findings | No actionable finding remains; disagreements have been adjudicated. |
| Conversations | Every review conversation is resolved with evidence. |
| Checks | Every required check passes, and required relevant suites ran. |
| Integration | Real prerequisites, branch protection and mergeability are satisfied. |

Never weaken a workflow, suppress a failure, relax branch protection or use an
administrator bypass to satisfy this table. If evidence is absent or stale,
return to the corresponding step.

Use the repository's permitted merge method with an exact-head assertion. For
example, when merge commits are the chosen method, GitHub CLI provides:

```bash
gh pr merge "$pr_number" --repo open-hax/knoxx \
  --merge --match-head-commit "$reviewed_head_sha"
```

Populate both variables from the verified active PR; this example is not a
standing command to merge an arbitrary PR. A refused merge returns to the loop.
If auto-merge or a merge queue is required, continue observing it until GitHub
confirms `MERGED` and supplies the merge commit. Scheduling a merge is not the
completion event.

Inspect the actual PR state during waits. On #329, repository automation merged
the qualified source while CodeRabbit was still reviewing it. Such a merge
requires an explicit review-gap record, completion of the outstanding review,
and a reviewed follow-up for any findings before the next layer advances. A bot
status can also report success for a rate-limited review; read its completion
evidence rather than using the status color as approval.

After confirmation, record the PR/merge commit, retarget the next authorized PR
to the correct surviving base, inspect its actual diff, and repeat qualification.
Do not assume retargeting alone removes predecessor changes after a squash:
repair ancestry through an ordinary integration commit if necessary, then review
the resulting diff. Keep exactly one active merge candidate.

## Continue through waits without noise

Record a compact resumable checkpoint in the owning task or established tracker:

```text
Authorized scope and merge method:
Active PR / task / intended base:
Head SHA / base SHA:
CodeRabbit completion or request URL:
Codex completion or request URL:
Required checks and test evidence:
Unresolved thread URLs and next actions:
Current blocker / quota retry time:
Continuation mechanism / actual schedule identifier:
Next action: fix, review, verify, merge, then advance the named next PR.
```

A quota or pending check changes the next action, not the objective. Reuse or
update an available authorized continuation so it resumes the complete merge
loop. Its first step is a fresh state read; unchanged waits back off. Notify only
for a merge, meaningful progress, a new failure or a required user decision.
If the harness cannot schedule continuation, state that limitation and preserve
the checkpoint rather than claiming background execution.

An individual task completes when its PR is confirmed merged and its required
review/fix loop is closed. The stack task
completes only after all authorized PRs merge, unless the user changes its scope.
Do not label a blocked or waiting stack complete.

## Skill discovery and provenance

Knoxx carries the skill and its machine-readable contract under
`.agents/skills/pr-review-to-merge/`. The project OpenCode entry at
`.opencode/skill/pr-review-to-merge/SKILL.md` is a relative symlink to that same
versioned file. The canonical local installation is
`~/.agents/skills/pr-review-to-merge/`; compatibility links may expose it to Codex
and legacy Pi. Keep the canonical files and project snapshot byte-identical
when updating this workflow. This guide installs no scheduler or GitHub rule.

The user requested this workflow during [PR #305](https://github.com/open-hax/knoxx/pull/305)
decomposition: collecting findings and requesting reviews had not completed the
authorized merge outcome. This policy preserves that ownership beyond review
queues, quotas and turn boundaries.
