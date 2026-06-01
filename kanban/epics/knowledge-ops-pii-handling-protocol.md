---
uuid: "knoxx-knowledge-ops-pii-handling-protocol"
title: "Knowledge Ops — PII Handling Protocol Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.391Z"
source: "specs/epics/knowledge-ops-pii-handling-protocol.md"
points: null
category: "epics"
---

# Knowledge Ops — PII Handling Protocol Spec

> Source: `specs/epics/knowledge-ops-pii-handling-protocol.md`

> *Classify at ingestion. Isolate by tenant. Encrypt in transit and at rest. Exclude from logs and training by default.*

---
## Purpose

Define the protocol for detecting, classifying, governing, and controlling Personally Identifiable Information (PII) across the full lifecycle of the multi-tenant knowledge ops platform: ingestion, storage, retrieval, translation, logging, training exports, and deletion.
---

## Classification Schema

Use a simple internal label set:

| Level | Meaning | Examples |

---
## Triage Notes (2026-05-28)

- **Status**: Keep as breakdown. 182-line protocol spec.
- **Action needed**: Split into implementable tasks (est. 3-4 tasks, each ≤5sp).
- **Key deliverables**:
  - PII detection/classification at ingestion
  - Tenant isolation enforcement
  - PII redaction in logs and training exports
  - Deletion/retention protocol
- **Assessment**: Standalone protocol. Can be implemented independently of other knowledge-ops work. Priority depends on whether multi-tenant PII is a current requirement.

Breakdown 2026-05-29: Codebase inspection confirms zero PII implementation. Tenant isolation is structurally present in the DB schema (tenants table, tenant_id FKs on ingestion_sources / ingestion_jobs / ingestion_file_state) but no PII detection, classification, redaction, or retention logic exists anywhere. Auth session crypto (AES-256-GCM) is unrelated to content at-rest encryption. Translation SFT export route (/api/translations/export/sft) emits raw content with no redaction. Epic split into 4 tasks each ≤3sp. --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban

Breakdown 2026-05-29: Codebase inspection confirms zero PII implementation. Tenant isolation is structurally present in the DB schema (tenants table, tenant_id FKs on ingestion_sources / ingestion_jobs / ingestion_file_state) but no PII detection, classification, redaction, or retention logic exists anywhere. Auth session crypto (AES-256-GCM) covers auth tokens only — content is stored raw. Translation SFT export route (/api/translations/export/sft) emits raw content with no redaction pass. Epic too large (13sp) — split into 4 tasks each ≤3sp. Verdict: epic. --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
