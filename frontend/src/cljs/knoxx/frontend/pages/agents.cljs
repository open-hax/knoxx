(ns knoxx.frontend.pages.agents
  "Shadow-owned Agents page.

   This page is for agent contracts, with a linked event-runtime side panel for
   triggering and schedule/status review. /events remains the full dispatch and
   scheduling console."
  (:require [cljs.pprint :as pprint]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-utils :as event-u]
             [knoxx.frontend.api.contracts :as contracts-api]
             [knoxx.frontend.api.event-agents :as event-api]
             ["react-router-dom" :as rr]
             ["@open-hax/knoxx-app-bridge" :as app]
              ["@open-hax/knoxx-app-bridge" :refer [useChatWorkspaceController ChatWorkspacePane]]
             [knoxx.frontend.components.layout.workbench :as layout]))

(def useLocation (.-useLocation rr))
(def useNavigate (.-useNavigate rr))

(def ^:private trigger-kind-options ["manual" "event" "cron"])
(def ^:private source-kind-options ["manual" "discord" "github" "webhook" "cron"])
(def ^:private thinking-options ["off" "minimal" "low" "medium" "high" "xhigh"])

(def ^:private default-agent-contract
  {:contract/id "new-agent"
   :contract/kind :agent
   :contract/version 1
   :enabled true
   :trigger-kind :manual
   :source-kind :manual
   :source-mode :respond
   :agent {:roles []
           :model "gemma4:31b"
           :thinking :off}
   :prompts {:system ""
             :task ""}
   :events {:always []
            :maybe []}
   :data {}
   :hooks {}})

(defn- get-tab-from-location [^js location]
  (let [search (or (.-search location) "")
        query (js/URLSearchParams. (if (str/starts-with? search "?") (subs search 1) search))
        tab (.get query "tab")]
    (if (= tab "audit") "audit" "contracts")))

(defn- set-tab-in-location [^js location tab]
  (let [query (js/URLSearchParams. (if (str/starts-with? (.-search location) "?") (subs (.-search location) 1) (.-search location)))]
    (.set query "tab" tab)
    (let [suffix (.toString query)
          pathname (.-pathname location)
          hash (.-hash location)]
      (str pathname (when (seq suffix) (str "?" suffix)) hash))))

(defn- error-message [err]
  (or (.-message err) (:message err) (str err)))

(defn- keywordish->text [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (symbol? value) (str value)
    (nil? value) ""
    :else (str value)))

(defn- keywordish->plain [value]
  (let [text (keywordish->text value)]
    (-> text
        (str/replace #"^:" "")
        (str/replace #"^.*/" ""))))

(defn- role-ref->id [value]
  (-> (keywordish->text value)
      (str/replace #"^:" "")
      (str/replace #"^role/" "")
      str/trim))

(defn- role-id->keyword [role-id]
  (keyword "role" role-id))

(defn- prompt-display [value]
  (cond
    (nil? value) ""
    (string? value) value
    :else (pr-str value)))

(defn- draft->edn [draft]
  (with-out-str
    (pprint/pprint draft)))

(defn- parse-contract-edn [edn-text]
  (let [parsed (reader/read-string edn-text)]
    (when-not (map? parsed)
      (throw (js/Error. "Contract EDN must parse to a map.")))
    parsed))

(defn- parse-int-or [value default-value]
  (let [n (js/parseInt (str value) 10)]
    (if (js/Number.isFinite n) n default-value)))

(defn- normalize-contract-id [value]
  (some-> value str str/trim not-empty))

(defn- selected-role-ids [draft]
  (let [agent (:agent draft)
        roles (:roles agent)
        role (:role agent)]
    (->> (concat (or roles [])
                 (cond
                   (sequential? role) role
                   role [role]
                   :else []))
         (map role-ref->id)
         (remove str/blank?)
         distinct
         vec)))

(defn- with-selected-role-ids [draft role-ids]
  (let [roles (->> role-ids
                   (remove str/blank?)
                   distinct
                   (mapv role-id->keyword))]
    (update draft :agent
            (fn [agent]
              (-> (or agent {})
                  (assoc :roles roles)
                  (dissoc :role))))))

(defn- has-permission? [auth permission]
  (.includes (or (.-permissions auth) #js []) permission))

(defn- runtime-control-jobs [runtime-status]
  (or (get-in runtime-status [:control :jobs]) []))

(defn- runtime-live-jobs [runtime-status]
  (or (get-in runtime-status [:runtime :jobs]) []))

(defn- runtime-for-job [runtime-status job-id]
  (event-u/runtime-for-job (runtime-live-jobs runtime-status) job-id))

(defn- event-job-contract-id [job]
  (some-> (:contractSourceId job) str str/trim not-empty))

(defn- pipeline-agent-targets [agent-ids pipeline]
  (->> (get-in pipeline [:pipeline :steps])
       (map :contract)
       (filter agent-ids)
       (remove str/blank?)
       vec))

(defn- trigger-agent-targets [agent-ids pipelines-by-id trigger]
  (let [target (some-> (get-in trigger [:trigger :target]) str str/trim not-empty)]
    (cond
      (nil? target) []
      (contains? pipelines-by-id target) (pipeline-agent-targets agent-ids (get pipelines-by-id target))
      (contains? agent-ids target) [target]
      :else [])))

(defn- cron-schedule->cadence-minutes [schedule]
  (let [schedule (str (or schedule ""))]
    (cond
      (re-find #"\*/(\d+)" schedule) (parse-int-or (second (re-find #"\*/(\d+)" schedule)) 5)
      (re-find #"^\d+" schedule) 60
      :else 5)))

(defn- trigger-event-kinds [trigger-spec]
  (->> (or (get-in trigger-spec [:source :events])
           (get-in trigger-spec [:filters :events])
           [])
       (map keywordish->text)
       (remove str/blank?)
       vec))

(defn- trigger->schedule-jobs [agent-ids pipelines-by-id trigger]
  (let [trigger-id (:id trigger)
        trigger-spec (:trigger trigger)
        trigger-kind (some-> (:kind trigger-spec) keywordish->plain str/trim not-empty)
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

(defn- trigger-schedule-jobs [agents triggers pipelines]
  (let [agent-ids (set (map :id agents))
        pipelines-by-id (into {} (map (fn [pipeline] [(:id pipeline) pipeline])) pipelines)]
    (->> triggers
         (mapcat #(trigger->schedule-jobs agent-ids pipelines-by-id %))
         (sort-by (juxt #(get-in % [:trigger :kind]) :id))
         vec)))

(defn- contract-job? [contract-id job]
  (= (some-> contract-id str str/trim not-empty)
     (event-job-contract-id job)))

(defn- schedule-label [job runtime]
  (or (:scheduleLabel runtime)
      (when (= (get-in job [:trigger :kind]) "cron")
        (str "Every " (get-in job [:trigger :cadenceMinutes]) " min"))
      (when-let [event-kinds (seq (get-in job [:trigger :eventKinds]))]
        (str/join ", " event-kinds))
      "manual / event-driven"))

(defn- selected-agent-contract-id
  [selected-id draft]
  (or (some-> selected-id str str/trim not-empty)
      (let [draft-id (some-> (:contract/id draft) str str/trim not-empty)]
        (when-not (= draft-id "new-agent")
          draft-id))))

(defn- runtime-status-label [runtime enabled]
  (cond
    (not enabled) "disabled"
    (:running runtime) "running"
    (:lastStatus runtime) (:lastStatus runtime)
    :else "ready"))

(defn- runtime-status-class [runtime enabled]
  (cond
    (not enabled) "bg-amber-500/10 text-amber-300"
    (:running runtime) "bg-sky-500/10 text-sky-300"
    (= (:lastStatus runtime) "ok") "bg-emerald-500/10 text-emerald-300"
    (= (:lastStatus runtime) "error") "bg-rose-500/10 text-rose-300"
    :else "bg-slate-700/40 text-slate-300"))

(defn- runtime-button-class [tone]
  (case tone
    :primary "rounded-md bg-sky-600 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-sky-500 disabled:opacity-60"
    :success "rounded-md bg-emerald-700 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-emerald-600 disabled:opacity-60"
    :danger "rounded-md bg-rose-700 px-2 py-1 text-[11px] font-semibold leading-none text-slate-50 hover:bg-rose-600 disabled:opacity-60"
    :warn "rounded-md border border-amber-700 bg-amber-950/40 px-2 py-1 text-[11px] font-semibold leading-none text-amber-100 hover:bg-amber-900/60 disabled:opacity-60"
    "rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] font-medium leading-none text-slate-100 hover:bg-slate-800 disabled:opacity-60"))

(defn- runtime-info-row [label value]
  (d/div {:class-name "flex items-center justify-between gap-2 text-[11px] leading-4"}
         (d/span {:class-name "text-slate-500"} label)
         (d/span {:class-name "min-w-0 truncate text-right text-slate-200"} value)))

(defnc text-field
  [{:keys [label value on-change disabled placeholder type]}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/input {:type (or type "text")
                     :value (or value "")
                     :on-change #(on-change (.. % -target -value))
                     :disabled disabled
                     :placeholder placeholder
                     :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                      "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")})))

(defnc textarea-field
  [{:keys [label value on-change disabled rows help]}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "flex items-baseline justify-between gap-2"}
                  (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                         label)
                  (when help
                    (d/div {:class-name "text-[10px] text-slate-500"} help)))
           (d/textarea {:value (or value "")
                        :on-change #(on-change (.. % -target -value))
                        :disabled disabled
                        :rows (or rows 4)
                        :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                         "font-mono text-xs leading-5 text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")})))

(defnc select-field
  [{:keys [label value on-change disabled options]}]
  (d/label {:class-name "block space-y-1"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  label)
           (d/select {:value (or value "")
                      :on-change #(on-change (.. % -target -value))
                      :disabled disabled
                      :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                       "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")}
                     (for [opt options]
                       (d/option {:key opt :value opt} opt)))))

(defnc checkbox-field
  [{:keys [label checked on-change disabled]}]
  (d/label {:class-name (str "inline-flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950/70 "
                             "px-2 py-1 text-xs text-slate-200")}
           (d/input {:type "checkbox"
                     :checked (boolean checked)
                     :on-change #(on-change (.. % -target -checked))
                     :disabled disabled})
           label))

(defnc role-picker
  [{:keys [roles selected on-change disabled]}]
  (let [[candidate set-candidate] (hooks/use-state "")
        selected-set (set selected)
        all-roles (->> (concat roles selected)
                       (remove str/blank?)
                       distinct
                       sort
                       vec)
        available (remove selected-set all-roles)]
    (d/div {:class-name "space-y-2"}
           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-400"}
                  "Roles from role contracts")
           (d/div {:class-name "flex min-h-8 flex-wrap gap-1 rounded-md border border-slate-800 bg-slate-950/70 p-1.5"}
                  (if (seq selected)
                    (for [role selected]
                      (d/span {:key role
                               :class-name "inline-flex items-center gap-1 rounded-full border border-sky-500/30 bg-sky-500/10 px-1.5 py-0.5 text-[10px] text-sky-100"}
                              role
                              (d/button {:type "button"
                                         :on-click #(on-change (vec (remove #{role} selected)))
                                         :disabled disabled
                                         :class-name "text-sky-200 hover:text-white disabled:opacity-50"}
                                        "×")))
                    (d/span {:class-name "text-xs text-slate-500"}
                            "No roles assigned.")))
           (d/div {:class-name "flex gap-2"}
                  (d/select {:value candidate
                             :on-change #(set-candidate (.. % -target -value))
                             :disabled (or disabled (empty? available))
                             :class-name (str "min-w-0 flex-1 rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1 "
                                              "text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60")}
                            (d/option {:value ""} "Add role…")
                            (for [role available]
                              (d/option {:key role :value role} role)))
                  (d/button {:type "button"
                             :disabled (or disabled (str/blank? candidate))
                             :on-click #(when-not (str/blank? candidate)
                                          (on-change (conj (vec selected) candidate))
                                          (set-candidate ""))
                             :class-name (str "rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] "
                                              "font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                            "➕")))))

(defnc agent-list
  [{:keys [agents selected-id on-select on-new loading]}]
  (d/aside {:class-name "flex min-h-0 flex-col rounded-xl border border-slate-800 bg-slate-950/50"}
           (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-2"}
                  (d/div {:class-name "flex items-center justify-between gap-2"}
                         (d/div {:class-name "text-sm font-semibold text-slate-100"} "Agent contracts")
                         (d/div {:class-name "text-[11px] text-slate-500"}
                                (str (count agents))))
                  (d/div {:class-name "mt-1 text-[11px] text-slate-500"}
                         "All :agent contracts, regardless of event wiring."))
           (d/div {:class-name "min-h-0 flex-1 space-y-1.5 overflow-y-auto p-2"}
                  (cond
                    loading
                    (d/div {:class-name "rounded-lg border border-slate-800 p-3 text-xs text-slate-400"}
                           "Loading agents…")

                    (seq agents)
                    (for [agent agents]
                      (let [active (= selected-id (:id agent))]
                        (d/button {:key (:id agent)
                                   :type "button"
                                   :on-click #(on-select (:id agent))
                                   :class-name (str "w-full rounded-lg border px-2.5 py-2 text-left transition "
                                                    (if active
                                                      "border-sky-500/60 bg-sky-500/10 shadow-[inset_2px_0_0_0_rgba(56,189,248,0.9)]"
                                                      "border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70"))}
                                  (d/div {:class-name "flex items-center justify-between gap-2"}
                                         (d/div {:class-name "min-w-0 truncate text-sm font-medium text-slate-100"}
                                                (:id agent))
                                         (d/span {:class-name (str "rounded-full px-1.5 py-0.5 text-[10px] "
                                                                   (if (:enabled agent)
                                                                     "bg-emerald-500/10 text-emerald-300"
                                                                     "bg-slate-700/40 text-slate-400"))}
                                                 (if (:enabled agent) "on" "off")))
                                  (d/div {:class-name "mt-1 truncate text-[11px] text-slate-500"}
                                         (or (:path agent) "contracts/agents")))))

                    :else
                    (d/div {:class-name "rounded-lg border border-dashed border-slate-800 p-4 text-center text-xs text-slate-500"}
                           "No agent contracts found.")))
           (d/div {:class-name "shrink-0 border-t border-slate-800 p-2"}
                  (d/button {:type "button"
                             :on-click on-new
                             :class-name (str "w-full rounded-lg border border-dashed border-slate-700 px-3 py-2 text-sm "
                                              "font-medium text-slate-300 hover:border-sky-500/50 hover:text-sky-100")}
                            "+ New agent"))))

(defnc runtime-job-card
  [{:keys [job runtime selected compact on-run on-inspect can-control running-job-id]}]
  (let [running? (= running-job-id (:id job))
        enabled? (:enabled job)
        contract-id (event-job-contract-id job)
        inspectable? (= "agent" (:contractSourceKind job))]
    (d/div {:class-name (str "rounded-lg border p-2 "
                             (if selected
                               "border-sky-500/40 bg-sky-500/5"
                               "border-slate-800 bg-slate-950/50"))}
           (d/div {:class-name "flex items-center justify-between gap-2"}
                  (d/div {:class-name "min-w-0"}
                         (d/div {:class-name "truncate text-xs font-semibold text-slate-100"}
                                (:name job))
                         (when-not compact
                           (d/div {:class-name "mt-0.5 truncate font-mono text-[10px] text-slate-500"}
                                  (:id job))))
                  (d/span {:class-name (str "shrink-0 rounded-full px-2 py-0.5 text-[10px] uppercase tracking-wide "
                                            (runtime-status-class runtime enabled?))}
                          (runtime-status-label runtime enabled?)))
           (d/div {:class-name "mt-1 grid grid-cols-2 gap-x-2 gap-y-0.5"}
                  (runtime-info-row "Trigger" (or (get-in job [:trigger :kind]) "—"))
                  (runtime-info-row "Runs" (str (or (:runCount runtime) 0)))
                  (runtime-info-row "Schedule" (schedule-label job runtime))
                  (runtime-info-row "Next" (event-u/to-local-date-time (:nextRunAt runtime)))
                  (when-not compact
                    (runtime-info-row "Last" (event-u/to-local-date-time (:lastFinishedAt runtime)))))
           (when (:lastError runtime)
             (d/div {:class-name "mt-1 rounded border border-rose-500/30 bg-rose-500/10 px-1.5 py-1 text-[10px] text-rose-200"}
                    (:lastError runtime)))
           (d/div {:class-name "mt-1.5 grid grid-cols-2 gap-1"}
                   (d/button {:type "button"
                              :on-click #(when (and inspectable? on-inspect contract-id)
                                           (on-inspect contract-id))
                              :disabled (or (not inspectable?) (nil? on-inspect) (nil? contract-id))
                              :title (if inspectable? "Inspect target agent" "Target is not an agent contract")
                              :class-name (runtime-button-class :default)}
                             "🔎 EDN")
                  (d/button {:type "button"
                             :on-click #(on-run (:id job))
                             :disabled (or (not can-control) running? (not enabled?))
                             :title "Run now"
                             :class-name (runtime-button-class :primary)}
                            (if running? "⏳ Queue…" "▶ Run"))))))

(defnc runtime-schedule-list
  [{:keys [jobs runtime-status selected-contract-id exclude-contract-id on-run on-inspect can-control running-job-id]}]
  (let [rows (->> jobs
                  (filter event-job-contract-id)
                  (remove #(= exclude-contract-id (event-job-contract-id %)))
                  (sort-by (fn [job]
                             [(if (= selected-contract-id (event-job-contract-id job)) 0 1)
                              (or (:contractSourceKey job) (:id job))
                              (:id job)])))]
    (d/div {:class-name "space-y-2"}
           (if (seq rows)
             (for [job rows]
               (let [runtime (or (runtime-for-job runtime-status (:id job)) {})]
                 ($ runtime-job-card {:key (:id job)
                                      :job job
                                      :runtime runtime
                                      :selected (= selected-contract-id (event-job-contract-id job))
                                      :compact true
                                      :on-run on-run
                                      :on-inspect on-inspect
                                      :can-control can-control
                                      :running-job-id running-job-id})))
             (d/div {:class-name "rounded-lg border border-dashed border-slate-800 p-3 text-xs text-slate-500"}
                    "No contract-backed runtime jobs are loaded.")))))

#_(legacy-agent-workbench-sidebar
  [{:keys [agents selected-id on-select on-new loading-agents selected-contract-id
            runtime-status loading-runtime runtime-error runtime-notice can-control
            running-job-id toggling-runtime resetting-runtime
            on-refresh-runtime on-start-runtime on-stop-runtime on-reset-runtime on-run on-inspect-contract
            hide-header]}]
  (let [[agents-open? set-agents-open!] (hooks/use-state true)
        [runtime-open? set-runtime-open!] (hooks/use-state true)
        jobs (schedule-jobs runtime-status agents)
        selected-jobs (filterv #(contract-job? selected-contract-id %) jobs)
        schedule-count (count (filter event-job-contract-id jobs))
        other-schedule-count (count (remove #(= selected-contract-id (event-job-contract-id %))
                                            (filter event-job-contract-id jobs)))
        runtime-running? (boolean (get-in runtime-status [:runtime :running]))]
    (d/aside {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950/60"}
             (when-not hide-header
               (d/div {:class-name "flex shrink-0 items-center justify-between gap-2 border-b border-slate-800 px-2 py-1.5"}
                      (d/div {:class-name "flex min-w-0 items-center gap-2"}
                             (d/span {:class-name "text-xs font-semibold text-slate-100"} "Agents")
                             (d/span {:class-name "rounded bg-slate-900 px-1.5 py-0.5 text-[10px] text-slate-400"}
                                     (str (count agents))))
                      (d/div {:class-name "flex items-center gap-1"}
                             (d/button {:type "button"
                                        :on-click on-new
                                        :title "New agent"
                                        :class-name (runtime-button-class :default)}
                                       "➕")
                             (d/button {:type "button"
                                        :on-click on-refresh-runtime
                                        :disabled (or loading-runtime toggling-runtime resetting-runtime)
                                        :title "Refresh runtime"
                                        :class-name (runtime-button-class :default)}
                                       (if loading-runtime "⏳" "↻"))
                             (if runtime-running?
                               (d/button {:type "button"
                                          :on-click on-stop-runtime
                                          :disabled (or toggling-runtime resetting-runtime)
                                          :title "Stop runtime"
                                          :class-name (runtime-button-class :danger)}
                                         (if toggling-runtime "⏳" "⏹"))
                               (d/button {:type "button"
                                          :on-click on-start-runtime
                                          :disabled (or toggling-runtime resetting-runtime)
                                          :title "Start runtime"
                                          :class-name (runtime-button-class :success)}
                                         (if toggling-runtime "⏳" "▶")))
                             (d/button {:type "button"
                                        :on-click on-reset-runtime
                                        :disabled (or loading-runtime toggling-runtime resetting-runtime)
                                        :title "Full reset"
                                        :class-name (runtime-button-class :warn)}
                                       (if resetting-runtime "⏳" "💥")))))
             (when-not can-control
               (d/div {:class-name "shrink-0 border-b border-slate-800 px-2 py-1 text-[11px] text-amber-300"}
                      "Runtime locked"))
             (when runtime-notice
               (d/div {:class-name (str "shrink-0 border-b px-2 py-1 text-[11px] "
                                        (case (:tone runtime-notice)
                                          :success "border-emerald-500/20 bg-emerald-500/10 text-emerald-200"
                                          "border-rose-500/20 bg-rose-500/10 text-rose-200"))}
                      (:text runtime-notice)))
             (when (seq runtime-error)
               (d/div {:class-name "shrink-0 border-b border-rose-500/20 bg-rose-500/10 px-2 py-1 text-[11px] text-rose-200"}
                      runtime-error))
             (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
                    (d/section {:class-name (str "flex min-h-0 flex-col border-b border-slate-800 "
                                                 (if agents-open? "basis-[44%]" "shrink-0"))}
                               (d/button {:type "button"
                                          :on-click #(set-agents-open! (not agents-open?))
                                          :class-name "flex shrink-0 items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1 text-left hover:bg-slate-900/60"}
                                         (d/div {:class-name "flex min-w-0 items-center gap-2"}
                                                (d/span {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                                        "Contracts")
                                                (d/span {:class-name "rounded bg-slate-900 px-1 py-0.5 text-[9px] text-slate-500"}
                                                        (str (count agents))))
                                         (d/span {:class-name "text-[10px] text-slate-500"}
                                                 (if agents-open? "▾" "▸")))
                               (when agents-open?
                                 (d/div {:class-name "min-h-0 flex-1 overflow-y-auto"}
                                        (if loading-agents
                                          (d/div {:class-name "p-2 text-xs text-slate-400"} "Loading…")
                                          (if (seq agents)
                                            (for [agent agents]
                                              (let [active (= selected-id (:id agent))]
                                                (d/button {:key (:id agent)
                                                           :type "button"
                                                           :on-click #(on-select (:id agent))
                                                           :class-name (str "flex w-full items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1.5 text-left "
                                                                            (if active
                                                                              "bg-sky-500/10 text-sky-100"
                                                                              "text-slate-300 hover:bg-slate-900/70"))}
                                                          (d/span {:class-name "min-w-0 truncate text-xs font-medium"}
                                                                  (:id agent))
                                                          (d/span {:class-name (str "shrink-0 rounded px-1 py-0.5 text-[9px] uppercase "
                                                                                    (if (:enabled agent)
                                                                                      "bg-emerald-500/10 text-emerald-300"
                                                                                      "bg-slate-800 text-slate-500"))}
                                                                  (if (:enabled agent) "on" "off")))))
                                            (d/div {:class-name "p-2 text-xs text-slate-500"} "No agents"))))))
                    (d/section {:class-name (str "flex min-h-0 flex-col "
                                                 (if runtime-open? "flex-1" "shrink-0"))}
                               (d/button {:type "button"
                                          :on-click #(set-runtime-open! (not runtime-open?))
                                          :class-name "flex shrink-0 items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1 text-left hover:bg-slate-900/60"}
                                         (d/div {:class-name "flex min-w-0 items-center gap-2"}
                                                (d/span {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                                        "Runtime")
                                                (d/span {:class-name (str "rounded px-1.5 py-0.5 text-[9px] uppercase "
                                                                         (if runtime-running?
                                                                           "bg-emerald-500/10 text-emerald-300"
                                                                           "bg-amber-500/10 text-amber-300"))}
                                                        (if runtime-running? "running" "stopped"))
                                                (d/span {:class-name "rounded bg-slate-900 px-1 py-0.5 text-[9px] text-slate-500"}
                                                        (str schedule-count)))
                                         (d/span {:class-name "text-[10px] text-slate-500"}
                                                 (if runtime-open? "▾" "▸")))
                               (when runtime-open?
                                 (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-2 p-2"}
                                        (if loading-runtime
                                          (d/div {:class-name "rounded border border-slate-800 p-2 text-xs text-slate-400"}
                                                 "Loading runtime…")
                                          (d/div {:class-name "shrink-0 space-y-1"}
                                                 (d/div {:class-name "flex items-center justify-between gap-2"}
                                                        (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                                               "Active job")
                                                        (d/code {:class-name "min-w-0 truncate rounded bg-slate-900 px-1.5 py-0.5 font-mono text-[10px] text-slate-400"}
                                                                (or selected-contract-id "new-agent")))
                                                 (if (seq selected-jobs)
                                                   (for [job selected-jobs]
                                                     (let [runtime (or (runtime-for-job runtime-status (:id job)) {})]
                                                       ($ runtime-job-card {:key (:id job)
                                                                            :job job
                                                                            :runtime runtime
                                                                            :selected true
                                                                            :compact false
                                                                            :on-run on-run
                                                                            :on-inspect on-inspect-contract
                                                                            :can-control can-control
                                                                            :running-job-id running-job-id})))
                                                   (d/div {:class-name "rounded border border-dashed border-slate-800 p-2 text-[11px] text-slate-500"}
                                                          "No mapped job."))))
                                        (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-1"}
                                               (d/div {:class-name "flex shrink-0 items-center justify-between gap-2"}
                                                      (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                                             "All schedules")
                                                      (d/span {:class-name "text-[10px] text-slate-500"}
                                                              (str other-schedule-count)))
                                               (d/div {:class-name "min-h-0 flex-1 overflow-y-auto pr-1"}
                                                      ($ runtime-schedule-list {:jobs jobs
                                                                                :runtime-status runtime-status
                                                                                :selected-contract-id selected-contract-id
                                                                                :exclude-contract-id selected-contract-id
                                                                                :on-run on-run
                                                                                :on-inspect on-inspect-contract
                                                                                :can-control can-control
                                                                                :running-job-id running-job-id}))))))))))

(defnc sidebar-section-toggle
  [{:keys [title count open? on-toggle status]}]
  (d/button {:type "button"
             :on-click on-toggle
             :class-name "flex shrink-0 items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1 text-left hover:bg-slate-900/60"}
            (d/div {:class-name "flex min-w-0 items-center gap-2"}
                   (d/span {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"} title)
                   status
                   (d/span {:class-name "rounded bg-slate-900 px-1 py-0.5 text-[9px] text-slate-500"} (str count)))
            (d/span {:class-name "text-[10px] text-slate-500"} (if open? "▾" "▸"))))

(defnc agent-contract-row
  [{:keys [agent active on-select]}]
  (d/button {:key (:id agent)
             :type "button"
             :on-click #(on-select (:id agent))
             :class-name (str "flex w-full items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1.5 text-left "
                              (if active "bg-sky-500/10 text-sky-100" "text-slate-300 hover:bg-slate-900/70"))}
            (d/span {:class-name "min-w-0 truncate text-xs font-medium"} (:id agent))
            (d/span {:class-name (str "shrink-0 rounded px-1 py-0.5 text-[9px] uppercase "
                                      (if (:enabled agent) "bg-emerald-500/10 text-emerald-300" "bg-slate-800 text-slate-500"))}
                    (if (:enabled agent) "on" "off"))))

(defnc agent-contract-section
  [{:keys [agents selected-id loading-agents open? on-toggle on-select]}]
  (d/section {:class-name (str "flex min-h-0 flex-col border-b border-slate-800 " (if open? "basis-[44%]" "shrink-0"))}
             ($ sidebar-section-toggle {:title "Contracts" :count (count agents) :open? open? :on-toggle on-toggle})
             (when open?
               (d/div {:class-name "min-h-0 flex-1 overflow-y-auto"}
                      (cond
                        loading-agents (d/div {:class-name "p-2 text-xs text-slate-400"} "Loading…")
                        (seq agents) (for [agent agents]
                                       ($ agent-contract-row {:key (:id agent) :agent agent :active (= selected-id (:id agent)) :on-select on-select}))
                        :else (d/div {:class-name "p-2 text-xs text-slate-500"} "No agents"))))))

(defnc selected-runtime-jobs
  [{:keys [selected-contract-id selected-jobs runtime-status loading-runtime on-run on-inspect-contract can-control running-job-id]}]
  (if loading-runtime
    (d/div {:class-name "rounded border border-slate-800 p-2 text-xs text-slate-400"} "Loading runtime…")
    (d/div {:class-name "shrink-0 space-y-1"}
           (d/div {:class-name "flex items-center justify-between gap-2"}
                   (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"} "Selected agent triggers")
                  (d/code {:class-name "min-w-0 truncate rounded bg-slate-900 px-1.5 py-0.5 font-mono text-[10px] text-slate-400"}
                          (or selected-contract-id "new-agent")))
           (if (seq selected-jobs)
             (for [job selected-jobs]
               (let [runtime (or (runtime-for-job runtime-status (:id job)) {})]
                 ($ runtime-job-card {:key (:id job) :job job :runtime runtime :selected true :compact false
                                      :on-run on-run :on-inspect on-inspect-contract
                                      :can-control can-control :running-job-id running-job-id})))
              (d/div {:class-name "rounded border border-dashed border-slate-800 p-2 text-[11px] text-slate-500"} "No triggers target this agent.")))))

(defnc runtime-schedules-section
  [{:keys [jobs runtime-status selected-contract-id other-schedule-count on-run on-inspect-contract can-control running-job-id]}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-1"}
         (d/div {:class-name "flex shrink-0 items-center justify-between gap-2"}
                (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"} "All triggers")
                (d/span {:class-name "text-[10px] text-slate-500"} (str other-schedule-count)))
         (d/div {:class-name "min-h-0 flex-1 overflow-y-auto pr-1"}
                ($ runtime-schedule-list {:jobs jobs :runtime-status runtime-status
                                          :selected-contract-id selected-contract-id
                                          :exclude-contract-id selected-contract-id
                                          :on-run on-run :on-inspect on-inspect-contract
                                          :can-control can-control :running-job-id running-job-id}))))

(defnc agent-runtime-section
  [{:keys [open? on-toggle runtime-running? schedule-count jobs selected-jobs runtime-status
           selected-contract-id loading-runtime other-schedule-count on-run on-inspect-contract
           can-control running-job-id]}]
  (d/section {:class-name (str "flex min-h-0 flex-col " (if open? "flex-1" "shrink-0"))}
             ($ sidebar-section-toggle {:title "Triggers" :count schedule-count :open? open? :on-toggle on-toggle
                                        :status (d/span {:class-name (str "rounded px-1.5 py-0.5 text-[9px] uppercase "
                                                                           (if runtime-running? "bg-emerald-500/10 text-emerald-300" "bg-slate-700/40 text-slate-300"))}
                                                        (if runtime-running? "live" "contracts"))})
             (when open?
               (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-2 p-2"}
                      ($ selected-runtime-jobs {:selected-contract-id selected-contract-id
                                                :selected-jobs selected-jobs
                                                :runtime-status runtime-status
                                                :loading-runtime loading-runtime
                                                :on-run on-run
                                                :on-inspect-contract on-inspect-contract
                                                :can-control can-control
                                                :running-job-id running-job-id})
                      ($ runtime-schedules-section {:jobs jobs
                                                    :runtime-status runtime-status
                                                    :selected-contract-id selected-contract-id
                                                    :other-schedule-count other-schedule-count
                                                    :on-run on-run
                                                    :on-inspect-contract on-inspect-contract
                                                    :can-control can-control
                                                    :running-job-id running-job-id})))))

(defnc audit-sessions-section
  [{:keys [open? on-toggle chat selected-contract-id]}]
  (d/section {:class-name (str "flex min-h-0 flex-col border-b border-slate-800 " (if open? "flex-1" "shrink-0"))}
             ($ sidebar-section-toggle {:title "Audit sessions" :count "chat" :open? open? :on-toggle on-toggle})
             (when (and open? chat)
               ($ app/AgentAuditSessionList {:controller chat
                                             :builtInContractId selected-contract-id}))))

(defnc agent-workbench-sidebar
  [{:keys [agents triggers pipelines selected-id on-select loading-agents selected-contract-id runtime-status loading-runtime
            runtime-error runtime-notice can-control running-job-id on-run on-inspect-contract audit? chat]}]
  (let [[agents-open? set-agents-open!] (hooks/use-state true)
        [runtime-open? set-runtime-open!] (hooks/use-state true)
        [audit-sessions-open? set-audit-sessions-open!] (hooks/use-state true)
        jobs (trigger-schedule-jobs agents triggers pipelines)
        contract-jobs (filter event-job-contract-id jobs)
        selected-jobs (filterv #(contract-job? selected-contract-id %) jobs)
        runtime-running? (boolean (get-in runtime-status [:runtime :running]))]
    (d/aside {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950/60"}
             (when-not can-control
               (d/div {:class-name "shrink-0 border-b border-slate-800 px-2 py-1 text-[11px] text-amber-300"} "Runtime locked"))
             (when runtime-notice
               (d/div {:class-name (str "shrink-0 border-b px-2 py-1 text-[11px] "
                                        (case (:tone runtime-notice) :success "border-emerald-500/20 bg-emerald-500/10 text-emerald-200" "border-rose-500/20 bg-rose-500/10 text-rose-200"))}
                      (:text runtime-notice)))
             (when (seq runtime-error)
               (d/div {:class-name "shrink-0 border-b border-rose-500/20 bg-rose-500/10 px-2 py-1 text-[11px] text-rose-200"} runtime-error))
             (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
                    ($ agent-contract-section {:agents agents :selected-id selected-id :loading-agents loading-agents
                                               :open? agents-open? :on-toggle #(set-agents-open! (not agents-open?)) :on-select on-select})
                    (when audit?
                      ($ audit-sessions-section {:open? audit-sessions-open?
                                                 :on-toggle #(set-audit-sessions-open! (not audit-sessions-open?))
                                                 :chat chat
                                                 :selected-contract-id selected-contract-id}))
                    ($ agent-runtime-section {:open? runtime-open? :on-toggle #(set-runtime-open! (not runtime-open?))
                                              :runtime-running? runtime-running? :schedule-count (count contract-jobs)
                                              :jobs jobs :selected-jobs selected-jobs :runtime-status runtime-status
                                              :selected-contract-id selected-contract-id :loading-runtime loading-runtime
                                              :other-schedule-count (count (remove #(= selected-contract-id (event-job-contract-id %)) contract-jobs))
                                              :on-run on-run :on-inspect-contract on-inspect-contract
                                              :can-control can-control :running-job-id running-job-id})))))

(defnc contract-editor-header
  [{:keys [draft can-save saving validating parse-error on-save on-validate]}]
  (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-2"}
         (d/div {:class-name "flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between"}
                (d/div {:class-name "min-w-0"}
                       (d/div {:class-name "flex flex-wrap items-center gap-2"}
                              (d/h2 {:class-name "truncate text-base font-semibold text-slate-100"} (or (:contract/id draft) "New agent"))
                              (d/span {:class-name (str "rounded-full px-2 py-0.5 text-[11px] "
                                                        (if (:enabled draft) "bg-emerald-500/10 text-emerald-300" "bg-slate-700/40 text-slate-400"))}
                                      (if (:enabled draft) "on" "off"))))
                (d/div {:class-name "flex flex-wrap gap-2"}
                       (d/button {:type "button" :on-click on-validate :disabled validating :title "Validate" :class-name (runtime-button-class :success)}
                                 (if validating "⏳" "✅ Check"))
                       (d/button {:type "button" :on-click on-save :disabled (or saving parse-error (not can-save))
                                  :title (when-not can-save "Requires platform.org.create permission")
                                  :class-name (runtime-button-class :primary)}
                                 (if saving "⏳ Save" "💾 Save"))))))

(defnc contract-identity-section
  [{:keys [draft saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Contract identity")
             (d/div {:class-name "grid gap-2 md:grid-cols-2"}
                    ($ text-field {:label "Contract id" :value (:contract/id draft) :on-change #(on-update assoc :contract/id %) :disabled saving})
                    ($ text-field {:label "Version" :type "number" :value (str (or (:contract/version draft) 1))
                                   :on-change #(on-update assoc :contract/version (parse-int-or % 1)) :disabled saving}))
             ($ checkbox-field {:label "Enabled" :checked (:enabled draft) :on-change #(on-update assoc :enabled %) :disabled saving})))

(defnc contract-source-section
  [{:keys [draft saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Activation and source")
             (d/div {:class-name "grid gap-2 md:grid-cols-3"}
                    ($ select-field {:label "Trigger kind" :value (keywordish->plain (:trigger-kind draft)) :options trigger-kind-options
                                     :on-change #(on-update assoc :trigger-kind (keyword %)) :disabled saving})
                    ($ select-field {:label "Source kind" :value (keywordish->plain (:source-kind draft)) :options source-kind-options
                                     :on-change #(on-update assoc :source-kind (keyword %)) :disabled saving})
                    ($ text-field {:label "Source mode" :value (keywordish->plain (:source-mode draft))
                                   :on-change #(on-update assoc :source-mode (keyword %)) :disabled saving}))
             ($ text-field {:label "Cadence minutes (:cadence-min)" :type "number" :value (str (or (:cadence-min draft) 5))
                            :on-change #(on-update assoc :cadence-min (parse-int-or % 5)) :disabled saving})))

(defnc agent-spec-section
  [{:keys [agent role-options selected-roles saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Agent spec")
             ($ role-picker {:roles role-options :selected selected-roles :on-change #(on-update with-selected-role-ids %) :disabled saving})
             (d/div {:class-name "grid gap-2 md:grid-cols-2"}
                    ($ text-field {:label "Model" :value (:model agent) :on-change #(on-update assoc-in [:agent :model] %) :disabled saving})
                    ($ select-field {:label "Thinking" :value (keywordish->plain (:thinking agent)) :options thinking-options
                                     :on-change #(on-update assoc-in [:agent :thinking] (keyword %)) :disabled saving}))))

(defnc prompts-section
  [{:keys [prompts saving on-update]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "text-sm font-semibold text-slate-100"} "Direct prompts")
             ($ textarea-field {:label "System prompt (:prompts :system)" :help "direct only" :value (prompt-display (:system prompts))
                                :on-change #(on-update assoc-in [:prompts :system] %) :disabled saving :rows 7})
             ($ textarea-field {:label "Task prompt (:prompts :task)" :help "direct only" :value (prompt-display (:task prompts))
                                :on-change #(on-update assoc-in [:prompts :task] %) :disabled saving :rows 5})))

(defnc contract-edn-section
  [{:keys [edn-text parse-error saving on-raw-change]}]
  (d/section {:class-name "space-y-2 rounded-lg border border-slate-800 bg-slate-950/50 p-2"}
             (d/div {:class-name "flex items-center justify-between gap-2"}
                    (d/div {:class-name "text-sm font-semibold text-slate-100"} "Full contract EDN")
                    (d/div {:class-name "text-[11px] text-slate-500"} "escape hatch for every schema field"))
             ($ textarea-field {:label "EDN" :value edn-text :on-change on-raw-change :disabled saving :rows 18})
             (when parse-error
               (d/div {:class-name "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200"} parse-error))))

(defnc contract-editor-notices
  [{:keys [notice error]}]
  (d/div
   (when notice
     (d/div {:class-name (str "mt-4 rounded-lg border px-3 py-2 text-sm "
                              (case (:tone notice) :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200" "border-rose-500/30 bg-rose-500/10 text-rose-200"))}
            (:text notice)))
   (when (seq error)
     (d/div {:class-name "mt-4 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"} error))))

(defnc contract-editor
  [{:keys [draft edn-text parse-error role-options can-save saving validating notice error
           on-update on-raw-change on-save on-validate]}]
  (let [agent (:agent draft)
        prompts (:prompts draft)
        selected-roles (selected-role-ids draft)]
    (d/div {:class-name "flex min-h-0 flex-1 flex-col bg-slate-950/40"}
           ($ contract-editor-header {:draft draft :can-save can-save :saving saving :validating validating
                                      :parse-error parse-error :on-save on-save :on-validate on-validate})
           (d/div {:class-name "min-h-0 flex-1 overflow-y-auto p-2"}
                  (d/div {:class-name "grid gap-2 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]"}
                         (d/div {:class-name "space-y-2"}
                                ($ contract-identity-section {:draft draft :saving saving :on-update on-update})
                                ($ contract-source-section {:draft draft :saving saving :on-update on-update})
                                ($ agent-spec-section {:agent agent :role-options role-options :selected-roles selected-roles
                                                       :saving saving :on-update on-update}))
                         (d/div {:class-name "space-y-2"}
                                ($ prompts-section {:prompts prompts :saving saving :on-update on-update})
                                ($ contract-edn-section {:edn-text edn-text :parse-error parse-error
                                                         :saving saving :on-raw-change on-raw-change})))
                  ($ contract-editor-notices {:notice notice :error error})))))

(defnc agent-chat-panel
  [{:keys [chat label storage-key]}]
  ($ layout/WorkbenchPanel
     {:edge "right"
      :label label
      :storage-key storage-key
      :default-width 420
      :min-width 360
      :max-width 640
      :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                      label)}
     ($ ChatWorkspacePane
        {:controller chat
         :showFiles false
         :showFilesToggle false
         :showCanvasToggle false
         :onShowFiles #()})))

(defnc AgentsPage []
  (let [auth (app/useAuth)
        location (useLocation)
        navigate (useNavigate)
        [tab set-tab] (hooks/use-state (fn [] (get-tab-from-location location)))
        [agents set-agents] (hooks/use-state [])
        [triggers set-triggers] (hooks/use-state [])
        [pipelines set-pipelines] (hooks/use-state [])
        [role-options set-role-options] (hooks/use-state [])
        [selected-id set-selected-id] (hooks/use-state nil)
        [draft set-draft] (hooks/use-state default-agent-contract)
        [edn-text set-edn-text] (hooks/use-state (draft->edn default-agent-contract))
        [parse-error set-parse-error] (hooks/use-state nil)
        [loading-library set-loading-library] (hooks/use-state false)
        [loading-contract set-loading-contract] (hooks/use-state false)
        [saving set-saving] (hooks/use-state false)
        [validating set-validating] (hooks/use-state false)
        [notice set-notice] (hooks/use-state nil)
        [error set-error] (hooks/use-state "")
        [runtime-status set-runtime-status] (hooks/use-state nil)
        [runtime-loading set-runtime-loading] (hooks/use-state false)
        [runtime-error set-runtime-error] (hooks/use-state "")
        [runtime-notice set-runtime-notice] (hooks/use-state nil)
        [running-job-id set-running-job-id] (hooks/use-state nil)
        [toggling-runtime set-toggling-runtime] (hooks/use-state false)
        [resetting-runtime set-resetting-runtime] (hooks/use-state false)
        ;; Chat state
        chat (useChatWorkspaceController
              #js {:initialShowCanvas false
                   :defaultRole "agent_librarian"
                   :defaultActorId "agent_librarian"
                   :sessionIdKey "knoxx_agents_session_id"
                   :scratchpadStorageKey "knoxx_agents_scratchpad_state"
                   :pinnedContextStorageKey "knoxx_agents_pinned_context"
                   :sessionStateKey "knoxx_agents_chat_session_state"
                   :sidebarWidthKey "knoxx_agents_sidebar_width_px"})
        can-save-contracts? (boolean (or (.-isSystemAdmin auth)
                                         (has-permission? auth "platform.org.create")))
        can-control-runtime? (boolean (or (.-isSystemAdmin auth)
                                          (has-permission? auth "org.event_agents.control")))]

    (letfn [(load-agent-library! []
              (set-loading-library true)
              (set-error "")
              (-> (js/Promise.all #js [(contracts-api/list-contracts "agents")
                                        (contracts-api/list-contracts "roles")
                                        (contracts-api/list-contracts "triggers")
                                        (contracts-api/list-contracts "pipelines")])
                  (.then (fn [responses]
                           (let [agent-items (->> (or (:contracts (aget responses 0)) [])
                                                  (sort-by :id)
                                                  vec)
                                 role-items (->> (or (:contracts (aget responses 1)) [])
                                                 (map :id)
                                                 (remove str/blank?)
                                                 distinct
                                                 sort
                                                 vec)
                                 trigger-items (->> (or (:contracts (aget responses 2)) [])
                                                    (sort-by :id)
                                                    vec)
                                 pipeline-items (->> (or (:contracts (aget responses 3)) [])
                                                     (sort-by :id)
                                                     vec)]
                              (set-agents agent-items)
                              (set-triggers trigger-items)
                              (set-pipelines pipeline-items)
                              (set-role-options role-items)
                             (when (and (nil? selected-id) (seq agent-items))
                               (set-selected-id (:id (first agent-items))))
                             (when (and selected-id
                                        (seq agent-items)
                                        (not (some #(= selected-id (:id %)) agent-items)))
                               (set-selected-id (:id (first agent-items)))))))
                  (.catch (fn [err]
                            (set-error (str "Failed to load agent contracts: " (error-message err)))
                            (set-agents [])
                            (set-triggers [])
                            (set-pipelines [])
                            (set-role-options [])))
                  (.finally (fn []
                              (set-loading-library false)))))
            (load-contract! [contract-id]
              (set-loading-contract true)
              (set-error "")
              (set-notice nil)
              (-> (contracts-api/get-contract contract-id "agents")
                  (.then (fn [res]
                           (let [contract (or (:contract res) default-agent-contract)
                                 text (:ednText res)]
                             (set-draft contract)
                             (set-edn-text (or text (draft->edn contract)))
                             (set-parse-error nil))))
                  (.catch (fn [err]
                            (set-error (str "Failed to load agent contract: " (error-message err)))))
                  (.finally (fn []
                              (set-loading-contract false)))))
            (load-runtime! []
              (when can-control-runtime?
                (set-runtime-loading true)
                (set-runtime-error "")
                (-> (event-api/get-event-agent-control)
                    (.then (fn [res]
                             (set-runtime-status res)))
                    (.catch (fn [err]
                              (set-runtime-error (str "Failed to load event runtime: " (error-message err)))
                              (set-runtime-status nil)))
                    (.finally (fn []
                                (set-runtime-loading false))))))
            (handle-start-runtime! []
              (when can-control-runtime?
                (set-toggling-runtime true)
                (set-runtime-error "")
                (set-runtime-notice nil)
                (-> (event-api/start-event-agent-runtime)
                    (.then (fn [res]
                             (set-runtime-status res)
                             (set-runtime-notice {:tone :success :text "Event runtime started."})))
                    (.catch (fn [err]
                              (set-runtime-notice {:tone :error :text (error-message err)})))
                    (.finally (fn []
                                (set-toggling-runtime false))))))
            (handle-stop-runtime! []
              (when can-control-runtime?
                (set-toggling-runtime true)
                (set-runtime-error "")
                (set-runtime-notice nil)
                (-> (event-api/stop-event-agent-runtime)
                    (.then (fn [res]
                             (set-runtime-status res)
                             (set-runtime-notice {:tone :success :text "Event runtime stopped."})))
                    (.catch (fn [err]
                              (set-runtime-notice {:tone :error :text (error-message err)})))
                    (.finally (fn []
                                (set-toggling-runtime false))))))
            (handle-reset-runtime! []
              (when can-control-runtime?
                (set-resetting-runtime true)
                (set-runtime-error "")
                (set-runtime-notice nil)
                (-> (event-api/reset-event-agent-runtime)
                    (.then (fn [res]
                             (set-runtime-status res)
                             (set-runtime-notice {:tone :success
                                                  :text (str "Event runtime reset. Cleared "
                                                             (or (get-in res [:reset :deletedCount]) 0)
                                                             " state key(s); review schedules before restarting.")})))
                    (.catch (fn [err]
                              (set-runtime-notice {:tone :error :text (error-message err)})))
                    (.finally (fn []
                                (set-resetting-runtime false))))))
            (handle-run-job! [trigger-id]
              (when (and can-control-runtime? (seq trigger-id))
                (set-running-job-id trigger-id)
                (set-runtime-error "")
                (set-runtime-notice nil)
                (-> (event-api/fire-trigger trigger-id)
                    (.then (fn [_]
                             (set-runtime-notice {:tone :success :text (str "Fired trigger " trigger-id ".")})))
                    (.catch (fn [err]
                               (set-runtime-notice {:tone :error :text (error-message err)})))
                    (.finally (fn []
                                (set-running-job-id nil))))))
            (replace-draft! [next]
              (set-draft next)
              (set-edn-text (draft->edn next))
              (set-parse-error nil)
              (set-notice nil))
            (update-draft! [f & args]
              (replace-draft! (apply f (or draft default-agent-contract) args)))
            (handle-raw-change [text]
              (set-edn-text text)
              (set-notice nil)
              (try
                (let [parsed (parse-contract-edn text)]
                  (set-draft parsed)
                  (set-parse-error nil))
                (catch js/Error err
                  (set-parse-error (.-message err)))))
            (handle-new! []
              (set-selected-id nil)
              (replace-draft! default-agent-contract)
              (set-error ""))
            (handle-validate! []
              (set-validating true)
              (set-notice nil)
              (set-error "")
              (-> (contracts-api/validate-contract edn-text "agents")
                  (.then (fn [res]
                           (let [ok? (:ok res)
                                 errors (or (:errors res) [])]
                             (set-notice {:tone (if ok? :success :error)
                                          :text (if ok?
                                                  "Validation passed."
                                                  (str "Validation failed: " (count errors) " error(s)."))})
                             (when-let [contract (:contract res)]
                               (set-draft contract)))))
                  (.catch (fn [err]
                            (set-notice {:tone :error
                                         :text (error-message err)})))
                  (.finally (fn []
                              (set-validating false)))))
            (handle-save! []
              (if-let [contract-id (normalize-contract-id (:contract/id draft))]
                (do
                  (set-saving true)
                  (set-notice nil)
                  (set-error "")
                  (-> (contracts-api/save-contract contract-id edn-text "agents")
                      (.then (fn [res]
                               (let [contract (:contract res)
                                     text (:ednText res)
                                     validation (:validation res)
                                     ok? (:ok validation)
                                     errors (or (:errors validation) [])]
                                 (set-selected-id contract-id)
                                 (set-draft (or contract draft))
                                 (set-edn-text (or text edn-text))
                                 (set-parse-error nil)
                                 (set-notice {:tone (if ok? :success :error)
                                              :text (if ok?
                                                      (str "Saved " contract-id ".")
                                                      (str "Saved " contract-id ", but validation has " (count errors) " error(s)."))})
                                  (load-agent-library!))))
                      (.catch (fn [err]
                                (set-notice {:tone :error
                                             :text (error-message err)})))
                      (.finally (fn []
                                  (set-saving false)))))
                (set-notice {:tone :error :text "Missing :contract/id."})))]

      (hooks/use-effect
       [location]
       (set-tab (get-tab-from-location location))
       nil)

      (hooks/use-effect
       []
       (load-agent-library!)
       nil)

      (hooks/use-effect
       [selected-id]
       (if selected-id
         (load-contract! selected-id)
         (replace-draft! default-agent-contract))
       nil)

      (hooks/use-effect
       [tab selected-id (:contract/id draft) edn-text]
       (when chat
         (try
           (let [pin (.-pinContextItem chat)]
             (when pin
                (when-let [contract-id (or selected-id (:contract/id draft))]
                  (if (= tab "audit")
                    (pin #js {:id (str "agent-audit:" contract-id)
                              :title (str contract-id " audit")
                              :path "/agents?tab=audit"
                              :snippet (str "Active and historical runs for agent contract " contract-id ".")
                              :kind "file"})
                    (pin #js {:id (str "agent:" contract-id)
                              :title contract-id
                              :path (str "/ops/contracts/agents/" contract-id)
                              :snippet (if edn-text
                                         (subs edn-text 0 (min 120 (count edn-text)))
                                         "")
                              :kind "file"})))))
            (catch js/Error _ nil)))
       nil)

      (hooks/use-effect
       [tab selected-id (:contract/id draft)]
       (when (and chat (= tab "audit"))
         (when-let [contract-id (selected-agent-contract-id selected-id draft)]
           (when-not (= contract-id "new-agent")
             (when-let [set-active-agent (aget chat "setActiveAgentId")]
               (set-active-agent contract-id)))))
       nil)

      (let [runtime-running? (boolean (get-in runtime-status [:runtime :running]))
            selected-contract-id (selected-agent-contract-id selected-id draft)
            tab-buttons (d/div {:class-name "flex items-center gap-2"}
                               (d/button {:type "button"
                                          :on-click #(navigate (set-tab-in-location location "contracts"))
                                          :class-name (str "rounded-md px-2 py-1 text-xs font-medium transition "
                                                           (if (= tab "contracts")
                                                             "bg-sky-600 text-slate-50 hover:bg-sky-500"
                                                             "bg-transparent text-slate-300 hover:bg-slate-800"))}
                                         "🧠 Contracts")
                               (d/button {:type "button"
                                          :on-click #(navigate (set-tab-in-location location "audit"))
                                          :class-name (str "rounded-md px-2 py-1 text-xs font-medium transition "
                                                           (if (= tab "audit")
                                                             "bg-sky-600 text-slate-50 hover:bg-sky-500"
                                                             "bg-transparent text-slate-300 hover:bg-slate-800"))}
                                         "📜 Audit"))]
        (d/div {:data-page "agents"
                :class-name "h-full min-h-0 overflow-hidden bg-slate-950 text-slate-100"}
               ($ layout/WorkbenchShell
                  ($ layout/WorkbenchPanel
                     {:edge "left"
                      :label "Agents"
                      :storage-key "knoxx_agents_left"
                      :default-width 320
                      :min-width 260
                      :max-width 520
                      :header (d/div {:class-name "flex min-w-0 flex-1 items-center gap-2"}
                                     (d/div {:class-name "flex min-w-0 items-center gap-2"}
                                            (d/span {:class-name "text-xs font-semibold text-slate-100"} "Agents")
                                            (d/span {:class-name "rounded bg-slate-900 px-1.5 py-0.5 text-[10px] text-slate-400"}
                                                    (str (count agents))))
                                     (d/div {:class-name "ml-auto flex items-center gap-1"}
                                            (d/button {:type "button"
                                                       :on-click handle-new!
                                                       :title "New agent"
                                                       :class-name (runtime-button-class :default)}
                                                      "➕")
                                             (d/button {:type "button"
                                                        :on-click load-agent-library!
                                                        :disabled loading-library
                                                        :title "Refresh contracts and triggers"
                                                        :class-name (runtime-button-class :default)}
                                                       (if loading-library "⏳" "↻"))))}
                     ($ agent-workbench-sidebar
                        {:agents agents
                         :triggers triggers
                         :pipelines pipelines
                         :selected-id selected-id
                         :on-select set-selected-id
                         :on-new handle-new!
                         :loading-agents loading-library
                         :selected-contract-id selected-contract-id
                         :runtime-status runtime-status
                         :loading-runtime runtime-loading
                         :runtime-error runtime-error
                         :runtime-notice runtime-notice
                         :can-control can-control-runtime?
                         :running-job-id running-job-id
                         :toggling-runtime toggling-runtime
                         :resetting-runtime resetting-runtime
                         :on-refresh-runtime load-runtime!
                         :on-start-runtime handle-start-runtime!
                         :on-stop-runtime handle-stop-runtime!
                         :on-reset-runtime handle-reset-runtime!
                         :on-run handle-run-job!
                         :on-inspect-contract set-selected-id
                         :audit? (= tab "audit")
                         :chat chat
                         :hide-header true}))
                  ($ layout/WorkbenchMain
                     (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
                            (d/header {:class-name "flex shrink-0 items-center justify-between gap-3 border-b border-slate-800 bg-slate-950 px-4 py-2"}
                                      (d/div {:class-name "min-w-0"}
                                             (d/h1 {:class-name "truncate text-lg font-semibold text-slate-100"}
                                                   (if (= tab "audit") "Agent Audit" "Agents"))
                                             (d/div {:class-name "truncate text-xs text-slate-500"}
                                                    (if (= tab "audit")
                                                      (str "Active and historical runs for " (or selected-contract-id "selected agent"))
                                                      (or selected-id "New agent contract"))))
                                      tab-buttons)
                            (if (= tab "audit")
                              (d/div {:class-name "min-h-0 flex-1 overflow-hidden bg-slate-950"}
                                     ($ ChatWorkspacePane {:controller chat
                                                           :showFiles false
                                                           :showFilesToggle false
                                                           :showCanvasToggle true
                                                           :onShowFiles #()}))
                              (if loading-contract
                                (d/div {:class-name "flex min-w-0 flex-1 items-center justify-center bg-slate-950/40 text-sm text-slate-400"}
                                       "Loading contract…")
                                ($ contract-editor {:draft draft
                                                    :edn-text edn-text
                                                    :parse-error parse-error
                                                    :role-options role-options
                                                    :can-save can-save-contracts?
                                                    :saving saving
                                                    :validating validating
                                                    :notice notice
                                                    :error error
                                                    :on-update update-draft!
                                                    :on-raw-change handle-raw-change
                                                    :on-save handle-save!
                                                    :on-validate handle-validate!})))))
                  (when-not (= tab "audit")
                    ($ agent-chat-panel {:chat chat
                                         :label "Agent Chat"
                                         :storage-key "knoxx_agents_chat"}))))))))
