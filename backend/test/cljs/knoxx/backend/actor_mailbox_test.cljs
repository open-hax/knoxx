(ns knoxx.backend.actor-mailbox-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.actor.mailbox :as mailbox]))

(deftest mailbox-normalizers-keep-delivery-vocabulary-small
  (testing "message/followup aliases normalize to follow-up"
    (is (= "follow-up" (mailbox/normalize-delivery-mode "message")))
    (is (= "follow-up" (mailbox/normalize-delivery-mode "followup"))))
  (testing "unknown modes and statuses fall back to safe queue defaults"
    (is (= "follow-up" (mailbox/normalize-delivery-mode "telepathy")))
    (is (= "pending" (mailbox/normalize-status "wat")))))

(deftest source-from-context-uses-agent-spec-lineage
  (is (= {:actor-id "child-agent"
          :session-id "sess-1"
          :conversation-id "conv-1"
          :run-id "run-1"
          :contract-id "contract-1"}
         (mailbox/source-from-context {:session-id "sess-1"
                                       :conversation-id "conv-1"
                                       :run-id "run-1"
                                       :agent-spec {:actor-id "child-agent"
                                                    :contract-id "contract-1"}}))))

(deftest mailbox-entry-stores-preview-not-content
  (let [entry (mailbox/mailbox-entry {:id "11111111-1111-1111-1111-111111111111"
                                      :source {:actor-id "source"}
                                      :target {:actor-id "target"}
                                      :delivery-mode "event"
                                      :preview (apply str (repeat 300 "x"))
                                      :metadata {:lineage true}})]
    (is (= "11111111-1111-1111-1111-111111111111" (:mailbox/id entry)))
    (is (= "event" (get-in entry [:mailbox/delivery :mode])))
    (is (= "target" (get-in entry [:mailbox/target :actor-id])))
    (is (= 241 (count (:mailbox/preview entry))))
    (is (nil? (:mailbox/content entry)))))

(deftest ^:async create-entry-degrades-when-database-is-absent
  (let [entry (await (mailbox/create-entry! nil {:id "22222222-2222-2222-2222-222222222222"
                                                  :target {:actor-id "target"}
                                                  :preview "hello"}))]
    (is (= "22222222-2222-2222-2222-222222222222" (:mailbox/id entry)))
    (is (false? (:mailbox/durable? entry)))))

(deftest ^:async acknowledge-entry-degrades-when-database-is-absent
  (let [entry (await (mailbox/acknowledge-entry! nil "33333333-3333-3333-3333-333333333333"))]
    (is (= "acknowledged" (:mailbox/status entry)))
    (is (false? (:mailbox/durable? entry)))))

(deftest retry-request-event-points-at-content-ref-not-content
  (let [event (mailbox/retry-request-event {:mailbox/id "44444444-4444-4444-4444-444444444444"
                                            :mailbox/status "pending"
                                            :mailbox/target {:actor-id "worker"}
                                            :mailbox/source {:actor-id "parent"}
                                            :mailbox/delivery {:mode "follow-up" :attempts 2}
                                            :mailbox/content-ref {:event-id "evt-1"}
                                            :mailbox/metadata {:lineage true}
                                            :mailbox/preview "short preview"})]
    (is (= "actors.mailbox.retry-requested" (:eventKind event)))
    (is (= "evt-1" (get-in event [:payload :contentRef :event-id])))
    (is (nil? (get-in event [:payload :content])))))

(deftest ^:async resolve-actor-session-reads-latest-active-route
  (let [runtime {:policy-context
                 {:query! (fn [_sql _params]
                            (js/Promise.resolve
                             {:rows [{:actor_id "worker"
                                      :conversation_id "conv-1"
                                      :session_id "sess-1"
                                      :run_id "run-1"
                                      :contract_id "contract-1"
                                      :status "active"}]}))}}
        route (await (mailbox/resolve-actor-session! runtime "worker"))]
    (is (= {:actor-id "worker"
            :conversation-id "conv-1"
            :session-id "sess-1"
            :run-id "run-1"
            :contract-id "contract-1"
            :status "active"
            :last-seen-at nil}
           route))))
