# Knowledge Ops — KMS Query Service

## Current Canonical Reading

- state: current retrieval-oriented query slice
- canonical implementation path: `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj`
- canonical nginx exposure path: `services/knoxx/config/conf.d/default.conf`
- note: Knoxx's primary interactive chat/runtime surface is now `/api/knoxx/*`; this spec describes the still-useful `/api/query/*` retrieval and synthesis surface

> *One Clojure query surface over many lakes.*

---

## Purpose

Define the `kms-query` surface currently implemented inside the `kms-ingestion` Clojure service. This is the first provider-independent query layer over:
- `devel-*` lakes
- `cephalon-hive`
- `sintel`

It is the first real self-management surface for the `devel` tenant.

---

## Implemented Endpoints

Currently served by:
- `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj`

Exposed through nginx at:
- `/api/query/*`

### `GET /api/query/presets`

Returns role presets.

Example response:

```json
{
  "presets": {
    "knowledge": ["devel-docs"],
    "development": ["devel-code", "devel-docs", "cephalon-hive"],
    "devsecops": ["devel-config", "devel-code", "devel-docs", "devel-data", "devel-events", "sintel"],
    "analyst": ["devel-data", "devel-events", "sintel", "devel-docs"],
    "owner": ["devel-docs", "devel-code", "devel-config", "devel-data", "devel-events", "cephalon-hive", "sintel"]
  }
}
```

### `POST /api/query/search`

Federated FTS search across one or more lake projects.

Request:

```json
{
  "q": "FutureSight",
  "role": "knowledge",
  "limit": 5,
  "tenant_id": "devel"
}
```

Response:

```json
{
  "projects": ["devel-docs"],
  "count": 1,
  "rows": [
    {
      "id": "...",
      "project": "devel-docs",
      "source": "kms-ingestion",
      "kind": "docs",
      "snippet": "..."
    }
  ]
}
```

### `POST /api/query/answer`

Federated search + synthesis through Proxx.

Behavior:
- searches selected projects
- if rows exist, formats context and sends to Proxx
- if no rows exist, returns a deterministic "no context" response instead of hallucinating

Example response:

```json
{
  "projects": ["devel-docs"],
  "count": 0,
  "rows": [],
  "answer": "No relevant context found in the selected lakes."
}
```

---

## Current Query Strategy

Currently, the service uses:
- OpenPlanner `/v1/search/fts`
- repeated once per `project`
- optional repeated once per `kind`
- merge by concatenation and truncation

This is not yet semantic federation. It is a pragmatic first cut.

---

## Role Presets

Implemented presets:

| Role | Lakes |
|------|-------|
| `knowledge` | `devel-docs` |
| `development` | `devel-code`, `devel-docs`, `cephalon-hive` |
| `devsecops` | `devel-config`, `devel-code`, `devel-docs`, `devel-data`, `devel-events`, `sintel` |
| `analyst` | `devel-data`, `devel-events`, `sintel`, `devel-docs` |
| `owner` / `cto` | all major lakes |

---

## Existing Code References

| File | Role |
|------|------|
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/api/routes.clj` | query endpoints, preset expansion, federated FTS, Proxx synthesis |
| `services/knoxx/config/conf.d/default.conf` | nginx exposure of `/api/query/*` |
| `services/openplanner/src/routes/v1/search.ts` | underlying OpenPlanner FTS endpoint |
| `services/cephalon-hive/sync/sintel-perception.mjs` | proves `sintel` is already a live project in OpenPlanner |
| `services/cephalon-hive/dashboard/server.mjs` | proves `cephalon-hive` is already a live project in OpenPlanner |

---

## Known Limitations

1. Search is currently FTS-only at the federated layer
2. Project merge is naive concatenation, not rank fusion
3. No explicit lake weighting yet
4. No UI for selecting presets/lakes yet
5. `cephalon-hive` lake needs better test queries or vector support to shine in the workbench

---

## Next Step

Add a dedicated query page or integrate the query surface into the existing frontend with:
- preset selector
- multi-lake toggle
- raw results pane
- synthesized answer pane

---

## Status

Specified 2026-04-02. Grounded in the running Clojure implementation.
