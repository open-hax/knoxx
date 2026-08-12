---
uuid: "knoxx-contract-owned-publication-pipeline"
title: "Contract-owned document publication — remove OpenPlanner as publication authority"
status: breakdown
priority: P1
labels: ["epics", "cms", "publication", "translations", "decouple", "openplanner"]
created_at: "2026-08-12T00:00:00Z"
points: 0
category: epics
---
# Contract-owned document publication — remove OpenPlanner as publication authority

## Signal

Knoxx must be able to describe **which documents exist and their intended publish state from resources alone**.

OpenPlanner may remain a storage/projection integration, but it must no longer be the semantic authority for gardens, document publication state, translation policy, or translation pipeline configuration.

```text
resource graph                = desired semantic state
runtime receipts/projections  = observed execution state
drift                         = desired - observed
```

A Knoxx process with no OpenPlanner REST service available must still answer what documents exist, which gardens/locales/revisions they target, whether publication is requested/withheld/archived, which translation/review gates apply, and what remains blocked.

It must not infer those facts from `garden_publications` metadata or `/api/openplanner/...` reads.

## Ownership rule

```text
Knoxx resources      own desired state
Knoxx law/domain     own admissibility and reconciliation decisions
Knoxx receipts       own observed execution facts
publication adapters perform effects
OpenPlanner          owns only its adapter/projection implementation
```

Publication is a relation:

```text
document × garden × locale × revision -> publication intent
```

## Priority waves

Priority is dependency urgency, not optionality.

```text
P0 — foundation + authority transfer
  resource contracts
  intent resolver
  legacy publication migration

P1 — runtime semantics + reconciliation — 18sp
  translation config authority ............ 5
  translation/review publication gate ..... 5
  publication adapter boundary ............. 8
      reconciliation plan laws ............. 3
      adapter effects + idempotency ......... 3
      receipts + fake-adapter proof ......... 2

P2 — cutover + retirement + proof
  CMS resource-backed publication UI
  OpenPlanner REST retirement
  no-OpenPlanner E2E gate
```

## Children

1. **P0 / breakdown / 5sp** `knoxx-publication-resource-contracts` — first-class document, garden, and publication resource laws.
2. **P0 / accepted / 5sp** `knoxx-publication-intent-resolver` — pure resource graph -> desired publication projection.
3. **P0 / accepted / 5sp** `knoxx-openplanner-publication-state-migration` — import existing gardens/publications into resources once, with conflict receipts, before resource intent becomes CMS authority.
4. **P1 / accepted / 5sp** `knoxx-translation-pipeline-config-resource` — remove OpenPlanner translation config authority across UI and ingestion worker.
5. **P1 / accepted / 5sp** `knoxx-translation-publication-gate` — compute translation/review blockers and derivative replacement work from receipts.
6. **P1 / breakdown / 0sp roll-up** `knoxx-publication-adapter-boundary` — coordination card for the original 8sp scope:
   - **P1 / accepted / 3sp** `knoxx-publication-reconcile-plan-laws`
   - **P1 / accepted / 3sp** `knoxx-publication-adapter-effects-idempotency`
   - **P1 / accepted / 2sp** `knoxx-publication-receipts-fake-adapter-proof`
7. **P2 / accepted / 5sp** `knoxx-cms-resource-backed-publication-ui` — make CMS read/write resource intent after migration has converged.
8. **P2 / accepted / 3sp** `knoxx-openplanner-rest-retirement` — delete CMS/translation REST compatibility authority and deploy flag.
9. **P2 / accepted / 5sp** `knoxx-contract-publication-e2e` — prove publish/translate/review/materialize with OpenPlanner REST absent.

## Build order

Migration must converge before the CMS authority cutover. Within the adapter boundary, pure law precedes effects, and effects precede receipt/proof closure.

```text
resource contracts
      -> intent resolver
      -> OpenPlanner state migration + conflict resolution
      -> translation config resource
      -> translation/review gate
      -> reconciliation plan laws
      -> adapter effects + idempotency
      -> receipts + fake-adapter proof
      -> CMS resource-authority cutover
      -> OpenPlanner REST retirement
      -> no-OpenPlanner E2E gate
```

## Explicit non-goals

- Do not replace OpenPlanner authority with another mutable garden database authority.
- Do not store runtime timestamps, job ids, worker phases, or deploy status in declarative resources.
- Do not promote `:translating` or `:reviewing` into desired publication state.
- Do not duplicate `knoxx-translations-event-sourced`; translation history remains that card's concern.
- Do not require the publication adapter to be OpenPlanner-specific.

## Done when

- Resources alone reconstruct desired document/garden/locale/revision topology.
- Existing OpenPlanner topology is migrated and conflicts resolved before CMS resource authority is enabled.
- Translation/review evidence gates publication without becoming desired state.
- The adapter boundary is proven as pure plan -> replaceable effects -> receipts, with the 3+3+2 children complete.
- CMS actions mutate resource intent rather than OpenPlanner metadata.
- OpenPlanner is optional at the semantic layer.
- Production deploy verification no longer conditionally skips CMS because OpenPlanner REST is absent.
- The E2E publishes a translated, reviewed document with OpenPlanner REST deliberately unavailable.
