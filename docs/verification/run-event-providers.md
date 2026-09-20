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
  direct query-port calls; no HTTP listener is exercised in this layer.
- The unique Mongo run index must complete before the provider is published;
  the installed event writer handles the first event immediately after startup.
- A queue acknowledgment waits for the run and its first event. Pending turns
  cannot start before their own admission, failed admissions release their exact
  slot, and diagnostic history trimming cannot evict owned runs.
- Completed runs and final events persist without OpenPlanner. Its optional
  archival projection remains best effort; durable run persistence fails visibly.
- Mongo thread queries hide expiry before TTL cleanup, portable expiry survives
  decoding, legacy cache metadata stays outside application values, and the
  disposable session cache is bounded with provider ownership preserved.

The optional Mongo proof admits concurrent events, checks stable retry and
collision behavior, restarts the actual owned process and recovers ordered
history. It uses `--nounixsocket` and a 0.25 GB WiredTiger cache. Missing or failed
native execution remains visible; the default proof does not substitute for it.

## Layer boundary and limitations

Mongo is the existing bootstrap authority here. Startup awaits the unique run
index and installs the durable event writer. EDN/Clio providers are exercised
through explicit provider selection in fixtures. Deployment selection and
application lifecycle composition remain in the later integration layer.

The existing HTTP routes and browser are not yet switched to the durable query
ports in this PR. The verifier prints this limitation every run. The later HTTP
layer owns actual authenticated route/reconnect and browser acceptance; these
checks do not claim that user surface is already migrated.

Mongo retains immutable run identity and event history in one document using
revision CAS, majority acknowledgment and journaling. Expired runs are hidden,
while their facts and ownership bindings remain retained. This increases storage;
Mongo's 16 MB document limit remains a practical bound. Oversized histories fail
instead of silently trimming events. Legacy nonempty histories lacking stable
IDs/sequences refuse with `run_events_migration_required`; ordering is not
fabricated. OpenPlanner remains an archival projection and cannot be installed
as an exact event authority.

`run-event-recovery-manifest.json` records the historical reconstruction. Its old
lost-workspace evidence is not evidence for the current checkout; use this
verifier and the current PR's exact-head validation record.
