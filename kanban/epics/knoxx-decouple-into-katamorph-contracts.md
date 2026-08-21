---
uuid: "knoxx-decouple-into-katamorph-contracts"
title: "Knoxx constitutional compliance and actor awareness — comply in place, do not extract yet"
status: incoming
priority: P2
labels: ["epics", "decouple", "katamorph", "compliance"]
created_at: "2026-08-04T00:00:00Z"
points: 0
category: epics
---
# Knoxx constitutional compliance and actor awareness

> **Scope correction, 2026-08-04.** An earlier draft of this epic proposed
> disentangling Knoxx's tool sets *into* Katamorph contracts and eta-mu
> packages. That duplicates an existing **P0** epic and contradicts a written
> ownership decision. Corrected below. The end state is unchanged; the next step
> is not extraction.

## The plan already exists upstream

`eta-mu/kanban/epics/katamorph-canonical-cutover.md` — **P0, status
breakdown** — is the cutover. Its directive: *one contract language, many
interpreters*; katamorph describes the EDN contracts and every system (knoxx,
sol, muse, rheos, eta-mu CLI) interprets that data. Its card 6 is
**`knoxx-katamorph-cutover` (icebox) — "knoxx cuts over last; reference
implementation may lag."**

Its acceptance criteria are already 3 of 5 done: sol cut over, katamorph tagged
with `ProviderContract`, and a `contract-redefinition-guard` exists. The two
open items are `capability-schema-reconciliation` (**ready**) and
`event-ledger-envelope-truth`.

`muse/docs/design/contract-ownership-and-host-translation.md` states the
sequencing directly:

> Knoxx is a downstream composition target. It remains deferred until these
> parts work independently; **code should not be migrated into or out of Knoxx
> to prove this architecture.**

## So what this epic is for

Everything Knoxx can do *without* moving code: become internally lawful and
actor-aware, so that when the upstream seams settle, extraction is mechanical
rather than archaeological.

- separate pure logic, effects, schemas and transformations in place
- carry an actor through every tool call
- make the constitution **enforceable**, not aspirational

## Why compliance has not happened by itself

The cutover epic already diagnosed the root cause, and it applies here verbatim:

> contract discipline is only load-bearing in muse (build fails without the data
> pipeline); in sol/knoxx contracts are optional config, so agents defined
> schemas in place. **Fix = cutover + make it enforceable, not more
> documentation.**

Knoxx already has four gates — `lint` (clj-kondo, with fn-length and
file-length as *errors*), `boundary:check`, `error-boundaries:check`,
`lint:size`. None of them checks layer dependencies. That is the lever.

## The build order to use

From `muse/AGENTS.md`, which shares Knoxx's four-category table and adds the
dependency order that makes disentangling incremental:

```
law → shape → extern → domain → infra
```

`law` first (describes a valid instance; no dependencies), `shape` pure
morphisms depending only on law, `extern` decoding foreign data at the edge,
`domain` pure decisions over shaped data, `infra` orchestrating effects. Any one
tool set can be walked up that order without a flag day.

## Children

- `knoxx-layer-enforcement-gate` — the lever; nothing else here sticks without it
- `knoxx-mcp-actor-ascription`
- `knoxx-deploy-actor-owning-local-credentials`
- `knoxx-tool-namespace-boundary-audit`
- `knoxx-mcp-consent-permission-groups` — blocked on `capability-schema-reconciliation`
- `knoxx-voice-tools-remote-transport`
- `knoxx-tool-vocabulary-rename`

### Rehomed capability work — 2026-08-13

Three children were removed from this compliance catch-all because they now have a more
specific semantic owner:

- `knoxx-translations-event-sourced` -> `knoxx-transduction-provider-pipeline`
- `knoxx-translation-pipeline-validation` -> `knoxx-transduction-provider-pipeline`
- `knoxx-cms-contract-validation` -> `knoxx-resource-repository-cms`

This epic may still enforce layer/actor law around those implementations, but it no
longer owns their product/domain semantics.

## The strategy: work, test, isolate, freeze

Order matters, and it is not the order an agent naturally reaches for.

1. **Get it working exactly as needed.** Not approximately. A feature that half
   works cannot be frozen, and cannot be tested against a definition.
2. **Aggressively test.** The recurring defect class here is a writer and a
   reader that only ever ran together against live infrastructure. Tests at each
   boundary, with a double, are what catch that — five of the eight MCP defects
   would have been caught by one.
3. **Isolate.** Remove all effectual code from all logical code. This is the
   `law → shape → extern → domain → infra` walk, per tool set.
4. **Freeze the feature.** Once isolated and tested, stop changing it. A frozen,
   lawful feature is what makes extraction mechanical later — and what stops the
   next agent from re-entangling it.

**Start with the tools exposed by MCP and their actor contracts.** They are the
smallest complete slice that exercises every layer: a law (who may act as whom),
a shape (the tool/capability projection), an extern (the MCP SDK), a domain
decision (is this grant admissible), and infra (the route).

## Explicit non-goals

- Moving code into eta-mu or katamorph. Knoxx cuts over **last**, upstream first.
- Adopting `katamorph.schema` here. Knoxx does not reference katamorph at all
  today and consumes `open-hax.contract-runtime` instead; that swap belongs to
  `knoxx-katamorph-cutover`, not this epic.
- Any flag-day rewrite. The product stays live throughout.
