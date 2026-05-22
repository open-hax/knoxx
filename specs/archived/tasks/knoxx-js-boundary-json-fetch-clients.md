# Knoxx JS Boundary — JSON Fetch Clients

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Story points: 5

## Purpose

Stop service/domain clients from deciding when CLJS payloads become JSON strings. JSON request encoding and response decoding should be owned by the transport boundary, not by each client namespace importing `xjson`.

## Problem

These clients still call `xjson/stringify` directly:

- `backend/src/cljs/knoxx/backend/domain/discord/rest_client.cljs`
- `backend/src/cljs/knoxx/backend/domain/voice/client.cljs`
- `backend/src/cljs/knoxx/backend/domain/contracts/client.cljs`
- `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs`
- `backend/src/cljs/knoxx/backend/domain/bluesky/client.cljs`
- `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs`
- `backend/src/cljs/knoxx/backend/infra/clients/proxx.cljs`

This makes each client a partial JS/JSON boundary. It also duplicates decisions about `Content-Type`, body encoding, logging, and non-map bodies.

## Target model

Clients pass CLJS request data to a transport adapter:

```clojure
(xfetch/json-request! client
  {:url url
   :method "POST"
   :headers headers
   :json payload
   :timeout-ms 30000})
```

The adapter owns:

- `Content-Type` defaults
- JSON stringify
- JSON response parse
- text fallback for invalid JSON
- native `RequestInit` construction
- preserving opaque binary/stream bodies when explicitly requested

## Deliverables

1. Extend or replace `knoxx.backend.extern.fetch` with explicit request modes:
   - `:json` for CLJS JSON bodies
   - `:body` for already-opaque native/binary bodies
   - `:form` only when a boundary adapter has prepared a native `FormData`
2. Remove direct `xjson/stringify` imports from the listed clients.
3. Replace ad hoc `stringify-body` in `infra/clients/proxx.cljs` with a transport-level decision.
4. Preserve client protocols and public function names.
5. Add tests for request construction using a stub `fetch-fn`:
   - CLJS map payload becomes JSON body once
   - nil body omits body
   - binary body is not JSON-stringified
   - non-2xx responses still surface CLJS response bodies

## File-specific notes

### `domain/discord/rest_client.cljs`

Move Discord JSON encoding into the Discord adapter or transport request mode. Multipart `form-data` remains out of scope here and is handled by `knoxx-js-boundary-discord-adapter.md`.

### `domain/voice/client.cljs`

TTS request bodies should use `:json`. STT audio buffers stay opaque `:body` values.

### `domain/contracts/client.cljs`

`validate-contract!` should pass `{:edn_text ... :contract_class ...}` as `:json`.

### `infra/clients/openplanner.cljs`

OpenPlanner request payloads should be CLJS maps until transport.

### `domain/bluesky/client.cljs`

ATProto JSON XRPC requests should use `:json`. Blob upload should keep binary `:body`.

### `infra/clients/knoxx_control.cljs`

Self-API control requests should use `:json`, while forwarding remains in the Fastify/streaming adapter child.

### `infra/clients/proxx.cljs`

Chat/completions and embedding request maps should use `:json`. Streaming response calls may still request native response objects, but body encoding belongs to the adapter.

## Implementation results

Implemented 2026-05-21.

Changed:

- `backend/src/cljs/knoxx/backend/extern/fetch.cljs`
  - Added explicit `:json`, `:body`, and `:form` request modes through the fetch boundary.
  - Added default JSON `Content-Type` insertion when `:json` is present.
  - Kept compatibility for existing `:opts` maps and native JS `RequestInit` objects.
  - Added `parse-json-object` for boundary-owned JSON object parsing needed by STT text/event-stream parsing.
- Removed direct `knoxx.backend.extern.json` imports and request-body `xjson/stringify` calls from:
  - `domain/discord/rest_client.cljs`
  - `domain/voice/client.cljs`
  - `domain/contracts/client.cljs`
  - `infra/clients/openplanner.cljs`
  - `domain/bluesky/client.cljs`
  - `infra/clients/knoxx_control.cljs`
  - `infra/clients/proxx.cljs`
- Removed `infra/clients/proxx.cljs` ad hoc `stringify-body` fallback.
- Added transport tests in `backend/test/cljs/knoxx/backend/extern_fetch_test.cljs` for JSON encoding, nil body omission, opaque body preservation, and non-2xx JSON body decoding.
- Regenerated `reports/js-boundary-inventory.md` and tightened `backend/config/js-boundary-allowlist.json` from 17 to 10 active non-extern generic extern import files.

Verification status:

- Passed `pnpm -C backend boundary:check`.
- Passed target grep: no listed JSON client imports `knoxx.backend.extern.json`, calls `xjson/stringify`, calls `.stringify js/JSON`, or calls `clj->js` for JSON request bodies.
- Passed `pnpm -C backend exec shadow-cljs compile test` with 333 tests, 839 assertions, 0 failures, 0 errors, and existing warnings.
- Passed `pnpm -C backend exec shadow-cljs compile server` with existing warnings.

Follow-up fixes needed to unblock the full suite:

- `backend/test/cljs/knoxx/backend/agents/session_registry_provider_test.cljs` now uses a sweep timestamp that matches the test intent: only `conv-2` is older than the 100ms TTL.
- `backend/src/cljs/knoxx/backend/infra/agent/stream/provider_events.cljs` now falls back to a `pr-str` preview of normalized tool args when tool-specific preview helpers cannot render generic provider JS args.

## Acceptance criteria

1. No listed client imports `knoxx.backend.extern.json`.
2. No listed client calls `xjson/stringify`, `.stringify js/JSON`, or `clj->js` for JSON request bodies.
3. Transport tests prove JSON encoding happens exactly at the fetch boundary.
4. Existing client protocols and return CLJS shapes are preserved.
5. Backend test build reports zero failures and zero errors.

## Verification

```bash
rg -n "knoxx\.backend\.extern\.json|xjson/stringify|\.stringify js/JSON" backend/src/cljs/knoxx/backend/domain/discord/rest_client.cljs backend/src/cljs/knoxx/backend/domain/voice/client.cljs backend/src/cljs/knoxx/backend/domain/contracts/client.cljs backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs backend/src/cljs/knoxx/backend/domain/bluesky/client.cljs backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs backend/src/cljs/knoxx/backend/infra/clients/proxx.cljs
pnpm -C backend exec shadow-cljs compile test
```
