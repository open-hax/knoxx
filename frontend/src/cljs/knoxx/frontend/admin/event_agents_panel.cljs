(ns knoxx.frontend.admin.event-agents-panel
  "Editable runtime control with native HTTP commands and preserved draft state."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.admin.event-agent-commands :as commands]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-sidebar :as sidebar]
            [knoxx.frontend.admin.event-agent-utils :as u]
            [knoxx.frontend.admin.event-agents :as agents]))

(def load-data "Compatibility entry point for the runtime command." commands/load-data)
(def update-job "Compatibility entry point for the runtime command." commands/update-job)
(def handle-save-token "Compatibility entry point for the runtime command." commands/handle-save-token)
(def parse-control-for-save "Compatibility entry point for the runtime command." commands/parse-control-for-save)
(def update-json-draft "Compatibility entry point for the runtime command." commands/update-json-draft)
(def handle-save-control "Compatibility entry point for the runtime command." commands/handle-save-control)
(def handle-run-job "Compatibility entry point for the runtime command." commands/handle-run-job)
(def handle-dispatch-event "Compatibility entry point for the runtime command." commands/handle-dispatch-event)
(def handle-stop-runtime "Compatibility entry point for the runtime command." commands/handle-stop-runtime)
(def handle-start-runtime "Compatibility entry point for the runtime command." commands/handle-start-runtime)
(def handle-reset-runtime "Compatibility entry point for the runtime command." commands/handle-reset-runtime)

(defn create-sidebar-controller
  "Bind runtime controls to authorized awaited commands."
  [{:keys [reload-data can-manage loading status draft json-drafts saving-control toggling-runtime resetting-runtime
           set-error set-notice set-status set-draft set-json-drafts set-saving-control set-toggling-runtime set-resetting-runtime]}]
  {:on-refresh reload-data
   :on-save #(handle-save-control set-error set-notice set-status set-draft set-json-drafts set-saving-control
                                  can-manage draft json-drafts)
   :on-stop #(handle-stop-runtime set-status set-draft set-json-drafts set-toggling-runtime set-error set-notice can-manage)
   :on-start #(handle-start-runtime reload-data set-status set-draft set-json-drafts set-toggling-runtime set-error set-notice can-manage)
   :on-reset #(handle-reset-runtime set-status set-draft set-json-drafts set-resetting-runtime set-error set-notice can-manage)
   :can-manage can-manage
   :loading loading
   :saving-control saving-control
   :toggling-runtime toggling-runtime
   :resetting-runtime resetting-runtime
   :status status
   :draft draft})
;; ---------------------------------------------------------------------------
;; Sub-components
;; ---------------------------------------------------------------------------

(hx/defnc runtime-toggle
  "Start or stop scheduling through the authorized command."
  [{:keys [status on-stop on-start can-manage toggling-runtime resetting-runtime]}]
(if (and status (get-in status [:runtime :running]))
           (d/button {:type "button"
                      :on-click on-stop
                      :disabled (or (not can-manage) toggling-runtime resetting-runtime)
                      :class-name (str "inline-flex items-center justify-center rounded-md bg-rose-700 "
                                       "px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-rose-600 disabled:opacity-60")
                      :title "Stops cron scheduling + unsubscribes Discord gateway. Does not hard-cancel an in-flight LLM request."}
                     (if toggling-runtime "Stopping…" "Stop runtime"))
           (d/button {:type "button"
                      :on-click on-start
                      :disabled (or (not can-manage) toggling-runtime resetting-runtime)
                      :class-name (str "inline-flex items-center justify-center rounded-md bg-emerald-700 "
                                       "px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-emerald-600 disabled:opacity-60")}
                     (if toggling-runtime "Starting…" "Start runtime"))) )

(hx/defnc sidebar-controls
  "Control buttons above the job list."
  [{:keys [on-refresh on-save on-stop on-start on-reset
           can-manage loading saving-control toggling-runtime resetting-runtime
           status draft]}]
  (d/div {:class-name "grid gap-2"}
         (d/button {:type "button"
                    :on-click on-refresh
                    :disabled (or loading saving-control toggling-runtime resetting-runtime)
                    :class-name (str "inline-flex items-center justify-center rounded-md border border-slate-700 bg-slate-900 "
                                     "px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60")}
                   (if loading "Loading…" "Refresh"))
         (d/button {:type "button"
                    :on-click on-save
                    :disabled (or (not can-manage) (not draft) saving-control toggling-runtime resetting-runtime)
                    :class-name (str "inline-flex items-center justify-center rounded-md bg-sky-600 "
                                     "px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60")}
                   (if saving-control "Saving…" "Save runtime"))
         (hx/$ runtime-toggle {:status status :on-stop on-stop :on-start on-start :can-manage can-manage
                                :toggling-runtime toggling-runtime :resetting-runtime resetting-runtime})
         (d/button {:type "button"
                    :on-click on-reset
                    :disabled (or (not can-manage) toggling-runtime resetting-runtime saving-control)
                    :class-name (str "inline-flex items-center justify-center rounded-md border border-amber-700 bg-amber-950/40 "
                                     "px-3 py-2 text-sm font-semibold text-amber-100 hover:bg-amber-900/60 disabled:opacity-60")
                    :title "Stop the runtime, clear persisted event-agent state, disable cron jobs, and leave the scheduler stopped for review."}
                   (if resetting-runtime "Resetting…" "Full reset"))))

(hx/defnc sidebar-stats
  "Stats cards in the sidebar."
  [{:keys [status draft recent-event-count seen-discord-channels]}]
  (d/div {:class-name "grid gap-2"}
         (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Discord token")
                (d/div {:class-name "mt-1 flex items-center gap-2 text-xs text-slate-200"}
                       (if (and status (:configured status))
                         (hx/$ c/badge {:tone :success} "Configured")
                         (hx/$ c/badge {:tone :warn} "Missing"))
                       (when (and status (:tokenPreview status))
                         (d/span {:class-name "font-mono text-[11px] text-slate-400"}
                                 (:tokenPreview status)))))
         (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Runtime")
                (d/div {:class-name "mt-1 flex items-center gap-2 text-xs text-slate-200"}
                       (if (and status (get-in status [:runtime :running]))
                         (hx/$ c/badge {:tone :success} "Running")
                         (hx/$ c/badge {:tone :warn} "Stopped"))
                       (d/span (str (if draft (count (:jobs draft)) 0) " jobs"))))
         (d/div {:class-name "grid grid-cols-2 gap-2"}
                (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                       (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Recent events")
                       (d/div {:class-name "mt-1 text-lg font-semibold text-slate-100"} recent-event-count)
                       (d/div {:class-name "text-[11px] text-slate-500"} "Buffered"))
                (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                       (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Freshness")
                       (d/div {:class-name "mt-1 text-lg font-semibold text-slate-100"} seen-discord-channels)
                       (d/div {:class-name "text-[11px] text-slate-500"} "Channels")))))

(hx/defnc notice-banner
  "Success or error notice banner."
  [{:keys [notice]}]
  (when notice
    (d/div {:class-name (str "rounded-lg border px-3 py-2 text-sm "
                             (case (:tone notice)
                               :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                               "border-rose-500/30 bg-rose-500/10 text-rose-200"))}
           (:text notice))))

(hx/defnc error-banner
  "Error banner."
  [{:keys [error]}]
  (when (seq error)
    (d/div {:class-name "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
           error)))

;; ---------------------------------------------------------------------------
;; Main panel
;; ---------------------------------------------------------------------------

(def ^:private initial-panel
  {:loading true :saving-control false :running-job-id nil :toggling-runtime false
   :resetting-runtime false :notice nil :error "" :status nil :draft nil :json-drafts {}
   :selected-job-id nil :job-search "" :draft-token "" :event-source-kind "github"})

(defn- use-panel-state []
  (let [[state set-state] (hooks/use-state initial-panel)
        setters (hooks/use-memo [set-state]
                  (into {} (for [field (keys initial-panel)]
                             [(keyword (str "set-" (name field)))
                              (fn [value] (set-state #(assoc % field (if (fn? value) (value (get % field)) value))))])))]
    (merge state setters)))

(defn- derive-panel [{:keys [draft status job-search selected-job-id]}]
  (let [query (u/normalize-search job-search)
        jobs (filterv #(or (empty? query) (str/includes? (u/job-search-text %) query)) (:jobs draft))
        selected (or (some #(when (= (:id %) selected-job-id) %) jobs) (first jobs))
        runtime-jobs (get-in status [:runtime :jobs] [])]
    {:filtered-jobs jobs :selected-job selected :runtime-jobs runtime-jobs
     :selected-runtime (when selected (u/runtime-for-job runtime-jobs (:id selected)))
     :recent-event-count (count (get-in status [:runtime :sources :recentEvents]))
     :seen-discord-channels (count (get-in status [:runtime :sources :discord :lastSeenChannels]))}))

(defn- bind-panel [{:keys [set-loading set-error set-notice set-status set-draft set-draft-token
                          set-json-drafts set-event-source-kind can-manage set-running-job-id draft] :as panel}]
  (let [reload #(load-data set-loading set-error set-notice set-status set-draft set-draft-token set-json-drafts set-event-source-kind)]
    {:reload-data reload :sidebar-controller (create-sidebar-controller (assoc panel :reload-data reload))
     :update-job #(update-job draft set-draft %1 %2)
     :run-job #(handle-run-job reload set-error set-notice can-manage set-running-job-id %)}))

(defn- use-panel-controller [can-manage on-selected-job-change]
  (let [state (use-panel-state) derived (derive-panel state)
        panel (merge state derived {:can-manage can-manage})
        {:keys [reload-data] :as bound} (bind-panel panel)
        {:keys [filtered-jobs selected-job]} derived
        {:keys [selected-job-id set-selected-job-id]} state]
    (hooks/use-effect :once (reload-data))
    (hooks/use-effect [filtered-jobs selected-job-id]
      (when (and (seq filtered-jobs) (not-any? #(= (:id %) selected-job-id) filtered-jobs))
        (set-selected-job-id (:id (first filtered-jobs)))))
    (hooks/use-effect [selected-job on-selected-job-change]
      (when on-selected-job-change (on-selected-job-change selected-job)))
    (merge panel bound)))

(hx/defnc panel-job-list
  "Select a filtered runtime job without mutating its draft."
  [{:keys [panel]}]
  (let [{:keys [filtered-jobs runtime-jobs selected-job-id set-selected-job-id]} panel]
    (d/div {:class-name "flex-1 min-h-0 space-y-1.5 overflow-y-auto pr-1"}
      (if (seq filtered-jobs)
        (for [job filtered-jobs]
          (hx/$ sidebar/job-button {:key (:id job) :job job
                                   :runtime (u/runtime-for-job runtime-jobs (:id job))
                                   :active (= (:id job) selected-job-id) :on-select #(set-selected-job-id (:id job))}))
        (d/div {:class-name "rounded-xl border border-dashed border-slate-800 px-3 py-6 text-center text-sm text-slate-500"}
          "No event agents match this search.")))))

(hx/defnc panel-search
  "Filter the event-agent list."
  [{:keys [value on-change]}]
  (d/label {:class-name "shrink-0 space-y-1 block"}
    (d/div {:class-name "sr-only"} "Search")
    (d/input {:aria-label "Search" :value value :on-change #(on-change (.. % -target -value))
              :placeholder "Search…"
              :class-name "w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"})))

(hx/defnc panel-sidebar
  "Runtime commands, status and current job selection."
  [{:keys [panel]}]
  (let [{:keys [filtered-jobs draft sidebar-controller job-search set-job-search]} panel]
    (d/aside {:class-name "flex flex-col overflow-hidden h-full gap-2 rounded-xl border border-slate-800 bg-slate-950/50 p-2.5"}
      (d/div {:class-name "shrink-0 flex items-center justify-between gap-2"}
        (d/div {:class-name "text-sm font-semibold text-slate-100"} "Agents")
        (d/div {:class-name "text-[11px] text-slate-500"} (str (count filtered-jobs) "/" (count (:jobs draft)))))
      (d/div {:class-name "shrink-0"} (hx/$ sidebar-controls {& sidebar-controller}))
      (d/div {:class-name "shrink-0"} (hx/$ sidebar-stats {& panel}))
      (hx/$ panel-search {:value job-search :on-change set-job-search})
      (hx/$ panel-job-list {:panel panel}))))

(hx/defnc panel-content
  "Compose the selected agent editor with its runtime capabilities."
  [{:keys [panel]}]
  (let [{:keys [filtered-jobs runtime-jobs selected-job-id set-selected-job-id selected-job selected-runtime
               run-job can-manage saving-control running-job-id error notice] update-current-job :update-job} panel]
    (d/div {:class-name "flex flex-col h-full min-h-0"}
      (d/div {:class-name "min-h-0 flex-1 overflow-hidden"}
        (d/div {:class-name "grid h-full min-h-0 min-w-[44rem] gap-3 grid-cols-[12rem_minmax(0,1fr)] xl:grid-cols-[13rem_minmax(0,1fr)]"}
          (hx/$ panel-sidebar {:panel panel})
          (hx/$ agents/main-content {:jobs filtered-jobs :runtime-jobs runtime-jobs :selected-job-id selected-job-id
                                    :on-select-job set-selected-job-id :selected-job selected-job :runtime selected-runtime
                                    :on-update update-current-job :on-run #(run-job (:id selected-job)) :can-manage can-manage
                                    :saving-control saving-control :running-job-id running-job-id})))
      (hx/$ error-banner {:error error}) (hx/$ notice-banner {:notice notice}))))

(hx/defnc event-agents-panel
  "Read and edit the runtime through the same authorized commands used by agents."
  [{:keys [can-manage on-selected-job-change]}]
  (let [{:keys [loading draft status] :as panel} (use-panel-controller can-manage on-selected-job-change)]
    (if (or loading (not draft) (not status)) (hx/$ agents/loading-state) (hx/$ panel-content {:panel panel}))))
