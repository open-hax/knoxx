(ns knoxx.frontend.pages.events
  "Shadow-owned Events page.

   During migration, we keep this page in CLJS so the /events route exists in
   the shadow router. The heavy UI surface is the CLJS EventAgentsPanel (runtime
   jobs control).

   This replaces the legacy TS EventsPage+DiscordSection path progressively."
  (:require ["@open-hax/knoxx-app-bridge" :as app]
            ["@open-hax/knoxx-frontend-bridge" :as bridge]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.admin.event-agents-panel :as event-panel]
            [knoxx.frontend.auth.context :as auth-ctx]
            [knoxx.frontend.components.layout.workbench :as layout]
            [knoxx.frontend.pages.events.context :as context]))

(defn- has-permission? [^js auth permission]
  (.includes (or (.-permissions auth) #js []) permission))

(def events-context-panel
  "Compatibility component for the native event context view."
  context/events-context-panel)

(hx/defnc events-main-header
  "Render the Events workspace title and available runtime operations."
  []
  (d/header {:class-name "flex shrink-0 items-center justify-between gap-3 border-b border-slate-800 bg-slate-950 px-4 py-2"}
            (d/div {:class-name "min-w-0"}
                   (d/h1 {:class-name "truncate text-lg font-semibold text-slate-100"}
                         "Events")
                   (d/div {:class-name "truncate text-xs text-slate-500"}
                          "Runtime control, schedules, dispatch, reset, and audit logs."))))

(hx/defnc events-audit-panel
  "Host the existing audit-log bridge in the native workspace."
  []
  (d/div {:class-name "h-full min-h-0 overflow-hidden bg-slate-950 p-2"}
         (hx/$ app/AgentAuditLogs {:default-mode "active"
                                :class-name "h-full min-h-0"})))

(defn- ^:async load-event-tools! [set-tools set-tools-error]
  (try
    (let [^js response (await (bridge/listAdminTools))]
      (set-tools (or (.-tools response) #js []))
      (set-tools-error nil))
    (catch :default error
      (set-tools #js [])
      (set-tools-error (or (.-message error) (str error))))))

(defn- use-event-tools [can-control? can-read-tools?]
  (let [[tools set-tools] (hooks/use-state #js [])
        [tools-error set-tools-error] (hooks/use-state nil)]
    (hooks/use-effect [can-control? can-read-tools?]
      (if (and can-control? can-read-tools?)
        (load-event-tools! set-tools set-tools-error)
        (do (set-tools #js []) (set-tools-error nil)))
      js/undefined)
    [tools tools-error]))

(hx/defnc events-center-panel
  "Compose permission-gated runtime control with the audit panel."
  [{:keys [can-control? tools set-selected-job]}]
  (hx/$ layout/WorkbenchMain
     {:bottom-panel (hx/$ layout/WorkbenchBottomPanel
                       {:label "Agent Logs"
                        :storage-key "knoxx_events_audit_logs"
                        :default-height 360
                        :min-height 180
                        :max-height 720
                        :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                                        "Agent Logs")}
                       (hx/$ events-audit-panel))}
     (d/div {:class-name "flex min-h-0 flex-1 flex-col overflow-hidden"}
            (hx/$ events-main-header)
            (if-not can-control?
              (d/div {:class-name "m-4 shrink-0 rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400"}
                     "Event runtime control access required.")
              (d/div {:class-name "min-h-0 flex-1 overflow-hidden p-2"}
                     (hx/$ event-panel/event-agents-panel {:can-manage can-control?
                                                        :tools tools
                                                        :on-selected-job-change set-selected-job}))))))

(hx/defnc events-chat-panel
  "Host the existing pinned-context chat workspace."
  [{:keys [chat]}]
  (hx/$ layout/WorkbenchPanel
     {:edge "right"
      :label "Events Chat"
      :storage-key "knoxx_events_chat"
      :default-width 420
      :min-width 360
      :max-width 640
      :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                      "Events Chat")}
     (hx/$ app/ChatWorkspacePane
        {:controller chat
         :showFiles false
         :showFilesToggle false
         :showCanvasToggle false
         :onShowFiles #()})))

(hx/defnc events-workbench
  "Arrange the context, runtime, logs and chat panels."
  [{:keys [can-control? can-read-tools? tools tools-error selected-job set-selected-job chat]}]
  (hx/$ layout/WorkbenchShell
     (hx/$ layout/WorkbenchPanel
        {:edge "left"
         :label "Event Context"
         :storage-key "knoxx_events_context"
         :default-width 300
         :min-width 240
         :max-width 480
         :header (d/span {:class-name "text-xs font-semibold text-slate-100"}
                         "Event Context")}
        (hx/$ events-context-panel {:can-control? can-control?
                                 :can-read-tools? can-read-tools?
                                 :tools tools
                                 :tools-error tools-error
                                 :selected-job selected-job}))
     (hx/$ events-center-panel {:can-control? can-control?
                             :tools tools
                             :set-selected-job set-selected-job})
     (hx/$ events-chat-panel {:chat chat})))

(defn- pin-selected-job! [^js chat selected-job]
  (try
    (when-let [pin (.-pinContextItem chat)]
      (pin (clj->js (context/pinned-context selected-job))))
    (catch js/Error _error nil)))

(defn- use-event-chat [selected-job]
  (let [chat (app/useChatWorkspaceController
              #js {:initialShowCanvas false :defaultRole "event_runtime_librarian"
                   :defaultActorId "event_runtime_librarian" :sessionIdKey "knoxx_events_session_id"
                   :scratchpadStorageKey "knoxx_events_scratchpad_state"
                   :pinnedContextStorageKey "knoxx_events_pinned_context"
                   :sessionStateKey "knoxx_events_chat_session_state"
                   :sidebarWidthKey "knoxx_events_sidebar_width_px"})]
    (hooks/use-effect [selected-job] (pin-selected-job! chat selected-job) nil)
    chat))

(hx/defnc EventsPage
  "Bind the authenticated event capabilities, selected job and chat workspace."
  []
  (let [^js auth (auth-ctx/use-auth)
        [selected-job set-selected-job] (hooks/use-state nil)
        chat (use-event-chat selected-job)
        can-control? (boolean (or (.-isSystemAdmin auth) (has-permission? auth "org.event_agents.control")))
        can-read-tools? (boolean (or (.-isSystemAdmin auth)
                                     (has-permission? auth "org.tool_policy.read")
                                     (has-permission? auth "platform.roles.manage")
                                     (has-permission? auth "org.user_policy.read")))
        [tools tools-error] (use-event-tools can-control? can-read-tools?)]
    (d/div {:data-page "events" :class-name "h-full min-h-0 overflow-hidden bg-slate-950 text-slate-100"}
      (hx/$ events-workbench {:can-control? can-control? :can-read-tools? can-read-tools?
                              :tools tools :tools-error tools-error :selected-job selected-job
                              :set-selected-job set-selected-job :chat chat}))))
