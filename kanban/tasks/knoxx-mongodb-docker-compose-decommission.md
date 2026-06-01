---
uuid: "knoxx-mongodb-docker-compose-decommission"
title: "Decommission DuckDB and ChromaDB from Docker Compose"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Decommission DuckDB and ChromaDB from Docker Compose

> Parent epic: `knoxx-knowledge-ops-mongodb-vector-unification`
> Points: 3

## Purpose

Remove the DuckDB and ChromaDB services from the OpenPlanner Docker Compose configuration and replace them with MongoDB 8.2 Community Edition + mongot, completing the storage consolidation to a single database instance.

## Scope

- Update `docker-compose.yml` (or equivalent): remove ChromaDB service and DuckDB volume; add `mongodb`, `mongot`, and `mongo-init` services per the spec's Docker Compose definition
- Update `openplanner` service in Compose: remove ChromaDB/DuckDB dependencies, add MongoDB dependencies and `MONGO_URL` env var
- Remove `src/plugins/duckdb.ts` and `src/plugins/chroma.ts` plugin registrations
- Update `src/lib/config.ts`: remove `CHROMA_URL` and `DUCKDB_PATH` env vars, add `MONGO_URL`
- Verify `mongodb` and `mongot` services start healthy and replica set initialises via `mongo-init`

## Definition of done

- `docker compose up` starts MongoDB, mongot, and mongo-init without errors; ChromaDB and DuckDB services are absent
- `mongosh --eval "rs.status()"` confirms replica set `rs0` is initialised with one member
- OpenPlanner service starts and connects to MongoDB without referencing ChromaDB or DuckDB env vars
- No references to `CHROMA_URL` or `DUCKDB_PATH` remain in the Compose file or `config.ts`

## Notes

Split from parent epic `knoxx-knowledge-ops-mongodb-vector-unification` on 2026-05-30.
