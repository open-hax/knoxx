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

## Runtime Operations

- Do not restart Knoxx PM2 processes unless the user explicitly asks for a restart.
- Prefer source edits and let shadow-cljs hot reload backend CLJS changes; Vite will reload frontend changes automatically.
- If a restart seems necessary, report why and wait for the user to restart or approve it.

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

## Licensing & Copyleft Doctrine

**License:** LGPL-3.0-only (see `LICENSE`)

### The Inviolable Rule

> **Any modification to LGPL-licensed code in this repository MUST be carried forward and published under the LGPL.**

This is not a suggestion. It is the structural contract of the copyleft itself. If you touch it, you ship it — under the same license.

### Open-Core Architecture

Knoxx operates an **open-core model**:

| Layer | License | Proprietary OK? |
|-------|---------|------------------|
| Backbone (agents, tools, ingestion, contracts) | LGPL-3.0 | No — modifications stay LGPL |
| Bespoke microservices (deployment configs, client-specific integrations) | Proprietary (separate process) | Yes — isolated behind API boundaries |
| Configuration / contract subsets | Per-contract terms | Scoped to deployment |

### Why LGPL (not GPL, not MIT)

- **LGPL permits proprietary linking** — a closed-source application can *use* the library without becoming GPL itself.
- **LGPL protects the commons** — changes to the library code itself must remain open.
- **API boundaries are the firewall** — proprietary logic lives in separate microservices that communicate with the open core over well-defined interfaces. This is the architectural expression of the license boundary.

### What This Means in Practice

1. **Fork the LGPL code?** Your fork stays LGPL. Publish your changes.
2. **Build a proprietary service that *calls* the open core?** Permitted. Keep it in a separate process/microservice.
3. **Modify a tool, agent, or contract handler?** Your modification is LGPL. Carry it forward.
4. **Deploy for a client?** The deployment config and bespoke glue can be proprietary. The core cannot.

### Enforcement

Agents operating in this codebase must:
- Never silently incorporate proprietary code into LGPL-licensed modules.
- Flag any proposed change that would mix license boundaries.
- When generating or modifying LGPL code, ensure the resulting artifact retains the LGPL header/license reference.
- Treat the API boundary between open core and proprietary microservices as a hard architectural constraint, not a convenience.

---

## Audio Labels System

The broadcast studio includes a labeling system for audio files:

### Features
- **Label any audio file** with arbitrary text labels (e.g., "intro", "outro", "music", "sfx")
- **Filter by label** to find related audio files
- **Symlink organization** - labels are synced to `./audio/<label>/` directories with symlinks
- **Agent-accessible** - labels are stored in `audio-labels.json` at the workspace root

### API Endpoints
- `GET /api/studio/labels?path=<file>` - Get labels for a file
- `GET /api/studio/labels?all=true` - Get all unique labels
- `POST /api/studio/labels/add` - Add a label to a file
- `POST /api/studio/labels/remove` - Remove a label from a file
- `GET /api/studio/labels/by-label?label=<label>` - Get all files with a label
- `POST /api/studio/sync-symlinks` - Create symlink directories in `./audio/`

### Usage for Agents
Agents can read `audio-labels.json` to discover labeled audio files, or use the API endpoints to query labels programmatically. The symlink directories provide filesystem-level access to labeled audio.
