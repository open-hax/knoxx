# Knowledge Ops — Federated Lakes Spec

> *`devel` is not the only lake. The system must speak to the lakes already alive in the ecosystem.*

---

## Purpose

Define the federated lake model for the platform. The system should not only query the new `devel-*` lakes created for workspace self-management; it should also be able to query the existing OpenPlanner-backed lakes already in use by the wider Promethean ecosystem.

This makes the platform real:
- it is not a greenfield toy
- it sits on top of live operational data
- it proves the platform can unify multiple working systems

---

## Canonical External Lakes

### 1. Devel Lakes

These are the new role-scoped artifact lakes for the workspace itself.

| Project key | Meaning |
|-------------|---------|
| `devel-docs` | markdown, specs, notes, READMEs |
| `devel-code` | source code and tests |
| `devel-config` | configs, compose, env, infra |
| `devel-data` | datasets, reports, jsonl/csv/parquet |
| `devel-events` | live operational/event stream for the workspace |

### 2. Cephalon Lakes

Already active through OpenPlanner integrations.

| Project key | Source | Meaning |
|-------------|--------|---------|
| `cephalon-hive` | `cephalon:<id>` and `cephalon-ts` | cephalon memory events, hive activity, agent cognition |

Observed code references:
- `services/cephalon-hive/dashboard/server.mjs`
- `orgs/octave-commons/cephalon/packages/cephalon-ts/src/openplanner/client.ts`

### 3. Sintel Lake

Already active through the Bluesky perception service.

| Project key | Source | Meaning |
|-------------|--------|---------|
| `sintel` | `bluesky-firehose` | live social/firehose events and perception signals |

Observed code reference:
- `services/cephalon-hive/sync/sintel-perception.mjs`

---

## Unified Query Model

The query layer must support all of these at once.

### Query envelope

```json
{
  "q": "what changed in deployment strategy?",
  "projects": ["devel-docs", "devel-config", "cephalon-hive"],
  "kinds": ["docs", "config", "memory.created"],
  "mode": "cross-lake-synthesis",
  "limit": 20
}
```

### Modes

| Mode | Behavior |
|------|----------|
| `single-lake` | Search exactly one project/lake |
| `multi-lake-union` | Search multiple projects and merge hits |
| `cross-lake-synthesis` | Retrieve from multiple projects, then synthesize through Proxx |

---

## Role Preset Expansion

### Knowledge team
- `devel-docs`

### Development team
- `devel-code`
- `devel-docs`
- optionally `cephalon-hive` for implementation memory

### DevSecOps team
- `devel-config`
- `devel-code`
- `devel-docs`
- `devel-events`
- optionally `sintel` for threat/perception context

### Data analysts
- `devel-data`
- `devel-events`
- `sintel`
- optionally `cephalon-hive`

### CTO / owner
- all `devel-*`
- `cephalon-hive`
- `sintel`

---

## Why Federated Instead of Flattened

Flattening everything into one index destroys intent.

Federation preserves:
- provenance
- role relevance
- selective querying
- future access control
- lake-level embedding policy

It also lets you mix:
- docs from `devel-docs`
- live cognition from `cephalon-hive`
- external social signals from `sintel`

That is stronger than “here is one folder indexed with AI,” because it reflects a system already useful across multiple real operating contexts.

---

## Existing Code References

| File | What it shows |
|------|---------------|
| `services/cephalon-hive/dashboard/server.mjs` | Writes cephalon memory events into OpenPlanner with `project: "cephalon-hive"` |
| `orgs/octave-commons/cephalon/packages/cephalon-ts/src/openplanner/client.ts` | Canonical cephalon OpenPlanner client and event envelope |
| `services/cephalon-hive/sync/sintel-perception.mjs` | Writes Bluesky perception events into OpenPlanner with `project: "sintel"` |
| `services/openplanner/src/routes/v1/search.ts` | Search filtering by `project`, `source`, `kind` already exists |
| `orgs/open-hax/knoxx/specs/knowledge-ops-role-scoped-lakes.md` | Defines `devel-*` lakes |

---

## Immediate Next Step

Implement a lake-aware query endpoint that accepts:
- one or more `projects`
- one or more `kinds`
- a `mode`

and returns either merged hits or a synthesized answer.

---

## Status

Specified 2026-04-02. Grounded in existing OpenPlanner integrations for `devel`, `cephalon-hive`, and `sintel`.
