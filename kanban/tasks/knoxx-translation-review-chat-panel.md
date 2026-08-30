---
uuid: "knoxx-translation-review-chat-panel"
title: "Restore the 3-pane document and split translation review UI"
status: accepted
priority: P0
labels: ["tasks", "8sp", "has-parent", "regression", "evaluation", "review", "frontend", "translations", "memory", "cms"]
created_at: "2026-05-30T00:00:00Z"
points: 8
category: tasks
---

# Restore the 3-pane document and split translation review UI

> Parent epic: `knoxx-evaluation-review-system`
> Product anchor: `knowledge-ops-translation-document-review-v2`

## Purpose

Restore the document-first, split-level translation workflow as the primary human surface
over the resource CMS. The UI remains a presentation adapter, but it is now a critical-path
acceptance surface: the translation workflow is not complete when only a headless client can
perform it.

The historical UI and its data shape are the target. Resource-backed candidates must carry
real persisted splits, so the UI can write durable review evidence rather than display
synthetic read-only paragraphs.

## Scope

- Present one canonical translation review case using the translation adapter's source,
  candidate, terminology/context, rubric, and existing judgments.
- Use artifact roles to choose the side-by-side comparison layout; do not persist
  `left/right` as semantic identity.
- Keep the agent chat panel as one interaction surface over the same domain operations the
  MCP flow uses.
- Surface explain-difference, suggest-revision, and check-terminology actions by invoking
  capability/domain operations rather than frontend-only review logic.
- Record corrections/judgments through the evaluation write boundary and immediately
  render the resulting receipt/state projection.
- List all resource-derived work states and expose dispatch/retry from the same page.
- Preserve the document-level fast path while allowing granular split correction/rejection.
- Show whether the effective reviewed text differs from the original machine candidate and
  which revision publication will consume.
- Demonstrate that an approved correction is available as context to a later translation run.

## Boundary rules

- No review law exists only in React/Helix/TypeScript state.
- The frontend does not own candidate identity, approval truth, translation memory, or
  training labels.
- Translation's candidate-bound receipt is the P0 semantic authority; the generic evaluation
  contract may adapt it without changing its identity or delaying the restored workflow.
- Client teams may replace this UI (for example with Angular) without changing stored
  evaluation semantics or MCP behavior.

## Sequencing

Build the translation-specific vertical slice around its canonical case/receipt operations.
Adapt those operations to the generic evaluation contract when that contract is available; it is
not a predecessor for this P0 repair. Do not put semantic review law into component state.

## Done when

- The UI consumes canonical translation case/receipt operations rather than bespoke mutable
  translation review state; the generic evaluation adapter can consume the same operations.
- The same translation case can be completed through MCP or this UI with equivalent
  durable receipts.
- Replacing the frontend implementation does not require changing evaluation contracts.
- Agent-produced resource translations expose real split ids and all historical review fields;
  no `contract_content` read-only branch suppresses review actions.
- A browser tour proves dispatch, correction, rejection, approval, memory retrieval, and
  publication against seeded resource-backed documents.
