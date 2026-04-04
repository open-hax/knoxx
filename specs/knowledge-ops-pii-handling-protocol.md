# Knowledge Ops — PII Handling Protocol Spec

> *Classify at ingestion. Isolate by tenant. Encrypt in transit and at rest. Exclude from logs and training by default.*

---

## Purpose

Define the protocol for detecting, classifying, governing, and controlling Personally Identifiable Information (PII) across the full lifecycle of the multi-tenant knowledge ops platform: ingestion, storage, retrieval, translation, logging, training exports, and deletion.

---

## Classification Schema

Use a simple internal label set:

| Level | Meaning | Examples |
| :-- | :-- | :-- |
| PII-0 | No personal data | Public docs, marketing copy |
| PII-1 | Basic identifiers | Name, business email, phone |
| PII-2 | Sensitive personal data | Financial, health, legal, government-linked IDs |
| PII-3 | Regulated/high-risk | SSN, passport, medical records (requires explicit approval paths) |

Classification is applied at ingest and stored as metadata alongside the document.

---

## Default Protocol

### 1. Minimize collection

Only collect data necessary for the workflow. Data you do not retain cannot be stolen.

### 2. Tag at ingest

Every incoming document is tagged with:
- `tenant_id`
- `sensitivity_class` (PII-0 through PII-3)
- `source`
- `retention_policy`
- `allowed_in_prompts` (boolean)
- `allowed_in_logs` (boolean)
- `allowed_in_exports` (boolean)
- `allowed_in_training` (boolean)

### 3. Encrypt everything

All data encrypted in transit (TLS) and at rest (AES-256+). Key management follows current strong protocols. Per-tenant KMS keys for higher isolation tiers.

### 4. Block unscoped access

Every read, search, export, or prompt assembly is explicitly tenant-scoped and role-checked. No exceptions.

### 5. Disable sensitive logging by default

Raw PII is never written to application logs, traces, analytics, or exception dumps. Sensitive responses are not cached casually. Private values are masked or tokenized before logging.

### 6. No training on PII by default

Raw PII does not enter fine-tuning datasets without explicit approval and a documented lawful/business basis. Requires:
- Tenant admin approval
- Documented justification
- Retention limit
- Audit trail

### 7. Enforce retention and deletion

Retention windows are applied automatically. Verified deletion/export procedures are available on request.

---

## System Rules (Non-Negotiable)

| Rule | Rationale |
| :-- | :-- |
| No raw PII in prompts unless required for the task | Minimization control |
| No PII in debug logs, traces, analytics, or exception dumps | Leakage prevention |
| No cross-tenant embeddings or shared training corpora containing PII | Tenant isolation |
| No automatic publication of translated sensitive content without policy checks | Trust model |
| All access must be least-privilege and auditable | Privacy-preserving access |

---

## Operational Workflow

| Stage | Required action |
| :-- | :-- |
| Ingest | Detect and classify PII, attach tenant_id, sensitivity, retention rule, and usage flags before storage |
| Storage | Encrypt at rest, separate by tenant, restrict access by role and policy |
| Retrieval | Filter by tenant and authorization before any generation step |
| Generation | Redact or minimize PII in prompts unless strictly needed |
| Translation | Run glossary/risk checks; route sensitive or low-confidence content to review |
| Logging | Mask or suppress PII; never cache sensitive responses casually |
| Training | Exclude raw PII by default; require approval, traceability, and retention limits for any exception |
| Deletion | Enforce retention windows and verified deletion/export procedures |

---

## Incident Protocol

When PII is exposed, over-retained, misrouted, or used in training without authorization:

1. **Freeze** affected pipelines or exports immediately
2. **Identify** affected tenant, records, systems, and logs
3. **Revoke** exposed credentials or tokens; rotate keys if needed
4. **Preserve** audit evidence; notify internal owners through a defined chain
5. **Remove** or quarantine affected data from prompts, stores, caches, and training datasets

---

## Patterns from TANF-App

Borrowed from [HHS/TANF-app](https://github.com/HHS/TANF-app) (federal welfare case data handling under ATO):

| TANF Pattern | Platform Equivalent |
| :-- | :-- |
| `get_file_shasum()` — SHA-256 on every upload | Integrity check on every ingested document chunk |
| `ClamAVFileScan` — virus scan before storage | Malware gate before ingest acceptance |
| `LogEntry` on every security-relevant action | PII access audit log on every retrieval, review, export |
| `SecurityEventToken` — RISC/OIDC events | Account compromise → revoke sessions + review queue access |
| S3 versioned prefixes per entity | Storage paths: `kb/{tenant_id}/{domain}/{doc_id}/` |
| OWASP ZAP in CI | Automated security scanning in deployment pipeline |

---

## Policy Statement

> "Personal data must be classified at ingestion, isolated by tenant, encrypted in transit and at rest, access-controlled by least privilege, excluded from logs and training by default, and retained only as long as required for the approved business purpose."

---

## Existing Code References

### futuresight-kms (PII fields exist, detection missing)

| File | What it has | Gap |
|------|-------------|-----|
| `packages/futuresight-kms/src/schema/index.ts` | `PiiLeakageEnum` (`:none`, `:suspected`, `:confirmed`), `RiskEnum` (`:safe`, `:sensitive-but-ok`, `:policy-violation`) as label dimensions on review records | PII is only a human label — no automated detection at ingest |
| `packages/futuresight-kms/python/km_labels/models.py` | `LabelDimensions.pii_leakage` (Optional enum), `LabelDimensions.risk` (Optional enum) | No PII metadata fields on documents. No sensitivity_class. No retention_policy |
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | Labels stored with `labels JSONB` column containing pii_leakage/risk values | No log scrubbing helpers. No PII redaction in responses |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | SFT/RLHF export — no PII filtering in export pipeline | Exports do not check `allowed_in_exports` or strip PII before training format |
| `packages/futuresight-kms/Dockerfile` | Python 3.12-slim, runs as non-root user `kms` | Good baseline — no PII-specific container hardening |

### Ragussy (PII detection absent)

| File | What it has | Gap |
|------|-------------|-----|
| `orgs/mojomast/ragussy/backend/app/api/rag.py` | `POST /ingest/text`, `POST /ingest/file` — accepts raw content into Qdrant | No PII scanning, no sensitivity tagging, no quarantine on detection |
| `orgs/mojomast/ragussy/backend/app/services/embeddings_service.py` | BGE-M3 embeddings (dense+sparse) | Embeddings may encode PII verbatim — no redaction before embedding |
| `orgs/mojomast/ragussy/backend/app/core/schemas.py` | Pydantic models for chat/ingest | No PII-related fields on any model |
| `orgs/mojomast/ragussy/backend/app/services/run_store.py` | JSONL event logs + SQLite run index | Logs contain raw chat content — no PII scrubbing |

### Shibboleth (adversarial safety, not corporate PII)

| File | What it has | Gap |
|------|-------------|-----|
| `orgs/octave-commons/shibboleth/src/promptbench/taxonomy/categories.clj` | `def-harm-category` macro — harm hierarchy | Harm categories are adversarial (jailbreak, injection), not corporate PII classes |
| `orgs/octave-commons/shibboleth/src/promptbench/util/crypto.clj` | SHA-256 hashing utilities | Could be reused for document integrity checks (TANF-app pattern) |

### No implementation exists for

- PII-0/1/2/3 classification pipeline
- Sensitivity tagging on ingest
- Log scrubbing
- Redacted preview generation
- Training export PII filtering

---

## Sources

- NIST Privacy Framework v1.0
- OWASP Sensitive Data Exposure
- Azure secure multitenant RAG guidance
- AWS Translate human review workflows
- HHS/TANF-app codebase (SHA-256, ClamAV, LogEntry, RISC tokens, S3 versioning)

---

## Status

Draft — derived from inbox conversation on 2026-04-01.
