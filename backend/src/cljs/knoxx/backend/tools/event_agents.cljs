(ns knoxx.backend.tools.event-agents
  "Scheduled event-agent tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.agent-templates :as templates]
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

(def spawn-agent-params
  [:map
   [:message {:description "User message or task to give the one-off agent."} :string]
   [:model {:optional true :description "Optional model id override."} :string]
   [:agent_spec_json {:optional true :description "Optional JSON object with direct-start style agent_spec fields such as role, contract_id, actor_id, system_prompt, task_prompt, thinking_level, and tool_policies."} :string]])


(defn- self-headers
  [config]
  (let [api-key (:knoxx-api-key (live-config config))]
    (cond-> #js {"Content-Type" "application/json"
                 "x-knoxx-user-email" "system-admin@open-hax.local"}
      (not (str/blank? (str api-key)))
      (aset "X-API-Key" api-key))))

(defn- api-base
  [config]
  (or (:knoxx-base-url (live-config config))
      "http://127.0.0.1:8000"))

(defn- fetch-json!
  [config method path body]
  (-> (js/fetch (str (api-base config) path)
                #js {:method method
                     :headers (self-headers config)
                     :body (when body (.stringify js/JSON (clj->js body)))})
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- event-agent-status!
  [config]
  (-> (fetch-json! config "GET" "/api/admin/config/events" nil)
      (.then (fn [result]
               (js->clj result :keywordize-keys true)))))

(defn- event-agent-dispatch!
  [config source-kind event-kind payload]
  (-> (fetch-json! config "POST" "/api/admin/config/events/dispatch"
                   {:sourceKind source-kind :eventKind event-kind :payload payload})
      (.then (fn [result] (js->clj result :keywordize-keys true)))))

(defn- event-agent-upsert-job! [config job-id job-patch]
  (let [template-id (or (:templateId job-patch) (:template-id job-patch))
        next-job (if template-id
                   (let [trigger (or (:trigger job-patch) {:kind "event" :cadenceMinutes 5 :eventKinds []})
                         source (or (:source job-patch) {:kind "manual" :mode "respond" :config {}})
                         filters (or (:filters job-patch) {:channels [] :keywords []})
                         overrides (dissoc job-patch :templateId :template-id :trigger :source :filters)]
                     (templates/instantiate-job template-id job-id trigger source filters overrides))
                   (merge job-patch {:id job-id}))
        normalized-job (templates/normalize-job-for-persistence next-job)]
    (-> (event-agent-status! config)
        (.then
         (fn [status]
           (let [current-control (:control status)
                 jobs (vec (or (:jobs current-control) []))
                 existing (some #(when (= (:id %) job-id) %) jobs)
                 merged-job (merge existing normalized-job)
                 next-jobs (->> jobs
                                (remove #(= (:id %) job-id))
                                (concat [merged-job])
                                vec)
                 next-control (assoc current-control :jobs next-jobs)]
             (-> (fetch-json! config "PUT" "/api/admin/config/events" next-control)
                 (.then (fn [_]
                          {:job merged-job
                           :message (str "Upserted job " job-id)
                           :templateId template-id
                           :thinkingLevel (get-in merged-job [:agentSpec :thinkingLevel])})))))))))

(defn event-agent-status-execute [_runtime config _tool-call-id _params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        result-promise (event-agent-status! config)]
    (maybe-tool-update! on-update "Reading event-agent runtime state…")
    (-> result-promise
        (.then (fn [result]
                 (let [control (:control result)
                       runtime-state (:runtime result)]
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
                    result)))))))

(defn event-agent-run-job-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (maybe-tool-update! on-update (str "Running event-agent job " job-id "…"))
    (-> (fetch-json! config "POST" (str "/api/admin/config/events/jobs/" (js/encodeURIComponent job-id) "/run") nil)
        (.then (fn [_]
                 (tool-text-result (str "Triggered event-agent job " job-id)
                                   {:jobId job-id :ok true}))))))

(defn event-agent-dispatch-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        source-kind (or (aget params "source_kind") (aget params "sourceKind") "manual")
        event-kind (or (aget params "event_kind") (aget params "eventKind") "manual.event")
        payload-json (or (aget params "payload_json") (aget params "payloadJson") "")
        payload (if (str/blank? (str payload-json)) {} (json-parse (str payload-json)))]
    (maybe-tool-update! on-update (str "Dispatching event " event-kind "…"))
    (-> (event-agent-dispatch! config source-kind event-kind payload)
        (.then (fn [result]
                 (tool-text-result (str "Dispatched event " event-kind " matched jobs: " (str/join ", " (:matchedJobs result)))
                                   result))))))

(defn agent-spawn-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        message (or (aget params "message") "")
        model (or (aget params "model") nil)
        agent-spec-json (or (aget params "agent_spec_json") (aget params "agentSpecJson") "")
        agent-spec (if (str/blank? (str agent-spec-json)) {} (json-parse (str agent-spec-json)))]
    (when (str/blank? (str message))
      (throw (js/Error. "message is required")))
    (maybe-tool-update! on-update "Spawning one-off agent run…")
    (-> (fetch-json! config "POST" "/api/knoxx/direct/start" {:message message
                                                                 :model model
                                                                 :agent_spec agent-spec})
        (.then (fn [result]
                 (let [result* (js->clj result :keywordize-keys true)]
                   (tool-text-result (str "Spawned one-off agent run " (:run_id result*))
                                     result*)))))))

(defn event-agent-upsert-job-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")
        job-json (or (aget params "job_json") (aget params "jobJson") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (when (str/blank? (str job-json))
      (throw (js/Error. "job_json is required")))
    (maybe-tool-update! on-update (str "Upserting event-agent job " job-id "…"))
    (-> (event-agent-upsert-job! config job-id (json-parse (str job-json)))
        (.then (fn [result]
                 (tool-text-result (str "Upserted event-agent job " job-id) result))))))

(defn schedule-event-agent-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        job-id (or (aget params "job_id") (aget params "jobId") "")
        job-json (or (aget params "job_json") (aget params "jobJson") "")]
    (when (str/blank? job-id)
      (throw (js/Error. "job_id is required")))
    (when (str/blank? (str job-json))
      (throw (js/Error. "job_json is required")))
    (maybe-tool-update! on-update (str "Scheduling event-agent job " job-id "…"))
    (-> (event-agent-upsert-job! config job-id (json-parse (str job-json)))
        (.then (fn [result]
                 (tool-text-result (str "Scheduled event-agent job " job-id) result))))))

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

(def events-status-tool
  (partial create-tool-obj
           "events.status"
           "Events Status"
           "Inspect the current generic events runtime state and trigger configuration."
           "Inspect event sources, triggers, and runtime state before dispatching or resetting events."
           ["Use this before dispatching events or resetting the scheduler."
            "This is the preferred replacement vocabulary for event_agents.status."]
           status-params
           event-agent-status-execute))

(def events-dispatch-tool
  (partial create-tool-obj
           "events.dispatch"
           "Events Dispatch"
           "Dispatch a normalized event onto the generic events runtime."
           "Publish a manual or synthetic event so matching triggers can react immediately."
           ["Use this instead of event_agents.dispatch for the new events vocabulary."
            "Put complex payload fields into payload_json as a JSON object string."]
           dispatch-params
           event-agent-dispatch-execute))

(def agents-spawn-tool
  (partial create-tool-obj
           "agents.spawn"
           "Agents Spawn"
           "Launch a one-off Knoxx agent run without creating or mutating an event trigger job."
           "Spawn a normal Knoxx agent directly through the shared agent runtime."
           ["Use this for one-off agent execution."
            "Pass direct-start style agent overrides in agent_spec_json when you need a specific role, contract, actor, or tool policy surface."]
           spawn-agent-params
           agent-spawn-execute))

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
   (let [allowed-any? (fn [& tool-ids]
                        (or (nil? auth-context)
                            (boolean (some #(ctx-tool-allowed? auth-context %) tool-ids))))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed-any? "event_agents.status" "events.status")
                  (event-agent-status-tool runtime config))
                (when (allowed-any? "event_agents.status" "events.status")
                  (events-status-tool runtime config))
                (when (allowed-any? "event_agents.dispatch" "events.dispatch")
                  (event-agent-dispatch-tool runtime config))
                (when (allowed-any? "event_agents.dispatch" "events.dispatch")
                  (events-dispatch-tool runtime config))
                (when (allowed-any? "event_agents.run_job" "agents.spawn")
                  (event-agent-run-job-tool runtime config))
                (when (allowed-any? "event_agents.run_job" "agents.spawn")
                  (agents-spawn-tool runtime config))
                (when (allowed-any? "event_agents.upsert_job")
                  (event-agent-upsert-job-tool runtime config))
                (when (allowed-any? "schedule_event_agent")
                  (schedule-event-agent-tool runtime config))]))))))
