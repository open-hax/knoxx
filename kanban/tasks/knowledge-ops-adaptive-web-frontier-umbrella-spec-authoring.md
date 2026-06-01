---
uuid: "knoxx-knowledge-ops-adaptive-web-frontier-umbrella-spec-authoring"
title: "Knowledge Ops — Author Umbrella Spec: Adaptive Web Frontier + Multiscale Backbone"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-29T00:00:00Z"
points: 2
category: tasks
---

# Knowledge Ops — Author Umbrella Spec: Adaptive Web Frontier + Multiscale Backbone

> Parent: `knoxx-knowledge-ops-adaptive-web-frontier-and-multiscale-backbone`
> Points: 2

## Purpose

Create the canonical parent spec file that does not yet exist at `orgs/open-hax/knoxx/specs/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`. This is the cross-repo coordination doc that child implementation specs reference as their parent.

## Problem

The companion implementation specs (`openplanner-web-edge-salience-and-backbone-projections.md`, `adaptive-frontier-salience-and-template-aware-pruning.md`) already reference this parent spec by path, but the file does not exist. Without it, there is no single place that explains the cross-system design intent and sequencing.

## Goals

1. Write the parent spec at `orgs/open-hax/knoxx/specs/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`.
2. Document the multi-repo responsibility split: OpenPlanner (lake + projection), Myrmex (web producer + adaptive expand), graph-weaver-aco (traversal brain), shuvcrawl (fetch/render/extraction).
3. Document the 4-phase delivery sequence matching the OpenPlanner companion spec phases.
4. Reference the child implementation specs so readers can navigate to implementation detail.

## Non-Goals

- Writing implementation code.
- Modifying the companion specs (they are already detailed).

## Affected files

- `orgs/open-hax/knoxx/specs/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md` (create)

## Definition of done

- File exists at `orgs/open-hax/knoxx/specs/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md`.
- File clearly names all four canonical repos and their roles.
- File lists the 4 delivery phases (Projection tables, Salience materialization, Backbone materialization, Explainability surfaces).
- File references both companion implementation specs with correct relative paths.
- `grep "orgs/octave-commons/myrmex" orgs/open-hax/knoxx/specs/knowledge-ops-adaptive-web-frontier-and-multiscale-backbone.md` returns a match.
