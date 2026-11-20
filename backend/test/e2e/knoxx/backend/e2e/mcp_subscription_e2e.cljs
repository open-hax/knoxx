(ns knoxx.backend.e2e.mcp-subscription-e2e
  "Exercise subscription and Proxx provider sessions admitted and steered over local MCP."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            [knoxx.backend.e2e.mcp-control-probe-e2e :as controls]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.native-subscription :as worker]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.agent.provider.native-subscription :as native]
            [knoxx.backend.infra.agent.service :as service]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.http-server :as server]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as oauth-store]
            [knoxx.backend.infra.stores.session-store-registry :as store-registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as persistence]
            ["node:crypto" :as crypto]))

(def ^:private native-owner
  {:org-id "e2e-org" :membership-id "e2e-membership" :actor-id "e2e_actor"})

(def ^:private proxx-model "openai/gpt-6.1-sol")
(def ^:private claude-model "claude-code/haiku")

(def ^:private claude-prompt
  (str "Start a plain-text list of 120 dates, one date per line, beginning with 2000-01-01. "
       "If a new instruction arrives, stop the list and follow it. Do not call tools or read files."))

(defn- fixture-token!
  ([token] (js/Promise.resolve (controls/oauth-fixture token)))
  ([_ token] (fixture-token! token)))

(defn- spawn! [client model message]
  (mcp/call-tool! client "agents_spawn" {:message message :model model}))

(defn- ^:async check-denied-spawns! [client models]
  (doseq [model models]
    (let [reply (await (spawn! client model "This request must be refused before admission."))]
      (is (= :tool-error (:status (mcp/call-outcome reply))) (str "MCP refuses " model))
      (is (nil? (:run_id (controls/details reply)))))))

(defn- ^:async assert-native-admission-denied! [config token models]
  (let [started (await (controls/start! config))
        turns* (atom [])]
    (try
      (with-redefs [oauth-store/get-token! fixture-token!
                    service/send-agent-turn! (fn [_ _ request]
                                               (swap! turns* conj (:model request))
                                               (js/Promise.resolve nil))]
        (await (check-denied-spawns! (assoc started :token token) models))
        (is (empty? @turns*) "denied subscription requests never reach the turn service"))
      (finally (await (server/close! (:app started)))))))

(deftest ^:async native-subscription-admission-restrictions
  (await (assert-native-admission-denied! {:native-subscription-owner nil}
                                          "fixture-oauth-bearer" [claude-model]))
  (await (assert-native-admission-denied! {:native-subscription-owner native-owner}
                                          "fixture-oauth-bearer" [claude-model]))
  (let [enabled {:native-subscription-owner native-owner
                 :native-claude-home "/tmp/knoxx-claude-fixture"}]
    (await (assert-native-admission-denied! enabled "fixture-other-member" [claude-model]))
    (await (assert-native-admission-denied! enabled "fixture-oauth-bearer"
                                            ["claude-code/unknown"]))))

(defn- pause! []
  (js/Promise. (fn [resolve] (js/setTimeout resolve 25))))

(defn- terminal? [record]
  (contains? #{"completed" "failed"} (:status record)))

(defn- ^:async run-record! [run-id]
  (await (persistence/get-run @store-registry/session-store* run-id)))

(defn- ^:async wait-for-steering-ready! [client run-id]
  (let [deadline (+ (.now js/Date) 25000)]
    (loop []
      (let [reply (await (mcp/call-tool! client "agents_status" {:run_id run-id}))
            status (controls/details reply)]
        (cond
          (terminal? status)
          (throw (ex-info "Subscription run reached terminal state before steering"
                          (select-keys status [:status :run_id])))

          (:can_steer status) reply

          (> (.now js/Date) deadline)
          (throw (ex-info "MCP did not report that steering was ready"
                          {:run-id run-id :status (:status status)}))

          :else (do (await (pause!)) (recur)))))))

(defn- ^:async wait-for-settlement! [{:keys [run_id conversation_id]} timeout-ms]
  (let [deadline (+ (.now js/Date) timeout-ms)]
    (loop []
      (let [record (await (run-record! run_id))]
        (cond
          (and (terminal? record)
               (nil? (turn-control/active-turn conversation_id)))
          record
          (> (.now js/Date) deadline)
          (throw (ex-info "Knoxx did not settle and release the native session and turn"
                          {:run-id run_id :status (:status record)}))
          :else (do (await (pause!)) (recur)))))))

(defn- ^:async assert-provider-completion! [client identifiers model marker terminal]
  ;; Simulate eviction so terminal visibility and control receipts depend on storage.
  (swap! state/runs* dissoc (:run_id identifiers))
  (let [after (await (mcp/call-tool! client "agents_status" {:run_id (:run_id identifiers)}))
        status (controls/details after)
        event-types (set (map :type (:recent_events status)))
        marker-matched? (= marker (:answer terminal))]
    (controls/assert-ok! after)
    (is (= identifiers (select-keys status (keys identifiers))))
    (is (= "completed" (:status status)))
    (is (false? (:can_steer status)))
    (is (= "completed" (:status terminal)) (:error terminal))
    (is (nil? (turn-control/active-turn (:conversation_id identifiers)))
        "the completed run releases its active-turn pointer")
    (is (= model (:model terminal)))
    (is (= (if (= claude-model model) "claude-code" "proxx")
           (get-in terminal [:resources :provider])))
    (is (= "e2e-membership" (:membership_id terminal)))
    (is (= "e2e_actor" (:actor_id terminal)))
    (is (pos? (or (:input_tokens terminal) 0)))
    (is (pos? (or (:output_tokens terminal) 0)))
    (is (contains? event-types "steer_queued"))
    (is (contains? event-types "run_completed"))
    (is marker-matched? "durable provider reply contains the marker sent only through MCP")
    (harness/report! (str "[native-subscription-canary] " model " run=" (:run_id identifiers)
                          " terminal=" (:status terminal) " output-tokens=" (:output_tokens terminal)
                          " steer-marker=" marker-matched?))))

(defn- ^:async steer-native-run! [client identifiers model]
  (let [run-id (:run_id identifiers)
        marker (str "KNOXX_STEER_" (.randomUUID crypto))
        before (await (wait-for-steering-ready! client run-id))
        steer (await (mcp/call-tool! client "actors_send-message"
                                    (assoc identifiers :target (str "session:" (:session_id identifiers))
                                                       :mode "steer"
                                                       :content (str "Reply with exactly " marker ". Do not call tools."))))]
    (controls/assert-ok! before)
    (is (= "running" (:status (controls/details before))))
    (is (= model (:model (controls/details before))))
    (controls/assert-ok! steer)
    (await (assert-provider-completion! client identifiers model marker
                                       (await (wait-for-settlement! identifiers 60000))))))

(defn- ^:async run-subscription-case! [client model prompt]
  (is (:ok (await (mcp/initialize! client))))
  (let [reply (await (spawn! client model prompt))
        identifiers (select-keys (controls/details reply) [:run_id :session_id :conversation_id])]
    (controls/assert-ok! reply)
    (when-not (every? identifiers [:run_id :session_id :conversation_id])
      (throw (js/Error. "MCP spawn did not return a run, session, and conversation")))
    (try
      (await (steer-native-run! client identifiers model))
      (finally
        (when-let [session (sessions/active-agent-session (:conversation_id identifiers))]
          (try (await (agent/abort! session))
               (finally (await (wait-for-settlement! identifiers 6000)))))))))

(defn- ^:async qualify-provider! [model prompt home]
  (let [cwd (disk/temporary-directory)
        seed {:run_id "isolated-seed-run" :session_id "isolated-seed-session"
              :conversation_id "isolated-seed-conversation"}]
    (try
      (let [client (await (controls/start! {:native-subscription-owner native-owner
                                           :workspace-root cwd
                                           :agent-dir (str cwd "/agent")
                                           :native-claude-home (or home cwd)
                                           :proxx-base-url ""
                                           :extra-workspace-roots []
                                           :agent-turn-timeout-ms 60000}))]
        (try
          (await (fixture/with-run!
                  seed
                  (^:async fn []
                    (with-redefs [oauth-store/get-token! fixture-token!]
                      (await (run-subscription-case! client model prompt))))))
          (finally (await (server/close! (:app client))))))
      (finally (disk/remove! cwd)))))

(defn- steering-worker!
  [_selection controller _prompt ready _delta]
  (js/Promise.
   (fn [resolve reject]
     (let [signal (.-signal controller)
           abort! #(reject (js/Error. "Fixture worker aborted"))]
       (.addEventListener signal "abort" abort! #js {:once true})
       (ready
        (fn [message]
          (let [marker (second (re-find #"(KNOXX_STEER_[a-f0-9-]+)" message))]
            (when-not marker (throw (js/Error. "Steering marker missing")))
            (.removeEventListener signal "abort" abort!)
            (resolve {:text marker :usage {:input 37 :output 11}})
            (js/Promise.resolve nil))))))))

(deftest ^:async local-mcp-persists-native-claude-steering
  (with-redefs [worker/run-turn! steering-worker!]
    (await (qualify-provider! claude-model claude-prompt nil))))

(deftest ^:async local-mcp-persists-proxx-steering
  (with-redefs [worker/run-turn! steering-worker!
                titles/maybe-prime-session-title! (constantly nil)
                sessions/create-session-manager!
                (fn
                  ([_runtime _config _conversation-id _model]
                   (throw (js/Error. "Fixture requires exact session coordinates")))
                  ([_runtime _config _conversation-id model _auth _thinking _session-id spec]
                   (is (= proxx-model model))
                   (js/Promise.resolve (native/create-session {:model "fixture"} (:system-prompt spec)))))]
    (await (qualify-provider! proxx-model "A fixture agent receives steering over MCP." nil))))

(deftest ^:async native-claude-subscription-canary
  (when (= "1" (aget js/process.env "KNOXX_NATIVE_CLAUDE_CANARY"))
    (let [home (aget js/process.env "KNOXX_CLAUDE_HOME")]
      (when-not (seq home)
        (throw (js/Error. "Native Claude canary requires its dedicated profile")))
      (await (qualify-provider! claude-model claude-prompt home)))))
