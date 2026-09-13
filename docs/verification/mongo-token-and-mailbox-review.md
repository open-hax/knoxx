# Mongo token inventory and mailbox streaming review

Actual Codex review comments on PR305 identified two incomplete provider and
transport boundaries. Neither was dismissed after the earlier broad review
summaries. The corrected native regression suite reproduced **11 failures in
39 tests / 142 assertions**, with zero test errors, before the production fixes.

## Mongo token-ID revocation

Comment `3996143617` showed that selecting Mongo clears the local OAuth dispatch
provider, but token-ID revocation only supported that local provider. The real
Fastify DELETE route therefore returned HTTP400 for every Mongo token digest.
Native driver-handle fixtures additionally showed that historical inventory
returned bearer-bearing `token_data` and omitted the digest used by the UI.

`extern.mongo-mcp-token-inventory` now resolves a digest within the exact
membership's Mongo rows. The final atomic delete still matches both raw token
and membership, so ownership changing after lookup cannot revoke another
member's credential. A missing or foreign digest returns false and becomes the
same HTTP404. Invalid identities fail before a collection is opened. Inventory
returns an explicit list of safe grant fields plus `tokenId`; it excludes legacy
bearer fields, foreign rows and expired grants.

The member-scoped scan deliberately supports pre-digest rows without rewriting
data or exposing raw credentials. Its cost is linear in that membership's token
inventory; this compatibility repair does not claim indexed digest lookup.

Nine additional checks passed against the actual MongoDB8.0.13 native server and
Node driver: live inventory, bearer privacy, foreign and absent digest refusal,
successful own-token revocation, loss of bearer authorization, preserved foreign
authorization, repeated revocation and empty final live inventory. The probe
always drops its uniquely named temporary database and closes the client.

## SSE backpressure

Comment `3996207512` correctly distinguished a buffered write from a failed
connection. [Node's response-write contract](https://nodejs.org/api/http.html#responsewritechunk-encoding-callback)
requires waiting for `drain` after a false return value. The prior implementation
ended the stream instead.

The mailbox worker now retains its single writer while waiting for `drain`,
keeps its existing bounded pending-change queue and refreshes authorization
before emitting the next coalesced invalidation. Close and error cleanup settle
the pending wait and remove its listeners. The already accepted frame is not
sent a second time. No message body or identifier enters the SSE frame.

The new tests use a real Node HTTP server and `ServerResponse`, with a one-byte
high-water mark and an explicitly corked native socket. They observe the real
false write result, queued invalidations, native drain, successful continued
delivery, revocation while paused and disconnect cleanup. No mocked write
result supplies the backpressure evidence.

## Verification and obstacles

- Final focused native suite: **40 tests / 148 assertions**, zero failures/errors,
  under the fatal asynchronous error guard; **453 compiler inputs, zero warnings**.
- Standalone Mongo probe: **9 checks**, actual native server; **70 compiler inputs,
  zero warnings**.
- All owned source and tests pass the seven optional lint rules plus the existing
  size rules with zero errors/warnings. Each standalone Clojure compiler script
  is linted separately because each intentionally runs in its own `user` namespace.

The first new test attempts exposed fixture defects: misplaced async metadata
and the wrong route-registration helper arity. Those were fixed before counting
the complete RED result. The native Mongo library build also exposed eleven
inference warnings in the existing Mongo facade. Collection creation now uses
its named extern adapter, and existing opaque native collection handles carry
their correct JS type hint. No compiler warning was disabled.

The compiler helper's reflection setting originally leaked into lazily loaded
Shadow internals. It is now scoped to the script's actual Java exit boundary,
leaving the compiler's own configuration intact. Compile exceptions remain
fatal; old output is never counted as current verification.

Run from `backend`:

```sh
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-review-proof.clj")'
CONTRACTS_DIR=test/fixtures/empty-contracts NODE_ENV=test node --require ./scripts/shadow-test-error-guard.cjs target/review-proof/tests.cjs
pnpm exec shadow-cljs clj-eval '(load-file "scripts/compile-mcp-mongo-probe.clj")'
MONGO_URI=mongodb://127.0.0.1:37177 NODE_ENV=test node -e 'require("./target/review-proof/mongo-probe.cjs").verify().then(result => console.log(JSON.stringify(result)), error => { console.error(error); process.exitCode = 1; })'
```

The final command requires an actual Mongo server at the explicitly selected
URI. In this sandbox it ran with `--nounixsocket --wiredTigerCacheSizeGB 0.25`
inside the same supervised execution as the client, using a temporary data
directory removed after shutdown. The command does not skip absent Mongo.
These isolated targets do not rewrite the frozen browser bundles. The combined
suite and complete browser publication workflow remain separate acceptance gates.
