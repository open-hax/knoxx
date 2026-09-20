(ns knoxx.backend.extern.mcp-call-id-test
  "A registered MCP call has its own occurrence identity and scoped actor."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mcp-sdk :as mcp-sdk]
            [knoxx.backend.infra.actor.acting :as actor-acting]))

(deftest registered-calls-keep-distinct-identities-and-exact-inputs
  (let [handlers (atom {})
        calls (atom [])
        server #js {:registerTool (fn [name _config handler]
                                    (swap! handlers assoc name handler))}
        tool #js {:name "audit_call"
                  :execute (fn [id params _signal _update _context]
                             (swap! calls conj {:id id :params params
                                                :actor (actor-acting/current-actor-id)
                                                :org (actor-acting/current-org-id)}))}
        parameters #js {:operation_id "stable-command" :content "Preserve the caller command"}]
    (mcp-sdk/register-tools! server nil #js [tool] {:actor-id "actor-a" :org-id "org-a"})
    ((get @handlers "audit_call") parameters)
    ((get @handlers "audit_call") parameters)
    (is (= 2 (count @calls)))
    (is (not= (:id (first @calls)) (:id (second @calls))))
    (is (every? #(re-matches #"mcp-[a-f0-9]{32}" (:id %)) @calls))
    (is (every? #(identical? parameters (:params %)) @calls))
    (is (every? #(= ["actor-a" "org-a"] [(:actor %) (:org %)]) @calls))
    (is (not (actor-acting/in-scope?)))))
