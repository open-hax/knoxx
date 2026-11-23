(ns knoxx.backend.social-handoff-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.actor.acting :as acting]
            [knoxx.backend.infra.agent.handoff :as handoff]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.clients.knoxx-control :as control]
            [knoxx.backend.shape.session-persistence :as persistence]))

(def ^:private owner
  {:actor-id "social" :org-id "team" :membership-id "first-member"})

(def ^:private agent-session
  {:conversation-id "parent-conversation"
   :session-id "parent-session"
   :agent-spec {:actor-id "social"}})

(def ^:private worker-resources
  [{:contractClass "actors"
    :contract {:actor/id "social" :actor/metadata {:delegates ["worker"]}}}
   {:contractClass "agents"
    :contract {:contract/id "worker" :contract/kind :agent :contract/actors ["social"]}}])

(def ^:private accepted-child
  {:ok true :queued true :model "gemma4:31b"
   :run_id "child-run" :session_id "child-session" :conversation_id "child-conversation"})

(defn- invoke-in-session!
  [identity session execute args]
  (acting/run-as! identity
    #(tool-context/run-with! session (fn [] (execute nil {} "call" args nil nil nil)))))

(defn- ^:async refusal
  [invoke]
  (try
    (await (invoke))
    nil
    (catch :default error (:code (ex-data error)))))

(deftest ^:async bounded-delegation-observes-only-the-issued-child
  (let [issued* (atom {})
        dispatch! (handoff/make-dispatch-execute nil issued*)
        status! (handoff/make-status-execute nil issued*)
        launches (atom [])
        lookups (atom [])
        delays (atom [])
        child (merge (dissoc accepted-child :ok :queued)
                     {:actor_id "social" :status "completed" :answer "Verified child result."})]
    (with-redefs [contracts/load-all-contracts-sync (constantly worker-resources)
                  control/direct-start! (fn [_ payload] (swap! launches conj payload) accepted-child)
                  persistence/get-run (fn [_ id] (when (> (count (swap! lookups conj id)) 1) child))
                  promise/delay-ms (fn [ms] (swap! delays conj ms))]
      (await (invoke-in-session! owner agent-session dispatch! {:agent "worker" :message "Check this fixture."}))
      (await (invoke-in-session! owner agent-session dispatch! {:agent "worker" :message "Check this fixture."}))
      (is (= [{:message "Check this fixture." :agent_spec {:contract_id "worker" :actor_id "social"}}] @launches))
      (doseq [[identity session run-id] [[(assoc owner :membership-id "another-member") agent-session "child-run"]
                                        [(assoc owner :org-id "another-team") agent-session "child-run"]
                                        [owner (assoc agent-session :session-id "another-session") "child-run"]
                                        [owner agent-session "unissued-run"]]]
        (is (= "agent_handoff_scope_denied"
               (await (refusal #(invoke-in-session! identity session status! {:run_id run-id :wait_ms 0}))))))
      (is (= "agent_handoff_scope_denied"
             (await (refusal #(acting/run-as! owner
                                (fn [] (status! nil {} "direct" {:run_id "child-run" :wait_ms 0} nil nil nil)))))))
      (is (empty? @lookups))
      (let [result (await (invoke-in-session! owner agent-session status! {:run_id "child-run" :wait_ms 1000}))]
        (is (= "completed" (aget result "details" "status")))
        (is (= "Verified child result." (aget result "details" "answer")))
        (is (= ["child-run" "child-run"] @lookups))
        (is (= [1000] @delays)))
      (with-redefs [persistence/get-run (fn [_ _] (assoc child :conversation_id "different-child"))]
        (is (= "agent_handoff_scope_denied"
               (await (refusal #(invoke-in-session! owner agent-session status! {:run_id "child-run" :wait_ms 0})))))))))
