(ns knoxx.frontend.pages.events
  "Shadow-owned Events page.

   During migration, we keep this page in CLJS so the /events route exists in
   the shadow router. The heavy UI surface is the CLJS EventAgentsPanel (runtime
   jobs control).

   This replaces the legacy TS EventsPage+DiscordSection path progressively."
   (:require [clojure.string :as str]
             [helix.core :as hx :refer [$ defnc]]
             [helix.hooks :as hooks]
             [helix.dom :as d]
             [knoxx.frontend.admin.event-agents-panel :as event-panel]
             [knoxx.frontend.components.layout.workbench :as layout]
             ["@open-hax/knoxx-frontend-bridge" :as bridge]
             ["@open-hax/knoxx-app-bridge" :as app]
             ["@open-hax/knoxx-app-bridge" :refer [useChatWorkspaceController ChatWorkspacePane]]))

(defn- has-permission? [auth permission]
  (.includes (or (.-permissions auth) #js []) permission))

(defn- snippet [value max-length]
  (let [text (str/trim (str (or value "")))]
    (if (> (count text) max-length)
      (str (subs text 0 max-length) "...")
      text)))

(defnc events-context-panel
  [{:keys [can-control? can-read-tools? tools tools-error selected-job]}]
  (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950/60"}
         (d/div {:class-name "shrink-0 border-b border-slate-800 px-3 py-3"}
                (d/div {:class-name "text-sm font-semibold text-slate-100"} "Event Context")
                (d/div {:class-name "mt-1 text-xs text-slate-500"}
                       "Runtime control, schedule review, audit logs, and chat share this page context."))
         (d/div {:class-name "min-h-0 flex-1 overflow-y-auto"}
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
                                                 (if can-read-tools? (str (.-length tools) " tools") "hidden")))))
                (when tools-error
                  (d/div {:class-name "border-b border-amber-700/40 bg-amber-950/30 px-3 py-2 text-xs text-amber-200"}
                         (str "Tool catalog unavailable: " tools-error)))
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
                                    "Select a runtime job in the center panel.")))
                (d/section {:class-name "p-3 text-xs text-slate-400"}
                           (d/div {:class-name "text-[10px] font-semibold uppercase tracking-wide text-slate-500"}
                                  "Chat context")
                           (d/p {:class-name "mt-2 leading-5"}
                                "The chat pane is pinned to this Events page and updates when a job is selected. Use it to ask about schedules, recent logs, failures, or tool behavior.")))))

(defnc events-main-header
  []
  (d/header {:class-name "flex shrink-0 items-center justify-between gap-3 border-b border-slate-800 bg-slate-950 px-4 py-2"}
            (d/div {:class-name "min-w-0"}
                   (d/h1 {:class-name "truncate text-lg font-semibold text-slate-100"}
                         "Events")
                   (d/div {:class-name "truncate text-xs text-slate-500"}
                          "Runtime control, schedules, dispatch, reset, and audit logs."))))

(defnc events-audit-panel
  []
  (d/div {:class-name "h-full min-h-0 overflow-hidden bg-slate-950 p-2"}
         ($ app/AgentAuditLogs {:default-mode "active"
                                :class-name "h-full min-h-0"})))

(defn- use-event-tools [can-control? can-read-tools?]
  (let [[tools set-tools] (hooks/use-state #js [])
        [tools-error set-tools-error] (hooks/use-state nil)]
    (hooks/use-effect
     [can-control? can-read-tools?]
     (if (and can-control? can-read-tools?)
       (-> (bridge/listAdminTools)
           (.then (fn [res]
                    (set-tools (or (.-tools res) #js []))
                    (set-tools-error nil)))
           (.catch (fn [err]
                     (set-tools #js [])
                     (set-tools-error (or (.-message err) (str err))))))
       (do
         (set-tools #js [])
         (set-tools-error nil)))
     js/undefined)
    [tools tools-error]))

(defnc events-center-panel
  [{:keys [can-control? tools set-selected-job]}]
  ($ layout/WorkbenchMain
     {:bottom-panel ($ layout/WorkbenchBottomPanel
                       {:label "Agent Logs"
                        :storage-key "knoxx_events_audit_logs"
                        :default-height 360
                        :min-height 180
                        :max-height 720
                        :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                                        "Agent Logs")}
                       ($ events-audit-panel))}
     (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
            ($ events-main-header)
            (if-not can-control?
              (d/div {:class-name "m-4 shrink-0 rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400"}
                     "Event runtime control access required.")
              (d/div {:class-name "min-h-0 flex-1 overflow-hidden p-2"}
                     ($ event-panel/event-agents-panel {:can-manage can-control?
                                                        :tools tools
                                                        :on-selected-job-change set-selected-job}))))))

(defnc events-chat-panel
  [{:keys [chat]}]
  ($ layout/WorkbenchPanel
     {:edge "right"
      :label "Events Chat"
      :storage-key "knoxx_events_chat"
      :default-width 420
      :min-width 360
      :max-width 640
      :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                      "Events Chat")}
     ($ ChatWorkspacePane
        {:controller chat
         :showFiles false
         :showFilesToggle false
         :showCanvasToggle false
         :onShowFiles #()})))

(defnc events-workbench
  [{:keys [can-control? can-read-tools? tools tools-error selected-job set-selected-job chat]}]
  ($ layout/WorkbenchShell
     ($ layout/WorkbenchPanel
        {:edge "left"
         :label "Event Context"
         :storage-key "knoxx_events_context"
         :default-width 300
         :min-width 240
         :max-width 480
         :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                         "Event Context")}
        ($ events-context-panel {:can-control? can-control?
                                 :can-read-tools? can-read-tools?
                                 :tools tools
                                 :tools-error tools-error
                                 :selected-job selected-job}))
     ($ events-center-panel {:can-control? can-control?
                             :tools tools
                             :set-selected-job set-selected-job})
     ($ events-chat-panel {:chat chat})))

(defnc EventsPage []
  (let [auth (app/useAuth)
         [selected-job set-selected-job] (hooks/use-state nil)
         chat (useChatWorkspaceController
               #js {:initialShowCanvas false
                    :defaultRole "event_runtime_librarian"
                    :defaultActorId "event_runtime_librarian"
                    :sessionIdKey "knoxx_events_session_id"
                    :scratchpadStorageKey "knoxx_events_scratchpad_state"
                    :pinnedContextStorageKey "knoxx_events_pinned_context"
                    :sessionStateKey "knoxx_events_chat_session_state"
                    :sidebarWidthKey "knoxx_events_sidebar_width_px"})
         can-control? (boolean (or (.-isSystemAdmin auth)
                                   (has-permission? auth "org.event_agents.control")))
         can-read-tools? (boolean (or (.-isSystemAdmin auth)
                                      (has-permission? auth "org.tool_policy.read")
                                      (has-permission? auth "platform.roles.manage")
                                      (has-permission? auth "org.user_policy.read")))
         [tools tools-error] (use-event-tools can-control? can-read-tools?)]

    (hooks/use-effect
     [selected-job]
     (try
       (let [pin (.-pinContextItem chat)]
         (when pin
           (pin #js {:id (if selected-job
                           (str "event-job:" (:id selected-job))
                           "events:runtime")
                     :title (if selected-job
                              (str "Event job: " (:name selected-job))
                              "Events runtime and audit logs")
                     :path (if selected-job
                             (str "/events/jobs/" (:id selected-job))
                             "/events")
                     :snippet (if selected-job
                                (snippet (or (:description selected-job) (:id selected-job)) 240)
                                "Event runtime control, schedule review, active/history audit logs, and recent agent sessions.")
                     :kind "file"})))
       (catch js/Error _ nil))
     nil)

    (d/div {:data-page "events"
            :class-name "h-full min-h-0 overflow-hidden bg-slate-950 text-slate-100"}
           ($ events-workbench {:can-control? can-control?
                                :can-read-tools? can-read-tools?
                                :tools tools
                                :tools-error tools-error
                                :selected-job selected-job
                                 :set-selected-job set-selected-job
                                 :chat chat}))))
