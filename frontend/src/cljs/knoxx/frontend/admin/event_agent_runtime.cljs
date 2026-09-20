(ns knoxx.frontend.admin.event-agent-runtime
  "Runtime snapshot and live stats components.

   Operates on CLJS data (keyword keys)."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-utils :as u]))

(hx/defnc stat-card
  "A single stat display card."
  [{:keys [label value]}]
  (d/div
    (d/div {:class-name "text-[10px] uppercase tracking-wide text-slate-500"} label)
    (d/div {:class-name "mt-0.5 text-xs text-slate-200"} value)))

(hx/defnc runtime-snapshot
  "Runtime snapshot card showing status, runs, last finished, next run."
  [{:keys [runtime]}]
  (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/60 p-2.5"}
         (d/div {:class-name "text-xs font-semibold text-slate-100"} "Runtime snapshot")
         (d/div {:class-name "mt-2 grid gap-x-3 gap-y-1.5 sm:grid-cols-2 text-xs"}
                (hx/$ stat-card {:label "Status"
                              :value (hx/$ c/status-badge {:status (:lastStatus runtime)
                                                        :enabled true
                                                        :running (:running runtime)})})
                (hx/$ stat-card {:label "Runs"
                              :value (d/div {:class-name "text-base font-semibold text-slate-100"}
                                            (or (:runCount runtime) 0))})
                (hx/$ stat-card {:label "Last finished"
                              :value (u/to-local-date-time (:lastFinishedAt runtime))})
                (hx/$ stat-card {:label "Next run"
                              :value (u/to-local-date-time (:nextRunAt runtime))}))))

(hx/defnc live-runtime
  "Live runtime details card."
  [{:keys [runtime]}]
  (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/60 p-2.5"}
         (d/div {:class-name "text-xs font-semibold text-slate-100"} "Live runtime")
         (d/div {:class-name "mt-2 grid gap-x-3 gap-y-1 text-xs text-slate-300"}
                (d/div {:class-name "flex items-center justify-between gap-2"}
                       (d/span {:class-name "text-slate-500"} "Schedule")
                       (d/span {:class-name "text-right text-slate-200"}
                               (or (:scheduleLabel runtime) "—")))
                (d/div {:class-name "flex items-center justify-between gap-2"}
                       (d/span {:class-name "text-slate-500"} "Last started")
                       (d/span {:class-name "text-right text-slate-200"}
                               (u/to-local-date-time (:lastStartedAt runtime))))
                (d/div {:class-name "flex items-center justify-between gap-2"}
                       (d/span {:class-name "text-slate-500"} "Last finished")
                       (d/span {:class-name "text-right text-slate-200"}
                               (u/to-local-date-time (:lastFinishedAt runtime))))
                (d/div {:class-name "flex items-center justify-between gap-2"}
                       (d/span {:class-name "text-slate-500"} "Duration")
                       (d/span {:class-name "text-right text-slate-200"}
                               (if (:lastDurationMs runtime)
                                 (str (:lastDurationMs runtime) " ms")
                                 "—"))))
         (when (:lastError runtime)
           (d/div {:class-name "mt-2 rounded border border-rose-500/30 bg-rose-500/10 px-2 py-1 text-[11px] text-rose-200"}
                  (:lastError runtime)))))

(hx/defnc quick-reference
  "Quick reference card with job metadata."
  [{:keys [job]}]
  (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/60 p-2.5"}
         (d/div {:class-name "text-xs font-semibold text-slate-100"} "Quick reference")
         (d/div {:class-name "mt-2 space-y-1 text-[11px] text-slate-400"}
                (d/div (d/span {:class-name "text-slate-500"} "Job id: ")
                       (d/code {:class-name "font-mono text-slate-200"} (:id job)))
                (d/div (d/span {:class-name "text-slate-500"} "Source mode: ") (:mode (:source job)))
                (d/div (d/span {:class-name "text-slate-500"} "Trigger cadence: ")
                       (str (:cadenceMinutes (:trigger job)) " min"))
                (d/div (d/span {:class-name "text-slate-500"} "Event kinds: ")
                       (if (seq (:eventKinds (:trigger job)))
                         (str/join ", " (:eventKinds (:trigger job)))
                         "none")))))
