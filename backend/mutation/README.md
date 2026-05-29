# Knoxx backend mutation testing

This harness is intentionally Clojure-first: it mutates ClojureScript source forms before Shadow compiles them, rather than mutating generated JavaScript.

## Commands

```bash
cd backend
clojure -M:mutation --dry-run --limit 250
clojure -M:mutation --run --limit 100
clojure -M:mutation --run --limit 0   # full campaign, no mutant limit
clojure -M:mutation-test
```

Package aliases are also available:

```bash
pnpm run mutation:smoke       # 10-mutant quick confidence check
pnpm run mutation:dry-run     # 250 planned mutants, no test execution
pnpm run test:mutation        # 100-mutant quality gate
pnpm run mutation:campaign    # full campaign, no mutant limit
pnpm run mutation:harness-test
```

## Architecture

1. Parse CLJS source with `tools.reader` as a sanity pass and `rewrite-clj` as a position-preserving zipper.
2. Apply pure mutation operators over s-expressions, e.g. `if` test negation, comparison flips, arithmetic operator flips, and literal flips.
3. Emit one temporary source overlay per mutant under `target/mutation/mutants/<id>/src/cljs/...`.
4. Invoke Shadow with a dynamic `--config-merge` source-path overlay so the mutant shadows the original namespace.
5. Run the compiled node-test bundle and classify mutants as `:killed` or `:survived`.
6. Write an EDN report to `target/mutation/report.edn`.

Use `mutation:smoke` for a fast harness check, `test:mutation` for the default quality gate, and `mutation:campaign` when you explicitly want to pay the runtime cost of all discovered mutants. Surviving mutants are test-gap candidates, not automatic bugs.
