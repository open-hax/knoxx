(ns knoxx.backend.infra.agent.startup-recovery-single-flight-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.agent.recovery :as recovery]
            [knoxx.backend.infra.agent.resume :as resume]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]))

(defn- deferred []
  (let [resolve* (atom nil)
        reject* (atom nil)
        promise (js/Promise. (fn [resolve reject]
                               (reset! resolve* resolve)
                               (reset! reject* reject)))]
    {:promise promise :resolve @resolve* :reject @reject*}))

(defn- app []
  #js {:log #js {:info (fn [& _]) :warn (fn [& _]) :error (fn [& _])}})

(deftest ^:async concurrent-callers-share-recovery-and-later-direct-call-rescans
  (let [old-state @resume/startup-resume-state*
        calls* (atom 0)
        pending (deferred)
        config {:provider :same}
        application (app)]
    (try
      (reset! resume/startup-resume-state* {:attempted? true})
      (with-redefs [sessions/available? (constantly true)
                    sessions/recover-sessions! (fn ([] (:promise pending))
                                                 ([_]
                                                  (swap! calls* inc)
                                                  (:promise pending)))]
        (let [first-call (resume/resume-on-startup! nil application config)
              second-call (resume/resume-on-startup! nil application config)
              guarded-call (resume/resume-on-process-startup! nil application config)]
          (is (identical? first-call second-call))
          (is (identical? first-call guarded-call))
          (is (= 1 @calls*))
          ((:resolve pending) [])
          (is (= {:stale 0 :resumed 0 :skipped 0} (await guarded-call)))
          (await (resume/resume-on-startup! nil application config))
          (is (= 2 @calls*))))
      (finally
        (reset! resume/startup-resume-state* old-state)))))

(deftest ^:async scan-error-is-shared-and-direct-retry-starts-a-new-scan
  (let [calls* (atom 0)
        pending (deferred)
        config {:provider :same}
        application (app)]
    (with-redefs [sessions/available? (constantly true)
                  sessions/recover-sessions! (fn ([] (js/Promise.resolve []))
                                               ([_]
                                                (if (= 1 (swap! calls* inc))
                                                  (:promise pending)
                                                  (js/Promise.resolve []))))]
      (let [first-call (resume/resume-on-startup! nil application config)
            second-call (resume/resume-on-startup! nil application config)]
        (is (identical? first-call second-call))
        ((:reject pending) (js/Error. "scan failed"))
        (let [result (await first-call)]
          (is (string? (:error result)))
          (is (= result (await second-call))))
        (is (= {:stale 0 :resumed 0 :skipped 0}
               (await (resume/resume-on-startup! nil application config))))
        (is (= 2 @calls*))))))

(deftest ^:async callers-share-recovery-until-batch-finishes
  (let [calls* (atom 0)
        pending (deferred)
        started (deferred)
        config {:provider :same}
        application (app)]
    (with-redefs [sessions/available? (constantly true)
                  sessions/recover-sessions! (fn ([] (js/Promise.resolve []))
                                               ([_]
                                                (swap! calls* inc)
                                                (js/Promise.resolve [{:status "running"}])))
                  resume/process-sessions! (fn [_ _ _ _]
                                             ((:resolve started) true)
                                             (:promise pending))]
      (let [first-call (resume/resume-on-startup! nil application config)]
        (await (:promise started))
        (let [second-call (resume/resume-on-startup! nil application config)]
          (is (identical? first-call second-call))
          (is (= 1 @calls*))
          ((:resolve pending) {:stale 0 :resumed 1 :skipped 0})
          (is (= {:stale 0 :resumed 1 :skipped 0} (await second-call))))))))

(deftest ^:async different-contexts-keep-their-own-recovery
  (let [pending (deferred)
        seen* (atom [])
        application (app)]
    (with-redefs [sessions/available? (constantly true)
                  sessions/recover-sessions! (fn ([] (:promise pending))
                                               ([_] (:promise pending)))
                  resume/process-sessions! (fn [_ _ config _]
                                             (swap! seen* conj config)
                                             (js/Promise.resolve {:config config}))]
      (let [first-call (resume/resume-on-startup! :old-runtime application {:provider :old})
            second-call (resume/resume-on-startup! :new-runtime (app) {:provider :new})]
        ((:resolve pending) [{:status "running"}])
        (is (= {:config {:provider :old}} (await first-call)))
        (is (= {:config {:provider :new}} (await second-call)))
        (is (= #{{:provider :old} {:provider :new}} (set @seen*)))))))

(deftest ^:async equal-but-distinct-configs-do-not-share
  (let [pending (deferred)
        calls* (atom 0)
        application (app)
        first-config (into {} [[:provider :same]])
        second-config (into {} [[:provider :same]])]
    (with-redefs [sessions/available? (constantly true)
                  sessions/recover-sessions! (fn ([] (:promise pending))
                                               ([_] (swap! calls* inc) (:promise pending)))]
      (is (= first-config second-config))
      (is (not (identical? first-config second-config)))
      (let [first-call (resume/resume-on-startup! nil application first-config)
            second-call (resume/resume-on-startup! nil application second-config)]
        (is (not (identical? first-call second-call)))
        (is (= 2 @calls*))
        ((:resolve pending) [])
        (await first-call)
        (await second-call)))))

(deftest ^:async kickoff-timeout-does-not-cancel-the-original-launch
  (let [launches* (atom [])
        active* (atom nil)
        launch-one (deferred)
        launch-two (deferred)
        fail! (fn [_] {:resumed false :reason :timeout})
        call! #(recovery/resume-with-message!
                nil {} "session" "conversation" "run" "message"
                nil nil nil nil nil :kickoff fail!)]
    (with-redefs [sessions/update-session! (fn ([_ _] (js/Promise.resolve nil))
                                            ([_ _ _] (js/Promise.resolve nil)))
                  turn-control/active-turn (fn [_] @active*)
                  turn/send-agent-turn! (fn [_ _ _]
                                          (let [launch (if (empty? @launches*) launch-one launch-two)]
                                            (swap! launches* conj launch)
                                            (:promise launch)))]
      (is (= {:resumed false :reason :timeout} (await (call!))))
      (reset! active* :new-turn)
      (is (true? (:resumed (await (call!)))))
      (is (= 2 (count @launches*)))
      ;; The first send promise remains live after the timeout and second launch.
      ((:resolve launch-one) :late)
      ((:resolve launch-two) :done))))
