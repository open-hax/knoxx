---
title: Async Refactor Plan — Promesa Migration
category: async
created: 2026-04-27
status: active
tags: [async, promesa, p/let, refactor, promise-chains]
related:
  - async/defn-async-macro.md
  - architecture/route-taxonomy.md
priority: high
---

# Async Refactor Plan — Promesa Migration

The knoxx backend has zero promesa usage despite heavy `.then`/`.catch` chain
usage across all files. This note tracks the migration plan.

## File Hotspots (sorted by raw chain count)

| File | `.then` | `.catch` | `js-await` | Priority |
|---|---|---|---|---|
| `policy_db.cljs` | 123 | 8 | 0 | 🔴 Critical |
| `app_routes.cljs` | 48 | 44 | 3 | 🔴 Critical |
| `auth/session.cljs` | 28 | 14 | 0 | 🔴 Critical |
| `tools/discord.cljs` | 41 | 0 | 0 | 🔴 Critical |
| `event_agents.cljs` | 27 | 9 | 2 | 🔴 Critical |
| `memory_routes.cljs` | 22 | 14 | 1 | 🟠 High |
| `tool_routes.cljs` | 19 | 13 | 0 | 🟠 High |
| `redis_client.cljs` | 8 | 17 | 0 | 🟠 High |
| `stores/*` | 0 | 0 | 17 | 🟢 Low (swap `js-await` → `p/let`) |

## Migration Steps

1. Add `funcool/promesa {:mvn/version "12.0.0-RC2"}` to `deps.edn`
2. Migrate `stores/*` files first — lowest risk, `js-await` → `p/let` drop-in
3. Migrate `redis_client.cljs` — high `.catch` ratio, consolidate per-fn
4. Migrate `policy_db.cljs` function-by-function (2128 lines; do not full-file rewrite)
5. Migrate route files in tier order (Tier 0 → Tier 4)

## Key Patterns to Replace

### Nested `.then` cascade → `p/let`
```clojure
;; BEFORE
(.then (query! pool "INSERT..." [...])
  (fn [_] (.then (query! pool "UPDATE..." [slug])
    (fn [org] (.then (role-permissions-uses-legacy-ids? pool)
      (fn [_] nil))))))

;; AFTER
(p/let [_ (query! pool "INSERT..." [...])
        _ (query! pool "UPDATE..." [slug])
        _ (role-permissions-uses-legacy-ids? pool)]
  nil)
```

### Fetch pipeline → `p/let`
```clojure
;; BEFORE
(.then (js/fetch url)
  (fn [resp] (.then (.text resp)
    (fn [text] (js/JSON.parse text)))))

;; AFTER
(p/let [resp (js/fetch url)
        text (.text resp)]
  (js/JSON.parse text))
```

### Error boundary → `p/catch`
```clojure
;; AFTER
(-> (p/let [result (.createSession db payload)] result)
    (p/catch (fn [err] nil)))
```
