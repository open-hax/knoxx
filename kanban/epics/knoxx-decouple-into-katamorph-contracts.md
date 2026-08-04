---
uuid: "knoxx-decouple-into-katamorph-contracts"
title: "Disentangle Knoxx's tool products into Katamorph contracts, without taking the product down"
status: incoming
priority: P2
labels: ["epics", "decouple", "katamorph"]
created_at: "2026-08-04T00:00:00Z"
points: 0
category: epics
---
# Disentangle Knoxx's tool products into Katamorph contracts

## Purpose

Knoxx currently carries several things that are each close to a product in their
own right — Discord agents, Bluesky agents, translation, the sandbox, voice, the
CMS — inside one backend, with their layers mixed. The direction is:

1. describe as much of each as practical as **Katamorph contracts** (EDN
   declarations of identities, capabilities, policies, triggers, actions,
   sources, stores, providers, agent config; Malli-validated boundaries;
   effects injected rather than owned), and
2. let **eta-mu** own each as a separate package.

We are not there. The constraint that shapes every card under this epic: **the
product has to keep working throughout.** No card here may be a flag-day
rewrite.

## Why now

The MCP surface made the coupling visible rather than theoretical. Getting
`https://knoxx.promethean.rest/mcp` working took eight defects across three
repos, five of them one shape — a writer and a reader that only ever ran
together against live infrastructure, so their contract drifted unnoticed. That
is what an undeclared boundary costs. Katamorph's premise is that those
boundaries should be data with a validator attached.

## Shape of the work

- **Boundary first, extraction second.** Name and validate a tool set's
  boundary in place before moving it. A namespace that has a contract can be
  moved later by anyone; a namespace that only has behaviour cannot.
- **One tool set at a time**, each independently shippable.
- **Contracts before renames.** Several cards here want to rename things
  (`semantic_*`); renaming across an undeclared boundary is how you get another
  silent drift.

## Children

- `knoxx-mcp-actor-ascription`
- `knoxx-deploy-actor-owning-local-credentials`
- `knoxx-mcp-consent-permission-groups`
- `knoxx-translations-event-sourced`
- `knoxx-translation-pipeline-validation`
- `knoxx-voice-tools-remote-transport`
- `knoxx-tool-vocabulary-rename`
- `knoxx-cms-contract-validation`
- `knoxx-tool-namespace-boundary-audit`

## Non-goals

- Moving anything into eta-mu packages yet. That is the end state, not the next
  step.
- Rewriting the agent runtime.

## Prior art on this board

This epic is not the first pass at decoupling. Reconcile with, do not duplicate:

- **`knoxx-contract-runtime-extraction`** (pending) — extract the contract
  runtime core as a package. The eta-mu end state.
- **`knoxx-runtime-decomposition-inventory`** (pending) — the existing
  decomposition inventory.
- **`knoxx-namespace-manifest-migration`**, **`knoxx-store-protocol`**,
  **`knoxx-rich-action-registry`** — earlier moves in the same direction.

If those cards already express the plan, this epic should become a thin wrapper
that adds only the tool-set slices and the "stay live throughout" constraint.
