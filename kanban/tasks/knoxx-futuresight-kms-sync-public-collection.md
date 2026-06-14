---
uuid: "knoxx-futuresight-kms-sync-public-collection"
title: "Write sync_public_collection.py to mirror published docs to Qdrant"
status: accepted
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Write sync_public_collection.py to mirror published docs to Qdrant

> Parent epic: `knoxx-knowledge-ops-chat-widget-layers`
> Points: 2

## Purpose

Deliver the script that keeps the `public_docs` Qdrant collection in sync with the CMS, ensuring the Layer 1 chat widget only retrieves knowledge that has been explicitly published through the CMS boundary.

## Scope

Create `packages/futuresight-kms/scripts/sync_public_collection.py`:

- Queries `GET /api/cms/public` (or directly from the `documents` table) to retrieve all docs with `visibility = "public"`
- Upserts each document into the Qdrant `public_docs` collection (embed with the same model used by `ingest_docs.py`)
- Deletes vectors for documents that have been archived or reverted to `internal`/`review` since the last sync run — the `public_docs` collection must not contain stale entries
- Accepts CLI args: `--km-labels-url`, `--qdrant-url`, `--collection` (default `public_docs`), `--dry-run`
- Exits non-zero on any upsert/delete failure so it is safe to call from a cron job or a CMS publish webhook

The script should be callable from the CMS publish action (`POST /api/cms/publish/{id}`) as a subprocess or post-commit hook, and independently as a scheduled full-reconciliation job.

## Definition of done

- Running the script against a test km_labels instance with one `public` document and one `archived` document results in exactly one vector present in `public_docs`
- `--dry-run` flag prints the add/remove plan without writing to Qdrant
- The script reuses the same embedding call pattern as `services/futuresight-kms/scripts/ingest_docs.py` (no duplicate embedding logic)
- Script exits 0 on success and prints a summary line: `Synced N documents to public_docs (added A, removed R)`

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-widget-layers` on 2026-05-30.
