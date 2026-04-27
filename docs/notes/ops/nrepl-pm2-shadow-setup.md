---
title: "nREPL + PM2 + shadow-cljs Watch Setup"
category: ops
created: 2026-04-24
original: 2026.04.24.09.38.51.md
status: note
---

The answer is already fully deterministic from the config you've pasted. No further tool calls needed.

## The Core Problem

Your `shadow.edn` declares `:nrepl {:port 4500}` and your `knoxx-backend` PM2 app runs `shadow-cljs watch server`. Shadow-cljs **does start the nREPL server automatically** when it sees that key—the nREPL is not a separate process. You don't need a second PM2 app.

## What Actually Happens

When `shadow-cljs watch server` starts:

1. The JVM launches, reads `shadow.edn`
2. nREPL binds to port `4500` before any build starts
3. The `:server` build compiles, then spawns the Node runtime (`dist/server.js`) as a child process managed by shadow-cljs internals
4. The Node child connects back to the shadow-cljs JVM over a WebSocket (this is the "runtime connection")
5. Your editor connects to port `4500`; after selecting the `:server` build, evals are relayed from nREPL → shadow JVM → WebSocket → live Node process

So the PM2 `knoxx-backend` entry already does both—one process, one port.

## The ESM/nREPL Friction Point

Your `:server` build uses `:target :esm` with `:runtime :node`. The ESM target is **not** `:node-script` or `:browser`—it compiles to a pure ESM module that shadow-cljs cannot directly launch and manage as a connected runtime. The runtime WebSocket handshake only works when shadow-cljs owns the Node process lifecycle, which requires `:target :node-script` (or the deprecated `:node-library`).

| Target | shadow manages Node process | nREPL eval works | Output format |
|---|---|---|---|
| `:node-script` | ✅ yes | ✅ yes | CJS/script |
| `:esm` `:runtime :node` | ❌ no, you own it | ⚠️ compile-only | ESM module |
| `:browser` | N/A | ✅ (via browser WS) | JS bundle |

This means with your current `:server` ESM build, the nREPL server **starts and compiles** fine, but **eval into the live Node process** won't work—the runtime is not connected. You get a Clojure REPL on port `4500`, not a live CLJS one.

## Options

**Option A — Add a `:node-script` companion build for the REPL**

Add a second build in `shadow.edn` alongside `:server`:

```clojure
:server-repl
{:target :node-script
 :output-to "target/server_repl.js"
 :main knoxx.backend.entrypoint/init
 :js-options {:js-provider :import
              :keep-as-import #{"pg" "discord.js" ...}}
 :compiler-options {:output-feature-set :es-next
                    :source-map true
                    :infer-externs :auto}}
```

Then watch both: `shadow-cljs watch server server-repl`. The `:node-script` build starts a managed Node child; the ESM `:server` build stays for production shape. Your PM2 `args` becomes:

```js
args: 'exec shadow-cljs --source-maps watch server server-repl',
```

You connect nREPL on `4500`, then `(shadow/nrepl-select :server-repl)` in CIDER/Calva.

**Option B — Keep ESM, accept compile-only nREPL**

If you're using the ESM `:server` build because your Node shim (`server.mjs` or equivalent) `import`s the compiled `dist/server.js` and owns the process lifecycle itself, then the nREPL on port `4500` still gives you full **Clojure** evaluation (JVM-side), hot recompilation on save, and source maps—but not live CLJS eval into the running Node process. This is still very useful for most development.

In your PM2 config nothing needs to change—the nREPL is already running. Just connect your editor to `localhost:4500`.

## PM2 nREPL Port Survival

One gotcha: PM2's `autorestart` will recycle the JVM if shadow-cljs crashes. When it restarts, it re-binds port `4500`. Your editor will lose its nREPL connection and need to reconnect. If that's annoying, `max_restarts: 0` for interactive dev sessions is reasonable.
