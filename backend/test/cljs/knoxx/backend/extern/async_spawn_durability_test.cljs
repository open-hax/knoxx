(ns knoxx.backend.extern.async-spawn-durability-test
  "Exercise the real spawn-error sink with isolated durable providers."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.agent-runner :as diagnostics]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-queue-fixture :as queue-fixture]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.run-admission :as admission]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.law.spawn-diagnostic :as public-diagnostic]
            [knoxx.backend.shape.session-persistence :as persistence]))

(def ^:private coordinates
  {:run_id "spawn-proof" :session_id "spawn-session" :conversation_id "spawn-conversation"})

(test/deftest public-status-refuses-arbitrary-diagnostic-values
  (doseq [status [nil "503" 200 600 503.5 {:token "private"}]]
    (test/is (= {:message "Agent turn could not be started."
                 :status 500 :code "async_spawn_failed"}
                (public-diagnostic/public-diagnostic
                 {:message "private" :stack "private" :data {:status status :token "private"}})))))

(test/deftest ^:async pre-admission-errors-stay-local-and-do-not-poison-same-id-retry
  (await
   (fixture/with-run!
    coordinates
    (^:async fn []
      (let [before @state/runs*
            logged* (atom [])
            refused (ex-info "Materialization/session construction failed" {:status 503})
            body {:session-id "spawn-session" :conversation-id "spawn-conversation"
                  :model "test-model" :message "Admission must be retryable."}
            ids (mapv #(str "never-admitted-" %) (range 24))]
        (with-redefs [policy/enforce-chat-policy! (fn [& _] true)
                      titles/maybe-prime-session-title! (fn [& _] nil)
                      turn/hydrate-and-materialize! (^:async fn [& _] (throw refused))
                      diagnostics/log-async-spawn-error! (fn [request error]
                                                           (swap! logged* conj [(:run-id request) error]))]
          (doseq [id ids]
            (let [request (assoc body :run-id id)]
              (try (await (turn/send-agent-turn! {} {} request))
                   (test/is false "The actual turn must reject before admission")
                   (catch :default error
                     (test/is (identical? refused error))
                     (runner/log-and-record-async-spawn-error! request error)))))
          (test/is (= 24 (count @logged*)))
          (test/is (= before @state/runs*) "Absent runs do not acquire nil registry entries")
          (doseq [id ids]
            (try (let [flushed (await (events/flush! id))]
                   (test/is (true? flushed) "No unadmitted event reached the writer"))
                 (catch :default _ (test/is false "Pre-admission failure poisoned the event queue"))))
          (let [id (first ids)]
            (try
              (await (admission/create-initial-run!
                      id "spawn-session" "spawn-conversation" "2026-09-20T10:00:00.000Z"
                      "test-model" "direct" nil nil {} [] {}))
              (await (events/flush! id))
              (test/is (= ["run_started"]
                          (mapv :type (await (persistence/events-since @registry/session-store* id nil)))))
              (catch :default _ (test/is false "A failed spawn must allow later admission with the same ID"))))))))))

(test/deftest ^:async pre-admission-reuse-cannot-alter-an-existing-run
  (await
   (fixture/with-run!
    coordinates
    (^:async fn []
      (let [id (:run_id coordinates)
            before (get @state/runs* id)
            provider @registry/session-store*
            persisted (await (persistence/get-run provider id))
            history (await (persistence/events-since provider id nil))
            request {:run-id id :session-id "spawn-session" :conversation-id "spawn-conversation"
                     :model "test-model" :message "A different invocation reuses this ID."}
            failure (ex-info "Failed before this invocation was admitted" {:status 503})]
        (with-redefs [policy/enforce-chat-policy! (fn [& _] true)
                      titles/maybe-prime-session-title! (fn [& _] nil)
                      turn/hydrate-and-materialize! (^:async fn [& _] (throw failure))
                      diagnostics/log-async-spawn-error! (fn [& _] nil)]
          (try (await (turn/send-agent-turn! {} {} request))
               (test/is false "The new invocation must fail before admission")
               (catch :default error
                 (test/is (identical? failure error))
                 (runner/log-and-record-async-spawn-error! request error))))
        (await (events/flush! id))
        (test/is (= before (get @state/runs* id)))
        (test/is (= persisted (await (persistence/get-run provider id))))
        (test/is (= history (await (persistence/events-since provider id nil)))))))))

(test/deftest ^:async admitted-spawn-errors-persist-only-validated-public-diagnostics
  (await
   (queue-fixture/with-queue!
    (^:async fn []
      (let [id "admitted-private-error"
            secret "private-provider-bearer-fixture"
            error (ex-info (str "Provider failed with " secret)
                           {:status 503 :code secret :authorization secret :nested {:token secret}})
            logged* (atom nil)
            public {:message "Agent turn could not be started."
                    :status 503 :code "async_spawn_failed"}]
        (set! (.-stack error) (str "internal stack " secret))
        (with-redefs [diagnostics/log-async-spawn-error! (fn [_ supplied] (reset! logged* supplied))]
          (await (runner/enqueue-event-turn!
                  {:event-agent-concurrency 1 :event-agent-queue-limit 1}
                  {:run-id id :session-id "spawn-session" :conversation-id "spawn-conversation"
                   :model "test-model" :message "An admitted queue invocation fails."
                   :agent-spec {:event-id "admitted-private-error-event"}}
                  (^:async fn [] (throw error))))
          (await (queue-fixture/wait-idle!)))
        (let [replay (await (persistence/events-since @registry/session-store* id nil))
              failures (filterv #(= "async_spawn_failed" (:type %)) replay)
              stored (await (persistence/get-run @registry/session-store* id))]
          (test/is (identical? error @logged*) "Private operator diagnostics retain the original error")
          (test/is (= 1 (count failures)))
          (test/is (= public (:diagnostic (first failures))))
          (test/is (= (:message public) (:error (first failures)) (:error stored)))
          (test/is (not (.includes (pr-str [replay stored]) secret))
                   "Neither replay nor the durable snapshot contains stack, ex-data or raw upstream text")))))))
