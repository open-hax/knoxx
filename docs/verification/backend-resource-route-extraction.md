# Resource route extraction verification

The 1,286-line resource route namespace combined route authorization, request decoding, EDN identity changes, validation diagnostics, indexing, native filesystem watchers, and serialized writes. This exceeded the repository's namespace limit and made the boundary between reversible filesystem changes and durable publication admission difficult to inspect.

The route namespace now registers routes and retains explicit public forwarding arities. Resource identity, validation, and response projections live in named domain namespaces; handlers, indexing, and serialized writes have separate infrastructure owners. Named filesystem, request/reply, and watcher extern adapters own native handles. Existing endpoints, permission checks, response fields, and compatibility contract operations are preserved. Configuration validation still calls the canonical resource contract before writes.

## Verification and obstacles

- The original scoped linter reported one namespace-size error and five warnings. The final scope passes all repository rules plus explicitly enabled optional docstring, unused-value, shadowing, underscored-binding, reflection, namespace-ordering, and refer rules: **0 errors, 0 warnings**. No rule or threshold was relaxed.
- The first extraction used `def` aliases for public functions. The unchanged overlapping-write test hung. A bounded generated-runner trace identified the exact test; inspection of generated ClojureScript showed that unknown multi-arity dispatch inside a native async caller introduced an awaited IIFE. This waited for the first admission promise before the caller could schedule and reject it. Explicit `defn` forwarding arities restore the compiler's call contract. The queue implementation and concurrent test were not weakened.
- A first focused CLI invocation used `--config-merge` with a different `:build-id`. Shadow retained the `test-ci` cache despite the distinct output path. No full-suite consumer was active and the advertised `target/test/test-ci.cjs` output was not changed. The final committed helper uses `shadow/compile*` with an independent build ID and output, following the existing policy proof helper. The abandoned diagnostic runner was terminated by exact process arguments.
- Final isolated compile: **517 inputs, 13 compiled, 0 compiler warnings**. Actual guarded Node execution: **22 tests, 76 assertions, 0 failures, 0 errors**. Coverage includes existing native filesystem save/refusal paths, exact admission scope, pre-admission rollback, preserved bytes after admission failure, and ordered overlapping writes.
- One new native filesystem regression verifies duplicate-root filtering, ignored non-EDN changes, coalesced write notifications, and cancellation of a pending refresh when the watcher closes. Temporary directories and watcher handles are released in `finally`.
- The repository JavaScript boundary check passes with **0 allow-listed non-extern generic import files**.
- An independent agent compared public names, explicit arities, queue retirement, rollback/admission boundaries, qualified identity copying, request decoding, and watcher cleanup with the original source. It found no confirmed introduced defect in that bounded review.

Run the isolated gate from `backend`:

```sh
clojure -M:cljs scripts/compile-resources-proof.clj
node target/resources-recovery/tests.cjs
```

These results describe the focused resource scope. The coordinating stack lane owns the combined advertised backend test/typecheck gates, production output lease, browser walkthrough, actual PR review loop, and merge. This report does not claim those broader gates from a focused test result.
