# Run, event, thread and cache providers

Run from this checkout:

```sh
node scripts/verify-run-event-providers.mjs
node scripts/verify-run-event-providers.mjs --mongod /absolute/path/to/mongod
```

The verifier prints the checkout SHA, compiles these exact sources against the
published dependency pins, rejects compiler warnings, and requires positive
assertion counts with zero failures/errors. Its native async guard rejects
unfinished or unhandled test work. Fixtures create and remove their own temporary
Clio ledgers; the optional Mongo fixture owns its TCP process, data directory and
shutdown. No running Knoxx service or application database is used.

The default proof shows:

- Stable run/event identities, exact retry and collision refusal, ordered event
  sequences and durable reconnect queries after reopen. Heap events and sequence
  fields cannot replace the provider's authoritative history.
- Unauthorized and cross-tenant durable query requests are refused. These are
  direct query-port calls; authenticated route selection remains a later layer.
- The unique Mongo run index must complete before the provider is published;
  the installed event writer handles the first event immediately after startup.
- The actual HTTP lifecycle waits for mandatory persistence before opening its
  listener or signaling PM2 readiness. Delayed installation leaves a real Fastify
  listener closed; missing Mongo or a rejected index closes the unpublished app
  and propagates failure. Background recovery starts only after successful listen.
- A queue acknowledgment waits for the run and its first event. Pending turns
  cannot start before their own admission, failed admissions release their exact
  slot, and diagnostic history trimming cannot evict owned runs.
- Run-bound live steering and follow-up controls wait for their audit event
  before responding or publishing over WebSocket. Delayed and rejected event
  writes are exercised on both successful and failed provider-control paths;
  rejected durability cannot produce a successful acknowledgment or broadcast.
  Dispatch happens before audit admission: audit rejection cannot undo a control
  already dispatched, and this boundary does not claim exactly-once control retry.
- Completed runs and final events persist without OpenPlanner. Its optional
  archival projection remains best effort; durable run persistence fails visibly.
- Initial run, thread or event refusal releases only the invocation's newly
  constructed session and exact observer callback. The proof runs actual session
  construction with an owned provider adapter and real Clio admission. Replacement
  sessions/observers, established sessions and other pending claimants survive.
  Failed hydration returns promptly even while session construction is pending;
  its observed continuation removes the late construction only when no other
  claimant needs it. Cleanup or diagnostic logging failures cannot mask the
  original refusal. This does not add provider cancellation or timeouts.
- `run_started` and `action_task_rendered` broadcasts follow their own durable
  event flushes. Delayed writes produce no premature broadcast; rejected writes
  preserve prior accepted facts and publish no refused event. This narrow repair
  moved forward from private PR341 admission work; no HTTP202 handshake or
  process-wide OpenPlanner observer activation moves into this provider layer.
- Passive and memory hydration events also flush before their broadcasts and
  before model execution. The actual turn proof delays and rejects each write
  independently against real Clio replay. A rejected write preserves the original
  error, never invokes the provider, and releases its startup session and sink.
  Hydration publication stays inside initial admission, before claim promotion;
  this ordering alone does not claim atomic rollback of earlier durable writes.
- Spawn failures before admission stay in the private operator log. The proof
  rejects 24 actual turns during hydration, verifies no missing-run events or nil
  registry entries remain, then admits the same run ID successfully. A separate
  pre-admission invocation reuses an existing ID and leaves its heap record,
  durable status and event history unchanged. Generic spawn-error handlers are
  log-only: an ID lookup cannot establish ownership of an invocation. Only the
  queue's explicit paths after their own awaited run/event admission record
  these failures; ordinary turn finalizers retain their own durable settlement.
  Admitted FIFO failures still produce an event, but replay and snapshots contain only fixed
  public text, a validated HTTP error status and a stable failure code. Raw
  upstream messages, stacks and nested diagnostic data remain private log data.
  Diagnostic events for already evicted heap runs likewise remain log-only;
  this does not discard or reset failures of events already admitted to the queue.
- Mongo thread queries hide expiry before TTL cleanup, portable expiry survives
  decoding, legacy cache metadata stays outside application values, and the
  disposable session cache is bounded with provider ownership preserved.
- Concurrent thread patches update only their supplied fields, retaining the
  other writer's transcript, run and streaming state. Rewinds compare the observed
  transcript before applying their fields and retry a changed transcript; two
  concurrent rewinds remove two turns instead of silently losing one operation.

The optional Mongo proof admits concurrent events, checks stable retry and
collision behavior, gracefully restarts the actual owned process and recovers
ordered history. It also admits 1,001 events totaling more than 16 MiB and a
single event larger than 16 MiB, measures actual fresh-append BSON command sizes,
and checks exact Unicode reconstruction across fragments. It injects lost
acknowledgments after real fragment, header, migration-fence and head writes;
these are application-side faults around actual journaled writes, not network
fault or replica-failover simulation. Canonical migration races an old writer,
and native revisionless migration compares BSON Date/ObjectId-bearing records.
Large v2 metadata remains writable; oversized metadata refuses before head
publication. Missing accepted fragments refuse full replay rather than return a
partial history. It also writes independent thread patches concurrently, rewinds twice
concurrently, and verifies the combined state after an actual process restart.
The thread/cache expiry and legacy decoding cases still use driver-shaped
fixtures. It uses `--nounixsocket` and a 0.25 GB WiredTiger cache. Missing or failed
native execution remains visible; the default proof does not substitute for it.

## Layer boundary and limitations

Mongo is the existing bootstrap authority here. Startup awaits the unique run
index and installs the durable event writer before listening or signaling ready.
The proof uses controlled connection/index boundaries and an owned Fastify TCP
listener; it does not launch PM2 or claim deployed startup acceptance. EDN/Clio
providers are exercised through explicit provider selection in fixtures. Broader
deployment selection remains in the later integration layer.

The existing HTTP routes and browser are not yet switched to the durable query
ports in this PR. The verifier prints this limitation every run. The later HTTP
layer owns actual authenticated route/reconnect and browser acceptance; these
checks do not claim that user surface is already migrated.

Mongo stores each event in immutable preparations: bounded UTF-16LE byte
fragments and a content-addressed header. Only membership in the chain referenced
by the accepted run head makes a preparation an accepted fact. All preparations
are majority-acknowledged and journaled before one head CAS publishes the tail.
A fresh UUID head token fences concurrent replacements and revision reuse.

```text
run head --accepted tail--> event header --> previous event header --> ...
                              |                    |
                         payload fragments    payload fragments

unreferenced preparations: retained, but neither accepted facts nor ID reservations
```

The head contains run metadata, immutable ownership coordinates and one chain
reference, never the history vector. A fresh event ID uses a bounded indexed
candidate lookup; a hit requires actual accepted-chain membership before retry
or collision handling. Normal append writes are proportional to the new payload,
independent of accepted history length. Each fragment is at most 64 KiB before
base64 encoding (96 KiB BSON ceiling); headers have a 4 KiB ceiling. Full Unicode
code units, namespaced EDN values and contiguous sequences survive reconstruction.
Digest indexes are hints; reconstructed identities and payloads decide equality.
Exact retry requires a visible run, preserves expiry/history, and performs a new
journaled revision write instead of treating a read-visible result as durable ack.

The native proof verifies histories beyond Mongo's former 16 MiB history ceiling.
Run metadata itself still has a bounded BSON head/command capacity, with `413
run_store_record_too_large` before publication. Old ordered canonical snapshots
migrate by validating their entire history, preparing immutable events, stamping
an exact observed snapshot with a small token and revision increment, then
replacing only that token/revision. A preceding canonical writer replaces away
the token, forcing migration to retry. The closed v2 envelope makes that preceding
snapshot provider refuse new heads rather than treat them as empty histories.

Revisionless native legacy rows retain a complete BSON equality predicate during
adoption. If that predicate plus the replacement exceeds the safe driver command
bound, `413 run_store_legacy_record_too_large` leaves the legacy row byte-for-byte
unchanged. Such rows need explicit recovery with old writers quiesced; automatic
migration does not discard fields or invent chronology. Prepared but unaccepted
event fragments may remain after refusal. Legacy nonempty histories lacking stable
IDs/sequences still refuse with `run_events_migration_required`. Arbitrary older
clients that issue blind native writes must be quiesced before adoption; this is
not a guarantee for those clients' rolling upgrades.

Limits remain explicit: unaccepted preparations accumulate without automatic GC;
old-ID retries/collisions and time-cursor replay may walk the accepted history;
normal fresh admission checks the tail and candidate index without rereading all
old payloads. Replay validates each requested event and refuses corruption. Expiry
and deletion hide the view while retaining history and owner bindings. Native
qualification uses standalone Mongo with journaling, graceful restarts and
instrumented lost acknowledgments; it does not establish replica failover behavior.
OpenPlanner remains an archival projection and cannot be installed as exact event
authority.

`run-event-recovery-manifest.json` records the historical reconstruction. Its old
lost-workspace evidence is not evidence for the current checkout; use this
verifier and the current PR's exact-head validation record.
