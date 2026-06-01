---
uuid: "knoxx-role-scoped-lakes-backend-preset-resolver"
title: "Role-Scoped Lakes: Backend API endpoint to resolve role presets into lake filters"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Role-Scoped Lakes: Backend API endpoint to resolve role presets into lake filters

> Parent epic: `knoxx-knowledge-ops-role-scoped-lakes`
> Points: 3

## Purpose

Expose a backend endpoint (or extend the existing search route) that accepts a role preset name (`knowledge_worker`, `developer`, `devsecops`, `data_analyst`, `org_admin`) and returns or applies the corresponding lake filter configuration (`lakes`, `kinds`, `defaultSearch`, `mode`) so that the frontend and agent tools can consume role-aware search without embedding preset logic in the client.

## Scope

- `services/openplanner/src/routes/v1/search.ts` — add optional `role` query parameter that resolves to a preset and injects the appropriate `source`, `kind`, and `project` filters before forwarding the query
- Define the five role presets (matching the spec JSON blocks) as a typed config map — either inline in the route or in a dedicated `src/lib/role-presets.ts` module
- Legacy role label mapping (`knowledge` -> `knowledge_worker`, `analyst` -> `data_analyst`, etc.) must be handled for backward-compat
- Return the resolved preset config in the search response envelope (e.g. `meta.lake_preset`) so the UI can reflect the active filter

## Definition of done

- `GET /api/v1/search?q=foo&role=developer` returns results filtered to `devel-code` and `devel-docs` kinds
- `GET /api/v1/search?q=foo&role=org_admin` returns cross-lake results across all five lakes
- Legacy role names (`knowledge`, `analyst`) are accepted and mapped without error
- TypeScript compiles cleanly (`pnpm typecheck` in the openplanner service passes)
- A curl smoke test against the running service confirms correct `kind` filtering in returned results

## Notes

Split from parent epic `knoxx-knowledge-ops-role-scoped-lakes` on 2026-05-30.
