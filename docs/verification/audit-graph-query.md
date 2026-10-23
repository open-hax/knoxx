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
