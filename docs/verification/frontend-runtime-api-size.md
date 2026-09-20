# Runtime response decoding, Proxx and media boundaries

The existing API core now owns runtime response decoding beside its existing
wire decoders. The mailbox, tool catalog and conversation normalizers retain
their individual fallback and alias semantics. In particular, the runtime record
predicate still accepts arrays where it did before; it does not silently inherit
the administration decoder's stricter object predicate. Public API calls retain
their original exports and transport behavior.

The existing Proxx API module now owns model listing, health and chat requests.
The media boundary owns studio audio library, labels, playlists, derived assets
and Discord media imports. Runtime/core/Proxx/media are 380/374/131/384 lines,
below the unchanged 400-line threshold. No new TypeScript paths were added.

Four new regressions exercise malformed mailbox rows, explicit false durability,
encoded acknowledgement identifiers, model sorting, exact sampling payloads,
zero-depth library queries, media URL encoding and generated-asset metadata.
The API test mock now replaces only request transport while retaining actual
response decoders. Intermediate typecheck correctly rejected interface exports
without `export type`; those exports were fixed before the final gates.

Complete typecheck and isolated legacy emission pass. The combined full suite
passes **45 files / 264 executed tests / 0 failures**, with **41 existing TODO
cases explicitly unexecuted**. All four source-file size gates pass. The latest
combined source also includes the actor/graph and Contracts controller work.
Production output remained untouched during these checks.
