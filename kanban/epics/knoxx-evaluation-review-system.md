---
uuid: "knoxx-evaluation-review-system"
title: "Evaluation and review — artifacts, rubrics, judgments, and receipts"
status: accepted
priority: P0
labels: ["epics", "evaluation", "review", "sme", "agents", "contracts", "translations", "memory", "regression"]
created_at: "2026-08-13T00:00:00Z"
points: 0
category: epics
---
# Evaluation and review — artifacts, rubrics, judgments, and receipts

## Signal

The translation review surface is one instance of a general evaluation system.
Many agent tasks need a human or SME to compare inputs, outputs, references, and
evidence; apply a rubric; then record durable judgments, corrections, labels, or
decisions that can later support training, evaluation, retrieval, or regression tests.

The core model should not be `TranslationReview` and should not assume a two-column UI.

## Ownership rule

```text
review case
  + artifacts / evidence
  + rubric
  + requested judgments
        -> evaluation
        -> judgments / corrections / decisions
        -> durable receipts
```

Suggested vocabulary:

- **Review Case** — one unit requiring judgment.
- **Artifact** — input, candidate, reference, previous result, or other evidence.
- **Rubric** — the questions/laws governing judgment.
- **Judgment** — one atomic determination.
- **Correction** — proposed replacement or improvement.
- **Decision** — accept/reject/defer/etc.
- **Receipt** — durable fact that the evaluation occurred.
- **Adjudication** — resolution when evaluations disagree.

Artifact roles are semantic (`:source`, `:candidate`, `:reference`, `:evidence`, ...),
not layout positions such as `:left` and `:right`.

## First concrete instance and product constraint

Translation SME review remains the reference workflow:

```text
source segment + candidate translation + terminology/context
        -> SME evaluation
        -> approval/correction/judgments + receipt
```

But the same contract must also admit:

```text
agent task input + agent output + rubric
        -> reviewer evaluation
        -> labels/correction/decision + receipt
```

The translation instance is not merely a disposable fixture. Its existing document-first,
split-level UI and data shape are the product acceptance surface. Generic contracts may lift
the durable semantics, but must preserve real ordered translation splits, corrected examples as
future translation memory, document-level fast paths, and the complete human workflow over the
resource CMS.

## Children

- `knoxx-evaluation-case-contracts` — define the generic case/artifact/rubric/judgment/receipt laws.
- `knoxx-evaluation-mcp-review-flow` — prove a headless AI-guided SME review from pending case to durable receipt and next case.
- `knowledge-ops-translation-document-review-v2` — reopened P0 regression card and canonical
  translation product contract.
- `knoxx-translation-split-memory-feedback` — P0 durable split/candidate authority and the
  correction-to-later-translation feedback loop.
- `knoxx-translation-review-chat-panel` — P0 UI adapter and mandatory end-to-end acceptance
  surface, developed against the same receipt operations as MCP.
- Future: translation-specific adapter mapping current segment/review data onto the generic evaluation model.
- Future: training/export projections derived from evaluation receipts rather than owned by the review UI.

## Relationship to publication

Publication may require evidence such as "the relevant candidate was approved for this
concrete revision". It consumes evaluation receipts; it does not own the evaluation
workflow or mutable review state.

The active `knoxx-translation-publication-gate` PR remains where it is. This epic owns
the general model that can later supply those receipt facts.

## Non-goals

- Treating a headless proof as completion while resource-backed translations remain read-only
  or invisible in the human workflow.
- Encoding workflow phases such as `in_review` as durable semantic truth when they can be derived.
- Making model-training export the primary storage model.
- Requiring every evaluation to compare exactly two artifacts.

## Done when

- A review case can be expressed without translation-specific fields in its generic core.
- Translation SME review can map onto that core without losing translation-specific context.
- An MCP/agent flow can enumerate work, explain a case, collect the SME judgment, persist a receipt, and advance without a dedicated UI.
- Evaluation receipts can be projected into labels/training/evaluation datasets without those projections becoming the authority.
- Approved corrected translation splits are retrievable as scoped translation memory by a later
  translation attempt, while rejected/stale candidates are excluded from positive memory.
- The restored three-pane UI completes the same revision-bound workflow end to end over the
  resource CMS.
