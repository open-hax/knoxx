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

## Done when

- Translation SME review and a non-translation agent-output review can both be expressed
  with the same generic core contracts.
- The contracts are pure and runtime-independent.
- Immutable version binding makes stale approvals/corrections mechanically detectable.
- The pure completion fold prevents partial, disputed, or stale receipt sets from advancing a
  case.
- No UI layout, publication state, or provider implementation is encoded in the core.
