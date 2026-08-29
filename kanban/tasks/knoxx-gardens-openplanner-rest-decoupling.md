---
uuid: "knoxx-gardens-openplanner-rest-decoupling"
title: "Gardens — Decouple Legacy OpenPlanner REST Transport"
status: "in_progress"
priority: "P1"
points: "3"
labels:
  - tasks
  - publication
  - gardens
  - has-parent
write-id: "1787759429714-0.pjnl4gjhlui9btdsl5"
---

# Gardens — Decouple Legacy OpenPlanner REST Transport

> Parent epic: `knoxx-publication-runtime-follow-up`

## Purpose

Remove the remaining supported Gardens publication/viewer dependency on `/api/openplanner/v1/gardens` by targeting a Knoxx-owned Gardens domain contract.

## Work

- Inventory all `/api/openplanner/v1/gardens` callers and every deployment/runtime use of `OPENPLANNER_API_KEY`.
- Define a canonical Gardens read/write/publication contract at the Knoxx domain boundary.
- Migrate supported garden selection, publication, metadata, and viewer consumers to that contract.
- Preserve public garden/viewer behavior and publication garden/revision/path/locale identity.
- Remove `OPENPLANNER_API_KEY` from services/deployment configuration only after inventory proves no supported caller still needs it.
- Add contract/integration coverage for the migrated path.

## Definition of Done

- No supported Gardens publication/viewer caller directly invokes `/api/openplanner/v1/gardens`.
- Gardens consumers use one Knoxx-owned domain contract.
- Publication identity semantics do not change.
- Existing public garden viewer behavior remains green.
- `OPENPLANNER_API_KEY` is removed if no remaining supported surface requires it; otherwise the remaining owner is explicitly documented outside this task.
- Relevant frontend/backend tests and builds pass.
