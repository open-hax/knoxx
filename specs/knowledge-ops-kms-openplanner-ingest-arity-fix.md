# Knowledge Ops — KMS OpenPlanner Ingest Arity Fix

Date: 2026-04-05
Status: next
Parent: `knowledge-ops-graph-memory-reconciliation.md`
Story points: 2

## Purpose

Restore the Knoxx ingestion worker's ability to write canonical document and graph events into OpenPlanner.

## Problem

The live `kms-ingestion` runtime is repeatedly logging:

- `Wrong number of args (7) passed to: kms-ingestion.jobs.worker/ingest-via-openplanner!`

This causes endless OpenPlanner backpressure and prevents canonical graph/data population.

## Goals

1. Fix the call/definition mismatch for `ingest-via-openplanner!`.
2. Restore successful OpenPlanner ingestion for the local stack.
3. Stop the repeated backpressure loop caused by this bug.

## Non-Goals

1. Full ingestion-throttling redesign.
2. Myrmex write-path repair.
3. New ingestion features.

## Affected files / surfaces

- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj` (if helper call signatures must align)
- `orgs/open-hax/knoxx/ingestion/test/**` as needed

## Verification

1. `kms-ingestion` no longer logs the arity error.
2. Ingestion jobs progress past the previous failure point.
3. OpenPlanner receives new events from KMS ingestion.

## Definition of done

- Arity bug fixed at source.
- Runtime logs confirm successful writes.
- Backpressure is no longer dominated by this failure mode.
