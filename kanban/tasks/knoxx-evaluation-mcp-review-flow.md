---
uuid: "knoxx-evaluation-mcp-review-flow"
title: "Prove a headless MCP-guided SME evaluation from case to durable receipt"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "evaluation", "review", "mcp", "sme"]
created_at: "2026-08-13T00:00:00Z"
points: 5
category: tasks
---
# Prove a headless MCP-guided SME evaluation from case to durable receipt

> Parent epic: `knoxx-evaluation-review-system`
> Depends on: `knoxx-evaluation-case-contracts`

## Purpose

Make the semantic review model useful before investing in another dedicated frontend.
An AI using Knoxx MCP tools should be able to walk an SME through one real evaluation
case from discovery to durable judgment, correction/decision, receipt, and next work.

Translation SME review is the first fixture, but tool names and core behavior should be
generic where the semantics are generic.

## Minimum capability surface

The exact names may change during implementation, but the behavioral surface must cover:

- list/claim or enumerate pending review cases;
- fetch one case with its artifacts, context, rubric, and evidence;
- explain what judgment is being requested and why;
- record one or more judgments;
- record an SME correction without destroying the candidate artifact;
- accept/reject/defer (or the rubric's equivalent decision);
- persist an immutable evaluation receipt;
- show whether the receipt satisfied the requested evaluation obligations;
- advance to the next case.

For the translation fixture the agent must be able to show source text, candidate text,
locale/terminology/context, collect the SME's correction or approval, and leave evidence
that publication law can later query.

## Boundary rules

- MCP is a capability adapter, not the evaluation domain model.
- Tool handlers delegate to pure evaluation/domain operations and repository/effect
  boundaries; no semantic law lives only in an MCP handler.
- The flow does not require the existing translation React/TS UI.
- Publication is not mutated merely to clear review state. Publication consumes the
  resulting evidence independently.
- The AI may explain, ask for a specific judgment, and surface relevant artifacts, but it
  may not fabricate the SME's judgment when the rubric requires human adjudication.

## TDD / proof

Use one deterministic translation segment fixture and a fake repository/receipt store:

1. Agent can discover the pending case.
2. Fetch returns source + candidate + rubric/context with immutable ids/versions.
3. Recording approval writes one receipt and leaves candidate evidence unchanged.
4. Recording a correction preserves both original candidate and correction.
5. Retrying the same receipt write is idempotent by receipt/event identity.
6. A new candidate revision does not inherit the old receipt.
7. The final query reports the case satisfied and returns the next pending case.
8. The entire proof runs with the translation review frontend absent.

## Done when

- An MCP client can complete one translation SME review start to finish using only the
  capability surface and explicit SME judgments.
- Durable output is expressed through the generic evaluation contracts.
- Translation-specific context is preserved without appearing in the generic core law.
- The same MCP/domain operations could be presented later by Angular, Helix, CLI, or
  another agent without changing the stored evaluation semantics.
