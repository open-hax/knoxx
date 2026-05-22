(ns knoxx.backend.domain.event.tools
  "Custom-tool entrypoint for the generic events/triggers/actions surface."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.clients.knoxx-control :as knoxx-client]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj json-parse live-config]]))

(def status-params [:map])

(def trigger-fire-params
  [:map
   [:trigger_id {:description "Trigger resource id to exercise immediately."} :string]])

(def dispatch-params
  [:map
   [:generator_kind {:optional true :description "Event generator/provenance kind such as manual, discord, github, or cron."} :string]
   [:event_type {:description "Event type string such as manual.note or discord.message."} :string]
   [:payload_json {:optional true :description "Optional JSON object payload for the event."} :string]])

(def spawn-agent-params
  [:map
   [:message {:description "User message or task to give the one-off agent."} :string]
   [:model {:optional true :description "Optional model id override."} :string]
   [:agent_spec_json {:optional true :description "Optional JSON object with direct-start style agent_spec fields such as role, contract_id, actor_id, system_prompt, task_prompt, thinking_level, and tool_policies."} :string]])

(defn- fetch-json!
  [config method path body]
  (knoxx-client/request-json! (knoxx-client/client (live-config config)) method path body))

(defn- ^:async events-status!
  [config]
  (js->clj (await (fetch-json! config "GET" "/api/admin/config/events" nil))
           :keywordize-keys true))

(defn- ^:async events-dispatch!
  [config generator-kind event-type payload]
  (js->clj (await (fetch-json! config "POST" "/api/admin/config/events/dispatch"
                               {:event/generator (cond-> {}
                                                   (not (str/blank? (str generator-kind)))
                                                   (assoc :kind generator-kind))
                                :event/type event-type
                                :event/payload payload}))
           :keywordize-keys true))

(defn- ^:async trigger-fire!
  [config trigger-id]
  (js->clj (await (fetch-json! config "POST" (str "/api/admin/triggers/" (js/encodeURIComponent trigger-id) "/fire") nil))
           :keywordize-keys true))

(defn- ^:async events-status-execute [_runtime config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
    (maybe-tool-update! on-update "Reading events runtime state…")
    (let [result (await (events-status! config))
          control (:control result)
          runtime-state (:runtime result)
          resources (:resources control)
          triggers (:trigger resources)]
      (tool-text-result
       (str "Events runtime running=" (:running runtime-state)
            ", triggers=" (count triggers)
            ", actions=" (count (:action resources))
            ", schedules=" (count (:schedule resources))
            ", generators=" (count (:generator resources)))
       result))))

(defn- ^:async trigger-fire-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        trigger-id (or (aget params "trigger_id") (aget params "triggerId") "")]
    (when (str/blank? trigger-id)
      (throw (js/Error. "trigger_id is required")))
    (maybe-tool-update! on-update (str "Firing trigger " trigger-id "…"))
    (let [result (await (trigger-fire! config trigger-id))]
      (tool-text-result (str "Fired trigger " trigger-id) result))))

(defn- ^:async events-dispatch-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        generator-kind (or (aget params "generator_kind") (aget params "generatorKind") "manual")
        event-type (or (aget params "event_type") (aget params "eventType") "manual.event")
        payload-json (or (aget params "payload_json") (aget params "payloadJson") "")
        payload (if (str/blank? (str payload-json)) {} (json-parse (str payload-json)))]
    (maybe-tool-update! on-update (str "Dispatching event " event-type "…"))
    (let [result (await (events-dispatch! config generator-kind event-type payload))]
      (tool-text-result (str "Dispatched event " event-type " matched triggers: " (str/join ", " (:matchedTriggers result)))
                        result))))

(defn- ^:async agent-spawn-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        message (or (aget params "message") "")
        model (or (aget params "model") nil)
        agent-spec-json (or (aget params "agent_spec_json") (aget params "agentSpecJson") "")
        agent-spec (if (str/blank? (str agent-spec-json)) {} (json-parse (str agent-spec-json)))]
    (when (str/blank? (str message))
      (throw (js/Error. "message is required")))
    (maybe-tool-update! on-update "Spawning one-off agent run…")
    (let [result (await (fetch-json! config "POST" "/api/knoxx/direct/start" {:message message
                                                                              :model model
                                                                              :agent_spec agent-spec}))
          result* (js->clj result :keywordize-keys true)]
      (tool-text-result (str "Spawned one-off agent run " (:run_id result*))
                        result*))))

(def events-status-tool
  (partial create-tool-obj
           "events.status"
           "Events Status"
           "Inspect the current events runtime state and resource catalog."
           "Inspect events, triggers, actions, schedules, generators, and runtime state."
           ["Use this before dispatching events, firing triggers, or resetting the runtime."]
           status-params
           events-status-execute))

(def events-dispatch-tool
  (partial create-tool-obj
           "events.dispatch"
           "Events Dispatch"
           "Dispatch a normalized event onto the events runtime."
           "Publish a manual or synthetic event so matching trigger contracts can react immediately."
           ["Use generator_kind/manual for synthetic events you want to test immediately."
            "Put complex payload fields into payload_json as a JSON object string."]
           dispatch-params
           events-dispatch-execute))

(def trigger-fire-tool
  (partial create-tool-obj
           "triggers.fire"
           "Triggers Fire"
           "Exercise a trigger resource immediately."
           "Dispatch one of a trigger resource's observed events through the event runtime."
           ["Use this for manual trigger testing after inspecting events.status."
            "Provide the exact trigger resource id."]
           trigger-fire-params
           trigger-fire-execute))

(def agents-spawn-tool
  (partial create-tool-obj
           "agents.spawn"
           "Agents Spawn"
           "Launch a one-off Knoxx agent run without creating or mutating trigger/schedule contracts."
           "Spawn a normal Knoxx agent directly through the shared agent runtime."
           ["Use this for one-off agent execution."
            "Pass direct-start style agent overrides in agent_spec_json when you need a specific role, contract, actor, or tool policy surface."]
           spawn-agent-params
           agent-spawn-execute))

(defn create-events-custom-tools
  ([runtime config] (create-events-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "events.status")
                  (events-status-tool runtime config))
                (when (allowed? "events.dispatch")
                  (events-dispatch-tool runtime config))
                (when (allowed? "triggers.fire")
                  (trigger-fire-tool runtime config))
                (when (allowed? "agents.spawn")
                  (agents-spawn-tool runtime config))]))))))
