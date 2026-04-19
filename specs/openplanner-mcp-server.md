# Openplanner MCP Server

Date: 2026-04-18
Status: next
Story points: 5
Parent epic: Knoxx Epistemic Kernel Integration
Depends on: `promptdb-core` schemas, openplanner HTTP API, openplanner-proxx (semantic search)

## Purpose

Expose openplanner's epistemic kernel (facts/obs/inferences/attestations/judgments)
and graph operations via an MCP server so Knoxx agents can treat openplanner as a
first-class tool.

## Problem

Knoxx agents access openplanner through direct HTTP calls wired in `agent_hydration.cljs`. This coupling means:
- epistemic writes bypass promptdb-core validation
- no unified tool interface for agents
- no MCP-level scoping enforcement
- the epistemic loop can't be expressed as agent tool calls

An MCP server between Knoxx and OpenPlanner solves all four.

## Responsibilities

- Serve MCP tools over HTTP (JSON-RPC 2.0 over HTTP/SSE)
- Translate tool calls into:
  - openplanner HTTP API calls (reads)
  - promptdb-core validated writes to the epistemic store (writes)
- Enforce per-org / per-project scoping based on API key
- Validate all writes against promptdb-core Malli schemas before persisting

## Placement

- Package: `@workspace/openplanner-mcp` in `orgs/open-hax/tooloxx/services/openplanner-mcp/`
- Tooloxx is the canonical MCP consolidation home — all MCP services live there
- Uses `@workspace/mcp-foundation` (shared from root workspace at `packages/mcp-foundation/`)
- Pattern follows `threat-radar-mcp`: Express + `createMcpHttpRouter` + Zod
- Docker Compose: `openplanner-mcp-server` service in the openplanner compose file
- Depends on:
  - `openplanner` (graph + events API on port 7777)
  - `openplanner-proxx` (semantic search, embeddings)

## Interfaces

### HTTP

| Endpoint | Purpose |
|---|---|
| `GET /health` | Liveness probe |
| `GET /mcp/schema` | MCP discovery (server metadata, tool list) |
| `POST /` | MCP tool invocation (JSON-RPC 2.0) |

### MCP Tools (first cut)

#### Read tools

**`openplanner.query-graph`**

Input:
```edn
{:project  "devel"          ;; required
 :query    "some text"      ;; required
 :lake     "knoxx-session"  ;; optional lake filter
 :nodeType "docs"           ;; optional node type filter
 :limit    10               ;; optional, 1..20
 :edgeLimit 20}             ;; optional, 0..60
```

Output:
```edn
{:nodes  [{:id "devel:file:README.md" :label "README.md" :type "docs" :score 0.87}]
 :edges  [{:source "devel:file:README.md" :target "web:url:https://..." :label "mentions_web_url"}]
 :annotations {:project "devel" :totalMatches 42}}
```

**`openplanner.search-events`**

Input:
```edn
{:project "devel"
 :query   "translation review"
 :top_k   5}
```

Output:
```edn
{:events [{:id "evt-123" :kind "obs" :score 0.91 :snippet "translation review completed for..."}]
 :total  142}
```

#### Write tools

Each append tool writes promptdb-core-shaped records into the epistemic store.
All writes are validated against the corresponding Malli schema before persisting.
Invalid records return 422 with schema error details.

**`openplanner.append-fact`**

Input (matches `promptdb.core/Fact`):
```edn
{:ctx   :己
 :claim "Actor A has role admin in org open-hax"
 :src   "knoxx:actor:abc123"
 :p     0.95
 :time  "2026-04-18T02:00:00Z"}
```

**`openplanner.append-obs`**

Input (matches `promptdb.core/Obs`):
```edn
{:ctx    :世
 :about  :discord/message
 :signal {:content "hello" :channel-id "123"}
 :p      0.9}
```

**`openplanner.append-inference`**

Input (matches `promptdb.core/Inference`):
```edn
{:from  [{:ctx :己 :claim "A is admin" :src "knoxx" :p 0.95 :time "2026-04-18T02:00:00Z"}]
 :rule  :contract/grant-access
 :actor :knoxx/agent-1
 :claim "A may access project devel"
 :p     0.9}
```

**`openplanner.append-attestation`**

Input (matches `promptdb.core/Attestation`):
```edn
{:actor     :knoxx/agent-1
 :did       "granted access to project devel"
 :run-id    #uuid "..."
 :causedby  #uuid "..."   ;; optional
 :p         0.95}
```

**`openplanner.append-judgment`**

Input (matches `promptdb.core/Judgment`):
```edn
{:of      #uuid "..."
 :verdict :held          ;; :held | :failed | :partial
 :auditor :knoxx/fulfillment-contract
 :p       0.9}
```

## Package structure

```
services/openplanner-mcp/          # lives in tooloxx, not openplanner
├── package.json
├── tsconfig.json
├── Dockerfile
├── .env.example
├── src/
│   ├── main.ts          # Express + MCP server entrypoint
│   ├── client.ts        # OpenPlanner HTTP API client
│   ├── schemas.ts       # Zod port of promptdb-core Malli schemas
│   └── tools.ts         # MCP tool registration (7 tools)
└── README.md
```

## Validation strategy

The MCP server must validate all writes before persisting. The schemas are defined
in `promptdb-core` as Malli schemas. The MCP server is TypeScript, so it needs
a TypeScript port of the validation rules.

Options:

1. **TS port of schemas** — duplicate the Malli schemas as Zod schemas in TS.
   Pros: no runtime CLJ dependency. Cons: schema drift between the two.

2. **CLJ subprocess** — call a small CLJ validator binary.
   Pros: exact same schemas. Cons: overhead, complexity.

3. **Shared .cljc with nbb/cljk** — use a CLJS runtime in Node.
   Pros: same source. Cons: nbb may not support Malli well.

**Decision: Option 1 (Zod schemas)** with a schema-sync test that imports the
promptdb-core EDN definitions and asserts they match the Zod schemas. This is
the pragmatic choice for a Node server, and schema drift is caught by CI.

## Configuration

Environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `OPENPLANNER_BASE_URL` | — | Openplanner HTTP API base URL |
| `OPENPLANNER_API_KEY` | — | API key for openplanner writes |
| `PROXX_BASE_URL` | — | Proxx semantic search base URL |
| `PROXX_AUTH_TOKEN` | — | Proxx auth token |
| `OPENPLANNER_MCP_PORT` | `8010` | Server listen port |
| `OPENPLANNER_MCP_HOST_PORT` | `8010` | Docker host port |
| `OPENPLANNER_MCP_DEFAULT_PROJECT` | `devel` | Default project scope |
| `OPENPLANNER_MCP_DEFAULT_SOURCE` | `knoxx` | Default source scope |
| `KNOXX_OPENPLANNER_API_KEY` | — | API key for Knoxx→MCP auth |

## Observability

- Structured JSON logs with `run-id`, `actor-id`, `org`, `tool-name`, `duration-ms`
- Optional OTEL spans tagged with `service.name=openplanner-mcp-server`
- `/health` returns `{"status":"ok","uptime":...,"toolsConnected":7}`

## Failure modes

| Failure | Behavior |
|---|---|
| Upstream openplanner/proxx unavailable | Tool errors surfaced to Knoxx as MCP error responses |
| Validation failure on epistemic write | 422 with detailed schema error |
| Rate limiting | Backoff strategy for Knoxx agents (exponential with jitter) |
| Invalid API key | 401 response |

## Docker Compose integration

```yaml
openplanner-mcp-server:
  build:
    context: ../../packages/openplanner-mcp-server
    dockerfile: Dockerfile
  environment:
    OPENPLANNER_BASE_URL: http://openplanner:7777
    OPENPLANNER_API_KEY: ${OPENPLANNER_API_KEY}
    PROXX_BASE_URL: http://proxx:8789
    PROXX_AUTH_TOKEN: ${PROXX_AUTH_TOKEN}
    OPENPLANNER_MCP_PORT: "8010"
    OPENPLANNER_MCP_DEFAULT_PROJECT: devel
    OPENPLANNER_MCP_DEFAULT_SOURCE: knoxx
  ports:
    - "${OPENPLANNER_MCP_HOST_PORT:-8010}:8010"
  networks:
    - default
    - ai-infra
  depends_on:
    openplanner:
      condition: service_healthy
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8010/health"]
    interval: 30s
    timeout: 5s
    retries: 3
```

## Verification

1. `/health` returns 200
2. `/mcp/schema` lists all 7 tools
3. `openplanner.query-graph` returns graph nodes for a known project
4. `openplanner.append-obs` validates and persists an Obs record
5. Invalid Obs (missing required field) returns 422 with error details
6. Cross-project scoping: API key for project `devel` cannot read `other-org` data

## Definition of done

- MCP server serves all 7 tools over HTTP
- All writes are validated against promptdb-core schemas (Zod port)
- Docker Compose integration with healthcheck
- Knoxx can connect via `mcp_bridge.cljs` and call tools
- Schema-sync test passes (Zod schemas match Malli schemas)
