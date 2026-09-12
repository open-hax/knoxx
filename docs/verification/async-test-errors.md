# Async test error delivery

An asynchronous CLJS test can throw before its first assertion. Its generated
wrapper calls `done` in `finally`, and the Shadow test runner can then call
`process.exit(0)` before Node delivers the rejected promise. This produced a
real false success while investigating an OpenPlanner bootstrap regression:
eight tests and 46 executed assertions printed green while the new test threw
without contributing an assertion.

The test-only `backend/scripts/shadow-test-error-guard.cjs` preload delays an
explicit exit for two event-loop check phases. Unhandled rejections and uncaught
exceptions emit a distinctive fatal marker and force a nonzero exit. The normal
and coverage runners inherit that preload through `NODE_OPTIONS`; their result
parser also rejects the fatal marker, empty/missing summaries, or any failed
summary even when a later summary is green. The marker matters because the
Shadow compiler process can itself exit zero after its test subprocess fails.

Run the independent regression with:

```sh
node --test backend/test/js/shadow-test-error-guard.test.mjs
```

It first demonstrates the original false success without the guard, then
requires the same throw-before-first-assertion control flow to fail with the
guard. It covers rejected awaits, microtasks, immediate callbacks, nested test
processes, ordinary success, and preservation of earlier nonzero exits.

For a directly compiled test artifact, use:

```sh
node --require ./backend/scripts/shadow-test-error-guard.cjs path/to/tests.cjs
```

This bounds error delivery at shutdown; it does not wait indefinitely for
detached timers or prove every test made an assertion. Tests must still await
their work and report caught errors through `cljs.test`. Previously printed
counts establish the assertions that ran, not the absence of this failure mode.
Full CLJS suites must be rerun under the guarded harness before their old green
results are used as a complete gate. The JavaScript harness regression can run
while the sandbox source/toolchain recovery proceeds; a real compiled CLJS
throw-before-first-assertion probe and the restored full suites remain required.
