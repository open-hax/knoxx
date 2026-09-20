(ns knoxx.frontend.components.agent-audit.api
  "Audit session REST calls. CLJS port of listMemorySessions /
   listActiveAgents / listAdminActiveAgents from src/lib/api/common.ts."
  (:require [knoxx.frontend.lib.api :as api]))

(def ^:private active-run-limit 25)

(defn ^:async list-active-agents
  "List active runs visible to the current caller."
  [limit]
  (:runs (await (api/request (str "/api/knoxx/agents/active?limit=" limit)))))

(defn ^:async list-admin-active-agents
  "List active runs through the administrator endpoint."
  [limit]
  (:runs (await (api/request (str "/api/admin/agents/active?limit=" limit)))))

(defn ^:async list-operator-active-agents
  "Admin view of active runs, falling back to the caller-scoped list."
  []
  (try
    (await (list-admin-active-agents active-run-limit))
    (catch :default _error
      (await (list-active-agents active-run-limit)))))

(defn list-memory-sessions
  "Read a contract-scoped memory page with the existing offset query wire."
  [{:keys [limit offset contract-id]}]
  (let [q (js/URLSearchParams.)]
    (.set q "limit" (str (or limit 12)))
    (when (and (number? offset) (pos? offset))
      (.set q "offset" (str offset)))
    (when contract-id
      (.set q "contractId" contract-id))
    (api/request (str "/api/memory/sessions?" (.toString q)))))
