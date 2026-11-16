(ns knoxx.backend.extern.agent-control-persistence-test
  "Actual live-control responses wait for their real ordered durable event writer."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-queue-fixture :as queue-fixture]
            [knoxx.backend.infra.agent.runtime :as runtime]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as runs]))

(def ^:private coordinates
  {:run_id "control-run" :session_id "control-session" :conversation_id "control-conversation"})

(defn- gate []
  (let [resolve* (atom nil) reject* (atom nil)
        promise (js/Promise. (fn [resolve reject] (reset! resolve* resolve) (reset! reject* reject)))]
    {:promise promise :resolve! #(@resolve* true) :reject! #(@reject* %)}))

(defn- control-session [deliver!]
  (reify agent/IAgentSession
    (streaming? [_] true)
    (current-turn [_] nil)
    (messages [_] [])
    (subscribe! [_ _] (fn []))
    (send-user-message! [_ _] nil)
    (follow-up! [_ message] (deliver! message))
    (steer! [_ message] (deliver! message))
    (set-thinking-level! [_ _] nil)
    (abort! [_] nil)))

(defn- session [failure]
  (control-session (^:async fn [_] (when failure (throw failure)))))

(defn- ^:async control-outcome! [kind settled*]
  (try
    {:value (await (runtime/queue-agent-control!
                    {} {} {:run-id (:run_id coordinates) :session-id (:session_id coordinates)
                           :conversation-id (:conversation_id coordinates) :kind kind :message "Continue"}))}
    (catch :default error {:error error})
    (finally (reset! settled* true))))

(defn- ^:async assert-result! [provider work kind control-failure write-failure published*]
  (let [result (await work)
        expected-type (str (if (= kind "follow_up") "follow_up" "steer")
                           (if control-failure "_failed" "_queued"))
        stored (await (runs/events-since provider (:run_id coordinates) nil))]
    (if write-failure
      (do (test/is (identical? write-failure (:error result)))
          (test/is (empty? @published*))
          (test/is (empty? stored)))
      (do (if control-failure
            (test/is (identical? control-failure (:error result)))
            (test/is (= true (get-in result [:value :ok]))))
          (test/is (= [expected-type] (mapv :type @published*)))
          (test/is (= [expected-type] (mapv :type stored)))))))

(defn- ^:async run-controlled! [kind control-failure write-failure]
  (let [provider @registry/session-store* append! runs/append-event!
        writer (gate) entered* (atom false) settled* (atom false) published* (atom [])
        active-session (session control-failure)]
    (with-redefs [turn-control/active-turn (constantly (assoc coordinates :session active-session))
                  sessions/active-agent-session (constantly active-session)
                  realtime/broadcast-ws-session! (fn [_ _ event] (swap! published* conj event))
                  runs/append-event! (^:async fn [store event]
                                      (reset! entered* true)
                                      (await (:promise writer))
                                      (await (append! store event)))]
      (let [work (control-outcome! kind settled*)]
        (try
          (await (queue-fixture/wait-until! #(deref entered*)))
          (await (js/Promise. (fn [resolve _reject] (js/setImmediate resolve))))
          (test/is (false? @settled*) "Acknowledgment/rejection must wait for durable admission")
          (test/is (empty? @published*) "WebSocket publication must wait for durable admission")
          (if write-failure ((:reject! writer) write-failure) ((:resolve! writer)))
          (await (assert-result! provider work kind control-failure write-failure published*))
          (finally
            ;; Observe the intentionally failed queue before restoring the fixture writer.
            (try (await (events/flush! (:run_id coordinates))) (catch :default _failure nil))
            (events/install! provider)))))))

(defn- ^:async prove! [control-fails? write-fails?]
  (doseq [kind ["follow_up" "steer"]]
    (await (fixture/with-run!
            coordinates
            #(run-controlled! kind
                               (when control-fails? (ex-info "Control rejected" {:status 409}))
                               (when write-fails? (ex-info "Durable event rejected" {:status 503})))))))

(test/deftest ^:async successful-control-waits-for-durable-event (await (prove! false false)))
(test/deftest ^:async rejected-write-prevents-success-publication (await (prove! false true)))
(test/deftest ^:async failed-control-waits-for-durable-failure-event (await (prove! true false)))
(test/deftest ^:async rejected-write-prevents-failure-publication (await (prove! true true)))


(test/deftest ^:async stale-control-is-rejected-before-delivery
  (let [delivered* (atom [])
        session (control-session #(swap! delivered* conj %))]
    (with-redefs [sessions/active-agent-session (constantly session)
                  turn-control/active-turn (constantly (assoc coordinates :session session))]
      (doseq [target [{:run-id "stale" :session-id (:session_id coordinates)}
                      {:run-id (:run_id coordinates) :session-id "stale"}]]
        (let [control (merge target {:conversation-id (:conversation_id coordinates)
                                     :kind "steer" :message "must stay undelivered"})
              outcome (try
                        (await (runtime/queue-agent-control! {} {} control))
                        (catch :default error (ex-message error)))]
          (test/is (= "Control target does not match the active run and session" outcome))))
      (test/is (empty? @delivered*)))))

(test/deftest ^:async replaced-session-is-rejected-before-delivery
  (let [delivered* (atom [])
        registered (session nil)
        replacement (control-session #(swap! delivered* conj %))
        {:keys [conversation_id session_id run_id]} coordinates]
    (with-redefs [turn-control/active-turn (constantly (assoc coordinates :session registered))
                  sessions/active-agent-session (constantly replacement)]
      (test/is (false? (runtime/control-ready? conversation_id session_id run_id)))
      (let [outcome (try
                      (await (runtime/queue-agent-control!
                              {} {} {:conversation-id conversation_id :session-id session_id
                                     :run-id run_id :kind "steer" :message "must stay undelivered"}))
                      (catch :default error (ex-message error)))]
        (test/is (= "No active running turn is available for live controls" outcome))
        (test/is (empty? @delivered*))))))
