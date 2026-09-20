# Durable event-turn admission and session recovery

The real translation workflow reached the queue before its first durable run
existed. `event_turn_started` then entered the ordered event writer; Clio refused
it with `run_store_not_found`. The later initial run write first flushed that
failed event and never reached its own admission. Codex independently reported
the same defect in review comment `3996346040` on PR305.

Admission now reserves the process-local FIFO position synchronously, then awaits
the base run and its initial queue event before acknowledging the request. A
promoted pending entry waits for its own admission promise before emitting a
started event or invoking the model. Failed admission removes only its exact
active or pending reservation. Queue overflow also admits a durable failed run
before recording rejection and failure events. Verified organization and
membership coordinates survive the initial run snapshot.

`enqueue-event-turn!` is deliberately asynchronous: callers must await its result.
The production dispatcher already did so. Older tests were changed to await this
contract, use real isolated Clio providers, and wait for observable settlement
instead of assuming that two Promise ticks completed filesystem operations.

| Owner | Responsibility |
| --- | --- |
| `infra.agent.runner` | Public facade, original queue/settler atoms, admission and execution orchestration |
| `shape.event-turn-queue` | Pure reservation, exact-owner removal, FIFO promotion and response/audit projection |
| `infra.agent.queued-run` | Initial durable run and ordered queue event |
| `extern.event-turn-admission` | Native one-shot admission promise; failure resolves as data to avoid detached rejection |
| `extern.agent-turn-request` | Existing direct-start request normalization |
| `infra.agent.session-gate` | Existing session readiness and orphan reclaim orchestration |
| `extern.session-recovery` | Native recovery clock, diagnostics and busy rejection |

The runner facade retains all thirteen public names, multi-arity `spawn-direct!`, and its
original `defonce` state owners. Its source shrank from 728 to 392 lines. This is
still a process-local FIFO with `restart_aware: false`; this repair does not claim
cross-process scheduling, durable FIFO resumption, or changed queue limits.

Independent review found a second existing defect in the extracted session gate:
`reclaim-and-dispatch!` ignored a false reclaim result. A refused durable write
still authorized a second turn. Both previous-instance and cold-current-instance
branches now keep the busy rejection unless reclaim succeeds.

## Evidence and obstacles

- Actual Clio queue regression before repair: **4 tests / 15 assertions, 13
  failures, no errors**. It covered durable ordering, promotion during unfinished
  admission, overflow, and initial persistence refusal.
- First repaired queue proof: **4 / 15**, zero failures/errors. A further native
  case verifies failed pending admission frees its slot without releasing the
  current owner or running the refused model callback.
- The first session reclaim fixture incorrectly replaced a multi-arity provider
  function with a single-arity function. The fatal async guard caught that fixture
  error. The fixture was corrected before recording the authentic unchanged-code
  reclaim failure: **7 tests / 26 assertions, 4 failures, no errors**. Both failed
  persistence branches dispatched, while successful reclaim ordering stayed green.
- Final queue/reclaim native proof: **7 / 26**, zero failures/errors, **513 compiler
  inputs and zero warnings**, with the fatal async-error guard enabled.
- The first wider runner test run caught another old fixture: event-triggered
  `spawn-direct!` had no durable provider. It now uses the same real isolated Clio
  fixture and awaits the event turn before checking the interactive timeout.
- Final wider proof, including the existing runner, document admission settlement,
  and translation dispatch tests: **42 tests / 210 assertions**, zero failures or
  errors, **534 compiler inputs and zero warnings**, guarded Node exit zero.
- All thirteen owned production/test paths pass clj-kondo with all seven optional
  repository rules enabled and warnings treated as failures. The two standalone
  compiler scripts are linted separately because each intentionally uses `user`.
- The peer's real translation scope/action/turn proof passed **35 / 234** over the
  coherent queue and session repair (including the resolved trigger catalog). Their independent review found no additional confirmed
  queue race or cleanup defect; they identified the reclaim defect fixed above.

The native negative cases deliberately emit provider refusal, queue overflow,
and settlement retry diagnostics. These are asserted failure paths, not compiler
warnings. The tests restore provider/heap ownership and remove their temporary
ledger directories. The broader fixture also flushes each admitted run's events
before restoring the provider.

From `backend/`, with the pinned sandbox activation sourced:

```sh
set -e
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-queue-admission-proof.clj")'
NODE_ENV=test node --require ./scripts/shadow-test-error-guard.cjs target/queue-admission-proof/tests.cjs
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-queue-regressions.clj")'
NODE_ENV=test node --require ./scripts/shadow-test-error-guard.cjs target/queue-regressions/tests.cjs
```

These isolated targets do not rewrite the frozen production/browser artifacts.
Their compiler helpers exit nonzero on compile exceptions; shell `set -e` prevents
an old output bundle from running after a failed compilation.

## Self-review and remaining acceptance

I checked public facade names, preserved native multi-arity forwarding and state
owners, traced reservation/promotion/failure interleavings, and verified both
failure and retry paths through real Clio persistence. The pending admission gate
resolves an error value instead of rejecting an unobserved promise; exact-owner
release is idempotent if admission failure races promotion. No model starts before
its own run and queue event are durable.

The full combined backend suite and production/browser run are separate gates.
The isolated queue commands use the currently declared dependency graph; the
coordinated production build also verifies the frozen newer identity/Clio graph.
Final dependency promotion, advertised top-level build/typecheck, external review,
and both browser publication cycles remain required before merge. This document
records the bounded queue proof and does not claim final stack acceptance.

## Current-head Codex follow-up: durable startup failure

Codex comment `3996417205` found that early admitted startup failures only changed
heap state and appended `async_spawn_failed`. Appending the event does not patch
the run snapshot, so durable `queued` or `running` records remained active until
expiry even after the settlement callback ran.

Two native Clio regressions reproduced **10 failures in9 tests /37 assertions**
on the unchanged implementation. They cover startup before and after an initial
running snapshot, read the committed status inside the actual settlement
callback, and exercise a refused terminal snapshot write. A delimiter typo in the
new test was caught and corrected by the linter before compilation and before the
authentic RED result.

The failure handler now awaits `persist-run!` before notifying settlement. That
boundary first flushes the ordered failure event and then commits the failed
snapshot. If either durable step refuses, the outer execution boundary reports
the error, withholds settlement, and releases the exact process-local queue slot.
It does not pretend a terminal write succeeded. The old durable state can still
remain active when persistence itself is unavailable; repair remains necessary
and the diagnostic is explicit.

Final wider native proof: **44 tests /221 assertions**, zero failures/errors,
**535 compiler inputs and zero warnings**, fatal async guard exit0. Both touched
source/test paths pass all seven optional linter rules with zero warnings. The
facade is now395 lines. Independent review confirmed flush-before-snapshot-before-
settlement ordering, explicit failure reporting, and exact-owner release. The
review also noted the separate pre-existing queued-run heap retention limit; that
bounded follow-up is coordinated with the retention owner and is not claimed
fixed here. Production/browser08 predates this small follow-up, so combined gates
and current-head external review remain required.
