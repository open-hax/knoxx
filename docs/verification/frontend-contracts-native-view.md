# Contracts native view extraction

`ContractsPage.tsx` was a 1,198-line combination of controller logic, EDN
helpers, inline widgets and the complete workbench view. Existing frontend
owners did not have capacity for a cohesive extraction below the unchanged
400-line warning threshold. The migration law also forbids new TypeScript paths.

The page is now a 371-line existing React controller, mounted with a native
ClojureScript view through the established app bridge. The contract-library
projection and its exact wire types moved to the existing contracts API owner
(338 lines). Native library, editor/librarian, presentation and page composition
are separate bounded namespaces; the React boundary decodes plain state and
callbacks while keeping chat and component handles opaque. No new TS/TSX path,
dependency, warning allowance or size threshold was introduced.

## What was retired and what remains

The previous metadata sidebar had a literal, unconditional `display: "none"`.
Its private `SearchableSelect`, `TagInput`, nested-agent and keyword-vector
helpers had no other callers. That unreachable subtree was removed. The visible
identity, kind, version and enabled fields remain, as do the raw CodeMirror
editor, library/class selection, search, validation, save, clone, normalized
JSON, notices, responsive overlay and librarian controls.

The controller preserves the existing transport configuration/authentication,
request bodies, selected class, save/copy sequencing, chat persistence keys and
source-opening callbacks. Its EDN field editing still uses the existing regular
expressions; this change does not claim a general structured EDN editor. The
chat controller and CodeMirror widget remain the existing legacy components.
The app bridge's internal page mount now receives its native renderer explicitly;
the router was updated with that composition.

## Obstacles and verification

1. There was no appropriate existing TSX owner large enough for the view. Moving
   the view to bounded native namespaces followed the existing migration rather
   than creating more legacy source paths.
2. Native view tests must not load the complete Vite application bridge. The
   page composition supplies opaque editor/chat components, letting interaction
   tests exercise native controls and the real prop codecs independently. The
   production proof separately builds and consumes both real bridges.
3. The warning-as-error compiler identified an untyped raw controller handle.
   The named React boundary now annotates native handles and uses a pure string
   join when constructing diagnostics. No compiler warning was suppressed.
4. The initial verification helper had a delimiter error. A later helper edit
   during `load-file` caused a trailing reader error after the application had
   compiled. Both helpers were corrected, linted, frozen and rerun to completion;
   the earlier successful compile alone was not counted as a successful command.

Fresh results:

- Existing pre-change Contracts suite: **3 passing tests**.
- Updated controller suite: **4 passing tests**, retaining save/validate,
  selected-class and clone coverage and adding failed-save draft preservation.
- Combined legacy suite, run by the frontend owner against the coherent source:
  **254 passing tests**; isolated legacy emission and advertised `pnpm typecheck`
  both completed with exit 0.
- Native interaction proof: **5 tests, 12 assertions, 0 failures/errors**,
  **76 compiler files, 0 warnings**. It checks disabled commands, exact class
  selection/search, all visible identity fields, editor diagnostics, clone input,
  narrow overlay and native chat source callbacks.
- Actual isolated Vite bridges: app **2,197 modules**, frontend **373 modules**.
  The isolated advanced application release completed with **181 files,
  0 warnings**, consuming those bridges.
- Scoped source/test/helper kondo: **0 errors, 0 warnings**. All seven extracted
  source owners pass the unchanged size gate; `git diff --check` is clean.

From `frontend`, the reproducible proof commands are:

```sh
pnpm exec vite build --config vite.app-bridge.config.ts --outDir dist-verification/contracts-bridge
pnpm exec vite build --config vite.bridge.config.ts --outDir dist-verification/contracts-bridge
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-contracts-proof.clj")'
pnpm exec shadow-cljs clj-eval '(load-file "scripts/release-contracts-proof.clj")'
```

These use distinct Shadow build IDs and outputs under `dist-verification`.
The running browser's `frontend/dist` and `backend/dist` were not modified.
Command evidence is recorded in the sandbox runtime under
`evidence/frontend-contracts-*`; the final native and release logs end in
`native-final.log` and `release-final.log`.

## Self-review and browser follow-up

The author compared each retained control and callback with the original JSX.
An independent bounded review found no confirmed introduced defect in controller
sequencing, class/identity selection, diagnostics, source callbacks or opaque
handle ownership. It did not claim pixel-by-pixel equivalence.

The actual rebuilt browser tour remains a separate required step. It must verify
authenticated API responses, class selection, EDN validation/save/clone, the
real failure notice, and narrow overlay/chat controls. This checkpoint records
source and automated build/test evidence, not screenshots from the new view.
