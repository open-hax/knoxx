(ns knoxx.backend.tools.event-agents
  "Scheduled event-agent tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.agent-templates :as templates]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.http :as backend-http :refer [js-array-seq]]
            [knoxx.backend.runtime.defaults :refer [default-settings]]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.triggers.control-config :as control-config]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional json-parse live-config]]))

(defn- event-agent-status!
  [config]
  (let [live (live-config config)
        control (control-config/event-agent-control-config live)
        runtime (event-agents/status-snapshot live)]
    {:control control
     :runtime runtime}))

(defn- event-agent-dispatch!
  [source-kind event-kind payload]
  (event-agents/dispatch-event! {:sourceKind source-kind
                                 :eventKind event-kind
                                 :payload payload}))

(defn- event-agent-upsert-job!
  "Create or update an event-agent job with Redis-first persistence.

   Supports two modes:
   1. Template-based: job-patch contains :templateId keyword
   2. Direct spec: job-patch contains full job definition

   Writes to Redis (hot store) and marks dirty for SQL flush.
   Does NOT mutate in-memory config* - Redis is source of truth."
  [config job-id job-patch]
  (let [;; Check if this is a template-based instantiation
        template-id (or (:templateId job-patch)
                        (:template-id job-patch))

        ;; Build the complete job spec
        next-job (if template-id
                   ;; Template mode: instantiate from template DSL
                   (let [trigger (or (:trigger job-patch)
                                     {:kind "event" :cadenceMinutes 5 :eventKinds []})
                         source (or (:source job-patch)
                                    {:kind "manual" :mode "respond" :config {}})
                         filters (or (:filters job-patch)
                                     {:channels [] :keywords []})
                         overrides (dissoc job-patch :templateId :template-id :trigger :source :filters)]
                     (templates/instantiate-job template-id job-id trigger source filters overrides))
                   ;; Direct mode: merge with existing or use patch as-is
                   (let [live (live-config config)
                         current-control (control-config/event-agent-control-config live)
                         existing (some #(when (= (:id %) job-id) %) (:jobs current-control))]
                     (merge existing job-patch {:id job-id})))

        ;; Normalize for persistence (ensures thinking-level, timestamps, etc.)
        normalized-job (templates/normalize-job-for-persistence next-job)]

    (-> (event-agents/upsert-job! job-id normalized-job)
        (.then (fn [saved-job]
                 {:job saved-job
                  :message (str "Upserted job " job-id " to Redis (dirty queue for SQL flush)")
                  :templateId template-id
                  :thinkingLevel (get-in saved-job [:agentSpec :thinkingLevel])})))))

;;; ========================================================================
;;; Music/Audio Tools
;;; ========================================================================



(defn create-event-agent-custom-tools
  ([runtime config] (create-event-agent-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         status-params (.Object Type #js {})
         run-job-params (.Object Type
                                 #js {:job_id (.String Type #js {:description "Event-agent job id to run immediately."})})
         dispatch-params (.Object Type
                                  #js {:source_kind (.String Type #js {:description "Event source kind such as manual, discord, github, or cron."})
                                       :event_kind (.String Type #js {:description "Event kind string such as manual.note or discord.message.keyword."})
                                       :payload_json (type-optional Type (.String Type #js {:description "Optional JSON object payload for the event."}))})
         upsert-job-params (.Object Type
                                    #js {:job_id (.String Type #js {:description "Unique event-agent job id."})
                                         :job_json (.String Type #js {:description "JSON object describing the event-agent job. Fields may include name, description, enabled, trigger, source, filters, and agentSpec."})})

         event-agent-status-execute (fn [_tool-call-id _params a b c]
                                      (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                            result (event-agent-status! config)
                                            control (:control result)
                                            runtime (:runtime result)]
                                        (maybe-tool-update! on-update "Reading event-agent runtime state…")
                                        (tool-text-result
                                         (str "Event-agent runtime running=" (:running runtime)
                                              ", jobs=" (count (:jobs runtime))
                                              "\n\n"
                                              (str/join "\n"
                                                        (map (fn [job]
                                                               (str (:id job) " :: trigger=" (get-in job [:trigger :kind])
                                                                    " cadence=" (get-in job [:trigger :cadenceMinutes])
                                                                    " enabled=" (:enabled job)))
                                                             (:jobs control))))
                                         result)))

         event-agent-run-job-execute (fn [_tool-call-id params a b c]
                                       (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                             job-id (or (aget params "job_id") (aget params "jobId") "")]
                                         (when (str/blank? job-id)
                                           (throw (js/Error. "job_id is required")))
                                         (maybe-tool-update! on-update (str "Running event-agent job " job-id "…"))
                                         (-> (event-agents/run-job! job-id)
                                             (.then (fn [_]
                                                      (tool-text-result (str "Triggered event-agent job " job-id)
                                                                        {:jobId job-id
                                                                         :ok true}))))))

         event-agent-dispatch-execute (fn [_tool-call-id params a b c]
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
         ]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "event_agents.status")
                  (doto (js-obj)
                    (aset "name" "event_agents.status")
                    (aset "label" "Event Agent Status")
                    (aset "description" "Inspect the current scheduled event-agent runtime configuration and live state.")
                    (aset "promptSnippet" "Inspect event-agent jobs, sources, and runtime state before changing schedules or dispatching events.")
                    (aset "promptGuidelines" (clj->js ["Use this before mutating jobs so you understand the current state."
                                                       "Helpful for debugging cron/event behavior or checking recent events."]))
                    (aset "parameters" status-params)
                    (aset "execute" event-agent-status-execute)))
                (when (allowed? "event_agents.dispatch")
                  (doto (js-obj)
                    (aset "name" "event_agents.dispatch")
                    (aset "label" "Event Agent Dispatch")
                    (aset "description" "Dispatch a structured event into the generic event-agent runtime.")
                    (aset "promptSnippet" "Dispatch manual or synthetic events so matching event-agent jobs can react immediately.")
                    (aset "promptGuidelines" (clj->js ["Use source_kind/manual for synthetic triggers you want to test immediately."
                                                       "Put complex payload fields into payload_json as a JSON object string."]))
                    (aset "parameters" dispatch-params)
                    (aset "execute" event-agent-dispatch-execute)))
                (when (allowed? "event_agents.run_job")
                  (doto (js-obj)
                    (aset "name" "event_agents.run_job")
                    (aset "label" "Event Agent Run Job")
                    (aset "description" "Run a configured event-agent job immediately without waiting for its schedule.")
                    (aset "promptSnippet" "Trigger an event-agent job now.")
                    (aset "promptGuidelines" (clj->js ["Use this for manual patrol/synthesis/response runs after inspecting status."
                                                       "Provide the exact job id."]))
                    (aset "parameters" run-job-params)
                    (aset "execute" event-agent-run-job-execute)))
                (when (allowed? "event_agents.upsert_job")
                  (let [tool (js-obj)]
                    (aset tool "name" "event_agents.upsert_job")
                    (aset tool "label" "Event Agent Upsert Job")
                    (aset tool "description" "Create or update a scheduled event-agent job, then reload the runtime.")
                    (aset tool "promptSnippet" "Create or update a generic scheduled event-agent job using JSON job config.")
                    (aset tool "promptGuidelines" (clj->js ["Use this to create new cron/event-driven agents or revise existing jobs."
                                                            "Pass a full JSON job object in job_json; include trigger, source, filters, and agentSpec when you need precise control."]))
                    (aset tool "parameters" upsert-job-params)
                    (aset tool "execute"
                          (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  job-id (or (aget params "job_id") (aget params "jobId") "")
                                  job-json (or (aget params "job_json") (aget params "jobJson") "")]
                              (when (str/blank? job-id)
                                (throw (js/Error. "job_id is required")))
                              (when (str/blank? (str job-json))
                                (throw (js/Error. "job_json is required")))
                              (maybe-tool-update! on-update (str "Upserting event-agent job " job-id "…"))
                              (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
                                (tool-text-result (str "Upserted event-agent job " job-id)
                                                  result)))))
                    tool))
                (when (allowed? "schedule_event_agent")
                  (let [tool (js-obj)]
                    (aset tool "name" "schedule_event_agent")
                    (aset tool "label" "Schedule Event Agent")
                    (aset tool "description" "Create or update a scheduled event-agent job with explicit trigger, source, prompts, and tool policies.")
                    (aset tool "promptSnippet" "Schedule an event-driven agent job that can react to Discord, GitHub, cron, or manual events.")
                    (aset tool "promptGuidelines" (clj->js ["Use this when the user wants to create or revise an event-based agent from conversation."
                                                            "Provide a full job object in job_json, including trigger, source, filters, and agentSpec."
                                                            "Use role slugs like translator, system_admin, or executive and include explicit toolPolicies so the scheduled agent has exactly the tools it needs."]))
                    (aset tool "parameters" upsert-job-params)
                    (aset tool "execute"
                          (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  job-id (or (aget params "job_id") (aget params "jobId") "")
                                  job-json (or (aget params "job_json") (aget params "jobJson") "")]
                              (when (str/blank? job-id)
                                (throw (js/Error. "job_id is required")))
                              (when (str/blank? (str job-json))
                                (throw (js/Error. "job_json is required")))
                              (maybe-tool-update! on-update (str "Scheduling event-agent job " job-id "…"))
                              (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
                                (tool-text-result (str "Scheduled event-agent job " job-id)
                                                  result)))))
                    tool))]))))))
