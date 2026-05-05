(ns knoxx.backend.tools.event-agents
  "Scheduled event-agent tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.agent-templates :as templates]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.triggers.control-config :as control-config]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj json-parse live-config]]))

(def status-params [:map])

(def run-job-params
  [:map
   [:job_id {:description "Event-agent job id to run immediately."} :string]])

(def dispatch-params
  [:map
   [:source_kind {:description "Event source kind such as manual, discord, github, or cron."} :string]
   [:event_kind {:description "Event kind string such as manual.note or discord.message.keyword."} :string]
   [:payload_json {:optional true :description "Optional JSON object payload for the event."} :string]])

(def upsert-job-params
  [:map
   [:job_id {:description "Unique event-agent job id."} :string]
   [:job_json {:description "JSON object describing the event-agent job. Fields may include name, description, enabled, trigger, source, filters, and agentSpec."} :string]])

(defn- event-agent-status! [config]
  (let [live (live-config config)
        control (control-config/event-agent-control-config live)
        runtime (event-agents/status-snapshot live)]
    {:control control :runtime runtime}))

(defn- event-agent-dispatch! [source-kind event-kind payload]
  (event-agents/dispatch-event! {:sourceKind source-kind :eventKind event-kind :payload payload}))

(defn- event-agent-upsert-job! [config job-id job-patch]
  (let [template-id (or (:templateId job-patch) (:template-id job-patch))
        next-job (if template-id
                   (let [trigger (or (:trigger job-patch) {:kind "event" :cadenceMinutes 5 :eventKinds []})
                         source (or (:source job-patch) {:kind "manual" :mode "respond" :config {}})
                         filters (or (:filters job-patch) {:channels [] :keywords []})
                         overrides (dissoc job-patch :templateId :template-id :trigger :source :filters)]
                     (templates/instantiate-job template-id job-id trigger source filters overrides))
                   (let [live (live-config config)
                         current-control (control-config/event-agent-control-config live)
                         existing (some #(when (= (:id %) job-id) %) (:jobs current-control))]
                     (merge existing job-patch {:id job-id})))
        normalized-job (templates/normalize-job-for-persistence next-job)]
    (-> (event-agents/upsert-job! job-id normalized-job)
        (.then (fn [saved-job]
                 {:job saved-job
                  :message (str "Upserted job " job-id " to Redis (dirty queue for SQL flush)")
                  :templateId template-id
                  :thinkingLevel (get-in saved-job [:agentSpec :thinkingLevel])})))))

(defn event-agent-status-execute [_runtime config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        result (event-agent-status! config)
        control (:control result)
        runtime-state (:runtime result)]
    (maybe-tool-update! on-update "Reading event-agent runtime state…")
    (tool-text-result
     (str "Event-agent runtime running=" (:running runtime-state)
          ", jobs=" (count (:jobs runtime-state))
          "\n\n"
          (str/join "\n"
                    (map (fn [job]
                           (str (:id job) " :: trigger=" (get-in job [:trigger :kind])
                                " cadence=" (get-in job [:trigger :cadenceMinutes])
                                " enabled=" (:enabled job)))
                         (:jobs control))))
     result)))

(defn event-agent-run-job-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (maybe-tool-update! on-update (str "Running event-agent job " job-id "…"))
    (-> (event-agents/run-job! job-id)
        (.then (fn [_]
                 (tool-text-result (str "Triggered event-agent job " job-id)
                                   {:jobId job-id :ok true}))))))

(defn event-agent-dispatch-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        source-kind (or (aget params "source_kind") (aget params "sourceKind") "manual")
        event-kind (or (aget params "event_kind") (aget params "eventKind") "manual.event")
        payload-json (or (aget params "payload_json") (aget params "payloadJson") "")
        payload (if (str/blank? (str payload-json)) {} (json-parse (str payload-json)))]
    (maybe-tool-update! on-update (str "Dispatching event " event-kind "…"))
    (-> (event-agent-dispatch! source-kind event-kind payload)
        (.then (fn [result]
                 (tool-text-result (str "Dispatched event " event-kind " matched jobs: " (str/join ", " (:matchedJobs result)))
                                   result))))))

(defn event-agent-upsert-job-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")
        job-json (or (aget params "job_json") (aget params "jobJson") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (when (str/blank? (str job-json))
      (throw (js/Error. "job_json is required")))
    (maybe-tool-update! on-update (str "Upserting event-agent job " job-id "…"))
    (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
      (tool-text-result (str "Upserted event-agent job " job-id) result))))

(defn schedule-event-agent-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")
        job-json (or (aget params "job_json") (aget params "jobJson") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (when (str/blank? (str job-json))
      (throw (js/Error. "job_json is required")))
    (maybe-tool-update! on-update (str "Scheduling event-agent job " job-id "…"))
    (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
      (tool-text-result (str "Scheduled event-agent job " job-id) result))))

(def event-agent-status-tool
  (partial create-tool-obj
           "event_agents.status"
           "Event Agent Status"
           "Inspect the current scheduled event-agent runtime configuration and live state."
           "Inspect event-agent jobs, sources, and runtime state before changing schedules or dispatching events."
           ["Use this before mutating jobs so you understand the current state."
            "Helpful for debugging cron/event behavior or checking recent events."]
           status-params
           event-agent-status-execute))

(def event-agent-dispatch-tool
  (partial create-tool-obj
           "event_agents.dispatch"
           "Event Agent Dispatch"
           "Dispatch a structured event into the generic event-agent runtime."
           "Dispatch manual or synthetic events so matching event-agent jobs can react immediately."
           ["Use source_kind/manual for synthetic triggers you want to test immediately."
            "Put complex payload fields into payload_json as a JSON object string."]
           dispatch-params
           event-agent-dispatch-execute))

(def event-agent-run-job-tool
  (partial create-tool-obj
           "event_agents.run_job"
           "Event Agent Run Job"
           "Run a configured event-agent job immediately without waiting for its schedule."
           "Trigger an event-agent job now."
           ["Use this for manual patrol/synthesis/response runs after inspecting status."
            "Provide the exact job id."]
           run-job-params
           event-agent-run-job-execute))

(def event-agent-upsert-job-tool
  (partial create-tool-obj
           "event_agents.upsert_job"
           "Event Agent Upsert Job"
           "Create or update a scheduled event-agent job, then reload the runtime."
           "Create or update a generic scheduled event-agent job using JSON job config."
           ["Use this to create new cron/event-driven agents or revise existing jobs."
            "Pass a full JSON job object in job_json; include trigger, source, filters, and agentSpec when you need precise control."]
           upsert-job-params
           event-agent-upsert-job-execute))

(def schedule-event-agent-tool
  (partial create-tool-obj
           "schedule_event_agent"
           "Schedule Event Agent"
           "Create or update a scheduled event-agent job with explicit trigger, source, prompts, and tool policies."
           "Schedule an event-driven agent job that can react to Discord, GitHub, cron, or manual events."
           ["Use this when the user wants to create or revise an event-based agent from conversation."
            "Provide a full job object in job_json, including trigger, source, filters, and agentSpec."
            "Use role slugs like translator, system_admin, or executive and include explicit toolPolicies so the scheduled agent has exactly the tools it needs."]
           upsert-job-params
           schedule-event-agent-execute))

(defn create-event-agent-custom-tools
  ([runtime config] (create-event-agent-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "event_agents.status")
                  (event-agent-status-tool runtime config))
                (when (allowed? "event_agents.dispatch")
                  (event-agent-dispatch-tool runtime config))
                (when (allowed? "event_agents.run_job")
                  (event-agent-run-job-tool runtime config))
                (when (allowed? "event_agents.upsert_job")
                  (event-agent-upsert-job-tool runtime config))
                (when (allowed? "schedule_event_agent")
                  (schedule-event-agent-tool runtime config))]))))))
