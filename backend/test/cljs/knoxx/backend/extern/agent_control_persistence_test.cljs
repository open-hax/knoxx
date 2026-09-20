(ns knoxx.backend.extern.agent-control-persistence-test
  "Actual live-control responses wait for their real ordered durable event writer."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.realtime :as realtime]
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

(defn- session [failure]
  (let [invoke (^:async fn [] (when failure (throw failure)))]
    (reify agent/IAgentSession
      (streaming? [_] true)
      (follow-up! [_ _] (invoke))
      (steer! [_ _] (invoke)))))

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
        writer (gate) entered* (atom false) settled* (atom false) published* (atom [])]
    (with-redefs [sessions/active-agent-session (fn [_] (session control-failure))
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
