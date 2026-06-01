---
uuid: "knoxx-lake-package-scaffold"
title: "Scaffold the knowledge-lake package structure"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Scaffold the knowledge-lake package structure

> Parent epic: `knoxx-knowledge-ops-multi-provider-epic`
> Points: 3

## Purpose

Create the `packages/knowledge-lake` package skeleton — directory layout, `package.json`, `tsconfig.json`, and entry-point barrel — so that every subsequent subtask has a stable home to land its files without merge friction.

## Scope

- Create `packages/knowledge-lake/` with sub-directories: `src/core/`, `src/domain/`, `providers/local/`, `providers/self-hosted/`, `providers/azure/`, `providers/aws/`, `tests/conformance/`, `tests/providers/`, `scripts/`
- Write `packages/knowledge-lake/package.json` with name `@open-hax/knowledge-lake`, ESM output, and peer deps placeholder
- Write `packages/knowledge-lake/tsconfig.json` extending the workspace root config
- Write `packages/knowledge-lake/src/index.ts` as a barrel re-exporting the core interfaces module (can be empty stubs initially)
- Register the package in the pnpm workspace if not already present

## Definition of done

- `pnpm --filter @open-hax/knowledge-lake build` exits 0 (even with stub sources)
- Directory tree matches the layout defined in the epic's file map
- Package appears in `pnpm ls -r` output

## Notes

Split from parent epic `knoxx-knowledge-ops-multi-provider-epic` on 2026-05-30.
