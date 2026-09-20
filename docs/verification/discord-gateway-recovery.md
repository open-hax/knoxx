# Discord gateway ownership and native regression proof

The 1,282-line gateway combined the Discord client, message transport, audio
codec, voice capture, manager composition and process registry. Six named extern
owners now hold those SDK operations; the original namespace retains its public
compatibility functions and the original `manager*`, `actor-managers*` and sodium
`defonce` vars. Existing managers therefore keep their registry identity across
source reloads. The manager factory receives the original registry setter as a
callback, keeping the dependency graph one-way.

Two actual runtime defects were found while verifying the split:

1. The options parser combined `setDefault`/`:set-default?` with `or`, which
   discarded explicit `false`. Creating an actor-specific manager overwrote the
   default manager. Tests against the original native factory failed three
   assertions across both option shapes and actor creation. The parser now
   distinguishes an absent option from explicit false.
2. The no-argument native `getVoiceConnection()` returned a map entry
   `[guild-id, connection]`. A real factory test with a finite voice-join adapter
   reproduced that mismatch while explicit guild lookup succeeded. Iterating
   map values now returns the expected opaque connection handle.

Before the optional-lint binding renames, a token-level comparison found 141 of
143 implementation definitions unchanged except namespace qualification,
visibility and documentation. The two declared changes then were the parser and
factory setter callback. The subsequently reproduced voice lookup repair is a
third intentional behavior change. Native signatures, all existing public
functions, registry vars and the CommonJS sodium loading workaround remain.
An independent bounded review checked the registry lifetime, dependency graph
and lifecycle/listener/message/voice method wiring without finding another issue.

## Fresh evidence

With Node 24.20, CLJS 1.12.145 and the frozen eta identity worktree at
`e0cdf3585b15101c83ef01b7bc2901652b3b54dd` supplied through Clojure local overrides:

- Original factory regression: **3 tests / 12 assertions, 3 failures**.
- Expanded extraction test: **7 tests / 33 assertions, 1 failure**, isolating
  the voice map-entry defect after the option parser repair.
- Final distinct `:discord-gateway-proof` compilation: **65 inputs,
  9 compiled, 0 warnings**.
- Final guarded native Node execution: **7 tests / 33 assertions,
  0 failures / 0 errors, actual process exit 0**.
- Scoped clj-kondo, explicitly enabling the seven optional rules required by
  `AGENTS.md`: **0 errors / 0 warnings**.

The proof covers real Discord client construction/event wiring without login,
message and WAV field conversion, sequential message chunks with the attachment
and reply sent only once, default/actor registry ownership, voice handle lookup
and connection teardown. It does not authenticate to Discord or prove live
gateway delivery, voice negotiation or microphone capture.

The focused compiler owns its own Shadow build ID and output directory:

```sh
cd backend
clojure -M:cljs scripts/compile-discord-gateway-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts \
  node --require ./scripts/shadow-test-error-guard.cjs \
  target/discord-gateway-proof/tests.cjs
```

The advertised dependency pins still require the coordinated eta promotion;
the evidence above used the explicit frozen local overrides. Full backend05
(1,797 tests / 8,124 assertions) predates this extraction. A subsequent complete
guarded run and production compilation must cover the final combined source.
