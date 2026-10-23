# Direct-Mongo actor graph verification

Build the candidate tool module with `pnpm -C backend exec shadow-cljs compile
graph-query-probe`. The `:app` legacy target is not used: it references the
retired eta-mu-session-ingester namespace.

Run `scripts/verify-audit-graph-query.mjs` using Node with the existing protected
application MONGODB_URI, MONGODB_DB=openplanner and
KNOXX_OPENPLANNER_CLIENT_MODE=mongo. KNOXX_GRAPH_PROBE_MODULE can name an absolute
path to the compiled probe.js inside an isolated candidate runtime. Keep the
entire dist-graph-probe directory together; it imports compiled sibling modules.
Never put credentials on a command line or in this repository.

The script reads the real registered graph_query tool from the same actor/MCP
tool factory the application uses, with the system_admin actor context. It
checks a harness-to-principal edge, then a syscall observation and its incident
host/effective-principal edges, preserving raw-evidence provenance. It also
checks edgeLimit=0, a wrong lake, and a context without graph permission.
It calls the actual tool execute function, not a replacement Atlas query.

No provider/model call is needed and no fixtures or graph data are written.
Read-only verification therefore has no fixture teardown. SDK initialization
may ensure the application's existing indexes, as on normal startup.

This module is an isolated candidate verification artifact. Record its source
revision, module hashes, target host/image and command output alongside the
test receipt. It does not by itself prove that production MCP serves that
revision. After reviewed deployment through Services, invoke the connected
actor graph_query tool again and record its result separately.

Direct-Mongo results explicitly report mode=structural and one-hop traversal.
They search literal node metadata and include a bounded, closed subgraph.
They do not claim vector similarity, weighted semantic traversal, or vector
coverage for the audit corpus. Lake filters constrain stored project names;
node-type filters select seeds, while incident endpoint types can differ.
The existing REST mode retains its own graph query implementation.

## Observed candidate verification

The candidate compiled from `28e42721962d2ad15646087f962ae886d6341ded`
was invoked inside the existing production backend container as its unprivileged
`node` user, using its existing application configuration. The isolated module
lived under `/app/state/verification/direct-mongo-audit-graph/`; production
application modules and serving processes were unchanged.

The actual actor tool factory exposed `graph_query` for `system_admin`. Its
execute function passed harness/principal retrieval, syscall provenance,
incident host/effective-principal/path relations, zero edge budget, wrong-lake
isolation and denied-context tool filtering. One returned observation was
`audit:observation:67102285bcffbd42945508260593fcc934948b4589b537d9752b7f320d3351e5`,
pointing to the same SHA in `linux_audit.events`. This is a read of live data,
not a seeded fixture and not a model-generated answer.

Local and remote SHA-256 hashes matched:

- `probe.js`: `1fee919e723e6ef03d74fad8e46580077787f64f272c6af21bb9e1427a183163`
- `knoxx.backend.extern.openplanner_graph.js`: `de6cc8d3c46d893594ca784aec3af9eb7a7a84c3d4a5d0e65747c319e3ba139a`
- `knoxx.backend.domain.graph_query.js`: `51462800d65c150a2e296419bab7cb6c255eee031587bdfd07a5e0dee5fa5b73`

The probe includes the current main branch's `fs-ext-extra-prebuilt` runtime
dependency in its isolated directory because the older serving image lacks it.
No package was installed into the serving application's node_modules.

The current-main candidate suite passed 1,851 tests and 9,119 assertions with
zero failures/errors and zero compiler warnings. Server and probe builds passed.
A separate invocation of the connected production MCP `graph_query` still
returned `OpenPlanner is not configured`: reviewed deployment and that final
connected-tool readback remain outstanding.
