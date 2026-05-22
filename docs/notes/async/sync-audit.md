---
title: Synchronous Code Audit
category: async
created: 2026-04-27
status: active
tags: [sync, blocking, fs, execSync, event-loop]
related: [async/async-refactor-plan.md]
---

# Synchronous Code Audit

Audit of blocking/synchronous patterns in the knoxx backend, classified by
whether the code runs at initialization time (acceptable) or at request runtime
(not acceptable — blocks the Node.js event loop).

## ✅ Initialization — Acceptable

These execute once at boot, before the server accepts requests.

| File | Pattern | Lines | Notes |
|---|---|---|---|
| `policy_db.cljs` | `readFileSync`, `readdirSync`, `mkdirSync` | 51–201 | Contract/policy file loading at boot |
| `contracts/loader.cljs` | `readdirSync`, `existsSync` | 80–192 | Contract directory scanning |
| `contracts/resolve.cljs` | `readFileSync` | 63 | Contract resolution at load time |
| `contracts/roles.cljs` | `readFileSync` | 23 | Role definitions at load time |
| `runtime/models.cljs` | `readFileSync`, `readdirSync` | 45–52 | Model config load |
| `triggers/control_config.cljs` | `readFileSync` | 48 | Trigger config at boot |
| `triggers/trigger_runner.cljs` | `readFileSync` | 24 | Trigger runner config at boot |
| `tools/policies.cljs` | `readFileSync`, `existsSync` | 18–56 | Policy file check at load time |
| `auth/session.cljs` | `createCipheriv`, `createDecipheriv` | 26, 45 | CPU-only microsecond-scale, fine at call site |
| `extension_runtime.cljs` | `swap!` on command handler registry | 108, 113 | Startup registration only |
| `tools/session_mycology.cljs` | `mkdirSync`, `writeFileSync` (spore scaffold) | 302–307 | One-time admin provisioning |

**Action:** Audit call sites of each — ensure they are not called per-request.
If any are, reclassify and fix.

## 🚨 Runtime — Must Fix

These execute inside request handlers or per-message loops.

### `event_agents.cljs` — `execSync` (CRITICAL)

```clojure
;; lines 379, 383, 388
result (js/require "child_process")  ;; runtime require
(.execSync result ...)                  ;; blocks entire event loop for up to 10s
```

A 10-second `execSync` freezes all concurrent requests. Replace:

```clojure
(defn exec! [cmd]
  (p/create (fn [resolve reject]
    (let [cp (js/require "child_process")]
      (.exec cp cmd (fn [err out] (if err (reject err) (resolve out))))))))

(p/let [out (exec! (str "curl -sL -o ..." ))]
  out)
```

### `session_mycology.cljs` — sync FS in tool calls

```clojure
;; lines 150–153, 146
(node-fs.readFileSync file-path "utf8")
(node-fs.appendFileSync file-path ... "utf8")
```

Replace with `fs.promises.readFile` / `appendFile` inside `p/let`.

### `policy_db.cljs` — `writeFileSync` inside mutations

```clojure
;; line 124 — runs on API call
(.writeFileSync fs file-path (str (pr-str contract) "\n") "utf8")
```

Replace with `p/let [_ (js/Promise. (fn [res] (.writeFile fs path content res)))]`
or use `promesa.core/create`.

### `openplanner_memory.cljs` — JSON roundtrip as coercion

```clojure
;; lines 265–267
(js/JSON.parse (js/JSON.stringify (clj->js (str/split node-type #","))))
```

Replace with `(clj->js [...] :keyword-fn name)` or `(into-array [...])`.

## Priority Order

1. 🔴 `event_agents.cljs` — `execSync` is the most dangerous (10s timeout)
2. 🟠 `session_mycology.cljs` — sync FS in tool execution path
3. 🟠 `policy_db.cljs` — `writeFileSync` in mutation handlers
4. 🟡 `openplanner_memory.cljs` — JSON coerce anti-pattern
