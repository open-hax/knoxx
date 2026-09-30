---
title: "nREPL + Structural Editing as Agent Capability"
category: architecture
created: 2026-04-21
original: 2026.04.21.17.02.30.md
status: note
---

## Two Separable Problems

**1. nREPL into a running shadow-cljs Node process** — already mostly free, shadow-cljs ships an nREPL server. You just need to start it and expose it on the MCP server as a tool.

**2. Structural editing as an agent capability** — this is the interesting part. The agent needs to think of code as a **zipper over a tree**, not as a string. The tools it calls should be `slurp`, `barf`, `wrap`, `splice`, `raise`, `transpose` — not `str/replace`.

***

> **Moved 2026-09-30:** Structural editing as an agent tool surface now lives in Foresight at [`docs/research/structural-editing-agent-tool-surface.md`](https://github.com/open-hax/foresight/blob/main/docs/research/structural-editing-agent-tool-surface.md) (relationship: muse, eta-mu).

## The nREPL Wiring (what's actually missing)

shadow-cljs already runs an nREPL server when you add this to `shadow-cljs.edn`:

```clojure
{:nrepl {:port 7888}}
```

The MCP tools then connect to it as a client using `nrepl.core` from the JVM side (a small Clojure sidecar), or you bridge it via a Node TCP socket if you want to stay fully in ClojureScript. The cleanest path given the current stack: **a small JVM sidecar** (`nrepl-bridge.clj`, ~50 lines) that accepts HTTP POSTs from the Node backend and forwards them to the nREPL socket. The Node side never needs to speak the nREPL protocol directly.

***
