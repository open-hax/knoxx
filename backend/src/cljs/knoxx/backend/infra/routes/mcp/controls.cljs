(ns knoxx.backend.infra.routes.mcp.controls
  "Execute MCP control tools using the resolved caller."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.actor.tools :as actor-tools]
            [knoxx.backend.domain.text :as text]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.tools :as tools]
            [knoxx.backend.infra.agent.provider.native-subscription :as native-subscription]
            [knoxx.backend.infra.agent.service :as agent-service]
            [knoxx.backend.infra.agent.turn :as agent-turn]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.infra.routes.app :as app-routes]
            [knoxx.backend.infra.routes.mcp.params :as params]
            [knoxx.backend.infra.routes.tools :as tool-routes]
            [knoxx.backend.infra.run-queries :as run-queries]
            [knoxx.backend.shape.app-shapes :as app-shapes]))

(defn- require-mcp-actor!
  [{:keys [auth-context actor-id]}]
  (when (or (str/blank? actor-id) (str/blank? (authz/ctx-membership-id auth-context)))
    (throw (params/http-error 403 "mcp_actor_required" "An authorized MCP actor is required")))
  (assoc auth-context :actor-id actor-id :actorId actor-id))

(defn- parsed-object [value]
  (if (str/blank? value)
    {}
    (let [parsed (tools/parse-json value)]
      (when-not (map? parsed)
        (throw (params/http-error 400 "invalid_tool_arguments" "Expected a JSON object")))
      parsed)))

(defn- structured-result [description data]
  (let [result (text/tool-text-result description data)]
    (aset result "structuredContent" (aget result "details"))
    result))

(defn- ^:async dispatch-mcp-event!
  [config ctx body]
  (let [result (await (tool-routes/dispatch-operator-event! config ctx body))]
    (when-not (:ok result)
      (throw (params/http-error 500 "event_dispatch_failed" (:detail result))))
    result))

(defn- ^:async authorize-message-target!
  [ctx mode {:keys [conversation-id run-id session-id actor-id] :as target}]
  (if (= "event" mode)
    (do (authz/ensure-permission! ctx "org.events.control") target)
    (do
      (authz/ensure-permission! ctx (if (= "steer" mode) "agent.controls.steer" "agent.controls.follow_up"))
      (agent-turn/ensure-conversation-access! ctx conversation-id)
      (let [active (turn-control/active-turn conversation-id)]
        (when (or (str/blank? (:run_id active))
                  (str/blank? (:session_id active))
                  (and run-id (not= run-id (:run_id active)))
                  (and session-id (not= session-id (:session_id active))))
          (throw (params/http-error 409 "active_turn_mismatch" "Control target does not match the active run and session")))
        (let [run (await (run-queries/read! ctx (:run_id active)))]
          (when-not (and (= conversation-id (:conversation_id run))
                         (= (:session_id active) (:session_id run))
                         (= (authz/ctx-membership-id ctx) (authz/record-membership-id run))
                         (authz/principal-match? ctx run)
                         (or (nil? actor-id) (= actor-id (authz/record-actor-id run))))
            (throw (params/http-error 403 "control_scope_denied" "Run is outside the MCP actor's control scope")))
          (assoc target :run-id (:run_id active) :session-id (:session_id active)))))))

(defn- ^:async start-agent!
  "Admit a one-off turn for the MCP actor through the direct-start policy."
  [runtime config ctx actor-id message model agent-spec]
  (authz/ensure-permission! ctx "agent.chat.use")
  (when (str/blank? message)
    (throw (params/http-error 400 "message_required" "message is required")))
  (let [spec (app-shapes/normalize-chat-body (clj->js {:agent_spec agent-spec}))
        requested (:agent-spec spec)
        safe-keys [:actor-id :system-prompt :task-prompt :model :thinking-level]]
    (when (or (and (:actor-id requested) (not= actor-id (:actor-id requested)))
              (some (fn [[k value]] (and (not (some #{k} safe-keys)) (some? value) (not= [] value)))
                    requested))
      (throw (params/http-error 403 "agent_spec_forbidden" "MCP agent overrides exceed the actor's scope")))
    (let [{:keys [agent-ctx policy-model body accepted-response]}
          (app-routes/direct-turn-request config (native-subscription/with-mcp-authorization ctx)
                                          {:message message :model model
                                           :agent-spec (assoc (select-keys requested safe-keys)
                                                              :actor-id actor-id)})]
      (await (app-routes/admit-direct-turn! runtime config agent-ctx policy-model body
                                 accepted-response "Async MCP agent start failed")))))

(defn- event-status
  [config ctx]
  (let [result (tool-routes/mcp-events-status config)
        operator? (or (authz/system-admin? ctx) (authz/ctx-permitted? ctx "org.events.control"))]
    (structured-result (str "Events runtime running=" (get-in result [:runtime :running]))
                       (cond-> result (not operator?) (dissoc :control)))))

(defn- ^:async dispatch-event!
  [config acting params]
  (let [ctx (require-mcp-actor! acting)
        event-type (aget params "event_type")
        result (await (dispatch-mcp-event!
                       config ctx {:event/type event-type
                                   :event/generator {:kind (or (aget params "generator_kind") "manual")}
                                   :event/payload (parsed-object (aget params "payload_json"))}))]
    (structured-result (str "Dispatched event " event-type) result)))

(defn- ^:async send-message!
  [runtime config acting params]
  (let [ctx (require-mcp-actor! acting)
        result (await (actor-tools/deliver-message!
                       runtime config params nil {:actor-id (:actor-id acting)}
                       {:authorize! (partial authorize-message-target! ctx)
                        :dispatch! (partial dispatch-mcp-event! config ctx)
                        :control! (fn [mode control]
                                    (agent-service/queue-agent-control!
                                     runtime config
                                     (assoc control :kind (if (= "steer" mode) "steer" "follow_up"))))}))]
    (aset result "structuredContent" (aget result "details"))
    result))

(defn- ^:async fire-trigger!
  [config acting params]
  (let [ctx (require-mcp-actor! acting)
        trigger-id (aget params "trigger_id")]
    (authz/ensure-permission! ctx "org.events.control")
    (let [result (await (event-runtime/fire-trigger-external! config trigger-id))]
      (when (some :failed (:results result))
        (throw (params/http-error 500 "trigger_fire_failed" "One or more trigger actions failed")))
      (structured-result (str "Fired trigger " trigger-id) result))))

(defn ^:async execute!
  "Run a granted tool, applying caller identity to control operations."
  [runtime config acting ^js tool params]
  (case (aget tool "name")
    "events_status" (event-status config (:auth-context acting))
    "agents_spawn" (let [ctx (require-mcp-actor! acting)
                         result (await (start-agent! runtime config ctx (:actor-id acting)
                                                     (aget params "message") (aget params "model")
                                                     (parsed-object (aget params "agent_spec_json"))))]
                     (structured-result (str "Spawned one-off agent run " (:run_id result)) result))
    "events_dispatch" (await (dispatch-event! config acting params))
    "actors_send-message" (await (send-message! runtime config acting params))
    "triggers_fire" (await (fire-trigger! config acting params))
    (await (.execute tool "mcp" params nil nil nil))))
