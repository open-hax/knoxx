(ns knoxx.frontend.pages.events
  "Shadow-owned Events page.

   During migration, we keep this page in CLJS so the /events route exists in
   the shadow router. The heavy UI surface is the CLJS EventAgentsPanel (runtime
   jobs control).

   This replaces the legacy TS EventsPage+DiscordSection path progressively."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agents-panel :as event-panel]
            ["@open-hax/knoxx-frontend-bridge" :as bridge]
            ["@open-hax/knoxx-app-bridge" :as app]))

(defnc EventsPage []
  (let [auth (app/useAuth)
        [tools set-tools] (hooks/use-state #js [])
        [tools-error set-tools-error] (hooks/use-state nil)
        can-control? (boolean (or (.-isSystemAdmin auth)
                                 (.includes (or (.-permissions auth) #js []) "org.event_agents.control")))
        can-read-tools? (boolean (or (.-isSystemAdmin auth)
                                    (.includes (or (.-permissions auth) #js []) "org.tool_policy.read")
                                    (.includes (or (.-permissions auth) #js []) "platform.roles.manage")
                                    (.includes (or (.-permissions auth) #js []) "org.user_policy.read")))]

    (hooks/use-effect
      [can-control? can-read-tools?]
      (if (and can-control? can-read-tools?)
        (do
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

    (d/div {:data-page "events"
            :class-name "flex flex-col h-full min-h-0 overflow-hidden bg-slate-950 p-4 text-slate-100"}
           (d/div {:class-name "mb-4 space-y-1 shrink-0"}
                  (d/h1 {:class-name "text-xl font-semibold text-slate-100"} "Events")
                  (d/p {:class-name "text-sm text-slate-400"}
                       "Generic event runtime control: schedules, triggers, dispatch, and reset."))

           (if-not can-control?
             (d/div {:class-name "shrink-0 rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400"}
                    "Event runtime control access required.")
             (d/div {:class-name "flex flex-col min-h-0 flex-1 gap-4"}
                    (when tools-error
                      (d/div {:class-name "shrink-0 rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-xs text-amber-200"}
                             (str "Tool catalog unavailable: " tools-error)))
                    ($ event-panel/event-agents-panel {:can-manage can-control?
                                                       :tools tools})))))))
