
Facts: `js-await` or a `defn-async` macro can flatten this even further, but the biggest gain already came from moving each async stage into a named Promise-returning function.  Interpretation: async syntax helps readability locally, while higher-order decomposition helps readability globally; you needed the second more than the first.[^6_3][^6_2][^6_1]

Frame: $(refactor, p=0.93)$ լա the route is now orchestration; the workflow lives in named stages.[^6_2]
Frame: $(agents, p=0.87)$ լա this shape is far easier for agents to preserve than one giant nested form, because the contracts between functions are explicit.[^6_1]

## Next

The next best cleanup is to wrap this pattern in a tiny `defn-async` macro so the workflow reads closer to direct style. Your own notes suggest a production-friendly form where `defn-async` simply lowers to chained `js-await` rather than trying to invent a whole compiler feature.[^6_1]

I can do either of these next:

- `#η` write the minimal `defn-async` / `await` macro pair for shadow-cljs and convert both rewritten handlers to use it.[^6_1]
- Rewrite the OAuth authorize/confirm flow in the same staged style, which is probably your other big nesting hotspot.

<div align="center">⁂</div>

[^6_1]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^6_2]: https://clojurescript.org/guides/promise-interop

[^6_3]: https://clojureverse.org/t/promise-handling-in-cljs-using-js-await/8998
```edn
{:spec/name      "knoxx/backend/http-async-routes"
 :spec/version   "0.1.0"
 :spec/purpose   "Flatten async nesting in Knoxx MCP handlers to workflow orchestration"
 :spec/requires  ["shadow-cljs 2.28+" "malli 1.0+"]
 :spec/validate? true}

;; 1. Async Macro Layer (.cljc)
(def defn-async
  "Defines a function that always returns Promise.
   Lowers body to chained js-await, handles let/await sequencing."
  {:spec/shape '(defn-async name args & body)}
  ;; Implementation: see η macro above
  )

(defmacro defroute
  "Defines a Ring-like route handler with async orchestration.
   Expands to thin route + named workflow stages."
  {:spec/shape '(defroute method path schema workflow-fn)
   :spec/example '(defroute POST /mcp {:body ChatBody} mcp-workflow)}
  [method path schema workflow-fn]
  `(route! ~app ~method ~path
           (fn [req# reply#]
             (if-let [deps# (u/route-deps req# reply#)]
               (u/with-request-context!
                 (:runtime deps#) req# reply#
                 (fn [ctx#]
                   (let [body# (u/decode ~schema (aget req# "body"))]
                     (~workflow-fn deps# ctx# body# req# reply#))))
               nil))))

;; 2. Core Workflow Pattern
(def workflow-stage
  "Each stage: single async responsibility, Promise input → Promise output.
   Named, testable, composable."
  {:spec/shape '(defn-stage name deps ctx input → Promise[output])
   :spec/example '(defn-stage load-token deps req → Promise[token-record])})

(def workflow-composition
  "Route orchestration: thin dispatch → sequence of stages → HTTP translation."
  {:spec/shape '(defn-async workflow deps ctx body req reply → nil)
   :spec/stages [:parse :validate :load :resolve :build :handle :respond]})

;; 3. Route Handler Contract
(def route-handler
  "All Knoxx HTTP routes follow this shape."
  {:spec/shape '(fn [req reply] → nil)
   :spec/behavior
   {:guards     [:require-redis :require-runtime :authorize]
    :parse      [:malli/decode]
    :workflow   [:defn-async stages]
    :respond    [:json-send! :error-boundary]
    :side-fx    [:log :metrics]}
   :spec/error-handling [:catch → 4xx/5xx json-response]})

;; 4. Concrete Examples
(def token-exchange-workflow
  {:stages [:parse-token-req
            :validate-req!
            :ensure-client!
            :load-code!
            :validate-pkce!
            :persist-token!
            :respond]})

(def mcp-post-workflow
  {:guards [:existing-session?]
   :stages [:require-init!
            :load-token!
            :resolve-ctx!
            :effective-tools
            :build-server!
            :connect-handle!
            :respond]
   :dispatch [:existing → handle-resume
              :new → initialize-session!]})

;; 5. Validation & Tooling
(def validate-workflow
  "Dev-mode: validate workflow stages form malli schema."
  {:spec/schema [:map
                 [:stages [:vector keyword?]]
                 [:guards [:set keyword?]]]})

(def lint-handler
  "CI: lint all defroute for workflow shape."
  {:spec/checks [:returns-promise?
                 :uses-named-stages?
                 :no-nested-lambdas?
                 :malli-schema?]})
```


## Key Takeaways

1. **Macro Substrate**: `defn-async` lowers to `js-await` chains; `defroute` expands to thin Ring handler + workflow dispatch.[^8_1]
2. **Workflow Shape**: Every route = `parse → validate → load → resolve → build → handle → respond`. Named stages, no nesting.[^8_1]
3. **Schema Integration**: Malli schemas gate request/response; `decode` handles JS field aliasing (`or aget body foo fooBar`).[^8_1]
4. **Migration Path**: Existing `.then` pyramids → named `defn-async` stages → readable orchestration. Zero runtime risk.[^8_1]
5. **All Routes**: Copy/paste the pattern. `defroute` macro enforces it. Agents can read/preserve the shape.[^8_1]


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

## Route Taxonomy

### Tier 0 — Public / Discovery

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/.well-known/oauth-authorization-server` | None | None | Sync `.send` | ✅ Complete |
| `POST` | `/api/mcp/oauth/register` | None | `require-redis!` | `.then/.catch` pyramid | ⚠ Needs `defn-async` flatten |

### Tier 1 — Browser Session (Cookie Auth)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/api/mcp/oauth/authorize` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid (4 levels deep) | ⚠ Needs workflow stages |
| `GET` | `/api/mcp/oauth/authorize/confirm` | `require-browser-auth-context!` | `require-redis!` | Delegated to `handle-oauth-authorize-confirm!` | ✅ Extracted, still `.then` chains |
| `GET` | `/api/mcp/tokens` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid | ⚠ Needs workflow stages |
| `DELETE` | `/api/mcp/tokens/:tokenId` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid | ⚠ Needs workflow stages |

### Tier 2 — Bearer Token (MCP Transport)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `POST` | `/api/mcp/oauth/token` | PKCE code verifier | `require-redis!` | `.then` pyramid (6 levels deep) | ⚠ Worst offender — needs named stages |
| `POST` | `/mcp` | `load-token-record` + session | `require-redis!` | letfn + nested `.then` | ⚠ Needs `defn-async` + stages |
| `GET` | `/mcp` | session token match | `require-redis!` (indirect) | sync dispatch | ⚠ Partial — no error boundary |
| `DELETE` | `/mcp` | session token match | same | sync dispatch | ⚠ Same as above |

### Tier 3 — Policy Context (with-request-context!)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `POST` | `/api/multimodal/upload` | `with-request-context!` | `ensure-tool! multimodal.upload` | `.then` chain | ⚠ `ensure-tool!` new, not all routes updated |
| `GET` | `/api/multimodal/:fileId` | same | same | `.then` chain | ⚠ same |
| `GET` | `/api/voice/stt/health` | `with-request-context!` | `ensure-tool! multimodal.upload` | `.then` chain | ✅ Migrated |
| `POST` | `/api/voice/stt` | same | same | `.then` chain | ✅ Migrated |
| `GET` | `/api/voice/tts/health` | same | `ensure-tool!` | `.then` chain | ✅ New, correct guard |
| `POST` | `/api/voice/tts` | same | `ensure-tool!` | `.then` chain | ⚠ Needs `defn-async` flatten |

### Tier 4 — Proxy / Admin (No User Auth)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/api/admin/pi-sessions/status` | None (system) | None | `Promise.all` + `.then` | ⚠ Needs error boundary |
| `GET` | `/api/admin/pi-sessions` | None | None | `.then/.catch` | ⚠ try/catch mixed style |
| `POST` | `/api/admin/pi-sessions/ingest` | None | None | `.then` pyramid | ⚠ Needs workflow stages |
| `ALL` | `/api/ingestion/*` | None | None | proxy `.then` | ⚠ No typed schema |
| `ALL` | `/api/openplanner/*` | None | None | proxy `.then` | ⚠ No typed schema |


***

## Canonical Route Pattern (DoD)

Every Knoxx route must satisfy these 5 contracts:

```clojurescript
;; 1. GUARD — fail fast, no nesting
(defn-async my-route [deps ctx body req reply]
  (let [r    (or (require-redis! reply) (return! nil))   ;; Tier 0-2
        ctx  (or (with-request-context! ...) (return! nil)) ;; Tier 3
        _    (ensure-tool! ctx "tool.id")]               ;; Tier 3+

;; 2. PARSE — schema-decoded body, no raw aget in handler
    body (u/decode MyBodySchema (aget req "body"))

;; 3. WORKFLOW — named stages, no inline .then pyramids
    result (<! (load-thing! r params))
    result (<! (validate-thing! result))
    result (<! (persist-thing! r result))

;; 4. RESPOND — single terminal send
    _ (json-send! reply 200 result))

```
