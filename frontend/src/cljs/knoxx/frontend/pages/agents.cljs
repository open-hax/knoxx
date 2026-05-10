(ns knoxx.frontend.pages.agents
  "Shadow-owned Agents page.

   Absorbs the legacy TS AgentsPage by rendering the CLJS EventAgentsPanel for
   the Runtime Jobs tab.

   (Audit logs will be reintroduced later as either a CLJS port or a stable TS
   mount once the shadow router baseline is stable.)"
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.admin.event-agents-panel :as event-panel]
            ["react-router-dom" :as rr]
            ["@open-hax/knoxx-frontend-bridge" :as bridge]
            ["@open-hax/knoxx-app-bridge" :as app]))

(def useLocation (.-useLocation rr))
(def useNavigate (.-useNavigate rr))

(defn- get-tab-from-location [^js location]
  (let [search (or (.-search location) "")
        query (js/URLSearchParams. (if (str/starts-with? search "?") (subs search 1) search))
        tab (.get query "tab")]
    (cond
      (= tab "audit") "audit"
      (= tab "control") "control"
      :else "control")))

(defn- set-tab-in-location [^js location tab]
  (let [query (js/URLSearchParams. (if (str/starts-with? (.-search location) "?") (subs (.-search location) 1) (.-search location)))]
    (.set query "tab" tab)
    (let [suffix (.toString query)
          pathname (.-pathname location)
          hash (.-hash location)]
      (str pathname (when (seq suffix) (str "?" suffix)) hash))))

(defnc AgentsPage []
  (let [auth (app/useAuth)
        location (useLocation)
        navigate (useNavigate)
        [tab set-tab] (hooks/use-state (fn [] (get-tab-from-location location)))
        [tools set-tools] (hooks/use-state #js [])
        [tools-error set-tools-error] (hooks/use-state nil)
        can-control? (boolean (or (.-isSystemAdmin auth)
                                 (.includes (or (.-permissions auth) #js []) "org.event_agents.control")))
        can-read-tools? (boolean (or (.-isSystemAdmin auth)
                                    (.includes (or (.-permissions auth) #js []) "org.tool_policy.read")
                                    (.includes (or (.-permissions auth) #js []) "platform.roles.manage")
                                    (.includes (or (.-permissions auth) #js []) "org.user_policy.read")))]

    (hooks/use-effect
      [location]
      (set-tab (get-tab-from-location location)))

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

    (d/div {:data-page "agents"
            :class-name "flex flex-col h-full min-h-0 overflow-hidden bg-slate-950 p-4 text-slate-100"}
           (d/div {:class-name "mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between shrink-0"}
                  (d/div {:class-name "flex items-center gap-2"}
                         (d/button {:type "button"
                                    :on-click #(navigate (set-tab-in-location location "control"))
                                    :class-name (str "rounded-md px-3 py-1.5 text-sm font-medium transition "
                                                    (if (= tab "control")
                                                      "bg-sky-600 text-slate-50 hover:bg-sky-500"
                                                      "bg-transparent text-slate-300 hover:bg-slate-800"))}
                                   "Runtime jobs")
                         (d/button {:type "button"
                                    :on-click #(navigate (set-tab-in-location location "audit"))
                                    :class-name (str "rounded-md px-3 py-1.5 text-sm font-medium transition "
                                                    (if (= tab "audit")
                                                      "bg-sky-600 text-slate-50 hover:bg-sky-500"
                                                      "bg-transparent text-slate-300 hover:bg-slate-800"))}
                                   "Agent Audit Logs"))))

           (d/div {:class-name "min-h-0 flex-1 overflow-hidden"}
                  (if (= tab "control")
                    (d/div {:class-name "flex flex-col h-full min-h-0 gap-4"}
                           (if-not can-control?
                             (d/div {:class-name "shrink-0 rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400"}
                                    "Event-agent control access required.")
                             (d/div
                               (when tools-error
                                 (d/div {:class-name "shrink-0 rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-xs text-amber-200"}
                                        (str "Tool catalog unavailable: " tools-error)))
                               ($ event-panel/event-agents-panel
                                  {:can-manage can-control?
                                   :tools tools}))))
                    (d/div {:class-name "flex h-full items-center justify-center text-sm text-slate-500"}
                           "Audit logs not yet ported to the shadow router."))))))
