;; Π STATE — knoxx fork-tax snapshot
;; Generated: 2026-04-17T05:40:00Z

(Π
  (repo "orgs/open-hax/openplanner/packages/knoxx")
  (branch "main")
  (head "ef6a5de4")

  (work-done
    (item "Fix value->preview-text: guaranteed JSON fallback for non-nil non-scalar objects")
    (item "Fix agent_turns.cljs tool_execution_start: raw-args JSON.stringify fallback")
    (item "PM2: rename knoxx-cepalon → knoxx, switch to shadow-cljs watch dev mode")
    (item "Create ecosystem.config.cjs for pm2")
    (item "Port mcp_gateway.mjs (419 lines) → mcp_bridge.cljs — full CLJS port, mjs deleted")
    (item "Create discord_gateway.cljs CLJS API wrapper (full inline blocked by node:events)")
    (item "Update 3 consumers (tool_routes, agent_hydration, event_agents) to use dg/ namespace")
    (item "Add HoneySQL 2.7.1368 dependency for future policy-db port"))

  (concurrent-dirt
    "backend/src/cljs/knoxx/backend/app_routes.cljs — unowned, likely concurrent work"
    "backend/src/cljs/knoxx/backend/app_shapes.cljs — unowned"
    "backend/src/cljs/knoxx/backend/discord_cron.cljs — unowned"
    "backend/src/cljs/knoxx/backend/runtime_config.cljs — unowned"
    "backend/src/cljs/knoxx/backend/session_recovery.cljs — unowned"
    "backend/src/cljs/knoxx/backend/session_store.cljs — unowned"
    "backend/src/cljs/knoxx/backend/tooling.cljs — unowned"
    "frontend/* — unowned frontend changes"
    "docs/knoxx-demo-prep-explainer.md — deleted, unowned")

  (remaining-mjs-files
    "discord-gateway.mjs — stays as runtime import (node:events constraint)"
    "policy-db.mjs — 1457 lines, HoneySQL ready, needs own PR"
    "pi-session-ingester.mjs — 732 lines, deferred"
    "server.mjs — 170 lines, glue file, slim down last")

  (blockers none))
