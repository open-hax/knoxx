# Knowledge Ops — Role-Scoped Lakes Spec

## Current Canonical Reading

- state: current conceptual spec, terminology refreshed toward Knoxx
- canonical ingestion path today: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`
- legacy preset labels in older notes should map to current built-in Knoxx roles as:
  - `knowledge` -> `knowledge_worker`
  - `development` -> `developer`
  - `analyst` -> `data_analyst`
  - `owner` / `cto` -> `org_admin` or `system_admin` depending scope

> *Not one corpus. A federation of lakes. Each role sees the slices it actually needs.*

---

## Purpose

Define the lake segmentation model for the canonical workspace tenant: `devel`. The system should not ingest the workspace into one undifferentiated search pile. It should ingest the same workspace into multiple parallel lakes, because different roles need different retrieval surfaces.

This is the canonical use case:
- the workspace owner is all of these roles at once
- the platform manages the workspace itself
- the product proves itself by running against a live corpus with real operational complexity

---

## The Problem

If every file goes into one corpus, retrieval quality collapses.

| User | What they want | What they do **not** want |
|------|----------------|----------------------------|
| Knowledge management | docs, specs, notes, READMEs | random source files and lockfiles |
| Development | code, APIs, tests, architecture docs | PDFs, screenshots, binary junk |
| DevSecOps | config, infra code, deployment docs, some code | narrative-only docs without operational relevance |
| Data analysts | datasets, firehoses, telemetry, event packs, sometimes docs | UI components and frontend churn |
| C-suite / owners | cross-lake synthesis | raw low-signal implementation details by default |

The answer is not permissions first. The answer is **lake design first**.

---

## Canonical Lakes

For the `devel` tenant, ingest into these canonical lakes:

| Lake | Purpose | Default audience |
|------|---------|------------------|
| `devel-docs` | markdown, notes, specs, READMEs, narrative docs | knowledge team, c-suite |
| `devel-code` | source code, tests, libraries, handlers, schemas | developers, CTO |
| `devel-config` | compose, nginx, env examples, yaml/json/toml/conf, CI | devsecops, CTO |
| `devel-data` | datasets, jsonl/csv/tsv/parquet, generated reports, event exports | analysts, devsecops |
| `devel-events` | live OpenPlanner event stream: sessions, imports, scans, receipts, social firehoses | analysts, operators |

Optional later:

| Lake | Purpose |
|------|---------|
| `devel-public` | curated public-facing subset for the widget |
| `devel-compact` | semantic packs merged across lakes |
| `devel-security` | security findings, scans, disclosures |

---

## File Classification Rules

Current implementation anchor:
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj`

The ingestion worker now classifies files into lakes by path and extension:

| Lake | Path / extension rules |
|------|------------------------|
| `docs` | `docs/`, `specs/`, `inbox/`, `.md`, `.txt`, `.org`, `.rst`, `.adoc`, `.tex` |
| `code` | `.clj`, `.cljs`, `.cljc`, `.edn`, `.ts`, `.tsx`, `.js`, `.jsx`, `.py`, `.go`, `.rs`, `.java`, `.sh`, `.sql`, etc. |
| `config` | `config/`, `configs/`, `Dockerfile`, `.env*`, `.yaml`, `.yml`, `.toml`, `.ini`, `.cfg`, `.conf`, `.json`, `.jsonc`, `.properties` |
| `data` | `data/`, `datasets/`, `.jsonl`, `.csv`, `.tsv`, `.parquet` |

Fallback classification:
- unknown text-like files fall into `docs`

---

## OpenPlanner Mapping

OpenPlanner already supports filtered search by:
- `source`
- `kind`
- `project`

We map lakes like this:

| OpenPlanner field | Value |
|-------------------|-------|
| `source` | `kms-ingestion` |
| `kind` | `docs` \| `code` \| `config` \| `data` |
| `source_ref.project` | `devel-docs` \| `devel-code` \| `devel-config` \| `devel-data` |
| `source_ref.session` | ingestion source ID |
| `source_ref.message` | stable file ID |

That gives us lake routing **without** inventing a new OpenPlanner schema first.

---

## Role Presets

The UI should expose role presets that expand into lake filters.

### 1. Knowledge Worker

```json
{
  "lakes": ["devel-docs"],
  "kinds": ["docs"],
  "defaultSearch": "hybrid"
}
```

### 2. Developer

```json
{
  "lakes": ["devel-code", "devel-docs"],
  "kinds": ["code", "docs"],
  "defaultSearch": "hybrid",
  "defaultPrimary": "devel-code"
}
```

### 3. DevSecOps Team

```json
{
  "lakes": ["devel-config", "devel-code", "devel-docs", "devel-data"],
  "kinds": ["config", "code", "docs", "data"],
  "defaultSearch": "hybrid",
  "defaultPrimary": "devel-config"
}
```

### 4. Data Analyst

```json
{
  "lakes": ["devel-data", "devel-events", "devel-docs"],
  "kinds": ["data", "events", "docs"],
  "defaultSearch": "hybrid",
  "defaultPrimary": "devel-data"
}
```

### 5. Org Admin / System Admin / Owner

```json
{
  "lakes": ["devel-docs", "devel-code", "devel-config", "devel-data", "devel-events"],
  "kinds": ["docs", "code", "config", "data", "events"],
  "defaultSearch": "hybrid",
  "mode": "cross-lake-synthesis"
}
```

---

## Query Modes

Each role may query one lake or a composition of lakes:

| Mode | Description |
|------|-------------|
| `single-lake` | query exactly one lake |
| `multi-lake-union` | query several lakes independently and merge results |
| `cross-lake-synthesis` | retrieve from several lakes, then ask the model to synthesize |

This distinction matters:
- a devsecops user often wants `config + code`
- a CTO often wants `docs + code + config`
- a knowledge worker usually wants only `docs`

---

## Real Use Story

The strongest proof is not “here is a toy corpus.”
It is:

> “This system manages my own devel workspace. I use it to navigate docs, code, configs, datasets, and live events across a corpus with hundreds of thousands of files.”

That is more credible than any synthetic demo, because the system is first useful to its operator.

---

## Existing Code References

| File | What it does |
|------|--------------|
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj` | Implements file classification into `docs` / `code` / `config` / `data` |
| `services/openplanner/src/routes/v1/search.ts` | Already supports filtered search by `source`, `kind`, `project` |
| `services/openplanner/src/lib/embedding-models.ts` | Already supports per-project and per-kind embedding model overrides |
| `orgs/open-hax/knoxx/specs/knowledge-ops-the-lake.md` | Existing lake architecture |
| `orgs/open-hax/knoxx/specs/knowledge-ops-chat-widget-layers.md` | Existing role/layer architecture |

---

## Next Step

Implement lake-aware search presets in the UI:
- Docs
- Code
- Config
- Data
- Mixed presets by role

---

## Status

Specified 2026-04-02. Derived from the devel self-management use case.
