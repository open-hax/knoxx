# Knoxx JS/CLJS Boundary Hardening Epic

Date: 2026-05-21
Status: draft
Source reports:

- `docs/notes/architecture/js-cljs-boundary-report.md`
- `docs/notes/architecture/js-cljs-boundary-domain-plan.md`
- User scan of `xjs` / `xjson` imports and calls in backend CLJS

Story points: 34

## Purpose

Remove the current “extern alias” loophole where non-extern namespaces can plausibly claim boundary enforcement while still creating, reading, stringifying, or sequencing raw JavaScript values.

The target rule is simple:

> Outside `knoxx.backend.extern.*` and explicitly approved low-level wrappers, Knoxx backend code operates on CLJS maps, vectors, scalars, and documented opaque handles only. JS objects are born, decoded, encoded, and consumed at named boundary adapters.

## Problem

The current `knoxx.backend.extern.js` and `knoxx.backend.extern.json` helpers are being imported by domain and infra namespaces. That made some raw interop easier to grep, but it did not create a real boundary. It only moved `js->clj`, `clj->js`, `JSON.stringify`, `Object.keys`, and JS array handling behind generic aliases.

This is a smell because:

1. A call site that invokes `xjson/stringify` still decides when a CLJS map becomes wire JSON.
2. A call site that invokes `xjs/js-array-seq` still accepts raw JS array provenance.
3. A call site that builds `#js` params or reads via `aget` still couples business logic to provider/runtime object shape.
4. Generic `extern.js` / `extern.json` helpers hide the specific boundary being crossed.
5. The actual origin of the JS object is often several functions away from the site that consumes it.

## Initial smell inventory from request

| Area | Files | Boundary leak |
|---|---|---|
| Discord domain | `backend/src/cljs/knoxx/backend/domain/discord/tools.cljs`, `domain/discord/rest_client.cljs` | Tool params, Discord REST JSON, multipart `FormData`/`Blob`, `Promise.all` result arrays, wrapper tool `#js` params. |
| Shared tool factory | `backend/src/cljs/knoxx/backend/domain/tools.cljs` | Tool definition JS object construction, update callback payloads, custom tool mutation, prompt guideline JS arrays. |
| Voice domain/routes | `backend/src/cljs/knoxx/backend/domain/voice/client.cljs`, `infra/routes/voice.cljs` | TTS/STT JSON parse/stringify, multipart parts arrays, websocket JSON payloads, Fastify request body JS access. |
| HTTP/Fastify infra | `backend/src/cljs/knoxx/backend/infra/http.cljs` | Request query/header `Object.keys`, raw request body forwarding, `RequestInit` JS construction, response body conversion. |
| Stores/memory | `backend/src/cljs/knoxx/backend/infra/core_memory.cljs`, `infra/stores/session_titles.cljs` | JSON row parsing and `Promise.all` result arrays hidden behind generic helpers. |
| Service clients | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs`, `infra/clients/knoxx_control.cljs`, `infra/clients/proxx.cljs`, `domain/bluesky/client.cljs`, `domain/contracts/client.cljs` | Each client decides its own JSON body encoding rather than passing CLJS payloads to a transport codec. |
| Multimodal routes | `backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs` | Multipart parts, upload result arrays, `fs.readdir` arrays, raw reply/file JS handling. |

A refreshed inventory should be the first implementation step because adjacent direct `aget`, `js->clj`, `clj->js`, `#js`, `js/JSON`, `js/Promise.all`, and `js/Object` usages in these files are part of the same boundary leak even when they do not import `xjs` or `xjson`.

## Goals

1. Replace generic extern alias use with domain-specific boundary adapters.
2. Make the origin and sink of every JS object explicit.
3. Ensure domain/tool/client code receives CLJS request maps and returns CLJS result maps.
4. Move JSON encoding/decoding into transport/client boundary namespaces.
5. Move Fastify, multipart, websocket, `Object.keys`, `Promise.all`, and `FormData`/`Blob` handling into explicit extern adapters.
6. Add verification gates so new non-extern `xjs`/`xjson` imports and generic JS data conversions do not return.
7. Preserve behavior during migration; this is a boundary refactor, not an API redesign.

## Non-goals

1. Rewriting every backend `aget` / `#js` occurrence in one sprint.
2. Changing public route shapes, Discord tool schemas, Voice/STT/TTS API behavior, or OpenPlanner/Proxx payload formats.
3. Strictly closing every third-party provider schema beyond fields Knoxx consumes.
4. Restarting PM2 processes.
5. Moving domain logic into extern namespaces. Extern owns conversion and native API calls only.

## Boundary law

1. Non-extern namespaces must not require `knoxx.backend.extern.js` or `knoxx.backend.extern.json` directly.
2. Non-extern namespaces must not call `js->clj`, `clj->js`, `js/JSON.parse`, `js/JSON.stringify`, `Object.keys`, or `array-seq` on provider/runtime values except in approved adapters being actively migrated.
3. Boundary adapters must be named by the boundary they own, not by generic mechanics:
   - good: `extern.fastify/request-body`, `extern.multipart/parts!`, `extern.discord/message-form`, `extern.fetch/json-request!`
   - bad: `extern.js/object`, `extern.json/stringify` imported everywhere
4. Boundary adapters accept CLJS data whenever possible and return CLJS data immediately after reading JS/provider output.
5. Opaque JS handles that cannot be converted cheaply, such as buffers, streams, native `Response`, or `FormData`, must be carried only through typed adapter APIs with no external inspection or mutation.
6. Every migrated call site gets a narrow regression fixture proving the same CLJS input produces the same wire/request/response behavior.

## Child specs

| Child spec | Points | Status | Scope |
|---|---:|---|---|
| `knoxx-js-boundary-inventory-law.md` | 3 | done | Baseline inventory, allowed-boundary policy, CI/grep gate, migration checklist. |
| `knoxx-js-boundary-json-fetch-clients.md` | 5 | done | Move JSON body encoding out of service/domain clients into fetch/transport adapters. |
| `knoxx-js-boundary-tool-runtime-adapter.md` | 5 | done | Tool definition construction, tool params normalization, update callback payloads, custom tool sanitation. |
| `knoxx-js-boundary-discord-adapter.md` | 8 | done | Discord REST, multipart upload, tool params, guild/channel/message result normalization. |
| `knoxx-js-boundary-fastify-media-adapter.md` | 8 | done | Fastify request/reply, query/header/body forwarding, multipart parts, websocket sends, multimodal file route arrays. |
| `knoxx-js-boundary-stores-memory-codecs.md` | 5 | done | JSON row codecs and promise array/result normalization in session titles and core memory. |

## Suggested sequence

1. Inventory/law gate first, so remaining work is measurable and new leaks are blocked.
2. JSON/fetch clients next, because it removes the `xjson/stringify` pattern across many small clients with low behavior risk.
3. Tool runtime adapter before Discord, because Discord tools currently inherit generic tool JS param/update conventions.
4. Discord adapter, because it has the most domain-specific JS boundary leaks and should not be “fixed” with another generic helper.
5. Fastify/media adapter, because routes touch many native objects and need a careful compatibility layer.
6. Stores/memory codecs last or in parallel, because they are smaller but should reuse the same JSON/promise boundary law.

## Acceptance criteria

1. `rg 'knoxx\.backend\.extern\.(js|json)' backend/src/cljs/knoxx/backend --glob '!extern/**'` returns zero non-extern imports, except documented temporary allow-list entries in the active child spec.
2. Listed files no longer call `xjs/js-array-seq` or `xjson/stringify` / `xjson/parse-object` directly.
3. For migrated call sites, business/domain functions accept CLJS maps/vectors and do not inspect raw JS objects.
4. Native JS object construction is contained in named extern adapters that correspond to real system boundaries.
5. Representative tests cover JSON request encoding, multipart upload shape, tool param normalization, and Fastify request extraction.
6. Backend test build reports zero failures and zero errors after each implementation child that touches CLJS code.

## Verification

For documentation-only edits:

```bash
rg -n "knoxx-js-boundary" specs
```

For each implementation child touching backend CLJS:

```bash
pnpm -C backend exec shadow-cljs compile test
```

For production backend behavior changes, also run:

```bash
pnpm -C backend exec shadow-cljs compile server
```
