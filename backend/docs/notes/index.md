---
title: Knoxx Backend Notes — Index
category: index
created: 2026-04-27
status: stable
---

# Knoxx Backend Notes

Engineering notes for the knoxx backend package.

## Architecture

- [Overview](architecture/overview.md) — Source layout, JS bootstrap pattern, shadow-cljs config
- [MCP HTTP Facade](architecture/mcp-http-facade.md) — MCP endpoints, OAuth flow, Redis token storage
- [Route Taxonomy](architecture/route-taxonomy.md) — Five auth tiers, DoD contracts, target macro form

## Async

- [Async Refactor Plan](async/async-refactor-plan.md) — Promesa migration, file hotspots, patterns
- [Sync Code Audit](async/sync-audit.md) — Init vs runtime blocking code, fix priorities
- [defn-async Macro Design](async/defn-async-macro.md) — Proposed macro substrate for route handlers

## Ops

- [Build & Deploy](ops/build-and-deploy.md) — Quick rebuild, manual steps, dev/watch mode
- [Troubleshooting](ops/troubleshooting.md) — Common errors, shadow-cljs `!`-suffix bug, image rebuild
