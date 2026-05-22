# Knowledge Ops — Demo Seed Spec

> *One script turns the devel workspace into a working tenant with populated collections and a review queue.*

---

## Purpose

Define what `seed_devel_tenant.py` creates so the devel workspace demonstrates all five layers end-to-end.

---

## What the Seed Script Does

### Step 1: Create the `devel` tenant

```python
POST /api/km-labels/tenants/
{
    "tenant_id": "devel",
    "name": "Devel Workspace",
    "domains": ["docs", "specs", "services", "packages", "inbox"],
    "config": {
        "isolation_mode": "pool",
        "default_model": "glm-4.7-flash",
        "embedding_model": "BAAI/bge-m3",
        "embedding_dim": 1024,
        "supported_languages": ["en"],
        "public_collection": "public_docs",
        "internal_collections": ["devel_docs", "devel_specs"]
    }
}
```

### Step 2: Ingest docs into Qdrant collections

Run `ingest_docs.py` three times:

| Command | Collection | Source | Purpose |
|---------|-----------|--------|---------|
| `python ingest_docs.py ~/devel/docs --collection devel_docs` | `devel_docs` | `docs/` | Layer 3 internal knowledge |
| `python ingest_docs.py ~/devel/specs --collection devel_specs --ext .md` | `devel_specs` | `specs/` | Layer 3 design docs |
| `python ingest_docs.py ~/devel/services --collection devel_docs --ext .md` | `devel_docs` | `services/*/README.md` | Layer 3 service docs |

**Skip**: `orgs/`, `node_modules/`, `.git/`, hidden files, files < 100 chars.

### Step 3: Create sample documents in CMS

Insert 3-5 documents into the `documents` table to demonstrate the visibility model:

| Doc | Title | Visibility | Source | Content |
|-----|-------|-----------|--------|---------|
| 1 | "What is Promethean?" | `public` | ai-drafted | Summary of the devel stack from ingested docs |
| 2 | "Deploying Futuresight KMS" | `public` | ingested | From `services/futuresight-kms/README.md` |
| 3 | "Internal Architecture Notes" | `internal` | manual | From `docs/MASTER_CROSS_REFERENCE_INDEX.md` |
| 4 | "New Feature Blog Draft" | `review` | ai-drafted | Draft generated from specs |
| 5 | "API Reference (Archived)" | `archived` | ingested | Old API doc, pulled from public |

Then sync docs 1 and 2 to Qdrant `public_docs` collection (they are `public`).

### Step 4: Create sample review items

Insert 3-5 rows into `km_labels` table to demonstrate Layer 5:

| Item | Question | Answer | Labels |
|------|----------|--------|--------|
| 1 | "How do I deploy futuresight-kms?" | "Run docker compose up..." | correctness: correct, overall: approve |
| 2 | "What is Shibboleth?" | "A Clojure DSL for adversarial prompt evaluation" | correctness: correct, overall: approve |
| 3 | "How do I reset my SSO token?" | "Navigate to Settings → Security" | correctness: partially-correct, overall: needs-edit, gold_answer: "Contact your admin at admin@..." |
| 4 | "What services does this team offer?" | "We offer AI consulting..." | correctness: incorrect, overall: reject |

### Step 5: Sync `public_docs` collection

For each `visibility=public` document, call:

```
POST Ragussy /api/rag/ingest/text
{
    "text": "<document content>",
    "source": "cms:<doc_id>",
    "collection": "public_docs",
    "metadata": { "tenant_id": "devel", "doc_id": "<doc_id>", "visibility": "public" }
}
```

---

## Script Structure

```python
#!/usr/bin/env python3
"""Seed the devel tenant with docs, collections, and sample data."""

import httpx
import os
import sys

KM_LABELS_URL = os.getenv("KM_LABELS_URL", "http://localhost:3002")
RAGUSSY_URL = os.getenv("RAGUSSY_URL", "http://localhost:8000")
TENANT_ID = "devel"

def main():
    # 1. Create tenant
    create_tenant()

    # 2. Ingest docs into Qdrant
    ingest_collections()

    # 3. Create CMS documents
    create_cms_documents()

    # 4. Create review items
    create_review_items()

    # 5. Sync public docs to public_docs collection
    sync_public_collection()

    print("\n✓ Seed complete")
    print(f"  Tenant: {TENANT_ID}")
    print(f"  Collections: devel_docs, devel_specs, public_docs")
    print(f"  CMS docs: 5 (2 public, 1 review, 1 internal, 1 archived)")
    print(f"  Review items: 4")
    print(f"\n  Layer 1 widget: POST /api/widget/chat (public_docs)")
    print(f"  Layer 2 CMS: GET /api/cms/documents")
    print(f"  Layer 3 search: POST /api/rag/search (devel_docs)")
    print(f"  Layer 5 review: GET /api/km-labels/devel")

if __name__ == "__main__":
    main()
```

---

## What You See After Seeding

| Layer | Endpoint | Response |
|-------|----------|----------|
| 1. Widget | `POST /api/widget/chat { "message": "What is Promethean?" }` | Answer from `public_docs` with citations |
| 2. CMS | `GET /api/cms/documents` | 5 docs with visibility badges |
| 2. CMS | `GET /api/cms/documents?visibility=public` | 2 published docs |
| 2. CMS | `POST /api/cms/draft { "topic": "Our coding standards" }` | AI-generated draft from `devel_docs` |
| 3. Search | `POST /api/rag/search { "query": "deployment", "collection": "devel_docs" }` | Internal docs about deployment |
| 5. Review | `GET /api/km-labels/devel` | 4 labeled items, 1 needs-edit |
| 5. Export | `GET /api/export/devel/sft` | JSONL training data from approved items |

---

## Files to Create

| File | Purpose |
|------|---------|
| `services/futuresight-kms/scripts/seed_devel_tenant.py` | Main seed script |
| `services/futuresight-kms/scripts/seed_cms_docs.py` | CMS document creation helper |
| `services/futuresight-kms/scripts/seed_review_items.py` | Review item creation helper |

## Files to Reference

| File | Why |
|------|-----|
| `services/futuresight-kms/scripts/ingest_docs.py` | Reuse for Qdrant ingestion |
| `packages/futuresight-kms/python/km_labels/routers/tenants.py` | Tenant creation endpoint |
| `packages/futuresight-kms/python/km_labels/routers/cms.py` | CMS document creation (to be built) |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Review item creation endpoint |

---

## Status

Specified 2026-04-01. Blocking questions resolved.
