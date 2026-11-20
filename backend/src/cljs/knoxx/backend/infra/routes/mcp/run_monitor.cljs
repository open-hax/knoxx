(ns knoxx.backend.infra.routes.mcp.run-monitor
  "MCP wire adapter for an authenticated, exact-run status read."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.text :as text]
            [knoxx.backend.extern.tools :as tools]
            [knoxx.backend.infra.agent.runtime :as runtime]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.run-queries :as queries]
            [knoxx.backend.law.run-store :as law]))

(def ^:private read-permissions
  ["agent.runs.read_own" "agent.runs.read_org" "agent.runs.read_all"])

(def ^:private summary-fields
  [:run_id :conversation_id :session_id :model :status :created_at :updated_at
   :ttft_ms :total_time_ms :input_tokens :output_tokens])

(def ^:private event-fields [:event_id :sequence :type :at])
(def ^:private retained-event-limit 20)

(defn- eligible?
  [ctx]
  (and (not (str/blank? (authz/ctx-membership-id ctx)))
       (or (authz/system-admin? ctx)
           (and (authz/ctx-permitted? ctx "agent.chat.use")
                (authz/ctx-any-permission? ctx read-permissions)))))

(defn- can-steer? [ctx run]
  (let [{:keys [run_id conversation_id session_id]} run]
    (and (= "running" (:status run))
         (= (authz/ctx-membership-id ctx) (authz/record-membership-id run))
         (authz/principal-match? ctx run)
         (or (authz/system-admin? ctx) (authz/ctx-permitted? ctx "agent.controls.steer"))
         (runtime/control-ready? conversation_id session_id run_id))))

(defn- run-summary
  [ctx {:keys [run events event-count]}]
  (assoc (select-keys run summary-fields)
         :can_steer (boolean (can-steer? ctx run))
         :recent_events (mapv #(select-keys % event-fields) events)
         :retained_event_count event-count))

(defn- ^:async execute!
  [ctx _config _call-id params _signal _on-update _extra]
  (let [run-id (law/require! law/NonBlank (aget params "run_id"))
        observation (await (queries/read-with-event-tail! ctx run-id retained-event-limit))
        summary (run-summary ctx observation)
        result (text/tool-text-result
                (str "Run " run-id " is " (:status summary)
                     (when-let [model (:model summary)] (str " (" model ")")))
                summary)]
    (aset result "structuredContent" (aget result "details"))
    result))

(defn tool
  "Build an exact-run monitor for this resolved membership. MCP registration
   separately intersects its name with the current token's explicit grant."
  [ctx]
  (when (eligible? ctx)
    (let [definition (tools/tool-definition
                      {:name "agents_status"
                       :label "Agent Run Status"
                       :description "Read one visible run's status, steering readiness, and up to 20 recent retained event headers."
                       :parameters-schema [:map [:run_id :string]]
                       :execute execute!
                       :runtime ctx})]
      (aset definition "annotations"
            #js {:readOnlyHint true
                 :destructiveHint false
                 :idempotentHint true
                 :openWorldHint false})
      definition)))
