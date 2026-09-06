# Frontend migration manifest

`manifest.ndedn` is generated source-of-truth for the TypeScript-to-CLJS
strangler migration. Each line is one canonical EDN record for a governed
`.ts`/`.tsx`/`.mts`/`.cts` file, bridge export, Shadow route, or legacy Vitest suite.
The `:ts` count includes `.ts`, `.mts`, and `.cts` files.
JavaScript variants (`.js`, `.jsx`, `.mjs`, and `.cjs`) under `frontend/src`
are rejected so a language-only rename cannot count as CLJS migration.
Local imports and file references from governed TypeScript must remain within
`frontend/src` so relocating a dependency cannot remove it from the inventory.
This also applies to local file, link, and workspace packages, even when the
TypeScript resolver marks them as external libraries.
This includes TypeScript path mappings and static Vite aliases. Vite bridge
entries must match the governed bridge files; dynamic configurations that
cannot be inspected without execution are rejected.
Vite plugins are limited to the existing zero-argument `@vitejs/plugin-react`
factory; additional Vite or Rollup hooks require explicit inventory support.
Rollup output, worker, and esbuild settings must use inspected static forms;
output text hooks and injected source are rejected.
Active Vite build commands must select an existing governed bridge config;
retiring a bridge also requires removing its build commands.
An active Shadow bridge resolution requires its corresponding governed build
script and config.
The production build may contain governed Vite phases, then Shadow release and
the optional CSS phase. Every active bridge must be compiled before Shadow.
The pipeline remains checked after bridge resolutions are retired.
Opaque build wrappers and decoy commands are rejected.
Vite glob imports are not supported by the inventory and fail explicitly;
comments and string literals that merely mention glob syntax are ignored.
Worker and SharedWorker URL dependencies must also stay in `frontend/src`;
dynamic worker URLs that cannot be inspected are rejected.
Dynamic `import()` calls must use literal module paths; variable imports are
rejected because their dependency set cannot be read from a literal specifier.
Vitest source scopes must stay within `frontend/src`; active test commands
must select the inspected static Vitest configuration.
The `test`, `test:coverage`, and `test:watch` entrypoints cannot hide the runner
behind wrappers; retiring Vitest removes these entrypoints and its config together.

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
Within canonical Route elements, any live application-bridge reference keeps
ownership legacy, including mixed React constructors inside native wrappers.
The canonical `(def Route (.-Route router-alias))` binding is supported;
copying Route values into other definitions or bindings is rejected.
Computed router API access is rejected when it could hide a route constructor.
Route-object APIs (`useRoutes` and the browser/hash/memory router factories)
are detected and rejected outside this grammar.

Manifest writes validate record schemas, unique IDs, and canonical sorted text
before creating directories or replacing the ledger.

An infrastructure-only pull request may retain the legacy count by placing
the exact declaration `Migration infrastructure: yes` in its body. The
declaration is not an escape hatch for new TypeScript, bridge growth, or route
regression.

Malli schemas and monotonicity laws live in
`knoxx.frontend.law.migration`, along with source-containment and runner/build
admission contracts; deterministic source classification lives in
`knoxx.frontend.domain.migration`. The CLI prints summaries derived from the
line records so no second inventory needs manual synchronization.
