(ns knoxx.frontend.pages.agents.runtime-view
  "Runtime schedules and contract selection views."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.admin.event-agent-utils :as event-u]
            [knoxx.frontend.components.agent-audit.session-list :as audit]
            [knoxx.frontend.pages.agents.runtime-model :as runtime-model]))

(defn runtime-info-row
  "Pair a compact runtime fact with its label." [label value]
  (d/div {:class-name "flex items-center justify-between gap-2 text-[11px] leading-4"}
         (d/span {:class-name "text-slate-500"} label)
         (d/span {:class-name "min-w-0 truncate text-right text-slate-200"} value)))

(hx/defnc runtime-job-heading
  "Identify a scheduled job and its latest status."
  [{:keys [job runtime enabled? compact]}]
(d/div {:class-name "flex items-center justify-between gap-2"}
                  (d/div {:class-name "min-w-0"}
                         (d/div {:class-name "truncate text-xs font-semibold text-slate-100"}
                                (:name job))
                         (when-not compact
                           (d/div {:class-name "mt-0.5 truncate font-mono text-[10px] text-slate-500"}
                                  (:id job))))
                  (d/span {:class-name (str "shrink-0 rounded-full px-2 py-0.5 text-[10px] uppercase tracking-wide "
                                            (runtime-model/runtime-status-class runtime enabled?))}
                          (runtime-model/runtime-status-label runtime enabled?))))

(hx/defnc runtime-job-actions
  "Expose inspection and capability-checked trigger execution."
  [{:keys [job inspectable? on-inspect contract-id on-run can-control running? enabled?]}]
(d/div {:class-name "mt-1.5 grid grid-cols-2 gap-1"}
                   (d/button {:type "button"
                              :on-click #(when (and inspectable? on-inspect contract-id)
                                           (on-inspect contract-id))
                              :disabled (or (not inspectable?) (nil? on-inspect) (nil? contract-id))
                              :title (if inspectable? "Inspect target agent" "Target is not an agent contract")
                              :class-name (runtime-model/runtime-button-class :default)}
                             "🔎 EDN")
                  (d/button {:type "button"
                             :on-click #(on-run (:id job))
                             :disabled (or (not can-control) running? (not enabled?))
                             :title "Run now"
                             :class-name (runtime-model/runtime-button-class :primary)}
                            (if running? "⏳ Queue…" "▶ Run"))))

(hx/defnc runtime-job-card
  "Display a schedule projection and its inspection or execution capabilities."
  [{:keys [job runtime selected compact on-run on-inspect can-control running-job-id]}]
  (let [running? (= running-job-id (:id job))
        enabled? (:enabled job)
        contract-id (runtime-model/event-job-contract-id job)
        inspectable? (= "agent" (:contractSourceKind job))]
    (d/div {:class-name (str "rounded-lg border p-2 "
                             (if selected
                               "border-sky-500/40 bg-sky-500/5"
                               "border-slate-800 bg-slate-950/50"))}
           (hx/$ runtime-job-heading {:job job :runtime runtime :enabled? enabled? :compact compact})
           (d/div {:class-name "mt-1 grid grid-cols-2 gap-x-2 gap-y-0.5"}
                  (runtime-info-row "Trigger" (or (get-in job [:trigger :kind]) "—"))
                  (runtime-info-row "Runs" (str (or (:runCount runtime) 0)))
                  (runtime-info-row "Schedule" (runtime-model/schedule-label job runtime))
                  (runtime-info-row "Next" (event-u/to-local-date-time (:nextRunAt runtime)))
                  (when-not compact
                    (runtime-info-row "Last" (event-u/to-local-date-time (:lastFinishedAt runtime)))))
           (when (:lastError runtime)
             (d/div {:class-name "mt-1 rounded border border-rose-500/30 bg-rose-500/10 px-1.5 py-1 text-[10px] text-rose-200"}
                    (:lastError runtime)))
           (hx/$ runtime-job-actions {:job job :inspectable? inspectable? :on-inspect on-inspect :contract-id contract-id
                                        :on-run on-run :can-control can-control :running? running? :enabled? enabled?}))))

(hx/defnc runtime-schedule-list
  "List other contract schedules with the selected contract sorted first."
  [{:keys [jobs runtime-status selected-contract-id exclude-contract-id on-run on-inspect can-control running-job-id]}]
  (let [rows (->> jobs
                  (filter runtime-model/event-job-contract-id)
                  (remove #(= exclude-contract-id (runtime-model/event-job-contract-id %)))
                  (sort-by (fn [job]
                             [(if (= selected-contract-id (runtime-model/event-job-contract-id job)) 0 1)
                              (or (:contractSourceKey job) (:id job))
                              (:id job)])))]
    (d/div {:class-name "space-y-2"}
           (if (seq rows)
             (for [job rows]
               (let [runtime (or (runtime-model/runtime-for-job runtime-status (:id job)) {})]
                 (hx/$ runtime-job-card {:key (:id job)
                                      :job job
                                      :runtime runtime
                                      :selected (= selected-contract-id (runtime-model/event-job-contract-id job))
                                      :compact true
                                      :on-run on-run
                                      :on-inspect on-inspect
                                      :can-control can-control
                                      :running-job-id running-job-id})))
             (d/div {:class-name "rounded-lg border border-dashed border-slate-800 p-3 text-xs text-slate-500"}
                    "No contract-backed runtime jobs are loaded.")))))

(hx/defnc sidebar-section-toggle
  "Toggle a workbench section while showing its item count and status."
  [{:keys [title open? on-toggle status] item-count :count}]
  (d/button {:type "button"
             :on-click on-toggle
             :class-name "flex shrink-0 items-center justify-between gap-2 border-b border-slate-900/80 px-2 py-1 text-left hover:bg-slate-900/60"}
            (d/div {:class-name "flex min-w-0 items-center gap-2"}
                   (d/span {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"} title)
                   status
                   (d/span {:class-name "rounded bg-slate-900 px-1 py-0.5 text-[9px] text-slate-500"} (str item-count)))
            (d/span {:class-name "text-[10px] text-slate-500"} (if open? "▾" "▸"))))

(hx/defnc agent-contract-row
  "Select an agent contract and display its enablement."
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

(hx/defnc agent-contract-section
  "Display the collapsible contract library and loading state."
  [{:keys [agents selected-id loading-agents open? on-toggle on-select]}]
  (d/section {:class-name (str "flex min-h-0 flex-col border-b border-slate-800 " (if open? "basis-[44%]" "shrink-0"))}
             (hx/$ sidebar-section-toggle {:title "Contracts" :count (count agents) :open? open? :on-toggle on-toggle})
             (when open?
               (d/div {:class-name "min-h-0 flex-1 overflow-y-auto"}
                      (cond
                        loading-agents (d/div {:class-name "p-2 text-xs text-slate-400"} "Loading…")
                        (seq agents) (for [agent agents]
                                       (hx/$ agent-contract-row {:key (:id agent) :agent agent :active (= selected-id (:id agent)) :on-select on-select}))
                        :else (d/div {:class-name "p-2 text-xs text-slate-500"} "No agents"))))))

(hx/defnc selected-runtime-jobs
  "Show runtime jobs mapped to the selected agent contract."
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
               (let [runtime (or (runtime-model/runtime-for-job runtime-status (:id job)) {})]
                 (hx/$ runtime-job-card {:key (:id job) :job job :runtime runtime :selected true :compact false
                                      :on-run on-run :on-inspect on-inspect-contract
                                      :can-control can-control :running-job-id running-job-id})))
              (d/div {:class-name "rounded border border-dashed border-slate-800 p-2 text-[11px] text-slate-500"} "No triggers target this agent.")))))

(hx/defnc runtime-schedules-section
  "Display schedules belonging to other contracts."
  [{:keys [jobs runtime-status selected-contract-id other-schedule-count on-run on-inspect-contract can-control running-job-id]}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-1"}
         (d/div {:class-name "flex shrink-0 items-center justify-between gap-2"}
                (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"} "All triggers")
                (d/span {:class-name "text-[10px] text-slate-500"} (str other-schedule-count)))
         (d/div {:class-name "min-h-0 flex-1 overflow-y-auto pr-1"}
                (hx/$ runtime-schedule-list {:jobs jobs :runtime-status runtime-status
                                          :selected-contract-id selected-contract-id
                                          :exclude-contract-id selected-contract-id
                                          :on-run on-run :on-inspect on-inspect-contract
                                          :can-control can-control :running-job-id running-job-id}))))

(hx/defnc agent-runtime-section
  "Compose the selected job and other schedules in the runtime section."
  [{:keys [open? on-toggle runtime-running? schedule-count jobs selected-jobs runtime-status
           selected-contract-id loading-runtime other-schedule-count on-run on-inspect-contract
           can-control running-job-id]}]
  (d/section {:class-name (str "flex min-h-0 flex-col " (if open? "flex-1" "shrink-0"))}
             (hx/$ sidebar-section-toggle {:title "Triggers" :count schedule-count :open? open? :on-toggle on-toggle
                                        :status (d/span {:class-name (str "rounded px-1.5 py-0.5 text-[9px] uppercase "
                                                                           (if runtime-running? "bg-emerald-500/10 text-emerald-300" "bg-slate-700/40 text-slate-300"))}
                                                        (if runtime-running? "live" "contracts"))})
             (when open?
               (d/div {:class-name "flex min-h-0 flex-1 flex-col gap-2 p-2"}
                      (hx/$ selected-runtime-jobs {:selected-contract-id selected-contract-id
                                                :selected-jobs selected-jobs
                                                :runtime-status runtime-status
                                                :loading-runtime loading-runtime
                                                :on-run on-run
                                                :on-inspect-contract on-inspect-contract
                                                :can-control can-control
                                                :running-job-id running-job-id})
                      (hx/$ runtime-schedules-section {:jobs jobs
                                                    :runtime-status runtime-status
                                                    :selected-contract-id selected-contract-id
                                                    :other-schedule-count other-schedule-count
                                                    :on-run on-run
                                                    :on-inspect-contract on-inspect-contract
                                                    :can-control can-control
                                                    :running-job-id running-job-id})))))

(hx/defnc audit-sessions-section
  "Display sessions associated with the selected agent contract."
  [{:keys [open? on-toggle chat selected-contract-id]}]
  (d/section {:class-name (str "flex min-h-0 flex-col border-b border-slate-800 " (if open? "flex-1" "shrink-0"))}
             (hx/$ sidebar-section-toggle {:title "Audit sessions" :count "chat" :open? open? :on-toggle on-toggle})
             (when (and open? chat)
               (hx/$ audit/agent-audit-session-list {:controller chat
                                            :built-in-contract-id selected-contract-id}))))

(hx/defnc runtime-notices
  "Keep unavailable permissions and command refusals visible."
  [{:keys [can-control runtime-notice runtime-error]}]
  (hx/<>
(when-not can-control
               (d/div {:class-name "shrink-0 border-b border-slate-800 px-2 py-1 text-[11px] text-amber-300"} "Runtime locked"))
             (when runtime-notice
               (d/div {:class-name (str "shrink-0 border-b px-2 py-1 text-[11px] "
                                        (case (:tone runtime-notice) :success "border-emerald-500/20 bg-emerald-500/10 text-emerald-200" "border-rose-500/20 bg-rose-500/10 text-rose-200"))}
                      (:text runtime-notice)))
             (when (seq runtime-error)
               (d/div {:class-name "shrink-0 border-b border-rose-500/20 bg-rose-500/10 px-2 py-1 text-[11px] text-rose-200"} runtime-error))))

(hx/defnc agent-workbench-sidebar
  "Compose contract selection, audit history and runtime projections."
  [{:keys [agents triggers pipelines selected-id on-select loading-agents selected-contract-id runtime-status loading-runtime
            runtime-error runtime-notice can-control running-job-id on-run on-inspect-contract audit? chat]}]
  (let [[agents-open? set-agents-open!] (hooks/use-state true)
        [runtime-open? set-runtime-open!] (hooks/use-state true)
        [audit-sessions-open? set-audit-sessions-open!] (hooks/use-state true)
        jobs (runtime-model/trigger-schedule-jobs agents triggers pipelines)
        contract-jobs (filter runtime-model/event-job-contract-id jobs)
        selected-jobs (filterv #(runtime-model/contract-job? selected-contract-id %) jobs)
        runtime-running? (boolean (get-in runtime-status [:runtime :running]))]
    (d/aside {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950/60"}
             (hx/$ runtime-notices {:can-control can-control :runtime-notice runtime-notice :runtime-error runtime-error})
             (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
                    (hx/$ agent-contract-section {:agents agents :selected-id selected-id :loading-agents loading-agents
                                               :open? agents-open? :on-toggle #(set-agents-open! (not agents-open?)) :on-select on-select})
                    (when audit?
                      (hx/$ audit-sessions-section {:open? audit-sessions-open?
                                                 :on-toggle #(set-audit-sessions-open! (not audit-sessions-open?))
                                                 :chat chat
                                                 :selected-contract-id selected-contract-id}))
                    (hx/$ agent-runtime-section {:open? runtime-open? :on-toggle #(set-runtime-open! (not runtime-open?))
                                              :runtime-running? runtime-running? :schedule-count (count contract-jobs)
                                              :jobs jobs :selected-jobs selected-jobs :runtime-status runtime-status
                                              :selected-contract-id selected-contract-id :loading-runtime loading-runtime
                                              :other-schedule-count (count (remove #(= selected-contract-id (runtime-model/event-job-contract-id %)) contract-jobs))
                                              :on-run on-run :on-inspect-contract on-inspect-contract
                                              :can-control can-control :running-job-id running-job-id})))))
