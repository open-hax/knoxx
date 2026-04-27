---
title: "Sync Code Audit — Init vs Runtime Blocking"
category: async
created: 2026-04-27
original: 2026.04.27.13.54.09.md
status: note
---


Here is the full classification. Every hit was examined against what the code does and when it runs. 

***

## ✅ Block 1 — Initialization Logic (synchronous is acceptable)

These execute at startup, during module load, or before the server accepts requests. Blocking the event loop here is tolerable because nothing is serving traffic yet.

### Filesystem reads for config, policy, contracts, models
Files: `policy_db.cljs`, `tools/policies.cljs`, `contracts/loader.cljs`, `contracts/resolve.cljs`, `contracts/roles.cljs`, `runtime/models.cljs`, `triggers/control_config.cljs`, `triggers/trigger_runner.cljs` 

```clojure
;; policy_db.cljs — reading persisted contract files at boot
(reader/read-string (str (.readFileSync fs file-path "utf8")))
(.readdirSync fs actor-dir)
(.mkdirSync fs dir #js {:recursive true})

;; contracts/loader.cljs — scanning contract directories
(->> (.readdirSync node-fs dir) ...)
(when (.existsSync node-fs candidate) ...)

;; runtime/models.cljs — model config load
(some-> (.readFileSync fs file-path "utf8") str reader/read-string)
```

These are all one-time cold-path reads. The correct pattern is to call them from an `-init!` or `start!` function invoked by `bootstrap.cljs`, not lazily from request handlers. They are **fine as-is** as long as they are not re-called on every request.

### `session_mycology.cljs` — spore/skill scaffolding
```clojure
;; lines 302–307: write skill/contract files when first creating a new spore
(node-fs.mkdirSync dir #js {:recursive true})
(node-fs.writeFileSync skill-path (build-live-skill spore) "utf8")
(node-fs.writeFileSync contract-path (build-live-contract spore) "utf8")
```
This is a setup/provisioning action invoked by the admin on first use, not per request. Acceptable. 

### Crypto setup in `auth/session.cljs`
```clojure
;; lines 26, 45 — AES-GCM cipher/decipher construction
cipher   (.createCipheriv crypto "aes-256-gcm" key-buf iv)
decipher (.createDecipheriv crypto "aes-256-gcm" key-buf iv)
```
`createCipheriv` / `createDecipheriv` are synchronous but CPU-local and microsecond-scale. These are fine at call-site (they don't do I/O). 

### `extension_runtime.cljs` — command handler registry
```clojure
(swap! command-handlers* assoc ...)  ;; line 108
(swap! command-handlers* dissoc ...) ;; line 113
```
Pure in-memory atom mutation used to register/deregister command handlers — this is startup/teardown registration, not hot path. 

***

## 🚨 Block 2 — Runtime Logic (synchronous is not OK)

These execute inside request handlers, per-message loops, or on-demand tool invocations where they block the Node.js event loop.

### `event_agents.cljs` — `execSync` inside a request handler
This is the most severe single instance :
```clojure
;; lines 379, 383, 388 — synchronous child_process exec at RUNTIME
result (js/require "child_process")   ;; runtime require
(.execSync result ...)                ;; blocks entire event loop
(.execSync result (str "curl -sL -o " ...) #js {:timeout 10000})
```
A 10-second synchronous `execSync` will freeze all concurrent requests. Replace with `p/let` + `child_process.exec` via promesa wrapper:
```clojure
(defn exec! [cmd]
  (p/create (fn [resolve reject]
    (let [cp (js/require "child_process")]
      (.exec cp cmd (fn [err stdout] (if err (reject err) (resolve stdout))))))))

(p/let [out (exec! (str "curl -sL -o " ...))]
  out)
```

### `session_mycology.cljs` — sync FS reads inside runtime tool calls
```clojure
;; line 150–153 — reads inside tool invocation path
(if-not (node-fs.existsSync file-path)
  ...
  (let [text (node-fs.readFileSync file-path "utf8")
        ...]))
;; line 146 — appendFileSync in append tool
(node-fs.appendFileSync file-path (str (js/JSON.stringify value) "\n") "utf8")
```
These run when a tool call is dispatched at request time. Replace with `fs.promises.readFile` / `appendFile` wrapped in `p/let`. 

### `policy_db.cljs` — `writeFileSync` inside mutations
```clojure
;; line 124 — writing policy files during a write operation
(.writeFileSync fs file-path (str (pr-str contract) "\n") "utf8")
```
This runs when a policy is saved, which is triggered by an API call. Switch to `fs.promises.writeFile`. 

### `openplanner_memory.cljs` — `js/JSON.parse(JSON.stringify(...))` as clj→js coercion
```clojure
;; lines 265–267 — used to coerce into JS-safe arrays for request body
(js/JSON.parse (js/JSON.stringify (clj->js (str/split node-type #","))))
```
This is a `clj->js` roundtrip done via JSON serialization, which is both CPU-wasteful and happens in a hot network path. Replace with `(clj->js [...] :keyword-fn name)` or `(into-array [...])`. 

### `redis_client.cljs` — serialize/deserialize on every op
```clojure
;; lines 24, 31, 120, 134, 194, 221 — JSON in every Redis get/set
(js/JSON.stringify (clj->js value))
(js->clj (js/JSON.parse value) :keywordize-keys true)
```
Each Redis read/write does a synchronous JSON round-trip in the calling fiber. Not catastrophic since it's CPU-only, but it is inline in async `.then` callbacks meaning it stalls continuation delivery. This is fine to leave for now but worth replacing with `transit` or a lazy decode if Redis ops become a throughput bottleneck. 

### `app_routes.cljs` / `tools/proxy_routes.cljs` — `JSON.stringify` in request body construction
```clojure
;; Multiple lines — constructing fetch body in route handlers
:body (js/JSON.stringify (or body #js {}))
```
This is synchronous inside route handlers. Again CPU-only and fast at typical payload sizes, but should be consistent — either always use a helper or accept that it is acceptable overhead. 

***

## Summary Frame

| Block | Files | Action |
|---|---|---|
| ✅ Init | `contracts/`, `runtime/models`, `policy_db` FS reads, `triggers/`, `auth/session` crypto | Audit call site — ensure they are not called per-request; otherwise fine |
| 🚨 Runtime | `event_agents` `execSync`, `session_mycology` FS r/w in tool calls, `policy_db` `writeFileSync` in mutations, `openplanner_memory` JSON coerce | Migrate to async equivalents; `execSync` is critical priority |
