---
title: Route Taxonomy & Auth Tiers
category: architecture
created: 2026-04-27
status: in-progress
tags: [routes, auth, async, dod]
related: [async/async-refactor-plan.md]
---

# Route Taxonomy & Auth Tiers

All Knoxx HTTP routes are classified into five tiers based on their auth model
and the async shape of their handler.

## Tier 0 — Public / Discovery

| Method | Path | Async Shape | DoD Gap |
|---|---|---|---|
| `GET` | `/.well-known/oauth-authorization-server` | Sync `.send` | ✅ Complete |
| `POST` | `/api/mcp/oauth/register` | `.then/.catch` pyramid | ⚠ Needs `defn-async` flatten |

## Tier 1 — Browser Session (Cookie Auth)

| Method | Path | Auth | DoD Gap |
|---|---|---|---|
| `GET` | `/api/mcp/oauth/authorize` | `require-browser-auth-context!` | ⚠ Needs workflow stages |
| `GET` | `/api/mcp/oauth/authorize/confirm` | same | ✅ Extracted, still `.then` chains |
| `GET` | `/api/mcp/tokens` | same | ⚠ Needs workflow stages |
| `DELETE` | `/api/mcp/tokens/:tokenId` | same | ⚠ Needs workflow stages |

## Tier 2 — Bearer Token (MCP Transport)

| Method | Path | DoD Gap |
|---|---|---|
| `POST` | `/api/mcp/oauth/token` | ⚠ Worst offender — 6-level `.then` pyramid |
| `POST` | `/mcp` | ⚠ Needs `defn-async` + named stages |
| `GET` | `/mcp` | ⚠ No error boundary |
| `DELETE` | `/mcp` | ⚠ No error boundary |

## Tier 3 — Policy Context (`with-request-context!`)

| Method | Path | DoD Gap |
|---|---|---|
| `POST` | `/api/multimodal/upload` | ⚠ `ensure-tool!` not on all routes |
| `GET` | `/api/multimodal/:fileId` | ⚠ same |
| `GET` | `/api/voice/stt/health` | ✅ Migrated |
| `POST` | `/api/voice/stt` | ✅ Migrated |
| `POST` | `/api/voice/tts` | ⚠ Needs `defn-async` flatten |

## Tier 4 — Proxy / Admin

| Method | Path | DoD Gap |
|---|---|---|
| `GET` | `/api/admin/pi-sessions/status` | ⚠ Needs error boundary |
| `GET` | `/api/admin/pi-sessions` | ⚠ Mixed try/catch style |
| `POST` | `/api/admin/pi-sessions/ingest` | ⚠ Needs workflow stages |
| `ALL` | `/api/ingestion/*` | ⚠ No typed schema |
| `ALL` | `/api/openplanner/*` | ⚠ No typed schema |

## Canonical Route DoD

Every Knoxx route must satisfy five contracts:

```clojurescript
;; 1. GUARD — fail fast, no nesting
(defn-async my-route [deps ctx body req reply]
  (let [r   (or (require-redis! reply) (return! nil))
        ctx (or (with-request-context! ...) (return! nil))
        _   (ensure-tool! ctx "tool.id")]

;; 2. PARSE — schema-decoded body, no raw aget in handler
    body (u/decode MyBodySchema (aget req "body"))

;; 3. WORKFLOW — named stages, no inline .then pyramids
    result (<! (load-thing! r params))
    result (<! (validate-thing! result))
    result (<! (persist-thing! r result))

;; 4. RESPOND — single terminal send
    _ (json-send! reply 200 result)))
```

## Desired Route Shape (Macro Form)

```clojure
;; Tier 0
(defroute app "GET" "/.well-known/oauth-authorization-server" nil discovery-metadata!)

;; Tier 1
(defroute app "GET" "/api/mcp/tokens" [:redis :browser-auth] list-user-tokens!)

;; Tier 2
(defroute app "POST" "/mcp" [:redis :bearer-token] handle-mcp-post!)

;; Tier 3
(defroute app "POST" "/api/voice/tts" [:ctx "multimodal.upload"] tts-synthesize!)

;; Tier 4
(defroute app "ALL" "/api/ingestion/*" [:proxy :ingestion] proxy-to!)
```
