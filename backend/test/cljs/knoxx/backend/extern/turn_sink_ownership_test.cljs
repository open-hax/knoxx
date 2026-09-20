(ns knoxx.backend.extern.turn-sink-ownership-test
  "Overlapping terminal settlement owns only the exact observer it installed."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.agent.agent-context :as context]
            [knoxx.backend.domain.voice.turn-control :as controls]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.agent.turn-finalization :as finalization]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.mongo-session-store :as threads]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as persistence]))

(defn- deferred []
  (let [resolve* (atom nil)
        promise (js/Promise. (fn [resolve _reject] (reset! resolve* resolve)))]
    {:promise promise :resolve! #(@resolve* %)}))

(defn- ^:async outcome! [operation]
  (try {:value (await (operation))} (catch :default error {:error error})))

(defn- ^:async assert-overlapping-settlement! [phase failure?]
  (let [previous @state/event-stream-sink* entered (deferred) gate (deferred)
        old-sink (fn [_]) events* (atom []) new-sink #(swap! events* conj %)
        removed* (atom []) failure (ex-info "settlement rejected" {:status 503})
        pause! (^:async fn [] ((:resolve! entered) true) (await (:promise gate))
                 (when failure? (throw failure)))
        persist! (if (= phase :persist) pause! (fn []))
        complete! (if (= phase :complete) pause! (fn []))]
    (try
      (state/set-event-stream-sink! old-sink)
      (with-redefs [sessions/remove-agent-session! #(swap! removed* conj %)]
        (let [work (outcome! #(finalization/settle! {:conversation-id "older" :event-stream-sink old-sink}
                                                   persist! complete!))]
          (try
            (await (:promise entered))
            (state/set-event-stream-sink! new-sink)
            ((:resolve! gate) true)
            (let [result (await work)]
              (is (= ["older"] @removed*))
              (is (identical? new-sink @state/event-stream-sink*)
                  "The older settlement cannot clear a newer installed callback")
              (if failure? (is (identical? failure (:error result))) (is (nil? (:error result)))))
            (when-let [sink @state/event-stream-sink*] (sink {:type "newer-event"}))
            (is (= [{:type "newer-event"}] @events*))
            (finally ((:resolve! gate) true) (await work)))))
      (finally (reset! state/event-stream-sink* previous)))))

(deftest ^:async terminal-settlement-retains-a-newer-observer-across-both-awaits
  (doseq [phase [:persist :complete] failure? [false true]]
    (await (assert-overlapping-settlement! phase failure?))))

(deftest ^:async settlement-without-an-owner-never-clears-an-ambient-observer
  (let [previous @state/event-stream-sink* sink (fn [_])]
    (try
      (state/set-event-stream-sink! sink)
      (with-redefs [sessions/remove-agent-session! (fn [_])]
        (await (finalization/settle! {:conversation-id "no-owner"} (fn []) (fn []))))
      (is (identical? sink @state/event-stream-sink*))
      (finally (reset! state/event-stream-sink* previous)))))

(defn- session [gate mode]
  (reify agent/IAgentSession
    (messages [_] [#js {:role "assistant" :content (if (= mode :refused) "" "owned answer")}])
    (subscribe! [_ _handler] (fn []))
    (send-user-message! [_ _content]
      ((^:async fn []
         (await (:promise gate))
         (when (= mode :failed) (throw (ex-info "provider refused" {}))))))))

(defn- ^:async seed! [id]
  (let [at (disk/instant (disk/now-ms))
        run {:run_id id :session_id id :conversation_id id :status "running"
             :created_at at :updated_at at :messages []}]
    (await (persistence/put-run! @registry/session-store* run))
    (await (threads/put-session! run))
    (state/store-run! id run)))

(defn- prompt! [id provider owned-sink]
  (outcome! #(turns/prompt-and-await! {} id id id (disk/now-ms) "model" "direct"
                                    provider "request" [] nil nil [] {} owned-sink)))

(defn- ^:async finish-overlap! [mode gate-a gate-b work-a work-b sink-b observed*]
  ((:resolve! gate-a) true)
  (let [result (await work-a)]
    (case mode
      :accepted (is (= "owned answer" (get-in result [:value :answer])))
      :refused (is (string? (get-in result [:value :error])))
      :failed (is (= "provider refused" (ex-message (:error result))))))
  (is (identical? sink-b @state/event-stream-sink*))
  (is (nil? (sessions/active-agent-session "turn-a")))
  (is (some? (sessions/active-agent-session "turn-b")))
  (state/append-run-event! "turn-b" {:run_id "turn-b" :session_id "turn-b" :conversation_id "turn-b"
                                    :at (disk/instant (disk/now-ms)) :type "newer-progress"})
  (await (events/flush! "turn-b"))
  (is (some #(= "newer-progress" (:type %)) @observed*) "Newer progress still reaches its observer")
  ((:resolve! gate-b) true)
  (is (= "owned answer" (get-in (await work-b) [:value :answer])))
  (is (nil? @state/event-stream-sink*) "The final owner releases its own observer")
  (is (nil? (sessions/active-agent-session "turn-b"))))

(defn- ^:async exercise-prompt-overlap! [mode]
  (let [before-sink @state/event-stream-sink* before-sessions @sessions/sessions*
        before-controls @controls/active-turns* before-context (context/get-context)
        gate-a (deferred) gate-b (deferred) sink-a (fn [_]) observed* (atom [])
        sink-b #(swap! observed* conj %) provider-a (session gate-a mode) provider-b (session gate-b :accepted)]
    (try
      (await (seed! "turn-a")) (await (seed! "turn-b"))
      (swap! sessions/sessions* assoc "turn-a" {:session provider-a} "turn-b" {:session provider-b})
      (state/set-event-stream-sink! sink-a)
      (let [work-a (prompt! "turn-a" provider-a sink-a)]
        (state/set-event-stream-sink! sink-b)
        (let [work-b (prompt! "turn-b" provider-b sink-b)]
          (try (await (finish-overlap! mode gate-a gate-b work-a work-b sink-b observed*))
               (finally ((:resolve! gate-a) true) ((:resolve! gate-b) true)
                        (await work-a) (await work-b)))))
      (finally
        (await (events/flush! "turn-a")) (await (events/flush! "turn-b"))
        (reset! state/event-stream-sink* before-sink) (reset! sessions/sessions* before-sessions)
        (reset! controls/active-turns* before-controls) (context/set-context! before-context)))))

(deftest ^:async real-overlapping-prompts-preserve-observer-through-every-terminal-path
  (doseq [mode [:accepted :refused :failed]]
    (await (fixture/with-run! {:run_id "sink-seed" :session_id "sink-seed" :conversation_id "sink-seed"}
             #(exercise-prompt-overlap! mode)))))
