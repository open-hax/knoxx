---
uuid: "knoxx-translation-transduction-boundary"
title: "Express machine translation as a typed transduction operation with replaceable providers"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "transduction", "translations", "contracts", "providers"]
created_at: "2026-08-13T00:00:00Z"
points: 5
category: tasks
---
# Express machine translation as a typed transduction operation with replaceable providers

> Parent epic: `knoxx-transduction-provider-pipeline`

## Purpose

Use the existing machine-translation path as the first concrete instance of a generic
transduction boundary: explicit input artifact contract, provider/config selection,
candidate output artifact contract, and provenance/receipt.

Do not rewrite the worker or add speech/image modalities in this slice. First make the
existing behavior describable without publication, review UI, or storage-provider
knowledge.

## Scope

- Identify the minimum source text artifact shape the current translation worker needs.
- Identify the candidate translated artifact shape it produces.
- Keep translation-specific semantics (source/target locale, terminology policy,
  model/provider choice) in the translation operation/provider contract.
- Return provenance sufficient to explain which provider/model/policy produced the
  candidate and from which immutable source revision/segment.
- Carry the canonical organization/scope from the source artifact and authenticated
  operation context through provider selection and output shaping. Derive one
  `EffectiveOrganization` before any config/repository/provider access: ordinary actors use
  their trusted membership organization; a server-authenticated system administrator may use
  the explicit resource-policy target already accepted by `resolve-org`. Provider arguments,
  caller headers, or results cannot override it, and a config artifact resolved for another
  effective organization is rejected before invocation. Receipts retain the authenticated
  principal/origin plus explicit delegation target so admin work stays attributable.
- Bind provenance to the exact resolved provider-config/policy version, normalized request
  parameters, provider/model identity, and raw-result evidence digest needed to reproduce or
  audit decoding; volatile transport timing stays in an excluded execution envelope.
  `knoxx-versioned-resolved-translation-config` (#275) owns the production artifact containing
  global and optional organization-override revisions; this contract consumes it unchanged.
- Isolate provider invocation behind one replaceable boundary.
- Keep persistence/history outside the provider call; `knoxx-translations-event-sourced`
  owns durable attempt history.
- Keep SME judgments outside the transduction result; the evaluation system consumes the
  candidate as an artifact.
- Keep publication outside the operation; publication may derive translation work and
  later inspect evidence, but translation does not mutate publication state.

## Compatibility target

Shape this so it can later be expressed by the upstream typed workflow/action vocabulary:

```text
operation requires SourceTextArtifact
operation provides CandidateTextArtifact + TransductionReceipt
workflow step wires named outputs to later inputs
```

Do not duplicate Eta-mu/Katamorph's workflow DSL inside Knoxx to achieve that future
compatibility.

## Laws

- Candidate output binds to the exact immutable source revision/segment it transformed.
- Provider/model identity is provenance, not candidate semantic identity.
- Re-running the same source may produce another candidate; neither run overwrites the
  historical evidence of the other.
- A provider failure cannot synthesize a successful candidate artifact.
- Candidate identity and provenance retain the server-derived effective organization,
  authenticated actor/delegation evidence, and exact resolved config revision; unauthorized
  cross-tenant targets, config injection, or provider-returned scope changes fail closed with no
  candidate or history append.
- The pure contract layer contains no HTTP, OpenPlanner, Mongo, React, or publication
  dependencies.

## Done when

- The current machine-translation operation can be described and tested as typed input ->
  candidate + provenance.
- A fake provider can substitute for the production provider in contract tests.
- Negative contract tests reject cross-tenant resolved config and provider-returned identity
  drift, and prove the successful receipt names the exact config/policy revision and canonical
  request/result evidence.
- An ordinary actor cannot target another organization; a trusted system administrator can
  target an explicit delegated organization, and config, provider input, candidate, event, and
  receipt all bind that effective organization plus the auditable actor/delegation evidence.
- Translation-specific data is present where needed without leaking publication/review/UI
  concepts into the transduction core.
- The boundary is compatible with a future generic workflow operation `requires` /
  `provides` contract without another semantic redesign.
