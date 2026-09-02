# Frontend migration manifest

`manifest.ndedn` is generated source-of-truth for the TypeScript-to-CLJS
strangler migration. Each line is one canonical EDN record for a governed
`.ts`/`.tsx` file, bridge export, Shadow route, or legacy Vitest suite.

Run from `frontend/`:

```bash
pnpm migration:write
pnpm migration:check
```

The check regenerates the ledger and fails on drift. In pull requests it also
compares the exact base revision and rejects:

- new governed TypeScript paths or increased `.ts`/`.tsx` counts;
- new frontend/application bridge exports;
- a native route returning to an application-bridge implementation;
- a migration-surface change that does not reduce the legacy surface.

An infrastructure-only pull request may retain the legacy count by placing
the exact declaration `Migration infrastructure: yes` in its body. The
declaration is not an escape hatch for new TypeScript, bridge growth, or route
regression.

Malli schemas and monotonicity laws live in
`knoxx.frontend.law.migration`; deterministic source classification lives in
`knoxx.frontend.shape.migration`. The CLI prints summaries derived from the
line records so no second inventory needs manual synchronization.
