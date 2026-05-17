---
title: "defroute Macro — Target Form"
category: async
created: 2026-04-21
original: 2026.04.21.16.13.05.md
status: note
---

## The `defroute` Macro (What All of the Above Collapses Into)

```clojure
;; Desired: every route above becomes one of these four forms

;; Tier 0 – public
(defroute app "GET" "/.well-known/oauth-authorization-server" nil
  discovery-metadata!)

;; Tier 1 – browser session
(defroute app "GET" "/api/mcp/tokens" [:redis :browser-auth]
  list-user-tokens!)

;; Tier 2 – bearer token
(defroute app "POST" "/mcp" [:redis :bearer-token]
  handle-mcp-post!)

;; Tier 3 – policy context
(defroute app "POST" "/api/voice/tts" [:ctx "multimodal.upload"]
  tts-synthesize!)

;; Tier 4 – proxy
(defroute app "ALL" "/api/ingestion/*" [:proxy :ingestion]
  proxy-to!)
```

The guard vector `[:redis :browser-auth]` is the entire declarative DoD.  The macro expands it to the appropriate `require-redis!` / `require-browser-auth-context!` / `ensure-tool!` chain, and the workflow fn (`tts-synthesize!`, etc.) is always a named `defn-async` with no inline `.then` chains. That's the full contract.
