# Knoxx Agent Style Guide

## Vertical Domain-Driven Slices

We organize agent tooling into **vertical domain-driven slices**, not horizontal layers.

Prefer:
- `knoxx.backend.tools.discord` — everything Discord (API wrappers, tool factories, formatting)
- `knoxx.backend.tools.music` — everything music/audio identification
- `knoxx.backend.tools.openplanner` — graph, memory, websearch, translation
- `knoxx.backend.tools.contracts` — contract librarian read/write tooling

Over:
- ~utils.cljs~ with scattered helpers
- ~agent_hydration.cljs~ as a 45k-token god namespace

### Why
- A domain can be understood, tested, and replaced in isolation.
- Tool factories live next to the private functions that power them.
- Shared infrastructure (media loading, path resolution, TypeBox helpers) is extracted explicitly into `tools.shared` and `tools.media`, not copy-pasted.

## Data-Oriented Design

- Pass plain maps. Return plain maps.
- Tool execute functions receive a parameter map and return a result map.
- Avoid OO-style stateful tool builders. A tool is data: `{:name ... :description ... :parameters ... :execute fn}`.
- Composition happens in the orchestration layer (`agent-hydration`) by concatenating domain tool vectors.

## Namespace Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Orchestration | `knoxx.backend.agent-*` | `agent-hydration`, `agent-runtime`, `agent-turns` |
| Domain tools | `knoxx.backend.tools.<domain>` | `tools.discord`, `tools.music`, `tools.openplanner` |
| Shared infra | `knoxx.backend.tools.shared` / `tools.media` | sanitization, media loading, path resolution |
| Cross-cutting | `knoxx.backend.<capability>` | `event-agents`, `discord-gateway`, `mcp-bridge` |

## Rules of Thumb 

1. If a namespace exceeds ~400 lines, it is a candidate for slicing by domain.
2. If a function is used by two or more domains, promote it to `tools.shared` or `tools.media`.
3. Keep `agent-hydration` thin: settings, passive hydration, message assembly, and tool-suite composition only. Implementation belongs in domain slices.
4. Private helpers (`defn-`) should outnumber public functions in domain namespaces. The public surface is the tool factory and any data schemas.
5. Never import a domain slice into another domain slice to grab a helper — move the helper up to shared.

## Modern CLJS Patterns

Always prefer modern shadow-cljs patterns over legacy verbose forms:

- Use `(require [shadow.cljs.modern :refer [js-await]])` and `js-await` for async/await instead of `(.then ...)` chains
- Use `when-let` instead of nesting `let` + `if` checks
- Prefer threading macros `->` and `->>` over manual nested let forms
- Use `some->` for optional chaining through potential nils

### Why js-await

```cljs
;; Instead of this:
(-> (js/fetch url)
    (.then (fn [resp] (.json resp)))
    (.catch (fn [err] ...)))

;; Prefer this:
(js-await [resp (js/fetch url)]
  (when-not (.-ok resp)
    (throw (js/Error. "Failed")))
  (.json resp))
```

The `js-await` form is flatter, easier to read, and more debuggable.
