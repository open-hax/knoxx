---
category: "epics"
labels: ["epics", "cms", "publication", "translations", "decouple", "openplanner"]
write-id: "1786565813522-0.gbpa8mxtyivpbk4qv5v"
points: "0"
title: "Contract-owned document publication — remove OpenPlanner as publication authority"
priority: "P1"
status: "breakdown"
uuid: "knoxx-contract-owned-publication-pipeline"
created_at: "2026-08-12T00:00:00Z"
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

1. **P0 / ready / 5sp** `knoxx-publication-resource-contracts` — first-class document, garden, and publication resource laws.
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

---
Breakdown 2026-08-12: epic held in breakdown as the container for the twelve task cards. All eleven implementable children are now ready; the epic closes behind them.

---

***

## Decision 2026-08-19 — how the pipeline actually gets wired in

**This is what the stack hit a wall on, recorded so the next attempt does not
rediscover it.** The epic's pure layers are built and tested: the resolver, the
migration decision logic, the gate, the plan laws, the effect boundary, receipts.
What does not exist is anything that *runs* them. `migrate-publication-records!`
has no production caller and none of its three effects — legacy reader, resource
writer, durable append-once receipt store — has an implementation. Review
correctly flagged it on PR #232; it is not an oversight in that card, it is a
missing spec for the whole seam.

**Direction (author, 2026-08-19):** rather than wire these namespaces into the
main application directly, stand the pipeline up as a **separate service exposing
the same API the old OpenPlanner system did**, and have the main application
consume that. For now. That keeps the main app's client contract unchanged while
the authority moves, and it gives the pure layers a single runtime home instead of
being called from inside the backend on an undesigned path.

Consequences worth naming before anyone starts:

- The spec for that service does not exist yet. It is the prerequisite, not a
  detail — the API surface it must reproduce is whatever the main application
  currently calls on OpenPlanner, which needs enumerating first.
- `knoxx-openplanner-publication-state-migration` (#232) stays a pure-decision
  card. Its driver belongs to the new service, so the "no runnable entry point"
  finding is not a defect in #232 to be fixed inside it.
- The receipt store's cross-run idempotence is delegated by name
  (`:append-receipt-once!`) and has no implementation. Whatever the service uses
  for it is a durability decision, not a detail of the fold.
- A dry-run mode becomes cheap once a real writer exists to stand in for, and is
  worth having before the migration is pointed at production data — the identity
  and payload conflict checks refuse rather than merge, so a real run can halt on
  data nobody has seen.

