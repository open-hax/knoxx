# Knoxx Agent Style Guide

## Vertical Domain-Driven Slices

Organize agent tooling into **vertical domain-driven slices**, not horizontal layers.

| ✅ Prefer | ⛔ Avoid |
|-----------|---------|
| `knoxx.backend.tools.discord` | `utils.cljs` scattered helpers |
| `knoxx.backend.tools.music` | `agent_hydration.cljs` as a 45k-token god namespace |
| `knoxx.backend.tools.openplanner` | |
| `knoxx.backend.tools.contracts` | |

Each domain slice owns everything for that domain: API wrappers, tool factories, formatting, private helpers. A domain can be understood, tested, and replaced in isolation.

### Namespace Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Orchestration | `knoxx.backend.agent-*` | `agent-hydration`, `agent-runtime`, `agent-turns` |
| Domain tools | `knoxx.backend.tools.<domain>` | `tools.discord`, `tools.music`, `tools.openplanner` |
| Shared infra | `knoxx.backend.tools.shared` / `tools.media` | sanitization, media loading, path resolution |
| Cross-cutting | `knoxx.backend.<capability>` | `event-agents`, `discord-gateway`, `mcp-bridge` |

### Rules of Thumb

1. If a namespace exceeds ~400 lines, it is a candidate for slicing by domain.
2. If a function is used by two or more domains, promote it to `tools.shared` or `tools.media`.
3. Keep `agent-hydration` thin: settings, passive hydration, message assembly, and tool-suite composition only. Implementation belongs in domain slices.
4. Private helpers (`defn-`) should outnumber public functions in domain namespaces. The public surface is the tool factory and any data schemas.
5. Never import one domain slice into another to grab a helper — promote it to `shared` first.

---

## Data-Oriented Design

- Pass plain maps. Return plain maps.
- Tool execute functions receive a parameter map and return a result map.
- Avoid OO-style stateful tool builders. A tool is data: `{:name … :description … :parameters … :execute fn}`.
- Composition happens in the orchestration layer (`agent-hydration`) by concatenating domain tool vectors.

---

## Async Style

**This codebase is fully async. There is no synchronous I/O.**

### Rule: no synchronous I/O

All I/O is async — filesystem, network, database, everything. If you encounter synchronous I/O while working on a code path, **convert it before you leave**.

This is a progressive migration, not a big-bang rewrite:
> If it's on your code path, leave it async. Leave the code better than you found it.

Common synchronous patterns to replace:

| ⛔ Synchronous | ✅ Async replacement |
|---------------|---------------------|
| `fs.readFileSync` | `(.readFile fsp path "utf8")` → `p/let` |
| `fs.writeFileSync` | `(.writeFile fsp path content)` → `p/let` |
| `fs.existsSync` | `(.access fsp path)` → `p/let` |
| Any blocking Node built-in | `fs/promises` equivalent → `p/let` |

### Rule: prefer `p/let` for async/await style

`p/let` (from `promesa.core`) is the canonical async/await idiom in this codebase. It keeps multi-step async flows flat and readable — the same reason JavaScript developers prefer `async`/`await` over `.then` chains.

```clojure
(ns knoxx.backend.tools.contracts
  (:require [promesa.core :as p]))
```

**✅ Good — sequential async with `p/let`**

```clojure
(defn load-contract! [id]
  (p/let [res    (fetch-json (str "/api/contracts/" id))
          detail (fetch-json (str "/api/contracts/" id "/detail"))]
    {:contract res
     :detail   detail}))
```

**✅ Good — async filesystem (Node `fs/promises`)**

```clojure
(defn read-config! [{:keys [fsp]} path]
  (p/let [raw (.readFile fsp path "utf8")]
    (js->clj (js/JSON.parse raw) :keywordize-keys true)))
```

**✅ Good — error handling with `p/catch`**

```clojure
(defn safe-read! [{:keys [fsp]} path]
  (-> (.readFile fsp path "utf8")
      (p/then #(js/JSON.parse %))
      (p/catch (fn [err]
                 (js/console.error "Read failed:" err)
                 nil))))
```

**⛔ Avoid — nested `js-await` for multi-step flows**

```clojure
;; don't do this for multi-step workflows
(js-await [res (fetch-json "/api/contracts")]
  (js-await [detail (fetch-json "/api/detail")]
    {:contract res :detail detail}))
```

**`js-await` is acceptable** for single-step, one-off Promise sugar where you do not need multi-step composition.

### Error handling rules

- Always include `p/catch` (or `p/try`) for domain-relevant errors in `p/let` blocks.
- Do not wrap `p/let` in a synchronous `try`/`catch` — it will not catch async errors.
- Log errors at the boundary where they are handled; do not swallow them silently.

---

## General Code Style

- **No `fn` when `partial` will do.** Prefer `(partial f arg)` over `(fn [x] (f arg x))`.
- **Small functions.** A function that can be named clearly in 3–5 words is probably the right size.
- **Threading over nesting.** Use `->` and `->>` over manual nested `let` forms.
- **`when-let` over nested `let`/`if`.** If you find yourself writing `(let [x ...] (if x ...))`, use `(when-let [x ...] ...)`.
- **Reveal intent through naming.** Names are the primary documentation. A function named `build-tool-params` needs no docstring; `process-data` needs both.
