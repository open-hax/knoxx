(ns knoxx.backend.extern.turn-finalization-test
  "Exercise every production finalizer against rejected mandatory persistence."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-queue-fixture :as queue-fixture]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.mongo-session-store :as threads]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as persistence]))

(def ^:private coordinates
  {:run_id "cleanup-run" :session_id "cleanup-session" :conversation_id "cleanup-conversation"})

(defn- finalize! [mode session]
  (let [{:keys [run_id session_id conversation_id]} coordinates at (disk/now-ms)]
    (case mode
      :accepted (#'turns/finalize-accepted-turn-output!
                 {} session run_id conversation_id session_id "model" "answer" ""
                 [] [] 1 {:input-tokens 1 :output-tokens 1} [] nil nil [] {} {:type "run_completed"})
      :refused (#'turns/finalize-refused-turn-output!
                {} session run_id conversation_id session_id at "model" [] {} {} []
                {:diagnostic-type :fixture/refused :message "refused" :reason "empty_output"})
      :failed (#'turns/finalize-turn-failure!
               {} {:abort-reason* (atom nil) :reasoning-chunks (atom [])}
               session run_id conversation_id session_id at nil nil [] {} (ex-info "provider failed" {})))))

(defn- ^:async finalization-outcome [mode session]
  (try (await (finalize! mode session)) (catch :default error error)))

(defn- ^:async assert-cleanup! [mode persist-fails? complete-fails?]
  (let [order* (atom []) diagnostics* (atom []) persist-error (ex-info "run persistence refused" {:status 503})
        completion-error (ex-info "secret-provider-message"
                                  {:status 503 :code "thread_completion_failed" :credential "secret-provider-data"})
        complete! (^:async fn [] (swap! order* conj :complete) (when complete-fails? (throw completion-error)))
        previous-sink @state/event-stream-sink* previous-sessions @sessions/sessions*
        clear! state/clear-event-stream-sink! remove! sessions/remove-agent-session!
        session (reify agent/IAgentSession (messages [_] []))]
    (try
      (state/set-event-stream-sink! (fn [_]))
      (swap! sessions/sessions* assoc (:conversation_id coordinates) {:session session})
      (with-redefs [events/persist-run! (^:async fn [_] (swap! order* conj :persist) (when persist-fails? (throw persist-error)))
                    threads/complete-session! (fn ([_ _ _] (complete!)) ([_ _ _ _] (complete!)))
                    state/clear-event-stream-sink! (fn [] (swap! order* conj :clear) (clear!))
                    sessions/remove-agent-session! (fn [id] (swap! order* conj :remove) (remove! id))
                    errors/log-error! (fn [boundary _context error] (swap! diagnostics* conj {:boundary boundary :data (ex-data error) :message (ex-message error)}) {:message (ex-message error)})]
        (let [result (await (finalization-outcome mode session))]
          (test/is (identical? (if persist-fails? persist-error completion-error) result))
          (test/is (= [:persist :complete :clear :remove] @order*))
          (test/is (nil? @state/event-stream-sink*))
          (test/is (nil? (sessions/active-agent-session (:conversation_id coordinates))))
          (when (and persist-fails? complete-fails?)
            (let [secondary (last @diagnostics*)]
              (test/is (= :agent-turn/session-completion-failed (:boundary secondary)))
              (test/is (= {:status 503 :code "thread_completion_failed"} (:data secondary)))
              (test/is (not (re-find #"secret-provider" (pr-str @diagnostics*))))))))
      (finally (reset! state/event-stream-sink* previous-sink) (reset! sessions/sessions* previous-sessions)))))

(test/deftest ^:async persistence-rejection-cleans-every-finalization-path
  (doseq [mode [:accepted :refused :failed]]
    (await (fixture/with-run! coordinates (^:async fn [] (await (assert-cleanup! mode true false)))))))

(test/deftest ^:async session-completion-rejection-still-clears-and-removes
  (doseq [mode [:accepted :refused :failed]]
    (await (fixture/with-run! coordinates (^:async fn [] (await (assert-cleanup! mode false true)))))))

(test/deftest ^:async both-rejections-preserve-mandatory-persistence-error
  (doseq [mode [:accepted :refused :failed]]
    (await (fixture/with-run! coordinates (^:async fn [] (await (assert-cleanup! mode true true)))))))

(test/deftest ^:async admitted-settlement-retains-existing-turn-responses
  (doseq [mode [:accepted :refused :failed]]
    (await
     (fixture/with-run!
      coordinates
      (^:async fn []
        (let [completed* (atom nil) session (reify agent/IAgentSession (messages [_] []))
              complete! (fn [payload] (reset! completed* payload))]
          (with-redefs [threads/complete-session! (fn ([_ _ payload] (complete! payload)) ([_ _ _ payload] (complete! payload)))
                        errors/log-error! (fn [_ _ error] {:message (ex-message error)})]
            (let [result (await (finalization-outcome mode session))]
              (test/is (= (if (= :accepted mode) "completed" "failed") (:status @completed*)))
              (case mode
                :accepted (do (test/is (= "answer" (:answer result)))
                              (test/is (= [{:role "assistant" :content "answer"}] (:messages @completed*))))
                :refused (do (test/is (= "refused" (:error result))) (test/is (= "" (:answer result))))
                :failed (test/is (= "provider failed" (ex-message result))))))))))))


(defn- settlement-gate []
  (let [resolve* (atom nil) reject* (atom nil)
        promise (js/Promise. (fn [resolve reject] (reset! resolve* resolve) (reset! reject* reject)))]
    {:promise promise :resolve! #(@resolve* true) :reject! #(@reject* %)}))

(defn- ^:async refused-outcome! [reason session settled*]
  (try
    {:value (await (#'turns/finalize-turn-success!
                    {} {:chunks (atom (if (= reason "empty_output") [] ["No tool was called"]))
                        :reasoning-chunks (atom [])}
                    session (:run_id coordinates) (:conversation_id coordinates) (:session_id coordinates)
                    (disk/now-ms) "model" nil nil nil []
                    (if (= reason "required_tool_not_called") {:tools-choice "required-first"} {})))}
    (catch :default error {:error error})
    (finally (reset! settled* true))))

(defn- ^:async assert-refusal-settlement! [reason phase failure gate entered* settled* order* work]
  (let [provider @registry/session-store* run-id (:run_id coordinates)]
    (await (queue-fixture/wait-until! #(or @entered* @settled*)))
    (when-not @entered* (throw (or (:error (await work)) (ex-info "Refusal skipped persistence" {}))))
    (await (js/Promise. (fn [resolve _reject] (js/setImmediate resolve))))
    (test/is (false? @settled*) "Refusal response waits for mandatory settlement")
    (test/is (empty? @order*) "No broadcast or cleanup precedes durable settlement")
    (test/is (= "running" (:status (await (persistence/get-run provider run-id)))))
    (test/is (= (if (= phase :event) [] ["run_failed"])
                (mapv :type (await (persistence/events-since provider run-id nil)))))
    (if failure ((:reject! gate) failure) ((:resolve! gate)))
    (let [result (await work) stored (await (persistence/get-run provider run-id))]
      (if failure
        (do (test/is (identical? failure (:error result)))
            (test/is (= "running" (:status stored)))
            (test/is (= [:complete :clear :remove] @order*)))
        (do (test/is (string? (get-in result [:value :error])))
            (test/is (= "" (get-in result [:value :answer])))
            (test/is (= "failed" (:status stored)))
            (test/is (= reason (:reason stored)))
            (test/is (= [:persisted [:broadcast "run_failed" reason] :complete :clear :remove] @order*)))))
    (test/is (nil? @state/event-stream-sink*))
    (test/is (nil? (sessions/active-agent-session (:conversation_id coordinates))))))

(defn- controlled-persistence [phase gate entered* order*]
  (let [append! persistence/append-event! put! persistence/put-run!
        wait! (^:async fn [] (reset! entered* true) (await (:promise gate)))]
    {:append! (^:async fn [store event]
                (when (= phase :event) (await (wait!)))
                (await (append! store event)))
     :put! (^:async fn [store run]
             (when (= phase :snapshot) (await (wait!)))
             (let [result (await (put! store run))]
               (swap! order* conj :persisted)
               result))}))

(defn- ^:async exercise-refusal! [reason phase failure session]
  (let [gate (settlement-gate) entered* (atom false) settled* (atom false) order* (atom [])
        controlled (controlled-persistence phase gate entered* order*)
        complete! threads/complete-session! clear! state/clear-event-stream-sink!
        remove! sessions/remove-agent-session! provider @registry/session-store*]
    (with-redefs [persistence/append-event! (:append! controlled)
                  persistence/put-run! (:put! controlled)
                  realtime/broadcast-ws-session! (fn [_ _ event] (swap! order* conj [:broadcast (:type event) (:reason event)]))
                  threads/complete-session! (fn ([id conversation payload] (swap! order* conj :complete) (complete! nil id conversation payload))
                                             ([db id conversation payload] (swap! order* conj :complete) (complete! db id conversation payload)))
                  state/clear-event-stream-sink! (fn [] (swap! order* conj :clear) (clear!))
                  sessions/remove-agent-session! (fn [id] (swap! order* conj :remove) (remove! id))
                  errors/log-error! (fn [_ _ error] {:message (ex-message error)})]
      (let [work (refused-outcome! reason session settled*)]
        (try
          (await (assert-refusal-settlement! reason phase failure gate entered* settled* order* work))
          (finally
            ((:resolve! gate))
            (await work)
            (try (await (events/flush! (:run_id coordinates))) (catch :default _failure nil))
            (events/install! provider)))))))

(defn- ^:async prove-refusal! [fails?]
  (doseq [reason ["required_tool_not_called" "empty_output"] phase [:event :snapshot]]
    (await
     (fixture/with-run!
      coordinates
      (^:async fn []
        (let [session (reify agent/IAgentSession (messages [_] [#js {:role "assistant" :content #js []}]))
              previous-sink @state/event-stream-sink* previous-sessions @sessions/sessions*]
          (try
            (state/set-event-stream-sink! (fn [_]))
            (swap! sessions/sessions* assoc (:conversation_id coordinates) {:session session})
            (await (exercise-refusal! reason phase
                                     (when fails? (ex-info "Refused settlement admission" {:status 503})) session))
            (finally
              (reset! state/event-stream-sink* previous-sink)
              (reset! sessions/sessions* previous-sessions)))))))))

(test/deftest ^:async refused-output-publication-waits-for-durable-event-and-snapshot
  (await (prove-refusal! false)))

(test/deftest ^:async rejected-refusal-settlement-never-publishes-and-still-cleans
  (await (prove-refusal! true)))
