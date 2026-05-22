# Knoxx ↔ Openplanner MCP Integration

Date: 2026-04-18
Status: next
Story points: 5
Parent epic: Knoxx Epistemic Kernel Integration
Depends on: `openplanner-mcp-server` package existing and serving tools

## Purpose

Let Knoxx agents use the Openplanner MCP server as a tool so that:

- agents can query and write to the openplanner epistemic kernel;
- prompts and policies live in Knoxx contracts, while durable world-model lives in openplanner;
- the canonical inference loop (obs → fact → inference → attestation → judgment) flows through MCP.

## Problem

Knoxx agents currently talk to Openplanner via ad-hoc HTTP routes (`openplanner-memory-search!`, `openplanner-graph-query!`, `openplanner-semantic-search!`). These are hand-wired in `agent_hydration.cljs` and bypass the epistemic kernel primitives defined in `promptdb-core`.

The MCP bridge (`mcp_bridge.cljs`) already supports connecting to arbitrary MCP servers over HTTP. But no openplanner MCP server exists yet, and Knoxx has no `openplanner.*` tools in its tool catalog.

## Roles

- **Knoxx Backend** — manages orgs, users, roles, contracts, agents. Configures MCP tool definitions per-org / per-agent.
- **Openplanner MCP Server** — presents openplanner as MCP tools. Enforces project/source/tenant scoping.
- **promptdb-core** — shared .cljc library defining the epistemic schemas (Fact, Obs, Inference, Attestation, Judgment). Both Knoxx and the MCP server validate against these schemas.

## MCP Tool Configuration in Knoxx

For each org:

- Tool name prefix: `openplanner`
- MCP server URL: `http://openplanner-mcp-server:8010`
- Auth: Bearer token mapped to an openplanner API key (`KNOXX_OPENPLANNER_API_KEY` env).
- Default parameters:
  - `project`: Knoxx workspace project name (e.g. `devel`).
  - `source`: `{org-slug}.knoxx`

Tools exposed (mirrors server spec):

| Tool | Epistemic Primitive | Direction |
|---|---|---|
| `openplanner.query-graph` | graph read | read |
| `openplanner.search-events` | obs search | read |
| `openplanner.append-fact` | Fact | write |
| `openplanner.append-obs` | Obs | write |
| `openplanner.append-inference` | Inference | write |
| `openplanner.append-attestation` | Attestation | write |
| `openplanner.append-judgment` | Judgment | write |

## Agent Behavior

Contract kind `:tool-call` grants access to the `openplanner` MCP tool family.

Agent contracts compose:
- obs from Discord / HTTP / workspace events
- facts about actor roles
- tool calls into openplanner MCP

The canonical cycle:

```
1. Knoxx agent receives an obs
2. Contract fires, asks openplanner.query-graph / search-events
3. Agent reasons and emits inference + attestation via MCP tools
4. Openplanner MCP writes them to the epistemic store (promptdb-core validation)
5. Fulfillment contracts later read them back (also via MCP) to issue judgments
```

## Affected surfaces

### Knoxx Backend

- `runtime_config.cljs` — add env vars: `OPENPLANNER_MCP_BASE_URL`, `OPENPLANNER_MCP_TOOL_NAME`, `KNOXX_OPENPLANNER_PROJECT`, `KNOXX_OPENPLANNER_SOURCE`
- `mcp_bridge.cljs` — register openplanner as a named MCP server at startup
- `agent_hydration.cljs` — `create-openplanner-mcp-tools` factory that wraps the MCP bridge's openplanner catalog as agent SDK tools
- `tooling.cljs` — add `openplanner.*` labels/descriptions to the tool catalog
- `contracts_routes.cljs` — contract `:tool-call` kind resolves to `openplanner.*` tool grants

### Openplanner MCP Server (new package)

- `packages/openplanner-mcp-server/` — Node 22 slim container
- Serves MCP tools over HTTP (JSON-RPC 2.0)
- Translates tool calls into openplanner HTTP API calls + promptdb-core writes
- Enforces per-org / per-project scoping via API key

### promptdb-core (shared)

- Already exists at `packages/promptdb-core/`
- Schemas: Fact, Obs, Inference, Attestation, Judgment
- MCP server and Knoxx both validate against these schemas
- No changes needed in this spec — just confirming the dependency

## Configuration

New env vars in `knoxx-backend` service:

```
OPENPLANNER_MCP_BASE_URL     # default: http://openplanner-mcp-server:8010
OPENPLANNER_MCP_TOOL_NAME    # default: openplanner
KNOXX_OPENPLANNER_PROJECT    # e.g. devel
KNOXX_OPENPLANNER_SOURCE     # e.g. open-hax.knoxx
```

The MCP_SERVERS env format already supported by `mcp_bridge.cljs`:

```
openplanner:http://openplanner-mcp-server:8010:http
```

With shared secret from `KNOXX_OPENPLANNER_API_KEY` set via MCP server config.

## Security / Multi-tenancy

- Knoxx maps org → openplanner API key, not one global key.
- MCP server enforces project/source constraints based on that key.
- No cross-org graph leaks via MCP.

## Observability

- Every MCP call tagged with Knoxx `org-id`, `actor-id`, `run-id`.
- Knoxx exposes per-org stats on:
  - MCP usage counts
  - error rates
  - latency buckets

## Implementation phases

### Phase 1: MCP Server skeleton (3 pts)

- Create `packages/openplanner-mcp-server/` with health + schema endpoints
- Implement `openplanner.query-graph` and `openplanner.search-events` as pass-through to existing openplanner routes
- Docker Compose service with healthcheck

### Phase 2: Append tools (2 pts)

- Implement `openplanner.append-fact`, `openplanner.append-obs`, `openplanner.append-inference`, `openplanner.append-attestation`, `openplanner.append-judgment`
- Validate writes against promptdb-core schemas
- Wire project/source scoping from API key

### Phase 3: Knoxx integration (3 pts)

- Register openplanner in MCP_SERVERS env / mcp_bridge startup
- Add `openplanner.*` tool labels to tooling.cljs
- Add `:tool-call` contract resolution for `openplanner.*` tools
- Test end-to-end: contract fires → MCP tool call → epistemic store write

### Phase 4: Migration (2 pts)

- Gradually replace direct `openplanner-memory-search!`, `openplanner-graph-query!`, `openplanner-semantic-search!` calls with MCP-mediated equivalents
- Keep direct calls as fallback when MCP server is unavailable

## Verification

1. Knoxx agent with `:tool-call` contract can call `openplanner.query-graph`
2. Agent can write an inference via `openplanner.append-inference` and read it back
3. Invalid epistemic records (failing promptdb-core validation) are rejected with 422
4. Cross-org scoping prevents one org from reading another's graph
5. MCP server healthcheck passes in Docker Compose

## Definition of done

- Knoxx agents speak to OpenPlanner exclusively through MCP tools
- Every epistemic write is validated by promptdb-core schemas
- The inference loop (obs → fact → inference → attestation → judgment) is expressible purely through MCP tool calls
- Direct HTTP routes remain as fallback but are no longer the primary path
