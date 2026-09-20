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

The startup proof delays or rejects each titles/temp-memory cache index. Required
persistence cannot finish or publish the run provider until both indexes complete.
The two cache reader facades also deliver provider failures asynchronously in both
of their supported arities.

These fixtures control the failing boundaries; they do not claim a deployed PM2
restart or a live Mongo outage. Native Mongo and authenticated route checks remain
separate qualifications described in [the provider walkthrough](run-event-providers.md).
