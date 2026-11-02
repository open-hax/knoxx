# Translation dispatch test decomposition

The 1,072-line translation dispatch test namespace exceeded the existing file
size contract. Its 40 tests now live in four cohesive namespaces: initial
dispatch, completion, ambiguous-send observation and source/batch recovery.
A shared fixture namespace provides a fresh evidence store and finite provider
for every test.

All 40 original test bodies and assertions were token-compared after the move.
Only namespace qualification and the names of two promise resolver bindings
changed. No test was dropped or weakened. The first native run after the move
passed **40 tests / 182 assertions** with the fatal guard and actual exit zero.

Two pre-existing oversized fixture functions were then shortened. The batch
provider now uses the existing complete OpenPlanner callback fixture, seeding
only the four intended methods; every unseeded method still throws explicitly.
The completion store obtains its one-shot failure closure from a named helper,
preserving a fresh atom and exactly-once failure for each wrapper instance.

After those changes, the isolated `:translation-dispatch-proof` compiler covered
**88 inputs, seven compiled, zero warnings**. The guarded native execution again
passed **40 tests / 182 assertions, zero failures/errors, actual exit zero**.
All five source/test fixture paths pass scoped clj-kondo with all seven optional
rules from `AGENTS.md` explicitly enabled: **zero errors/warnings**.

This final focused proof used Node 24.20, CLJS 1.12.145 and local dependency
overrides to the frozen eta identity initialization worktree at
`fc3b6a09c6cd90ca200023cbc1fc54ec57a630a0`. It does not stand in for a subsequent
whole backend run or production build.

```sh
cd backend
clojure -M:cljs scripts/compile-translation-dispatch-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts \
  node --require ./scripts/shadow-test-error-guard.cjs \
  target/translation-dispatch-proof/tests.cjs
```

During coordinated sandbox recovery, the command above also needs the same
explicit frozen eta overrides until the advertised dependency pins are promoted.
