(ns knoxx.backend.extern.hydration-publication-test
  "Real turn startup waits for durable hydration before publication and prompting."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-queue-fixture :as waiting]
            [knoxx.backend.infra.agent.hydration :as hydration]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.agent.tool-catalog :as catalog]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as runs]))

(def ^:private hydration-types #{"passive_hydration" "memory_hydration"})
(defn- provider-session [] (reify agent/IAgentSession (set-thinking-level! [_ _] nil) (messages [_] [])))
(defn- gate []
  (let [release* (atom nil) reject* (atom nil)
        promise (js/Promise. (fn [release reject] (reset! release* release) (reset! reject* reject)))]
    {:promise promise :release! #(@release* true) :reject! #(@reject* %)}))
(defn- hydration-events [events] (filterv hydration-types (mapv :type events)))
(defn- ^:async outcome! [request]
  (try {:value (await (turns/send-agent-turn! {} {} request))}
       (catch :default failure {:error failure})))

(defn- passive-result!
  ([_ _ _ _] (passive-result! nil nil nil nil nil))
  ([_ _ _ _ _] (js/Promise.resolve {:query "owned query" :results [] :elapsedMs 1})))
(defn- memory-result!
  ([_ _ _] (memory-result! nil nil nil nil nil))
  ([_ _ _ _] (memory-result! nil nil nil nil nil))
  ([_ _ _ _ _] (js/Promise.resolve {:query "owned query" :hits [{:text "Owned memory"}] :elapsedMs 1})))

(defn- ^:async verify-observations! [phase fails? request controls work]
  (let [{:keys [entered* settled* prompts* published* blocker provider failure]} controls
        run-id (:run-id request)
        prior (if (= phase "passive_hydration") [] ["passive_hydration"])]
    (await (waiting/wait-until! #(or @entered* (:error @settled*))))
    (when-not @entered* (throw (:error @settled*)))
    (test/is @entered*)
    (test/is (nil? @settled*) "The caller is pending while its hydration fact is pending")
    (test/is (zero? @prompts*) "No model or tool execution before hydration admission")
    (test/is (= prior (filterv hydration-types @published*)))
    (test/is (= prior (hydration-events (await (runs/events-since provider run-id nil)))))
    (if fails? ((:reject! blocker) failure) ((:release! blocker)))
    (await work)
    (try (await (events/flush! run-id)) (catch :default _error nil))
    (test/is (= (if fails? 0 1) @prompts*))
    (test/is (if fails? (identical? failure (:error @settled*)) (= :prompted (:value @settled*))))
    (let [accepted (if fails? prior ["passive_hydration" "memory_hydration"])]
      (test/is (= accepted (filterv hydration-types @published*)))
      (test/is (= accepted (hydration-events (await (runs/events-since provider run-id nil))))))
    (when fails?
      (test/is (nil? (sessions/active-agent-session (:conversation-id request)))
               "A hydration-write failure releases the unadmitted provider session")
      (test/is (nil? @state/event-stream-sink*) "A hydration-write failure releases its event sink"))))

(defn- ^:async exercise! [phase fails?]
  (let [id (str "hydration-durable-" (random-uuid))
        request {:run-id id :session-id (str id "-session") :conversation-id (str id "-conversation")
                 :model "fixture-model" :message "Hydrate before execution"}
        provider @registry/session-store* append! runs/append-event!
        before-sessions @sessions/sessions* before-sink @state/event-stream-sink*
        blocker (gate) entered* (atom false) settled* (atom nil) prompts* (atom 0) published* (atom [])
        failure (ex-info "Owned hydration write refusal" {:code "hydration_write_refused"})
        session (provider-session)]
    (with-redefs [sessions/create-session-manager! (fn ([_ _ _ _ _ _ _] session) ([_ _ _ _ _ _ _ _] session))
                  catalog/visible-session-signature (fn [& _] "hydration-publication-proof")
                  hydration/passive-hydration! passive-result!
                  hydration/passive-memory-hydration! memory-result!
                  titles/maybe-prime-session-title! (fn [& _] nil)
                  turns/prompt-and-await! (fn [& _] (swap! prompts* inc) :prompted)
                  realtime/broadcast-ws-session! (fn [_ _ event] (swap! published* conj (:type event)))
                  runs/append-event! (^:async fn [store event]
                                       (when (= phase (:type event))
                                         (reset! entered* true) (await (:promise blocker)))
                                       (await (append! store event)))]
      (let [work ((^:async fn [] (reset! settled* (await (outcome! request)))))]
        (try
          (await (verify-observations!
                  phase fails? request {:entered* entered* :settled* settled* :prompts* prompts* :published* published*
                                        :blocker blocker :provider provider :failure failure} work))
          (finally
            ((:release! blocker)) (await work)
            (try (await (events/flush! id)) (catch :default _error nil))
            (events/install! provider)
            (reset! sessions/sessions* before-sessions) (reset! state/event-stream-sink* before-sink)))))))

(test/deftest ^:async hydration-publication-and-model-execution-follow-durable-admission
  (doseq [phase ["passive_hydration" "memory_hydration"] fails? [false true]]
    (await (fixture/with-run!
            {:run_id "hydration-seed" :session_id "hydration-seed" :conversation_id "hydration-seed"}
            #(exercise! phase fails?)))))
