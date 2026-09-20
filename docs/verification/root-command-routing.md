# Root command routing recovery

On 2026-09-12, running Knoxx from Foresight's checkout exposed a false success:
`pnpm --filter @open-hax/knoxx-frontend ...` printed "No projects matched" and
exited zero. Knoxx has no workspace declaration, and the enclosing Foresight
workspace selects different packages. Consequently, the old root `build` and
`dev` commands could execute nothing.

The root manifest now delegates through explicit child directories. Build and
typecheck cover both application packages. Test includes backend Node/CLJS,
frontend CLJS and frontend Vitest. Lint runs backend `lint`, frontend `lint`,
and the existing repository file-size gate. The frontend manifest defines
`lint` as `clj-kondo --lint src/cljs src/dev src/e2e test/cljs ../shared/src/cljs --fail-level warning`;
the backend command also rejects warnings. Development retains its
frontend-server responsibility; the existing local startup documentation owns
backend and service startup.

`backend/test/js/root-command-routing.test.mjs` executes actual pnpm processes
with the real root scripts inside a temporary enclosing workspace that excludes
Knoxx. The child fixtures expose only script names from the checked-in backend
and frontend manifests, so a missing child script cannot be fabricated by the
test. Child commands record their stage and working directory. The regression
checks order, nonzero failure propagation, stopping later stages, the root
file-size gate and a missing frontend. It requires pnpm on PATH and downloads
no dependencies. These routing probes do not claim application verification.

Historical validation in the recovered sandbox:

- The original manifest failed all eleven initial routing regressions, including
  the successful no-project build. The corrected manifest passes twelve tests,
  including the added file-size failure regression, with no skips.
- Node syntax checking and Git diff whitespace checking passed.
- Actual full backend lint failed with **8 errors and 257 warnings**. The root
  lint command now propagates that failure; it cannot report success while
  warnings remain. Frontend lint and file-size debt remain visible downstream
  gates and must be run again after backend cleanup.
- Full application builds, typechecks and tests were not rerun for this routing
  checkpoint: the browser-verification lane owns the compiled output snapshot.
  Their full results must be recorded separately before merge.

No generated Foresight manifests or gate classifications were changed. Once
this Knoxx commit is promoted through its root gitlink, the orchestration layer
can discover the new child-owned commands without knowing package layout.

## Split-layer review verification, 2026-09-19

The manifest-backed routing fixture reproduced the missing frontend `lint`
command: 10 routing tests passed and 2 failed before the command was added.
The added frontend script is identical to the full lint command in cumulative
source `1d3b207ab2e5bfc6b2fa149836b855d458a9c997`; no later frontend implementation
was imported. The routing, defroute-hook, async-error-guard and Shadow-runner
Node suites then passed **29 tests, zero failures, zero skips**.
The updated routing fixture also passed all **12 routing tests** in a separate
checkout of the cumulative source, without changing its frontend manifest.

The three deployment/sandbox workflow additions pin `clj-kondo` to
`2025.07.28`, the installed release used for this validation. YAML parsing,
matching version checks, Node syntax and diff whitespace checks passed.

Actual `pnpm -C frontend run lint` now executes, but this early layer still
fails with **6 errors and 842 warnings** in unchanged frontend sources. The
errors are function-length findings in `admin/event_agent_editor.cljs`,
`admin/event_agents.cljs`, `admin/event_agents_panel.cljs`, `app.cljs`,
`pages/agents.cljs`, and `pages/source_doc/view.cljs`, under
`frontend/src/cljs/knoxx/frontend/`. This is visible inherited lint debt, not a
passing frontend gate. Full application tests/builds were not run for these
workflow, manifest, routing-test and documentation edits; the isolated layer
still requires later stack namespaces and separate main integration.
