(ns knoxx.backend.agent-turn-timeout-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]
            [knoxx.backend.infra.agent.turn :as agent-turns]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.shape.agent :as agent-shape]))

(defn- pending-agent-session
  ([aborted*] (pending-agent-session aborted* nil))
  ([aborted* abort-outcome]
   (reify agent-shape/IAgentSession
     (streaming? [_] true)
     (current-turn [_] nil)
     (messages [_] [])
     (subscribe! [_ _handler] (fn [] nil))
     (send-user-message! [_ _content] (js/Promise. (fn [_resolve _reject] nil)))
     (follow-up! [_ _message] (js/Promise.resolve nil))
     (steer! [_ _message] (js/Promise.resolve nil))
     (set-thinking-level! [_ _level] nil)
     (abort! [_]
       (when aborted* (reset! aborted* true))
       (cond
         (= :pending abort-outcome)
         (js/Promise. (fn [_resolve _reject] nil))

         abort-outcome
         (js/Promise.reject abort-outcome)

         :else
         (js/Promise.resolve nil))))))

(defn- delayed-send-session
  "A session whose send-user-message! resolves to `value` after `delay-ms`."
  [value delay-ms]
  (reify agent-shape/IAgentSession
    (streaming? [_] true)
    (current-turn [_] nil)
    (messages [_] [])
    (subscribe! [_ _handler] (fn [] nil))
    (send-user-message! [_ _content]
      (js/Promise. (fn [resolve _reject]
                     (js/setTimeout #(resolve value) delay-ms))))
    (follow-up! [_ _message] (js/Promise.resolve nil))
    (steer! [_ _message] (js/Promise.resolve nil))
    (set-thinking-level! [_ _level] nil)
    (abort! [_] (js/Promise.resolve nil))))

(deftest ^:async send-user-message-runs-unbounded-when-timeout-disabled
  (testing "a zero/nil timeout means the provider send is never raced against a timer"
    ;; A 20ms send would lose to a 1ms race timer; with the timeout disabled it
    ;; resolves normally, proving autonomous agents can run as long as they need.
    (is (= "agent-done"
           (await (agent-turns/send-user-message-with-timeout!
                   (delayed-send-session "agent-done" 20) "hello" 0))))
    (is (= "agent-done"
           (await (agent-turns/send-user-message-with-timeout!
                   (delayed-send-session "agent-done" 20) "hello" nil))))))

(deftest ^:async send-user-message-still-races-when-timeout-positive
  (testing "an explicit positive timeout still force-closes a turn that overruns it"
    (try
      (await (agent-turns/send-user-message-with-timeout!
              (delayed-send-session "agent-done" 50) "hello" 1))
      (is false "should have rejected on timeout")
      (catch :default err
        (is (re-find #"Agent turn timed out after 1ms" (.-message err)))))))

(deftest ^:async prompt-timeout-finalizes-session-as-failed
  (testing "provider turns that never settle clear the active stream through failure finalization"
    (let [completed* (atom nil)
          aborted* (atom false)]
      (with-redefs [session-store/complete-session!
                    (fn ([session-id conversation-id payload]
                         (reset! completed* {:session-id session-id
                                             :conversation-id conversation-id
                                             :payload payload}))
                      ([_db session-id conversation-id payload]
                       (reset! completed* {:session-id session-id
                                           :conversation-id conversation-id
                                           :payload payload})))]
        (try
          (await (agent-turns/prompt-and-await!
                  {:agent-turn-timeout-ms 1}
                  "session-timeout" "run-timeout" "conversation-timeout"
                  (.now js/Date) "gpt-5.5" "direct"
                  (pending-agent-session aborted*)
                  "hello" [] nil nil [{:role "user" :content "hello"}] {}))
          (is false "prompt-and-await! should reject on timeout")
          (catch :default err
            (is (re-find #"Agent turn timed out after 1ms" (.-message err)))
            (is (true? @aborted*)
                "the provider is aborted before the FIFO may release its slot")
            (is (= {:session-id "session-timeout"
                    :conversation-id "conversation-timeout"
                    :payload {:status "failed"
                              :error "Agent turn timed out after 1ms"
                              :messages [{:role "user" :content "hello"}]}}
                   @completed*))))))))

(deftest ^:async prompt-timeout-fail-stops-when-provider-abort-never-settles
  (testing "a hung abort cannot release the FIFO into a still-live provider"
    (let [completed* (atom nil)
          aborted* (atom false)
          abort-diagnostic* (atom nil)
          exit-code* (atom nil)]
      (with-redefs [session-store/complete-session!
                    (fn ([session-id conversation-id payload]
                         (reset! completed* {:session-id session-id
                                             :conversation-id conversation-id
                                             :payload payload}))
                      ([_db session-id conversation-id payload]
                       (reset! completed* {:session-id session-id
                                           :conversation-id conversation-id
                                           :payload payload})))
                    errors/log-error!
                    (fn [boundary context err]
                      (reset! abort-diagnostic*
                              {:boundary boundary
                               :context context
                               :message (.-message err)}))
                    xturn-node/terminate-process!
                    (fn [exit-code]
                      (reset! exit-code* exit-code)
                      (throw (js/Error. "process termination requested")))]
        (try
          (await (agent-turns/prompt-and-await!
                  {:agent-turn-timeout-ms 1
                   :agent-turn-abort-grace-ms 1}
                  "session-abort-hangs" "run-abort-hangs"
                  "conversation-abort-hangs" (.now js/Date) "gpt-5.5"
                  "direct"
                  (pending-agent-session aborted* :pending)
                  "hello" [] nil nil [{:role "user" :content "hello"}] {}))
          (is false "prompt-and-await! should fail-stop")
          (catch :default err
            (is (= "process termination requested" (.-message err)))
            (is (true? @aborted*))
            (is (= 1 @exit-code*))
            (is (nil? @completed*)
                "the current process must not settle and release its FIFO")
            (is (= :agent-turn/provider-abort-failed
                   (:boundary @abort-diagnostic*)))
            (is (= "Provider abort did not settle before its safety grace elapsed"
                   (:message @abort-diagnostic*)))))))))
