(ns knoxx.backend.extern.initial-admission-cleanup-test
  "Initial persistence failure releases this invocation's startup resources."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.infra.agent.initial-admission :as initial]
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
            [knoxx.backend.shape.session-persistence :as runs]
            [knoxx.backend.shape.startup-admission :as startup]))

(def ^:private seed {:run_id "startup-seed" :session_id "startup-seed" :conversation_id "startup-seed"})
(defn- provider-session [] (reify agent/IAgentSession (set-thinking-level! [_ _] nil) (messages [_] [])))
(defn- request [id] {:run-id id :session-id (str id "-session") :conversation-id (str id "-conversation")
                     :model "fixture-model" :message "Do not prompt before durable admission."})
(defn- gate []
  (let [release* (atom nil) promise (js/Promise. (fn [release _] (reset! release* release)))]
    {:promise promise :release! #(@release* true)}))
(defn- ^:async outcome! [request]
  (try {:value (await (turns/send-agent-turn! {} {} request))} (catch :default error {:error error})))

(defn- controlled-writes [phase entered* blocker failure]
  (let [claim! startup/claim-startup! append! runs/append-event! provider @registry/session-store*
        refuse! (^:async fn [] (reset! entered* true) (await (:promise blocker)) (throw failure))]
    {:claim! (^:async fn [store record view]
               (when (= phase (if (identical? store provider) :run :thread)) (await (refuse!)))
               (await (claim! store record view)))
     :event! (^:async fn [store event]
               (when (= phase :event) (await (refuse!))) (await (append! store event)))}))

(defn- ^:async assert-owned-cleanup! [request overlap? controls]
  (let [{:keys [entered* settled* session replacement replacement-sink blocker failure prompts*]} controls
        conversation (:conversation-id request)
        work ((^:async fn [] (reset! settled* (await (outcome! request)))))]
    (await (waiting/wait-until! #(or @entered* @settled*)))
    (when-not @entered* (throw (:error @settled*)))
    (test/is (identical? session (sessions/active-agent-session conversation)))
    (test/is (some? @state/event-stream-sink*))
    (when overlap?
      (swap! sessions/sessions* assoc conversation {:session replacement})
      (state/set-event-stream-sink! replacement-sink))
    ((:release! blocker))
    (test/is (identical? failure (:error (await work))))
    (test/is (zero? @prompts*))
    (test/is (identical? (when overlap? replacement) (sessions/active-agent-session conversation)))
    (test/is (identical? (when overlap? replacement-sink) @state/event-stream-sink*))))

(defn- ^:async exercise! [phase overlap?]
  (let [id (str "cleanup-" (name phase) "-" overlap?) request (request id)
        session (provider-session) replacement (provider-session) replacement-sink (fn [_])
        before-sessions @sessions/sessions* before-sink @state/event-stream-sink*
        blocker (gate) entered* (atom false) prompts* (atom 0) settled* (atom nil)
        failure (ex-info "Owned initial persistence refusal" {:status 503 :code "initial_refused"})
        writes (controlled-writes phase entered* blocker failure) provider @registry/session-store*]
    (try
      (with-redefs [sessions/create-session-manager! (fn ([_ _ _ _ _ _ _] session) ([_ _ _ _ _ _ _ _] session))
                    catalog/visible-session-signature (fn [& _] "owned-startup-proof")
                    hydration/passive-hydration! (fn ([_ _ _ _] nil) ([_ _ _ _ _] nil))
                    hydration/passive-memory-hydration! (fn ([_ _ _] nil) ([_ _ _ _] nil) ([_ _ _ _ _] nil))
                    titles/maybe-prime-session-title! (fn [& _] nil)
                    turns/prompt-and-await! (fixture/prompt-stub #(swap! prompts* inc))
                    startup/claim-startup! (:claim! writes)
                    runs/append-event! (:event! writes)]
        (await (assert-owned-cleanup!
                request overlap? {:entered* entered* :settled* settled* :session session :replacement replacement
                                  :replacement-sink replacement-sink :blocker blocker :failure failure :prompts* prompts*})))
      (finally
        ((:release! blocker))
        (try (await (events/flush! id)) (catch :default _error nil))
        (events/install! provider)
        (reset! sessions/sessions* before-sessions) (reset! state/event-stream-sink* before-sink)))))

(test/deftest ^:async initial-run-thread-and-event-refusal-release-only-owned-resources
  (doseq [phase [:run :thread :event] overlap? [false true]]
    (await (fixture/with-run! seed #(exercise! phase overlap?)))))

(defn- ^:async same-session-claims! [admitted?]
  (let [session (provider-session) conversation (str "shared-startup-" admitted?)]
    (with-redefs [sessions/create-session-manager! (fn ([_ _ _ _ _ _ _] session) ([_ _ _ _ _ _ _ _] session))
                  catalog/visible-session-signature (fn [& _] "same-session-proof")]
      (let [first-session (await (sessions/ensure-agent-session! {} {} conversation "model" nil "off" "session" {} "owner-a"))
            second-session (await (sessions/ensure-agent-session! {} {} conversation "model" nil "off" "session" {} "owner-b"))]
        (test/is (identical? first-session second-session))
        (sessions/settle-startup-session! conversation "owner-a" false)
        (test/is (identical? session (sessions/active-agent-session conversation)) "Other pending claimant retains shared construction")
        (sessions/settle-startup-session! conversation "owner-b" admitted?)
        (test/is (identical? (when admitted? session) (sessions/active-agent-session conversation)))
        (sessions/settle-startup-session! conversation "owner-a" false)
        (test/is (identical? (when admitted? session) (sessions/active-agent-session conversation)))))))

(test/deftest ^:async shared-startup-claims-promote-or-release-on-last-refusal
  (let [previous @sessions/sessions*]
    (try
      (doseq [admitted? [false true]] (await (same-session-claims! admitted?)))
      (finally (reset! sessions/sessions* previous)))))

(test/deftest ^:async established-session-is-not-owned-by-a-new-invocation
  (let [previous @sessions/sessions* session (provider-session) conversation "established-startup"]
    (try
      (with-redefs [sessions/create-session-manager! (fn ([_ _ _ _ _ _ _] session) ([_ _ _ _ _ _ _ _] session))
                    catalog/visible-session-signature (fn [& _] "established-proof")]
        (await (sessions/ensure-agent-session! {} {} conversation "model" nil "off" "session" {}))
        (await (sessions/ensure-agent-session! {} {} conversation "model" nil "off" "session" {} "new-owner"))
        (sessions/settle-startup-session! conversation "new-owner" false)
        (test/is (identical? session (sessions/active-agent-session conversation))))
      (finally (reset! sessions/sessions* previous)))))

(defn- ^:async construct-with-overlap! [construct! overlapping? arguments]
  (let [session (await (apply construct! arguments))]
    (when overlapping?
      (await (apply sessions/ensure-agent-session! (conj (vec (take 8 arguments)) "other-owner"))))
    session))

(defn- ^:async assert-hydration-failure! [late? overlapping? conversation controls work]
  (let [{:keys [entered* materialization outcome* session failure created* construction prompts*]} controls]
    (await (waiting/wait-until! #(deref entered*)))
    (when-not late? (await (waiting/wait-until! #(sessions/active-agent-session conversation))))
    ((:release! materialization))
    (await (waiting/wait-until! #(some? @outcome*)))
    (test/is (identical? failure (:error (await work))))
    (test/is (identical? (when (and overlapping? (not late?)) session) (sessions/active-agent-session conversation)))
    (when late? (test/is (false? @created*) "First failure returns while construction remains pending"))
    ((:release! construction))
    (await (waiting/wait-until! #(deref created*)))
    (await (js/Promise. (fn [resolve _] (js/setImmediate resolve))))
    (test/is (identical? (when overlapping? session) (sessions/active-agent-session conversation))
             "Late cleanup preserves another claimant, otherwise removes its construction")
    (sessions/settle-startup-session! conversation "other-owner" false)
    (test/is (nil? (sessions/active-agent-session conversation)))
    (test/is (zero? @prompts*))))

(defn- ^:async verify-hydration-refusal! [late? overlapping?]
  (let [request (request (str "hydration-refused-" late?)) conversation (:conversation-id request)
        session (provider-session) construction (gate) materialization (gate) entered* (atom false)
        failure (ex-info "Owned materialization refusal" {:code "materialization_refused"})
        outcome* (atom nil) created* (atom false) prompts* (atom 0)
        construct! sessions/construct-session-and-ext-ctx!
        create! (^:async fn [] (when late? (await (:promise construction))) (reset! created* true) session)]
    (with-redefs [sessions/construct-session-and-ext-ctx!
                  (fn [& arguments] (construct-with-overlap! construct! overlapping? arguments))
                  sessions/create-session-manager! (fn ([_ _ _ _ _ _ _] (create!)) ([_ _ _ _ _ _ _ _] (create!)))
                  catalog/visible-session-signature (fn [& _] "hydration-refusal")
                  hydration/passive-hydration! (fn ([_ _ _ _] nil) ([_ _ _ _ _] nil))
                  hydration/passive-memory-hydration! (fn ([_ _ _] nil) ([_ _ _ _] nil) ([_ _ _ _ _] nil))
                  titles/maybe-prime-session-title! (fn [& _] nil)
                  turns/materialize-content-parts! (^:async fn [& _] (reset! entered* true) (await (:promise materialization)) (throw failure))
                  turns/prompt-and-await! (fixture/prompt-stub #(swap! prompts* inc))]
      (let [work ((^:async fn [] (reset! outcome* (await (outcome! request)))))]
        (try
          (await (assert-hydration-failure!
                  late? overlapping? conversation
                  {:entered* entered* :materialization materialization :outcome* outcome* :session session
                   :failure failure :created* created* :construction construction :prompts* prompts*} work))
          (finally ((:release! materialization)) ((:release! construction)) (await work)))))))

(test/deftest ^:async hydration-refusal-cleans-current-and-late-owned-construction
  (let [previous @sessions/sessions*]
    (try
      (doseq [late? [false true] overlapping? [false true]]
        (await (fixture/with-run! seed #(verify-hydration-refusal! late? overlapping?))))
      (finally (reset! sessions/sessions* previous)))))

(test/deftest ^:async cleanup-and-logging-failure-cannot-mask-the-first-refusal
  (let [failure (ex-info "First admission refusal" {:code "first_refusal"})]
    (with-redefs [sessions/settle-startup-session! (fn [& _] (throw (ex-info "Secret cleanup failure" {})))
                  errors/log-error! (fn [& _] (throw (ex-info "Logger also refused" {})))]
      (try (await (initial/admit! {:conversation-id "cleanup-error" :startup-owner "owned"}
                                  (^:async fn [] (throw failure))))
           (test/is false "Admission must refuse")
           (catch :default error (test/is (identical? failure error)))))))

(test/deftest ^:async late-cleanup-returns-a-classified-receipt-if-diagnostics-refuse
  (with-redefs [sessions/settle-startup-session! (fn [& _] (throw (ex-info "Private cleanup error" {})))
                errors/log-error! (fn [& _] (throw (ex-info "Private log error" {})))]
    (let [receipt (await (initial/construct-session!
                          {:conversation-id "late-error" :startup-owner "owned" :startup-failed* (atom true)}
                          (fn [] (provider-session))))]
      (test/is (= {:ok false :code "initial_cleanup_failed" :diagnostic-emitted false} receipt)))))
