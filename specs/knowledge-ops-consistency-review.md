# Knowledge-ops spec consistency review

Date: 2026-04-04

## Scope

Reviewed the 34 `knowledge-ops-*.md` specs moved from:

- `specs/drafts/`

into:

- `orgs/open-hax/knoxx/specs/`

Also normalized the obvious internal cross-references that still pointed at `specs/drafts/knowledge-ops-*.md`.

## What was normalized immediately

### 1. Canonical location
All `knowledge-ops-*.md` specs now live under:

- `orgs/open-hax/knoxx/specs/`

### 2. Internal cross-spec links
The moved specs no longer point at `specs/drafts/knowledge-ops-*.md` for the few direct references found during the scan.

## Main consistency findings

The set is valuable, but it is not yet internally uniform. It contains multiple generations of architecture thinking.

### A. Product naming is inconsistent
The spec set mixes:

- `knowledge-ops`
- `The Lake`
- `futuresight-kms`
- `ragussy`
- `OpenPlanner`
- `Knoxx`

#### Recommended canonical interpretation
For this directory, interpret the current product name as:

- **Knoxx** = product/workbench/runtime

And interpret older names as historical layers:

- `futuresight-kms` = earlier service/package framing
- `ragussy` = legacy backend/frontend reference layer
- `The Lake` = architectural lineage around OpenPlanner/data-lake design
- `knowledge-ops` = umbrella concept, not the canonical runtime product name

### B. Source-code paths are inconsistent with the current repo layout
A scan found many references to legacy paths, especially:

- `services/futuresight-kms`
- `packages/futuresight-kms`
- `orgs/mojomast/ragussy`

Meanwhile current live Knoxx work is centered in:

- `orgs/open-hax/knoxx/backend`
- `orgs/open-hax/knoxx/frontend`
- `orgs/open-hax/knoxx/ingestion`
- `services/knoxx`

#### Recommended canonical interpretation
When a spec references `futuresight-kms` or `ragussy`, treat it as:

- **historical donor / legacy implementation context**

When specifying current implementation work, prefer:

- `orgs/open-hax/knoxx/*`
- `services/knoxx/*`

### C. Search/storage architecture is not fully converged
The moved specs currently contain competing or sequentially evolved backends:

- Ragussy + Qdrant
- OpenPlanner + DuckDB + ChromaDB
- OpenPlanner + MongoDB vector search
- provider abstraction with local/AWS/Azure/self-hosted modes

These are not all wrong; they represent different moments or options. But they are not one single settled architecture.

#### Recommended canonical interpretation
For Knoxx-facing implementation work today, treat the current canonical path as:

- **Knoxx CLJS backend + OpenPlanner memory/search + Proxx inference + Postgres policy/control-plane**

And treat the following as exploratory or legacy branches unless explicitly revived:

- direct Ragussy/Qdrant centrality
- DuckDB/ChromaDB as the sole settled future
- MongoDB vector unification as a design option, not current reality

### D. UI vocabulary is inconsistent
The spec set still mixes older UI terms like:

- `Canvas`
- `Chat Lab`
- `QueryPage`
- `DocumentsPage`

with the newer Knoxx workbench framing:

- `Context Bar`
- `Agent Runtime`
- `Scratchpad`
- `Recent Sessions`
- `Live Intervention`

#### Recommended canonical interpretation
For Knoxx UI work, prefer the current visible workbench vocabulary:

- left rail = **Context Bar**
- center = **Agent Runtime / conversation workbench**
- right rail = **Scratchpad**

Interpret `Canvas` as an older name for what is now mostly the **Scratchpad** surface.

### E. Role model is behind the current RBAC work
Older specs talk about role/layer concepts without the new product control-plane roles.

Current built-ins are now:

- `system_admin`
- `org_admin`
- `knowledge_worker`
- `data_analyst`
- `developer`

#### Recommended canonical interpretation
For all future Knoxx admin/multi-tenant work, these are the canonical seeded built-ins.
Older role labels or looser persona language should not override them.

### F. Some specs are donor/audit docs rather than active implementation contracts
A few moved files function more like:

- prior-art research
- migration archaeology
- donor inventory
- architecture alternatives

rather than “this is the one true current implementation contract.”

Notably in that bucket:

- `knowledge-ops-gap-analysis-prior-art.md`
- `knowledge-ops-legacy-ui-inventory.md`
- `knowledge-ops-promethean-stack.md`
- `knowledge-ops-mongodb-vector-unification.md`
- `knowledge-ops-multi-provider-epic.md`

These should remain in the set, but be read as supporting strategy material rather than immediate source-of-truth specs.

## High-signal counts from the review scan

Approximate legacy/current signal found in the moved spec corpus:

- references to `services/futuresight-kms`: **38**
- references to `orgs/mojomast/ragussy`: **59**
- references to `Qdrant` / `qdrant`: **75** combined
- references to `ChromaDB`: **49**
- references to `DuckDB`: **39**
- references to `orgs/open-hax/knoxx`: **21**
- references to `services/knoxx`: **0** in the moved files before review output

Interpretation:

- the corpus is still strongly anchored in pre-Knoxx naming and earlier stack layouts
- the move is correct, but full semantic convergence still requires editorial cleanup

## Recommended canonical reading order

If someone wants the most Knoxx-relevant reading order now, start with:

1. `knowledge-ops-architecture-migration.md`
2. `knowledge-ops-clojure-backend-migration.md`
3. `knowledge-ops-role-scoped-lakes.md`
4. `knowledge-ops-kms-query.md`
5. `knowledge-ops-federated-lakes.md`
6. `knowledge-ops-cms-data-model.md`
7. `knowledge-ops-multi-tenant-control-plane.md`
8. `knowledge-ops-workbench-ui.md`
9. `knowledge-ops-ui-design-system.md`
10. `knowledge-ops-pii-handling-protocol.md`

Then consult the rest as donor/alternative material.

## Recommended next cleanup passes

### Pass 1: path canonicalization
Update current-implementation references from legacy paths to live Knoxx paths where the spec is clearly describing the present system rather than history.

Priority files:

- `knowledge-ops-architecture-migration.md`
- `knowledge-ops-clojure-backend-migration.md`
- `knowledge-ops-kms-query.md`
- `knowledge-ops-role-scoped-lakes.md`
- `knowledge-ops-workbench-ui.md`
- `knowledge-ops-deployment.md`

### Pass 2: architecture status tagging
Add a short status block near the top of each spec, e.g.:

- `status: current`
- `status: legacy-donor`
- `status: exploratory`
- `status: superseded-in-part`

This would dramatically reduce confusion.

### Pass 3: storage/runtime convergence
Explicitly mark which of these are:

- current Knoxx reality
- supported future option
- rejected / superseded path

for:

- Qdrant
- ChromaDB
- DuckDB
- MongoDB vector search
- OpenPlanner document storage
- Proxx vs local model serving

### Pass 4: UI vocabulary normalization
Normalize old `Canvas` / `Chat Lab` language toward the current Knoxx workbench terms where appropriate.

### Pass 5: RBAC / tenancy refresh
Update older specs to reflect the now-seeded control-plane roles and the Postgres-backed org/user/role/data-lake model.

## Bottom line

The move is done.

The corpus is now in the right home:

- `orgs/open-hax/knoxx/specs/`

The review conclusion is:

- the spec set is **strategically coherent**
- but **editorially multi-era**
- so it should be treated as a layered corpus with a current Knoxx interpretation, not as a perfectly uniform single-version manual

The biggest remaining consistency issue is not the location anymore.
It is the gap between:

- older `futuresight-kms` / `ragussy` / `Qdrant` assumptions
- and the current Knoxx + OpenPlanner + Proxx + Postgres control-plane reality
