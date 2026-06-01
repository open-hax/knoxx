---
uuid: "knoxx-knowledge-ops-pass5-rbac-refresh"
title: "Knowledge-ops consistency pass 5: RBAC role and policy refresh"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Knowledge-ops consistency pass 5: RBAC role and policy refresh

> Parent epic: `knoxx-knowledge-ops-consistency-review`
> Points: 3

## Purpose

Verify that role names, actor identifiers, and policy gates referenced in the knowledge-ops specs match the current contract definitions under `contracts/`, removing stale or invented role names introduced before the contracts layer was formalised.

## Scope

- Cross-reference every role name and actor identifier mentioned in the 34 specs against the live contract files under `orgs/open-hax/openplanner/packages/agents/knoxx/contracts/`
- Flag and correct any spec that references a role not defined in contracts (e.g. old names like `admin` instead of `system-admin`, `viewer` instead of `read-only`)
- Where a spec describes a new access rule that has no corresponding contract entry, add a TODO note for a follow-up contract authoring task

## Definition of done

- All role and actor names in the 34 specs resolve to an entry in the live contracts directory
- No spec silently assumes a role that does not exist in contracts
- Any gaps are annotated with a TODO referencing `knoxx-contract-authoring` for follow-up

## Notes

Split from parent epic `knoxx-knowledge-ops-consistency-review` on 2026-05-30.
