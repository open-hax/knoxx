---
uuid: "big-pi-optimizely-tenant-security"
title: "Connect and qualify authorized vendor tenants"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Connect and qualify authorized vendor tenants

> Parent epic: `big-pi-optimizely-compatibility`

As an operator, I can connect a designated tenant with the right user authority and prevent one tenant's data or credentials from leaking into another.

**Acceptance**

- Implement the selected vendor edition's OAuth and token lifecycle and per-user or explicitly documented service-principal authorization. Preserve Knoxx org and actor authority; do not accept them as untrusted request fields.
- Reject cross-tenant reads and writes without existence disclosure, protect secrets and logs, and define revocation, failure, retry and destination inspection for ambiguous writes.
- Run the bridge against the actual authenticated Knoxx CMS and an authorized vendor tenant, proving exact version plus ETag and payload, metadata preservation, stale conflicts and draft-only behavior.

**Evidence**: redacted live integration run, tenant-isolation and denied-access tests, credential handling review and reproducible setup instructions.
