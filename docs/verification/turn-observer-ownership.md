# Turn observer ownership

An older turn can finish while a newer turn has installed an OpenPlanner event
observer. Terminal cleanup must release only the exact callback owned by that
turn, including when run persistence or thread completion rejects. The original
unconditional clear erased the newer callback after either awaited operation.

Run the existing checkout-bound verifier with Node 24:

```sh
volta run --node 24.14.1 node scripts/verify-run-event-providers.mjs
```

Its compiler includes `extern.turn-sink-ownership-test`. The fixture runs real
prompt success, empty-output refusal, and provider failure through the production
finalizers using temporary Clio run/thread stores. It holds two prompts open,
finishes the older one, records newer progress through the still-live callback,
and then verifies that the newer owner clears its callback. Separate controlled
settlement waits cover replacement during run persistence and thread completion,
with both successful and rejected outcomes. Temporary stores and global fixture
registries are restored in `finally` blocks.

For the narrower regression, from `backend/`:

```sh
volta run --node 24.14.1 clojure -M:cljs scripts/compile-turn-sink-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts volta run --node 24.14.1 node \
  --require ./scripts/shadow-test-error-guard.cjs target/turn-sink-proof/tests.cjs
```

The public 14-argument prompt API remains available. It claims no ambient
observer. The production caller passes its captured callback as the new final
argument; callbacks are not serialized into durable records or error diagnostics.

This proof does not contact an LLM or hosted OpenPlanner and does not qualify a
browser reconnect. The verifier reports the later HTTP/composition limitation as
`WARN`; process-wide observer selection remains the separate #341 boundary.
