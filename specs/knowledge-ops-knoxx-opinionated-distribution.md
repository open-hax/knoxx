# Knowledge Ops — Knoxx as Opinionated Distribution

Date: 2026-04-05
Status: active architecture doctrine

## Purpose

Define Knoxx as an **opinionated distribution** of a family of more generic subsystems.

This document exists to make one distinction explicit:

- the subsystems should remain generic, categorical, and reusable
- Knoxx should be the integrated package with stronger product opinions, defaults, and client-facing tradeoffs

## Claim

Knoxx is not the ontology of the whole ecosystem.
Knoxx is the **packaged product cut**.

The subsystems beneath Knoxx should each be able to stand alone.
Knoxx is the low-cost, strongly-integrated distribution a client can adopt when they need the whole stack without funding a custom architecture program.

If a client needs more customization, the subsystem layer remains flexible.

## Current reading

The ecosystem is converging toward these roles:

- **Cephalon** — head / agentic loop / context compiler / tool router
- **OpenPlanner** — canonical lake for memory and graph receipts
- **Graph-Weaver** — graph workbench and intervention surface
- **Graph-Weaver-ACO** — tiny traversal engine
- **Myrmex** — web foraging orchestrator over ACO + rich extraction + lake writes
- **Proxx** — inference/proxy/gateway surface where applicable
- **Knoxx** — opinionated integrated distribution over those subsystems

## Why this distinction matters

Without this distinction, one of two failures happens:

### Failure mode 1 — Knoxx becomes a monolith

Every subsystem gets reimplemented inside Knoxx until reuse dies and upstream repos become decorative.

### Failure mode 2 — no product ever stabilizes

Everything stays abstract and generic, but no one ships a coherent package a small client can actually buy and run.

The correct move is:

> subsystems remain flexible; Knoxx is where strong opinions become a product.

## Distribution doctrine

### Subsystems should remain generic

Subsystem repos should optimize for:

- clear ownership
- reusable contracts
- narrow responsibilities
- low policy coupling
- portability across multiple products or deployments

Examples:

- OpenPlanner should not be Knoxx-only memory storage.
- Graph-Weaver should not be Knoxx-only graph UI.
- Myrmex should not be Knoxx-only crawl logic.
- Cephalon should not be Knoxx-only head/runtime doctrine.

### Knoxx should remain opinionated

Knoxx should own:

- integration defaults
- product packaging
- deployment shape
- policy posture
- workbench UX
- low-cost client path
- curated combination of subsystems into one coherent offering

Knoxx is where the ecosystem stops being a pile of organs and becomes a creature a client can actually adopt.

## Desired Knoxx posture

### 1. Cephalon owns the agentic loop

Long-term, Knoxx should not invent an unrelated private agent runtime if Cephalon is already the head/runtime family.

The intended direction is:

- Cephalon owns the agentic loop
- Knoxx packages that loop into the client-facing workbench and operations product

### 2. OpenPlanner owns canonical truth

Knoxx should consume OpenPlanner as:

- the memory/event lake
- the graph receipt ledger
- the graph export/query base truth

### 3. Graph-Weaver owns graph workbench behavior

Knoxx may embed or adapt Graph-Weaver surfaces, but should not duplicate the graph workbench contract unnecessarily.

### 4. Myrmex owns web foraging composition

Knoxx should consume Myrmex as the web graph ingestion organism rather than burying that responsibility in ad hoc local services.

## Product tiers implied by this doctrine

### Tier A — Knoxx distribution

For clients who need:

- low cost
- fast deployment
- strong defaults
- integrated operator UX
- a coherent stack without bespoke systems work

Offer Knoxx as the packaged opinionated distribution.

### Tier B — customized subsystem composition

For clients who need:

- deeper customization
- partial adoption
- unusual deployment or policy requirements
- selective replacement of one or more organs

Use the subsystem layer directly:

- Cephalon
- OpenPlanner
- Graph-Weaver
- Myrmex
- Proxx
- adjacent doctrine/runtime layers as needed

## Architectural consequence

This doctrine means Knoxx should prefer:

- adapting subsystem contracts
- composing subsystem services
- theming and policying the product layer

over:

- re-implementing generic subsystem logic inside Knoxx

## Relationship to existing knowledge-ops planning

This reading fits the existing roadmap:

- immediate Knoxx work is still tenant/runtime enforcement and graph-memory coherence
- those repairs are about making the distribution trustworthy
- they do not negate the deeper subsystem boundaries

In other words:

> Knoxx is the package. The subsystems are the organs. Repairing the package does not mean the organs stop existing.

## Definition of done

This doctrine is successful when future work can answer all of these cleanly:

1. What belongs in Knoxx because it is product opinion?
2. What belongs in subsystem repos because it is generic reusable machinery?
3. Why is Knoxx the low-cost packaged offer?
4. How do we customize for clients without forking the whole organism?
