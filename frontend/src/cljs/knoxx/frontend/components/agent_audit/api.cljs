(ns knoxx.frontend.components.agent-audit.api
  "Audit session REST calls. CLJS port of listMemorySessions /
   listActiveAgents / listAdminActiveAgents from src/lib/api/common.ts."
  (:require [knoxx.frontend.lib.api :as api]))

(def ^:private active-run-limit 25)

(defn list-active-agents [limit]
  (-> (api/request (str "/api/knoxx/agents/active?limit=" limit))
      (.then :runs)))

(defn list-admin-active-agents [limit]
  (-> (api/request (str "/api/admin/agents/active?limit=" limit))
      (.then :runs)))

(defn list-operator-active-agents
  "Admin view of active runs, falling back to the caller-scoped list."
  []
  (-> (list-admin-active-agents active-run-limit)
      (.catch (fn [_] (list-active-agents active-run-limit)))))

(defn list-memory-sessions [{:keys [limit offset contract-id]}]
  (let [q (js/URLSearchParams.)]
    (.set q "limit" (str (or limit 12)))
    (when (and (number? offset) (pos? offset))
      (.set q "offset" (str offset)))
    (when contract-id
      (.set q "contractId" contract-id))
    (api/request (str "/api/memory/sessions?" (.toString q)))))
