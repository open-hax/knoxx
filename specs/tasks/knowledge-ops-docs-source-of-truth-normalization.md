# Knowledge Ops — Docs Source-of-Truth Normalization

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 2

## Purpose

Normalize the docs so current readers stop getting conflicting stories about what Knoxx, OpenPlanner, and Graph-Weaver are today.

## Problem

The current doc set mixes:

- stale READMEs
- donor-era knowledge-ops docs
- current source/runtime behavior

This increases planning overhead and causes architectural drift in future work.

## Goals

1. Point readers at the reconciliation spec as the current-state anchor.
2. Correct obviously stale backend/runtime descriptions.
3. Make source-home vs runtime-home explicit where needed.

## Non-Goals

1. Rewriting the entire knowledge-ops corpus.
2. Perfecting product messaging.
3. Deleting historical donor material.

## Affected files

- `orgs/open-hax/knoxx/README.md`
- `orgs/open-hax/knoxx/specs/README.md`
- `orgs/open-hax/openplanner/README.md`
- `services/knoxx/README.md` as needed

## Verification

1. README-level readers land on the right current-state docs quickly.
2. Knoxx backend is no longer described as the old Python/FastAPI implementation where that is false.

## Definition of done

- The obvious current-state doc contradictions are removed or superseded.
