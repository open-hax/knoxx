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

The frontend `test:cljs` package command also delegates to this guarded runner.
This protects the root aggregate test command and frontend CI without requiring
the caller to remember a `NODE_OPTIONS` preload. Direct `shadow-cljs compile
test` remains an unguarded diagnostic command and is not the package test gate.

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

## Frontend package-command qualification

[PR #327's frontend review finding](https://github.com/open-hax/knoxx/pull/327#discussion_r4056210134)
identified that its package command still bypassed the guard. A permanent
manifest-driven process regression now executes that actual command against a
Shadow launcher that returns zero after a child fails. It checks both ordinary
success and the rejected promise hidden behind green counters. Before the
manifest change, the guard suite passed 10 tests and failed this new test;
afterward, the guard, runner and root-routing suites passed all 27 tests.

A temporary `knoxx.frontend.zzz-async-error-probe-test` namespace also exercised
the real frontend compiler with one positive assertion and a native async test
that immediately threw `frontend-compiled-before-first-is`. With `NODE_OPTIONS`
unset, `pnpm -C frontend run test:cljs` printed **458 tests / 2,046 assertions,
zero failures/errors** and exited **0** under the original raw Shadow command.
The same command after the manifest repair printed those same counters but
reported the guard's fatal unhandled-rejection marker and exited **1**. Both
compilations reported zero warnings. This demonstrates why green counters alone
did not establish that every async test completed successfully.

The failing probe was removed before the clean package suite was rerun with
`NODE_OPTIONS` unset: **456 tests / 2,045 assertions**, zero failures/errors,
zero compiler warnings, and exit **0**. No
application source, compiler target, test-discovery filter or failure threshold
was changed by this repair.
