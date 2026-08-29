---
uuid: "knoxx-evaluation-case-contracts"
title: "Define generic evaluation case, artifact, rubric, judgment, and receipt contracts"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "evaluation", "review", "contracts"]
created_at: "2026-08-13T00:00:00Z"
points: 5
category: tasks
---
# Define generic evaluation case, artifact, rubric, judgment, and receipt contracts

> Parent epic: `knoxx-evaluation-review-system`

## Purpose

Extract the durable semantics currently implicit in translation review into a generic
evaluation model that can also describe review/labeling of arbitrary agent task inputs
and outputs.

The model must not assume a translation domain or a left/right comparison UI.

## Scope

Define pure Malli contracts and laws for:

- `ReviewCase` — identity, subject, artifact refs, rubric ref, requested judgments;
- `EvaluationArtifact` — identity/ref plus semantic role such as source, candidate,
  reference, evidence, previous result;
- `Rubric` / requested judgment descriptors;
- `Judgment` — one atomic determination with optional rationale/evidence refs;
- `Correction` — a proposed replacement/improvement tied to an artifact;
- `Decision` — accept/reject/defer or a domain-defined decision value;
- `EvaluationReceipt` — immutable durable fact connecting reviewer/actor, case,
  judgments, corrections/decisions, and the evaluated artifact versions.

Translation-specific fields such as locale and terminology remain in translation
artifacts/context or an adapter-owned extension map; they are not required by the
generic core.

Each requested judgment descriptor has a stable obligation id; exact case/rubric/artifact
version refs; allowed judgment or decision values; required reviewer roles and quorum; an
explicit compatibility/exclusivity group; and the roles allowed to adjudicate that group.
The generic status fold consumes those fields directly rather than inferring completion from
the number of receipts or from UI workflow state.

Every allowed value declares a required completion effect: `:satisfies` or `:keeps-pending`;
there is no default. The built-in `:defer` value is always `:keeps-pending`, never evidence that
an obligation is satisfied. Its receipt remains durable and the case remains discoverable as
pending work (with the defer reason/evidence); only a later satisfying judgment or valid
supersession can advance it.

Quorum is counted per obligation and required role over **distinct authenticated principal
identities**, using the immutable principal/role facts recorded on admitted receipts. Multiple
receipt ids or revisions from one principal contribute at most one vote to that obligation-role
requirement; a replacement/correction may change that principal's effective judgment but
cannot manufacture another reviewer. A principal may satisfy a different required role only
when its authenticated role facts and the rubric's role-overlap policy explicitly allow it.

A changed vote is another immutable receipt with obligation-scoped supersession entries of
`{obligation-id, prior-receipt-id, prior-judgment-id}`. Each target must be the current
unsuperseded judgment for the same organization, case/rubric/artifact versions, obligation,
authenticated principal, and reviewer role. Admission validates and advances every named
principal/obligation/role head atomically or appends nothing: two concurrent successors cannot
both win. The status fold uses the one unsuperseded judgment leaf per obligation and retains
every receipt/ancestor as history; correcting obligation A does not suppress unrelated judgment
B carried by the same prior receipt. It never chooses by wall clock. Multiple judgments without
a valid supersession relation remain coexisting evidence and conflicting exclusive values yield
`:needs-adjudication`. Cross-principal, cross-obligation, cross-version, already-superseded,
missing-target, and cyclic relations fail validation without appending.

## Laws

- Artifact roles are semantic, not layout coordinates (`:source` / `:candidate`, never
  canonical `:left` / `:right`).
- A receipt binds to immutable artifact/version identities so later replacement output
  does not inherit an old approval accidentally.
- A correction never destructively replaces the candidate evidence it corrects.
- Rubric identity/version is part of the receipt when it can change the meaning of a
  judgment.
- Generic evaluation law does not depend on frontend, MCP, worker, publication, or
  translation provider namespaces.
- Multiple evaluations may coexist; disagreement is historical evidence and may later
  be adjudicated rather than overwritten.
- Case completion is a pure status fold over the immutable case/rubric/artifact versions and
  their receipts. It returns `:pending` while any requested judgment or reviewer-role
  obligation is missing, `:needs-adjudication` while mutually exclusive judgments or
  decisions conflict, and `:satisfied` only when every requested obligation is met and no
  conflict remains. Receipts for another case, rubric, or artifact version never count.
- Adjudication is another immutable, version-bound receipt that names the conflicting receipt
  identities and is produced only by a reviewer role authorized by the rubric; history is not
  overwritten to manufacture satisfaction.

A conflict set has a canonical identity derived from the exact case/rubric/artifact versions,
obligation/exclusivity group, and sorted conflicting receipt ids. The rubric declares allowed
resolutions plus adjudicator roles and distinct-principal quorum. Adjudicator proposal receipts
are immutable evidence but do not directly satisfy the case. Once compatible proposals reach
quorum, a domain operation atomically creates one `AdjudicationDecisionReceipt` in the unique
slot for that conflict-set identity, naming the resolution and exact quorum-member receipt ids.
An opposite decision racing for the empty slot yields exactly one winner and one
`:evaluation/conflict`; an equal retry is unchanged. Later incompatible proposals cannot change
the admitted decision, and the fold never selects an adjudication by timestamp or array order.

## TDD plan

1. Minimal one-artifact classification case validates.
2. Source + candidate translation-shaped case validates without translation keys in the
   generic schema.
3. Three-artifact source/candidate/reference case validates.
4. A receipt against candidate revision A cannot satisfy candidate revision B.
5. Two reviewers can produce conflicting receipts without overwriting each other.
6. A correction retains a reference to the candidate it corrects.
7. Layout-only keys are neither required nor emitted by the generic projection.
8. A partial multi-judgment or multi-role receipt set remains `:pending`.
9. Conflicting exclusive judgments remain `:needs-adjudication`; an authorized adjudication
   receipt resolves the named conflict without deleting either input receipt.
10. A complete non-conflicting receipt set for the exact case, rubric, and artifact versions
    is `:satisfied`; stale-version receipts do not satisfy the case.
11. Two descriptors with different role/quorum and exclusivity rules fold identically in the
    pure law and every adapter; missing descriptor fields fail validation rather than
    defaulting to satisfied.
    An allowed value without completion effect fails validation; `:defer` keeps the case
    pending/discoverable and cannot contribute satisfaction quorum.
12. Repeated receipts from one principal leave a two-reviewer quorum `:pending`; adding a
    second authenticated principal satisfies it, while spoofed reviewer ids never count.
13. An approve receipt followed by a valid same-principal reject supersession retains both but
    folds one effective reject vote. Without the relation it needs adjudication; concurrent
    successors admit exactly one, and cross-principal/version/cyclic supersession is rejected.
    In a two-obligation receipt, superseding only obligation A preserves the original effective
    judgment for B; a multi-edge correction is all-or-nothing.
14. Opposite adjudicator proposals for one canonical conflict set remain
    `:needs-adjudication` until a compatible distinct-principal quorum exists. Race opposite
    decision finalizations and prove exactly one immutable decision wins, the other conflicts,
    and every provider folds the same case status with all proposal evidence retained.

## Done when

- Translation SME review and a non-translation agent-output review can both be expressed
  with the same generic core contracts.
- The contracts are pure and runtime-independent.
- Immutable version binding makes stale approvals/corrections mechanically detectable.
- The pure completion fold prevents partial, disputed, or stale receipt sets from advancing a
  case.
- No UI layout, publication state, or provider implementation is encoded in the core.
