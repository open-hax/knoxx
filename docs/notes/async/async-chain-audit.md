---
title: "Async Chain Audit — .then/.catch Hotspots"
category: async
created: 2026-04-27
original: 2026.04.27.13.48.58.md
status: note
---

This is a comprehensive picture. Here's the full audit.

## Signal — Scope of the Problem

Zero files in the knoxx backend import promesa, yet the codebase has deep `.then`/`.catch` promise chain contamination throughout :

| File | `.then` | `.catch` | `js-await` | Priority |
|---|---|---|---|---|
| `policy_db.cljs` | 123 | 8 | 0 | 🔴 Critical |
| `app_routes.cljs` | 48 | 44 | 3 | 🔴 Critical |
| `auth/session.cljs` | 28 | 14 | 0 | 🔴 Critical |
| `tools/discord.cljs` | 41 | 0 | 0 | 🔴 Critical |
| `event_agents.cljs` | 27 | 9 | 2 | 🔴 Critical |
| `memory_routes.cljs` | 22 | 14 | 1 | 🟠 High |
| `tool_routes.cljs` | 19 | 13 | 0 | 🟠 High |
| `session_titles.cljs` | 17 | 13 | 0 | 🟠 High |
| `translation_routes.cljs` | 15 | 14 | 0 | 🟠 High |
| `tools/sandbox_container.cljs` | 26 | 3 | 0 | 🟠 High |
| `redis_client.cljs` | 8 | 17 | 0 | 🟠 High |
| `agent_runtime.cljs` | 23 | 5 | 0 | 🟡 Medium |

The `stores/*` files already use `js-await` but zero promesa , making those clean targets.

## Patterns Found

### Pattern 1 — Deeply nested `.then` cascade (worst offender)
`policy_db.cljs` shows pyramid-of-doom nesting, lines like :
```clojure
;; BEFORE — deeply nested .then tree
(.then (query! pool "INSERT..." [...])
  (fn [_]
    (.then (query! pool "UPDATE orgs SET..." [slug])
      (fn [org]
        (.then (role-permissions-uses-legacy-ids? pool)
          (fn [_] nil))))))
```
**→ Replace with `p/let`:**
```clojure
(p/let [_ (query! pool "INSERT..." [...])
        _ (query! pool "UPDATE orgs SET..." [slug])
        _ (role-permissions-uses-legacy-ids? pool)]
  nil)
```

### Pattern 2 — `.then`/`.catch` alternation (auth/session pattern)
```clojure
;; BEFORE — interleaved .then/.catch
(-> (.createSession db payload)
    (.then (fn [result] ...))
    (.catch (fn [err] ...))
    (.then (fn [_] ...))
    (.catch (fn [err2] nil)))
```
**→ Replace with `p/let` + `p/catch`:**
```clojure
(-> (p/let [result (.createSession db payload)]
      (do-thing result))
    (p/catch (fn [err] nil)))
```

### Pattern 3 — Fetch pipeline (discord, event_agents)
Both files have the same HTTP fetch-decode pattern using nested `.then` on response body :
```clojure
;; BEFORE
(.then (js/fetch url)
  (fn [resp]
    (.then (.text resp)
      (fn [text] (js/JSON.parse text)))))
```
**→ Replace with `p/let`:**
```clojure
(p/let [resp (js/fetch url)
        text (.text resp)]
  (js/JSON.parse text))
```

### Pattern 4 — Mixed `js-await` + `.then` (inconsistent within same file)
`app_routes.cljs`, `event_agents.cljs`, `tools/proxy_routes.cljs` mix both styles . These are the highest-confusion files: `js-await` only works inside an `async` context, while raw `.then` can appear anywhere. Promesa `p/let` is composable anywhere and handles both.

### Pattern 5 — Error-only catches on terminal ops
`redis_client.cljs` (8 `.then`, 17 `.catch`) suggests it wraps every Redis call in isolated `.catch (fn [_] nil)` error swallowing . This is a sign those should be `p/let` + a single `p/catch` at the call site boundary.

## Refactor Priority

- **Add promesa to `deps.edn`** first: `funcool/promesa {:mvn/version "12.0.0-RC2"}` [clojars](https://clojars.org/funcool/promesa/versions/12.0.0-RC2)
- **Start with `policy_db.cljs`** — 131 raw chain calls, no `js-await`, no promesa; nearly every function there is a candidate for `p/let` + `p/catch`
- **`stores/*`** files already use `js-await` only — swap to `p/let` since promesa is a near drop-in and eliminates the `async` boundary constraint
- **`redis_client.cljs`** — high `.catch` ratio indicates defensive swallowing; consolidate with `p/catch` per fn boundary
- **Route files** (`app_routes`, `memory_routes`, `tool_routes`, `translation_routes`) are high-volume but likely repetitive; one pattern refactored teaches the rest

## Countermoves

- (ctx, p=0.85) ლა `policy_db.cljs`'s 2128 lines likely contain deeply entangled query sequences where `p/let` will flatten the nesting dramatically but function-by-function migration is safer than a full-file rewrite
- The `stores/*` files (`redis_session_store`, `openplanner_session_store`, `composite_session_store`) only use `js-await`  and are the **lowest-risk migration targets** — swap `js-await` calls to `p/let` with no structural changes needed
- The `app_routes.cljs` having 48 `.then` + 44 `.catch` suggests route handlers each do multiple async ops; those should likely be extracted to a `p/let`-based service layer rather than inlined in route handlers
