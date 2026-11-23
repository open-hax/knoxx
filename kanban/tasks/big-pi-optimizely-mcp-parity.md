---
uuid: "big-pi-optimizely-mcp-parity"
title: "Qualify MCP tools and clients by product version"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Qualify MCP tools and clients by product version

> Parent epic: `big-pi-optimizely-compatibility`

As a coding-agent user, I can discover and invoke documented CMS tools with the appropriate permissions and know whether a tool is local or vendor-compatible.

**Acceptance**

- Pin the applicable official SaaS and CMS 13 MCP tool inventories independently; map names, input/output schemas, errors and annotations, distinguishing local `big_pi_*` tools from official `cms_*` and `graph_*` surfaces.
- Implement the selected supported mapping over the same authorized CMS operations; bind every call to its caller, avoid broad privilege substitution and honor the vendor's token requirements.
- Verify SDK clients over supported transports, negative authentication, schema drift, errors and uncertain writes; publishing requires its own authorization and is not inferred from tool availability.

**Evidence**: real MCP SDK protocol integration tests, a versioned mapping with unsupported entries and selected authorized vendor comparison.
