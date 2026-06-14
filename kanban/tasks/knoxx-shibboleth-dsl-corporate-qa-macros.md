---
uuid: "knoxx-shibboleth-dsl-corporate-qa-macros"
title: "Define Shibboleth-Lite DSL macros for corporate knowledge QA"
status: icebox
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Define Shibboleth-Lite DSL macros for corporate knowledge QA

> Parent epic: `knoxx-knowledge-ops-shibboleth-lite-labeling`
> Points: 3

## Purpose

Port the Shibboleth adversarial DSL macro vocabulary to the corporate knowledge QA domain, replacing harm-category and attack-taxonomy semantics with knowledge quality dimensions (accuracy, completeness, translation fidelity, etc.) as specified in the parent epic's label schema.

## Scope

- Define macro forms (`defdimension`, `deflabel-policy`, `defpipeline`) in the Shibboleth-Lite DSL layer covering the 8 quality dimensions from the parent epic
- Write the corresponding ClojureScript/Clojure DSL namespace (or extend existing Shibboleth macros) under `backend/` or `ingestion/`
- Include unit tests asserting macro expansion and policy evaluation for at least 3 corporate QA label categories

## Definition of done

- All 8 label dimensions from the parent epic are representable as first-class DSL constructs
- Unit tests pass (`clj -M:test` or `pnpm test`) covering macro expansion for corporate QA policies
- No adversarial/harm semantics leak into the corporate QA macro namespace

## Notes

Split from parent epic `knoxx-knowledge-ops-shibboleth-lite-labeling` on 2026-05-30.
