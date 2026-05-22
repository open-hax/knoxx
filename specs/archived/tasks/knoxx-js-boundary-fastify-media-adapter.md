# Knoxx JS Boundary — Fastify, Voice, and Multimodal Media Adapter

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Story points: 8

## Purpose

Move Fastify request/reply, multipart upload parts, websocket send/close, `Object.keys`, request forwarding, and route-level JSON construction behind named adapters. Routes should express authorization and domain orchestration in CLJS data, not inspect raw JS request objects at every step.

## Problem

The current route/HTTP namespaces contain raw JS boundary handling:

- `backend/src/cljs/knoxx/backend/infra/http.cljs`
  - imports `extern.js`
  - reads request body/query/headers with `aget`
  - uses `Object.keys` through `xjs/js-array-seq`
  - builds `#js` `RequestInit` fragments
  - stringifies fallback request bodies with `js/JSON`
  - assigns objects with `js/Object.assign`
- `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs`
  - imports `extern.js`
  - parses multipart parts arrays
  - serializes websocket payloads with `js/JSON` / `clj->js`
  - reads voice gateway events via `aget` and `js->clj`
  - builds Fastify websocket route options with `clj->js`
  - builds TTS JSON payloads in-route
- `backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs`
  - imports `extern.js`
  - parses multipart parts arrays
  - converts `Promise.all` results and `fs.readdir` arrays via `xjs/js-array-seq`
  - directly handles Node/Fastify file/reply objects

These are legitimate system boundaries, but they are not isolated. The same route functions that enforce tools and shape domain responses also decide how to traverse JS request internals.

## Target model

Add named adapters such as:

```clojure
(fastify/request-body request)           ;; => CLJS map
(fastify/request-query request)          ;; => CLJS map or query vector
(fastify/request-headers request)        ;; => CLJS map
(fastify/send-json! reply status body)   ;; CLJS body in, JS send inside
(fastify/route-options route-map)        ;; Fastify JS options inside adapter

(multipart/parts! request)               ;; Promise<vec<part-map>>
(multipart/file-buffer! part)            ;; Promise<Buffer>
(websocket/send-json! socket payload)    ;; CLJS payload in
(fetch-forward/request-init request extra);; JS RequestInit inside adapter only
```

Route code should receive CLJS maps/vectors and only pass opaque handles to adapters when unavoidable.

## Deliverables

1. Create or extend Fastify adapter namespace for:
   - JSON reply send
   - request body/query/header extraction
   - params extraction
   - route option construction
   - reply header/send helpers
2. Create multipart adapter for:
   - `.parts` consumption
   - native file part normalization
   - file buffer extraction
   - part metadata CLJS maps
3. Create websocket adapter for:
   - ready-state check
   - JSON send
   - close
   - event subscription
4. Refactor `infra/http.cljs` request forwarding so raw request inspection and `RequestInit` creation are adapter-owned.
5. Refactor `infra/routes/voice.cljs` so:
   - STT file extraction uses multipart adapter
   - TTS body parsing uses CLJS request body map
   - voice gateway request JSON uses fetch JSON request mode
   - websocket relay normalizes provider events once
6. Refactor `infra/routes/multimodal.cljs` so:
   - upload parts are CLJS part maps
   - `Promise.all` upload results are CLJS vectors
   - `fs.readdir` arrays are normalized at a Node/fs adapter boundary
7. Add regression tests for Fastify extraction and multipart normalization with stub JS objects.

## Migration notes

1. `json-response!` already centralizes some reply behavior in `infra/http.cljs`; it should either move to the adapter or become a thin wrapper around it.
2. Keep file buffers/streams opaque, but do not inspect them in route orchestration code.
3. Route API shapes must not change.
4. This child may need to coordinate with existing `domain/node/fs` and `domain/node/path` adoption from `docs/notes/architecture/js-cljs-boundary-report.md`.

## Acceptance criteria

1. `infra/http.cljs`, `infra/routes/voice.cljs`, and `infra/routes/multimodal.cljs` no longer import `knoxx.backend.extern.js`.
2. Those files no longer call `xjs/js-array-seq`.
3. Raw `Object.keys`, `#js` `RequestInit`, websocket JSON serialization, and multipart `.parts` traversal are isolated in adapters.
4. Route functions operate primarily on CLJS maps/vectors and opaque handles passed to adapters.
5. Existing voice/STT/TTS and multimodal route responses are preserved for fixtures.
6. Backend test build reports zero failures and zero errors.

## Verification

```bash
rg -n "knoxx\.backend\.extern\.js|xjs/js-array-seq|Object\.keys|js/Object|#js|clj->js|js->clj|\.stringify js/JSON|aget" backend/src/cljs/knoxx/backend/infra/http.cljs backend/src/cljs/knoxx/backend/infra/routes/voice.cljs backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs
pnpm -C backend boundary:check
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Latest verification (2026-05-21):

- Target grep returned no matches for generic JS boundary forms in `infra/http.cljs`, `infra/routes/voice.cljs`, and `infra/routes/multimodal.cljs`.
- `pnpm -C backend boundary:check` passed with 5 remaining allow-listed non-extern generic extern import files.
- `pnpm -C backend exec shadow-cljs compile test` passed: 355 tests, 941 assertions, 0 failures, 0 errors.
- `pnpm -C backend exec shadow-cljs compile server` exited 0 with existing warnings.
- `git diff --check` passed for touched Fastify/media adapter files, tests, specs, inventory, and allow-list.
