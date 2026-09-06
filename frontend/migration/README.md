# Frontend migration manifest

`manifest.ndedn` is generated source-of-truth for the TypeScript-to-CLJS
strangler migration. Each line is one canonical EDN record for a governed
`.ts`/`.tsx`/`.mts`/`.cts` file, bridge export, Shadow route, or legacy Vitest suite.
The `:ts` count includes `.ts`, `.mts`, and `.cts` files.
JavaScript variants (`.js`, `.jsx`, `.mjs`, and `.cjs`) under `frontend/src`
are rejected so a language-only rename cannot count as CLJS migration.
Local imports and file references from governed TypeScript must remain within
`frontend/src` so relocating a dependency cannot remove it from the inventory.
This includes TypeScript path mappings and static Vite aliases. Vite bridge
entries must match the governed bridge files; dynamic configurations that
cannot be inspected without execution are rejected.
Active Vite build commands must select an existing governed bridge config;
retiring a bridge also requires removing its build commands.
Vite glob imports are not supported by the inventory and fail explicitly;
comments and string literals that merely mention glob syntax are ignored.
Worker and SharedWorker URL dependencies must also stay in `frontend/src`;
dynamic worker URLs that cannot be inspected are rejected.
Dynamic `import()` calls must use literal module paths; variable imports are
rejected because their dependency set cannot be read from a literal specifier.

Run from `frontend/`:

```bash
pnpm migration:write
pnpm migration:check
```

The check regenerates the ledger and fails on drift. In pull requests it also
compares the exact base revision and rejects:

- new governed TypeScript paths or increased `:ts`/`:tsx` counts;
- new frontend/application bridge exports;
- a native route returning to an application-bridge implementation;
- new legacy route identities, including expression-only legacy route renames;
- a migration-surface change that does not reduce the legacy surface.

Route IDs use canonical source expressions. A new legacy ID is rejected to
prevent an expression change from hiding a rollback. Native route IDs may be
added or renamed.

Route declarations currently belong in `frontend/src/cljs/knoxx/frontend/app.cljs`;
the inventory rejects route declarations extracted into other source files.
The conservative census treats Route-named components and literal
`:path`, `:element`, `:index`, or `:Component` props as route candidates.
Explicit comment and quote bodies cannot contribute routes or implementation ownership.
Direct React `createElement` route construction is detected and rejected as
unsupported syntax rather than silently dropping the route.
The canonical `(def Route (.-Route router-alias))` binding is supported;
copying Route values into other definitions or bindings is rejected.

An infrastructure-only pull request may retain the legacy count by placing
the exact declaration `Migration infrastructure: yes` in its body. The
declaration is not an escape hatch for new TypeScript, bridge growth, or route
regression.

Malli schemas and monotonicity laws live in
`knoxx.frontend.law.migration`; deterministic source classification lives in
`knoxx.frontend.domain.migration`. The CLI prints summaries derived from the
line records so no second inventory needs manual synchronization.
