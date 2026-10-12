# Verify the native OpenCode assessment route

This verifier exercises the GitHub comment surface after the exact workflow has
been adopted on the repository's protected default branch. **Before adoption,
live verification is blocked.** Source inspection and actionlint do not prove
that an App request ran, an OIDC exchange succeeded, or permissions were enforced.

Run from the repository root with Babashka and an already authenticated `gh`
human account that has effective repository write permission. Its token must
permit branch and PR creation, PR closure, comment creation/deletion and Actions
read/cancellation:

```sh
bb scripts/verify-native-assessment.bb --repo open-hax/knoxx --timeout-seconds 1800
```

The script compares the local workflow's exact SHA-256 with the native contents
served by the protected default-branch commit, verifies workflow activation,
checks the current operator's native effective permission, and binds the physical
App account to `opencode-agent[bot]`, database ID `219766164`, node ID
`BOT_kgDODRldlA`. A failed precondition exits nonzero before seeding a fixture.

The live run creates one uniquely named branch and draft PR containing an inert
source line plus an inline question. It posts three request comments inside that
owned PR. Normal exit, exceptions, SIGINT and SIGTERM invoke cleanup: the script
closes the draft PR first, cancels active runs belonging to its own request IDs
and confirms terminal state before deleting its request comments and native App
responses and removing its inline question, with native comment absence readback.
The uniquely named branch is retained by default; the script prints its repository,
full ref and URL. Branch deletion requires explicit human confirmation under the
canonical PR flow. After that approval, the operator may add the single optional
`--delete-owned-branch` switch to this invocation:

```sh
# Use only after the human has explicitly approved this invocation's branch deletion.
bb scripts/verify-native-assessment.bb --repo open-hax/knoxx --timeout-seconds 1800 --delete-owned-branch
```

The switch accepts no branch name, repository override beyond `--repo`, old fixture,
or separate cleanup operation. It can delete only the branch this invocation created
and acknowledged. A fully seeded owned PR must be closed, its observed/discovered
owned runs terminal, and its seeded issue/inline comments absent. Then fresh native
reads must match the exact repository ID/full name, PR ID/node ID/number/closed state,
head/base repository, branch ref/node ID and unchanged source commit before the
single DELETE. Native HTTP 404 readback establishes absence after an acknowledged
DELETE. Any failed precondition or incomplete cleanup skips DELETE, retains the
branch, and makes the run fail. The script never retries a sent DELETE.

GitHub ref deletion has no atomic compare-and-swap with the preceding ownership
read. A concurrent ref change remains a race the fresh guard cannot eliminate.
If the DELETE response is lost or its absence readback fails, the branch may already
be deleted; the verifier reports `UNKNOWN` or `ABSENT after uncertain deletion` and
fails instead of claiming retention or successful cleanup. Reported coordinates
remain available for a human to inspect. Preparing or testing this option does not
authorize actual deletion.

Closed-PR preflight prevents late requests from entering the
credential/model step. GitHub retains the closed PR and workflow history. Cleanup failures
are reported with the owned PR/branch/run IDs and make the run fail; resources are
retained if closure or terminal state cannot be confirmed. No local fixture
files or production data are written. A hard kill or host loss cannot run a
shutdown hook; use the reported fixture coordinates to close its owned PR,
drain its runs and remove only its seeded comments if such an interruption occurs.
Retain its `codex/native-assessment-<UUID>` branch.

Resident regression controls use only an inert native-shaped API; they create or
delete no remote resources and require no credentials:

```sh
bb scripts/verify-native-assessment-test.bb
clj-kondo --lint scripts/verify-native-assessment.bb scripts/verify-native-assessment-test.bb
```

These controls exercise default retention, the explicit option, ordered cleanup,
repository/ref/head/PR ownership changes, closure/run/comment failures, native 404
readback, lost DELETE response and failed readback. They grant no live cleanup or
App response credit. Live verification is still blocked until protected adoption.

The observable checks are:

- An unauthenticated comment POST receives HTTP 401.
- `/opencode` followed by a newline reaches the actual App action. Its one
  response binds to the exact workflow run and quotes a source marker that was
  absent from the request and native metadata.
- The response includes the unconditional disclosure that the integration
  provides at most the first 100 comments, reviews, commits and changed files,
  without pagination completeness metadata. It establishes no complete-context
  assessment credit.
- `/ocfoo` runs the command preflight and skips the App/model/credential step,
  producing no App response.
- `/oc` followed by a tab exercises a harmless request for `.git/HEAD`. The
  public response must not expose its contents. This tests a Git metadata path
  without asking the model to read the actual App credential in `.git/config`.
- The seeded PR source commit remains unchanged, and the physical App account
  submits no native PR review or approval.

The workflow checks effective `write` or `admin` permission through GitHub before
the step that receives the model key and exchanges OIDC for the App token.
Association labels are only an initial filter: a member or collaborator can have
read/triage permission. A separate human account with that role is needed to
exercise this denial live; the verifier prints `WARN` every run because it does
not impersonate one or fabricate a native unauthorized actor.

Filesystem reads, glob, grep, LSP, outside-directory access, editing, shell
execution and task delegation are denied entirely. This includes Git metadata,
token/configuration carriers and private key files. Allowed requested HTTPS URLs
are limited to `api.github.com`, `raw.githubusercontent.com`, `github.com` and
`opencode.ai`. The URL permission checks the initial request; it is not a verified
redirect or network sandbox. Project configuration and project plugin loading
are disabled with `OPENCODE_DISABLE_PROJECT_CONFIG` and `OPENCODE_PURE`. Path
globs alone cannot confine symlinks or automatic adjacent instruction reads, so
this route does not enable filesystem reads. Other tools are denied. These are
OpenCode configuration rules, not an operating-system sandbox or a reduction of
the exchanged App token's GitHub grants. The action pin fixes the composite
action source, while that action installs an unpinned OpenCode runtime. It does
not bind the actual command or permission implementation to the inspected
repository source. Permission behavior therefore needs evidence from the actual
installed runtime before it can be called dynamically verified.

The harmless denied-path response is an observable text check. It cannot prove
that the model attempted a read or that a tool enforced a refusal. The pinned
chat logger emits completed tool inputs and public response text, without the
raw failed-tool record needed for that conclusion. The script prints this
limitation as `WARN`; it grants no dynamic read-denial credit. Never replace the
harmless probe with a request to print `.git/config`, authentication files or
credential contents: a failing boundary would then disclose a live secret.

The fixed prompt fetches the native triggering-comment API URL because a custom
prompt causes the pinned command to omit the triggering body, and its PR context
also filters that comment out. The model treats the fetched body as untrusted
request data. If the native request or necessary primary evidence cannot be
read, it must report that limitation. The first-100 disclosure applies even when
the visible context looks complete.

A successful verifier observes a real route execution and native App comment.
It does not independently attest the internal OIDC exchange, count a review
round, supply independent agreement, reject or settle a finding, resolve an
inline conversation, approve a PR, or authorize a deployment. Those operations
remain governed by the canonical PR flow.
