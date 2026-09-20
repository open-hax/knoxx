(ns knoxx.backend.infra.mailbox-delivery-test
  (:require [cljs.test :refer [deftest is]] [knoxx.backend.domain.event.dispatch :as events]
            [knoxx.backend.infra.agent.service :as agent] [knoxx.backend.infra.mailbox-delivery :as delivery]
            [knoxx.backend.infra.stores.mongo-session-store :as threads] [knoxx.backend.shape.mailbox-delivery :as port]))
(def context {:org-id "org-a" :user-id "user-a" :membership-id "member-a" :actor-binding "sender"
              :permissions ["agent.chat.use" "agent.controls.follow_up"]})
(def request {:mode "follow-up" :target {:session-id "session" :conversation-id "conversation" :run-id "forged"}
              :content "message" :metadata {} :mailbox-id "mailbox"})
(def thread {:org_id "org-a" :membership_id "member-a" :session_id "session" :conversation_id "conversation" :run_id "actual"})
(defn- ^:async refusal [f] (try (await (f)) nil (catch :default error (ex-data error))))
(deftest ^:async control-uses-owned-durable-thread-and-its-actual-run
  (try
    (let [effects (atom []) target (atom thread) provider (delivery/provider)]
      (with-redefs [threads/get-session (fn ([_id] @target) ([_db _id] @target))
                    agent/control-turn! (fn ([command] (swap! effects conj command) {:ok true})
                                          ([_runtime _config command] (swap! effects conj command) {:ok true}))]
        (await (port/deliver! provider {} {} context request))
        (is (= "actual" (:run-id (first @effects)))) (is (= "follow_up" (:kind (first @effects))))
        (reset! target (assoc thread :org_id "foreign"))
        (is (= 404 (:status (await (refusal #(port/deliver! provider {} {} context request))))))
        (reset! target (assoc thread :membership_id "foreign"))
        (is (= 403 (:status (await (refusal #(port/deliver! provider {} {} context request))))))
        (is (= 1 (count @effects)))))
    (catch :default error (is false (str "Unexpected delivery boundary failure: " error)))))
(deftest ^:async operator-event-never-acquires-trusted-internal-provenance
  (try
    (let [external (atom []) internal (atom []) provider (delivery/provider) event-request (assoc request :mode "event")]
      (with-redefs [events/dispatch-external! (fn ([event] (swap! external conj event) {:accepted true})
                                                ([_config event] (swap! external conj event) {:accepted true}))
                    events/dispatch! (fn [& args] (swap! internal conj args))]
        (is (= 403 (:status (await (refusal #(port/deliver! provider {} {} context event-request))))))
        (await (port/deliver! provider {} {} (update context :permissions conj "org.events.control") event-request))
        (is (= "org-a" (:orgId (first @external)))) (is (= "message" (get-in (first @external) [:payload :content])))
        (is (empty? @internal))))
    (catch :default error (is false (str "Unexpected delivery boundary failure: " error)))))
