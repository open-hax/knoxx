# Run finalization and cache startup

Run `node scripts/verify-run-event-providers.mjs` with Node 24 on PATH, frozen
backend dependencies and the built sibling OpenPlanner SDK. The existing verifier
prints the checkout commit, compiles fresh sources and rejects failed assertions,
compiler warnings and unhandled asynchronous work.

The selected regressions call each actual accepted, refused and failed turn
finalizer against isolated Clio run/thread providers. They inject a mandatory run
persistence rejection, a thread completion rejection, and both together. Thread
completion is attempted before clearing the event sink and removing the active
agent session. A second completion failure logs a fixed message and only its
classified status/code, while the original run persistence rejection remains
visible to the caller. Provider messages and unrelated error data never enter
that secondary diagnostic. Successful finalizer responses retain their
existing shape and transcript behavior.

The refusal regression enters the actual completion selector with either a
`required-first` tool obligation and no tool receipt, or an empty assistant
output. It pauses each mandatory write separately: the ordered event append and
the final run snapshot. Before either write settles, neither the response nor the
WebSocket `run_failed` publication may escape. Successful settlement persists the
failed run before exactly one publication; a rejected write remains visible to
the caller, emits no WebSocket failure event, and still completes the thread,
clears the observer and removes the active agent session in order.

The event and snapshot are separate durable operations. A snapshot rejection can
leave its already accepted failure event in the ledger; this proof does not claim
cross-operation rollback. The best-effort OpenPlanner observer remains outside
the WebSocket publication guarantee. The verifier selects these regressions
through `extern.turn-finalization-test` without any additional flags.

The startup proof delays or rejects each titles/temp-memory cache index. Required
persistence cannot finish or publish the run provider until both indexes complete.
The two cache reader facades also deliver provider failures asynchronously in both
of their supported arities.

These fixtures control the failing boundaries; they do not claim a deployed PM2
restart or a live Mongo outage. Native Mongo and authenticated route checks remain
separate qualifications described in [the provider walkthrough](run-event-providers.md).
