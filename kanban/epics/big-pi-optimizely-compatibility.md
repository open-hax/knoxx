---
uuid: "big-pi-optimizely-compatibility"
title: "Big Pi — Optimizely CMS, API and MCP compatibility"
status: "incoming"
priority: "P2"
labels: ["epics", "cms", "resources", "optimizely", "mcp"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: "epics"
---

# Big Pi — Optimizely CMS, API and MCP compatibility

> Parent: `knoxx-resource-repository-cms`. Big Pi is the resource CMS/publication work being extracted; Mu owns review, acceptance and terminology decisions. Both currently live in Knoxx. Keep this child under the existing parent while product boundaries mature.

## User outcome

As a technical content maintainer, I can author content and language versions in a local or qualified Optimizely-compatible environment, send an explicitly identified version to the existing Knoxx workflow, and return an authorized result as a draft with traceable source evidence. I can operate the supported API, MCP and UI and understand which product and edition each surface targets.

The first executable slice is `packages/big-pi-optimizely`: an unofficial local SaaS v1 model with a small authoring UI, local HTTP/MCP tools, and a bridge to Knoxx CMS document history. It is not full API, UI, MCP, Graph, CMS 12/13 or vendor parity. Local fixture tests do not establish successful operation against the running Knoxx application or a real vendor tenant. Exact route coverage lives in `packages/big-pi-optimizely/compatibility.edn`. The broad work below remains incoming; no original board state is changed.

## Child stories

- `big-pi-optimizely-provider-contract` — provider-neutral resource authority and retained exact closure.
- `big-pi-optimizely-api-coverage` — edition-pinned CMS API coverage and conformance.
- `big-pi-optimizely-tenant-security` — vendor authentication, isolation and live tenant qualification.
- `big-pi-optimizely-mcp-parity` — versioned official MCP tool discovery and tested integration.
- `big-pi-optimizely-authoring-ui` — practical authoring, accessibility and human verification.
- `big-pi-optimizely-review-publication` — Mu review evidence, draft return, publication and delivery separation.

## Boundary and completion

The existing Clio CMS bridge is not the generic resource repository provider. Fake/file repository implementations, exact historical closure, authorized atomic observations and compatible Optimizely provider laws must be implemented and proved by the parent contracts before claiming substitution. CMS writes can branch, vendor version IDs may mutate, and there is no cross-system transaction. Preserve source key, version, locale, ETag and payload; return drafts only until separately authorized publication work is implemented. A chosen revision is not an approval.

The workstream is complete only when each adopted profile has a pinned, versioned vendor specification and documented operation/tool/schema matrix, executable conformance evidence, a runnable and accessible authoring path, authorized tenant and real Knoxx tests, and explicit evidence for review and publication boundaries. Full-scope backlog is retained; completion is never inferred from the initial local subset.

---
Added the initial local CMS/API/MCP package and exact-revision Knoxx bridge. The six child stories retain full provider, edition/API, tenant, MCP, authoring and Mu/publication qualification. No full-parity or live-tenant completion is claimed. --tasks-dir kanban
---
