---
uuid: "knoxx-translation-pipeline-validation"
title: "Validate the whole translation pipeline end to end"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent", "translations", "validation"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Validate the whole translation pipeline

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The translation pipeline has never been validated end to end. It is on the
production deploy's health gate, so a silent break there fails every deployment
— and it has already been the subject of several emergency fixes (#210, #211,
and the Mongo boundary migration).

## Scope

Walk the pipeline and assert each hop, from ingestion to a rendered translation:

- document/segment ingestion
- segment identity and tenant scoping
- the translate call itself and its provider path
- `assert-translated!` (currently rejects a translation byte-identical to its
  source — confirm that is the rule we want)
- persistence (see `knoxx-translations-event-sourced`)
- read-back through `/api/translations/segments` and the Studio surface
- the labelling/review states (`pending`, `in_review`, `approved`, `rejected`)

## Approach

- Prefer contract tests at each boundary over one long end-to-end script; the
  recurring defect class in this codebase is a writer and a reader disagreeing,
  which a boundary test catches and an integration test only sometimes does.
- `law.openplanner-translation` already holds Malli contracts for this boundary
  — extend rather than invent.

## Done when

- Each hop has a test that fails when that hop is broken, independently.
- A deliberate break anywhere in the chain is attributable from the failure alone.

## Prior art on this board

- **`knowledge-ops-translation-mt-pipeline`** (done, LANDED) — the MT pipeline
  itself. This card validates it rather than builds it.
- **`knowledge-ops-translation-routes`**, **`knowledge-ops-translation-export`**,
  **`knowledge-ops-translation-document-review-v2`** — check their state before
  scoping; parts of the chain may already have coverage.
