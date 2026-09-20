(ns knoxx.frontend.pages.agents.runtime-model
  "Pure trigger-to-job and runtime display projections."
  (:require [clojure.string :as str]
            [knoxx.frontend.admin.event-agent-utils :as event-u]
            [knoxx.frontend.pages.agents.model :as model]))

(defn runtime-live-jobs
  "Read current runtime job projections or an empty collection." [runtime-status]
  (or (get-in runtime-status [:runtime :jobs]) []))

(defn runtime-for-job
  "Find the live projection corresponding to a scheduled job." [runtime-status job-id]
  (event-u/runtime-for-job (runtime-live-jobs runtime-status) job-id))

(defn event-job-contract-id
  "Read the target contract identity associated with a runtime job." [job]
  (some-> (:contractSourceId job) str str/trim not-empty))

(defn pipeline-agent-targets
  "Select pipeline steps whose targets are known agent contracts." [agent-ids pipeline]
  (->> (get-in pipeline [:pipeline :steps])
       (map :contract)
       (filter agent-ids)
       (remove str/blank?)
       vec))

(defn trigger-agent-targets
  "Resolve a trigger target through either a pipeline or an agent contract." [agent-ids pipelines-by-id trigger]
  (let [target (some-> (get-in trigger [:trigger :target]) str str/trim not-empty)]
    (cond
      (nil? target) []
      (contains? pipelines-by-id target) (pipeline-agent-targets agent-ids (get pipelines-by-id target))
      (contains? agent-ids target) [target]
      :else [])))

(defn cron-schedule->cadence-minutes
  "Interpret the editor-supported cron cadence with the existing fallback." [schedule]
  (let [schedule (str (or schedule ""))]
    (cond
      (re-find #"\*/(\d+)" schedule) (model/parse-int-or (second (re-find #"\*/(\d+)" schedule)) 5)
      (re-find #"^\d+" schedule) 60
      :else 5)))

(defn trigger-event-kinds
  "Read declared event filters as displayable event names." [trigger-spec]
  (->> (or (get-in trigger-spec [:source :events])
           (get-in trigger-spec [:filters :events])
           [])
       (map model/keywordish->text)
       (remove str/blank?)
       vec))

(defn trigger->schedule-jobs
  "Project each trigger target into the existing runtime schedule row shape." [agent-ids pipelines-by-id trigger]
  (let [trigger-id (:id trigger)
        trigger-spec (:trigger trigger)
        trigger-kind (some-> (:kind trigger-spec) model/keywordish->plain str/trim not-empty)
        target (some-> (:target trigger-spec) str str/trim not-empty)
        agent-targets (trigger-agent-targets agent-ids pipelines-by-id trigger)
        row-targets (if (seq agent-targets) agent-targets [target])]
    (when (and trigger-id trigger-kind target)
      (mapv (fn [row-target]
              {:id trigger-id
               :name trigger-id
               :enabled (not (false? (:enabled trigger)))
                :trigger {:kind trigger-kind
                          :cadenceMinutes (cron-schedule->cadence-minutes (:schedule trigger-spec))
                          :eventKinds (cond-> (trigger-event-kinds trigger-spec)
                                        (:schedule trigger-spec) (conj (:schedule trigger-spec)))}
               :source (:source trigger-spec)
               :contractSourceId row-target
               :contractSourceKind (if (contains? agent-ids row-target) "agent" "target")
               :contractSourceKey (str "trigger:" trigger-id " -> " target)
               :triggerContractId trigger-id
               :triggerTarget target})
            row-targets))))

(defn trigger-schedule-jobs
  "Build sorted schedule rows from the current contract library." [agents triggers pipelines]
  (let [agent-ids (set (map :id agents))
        pipelines-by-id (into {} (map (fn [pipeline] [(:id pipeline) pipeline])) pipelines)]
    (->> triggers
         (mapcat #(trigger->schedule-jobs agent-ids pipelines-by-id %))
         (sort-by (juxt #(get-in % [:trigger :kind]) :id))
         vec)))

(defn contract-job?
  "Check whether a scheduled job belongs to a selected contract." [contract-id job]
  (= (some-> contract-id str str/trim not-empty)
     (event-job-contract-id job)))

(defn schedule-label
  "Describe a job schedule using runtime data or its trigger declaration." [job runtime]
  (or (:scheduleLabel runtime)
      (when (= (get-in job [:trigger :kind]) "cron")
        (str "Every " (get-in job [:trigger :cadenceMinutes]) " min"))
      (when-let [event-kinds (seq (get-in job [:trigger :eventKinds]))]
        (str/join ", " event-kinds))
      "manual / event-driven"))

(defn runtime-status-label
  "Describe disabled, running and completed runtime states." [runtime enabled]
  (cond
    (not enabled) "disabled"
    (:running runtime) "running"
    (:lastStatus runtime) (:lastStatus runtime)
    :else "ready"))

(defn runtime-status-class
  "Select the status styling for a runtime projection." [runtime enabled]
  (cond
    (not enabled) "bg-amber-500/10 text-amber-300"
    (:running runtime) "bg-sky-500/10 text-sky-300"
    (= (:lastStatus runtime) "ok") "bg-emerald-500/10 text-emerald-300"
    (= (:lastStatus runtime) "error") "bg-rose-500/10 text-rose-300"
    :else "bg-slate-700/40 text-slate-300"))

(defn runtime-button-class
  "Select the existing action styling for a runtime command." [tone]
  (case tone
    :primary "rounded-md bg-sky-600 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-sky-500 disabled:opacity-60"
    :success "rounded-md bg-emerald-700 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-emerald-600 disabled:opacity-60"
    :danger "rounded-md bg-rose-700 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-rose-600 disabled:opacity-60"
    :warn "rounded-md border border-amber-700 bg-amber-950/40 px-2 py-1 text-[11px] font-semibold leading-none text-amber-100 hover:bg-amber-900/60 disabled:opacity-60"
    "rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] font-medium leading-none text-slate-100 hover:bg-slate-800 disabled:opacity-60"))
