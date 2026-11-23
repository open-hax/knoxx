---
uuid: "big-pi-optimizely-api-coverage"
title: "Qualify the CMS API against versioned contracts"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Qualify the CMS API against versioned contracts

> Parent epic: `big-pi-optimizely-compatibility`

As an integrator, I can tell exactly which CMS operation, request and response is supported for the selected edition and version.

**Acceptance**

- Obtain and pin an authorized vendor OpenAPI artifact and track all documented operations and models in an edition-specific matrix; distinguish schemas from routes and local extensions from vendor endpoints.
- Implement and verify remaining adopted CMS resources and lifecycle semantics, including content, content types, locales, apps, blueprints, sources, bindings, templates, property formats/groups and manifest, or mark each explicitly unsupported.
- Qualify validation, pagination, errors, conditional operations, versioning, media, Graph and delivery as separate profiles; do not infer CMS 12 or CMS 13 compatibility from SaaS v1.

**Evidence**: automated contract suite and recorded differential tests against a permitted selected edition, plus updated compatibility registry and human verification script.
