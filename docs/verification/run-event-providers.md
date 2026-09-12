# Run-event providers: reconstructed checkpoint

## Recovery status

This document and the 26 paths in `run-event-recovery-manifest.json` were reconstructed after the second scratch workspace disappearance at approximately 2026-09-12 10:45 UTC. The executor subsequently returned HTTP 409 `environment_offline`, including for commands outside the removed workspace. No new execution was possible.

The lost local checkpoint was `8550c8dbc82dedaf556e82d98e197fac99c6619c`. Its original file-hash manifest did not survive. Recovery uses literal source writes retained in the agent session and unchanged portions fetched from the published base `377892dcf4908c31b74ccc9349db04356a5d9181`. All 26 recovered changes are classified as **reconstructed, unverified**. Matching intent or remembered source does not establish byte equivalence. Historical green results below do not transfer to these recovered bytes.

Two shared integration files are deliberately excluded: `domain/action/run_state.cljs` and `infra/agent/turn.cljs`. Their coordinated changes are held by the runtime publisher and must be integrated after the new `extern/run_event.cljs` and `infra/run_event_payload.cljs` dependencies exist.

## Intended behavior

EDN and Mongo implement ordered durable run events, snapshots, and authorized scoped directory listing. A producer allocates a UUID once through a named extern boundary. Two otherwise identical events in the same millisecond receive distinct identities; retrying the same immutable event retains its identity and admitted sequence.

The `/api/knoxx/runs/:runId` and `/api/runs/:runId` surfaces read the selected durable provider after authentication and ownership checks. `/api/runs` lists the caller's authorized scope with a bounded limit of 1–500. Missing or unsupported providers return a visible 503; process-local run maps are not a restart fallback.

Mongo uses a unique `run_id` index and per-document revision CAS. Accepted transitions allocate sequence numbers; snapshot writes cannot replace event history, ownership, or conversation identity. The `run_state_edn` field preserves namespaced values. Accepted writes and exact retries use majority acknowledgement and journaling. Exact retries rewrite the same logical state under the next storage revision so uncertain prior writes receive a fresh acknowledgement without changing the event identity or sequence.

Expired runs are hidden by the view while their facts and ownership bindings remain retained. New documents omit the legacy TTL date to prevent TTL deletion from permitting identity reuse. This increases retained storage. Mongo's 16 MB document limit remains a practical bound; oversized histories fail rather than silently trimming events. A future separate event collection would require an equally strong ordered transactional contract.

Legacy empty histories can migrate on their next write. Existing nonempty histories without stable event IDs and sequence numbers refuse with `run_events_migration_required`; ordering is not fabricated.

The existing OpenPlanner session class is an approximate archival projection, not an exact CAS event authority. Configured authoritative run providers remain EDN or Mongo. Installing the archive as an event authority refuses explicitly; archival projection remains available.

## Fresh verification required after reconnect

Use the existing Node, Clojure, and Mongo toolchain in this same sandbox:

```sh
node scripts/verify-run-event-providers.mjs --mongod /absolute/path/to/mongod
```

The verifier uses distinct Shadow build IDs, checks for compiler warnings, requires positive test and assertion counts, and uses the native async failure guard. It starts only its own temporary TCP Mongo process with a 0.25 GB WiredTiger cache, restarts that actual process against the same owned directory, and cleans up on completion or signals. Omitting Mongo prints an explicit native-proof omission.

Run scoped lint, both focused proofs, production compilation, the full guarded backend suite, and the real browser tour against the recovered integrated tree before accepting it. The script itself is reconstructed and requires syntax checking before use.

## Historical evidence, prior to workspace loss

These results applied to the lost original checkpoint, not to this reconstruction:

- Focused selected-provider, producer-identity, and actual Fastify route proof: 19 tests, 131 assertions, zero failures/errors; 438 compiled files, zero warnings.
- Native Mongo proof: 1 test, 13 assertions, zero failures/errors; 102 compiled files, zero warnings. It admitted 20 concurrent distinct events, verified same-ID retry and collision handling, and replayed 22 ordered events after an actual Mongo process restart.
- Parent production server build: 615 files, zero warnings.
- Scoped changed-source lint: zero errors/warnings.
- No final complete browser or full-suite result is claimed.

## Mongo obstacle and resolution observed before loss

Mongo 8.0.13 initially exited with code 100 and an `Operation not permitted` error before the first assertion. Its async test printed a misleading zero-failure, zero-assertion summary, but the test preload correctly rejected the unhandled error and exited nonzero. A smaller cache and disabled diagnostics alone did not solve startup. System-call tracing was blocked by the sandbox's ptrace restrictions.

The previously documented TCP-only setting `--nounixsocket`, combined with `--wiredTigerCacheSizeGB 0.25`, made the same Mongo binary run in the same sandbox. The native concurrent-write and restart proof then passed. The recovered fixture retains bounded diagnostic output so a fresh startup failure remains visible.
