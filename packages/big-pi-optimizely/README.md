# Big Pi: local Optimizely compatibility lab

A local CMS, REST API, MCP server, and bridge to the current Knoxx CMS for testing
technical content and translation workflows. This package is the first executable
slice of the [Big Pi Optimizely workstream](../../kanban/epics/big-pi-optimizely-compatibility.md).
It is independently runnable inside Knoxx while Big Pi's product extraction proceeds.

The profile is **`cms-saas-rest-v1-local-subset`**. It implements selected documented
SaaS v1 request shapes over a local test store. It is an unofficial compatibility
model, not Optimizely software or a certified substitute. It does not yet implement
the entire API, Graph, the Optimizely editor, CMS 12/13, or vendor MCP tool parity.
The [coverage contract](compatibility.edn) and parent workstream retain that scope.

## Run locally

Use Node.js 24 or later. From the Knoxx checkout:

```bash
npm ci --ignore-scripts --prefix packages/big-pi-optimizely
bash scripts/start-big-pi-local.sh
```

Open **http://127.0.0.1:4175**. The server prints its generated local bearer token
to the terminal; paste it into the workbench. The token stays in the page's memory.
Use **Load a technical example** for synthetic source text, then edit or create a
language draft. Values in the example are test data, not FLIR specifications.

State and its persistent source-store identity live in `packages/big-pi-optimizely/.local-data/` by default. Set
`BIG_PI_DATA_DIR` to choose a different directory and `BIG_PI_PORT` to choose a port.
`BIG_PI_TOKEN` supplies a stable token for local clients. Keep it outside Git.
The server binds loopback only and refuses a second writer on the same directory.
Malformed persisted state fails startup instead of being silently replaced.

After a crash, a writer lock may remain. Confirm that its process is gone and
preserve a copy of the state before moving the stale lock aside. The launcher does
not automatically erase locks or state. This local store is a test fixture, not
Clio history or the planned generic resource repository provider.

## Content operations

The local API supports content creation, content/version reads, distinct language
versions, draft patches, basic content-type and locale definitions, pagination,
ETag conditional reads and writes, and JSON Merge Patch. Creation and mutation
return a body when requested with `Prefer: return=representation`.
The model rejects unsupported and read-only input fields; some vendor endpoints
instead ignore read-only values. Locale validation accepts a conservative subset
of language-tag syntax. These differences are part of the qualification matrix.

```bash
curl -sS http://127.0.0.1:4175/v1/content \
  -H "Authorization: Bearer $BIG_PI_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Prefer: return=representation' \
  -d '{"contentType":"Article","initialVersion":{"displayName":"Technical note","locale":"en","properties":{"Body":"Calibration and adjustment are distinct operations.","PartNumber":"TEST-001"}}}'
```

`GET /v1/content` is a **local discovery extension**, not a documented vendor
collection endpoint. `GET /v1/capabilities` describes local support. Unsupported
routes fail explicitly; an unsupported operation never returns fabricated success.
The content discovery extension's `locales` and `statuses` filters match content
when any one of its versions satisfies both filters. Version listings filter
versions directly. Content-type and locale definition listings support pagination
and reject those version filters. Totals count matching items before pagination.
Publishing is deliberately unavailable in this initial draft workflow. The full
publication and delivery compatibility stories remain in the epic.

Supply `If-Match` with the ETag from an exact version GET before PATCH. A stale
ETag returns 412. Version identifiers are not immutable content hashes: a draft
can change without changing its version identifier. The bridge therefore binds
the exact payload and ETag as well as the key, version and locale.

## MCP and coding agents

The authenticated Streamable HTTP endpoint is **http://127.0.0.1:4175/mcp**.
The stdio bridge connects to that endpoint, so it shares the same persistent
store instead of starting a second writer:

```bash
export BIG_PI_TOKEN='your-local-token'
bash scripts/start-big-pi-mcp.sh
```

The stdio command is for an MCP client; it waits for JSON-RPC on stdin. Do not
expect a human terminal prompt. `BIG_PI_BASE_URL` selects another loopback HTTP
port. Tokens and runtime diagnostics never go to stdout.

Ready-to-merge configuration examples are in `config/`:

- `codex.toml`: Codex's `mcp_servers` table with environment forwarding.
- `claude-code.mcp.json`: Claude Code's `mcpServers` with environment expansion.
- `opencode-v2.json`: OpenCode 2's **`mcp.servers`** structure.

Replace the absolute path and merge the named entry into the desired config.
The examples do not overwrite existing agent settings. Claude Desktop has
different environment expansion rules; launch with the token in its environment
or configure it through its local settings rather than assuming Claude Code's
expansion works there. ChatGPT hosted connectors cannot reach an arbitrary
loopback address; exposing this lab remotely and adding user authentication is
a separate deployment story, not implied by these examples.

Tools are named `big_pi_*` and are local Big Pi tools. MCP transport interoperability
does not imply compatibility with Optimizely's `cms_*`/`graph_*` tools. Listing,
reading and draft operations use the same handlers as the REST API. There is no
MCP publish tool.

## Connect the existing Knoxx CMS

Set `BIG_PI_KNOXX_URL` and `BIG_PI_KNOXX_TOKEN` before starting the server. Use a
local Knoxx URL or an HTTPS endpoint and a token authorized for the relevant
organization. If the integration is unconfigured, bridge operations refuse to run.
Knoxx retains its own authentication, organization scope and current publication
permission checks. The sandbox is a **single-user local integration**; it does not
claim to delegate a separate CMS identity for each MCP client.

Import through `POST /big-pi/import` (or the corresponding MCP tool):

```json
{"key":"content-1","version":"1","field":"Body","source-system":"local-optimizely"}
```

This reads the selected source version and imports the selected plain string field
to `POST /api/cms/documents`, preserving its source coordinate, ETag and complete
payload under `metadata.optimizely`. The payload is a data-only EDN string in
`payload-edn`, tagged with `payload-format: big-pi-edn-v1`. This preserves arbitrary
JSON field names through the current Knoxx HTTP codec, which cannot round-trip
every nested object key. Export decodes and validates that recorded payload before
comparing it with the source. The source title, field and metadata must fit
the current Knoxx CMS contract; unsupported structures are rejected, not flattened.
The local `X-Big-Pi-Source-Id` response header identifies the source store; the
bridge records that trusted identity under `metadata.optimizely.source-id`.
`source-system` is a descriptive caller label. Export refuses a different store
even if the key, version, payload and ETag happen to match. Back up the whole data
directory, including `source-id`. Older provenance without the identity or payload format must be
re-imported before export.

The imported document has internal visibility. To update an existing imported
document, provide its `document-id` and the `expected-revision` you observed.
Updates require the same recorded source identity, payload, ETag, field and label.
They omit metadata from the PATCH so Knoxx retains the parent revision's exact
stored metadata, including keys that its current HTTP codec cannot round-trip.
Changing the recorded source or attaching provenance to an unrelated existing
document is refused with `source-rebind-required`; import into a new document
instead. Updating provenance in place needs a lossless CMS metadata contract and
remains in the provider story. Knoxx can represent concurrent saves as branches;
the bridge reports actual revision heads and conflicts instead of claiming CAS.

After translation or review in the existing workflow, return a specific revision
through `POST /big-pi/export`:

```json
{"document-id":"the-imported-doc-id","revision":"the-exact-knoxx-revision","target-locale":"de","field":"Body"}
```

The bridge reads exact history, requires a sole current unconflicted head, checks
the source-store identity, original vendor payload and ETag, rechecks the Knoxx head, then creates a new
target-language **draft**. Only the selected translated field changes; unrelated
source properties are preserved. CMS annotations and internal metadata are not
copied into vendor properties. A revision selection is not an SME approval receipt.
Mu owns review, acceptance and terminology decisions.

The local bridge uses this lab's Optimizely model as its vendor side. Configuring
a real vendor client and OAuth token refresh remains part of tenant qualification.
There is no cross-system transaction: a source edit can race the final draft write.
The result reports the exact basis; it cannot authorize publication. Network failures
after a write are reported as uncertain and are not automatically retried. Inspect
the destination before issuing another write to avoid duplicate versions.

## Verification

```bash
bash scripts/verify-big-pi-local.sh
npm run test:browser --prefix packages/big-pi-optimizely
```

The package tests include pure behavior tests, injected transport tests, a contract
test against the actual unmodified Knoxx HTTP helpers and CMS body law, and live
local HTTP/MCP tests using the real MCP SDK. The contract test adds the current
repository's `backend/src/cljs` to the NBB test classpath; the package runtime does
not depend on the backend. Bridge transport tests use a separately
implemented Knoxx HTTP fixture. These do not establish that the complete Knoxx
application, FutureSight's tenant, a vendor CMS installation or the entire Optimizely
API passed. See [the verification guide](../../docs/verification/big-pi-optimizely-local.md)
for the browser tour and the outstanding qualification gates.

## Reuse and licensing

New code follows Knoxx's **GPL-3.0-or-later** license. The package reuses NBB, Fastify,
Zod, and the official MCP SDK for runtime/protocol behavior. Their licenses remain
their own; the repository license does not relicense dependencies.

The [research register](../../docs/integrations/optimizely-reuse.md) records the
existing Optimizely SDKs, MCP projects and starter applications inspected. Those
packages either need a real CMS or cover different Optimizely products. No complete
open-source Optimizely CMS server was identified in the bounded search. The lab
fills the local stateful testing gap while preserving separate vendor qualification.
