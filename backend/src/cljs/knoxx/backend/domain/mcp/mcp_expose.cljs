(ns knoxx.backend.domain.mcp.mcp-expose
  "JS-facing helpers for exposing Knoxx agent tools over external protocols (MCP).

   These helpers exist so server.mjs can build an MCP tool catalog that matches
   the Knoxx agent runtime's tool objects (name/description/execute).

   Important: internal callers should pass a CLJS request context. JS callers
   may still pass a native object at this explicit JS-facing boundary, where it
   is decoded before delegating to the agent hydration tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.hydration :as hydration]
            [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.infra.config :as runtime-config]
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

(def ^:private sub-agent-aware-tool-names
  #{"agents.spawn"
    "triggers.fire"})

(defn- display-value
  [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (nil? value) nil
    :else (str value)))

(defn- compact-text
  [value max-len]
  (let [text (some-> value str (str/replace #"\s+" " ") str/trim not-empty)]
    (when text
      (if (> (count text) max-len)
        (str (subs text 0 max-len) "…")
        text))))

(defn- sub-agent-record->metadata
  [record]
  (let [contract (:contract record)
        caps (->> (:sub-agent/capabilities contract)
                  (map display-value)
                  (remove str/blank?)
                  vec)]
    {:id (:contract/id contract)
     :kind "sub-agent"
     :role (or (:sub-agent/role contract)
               (some-> (get-in contract [:agent :role]) display-value))
     :model (or (:sub-agent/model contract)
                (get-in contract [:agent :model]))
     :thinking (or (:sub-agent/thinking contract)
                   (some-> (get-in contract [:agent :thinking]) display-value))
     :mode (some-> (:sub-agent/mode contract) display-value)
     :parentCapabilities (some-> (:sub-agent/parent-capabilities contract) display-value)
     :capabilities caps
     :timeoutMs (:sub-agent/timeout-ms contract)
     :summary (or (compact-text (get-in contract [:prompts :task]) 220)
                  (compact-text (get-in contract [:prompts :system]) 220))
     :data (:data contract)}))

(defn- sub-agent-catalog
  [cfg]
  (try
    (->> (contracts/load-all-contracts-sync cfg)
         (filter #(= "sub_agents" (:contractClass %)))
         (map :contract)
         (filter #(not= false (:enabled %)))
         (sort-by :contract/id)
         (mapv (fn [contract]
                 (sub-agent-record->metadata {:contract contract}))))
    (catch :default err
      (.warn js/console "[knoxx-mcp] failed to load sub-agent metadata" err)
      [])))

(defn- append-sub-agent-description
  [description ids]
  (let [base (str (or description ""))]
    (if (seq ids)
      (str base
           "\n\nKnoxx sub-agents advertised by this MCP server: "
           (str/join ", " ids)
           ". Use these ids when constructing Knoxx sub-agent payloads.")
      base)))

(defn- attach-sub-agent-metadata!
  [tool catalog]
  (let [tool-name (some-> (aget tool "name") str str/trim not-empty)
        ids (mapv :id catalog)]
    (when (and (seq ids) (contains? sub-agent-aware-tool-names tool-name))
      (let [meta (or (aget tool "_meta") #js {})]
        (aset meta "knoxx/subAgents" (clj->js catalog))
        (aset meta "knoxx/subAgentIds" (clj->js ids))
        (aset tool "_meta" meta)
        (aset tool "knoxxSubAgents" (clj->js catalog))
        (aset tool "description"
              (append-sub-agent-description (aget tool "description") ids))))
    tool))

(defn- attach-sub-agent-metadata-to-tools!
  [tools cfg]
  (let [catalog (sub-agent-catalog cfg)]
    (doseq [tool (array-seq (or tools #js []))]
      (attach-sub-agent-metadata! tool catalog))
    tools))

(defn create-knoxx-custom-tools-js
  "Return the same JS tool objects the Knoxx agent runtime uses.

   Parameters:
   - runtime: JS runtime bundle passed from server.mjs
   - config: Knoxx config map
   - ctx-js: a CLJS request context map, or a JS object at this JS-facing boundary

   Returns: a JS array of tool objects.
   Each tool has at least:
   - name (string)
   - description (string)
   - parameters (TypeBox schema)
   - execute (fn)

  NOTE: This is a JS-facing compatibility entrypoint. Internal CLJS code should
   pass context maps directly; native objects are decoded here only for external
   callers."
  [runtime config ctx-js]
  (let [ctx (cond
              (map? ctx-js) ctx-js
              ctx-js (js->clj ctx-js :keywordize-keys true)
              :else nil)
        cfg (resolve-config config)
        tools (hydration/create-knoxx-custom-tools runtime cfg ctx)]
    (attach-sub-agent-metadata-to-tools! tools cfg)))
