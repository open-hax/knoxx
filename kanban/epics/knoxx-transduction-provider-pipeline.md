---
uuid: "knoxx-transduction-provider-pipeline"
title: "Transduction pipeline — typed transformations with replaceable providers"
status: incoming
priority: P1
labels: ["epics", "transduction", "translations", "contracts", "providers"]
created_at: "2026-08-13T00:00:00Z"
points: 0
category: epics
---
# Transduction pipeline — typed transformations with replaceable providers

## Signal

Translation is one instance of a more general capability: **transduction**.

Knoxx currently has a working text-to-text translation pipeline, but its contracts,
persistence, provider selection, publication gating, and SME review have grown together.
The next slice should isolate the transformation itself so the same outer shape can
support text-to-text, speech-to-text, text-to-speech, and other typed transformations
without making those modalities part of the translation implementation.

## Ownership rule

```text
transduction input contract
        + operation/provider policy
        -> candidate artifact
        + provenance / execution receipt
```

A transduction provider owns how a candidate is produced. It does **not** own:

- publication intent;
- long-term resource authority;
- human/SME judgment about the candidate;
- UI layout;
- final HTML/static representation.

Those are separate capabilities and may be connected by workflow dataflow when output
contracts satisfy the next operation's input contract.

## First concrete instance

The existing machine-translation path is the reference implementation:

```text
source text artifact
      -> translation transduction
      -> target-locale candidate artifact + provenance
```

Do not generalize by adding modality conditionals to the translation worker. Generalize
the boundary first; additional providers/types can implement that boundary later.

## Children / board moves

- `knoxx-translation-transduction-boundary` — express the existing machine-translation path
  as typed source artifact -> candidate artifact + provenance behind a replaceable provider.
- `knoxx-translation-config-publication-dependency-removal` — remove direct law/domain and
  transitive shared-loader publication dependencies, then prove publication-free provider
  selection through the existing Knoxx adapter without creating a parallel config boundary.
- `knoxx-translations-event-sourced` — preserve translation/transduction attempts as immutable history rather than destructive current-state upserts.
- `knoxx-translation-pipeline-validation` — validate the translation transduction path only through candidate artifact + receipt; evaluation and rendering are separate acceptance surfaces.
- Future: typed transduction operation/provider contract once the upstream Eta-mu/Katamorph workflow action vocabulary exposes `requires` / `provides` dataflow contracts.
- Future: adapt the translation worker to that typed operation boundary without changing its behavior first.

## Relationship to active publication epic

`knoxx-contract-owned-publication-pipeline` is an **integration consumer** of translation
results. Its currently stacked PRs remain intact. Publication may ask for translation work
and inspect translation/review evidence, but it does not become the owner of the general
transduction model.

In particular, the already-active cards
`knoxx-translation-pipeline-config-resource` and `knoxx-translation-publication-gate`
should finish under their current publication stack rather than being reparented mid-PR.
Their resulting seams become inputs to this epic's later cleanup.

## Non-goals

- Building STT/TTS implementations in this epic.
- Replacing the workflow DSL in Knoxx; typed operation/dataflow language belongs upstream.
- Folding SME review into the transduction result.
- Making publication state a transduction concern.
- Rewriting the existing worker before its present behavior is covered by boundary tests.

## Done when

- Knoxx can describe the translation operation as typed input -> candidate output + provenance without reference to publication or review UI.
- Translation attempts/history are non-destructive and distinguish candidate production from current projections.
- Each translation/transduction boundary has independently attributable contract tests.
- Provider selection is replaceable behind the operation boundary.
- The resulting contract is capable of admitting a future STT/TTS provider without changing publication or evaluation contracts.
