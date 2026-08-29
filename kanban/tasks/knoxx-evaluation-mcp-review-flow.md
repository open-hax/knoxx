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

- enumerate tenant-scoped pending review cases. This first slice does not introduce a mutable
  claim/lease state; concurrent reviewers are reconciled by the immutable receipt and case
  status laws rather than a UI-only reservation;
- fetch one case with its artifacts, context, rubric, and evidence;
- explain what judgment is being requested and why;
- record one or more judgments;
- record an SME correction without destroying the candidate artifact;
- accept/reject/defer (or the rubric's equivalent decision);
- persist an immutable evaluation receipt;
- show the pure case status (`:pending`, `:needs-adjudication`, or `:satisfied`) derived from
  all version-bound receipts and the requested judgment/reviewer-role obligations;
- advance to the next case.

Every receipt-write tool input includes a required caller/workflow-stable `receipt_id`
(idempotency key), and every result echoes it with the admitted immutable receipt. The MCP
handler validates and passes that identity unchanged to atomic receipt admission; it cannot
mint a new id per call, substitute a transport tool-call id, or derive identity from receipt
content. Organization/reviewer facts remain server-derived. Distinct receipt ids with
canonically equal evaluations remain distinct historical evaluations.

For the translation fixture the agent must be able to show source text, candidate text,
locale/terminology/context, collect the SME's correction or approval, and leave evidence
that publication law can later query.

## Boundary rules

- MCP is a capability adapter, not the evaluation domain model.
- Tool handlers delegate to pure evaluation/domain operations and repository/effect
  boundaries; no semantic law lives only in an MCP handler.
- The MCP adapter derives organization, reviewer identity, and reviewer roles from the
  authenticated principal/actor context. Request payloads cannot select or override tenant,
  reviewer, or role. Case discovery is tenant-scoped; direct case/artifact/receipt operations
  enforce the same ownership before read or write and return a non-enumerating
  `:authorization/forbidden` failure across the boundary.
- The flow does not require the existing translation React/TS UI.
- Publication is not mutated merely to clear review state. Publication consumes the
  resulting evidence independently.
- The AI may explain, ask for a specific judgment, and surface relevant artifacts, but it
  may not fabricate the SME's judgment when the rubric requires human adjudication.

## TDD / proof

Use deterministic translation segment fixtures for two organizations and a fake
repository/receipt store:

1. Agent can discover the pending case.
2. Fetch returns source + candidate + rubric/context with immutable ids/versions.
3. Recording approval writes one receipt and leaves candidate evidence unchanged.
4. Recording a correction preserves both original candidate and correction.
5. Retrying the same receipt/event identity is idempotent only when the complete canonical,
   validated receipt payload is semantically equal. Store-assigned envelope metadata that
   the receipt contract explicitly excludes is not part of this comparison.
6. Retrying that identity with any canonical payload difference fails with
   `:evaluation/conflict`. This includes, without limiting the comparison, reviewer
   identity/role, case identity/version, rubric identity/version, source/candidate artifact
   identity/version, judgment, correction, decision, provenance, and evidence. Read-back
   returns the original immutable receipt unchanged and no second receipt is appended.
7. Receipt admission is one atomic unique-insert/compare operation, not a read followed by an
   append. Race two different canonical payloads for the same absent receipt identity: exactly
   one persists and the other returns `:evaluation/conflict`; read-back returns exactly the
   winner. Race two equal payloads: one receipt persists and both callers observe it
   idempotently.
   Exercise the real MCP schema/handler with a commit-then-lost-response retry: the same
   caller-stable `receipt_id` returns the original receipt, changed reuse conflicts, and two
   distinct ids with equal evaluation content persist as two intentional evaluations.
8. A new candidate revision does not inherit the old receipt.
9. The pure status fold keeps incomplete multi-judgment or multi-role sets `:pending`, reports
   exclusive conflicting receipts as `:needs-adjudication`, and returns `:satisfied` only for
   a complete non-conflicting set bound to the exact case, rubric, and artifact versions. An
   authorized adjudication receipt names the conflicting receipts rather than replacing them.
   The fixture includes explicit obligation ids, allowed values, role/quorum rules,
   exclusivity groups, and adjudicator roles so no adapter invents completion defaults.
10. Only `:satisfied` advances to the next pending case; `:pending` and
    `:needs-adjudication` remain visible work.
11. The authenticated organization discovers only its cases. Cross-tenant direct fetches and
    writes return the same non-enumerating `:authorization/forbidden` result and persist
    nothing. A client-supplied tenant, reviewer identity, or reviewer role cannot override the
    authenticated principal, including when it names a real SME in the same organization.
12. The entire proof runs with the translation review frontend absent.

## Done when

- An MCP client can complete one translation SME review start to finish using only the
  capability surface and explicit SME judgments.
- Durable output is expressed through the generic evaluation contracts.
- Translation-specific context is preserved without appearing in the generic core law.
- Concurrent receipt retries cannot create two authorities for one identity; unique
  admission and canonical-payload comparison are atomic.
- Caller-stable receipt identity survives the real MCP boundary and response loss; store-only
  idempotency tests do not satisfy this card.
- Discovery, artifact reads, and receipt writes are tenant-owned, and durable reviewer facts
  come from the authenticated actor rather than client assertions.
- Case advancement consumes the pure version-bound satisfaction/adjudication fold and cannot
  skip partial or disputed work.
- The same MCP/domain operations could be presented later by Angular, Helix, CLI, or
  another agent without changing the stored evaluation semantics.
