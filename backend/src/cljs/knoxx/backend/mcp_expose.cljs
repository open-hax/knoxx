(ns knoxx.backend.mcp-expose
  "JS-facing helpers for exposing Knoxx agent tools over external protocols (MCP).

   These helpers exist so server.mjs can build an MCP tool catalog that matches
   the Knoxx agent runtime's tool objects (name/description/execute).

   Important: this takes a *JS* request context (as returned by policyDb's
   resolveRequestContext) and converts it into a CLJS map before delegating to
   the agent hydration tool factories."  
  (:require [knoxx.backend.agent-hydration :as hydration]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn- resolve-config
  "Resolve the effective CLJS config map.

   server.mjs currently holds a JS object (from core/config-js), but the tool
   factories expect a CLJS map with keyword keys. We prefer the live in-memory
   config (runtime.state/config*) because it includes enrich-config and any
   admin overrides, falling back to runtime-config/cfg if needed."  
  [config]
  (cond
    (map? config) config
    :else (or @runtime-state/config*
              (runtime-config/cfg))))

(defn create-knoxx-custom-tools-js
  "Return the same JS tool objects the Knoxx agent runtime uses.

   Parameters:
   - runtime: JS runtime bundle passed from server.mjs
   - config: Knoxx config map
   - ctx-js: a JS request context object from policyDb.resolveRequestContext

   Returns: a JS array of tool objects.
   Each tool has at least:
   - name (string)
   - description (string)
   - parameters (TypeBox schema)
   - execute (fn)

  NOTE: We intentionally accept JS contexts here because the JS bootstrap owns
   the policyDb instance. CLJS code expects keyword keys, so we keywordize."  
  [runtime config ctx-js]
  (let [ctx (when ctx-js (js->clj ctx-js :keywordize-keys true))
        cfg (resolve-config config)]
    (hydration/create-knoxx-custom-tools runtime cfg ctx)))
