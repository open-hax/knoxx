(ns knoxx.backend.e2e.mcp-control-probe-e2e
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as actions]
            [knoxx.backend.domain.action.run-state :as runs]
            [knoxx.backend.domain.event.dispatch :as dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-turn-admission :as admission]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.service :as service]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.http-server :as server]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.mcp :as mcp-routes]
            [knoxx.backend.infra.routes.tools :as tools]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as oauth-store]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.runtime.state :as state]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.app-shapes :as shapes]
            ["node:crypto" :as crypto]))

(def tool-grant
  ["events_status" "events_dispatch" "agents_spawn" "actors_send-message" "memory_session" "agents_status"])

(def operator
  (assoc (harness/context-with-tool-policies
          ["events.status" "events.dispatch" "agents.spawn" "actors.send-message" "memory.session"])
         :actor-id "e2e_actor" :permissions ["agent.chat.use" "agent.runs.read_own" "agent.controls.steer"
                                       "agent.controls.follow_up" "org.events.control"]))

(def service-principal
  (assoc harness/system-admin-context
         :actor-id "control_service" :user-id "control-user"
         :actor-binding "control_service" :membership-id "control-membership"))

(defn- fixture-principal [membership-id]
  (case membership-id
    "e2e-membership" operator
    "other-member" (assoc operator :membership-id "other-member" :user-id "other-user")
    "read-only-member" (assoc operator :membership-id "read-only-member" :permissions ["agent.chat.use"])
    nil))

(defn- internal-context
  [request]
  (let [headers (.-headers request)
        email (aget headers "x-knoxx-user-email")
        key (aget headers "x-api-key")]
    (cond
      (and (= "system-admin@open-hax.local" email) (= "fixture-control-key" key))
      service-principal
      (= "e2e-membership" (aget headers "x-knoxx-membership-id"))
      operator)))

(defn- route-deps []
  {:route! shapes/route!
   :json-response! http/json-response!
   :error-response! http/error-response!
   :ensure-permission! authz/ensure-permission!
   :with-request-context! (fn [_ request _reply handler] (handler (internal-context request)))
   :session-guard (fn [request reply done]
                    (if-let [ctx (internal-context request)]
                      (do (aset request "ctx" ctx) (done))
                      (http/json-response! reply 401 {:error "fixture credential required"})))})

(defn ^:async start!
  ([] (await (start! {})))
  ([config-overrides]
   (let [runtime #js {}
         app (server/create-app!)
         cfg (merge (harness/e2e-config)
                    {:knoxx-api-key "fixture-control-key"
                     :openplanner-client-mode "rest"
                     :openplanner-base-url ""
                     :openplanner-api-key ""}
                    config-overrides)
         deps (route-deps)]
     (state/remember-context!
      runtime cfg (assoc (harness/policy-context operator)
                         :resolve-context! (fn [headers]
                                             (js/Promise.resolve
                                              (fixture-principal (get headers "x-knoxx-membership-id"))))))
     (server/ensure-json-empty-body-parser! app)
     (await (server/register-default-plugins! app))
     (mcp-routes/register-mcp-http-routes! app runtime cfg)
     (tools/register-events-get-route! app runtime cfg deps)
     (tools/register-events-dispatch-route! app runtime cfg deps)
     (app-routes/api-knoxx-direct-start! app runtime cfg deps)
     (app-routes/api-knoxx-steer! app runtime cfg deps)
     (app-routes/api-knoxx-follow-up! app runtime cfg deps)
     (await (server/listen! app "127.0.0.1" 0))
     (let [base-url (str "http://127.0.0.1:" (.-port (.address (.-server app))))]
       (swap! state/config* assoc :knoxx-base-url base-url)
       {:app app :base-url base-url :token "fixture-oauth-bearer"}))))

(defn details [reply]
  (or (get-in reply [:result :structuredContent]) (get-in reply [:result :details])))

(defn assert-ok! [reply]
  (is (= :ok (:status (mcp/call-outcome reply))) (pr-str (mcp/call-outcome reply))))

(defn- ^:async probe-events! [client observed*]
  (let [marker (.randomUUID crypto)
        reply (await (mcp/call-tool! client "events_dispatch"
                                    {:event_type "mcp.probe.local"
                                     :generator_kind "manual"
                                     :payload_json (js/JSON.stringify #js {:marker marker})}))
        event (last @dispatch/recent-events*)]
    (assert-ok! reply)
    (harness/report! (str "[mcp-control-probe] event fields " (pr-str (select-keys event [:event/type :event/actor :event/generator :event/payload]))))
    (is (= :mcp.probe.local (:event/type event)) "event type survives the MCP/self-HTTP boundary")
    (is (= "manual" (get-in event [:event/generator :kind])) "generator kind survives")
    (is (= marker (get-in event [:event/payload :marker])) "payload survives")
    (is (= "e2e_actor" (:event/actor event)) "event is ascribed to the OAuth actor")
    (is (= marker (get-in @observed* [:mcp.probe.local :marker]))
        "fixture action subscribed to the requested event sees the payload")
    (let [status (await (mcp/call-tool! client "events_status" {}))]
      (assert-ok! status)
      (is (false? (get-in (details status) [:runtime :running])) "manual dispatch can succeed with background runtimes stopped")
      (is (nil? (get-in (details status) [:runtime :events])) "status excludes unrelated recent event payloads"))
    (with-redefs [event-runtime/running? (constantly true)]
      (let [status (await (mcp/call-tool! client "events_status" {}))]
        (assert-ok! status)
        (is (true? (get-in (details status) [:runtime :running])) "MCP uses the lifecycle state")))))

(declare probe-messages!)

(defn- ^:async probe-spawn! [client accepted*]
  (doseq [model ["openai/gpt-test" "anthropic/claude-test"]]
    (let [reply (await (mcp/call-tool! client "agents_spawn"
                                      {:message "Fixture-only prompt."
                                       :model model}))
          accepted @accepted*
          identifiers (select-keys (details reply) [:run_id :session_id :conversation_id])
          args (select-keys identifiers [:run_id])
          status (await (mcp/call-tool! client "agents_status" args))
          other (await (mcp/call-tool! (assoc client :token "fixture-other-member") "agents_status" args))]
      (assert-ok! reply)
      (assert-ok! status)
      (is (= identifiers (select-keys (details status) [:run_id :session_id :conversation_id])))
      (is (= "queued" (:status (details status))) "a started MCP call is observable before worker setup")
      (is (false? (:can_steer (details status))))
      (is (nil? (get @runs/runs* (:run_id identifiers))) "read is backed by the durable provider")
      (is (= :tool-error (:status (mcp/call-outcome other))) "another membership cannot observe the queued run")
      (harness/report! (str "[mcp-control-probe] spawn model " model
                           "; response keys " (pr-str (keys (:result reply)))
                           "; turn principal " (pr-str (select-keys (:auth-context accepted)
                                                                      [:user-id :actor-id :membership-id]))))
      (is (= model (:model accepted)) "model override reaches the agent runtime boundary")
      (is (= "e2e-user" (authz/ctx-user-id (:auth-context accepted))) "OAuth user owns the spawned turn")
      (is (= identifiers {:run_id (:run-id accepted) :session_id (:session-id accepted)
                           :conversation_id (:conversation-id accepted)})
          "spawn returns the accepted run, session and conversation coordinates")
      (await (probe-messages! client accepted)))))

(defn- ^:async probe-failed-admission! [client]
  (let [calls* (atom 0)
        args {:message "Fixture-only prompt." :model "openai/gpt-test"}]
    (with-redefs [registry/session-store* (atom nil)
                  service/send-agent-turn! (fn [_ _ _] (swap! calls* inc))]
      (let [reply (await (mcp/call-tool! client "agents_spawn" args))]
        (is (= :tool-error (:status (mcp/call-outcome reply))))
        (is (nil? (:run_id (details reply))))
        (is (zero? @calls*) "a refused reservation cannot start a worker")))))

(defn- ^:async probe-failed-startup! [client]
  (let [start (admission/gate)
        settled (admission/gate)
        fixture-error (ex-info "Fixture startup did not finish" {})
        args {:message "Fixture-only prompt." :model "openai/gpt-test"}]
    (with-redefs [service/send-agent-turn! (^:async fn [_ _ _] (throw (await (:promise start))))
                  runner/log-and-record-async-spawn-error! (fn [_ _] ((:complete! settled) nil))]
      (let [reply (try
                    (await (promise/with-timeout-error (mcp/call-tool! client "agents_spawn" args)
                                                       2000 (js/Error. "Spawn waited for its worker")))
                    (finally ((:complete! start) fixture-error)))
            coordinates (select-keys (details reply) [:run_id :session_id :conversation_id])]
        (assert-ok! reply)
        (await (promise/with-timeout-error (:promise settled) 2000 (js/Error. "Startup failure did not settle")))
        (let [status (await (mcp/call-tool! client "agents_status" (select-keys coordinates [:run_id])))]
          (assert-ok! status)
          (is (= coordinates (select-keys (details status) [:run_id :session_id :conversation_id])))
          (is (= "failed" (:status (details status))))
          (is (false? (:can_steer (details status)))))))))

(defn- ^:async probe-mailbox-event! [client observed*]
  (let [marker (.randomUUID crypto)
        reply (await (mcp/call-tool! client "actors_send-message"
                                    {:target "actor:local_fixture_recipient"
                                     :mode "event"
                                     :content marker}))
        status (details reply)
        event (last @dispatch/recent-events*)]
    (assert-ok! reply)
    (is (= :actors.message (:event/type event)))
    (is (= "e2e_actor" (:event/actor event)))
    (is (= "actor" (get-in event [:event/generator :kind])))
    (is (= marker (get-in event [:event/payload :content])))
    (is (= "local_fixture_recipient" (get-in event [:event/payload :actorId])))
    (is (seq (:mailbox_id status)))
    (is (false? (:mailbox_durable status)))
    (is (= marker (get-in @observed* [:actors.message :content]))
        "fixture action subscribed to actor messages sees the content")
    (harness/report! (str "[mcp-control-probe] mailbox event emitted; "
                          "durable mailbox " (pr-str (:mailbox_durable status))
                          "; matched triggers " (pr-str (get-in status [:result :matchedTriggers]))))))

(defn- ^:async probe-boundaries! [client accepted*]
  (let [existing @accepted*
        spoof (await (mcp/call-tool! client "agents_spawn"
                                   {:message "must be refused"
                                    :agent_spec_json "{\"actor_id\":\"different_actor\"}"}))
        no-event-permission (await (mcp/call-tool! (assoc client :token "fixture-no-permission") "events_dispatch"
                                                 {:event_type "mcp.probe.local"}))
        limited-status (await (mcp/call-tool! (assoc client :token "fixture-no-permission") "events_status" {}))
        no-grant (await (mcp/list-tools! (assoc client :token "fixture-no-monitor")))
        no-run-permission (await (mcp/list-tools! (assoc client :token "fixture-no-permission")))]
    (is (= :tool-error (:status (mcp/call-outcome spoof))))
    (is (= existing @accepted*) "foreign actor spoof never started a turn")
    (is (= :tool-error (:status (mcp/call-outcome no-event-permission))))
    (assert-ok! limited-status)
    (is (contains? (:runtime (details limited-status)) :running))
    (is (nil? (:control (details limited-status))) "non-operators cannot enumerate event resources")
    (doseq [result [no-grant no-run-permission]]
      (is (:ok result))
      (is (not-any? #(= "agents_status" (:name %)) (get-in result [:result :tools]))))))

(defn- ^:async probe-valid-controls! [client args calls*]
  (let [run-id (:run_id args)]
    (doseq [[mode kind event-type] [["steer" :steer "steer_queued"]
                                    ["follow-up" :follow-up "follow_up_queued"]]]
      (let [reply (await (mcp/call-tool! client "actors_send-message" (assoc args :mode mode)))
            events (await (runs/get-run-events-since run-id ""))]
        (assert-ok! reply)
        (is (= [kind (:content args)] (last @calls*)))
        (is (= event-type (:type (last events)))
            "fixture session invocation has a same-run audit event")))
    (let [reply (await (mcp/call-tool! client "agents_status" {:run_id run-id}))
          status (details reply)]
      (assert-ok! reply)
      (is (= run-id (:run_id status)))
      (is (= "running" (:status status)))
      (is (true? (:can_steer status)))
      (is (= ["steer_queued" "follow_up_queued"] (mapv :type (:recent_events status))))
      (is (every? #(every? #{:event_id :sequence :type :at} (keys %)) (:recent_events status))))))

(defn- ^:async probe-control-rejections! [client args calls*]
  (doseq [stale [(assoc args :run_id "stale-probe-run")
                 (assoc args :session_id "stale-probe-session")]]
    (let [reply (await (mcp/call-tool! client "actors_send-message" (assoc stale :mode "steer")))]
      (is (= :tool-error (:status (mcp/call-outcome reply))) "stale control is rejected")
      (is (= 2 (count @calls*)) "stale control has no session effect")))
  (let [other (assoc client :token "fixture-other-member")
        monitor (await (mcp/call-tool! other "agents_status" {:run_id (:run_id args)}))
        steer (await (mcp/call-tool! other "actors_send-message" (assoc args :mode "steer")))]
    (is (= :tool-error (:status (mcp/call-outcome monitor))))
    (is (= :tool-error (:status (mcp/call-outcome steer))))
    (is (= 2 (count @calls*)) "cross-membership control has no session effect")))

(defn- ^:async probe-terminal-status! [client run-id]
  (let [terminal (runs/update-run! run-id #(assoc % :status "completed" :output_tokens 23))
        args {:run_id run-id}
        before (await (mcp/call-tool! client "agents_status" args))]
    (assert-ok! before)
    (is (= "running" (:status (details before))) "monitor waits for durable terminal state")
    (await (run-events/persist-run! terminal))
    (let [after (await (mcp/call-tool! client "agents_status" args))
          status (details after)]
      (assert-ok! after)
      (is (= "completed" (:status status)))
      (is (= 23 (:output_tokens status)))
      (is (false? (:can_steer status))))))

(defn- ^:async probe-messages! [client accepted]
  (let [{:keys [run-id session-id conversation-id auth-context model]} accepted
        identifiers {:run_id run-id :session_id session-id :conversation_id conversation-id}
        coordinates (merge identifiers (authz/auth-snapshot auth-context) {:model model})
        args (assoc identifiers :target (str "session:" session-id) :content "fixture steering marker")
        calls* (atom [])
        session (reify agent/IAgentSession
                  (streaming? [_] true)
                  (steer! [_ text] (swap! calls* conj [:steer text]) (js/Promise.resolve nil))
                  (follow-up! [_ text] (swap! calls* conj [:follow-up text]) (js/Promise.resolve nil)))]
    (await (fixture/with-run!
            coordinates
            (^:async fn []
              (with-redefs [turn-control/active-turn #(when (= % conversation-id)
                                                        (assoc coordinates :session session))
                            sessions/active-agent-session #(when (= % conversation-id) session)]
                (await (probe-valid-controls! client args calls*))
                (await (probe-control-rejections! client args calls*))
                (await (probe-terminal-status! client run-id))
                (harness/report! (str "[mcp-control-probe] " model
                                     " monitored and controlled; session callback count " (count @calls*)))))))))

(defn- fixture-triggers []
  (mapv (fn [[id event-type]]
          {:resource/kind :trigger
           :resource/id id
           :resource/definition {:contract/id id
                                 :trigger/events [event-type]
                                 :trigger/action :actions/mcp-control-fixture}})
        [["probe-dispatch" :mcp.probe.local] ["probe-mailbox" :actors.message]]))

(defn oauth-fixture [token]
  (let [membership-id (case token
                        "fixture-oauth-bearer" "e2e-membership"
                        "fixture-no-monitor" "e2e-membership"
                        "fixture-other-member" "other-member"
                        "fixture-no-permission" "read-only-member"
                        nil)]
    (when membership-id
      (js/JSON.stringify
       (clj->js {:membershipId membership-id
                 :userEmail "fixture@example.invalid"
                 :actorId "e2e_actor"
                 :tools (if (= token "fixture-no-monitor")
                          (filterv #(not= "agents_status" %) tool-grant)
                          tool-grant)})))))

(defn- ^:async probe-control-tools! [client]
  (let [catalog (await (mcp/list-tools! client))
        names (mapv :name (get-in catalog [:result :tools]))]
    (harness/report! (str "[mcp-control-probe] catalog " (pr-str names)))
    (is (every? (set names) ["events_status" "events_dispatch" "agents_spawn" "actors_send-message"]))
    (is (some #(re-find #"^(agents|runs)_(status|active|get|monitor|events)" %) names)
        "OAuth actor can monitor exact run state through MCP")))

(defn- ^:async probe-production-route-contract! []
  (let [accepted* (atom nil)
        observed* (atom {})
        client (await (start!))]
    (actions/register-action! :actions/mcp-control-fixture {}
                              (fn [{:keys [event]} _]
                                (swap! observed* assoc (:event/type event) (:event/payload event))
                                (js/Promise.resolve {:ok true})))
    (try
      (with-redefs [resources/load-all-resources-sync (fn [_] (fixture-triggers))
                    oauth-store/get-token! (fn
                                             ([token] (js/Promise.resolve (oauth-fixture token)))
                                             ([_ _] (js/Promise.resolve nil)))
                    policy/validate-chat-policy! (fn [_ _] (js/Promise.resolve nil))
                    service/send-agent-turn! (fn [_ _ request]
                                               (reset! accepted* request)
                                               (js/Promise.resolve nil))]
        (is (= 401 (:status (await (mcp/initialize! (dissoc client :token))))))
        (is (:ok (await (mcp/initialize! client))))
        (await (probe-control-tools! client))
        (testing "event dispatch fidelity" (await (probe-events! client observed*)))
        (testing "spawn ascription and coordinates" (await (probe-spawn! client accepted*)))
        (testing "failed durable admission" (await (probe-failed-admission! client)))
        (testing "failure after acknowledged admission" (await (probe-failed-startup! client)))
        (testing "grant and permissions" (await (probe-boundaries! client accepted*)))
        (testing "mailbox event mode" (await (probe-mailbox-event! client observed*))))
      (finally (await (server/close! (:app client)))))))

(deftest ^:async production-route-contract
  (await (fixture/with-run!
          {:run_id "fixture-seed-run" :session_id "fixture-seed-session"
           :conversation_id "fixture-seed-conversation"}
          probe-production-route-contract!)))
