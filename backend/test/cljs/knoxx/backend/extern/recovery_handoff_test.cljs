(ns knoxx.backend.extern.recovery-handoff-test
  "Private restart recovery claims preserve immutable old runs and exact snapshot ownership."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.voice.turn-control :as control]
            [knoxx.backend.extern.agent-recovery :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.law.thread-recovery :as law]
            [knoxx.backend.extern.recovery-handoff-fixture :as fixture]
            [knoxx.backend.infra.agent.recovery :as recovery]
            [knoxx.backend.infra.agent.session :as agents]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.stores.clio-run-store :as runs]
            [knoxx.backend.infra.stores.clio-thread-store :as threads]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.shape.thread-store :as thread]))

(defn- ^:async with-stores! [operation]
  (let [directory (disk/temporary-directory) at (disk/now-ms)
        writer (threads/open! {:directory (str directory "/threads") :instance-id "previous-owned-process" :now-ms (constantly at)})
        provider (threads/open! {:directory (str directory "/threads") :now-ms (constantly at)})
        run-provider (runs/open! {:directory (str directory "/runs")})]
    (try (await (operation writer provider run-provider))
         (finally (disk/remove! directory)))))

(test/deftest ^:async actual-restart-recovery-admits-new-run-and-retains-old-run
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (doseq [legacy? [false true] scan? [false true :cache]]
              (await (fixture/success! writer provider run-provider (str "resume-" legacy? "-" scan?) legacy? scan?)))))))

(test/deftest ^:async stale-forged-and-unreceipted-recovery-refuse
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (doseq [[label transform!]
                    [["token" (fn [_ value] (assoc value :startup_token "foreign-token"))]
                     ["owner" (fn [_ value] (assoc value :user_id "foreign-user"))]
                     ["json" (fn [_ value] (js->clj (js/JSON.parse (js/JSON.stringify (clj->js value))) :keywordize-keys true))]
                     ["successor" (^:async fn [store value]
                                    (await (thread/patch-thread! store (:session_id value) {:run_id "successor" :startup_token "successor"})) value)]
                     ["generation" (^:async fn [store value]
                                     (await (thread/delete-thread! store (:session_id value)))
                                     (await (thread/put-thread! writer value))
                                     (test/is (= value (await (thread/read-thread store (:session_id value)))))
                                     value)]
                     ["same-instance" (^:async fn [store value]
                                        (await (thread/patch-thread! store (:session_id value) {}))
                                        (await (sessions/get-session (:session_id value))))]]]
              (await (fixture/refused! writer provider run-provider label transform!)))))))

(test/deftest ^:async recovery-failure-and-no-message-never-complete-a-successor
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (doseq [message? [false true]]
              (let [value (cond-> (fixture/record (str "successor-" message?)) (not message?) (assoc :messages []))]
                (await (fixture/seed! writer value))
                (await (fixture/with-runtime!
                        provider run-provider (fn [])
                        (^:async fn []
                          (let [snapshot (await (sessions/get-session (:session_id value)))
                                supersede! (^:async fn [& _]
                                             (await (thread/patch-thread! provider (:session_id value)
                                                       {:run_id "next" :startup_token "next" :status "running"}))
                                             (throw (ex-info "Later failure" {:code "owned_late_failure"})))]
                            (with-redefs [turn/send-agent-turn! supersede!
                                          agents/ensure-agent-session! (fn
                                                                        ([_ _ _ _ _ _ _ _] (supersede!))
                                                                        ([_ _ _ _ _ _ _ _ _] (supersede!)))]
                              (test/is (false? (:resumed (await (recovery/resume-recovered-session! {} {} snapshot))))))
                            (let [current (await (thread/read-thread provider (:session_id value)))]
                              (test/is (= "running" (:status current)))
                              (test/is (= "next" (:run_id current) (:startup_token current))))))))))))))

(test/deftest ^:async actual-local-owner-refuses-even-a-prior-instance-snapshot
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (await (fixture/refused!
                    writer provider run-provider "local-owner"
                    (fn [_ value]
                      (swap! control/active-turns* assoc (:conversation_id value) {:run-id "live-local"})
                      value)))))))

(test/deftest ^:async no-message-recovery-releases-once-and-keeps-the-transcript
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (let [value (assoc (fixture/record "no-message") :messages [{:role "system" :content "Keep context"}])]
              (await (fixture/seed! writer value))
              (await (fixture/with-runtime!
                      provider run-provider (fn [])
                      (^:async fn []
                        (let [snapshot (await (sessions/get-session (:session_id value)))
                              result (await (recovery/resume-recovered-session! {} {} snapshot))
                              current (await (sessions/get-session (:session_id value)))]
                          (test/is (= "no pending user message to resume" (:reason result)))
                          (test/is (false? (:resumed result)))
                          (test/is (= "waiting_input" (:status current)))
                          (test/is (= (:messages value) (:messages current)))
                          (test/is (= (:run_id value) (:recovered_from_run_id current))))))))))))

(test/deftest legacy-missing-instance-remains-explicitly-eligible
  (let [record (dissoc (fixture/record "legacy-law") :startup_token)]
    (test/is (= record (law/assert-release! record record true {:instance-id "current"})))
    (test/is (thrown? cljs.core/ExceptionInfo
                     (law/assert-release! (assoc record :system_instance_id "current")
                                          (assoc record :system_instance_id "current") true {:instance-id "current"})))))

(test/deftest ^:async unrelated-live-turn-cannot-acknowledge-recovery-kickoff
  (await (with-stores!
          (^:async fn [writer provider run-provider]
            (let [value (fixture/record "kickoff-owner")]
              (await (fixture/seed! writer value))
              (await (fixture/with-runtime!
                      provider run-provider (fn [])
                      (^:async fn []
                        (let [snapshot (await (sessions/get-session (:session_id value)))
                              entered* (atom nil) reject* (atom nil) settled* (atom nil)
                              entered (js/Promise. (fn [resolve _] (reset! entered* resolve)))
                              launch (js/Promise. (fn [_ reject] (reset! reject* reject)))]
                          (with-redefs [turn/send-agent-turn! (fn [& _] (@entered* true) launch)]
                            (let [work ((^:async fn [] (reset! settled* (await (recovery/resume-recovered-session!
                                                                              {} {} snapshot {:wait-for :kickoff})))))]
                              (await entered)
                              (swap! control/active-turns* assoc (:conversation_id value) {:run_id "different-live-run"})
                              (try
                                (await (js/Promise. (fn [resolve _] (js/setTimeout resolve 75))))
                                (test/is (nil? @settled*) "Another run cannot acknowledge this pending launch")
                                (@reject* (ex-info "Own launch refused" {:code "own_launch_refused"}))
                                (let [result (await work)]
                                  (test/is (false? (:resumed result)))
                                  (test/is (= "own_launch_refused" (:code result))))
                                (finally (@reject* (ex-info "Fixture cleanup" {})) (await work))))))))))))))

(test/deftest ^:async logging-failure-never-creates-an-unhandled-launch-observer
  (with-redefs [native/log-failure! (fn [& _] (throw (ex-info "Logger unavailable" {})))]
    (test/is (= {:code "recovery_logging_failed" :diagnostic-emitted false}
                (await (native/observe-launch! (js/Promise.reject (ex-info "Launch refused" {})) "session" "conversation"))))))
