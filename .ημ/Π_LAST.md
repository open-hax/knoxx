# Π Last — Knoxx Backend MJS Extraction

**Date**: 2026-04-17
**Branch**: main
**Head**: ef6a5de4

## What was done

1. **Tool args preview fix**: `value->preview-text` now guarantees a JSON.stringify fallback for non-nil, non-scalar objects. `agent_turns.cljs` handler-level fallback with `"{}"` skip guard.

2. **PM2 rename**: `knoxx-cepalon` → `knoxx`, switched to `shadow-cljs watch app`, persisted via `ecosystem.config.cjs`.

3. **mcp_gateway.mjs → mcp_bridge.cljs**: Full 419-line port. Server connections, tool catalog, tool calls, SSE parsing — all in CLJS. `mcp_gateway.mjs` deleted. `import './mcp_gateway.mjs'` removed from `server.mjs`.

4. **discord_gateway.cljs**: CLJS API wrapper around `globalThis.knoxxDiscordGateway`. Full inline blocked by discord.js's `node:events` requires which shadow-cljs `:js-provider :import` cannot resolve. Updated 3 consumers to use `dg/` namespace.

5. **HoneySQL dependency added**: `com.github.seancorfield/honeysql 2.7.1368` in `shadow-cljs.edn`, ready for policy-db port.

## Concurrent dirt (not absorbed)

- `app_routes.cljs`, `app_shapes.cljs`, `discord_cron.cljs`, `runtime_config.cljs`, `session_recovery.cljs`, `session_store.cljs`, `tooling.cljs` — likely concurrent work
- Various frontend changes
- `docs/knoxx-demo-prep-explainer.md` — deleted

## Remaining mjs files

| File | Lines | Status |
|------|-------|--------|
| `discord-gateway.mjs` | 302 | Stays (node:events) |
| `policy-db.mjs` | 1457 | Ready for HoneySQL port |
| `pi-session-ingester.mjs` | 732 | Deferred |
| `server.mjs` | ~170 | Slim down last |

## Verification

- Shadow-cljs compile: passes (0 errors, 174 warnings — all infer-warnings)
- PM2: `knoxx` online
