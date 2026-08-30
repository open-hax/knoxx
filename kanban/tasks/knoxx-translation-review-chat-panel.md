---
uuid: "knoxx-translation-review-chat-panel"
title: "Present translation evaluation cases in the 3-pane review UI"
status: icebox
priority: P3
labels: ["tasks", "3sp", "has-parent", "evaluation", "review", "frontend", "translations"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Present translation evaluation cases in the 3-pane review UI

> Parent epic: `knoxx-evaluation-review-system`
> Deferred behind: `knoxx-evaluation-case-contracts`, `knoxx-evaluation-mcp-review-flow`

## Purpose

Keep the useful translation comparison/reviewer experience as a **presentation adapter**
over the generic evaluation model, not as the place where translation-review semantics
live.

The previous version of this card made `TranslationReviewPage` and `AgentChatPanel` the
next architectural step. That is intentionally deferred: first prove that an AI can walk
an SME through the same review from case to durable receipt using contracts + MCP alone.

## Eventual scope

- Present one generic evaluation case using the translation adapter's source, candidate,
  terminology/context, rubric, and existing judgments.
- Use artifact roles to choose the side-by-side comparison layout; do not persist
  `left/right` as semantic identity.
- Keep the agent chat panel as one interaction surface over the same domain operations the
  MCP flow uses.
- Surface explain-difference, suggest-revision, and check-terminology actions by invoking
  capability/domain operations rather than frontend-only review logic.
- Record corrections/judgments through the evaluation write boundary and immediately
  render the resulting receipt/state projection.

## Boundary rules

- No review law exists only in React/Helix/TypeScript state.
- The frontend does not own candidate identity, approval truth, or training labels.
- Translation is one adapter/domain specialization of the generic evaluation case.
- Client teams may replace this UI (for example with Angular) without changing stored
  evaluation semantics or MCP behavior.

## Why iceboxed now

The frontend is intentionally not the critical path. Reopen this card once the generic
evaluation contracts and the headless SME review proof work end to end; at that point the
UI is an ergonomic projection over a model already proven without it.

## Done when eventually resumed

- The UI consumes the generic evaluation case/receipt API rather than bespoke mutable
  translation review state.
- The same translation case can be completed through MCP or this UI with equivalent
  durable receipts.
- Replacing the frontend implementation does not require changing evaluation contracts.
