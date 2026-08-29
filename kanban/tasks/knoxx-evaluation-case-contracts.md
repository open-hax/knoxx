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

`:keeps-pending` values do not enter compatibility/exclusivity conflict sets because they
express no terminal position. They still occupy that principal/role/obligation's current
judgment head. A later satisfying value from the same principal is a changed vote and must
explicitly supersede the pending head; an unlinked changed value is rejected at admission and
cannot be chosen by timestamp or array order. A satisfying value from another principal may
count toward the rubric's quorum without conflicting with the defer, while the defer remains
durable history. Thus defer-then-approve is deterministic without manufacturing adjudication
or silently discarding the pending receipt.

Quorum is counted per obligation and required role over **distinct authenticated principal
identities**, using the immutable principal/role facts recorded on admitted receipts. Multiple
receipt ids or revisions from one principal contribute at most one vote to that obligation-role
requirement; a replacement/correction may change that principal's effective judgment but
cannot manufacture another reviewer. A principal may satisfy a different required role only
when its authenticated role facts and the rubric's role-overlap policy explicitly allow it.

The server derives one `judgment-head-id` from `{org-id, case/rubric/artifact versions,
obligation-id, authenticated-principal-id, reviewer-role}`; receipt id and judgment value are
not head identity. Its linearizable slot holds a `head-version`, the current canonical effective
determination, and every immutable receipt/judgment/correction id that attests that equal
determination. The determination contains the judgment value/completion effect and an optional
effective correction fingerprint over the exact corrected artifact/version, correction-schema
version, and canonical replacement/patch bytes. Its projection exposes that fingerprint and
the authoritative correction ref/content; correction receipt ids alone never choose a winner.
A distinct receipt id with a canonically equal determination appends intentional evidence and
joins the current head generation without advancing `head-version`, but creates no second leaf
or vote. Equal-duplicate admission and a changed successor serialize through this same slot, so
a late old determination cannot resurrect an earlier generation. Head equality includes
correction absence/presence and that semantic fingerprint: identical correction content under
distinct receipt ids may join one generation, while the same judgment value with different
replacement text is a changed determination. Different rationale/evidence remains distinct
receipt history but cannot multiply that principal's vote or change conflict identity.

A changed determination—judgment value/effect or effective correction—is another immutable
receipt with obligation-scoped supersession entries of
`{obligation-id, judgment-head-id, expected-head-version}`. Each target must be the current
head for the same organization, case/rubric/artifact versions, obligation, authenticated
principal, and reviewer role. Admission validates and advances every named
principal/obligation/role slot atomically or appends nothing: two concurrent successors cannot
both win. Advancing one slot makes every equal receipt in its prior generation an immutable
ancestor, so correcting once cannot leave a duplicate old vote effective. The status fold uses
one current head per `{obligation-id, authenticated-principal-id, reviewer-role}` slot and
retains every receipt/ancestor as history; correcting obligation A or one reviewer's slot does
not suppress unrelated judgments or another principal/role slot carried by the same prior
receipt. It never chooses by wall clock. Any changed same-principal value or correction,
including one after a `:keeps-pending` head, is rejected without a valid head/version
supersession. Independently
admitted judgments from distinct principals remain coexisting evidence, and conflicting
exclusive values yield
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
  their receipts. It returns `:needs-adjudication` whenever any unresolved exclusivity or
  decision conflict exists, even when unrelated obligations are also missing; otherwise it
  returns `:pending` while any requested judgment or reviewer-role obligation is missing and
  `:satisfied` only when every requested obligation is met. The result also carries
  deterministic `:conflict-sets` and `:missing-obligations` details, so conflict precedence
  never hides pending work. Receipts for another case, rubric, or artifact version never count.
- Adjudication is another immutable, version-bound receipt that names the conflicting receipt
  identities and is produced only by a reviewer role authorized by the rubric; history is not
  overwritten to manufacture satisfaction.

A conflict set has a canonical identity derived from the server-authenticated organization,
exact case/rubric/artifact versions, obligation/exclusivity group, and sorted conflicting
effective `{judgment-head-id, head-version}` pairs. Equal receipt ids/evidence members are
identity-neutral because they do not change an effective head generation. The organization is
part of both conflict-set identity and the unique decision slot; caller-supplied scope never
participates. The rubric declares allowed resolutions plus adjudicator roles and
distinct-principal quorum. Every allowed adjudication resolution declares the same required
closed completion effect as an ordinary judgment value: `:satisfies` or `:keeps-pending`; there
is no default, and built-in `:defer` is always `:keeps-pending`. Proposal admission derives and
binds the exact rubric version, resolution value, and declared effect, rejecting a missing or
mismatched effect. Adjudicator proposal receipts are immutable evidence but do not
directly satisfy the case. Quorum folds one server-derived `AdjudicationProposalHead` per exact
organization, conflict-set identity, authenticated principal, and adjudicator role. Equal
canonical proposals under distinct receipt ids join one head generation and count once. A
changed resolution must explicitly supersede the current proposal head id/version; the atomic
compare admits exactly one concurrent successor and rejects cross-organization, cross-conflict,
cross-principal, cross-role, stale, or cyclic edges while retaining every receipt as history.
Only current proposal heads are quorum-eligible.

Once compatible current heads reach quorum, a domain operation atomically verifies those named
heads are still current and creates one `AdjudicationDecisionReceipt` in that organization's
unique slot for the conflict-set identity, naming the resolution, rubric version, declared
completion effect, exact effective judgment-head generations, exact proposal-head generations,
and quorum-member proposal receipt ids.
An opposite decision racing for the empty slot yields exactly one winner and one
`:evaluation/conflict`; an equal retry is unchanged. Later incompatible proposals cannot change
the admitted decision, and the fold never selects an adjudication by timestamp or array order.
The pure fold applies the receipt-bound effect: `:satisfies` discharges the named conflict's
obligation/exclusivity group, while `:keeps-pending` resolves that conflict into an explicit
missing/pending obligation with its decision reason/evidence and can never satisfy the case.
Advancing an underlying judgment head creates a new conflict generation; the old decision and
effect cannot resolve it.
Appending equal evidence after finalization leaves the same conflict identity/decision. A real
head advance creates a new conflict generation that the old decision cannot resolve.

## TDD plan

1. Minimal one-artifact classification case validates.
2. Source + candidate translation-shaped case validates without translation keys in the
   generic schema.
3. Three-artifact source/candidate/reference case validates.
4. A receipt against candidate revision A cannot satisfy candidate revision B.
5. Two reviewers can produce conflicting receipts without overwriting each other.
6. A correction retains a reference to the candidate it corrects. Two distinct receipts with
   the identical canonical correction join one head generation; a second `:corrected`
   determination with different replacement text must supersede the current head/version and
   becomes the sole effective correction while both proposals remain history. The same-value
   changed correction without supersession is rejected and cannot yield `:satisfied`.
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
    pending/discoverable and cannot contribute satisfaction quorum. A same-principal
    defer-then-approve succeeds only with an explicit supersession edge and then uses the
    approve leaf; the unlinked form is rejected without append. A different principal's
    satisfying value does not conflict with the defer and is evaluated normally under quorum.
12. Repeated receipts from one principal leave a two-reviewer quorum `:pending`; equal
    judgments under distinct receipt ids join one server-derived head and count once. A later
    reject advances that one head and leaves no duplicate approve effective. Race the equal
    duplicate with the changed successor so an old generation cannot reopen. Adding a second
    authenticated principal satisfies quorum, while spoofed reviewer ids never count.
13. An approve receipt followed by a valid same-principal reject supersession retains both but
    folds one effective reject vote. Without the current head id/version relation it is rejected;
    concurrent successors admit exactly one, and cross-principal/version/cyclic supersession is
    rejected.
    In a two-obligation receipt, superseding only obligation A preserves the original effective
    judgment for B; a multi-edge correction is all-or-nothing.
14. Opposite adjudicator proposals for one canonical conflict set remain
    `:needs-adjudication` until a compatible distinct-principal quorum exists. Race opposite
    decision finalizations and prove exactly one immutable decision wins, the other conflicts,
    and every provider folds the same case status with all proposal evidence retained. Two
    organizations reuse every case/rubric/artifact/receipt id and still receive independent
    conflict-set identities, proposal quorums, and decision slots with no existence signal.
    Have two principals propose accept and then each supersede with reject before finalization:
    the old accept heads cannot authorize a decision, while the current reject heads can. Race
    one supersession with finalization and prove the decision transaction either names the still
    current generation or retries; equal duplicate proposals never add another quorum member.
    Append an equal duplicate judgment after finalization and prove the existing decision still
    applies with no second slot; advancing a conflicting head produces a new unresolved set.
15. A fixture with one missing obligation plus an unrelated exclusive conflict returns
    `:needs-adjudication` and reports both the conflict set and missing obligation. Resolving
    only the conflict transitions it to `:pending`; satisfying the remaining obligation then
    transitions it to `:satisfied`.
16. Two otherwise identical rubrics give an adjudication resolution `:satisfies` and
    `:keeps-pending` respectively. Their version-bound decisions deterministically fold to
    `:satisfied` and `:pending` when no other work remains; a missing/mismatched effect is invalid,
    and `:defer` cannot be declared satisfying. The pending decision remains discoverable until
    an underlying judgment-head advance creates a new generation.

## Done when

- Translation SME review and a non-translation agent-output review can both be expressed
  with the same generic core contracts.
- The contracts are pure and runtime-independent.
- Immutable version binding makes stale approvals/corrections mechanically detectable.
- Equal same-principal determinations, including equal semantic corrections, share one
  effective head generation. Same-value correction revisions advance that head atomically, so
  the status and downstream projection name exactly one effective replacement without leaving
  duplicate old votes or ambiguous correction text.
- The pure completion fold prevents partial, disputed, or stale receipt sets from advancing a
  case.
- Mixed conflict/incomplete cases use one precedence law without hiding either conflict or
  pending-work details.
- Adjudication conflict and decision identities are scoped by server-authenticated organization.
- Adjudication quorum counts only one current proposal head per authenticated principal/role;
  proposal revision races cannot finalize from withdrawn or mixed generations.
- Every versioned adjudication resolution has an explicit receipt-bound completion effect, so
  terminal and nonterminal decisions fold identically in every provider.
- No UI layout, publication state, or provider implementation is encoded in the core.
