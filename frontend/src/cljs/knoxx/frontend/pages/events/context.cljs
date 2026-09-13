(ns knoxx.frontend.pages.events.context
  "Native role and selected-job context for the Events workspace and chat."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]))

(defn- snippet [value max-length]
  (let [text (str/trim (str (or value "")))]
    (if (> (count text) max-length)
      (str (subs text 0 max-length) "...")
      text)))

(hx/defnc access-card
  "Show the current role's runtime and tool-catalog capabilities."
  [{:keys [can-control? can-read-tools? tools]}]
  (d/section {:class-name "border-b border-slate-800 p-3"}
                           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                  "Access")
                           (d/div {:class-name "mt-2 grid gap-2"}
                                  (d/div {:class-name "flex items-center justify-between gap-2 text-xs"}
                                         (d/span {:class-name "text-slate-400"} "Runtime control")
                                         (d/span {:class-name (str "rounded px-2 py-0.5 "
                                                               (if can-control?
                                                                 "bg-emerald-500/10 text-emerald-300"
                                                                 "bg-amber-500/10 text-amber-300"))}
                                                 (if can-control? "allowed" "locked")))
                                  (d/div {:class-name "flex items-center justify-between gap-2 text-xs"}
                                         (d/span {:class-name "text-slate-400"} "Tool catalog")
                                         (d/span {:class-name (str "rounded px-2 py-0.5 "
                                                               (if can-read-tools?
                                                                 "bg-emerald-500/10 text-emerald-300"
                                                                 "bg-slate-800 text-slate-500"))}
                                                 (if can-read-tools? (str (.-length tools) " tools") "hidden"))))))

(hx/defnc selected-job-card
  "Show the selected job identity, source, trigger and description."
  [{:keys [selected-job]}]
  (d/section {:class-name "border-b border-slate-800 p-3"}
                           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                  "Selected job")
                           (if selected-job
                             (d/div {:class-name "mt-2 space-y-2 text-xs"}
                                    (d/div {:class-name "font-semibold text-slate-100"}
                                           (:name selected-job))
                                    (d/code {:class-name "block break-all rounded bg-slate-900 px-2 py-1 font-mono text-[11px] text-slate-300"}
                                            (:id selected-job))
                                    (d/div {:class-name "flex flex-wrap gap-1.5 text-[11px] text-slate-400"}
                                           (d/span {:class-name "rounded bg-slate-900 px-2 py-0.5"}
                                                   (get-in selected-job [:source :kind]))
                                           (d/span {:class-name "rounded bg-slate-900 px-2 py-0.5"}
                                                   (get-in selected-job [:trigger :kind]))
                                           (when-let [contract-id (:contractSourceId selected-job)]
                                             (d/span {:class-name "rounded bg-sky-950 px-2 py-0.5 text-sky-200"}
                                                     contract-id)))
                                    (when-let [description (:description selected-job)]
                                      (d/div {:class-name "text-slate-400"}
                                             (snippet description 180))))
                             (d/div {:class-name "mt-2 text-xs text-slate-500"}
                                    "Select a runtime job in the center panel."))))

(hx/defnc events-context-panel
  "Explain runtime capabilities and the current job alongside chat."
  [{:keys [can-control? can-read-tools? tools tools-error selected-job]}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950/60"}
         (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-3"}
                (d/div {:class-name "text-sm font-semibold text-slate-100"} "Event Context")
                (d/div {:class-name "mt-1 text-xs text-slate-500"}
                       "Runtime control, schedule review, audit logs, and chat share this page context."))
         (d/div {:class-name "min-h-0 flex-1 overflow-y-auto"}
                (hx/$ access-card {:can-control? can-control? :can-read-tools? can-read-tools? :tools tools})
                (when tools-error
                  (d/div {:class-name "border-b border-amber-700/40 bg-amber-950/30 px-3 py-2 text-xs text-amber-200"}
                         (str "Tool catalog unavailable: " tools-error)))
                (hx/$ selected-job-card {:selected-job selected-job})
                (d/section {:class-name "p-3 text-xs text-slate-400"}
                           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                  "Chat context")
                           (d/p {:class-name "mt-2 leading-5"}
                                "The chat pane is pinned to this Events page and updates when a job is selected. Use it to ask about schedules, recent logs, failures, or tool behavior.")))))

(defn pinned-context
  "Project the selected runtime job into the existing chat context contract."
  [selected-job]
  {:id (if selected-job (str "event-job:" (:id selected-job)) "events:runtime")
   :title (if selected-job (str "Event job: " (:name selected-job)) "Events runtime and audit logs")
   :path (if selected-job (str "/events/jobs/" (:id selected-job)) "/events")
   :snippet (if selected-job
              (snippet (or (:description selected-job) (:id selected-job)) 240)
              "Event runtime control, schedule review, active/history audit logs, and recent agent sessions.")
   :kind "file"})
