(ns knoxx.frontend.admin.event-agents
  "Main event agent runtime control panel.
    Replaces the legacy DiscordSection.tsx.

    All data is CLJS (keyword keys)."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-utils :as u]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-sidebar :as sidebar]
            [knoxx.frontend.admin.event-agent-runtime :as runtime]
            [knoxx.frontend.admin.event-agent-editor :as editor]))

(defnc loading-state
  "Shown while control plane data is loading."
  []
  (d/div {:class-name "text-sm text-slate-300"}
         "Loading event-agent control plane…"))

(defnc empty-state
  "Shown when no job is selected."
  []
  (d/div {:class-name "rounded-xl border border-dashed border-slate-800 bg-slate-950/30 px-4 py-8 text-center text-xs text-slate-400"}
         "Select an event agent from the sidebar to inspect it."))

(defnc job-header
  "Header row for the selected job."
  [{:keys [job runtime on-run can-manage running-job-id]}]
  (d/div {:class-name "flex flex-col gap-2 border-b border-slate-800 pb-3 md:flex-row md:items-start md:justify-between"}
         (d/div {:class-name "min-w-0"}
                (d/div {:class-name "flex flex-wrap items-center gap-1.5"}
                       (d/h3 {:class-name "text-base font-semibold text-slate-100"}
                             (:name job))
                       ($ c/badge {:tone (if (:enabled job) :success :warn)}
                          (if (:enabled job) "Enabled" "Disabled"))
                       (d/span {:class-name "text-[11px] text-slate-500"}
                               (str (get-in job [:source :kind]) " · " (get-in job [:trigger :kind]) " · "
                                    (if (:contractSourceId job) "contract" "custom")))
                       (when (:running runtime)
                         ($ c/badge {:tone :info} "Running now")))
                (when (:contractSourceId job)
                  (d/div {:class-name "mt-1 text-[11px] text-slate-500"}
                         "Contract-backed from "
                         (d/code {:class-name "font-mono text-slate-300"}
                                 (str (or (:contractSourceKind job) "agent") ":" (:contractSourceId job))))))
         (d/button {:type "button"
                    :on-click on-run
                    :disabled (or (not can-manage) (= running-job-id (:id job)))
                    :class-name (str "inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 "
                                     "px-2.5 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                   (if (= running-job-id (:id job)) "Queueing…" "Run now"))))

;; ---------------------------------------------------------------------------
;; Schedule review
;; ---------------------------------------------------------------------------

(defn- contract-key-for-job [job]
  (or (:contractSourceKey job)
      (when (:contractSourceId job)
        (str (or (:contractSourceKind job) "agent") ":" (:contractSourceId job)))
      (str "custom:" (:id job))))

(defn- status-tone [runtime enabled]
  (cond
    (not enabled) :warn
    (:running runtime) :info
    (= (:lastStatus runtime) "ok") :success
    (= (:lastStatus runtime) "error") :danger
    :else :default))

(defn- schedule-row [job runtime-jobs]
  (let [runtime (u/runtime-for-job runtime-jobs (:id job))]
    {:job job
     :runtime runtime
     :contract-key (contract-key-for-job job)
     :next-run-at (when (and runtime (number? (:nextRunAt runtime)))
                    (:nextRunAt runtime))}))

(defn- sort-schedule-rows [left right]
  (let [left-cron (if (= (get-in (:job left) [:trigger :kind]) "cron") 0 1)
        right-cron (if (= (get-in (:job right) [:trigger :kind]) "cron") 0 1)]
    (if (not= left-cron right-cron)
      (- left-cron right-cron)
      (let [left-next (or (:next-run-at left) js/Number.MAX_SAFE_INTEGER)
            right-next (or (:next-run-at right) js/Number.MAX_SAFE_INTEGER)]
        (if (not= left-next right-next)
          (- left-next right-next)
          (compare (:contract-key left) (:contract-key right)))))))

(defn- schedule-trigger-label [job]
  (if (= (get-in job [:trigger :kind]) "cron")
    (str "Every " (get-in job [:trigger :cadenceMinutes]) " min")
    (let [event-kinds (get-in job [:trigger :eventKinds])]
      (if (and event-kinds (seq event-kinds))
        (str/join ", " event-kinds)
        "event-driven"))))

(defn- schedule-info-cell [label content]
  (d/div {:class-name "min-w-0 rounded-lg border border-slate-800/70 bg-slate-950/40 p-2"}
         (d/div {:class-name "text-[10px] uppercase tracking-wide text-slate-500"} label)
         (d/div {:class-name "mt-1 min-w-0 break-words text-xs text-slate-200"} content)))

(defn- schedule-card [row selected-job-id on-select-job]
  (let [job (:job row)
        runtime (:runtime row)
        selected (= selected-job-id (:id job))
        enabled (:enabled job)
        trigger-kind (get-in job [:trigger :kind])
        source-kind (get-in job [:source :kind])
        source-mode (get-in job [:source :mode])
        contract-label (or (:contractSourceKey job)
                           (when (:contractSourceId job)
                             (str (or (:contractSourceKind job) "agent") ":" (:contractSourceId job)))
                           "custom")]
    (d/div {:key (:id job)
            :class-name (str "rounded-xl border p-3 transition "
                             (if selected
                               "border-sky-500/40 bg-sky-500/5 shadow-[0_0_0_1px_rgba(14,165,233,0.15)]"
                               "border-slate-800 bg-slate-950/50 hover:border-slate-700"))}
           (d/div {:class-name "flex flex-col gap-3"}
                  (d/div {:class-name "flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between"}
                         (d/div {:class-name "min-w-0 flex-1 space-y-2"}
                                (d/h4 {:class-name "min-w-0 break-words text-sm font-semibold text-slate-100"}
                                      (:name job))
                                (d/div {:class-name "flex flex-wrap items-center gap-2 text-[11px] text-slate-500"}
                                       (d/span {:class-name "rounded-full border border-slate-800 bg-slate-950 px-2 py-0.5 font-medium text-slate-400"}
                                               "Contract")
                                       (d/code {:class-name "rounded-md border border-slate-800 bg-slate-900 px-2 py-0.5 font-mono text-slate-200 break-all"}
                                               contract-label)
                                       (d/span {:class-name "break-words"}
                                               (str source-kind " · " source-mode))))
                         (d/button {:type "button"
                                    :on-click #(on-select-job (:id job))
                                    :class-name (str "inline-flex shrink-0 items-center justify-center rounded-lg border px-2.5 py-1.5 text-xs font-medium transition "
                                                     (if selected
                                                       "border-sky-500/40 bg-sky-500/10 text-sky-100 hover:bg-sky-500/15"
                                                       "border-slate-700 bg-slate-900 text-slate-100 hover:bg-slate-800"))}
                                   (if selected "Selected" "Inspect")))
                  (d/div {:class-name "flex flex-wrap items-center gap-2"}
                         ($ c/badge {:tone (if enabled :success :warn)}
                            (if enabled "Enabled" "Disabled"))
                         ($ c/badge {:tone (if (= trigger-kind "cron") :info :default)}
                            trigger-kind)
                         (when (:running runtime)
                           ($ c/badge {:tone :info} "Running now"))
                         (when selected
                           ($ c/badge {:tone :info} "Selected")))
                  (d/div {:class-name "grid gap-2 sm:grid-cols-2 xl:grid-cols-4"}
                         (schedule-info-cell
                          "Trigger"
                          ($ c/badge {:tone (if (= trigger-kind "cron") :info :default)}
                             trigger-kind))
                         (schedule-info-cell
                          "Schedule"
                          (schedule-trigger-label job))
                         (schedule-info-cell
                          "Next run"
                          (or (u/to-local-date-time (:next-run-at row))
                              "Not scheduled"))
                         (schedule-info-cell
                          "Status"
                          (d/div {:class-name "flex flex-wrap items-center gap-2"}
                                 ($ c/badge {:tone (status-tone runtime enabled)}
                                    (if enabled
                                      (if (:running runtime)
                                        "running"
                                        (or (:lastStatus runtime) "ready"))
                                      "disabled"))
                                 (d/span {:class-name "text-[11px] text-slate-500"}
                                         (str (or (:runCount runtime) 0) " runs")))))))))

(defnc schedule-review
  "Cards showing all event agent jobs with schedule, status, and review actions."
  [{:keys [jobs runtime-jobs on-select-job selected-job-id]}]
  (let [rows (sort sort-schedule-rows
                   (map #(schedule-row % runtime-jobs) jobs))]
    (d/div {:class-name "space-y-2"}
           (for [row rows]
             (schedule-card row selected-job-id on-select-job)))))

(defnc selected-job-panel
  "Right panel showing the selected job details and editor."
  [{:keys [job runtime on-update on-run can-manage saving-control running-job-id]}]
  (d/div {:class-name "rounded-xl border border-slate-800 bg-slate-950/40 p-3"}
         ($ job-header {:job job
                        :runtime runtime
                        :on-run on-run
                        :can-manage can-manage
                        :running-job-id running-job-id})
         (d/div {:class-name "mt-3 space-y-3"}
                (d/div {:class-name "grid gap-2 xl:grid-cols-3"}
                       ($ runtime/runtime-snapshot {:runtime runtime})
                       ($ runtime/live-runtime {:runtime runtime})
                       ($ runtime/quick-reference {:job job}))
                ($ editor/job-form {:job job
                                    :on-update (fn [field value]
                                                 (let [patch (case field
                                                               :enabled {:enabled value}
                                                               :trigger-kind {:trigger {:kind value}}
                                                               :source-kind {:source {:kind value}}
                                                               :source-mode {:source {:mode value}}
                                                               :cadence {:trigger {:cadenceMinutes value}}
                                                               :event-kinds {:trigger {:eventKinds value}}
                                                               :role {:agentSpec {:role value}}
                                                               :model {:agentSpec {:model value}}
                                                               :thinking-level {:agentSpec {:thinkingLevel value}}
                                                               :description {:description value}
                                                               :system-prompt {:agentSpec {:systemPrompt value}}
                                                               {})]
                                                   (on-update (:id job) patch)))
                                    :can-manage can-manage
                                    :saving-control saving-control}))))

(defnc main-content
  "The two-column main area: schedule review (left) + job editor (right)."
  [{:keys [jobs runtime-jobs selected-job-id on-select-job
           selected-job runtime on-update on-run
           can-manage saving-control running-job-id]}]
  (d/div {:class-name "grid gap-3 min-w-0 h-full min-h-0 grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]"}
         ;; Left: Schedule review
         (d/div {:class-name "flex flex-col h-full overflow-hidden rounded-xl border border-slate-800 bg-slate-950/40"}
                (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-2"}
                       (d/div {:class-name "text-sm font-semibold text-slate-100"}
                              "Schedule review")
                       (d/div {:class-name "text-[11px] text-slate-500"}
                              "Review cadence and next-run timing."))
                (d/div {:class-name "flex-1 min-h-0 overflow-y-auto p-2"}
                       ($ schedule-review {:jobs jobs
                                            :runtime-jobs runtime-jobs
                                            :on-select-job on-select-job
                                            :selected-job-id selected-job-id})))
         ;; Right: Job editor
         (d/div {:class-name "h-full overflow-y-auto min-w-0 space-y-3 pr-1"}
                (if selected-job
                  ($ selected-job-panel {:job selected-job
                                          :runtime runtime
                                          :on-update on-update
                                          :on-run on-run
                                          :can-manage can-manage
                                          :saving-control saving-control
                                          :running-job-id running-job-id})
                  ($ empty-state)))))
