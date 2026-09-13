(ns knoxx.backend.agent-turn-admission-test
  "Failure admission must stop the public agent command before model execution."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.shape.session-persistence :as runs]))

(deftest ^:async refused-initial-thread-prevents-published-run-and-model-call
  (await
   (fixture/with-run!
    {:run_id "fixture-run" :session_id "fixture-session" :conversation_id "fixture-conversation"}
    (^:async fn []
      (let [failure (ex-info "Thread ledger refused admission" {:status 503 :code "fixture_thread_refused"})
            model-calls (atom 0)
            context {:org-id "admission-org" :user-id "admission-user" :membership-id "admission-member"
                     :permissions ["agent.chat.use"]}]
        (with-redefs [policy/enforce-chat-policy! (fn [_ _] true)
                      titles/maybe-prime-session-title! (fn [& _] nil)
                      turns/hydrate-and-materialize! (fn [& _] [nil nil [] nil])
                      sessions/put-session! (fn ([_] (throw failure)) ([_ _] (throw failure)))
                      turns/prompt-and-await! (fn [& _] (swap! model-calls inc))]
          (try
            (await (turns/send-agent-turn!
                    {} {} {:run-id "refused-run" :session-id "refused-session"
                           :conversation-id "refused-conversation" :auth-context context
                           :model "test-model" :message "Do not start before admission."}))
            (is false "Initial thread refusal must reach the caller")
            (catch :default error (is (identical? failure error))))
          (is (nil? (get @state/runs* "refused-run"))))
        (is (zero? @model-calls))
        (is (empty? (await (runs/events-since @registry/session-store* "refused-run" nil))))
        (is (some? (await (runs/get-run @registry/session-store* "refused-run")))
            "Earlier run admission is retained: this is not a cross-ledger transaction."))))))
