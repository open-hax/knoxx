(ns knoxx.frontend.admin.event-agents-panel
  "Main event agent runtime control panel with state management.
   Replaces the legacy DiscordSection.tsx."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agent-utils :as u]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-sidebar :as sidebar]
            [knoxx.frontend.admin.event-agents :as agents]
            ["@open-hax/knoxx-frontend-bridge" :as bridge]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- await
  "Single-binding helper: (await [x (foo)] (bar x))"
  [[sym expr] & body]
  `(-> ~expr
       (.then (fn [~sym] ~@body))))

(defn- await-all
  "Parallel await for multiple promises."
  [bindings & body]
  (let [syms (take-nth 2 bindings)
        exprs (take-nth 2 (rest bindings))]
    `(-> (js/Promise.all (cljs.core/array ~@exprs))
         (.then (fn [res#]
                  (let [~@(interleave syms (map-indexed (fn [i _] `(aget res# ~i)) exprs))]
                    ~@body))))))

;; ---------------------------------------------------------------------------
;; Sub-components
;; ---------------------------------------------------------------------------

(defnc sidebar-controls
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
         (if (and status (.. status -runtime -running))
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
                      (if toggling-runtime "Starting…" "Start runtime")))
         (d/button {:type "button"
                    :on-click on-reset
                    :disabled (or (not can-manage) toggling-runtime resetting-runtime saving-control)
                    :class-name (str "inline-flex items-center justify-center rounded-md border border-amber-700 bg-amber-950/40 "
                                     "px-3 py-2 text-sm font-semibold text-amber-100 hover:bg-amber-900/60 disabled:opacity-60")
                    :title "Stop the runtime, clear persisted event-agent state, disable cron jobs, and leave the scheduler stopped for review."}
                    (if resetting-runtime "Resetting…" "Full reset"))))

(defnc sidebar-stats
  "Stats cards in the sidebar."
  [{:keys [status draft recent-event-count seen-discord-channels]}]
  (d/div {:class-name "grid gap-2"}
         (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Discord token")
                (d/div {:class-name "mt-1 flex items-center gap-2 text-xs text-slate-200"}
                       (if (and status (.-configured status))
                         ($ c/badge {:tone :success} "Configured")
                         ($ c/badge {:tone :warn} "Missing"))
                       (when (and status (.-tokenPreview status))
                         (d/span {:class-name "font-mono text-[11px] text-slate-400"}
                                 (.-tokenPreview status)))))
         (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Runtime")
                (d/div {:class-name "mt-1 flex items-center gap-2 text-xs text-slate-200"}
                       (if (and status (.. status -runtime -running))
                         ($ c/badge {:tone :success} "Running")
                         ($ c/badge {:tone :warn} "Stopped"))
                       (d/span (str (if draft (count (.-jobs draft)) 0) " jobs"))))
         (d/div {:class-name "grid grid-cols-2 gap-2"}
                (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                       (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Recent events")
                       (d/div {:class-name "mt-1 text-lg font-semibold text-slate-100"} recent-event-count)
                       (d/div {:class-name "text-[11px] text-slate-500"} "Buffered"))
                (d/div {:class-name "rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"}
                       (d/div {:class-name "text-[11px] uppercase tracking-wide text-slate-500"} "Freshness")
                       (d/div {:class-name "mt-1 text-lg font-semibold text-slate-100"} seen-discord-channels)
                        (d/div {:class-name "text-[11px] text-slate-500"} "Channels")))))

(defnc notice-banner
  "Success or error notice banner."
  [{:keys [notice]}]
  (when notice
    (d/div {:class-name (str "rounded-lg border px-3 py-2 text-sm "
                             (case (:tone notice)
                               :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                               "border-rose-500/30 bg-rose-500/10 text-rose-200"))}
           (:text notice))))

(defnc error-banner
  "Error banner."
  [{:keys [error]}]
  (when (seq error)
    (d/div {:class-name "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
           error)))

;; ---------------------------------------------------------------------------
;; Main panel
;; ---------------------------------------------------------------------------

(defnc event-agents-panel
  "Main panel component.
   Props:
   - can-manage: boolean
   - tools: vector of AdminToolDefinition
   - on-selected-job-change: callback fn"
  [{:keys [can-manage tools on-selected-job-change]}]
  ;; State
  (let [[loading set-loading] (hooks/use-state true)
        [saving-token set-saving-token] (hooks/use-state false)
        [saving-control set-saving-control] (hooks/use-state false)
        [running-job-id set-running-job-id] (hooks/use-state nil)
        [dispatching-event set-dispatching-event] (hooks/use-state false)
        [toggling-runtime set-toggling-runtime] (hooks/use-state false)
        [resetting-runtime set-resetting-runtime] (hooks/use-state false)
        [notice set-notice] (hooks/use-state nil)
        [error set-error] (hooks/use-state "")
        [status set-status] (hooks/use-state nil)
        [draft set-draft] (hooks/use-state nil)
        [json-drafts set-json-drafts] (hooks/use-state {})
        [selected-job-id set-selected-job-id] (hooks/use-state nil)
        [job-search set-job-search] (hooks/use-state "")
        [draft-token set-draft-token] (hooks/use-state "")
        [event-source-kind set-event-source-kind] (hooks/use-state "github")
        [event-kind set-event-kind] (hooks/use-state "issues.opened")
        [event-payload set-event-payload] (hooks/use-state "{\"repository\": \"open-hax/openplanner\", \"title\": \"Example event\", \"content\": \"Investigate this issue\"}")

        ;; Derived state
        runtime-jobs (if status (.. status -runtime -jobs) #js [])
        discord-source (if draft (.. draft -sources -discord) #js {})
        recent-event-count (if (and status (.. status -runtime -sources -recentEvents)
                                    (js/Array.isArray (.. status -runtime -sources -recentEvents)))
                             (.. status -runtime -sources -recentEvents -length)
                             0)
        discord-runtime (if status (.. status -runtime -sources -discord) nil)
        seen-discord-channels (if (and discord-runtime
                                       (js/Array.isArray (.-lastSeenChannels discord-runtime)))
                                (.. discord-runtime -lastSeenChannels -length)
                                0)

        filtered-jobs (if draft
                        (let [query (u/normalize-search job-search)]
                          (if (empty? query)
                            (.-jobs draft)
                            (->> (.-jobs draft)
                                 (filter #(str/includes? (u/job-search-text %) query)))))
                        [])

        selected-job (if (seq filtered-jobs)
                       (or (some #(when (= (.-id %) selected-job-id) %) filtered-jobs)
                           (first filtered-jobs))
                       nil)

        selected-runtime (if selected-job
                           (u/runtime-for-job runtime-jobs (.-id selected-job))
                           nil)

        selected-job-json-draft (if selected-job
                                  (or (get json-drafts (.-id selected-job))
                                      {:source-config "{}"
                                       :filters "{}"
                                       :tool-policies "[]"})
                                  nil)

        ;; Handlers
        load-data (fn []
                    (set-loading true)
                    (set-error "")
                    (set-notice nil)
                    (-> (js/Promise.all (array (bridge/getDiscordConfig)
                                               (bridge/getEventAgentControl)))
                        (.then (fn [res]
                                 (let [token-status (aget res 0)
                                       control-status (aget res 1)
                                       merged (js/Object.assign #js {} control-status
                                                                #js {:configured (.-configured token-status)
                                                                     :tokenPreview (.-tokenPreview token-status)})]
                                   (set-status merged)
                                   (set-draft (.-control merged))
                                   (set-draft-token "")
                                   (set-json-drafts (u/seed-json-drafts (.. merged -control -jobs)))
                                   (set-event-source-kind (if (.includes (.-availableSourceKinds merged) "github")
                                                            "github"
                                                            (or (first (.-availableSourceKinds merged)) "manual")))
                                   (set-loading false))))
                        (.catch (fn [err]
                                  (set-error (.-message err))
                                  (set-loading false)))))

        update-job (fn [job-id patch]
                     (when draft
                       (set-draft
                         (update draft :jobs
                                 (fn [jobs]
                                   (mapv (fn [job]
                                           (if (= (.-id job) job-id)
                                             (js/Object.assign #js {} job patch)
                                             job))
                                         jobs))))))

        update-json-draft (fn [job-id field value]
                            (set-json-drafts
                              (update json-drafts job-id
                                      (fn [d]
                                        (assoc (or d {:source-config "{}"
                                                      :filters "{}"
                                                      :tool-policies "[]"})
                                               field value)))))

        parse-control-for-save (fn []
                                 (when-not draft
                                   (throw (js/Error. "No draft control loaded")))
                                 (let [jobs (mapv (fn [job]
                                                    (let [drafts (or (get json-drafts (.-id job))
                                                                     {:source-config (u/pretty-json (or (.. job -source -config) {}))
                                                                      :filters (u/pretty-json (or (.-filters job) {}))
                                                                      :tool-policies (u/pretty-json (or (.. job -agentSpec -toolPolicies) []))})
                                                          source-config (try
                                                                          (js/JSON.parse (:source-config drafts))
                                                                          (catch js/Error err
                                                                            (throw (js/Error. (str "Invalid source config JSON for job " (.-name job) ": " (.-message err))))))
                                                          filters (try
                                                                    (js/JSON.parse (:filters drafts))
                                                                    (catch js/Error err
                                                                      (throw (js/Error. (str "Invalid filters JSON for job " (.-name job) ": " (.-message err))))))
                                                          tool-policies (try
                                                                          (js/JSON.parse (:tool-policies drafts))
                                                                          (catch js/Error err
                                                                            (throw (js/Error. (str "Invalid tool policy JSON for job " (.-name job) ": " (.-message err))))))]
                                                      (js/Object.assign #js {} job
                                                                        #js {:source (js/Object.assign #js {} (.-source job) #js {:config source-config})
                                                                             :filters filters
                                                                             :agentSpec (js/Object.assign #js {} (.-agentSpec job) #js {:toolPolicies tool-policies})})))
                                                  (.-jobs draft))]
                                   (js/Object.assign #js {} draft #js {:jobs jobs})))

        handle-save-token (fn []
                            (when can-manage
                              (let [normalized (str/trim draft-token)]
                                (if (empty? normalized)
                                  (set-error "Bot token must not be blank")
                                  (do
                                    (set-saving-token true)
                                    (set-error "")
                                    (set-notice nil)
                                    (-> (bridge/updateDiscordConfig normalized)
                                        (.then (fn [updated]
                                                 (set-status (fn [current]
                                                               (when current
                                                                 (js/Object.assign #js {} current
                                                                                   #js {:configured (.-configured updated)
                                                                                        :tokenPreview (.-tokenPreview updated)}))))
                                                 (set-draft-token "")
                                                 (set-notice {:tone :success :text (str "Discord bot token saved. Preview: " (.-tokenPreview updated))})
                                                 (load-data)))
                                        (.catch (fn [err]
                                                  (set-notice {:tone :error :text (.-message err)})))
                                        (.finally (fn []
                                                    (set-saving-token false)))))))))

        handle-save-control (fn []
                              (when (and can-manage draft)
                                (set-saving-control true)
                                (set-error "")
                                (set-notice nil)
                                (try
                                  (let [next (parse-control-for-save)]
                                    (-> (bridge/updateEventAgentControl next)
                                        (.then (fn [updated]
                                                 (set-status updated)
                                                 (set-draft (.-control updated))
                                                 (set-json-drafts (u/seed-json-drafts (.. updated -control -jobs)))
                                                 (set-notice {:tone :success :text "Event-agent control plane updated and runtime reloaded."})))
                                        (.catch (fn [err]
                                                  (set-notice {:tone :error :text (.-message err)})))
                                        (.finally (fn []
                                                    (set-saving-control false)))))
                                  (catch js/Error err
                                    (set-notice {:tone :error :text (.-message err)})
                                    (set-saving-control false)))))

        handle-run-job (fn [job-id]
                         (when can-manage
                           (set-running-job-id job-id)
                           (set-error "")
                           (set-notice nil)
                           (-> (bridge/runEventAgentJob job-id)
                               (.then (fn [_]
                                        (set-notice {:tone :success :text (str "Queued job " job-id ".")})
                                        (load-data)))
                               (.catch (fn [err]
                                         (set-notice {:tone :error :text (.-message err)})))
                               (.finally (fn []
                                           (set-running-job-id nil))))))

        handle-dispatch-event (fn []
                                (when can-manage
                                  (set-dispatching-event true)
                                  (set-error "")
                                  (set-notice nil)
                                  (try
                                    (let [payload (js/JSON.parse (or event-payload "{}"))]
                                      (-> (bridge/dispatchEventAgentEvent
                                            #js {:sourceKind event-source-kind
                                                 :eventKind event-kind
                                                 :payload payload})
                                          (.then (fn [result]
                                                   (set-notice {:tone :success
                                                                :text (str "Dispatched " event-source-kind ":" event-kind
                                                                           ". Matched jobs: "
                                                                           (or (str/join ", " (.-matchedJobs result))
                                                                               "none") ".")})
                                                   (load-data)))
                                          (.catch (fn [err]
                                                    (set-notice {:tone :error :text (.-message err)})))
                                          (.finally (fn []
                                                      (set-dispatching-event false)))))
                                    (catch js/Error err
                                      (set-notice {:tone :error :text (.-message err)})
                                      (set-dispatching-event false)))))

        handle-stop-runtime (fn []
                              (when can-manage
                                (set-toggling-runtime true)
                                (set-error "")
                                (set-notice nil)
                                (-> (bridge/stopEventAgentRuntime)
                                    (.then (fn [updated]
                                             (set-status updated)
                                             (set-draft (.-control updated))
                                             (set-json-drafts (u/seed-json-drafts (.. updated -control -jobs)))
                                             (set-notice {:tone :success :text "Event-agent runtime stopped (schedulers cleared)."})))
                                    (.catch (fn [err]
                                              (set-notice {:tone :error :text (.-message err)})))
                                    (.finally (fn []
                                                (set-toggling-runtime false))))))

        handle-start-runtime (fn []
                               (when can-manage
                                 (set-toggling-runtime true)
                                 (set-error "")
                                 (set-notice nil)
                                 (-> (bridge/startEventAgentRuntime)
                                     (.then (fn [updated]
                                              (set-status updated)
                                              (set-draft (.-control updated))
                                              (set-json-drafts (u/seed-json-drafts (.. updated -control -jobs)))
                                              (set-notice {:tone :success :text "Event-agent runtime started."})
                                              (load-data)))
                                     (.catch (fn [err]
                                               (set-notice {:tone :error :text (.-message err)})))
                                     (.finally (fn []
                                                 (set-toggling-runtime false))))))

        handle-reset-runtime (fn []
                               (when can-manage
                                 (set-resetting-runtime true)
                                 (set-error "")
                                 (set-notice nil)
                                 (-> (bridge/resetEventAgentRuntime)
                                     (.then (fn [updated]
                                              (set-status updated)
                                              (set-draft (.-control updated))
                                              (set-json-drafts (u/seed-json-drafts (.. updated -control -jobs)))
                                              (set-notice {:tone :success
                                                           :text (str "Event-agent runtime reset. Cleared "
                                                                      (.. updated -reset -deletedCount)
                                                                      " persisted state key(s) and disabled "
                                                                      (.. updated -reset -disabledCronJobCount)
                                                                      " cron job(s). Review schedules before restarting.")})))
                                     (.catch (fn [err]
                                               (set-notice {:tone :error :text (.-message err)})))
                                     (.finally (fn []
                                                 (set-resetting-runtime false))))))]

    ;; Effects
    (hooks/use-effect
      :once
      (load-data))

    (hooks/use-effect
      [filtered-jobs]
      (when (seq filtered-jobs)
        (when-not (some #(= (.-id %) selected-job-id) filtered-jobs)
          (set-selected-job-id (.-id (first filtered-jobs))))))

    (hooks/use-effect
      [selected-job on-selected-job-change]
      (when on-selected-job-change
        (on-selected-job-change selected-job))

    ;; Render
    (if (or loading (not draft) (not status))
      ($ agents/loading-state)
      (d/div {:class-name "flex flex-col h-full min-h-0"}
             (d/div {:class-name "min-h-0 flex-1 overflow-hidden"}
                    (d/div {:class-name "grid h-full min-h-0 min-w-[44rem] gap-3 grid-cols-[12rem_minmax(0,1fr)] xl:grid-cols-[13rem_minmax(0,1fr)]"}
                           ;; Sidebar
                           (d/aside {:class-name "flex flex-col overflow-hidden h-full space-y-2 rounded-xl border border-slate-800 bg-slate-950/50 p-2.5"}
                                    (d/div {:class-name "flex items-center justify-between gap-2"}
                                           (d/div {:class-name "text-sm font-semibold text-slate-100"} "Agents")
                                           (d/div {:class-name "text-[11px] text-slate-500"}
                                                  (str (count filtered-jobs) "/" (count (.-jobs draft)))))
                                    ($ sidebar-controls {:on-refresh load-data
                                                         :on-save handle-save-control
                                                         :on-stop handle-stop-runtime
                                                         :on-start handle-start-runtime
                                                         :on-reset handle-reset-runtime
                                                         :can-manage can-manage
                                                         :loading loading
                                                         :saving-control saving-control
                                                         :toggling-runtime toggling-runtime
                                                         :resetting-runtime resetting-runtime
                                                         :status status
                                                         :draft draft})
                                    ($ sidebar-stats {:status status
                                                      :draft draft
                                                      :recent-event-count recent-event-count
                                                      :seen-discord-channels seen-discord-channels})
                                    (d/label {:class-name "space-y-1 block"}
                                             (d/div {:class-name "sr-only"} "Search")
                                             (d/input {:aria-label "Search"
                                                       :value job-search
                                                       :on-change #(set-job-search (.. % -target -value))
                                                       :placeholder "Search…"
                                                       :class-name (str "w-full rounded-md border border-slate-800 bg-slate-950/80 "
                                                                        "px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500")}))
                                    (d/div {:class-name "flex-1 min-h-0 space-y-1.5 overflow-y-auto pr-1"}
                                           (if (seq filtered-jobs)
                                             (for [job filtered-jobs]
                                               ($ sidebar/job-button {:key (.-id job)
                                                                      :job job
                                                                      :runtime (u/runtime-for-job runtime-jobs (.-id job))
                                                                      :active (= (.-id job) selected-job-id)
                                                                      :on-select #(set-selected-job-id (.-id job))}))
                                             (d/div {:class-name "rounded-xl border border-dashed border-slate-800 px-3 py-6 text-center text-sm text-slate-500"}
                                                    "No event agents match this search."))))
                           ;; Main content
                           ($ agents/main-content {:jobs filtered-jobs
                                                   :runtime-jobs runtime-jobs
                                                   :selected-job-id selected-job-id
                                                   :on-select-job set-selected-job-id
                                                   :selected-job selected-job
                                                   :runtime selected-runtime
                                                   :on-update update-job
                                                   :on-run handle-run-job
                                                   :can-manage can-manage
                                                   :saving-control saving-control
                                                   :running-job-id running-job-id})))
             ($ error-banner {:error error})
             ($ notice-banner {:notice notice}))))))
