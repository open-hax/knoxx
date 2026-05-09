(ns knoxx.frontend.admin.event-agents
  "Main event agent runtime control panel.
   Replaces the legacy DiscordSection.tsx."
  (:require [helix.core :as hx :refer [$ defnc]]
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
                             (.-name job))
                       ($ c/badge {:tone (if (.-enabled job) :success :warn)}
                          (if (.-enabled job) "Enabled" "Disabled"))
                       (d/span {:class-name "text-[11px] text-slate-500"}
                               (str (.. job -source -kind) " · " (.. job -trigger -kind) " · "
                                    (if (.-contractSourceId job) "contract" "custom")))
                       (when (.-running runtime)
                         ($ c/badge {:tone :info} "Running now")))
                (d/p {:class-name "mt-1 text-xs text-slate-400"}
                     (or (.-description job) "No description provided."))
                (when (.-contractSourceId job)
                  (d/div {:class-name "mt-1 text-[11px] text-slate-500"}
                         "Contract-backed from "
                         (d/code {:class-name "font-mono text-slate-300"}
                                 (str (or (.-contractSourceKind job) "agent") ":" (.-contractSourceId job))))))
         (d/button {:type "button"
                    :on-click on-run
                    :disabled (or (not can-manage) (= running-job-id (.-id job)))
                    :class-name (str "inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 "
                                     "px-2.5 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                   (if (= running-job-id (.-id job)) "Queueing…" "Run now"))))

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
                                    :on-update on-update
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
                       "TODO: EventAgentScheduleReview"))
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
