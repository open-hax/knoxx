# Root command routing recovery

On 2026-09-12, running Knoxx from Foresight's checkout exposed a false success:
`pnpm --filter @open-hax/knoxx-frontend ...` printed "No projects matched" and
exited zero. Knoxx has no workspace declaration, and the enclosing Foresight
workspace selects different packages. Consequently, the old root `build` and
`dev` commands could execute nothing.

The root manifest now delegates through explicit child directories. Build and
typecheck cover both application packages. Test includes backend Node/CLJS,
frontend CLJS and frontend Vitest. Lint runs both complete CLJS lint commands
and the existing repository file-size gate. Backend lint now rejects warnings,
matching the frontend's strict full lint command. Development retains its
frontend-server responsibility; the existing local startup documentation owns
backend and service startup.

`backend/test/js/root-command-routing.test.mjs` executes actual pnpm processes
with the real root scripts inside a temporary enclosing workspace that excludes
Knoxx. Child commands record their stage and working directory. The regression
checks order, nonzero failure propagation, stopping later stages, the root
file-size gate and a missing frontend. It requires pnpm on PATH and downloads
no dependencies. These routing probes do not claim application verification.

Validation in the recovered sandbox:

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
