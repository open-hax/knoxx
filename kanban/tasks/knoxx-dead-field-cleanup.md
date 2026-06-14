---
uuid: "knoxx-dead-field-cleanup"
title: "Dead Field Cleanup"
status: done
priority: "P2"
labels: ["tasks", "2sp", "resource-architecture"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 2
category: "tasks"
---
# Dead Field Cleanup

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`

## Context

Many fields in schemas and contracts are dead — never read by runtime code. Verified against code as of 2026-06-10.

## Work

### Remove from schemas (`schema.cljs`, `law/contracts.cljs`):
- `:contract/version` (all resource kinds)
- `:trigger/domain`
- `:trigger/predicate`
- `:action/responds-to`
- `:action/result`
- `:action/params`
- `:hooks` (agent contracts)
- `:events` (agent contracts — forbidden by catalog)
- `:source-kind` (agent contracts)
- `:source-mode` (agent contracts)
- `:sub-agents` (agent contracts)

### Remove from contracts:
- All empty `:hooks {}` blocks
- Unused `:data` fields (keep only where fallback paths exist)
- Dead `:events` blocks on agent contracts

### Update AGENTS.md:
- Remove dead field references from authoring guide

## Definition of Done

- [ ] Dead fields removed from schemas
- [ ] Dead fields removed from contracts
- [ ] AGENTS.md updated
- [ ] Tests pass

## Risks

- Breaking contracts that use dead fields
- Need to verify no code reads them
