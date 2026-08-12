---
uuid: "knoxx-contract-owned-publication-pipeline"
title: "Contract-owned document publication — remove OpenPlanner as publication authority"
status: incoming
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

The target invariant is:

```text
resource graph                = desired semantic state
runtime receipts/projections  = observed execution state
drift                         = desired - observed
```

A Knoxx process with no OpenPlanner REST service available must still be able to answer:

- what documents exist;
- where their canonical source lives;
- which gardens they target;
- which locale/revision is intended for each target;
- whether publication is requested, withheld, or archived;
- which translation/review policy gates each publication;
- what remains blocked before the requested publication can materialize.

It must **not** infer any of those facts from `garden_publications` metadata or from `/api/openplanner/...` reads.

## Why this is the last decoupling seam

Translation review already uses Knoxx-owned `/api/translations/...` routes and translation persistence has moved behind Knoxx boundaries. The remaining coupling is publication semantics and a small amount of translation pipeline configuration:

- CMS discovers gardens through `/api/openplanner/v1/gardens`;
- CMS reads `metadata.garden_publications` as publication truth;
- translation pipeline config still reads/writes `/api/openplanner/v1/translations/config`;
- the deploy gate still has a conditional OpenPlanner REST branch for CMS.

The existing `knoxx-cms-contract-validation` card already identifies CMS as the last REST-only OpenPlanner dependency in the deployed stack. This epic is the concrete architectural answer to that card.

## Ownership rule

```text
Knoxx resources      own desired state
Knoxx law/domain     own admissibility and reconciliation decisions
Knoxx receipts       own observed execution facts
publication adapters perform effects
OpenPlanner          owns only its own adapter/projection implementation
```

Publication is a relation, not a boolean on a document:

```text
document × garden × locale × revision -> publication intent
```

That permits one document to be public in English, awaiting review in Spanish, absent from another garden, and archived in a fourth without inventing contradictory document-level state.

## Explicit non-goals

- Do not replace OpenPlanner authority with a new mutable "Knoxx garden database" authority.
- Do not store runtime facts such as `published_at`, worker run ids, translation job ids, or last deploy status in declarative resource data.
- Do not encode workflow observations such as `:translating` or `:reviewing` as desired publication state.
- Do not duplicate `knoxx-translations-event-sourced`; translation history remains that card's concern.
- Do not require the publication adapter to be OpenPlanner-specific.

## Children — all incoming

1. `knoxx-publication-resource-contracts` — first-class document, garden, and publication resource laws.
2. `knoxx-publication-intent-resolver` — pure resource graph -> desired publication projection.
3. `knoxx-cms-resource-backed-publication-ui` — make CMS read/write resource intent instead of OpenPlanner publication metadata.
4. `knoxx-translation-pipeline-config-resource` — remove the remaining OpenPlanner translation config authority.
5. `knoxx-translation-publication-gate` — compute publication blockers from translation/review policy + receipts.
6. `knoxx-publication-adapter-boundary` — define effect boundary and reconciliation plan; OpenPlanner becomes optional adapter.
7. `knoxx-openplanner-publication-state-migration` — import existing gardens/publications into resources once, with conflict receipts.
8. `knoxx-openplanner-rest-retirement` — delete the CMS/translation REST compatibility dependency and deploy flag.
9. `knoxx-contract-publication-e2e` — prove the full publish/translate/review/materialize path with OpenPlanner REST absent.

## Build order

```text
resource contracts
      -> intent resolver
      -> CMS + translation config cutover
      -> translation/review gate
      -> publication adapter boundary
      -> migration
      -> OpenPlanner REST retirement
      -> no-OpenPlanner E2E gate
```

`knoxx-cms-contract-validation` should validate the surviving CMS contract surface against this model rather than deciding the dependency direction again.

## Done when

- Reading Knoxx resources alone is sufficient to reconstruct desired document/garden/locale/revision publication topology.
- CMS publication actions mutate resource intent, not OpenPlanner metadata.
- Translation/review state blocks or admits publication without becoming contract state itself.
- OpenPlanner is optional at the semantic layer and can be replaced by another publication adapter without changing document/publication contracts.
- Production deploy verification no longer conditionally skips CMS because OpenPlanner REST is absent.
- An end-to-end test publishes a translated, reviewed document with the OpenPlanner REST service deliberately unavailable.
