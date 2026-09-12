(ns knoxx.frontend.app
  "Shadow-cljs owned UI entrypoint (routing + mounting).

   This replaces TS App.tsx + main.tsx as the runtime entry.
   Existing TS components are imported from the Vite-built app bridge.

   react-router-dom is imported directly by shadow-cljs (do NOT re-export it from TS)."
  (:require ["@open-hax/knoxx-app-bridge" :as app]
            ["@open-hax/knoxx-frontend-bridge" :as frontend]
            ["react-dom/client" :as rdom]
            ["react-router-dom" :as rr]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.app-routes :as routes]
            [knoxx.frontend.auth.account :as account]
            [knoxx.frontend.auth.boundary :as boundary]
            [knoxx.frontend.auth.context :as auth-ctx]
            [knoxx.frontend.infra.navigation-guard :as navigation]
            [knoxx.frontend.pages.agents :as agents-page]
            [knoxx.frontend.pages.cms.view :as cms]
            [knoxx.frontend.pages.events :as events-page]
            [knoxx.frontend.pages.gardens.view :as gardens-page]
            [knoxx.frontend.pages.mail.view :as mail-page]
            [knoxx.frontend.pages.translations.view :as translations-page]))

;; The shared auth React context instance must be the bridge's module
;; instance so TS pages' useAuth reads the value the CLJS boundary provides.
(auth-ctx/set-context-instance! (.-AuthContextInstance app))

(def NavLink "Router navigation link." (.-NavLink rr))
(def Navigate "Router redirect." (.-Navigate rr))
(def ^{:doc "Router route declaration."} Route (.-Route rr))
(def Routes "Router switch." (.-Routes rr))
(def BrowserRouter "Browser router provider." (.-BrowserRouter rr))
(def useLocation "Current router location." (.-useLocation rr))

(hx/defnc ProtectedSurface "Enforce the current role grants before rendering a protected route." [{:keys [children]}]
  (let [auth (auth-ctx/use-auth)
        location (useLocation)]
    (if (routes/can-access-path? (.-pathname location) (.-roleSlugs auth))
      children
      (hx/$ Navigate {:to "/" :replace true}))))

(hx/defnc LegacyOpsRedirect "Preserve old Operations URLs through the route mapper." []
  (let [location (useLocation)]
    (hx/$ Navigate {:to (routes/remap-legacy-ops-path (.-pathname location)
                                                   (.-search location)
                                                   (.-hash location))
                 :replace true})))

(hx/defnc UserMenu "Offer account management and guard sign-out against unsaved work." []
  (let [[open? set-open] (hooks/use-state false)
        auth (auth-ctx/use-auth)]
    (when (.-user auth)
      (let [display (or (.. auth -user -displayName) (.. auth -user -email) "")
            org-name (some-> auth .-org .-name)]
        (d/div {:class-name "app-shell__user-menu relative ml-4"}
               (d/button {:aria-label "Account menu" :on-click #(set-open (not open?))
                          :class-name "flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800 transition"}
                         (d/span {:class-name "h-6 w-6 rounded-full bg-blue-600 flex items-center justify-center text-xs font-bold text-white"}
                                 (-> display (subs 0 1) (.toUpperCase)))
                         (d/span {:class-name "hidden md:inline"} display))
               (when open?
                 (d/div
                  (d/div {:class-name "fixed inset-0 z-10" :on-click #(set-open false)})
                  (d/div {:class-name "absolute right-0 top-full mt-1 z-20 w-56 rounded-lg border border-slate-700 bg-slate-900 py-1 shadow-xl"}
                         (d/div {:class-name "px-3 py-2 border-b border-slate-800"}
                                (d/p {:class-name "text-sm font-medium text-white truncate"} display)
                                (d/p {:class-name "text-xs text-slate-400 truncate"} (.. auth -user -email))
                                (when org-name
                                  (d/p {:class-name "text-xs text-slate-500 mt-0.5"} org-name)))
                         (hx/$ NavLink {:to "/account" :onClick #(set-open false) :className "block px-3 py-2 text-sm text-cyan-300"} "Account & sign-in")
                         (when-let [message (.-error auth)] (d/p {:role "alert" :class-name "px-3 text-red-300"} message))
                         (d/button {:on-click (fn ^:async sign-out []
                                                (when (navigation/request-sign-out!)
                                                  (try (await (.logout auth)) (set-open false)
                                                       (catch :default _ nil))))
                                    :class-name "w-full px-3 py-2 text-left text-sm text-red-400 hover:bg-slate-800 transition"}
                                   "Sign out")))))))))

(hx/defnc PlaceholderPage "Identify a route awaiting migration." [{:keys [title]}]
  (d/div {:class-name "flex h-full items-center justify-center p-8 text-slate-300"}
         (d/div {:class-name "max-w-xl rounded-lg border border-slate-800 bg-slate-900/40 p-6"}
                (d/h2 {:class-name "text-lg font-semibold text-white"} title)
                (d/p {:class-name "mt-2 text-sm text-slate-400"}
                     "Route is now owned by shadow-cljs. Page implementation is pending migration."))))

(def route-elements
  "Explicit route declarations shared by the browser and migration census."
  #js [
   (hx/$ Route {:path routes/chat-route
               :key "route-0" :element (hx/$ app/ChatPage)})
   (hx/$ Route {:path "/account"
               :key "route-1" :element (hx/$ account/account-page)})
   (hx/$ Route {:path routes/login-route
               :key "route-2" :element (hx/$ Navigate {:to routes/chat-route :replace true})})
   (hx/$ Route {:path routes/signup-route
               :key "route-3" :element (hx/$ Navigate {:to routes/chat-route :replace true})})
   (hx/$ Route {:path routes/mail-route
               :key "route-4" :element (hx/$ ProtectedSurface {:children (hx/$ mail-page/mail-page)})})
   (hx/$ Route {:path routes/studio-route
               :key "route-5" :element (hx/$ ProtectedSurface {:children (hx/$ app/BroadcastStudioPage)})})
   (hx/$ Route {:path routes/cms-route
               :key "route-6" :element (hx/$ ProtectedSurface {:children (hx/$ cms/cms-page {:Markdown frontend/Markdown})})})
   (hx/$ Route {:path (str routes/cms-editor-route "/*")
               :key "route-7" :element (hx/$ ProtectedSurface {:children (hx/$ cms/cms-page {:Markdown frontend/Markdown})})})
   (hx/$ Route {:path routes/contracts-route
               :key "route-8" :element (hx/$ ProtectedSurface {:children (hx/$ app/ContractsPage)})})
   (hx/$ Route {:path routes/data-route
               :key "route-9" :element (hx/$ ProtectedSurface {:children (hx/$ app/DataPage)})})
   (hx/$ Route {:path (str routes/data-route "/:tab")
               :key "route-10" :element (hx/$ ProtectedSurface {:children (hx/$ app/DataPage)})})
   (hx/$ Route {:path routes/gardens-route
               :key "route-11" :element (hx/$ ProtectedSurface {:children (hx/$ gardens-page/gardens-page)})})
   (hx/$ Route {:path routes/translations-route
               :key "route-12" :element (hx/$ ProtectedSurface {:children (hx/$ translations-page/translation-review-page)})})
   (hx/$ Route {:path (str routes/translations-route "/:documentId/:targetLang")
               :key "route-13" :element (hx/$ ProtectedSurface {:children (hx/$ translations-page/translation-review-page)})})
   (hx/$ Route {:path routes/events-route
               :key "route-14" :element (hx/$ ProtectedSurface {:children (hx/$ events-page/EventsPage)})})
   (hx/$ Route {:path routes/agents-route
               :key "route-15" :element (hx/$ ProtectedSurface {:children (hx/$ agents-page/AgentsPage)})})
   (hx/$ Route {:path routes/legacy-event-agents-route
               :key "route-16" :element (hx/$ Navigate {:to routes/events-route :replace true})})
   (hx/$ Route {:path (str routes/ops-base-path "/*")
               :key "route-17" :element (hx/$ ProtectedSurface {:children (hx/$ app/OpsRoot)})})
   (hx/$ Route {:path (str routes/legacy-ops-base-path "/*")
               :key "route-18" :element (hx/$ LegacyOpsRedirect)})
   (hx/$ Route {:path "*"
               :key "route-19" :element (hx/$ Navigate {:to routes/chat-route :replace true})})])

(hx/defnc MainRoutes "Render the statically declared application routes." []
  (hx/$ Routes {:children route-elements}))

(hx/defnc AppHeader "Show route navigation for the current authorization context." []
  (let [auth (auth-ctx/use-auth)
        basic-user? (routes/basic-user-role? (.-roleSlugs auth))
        nav-class (fn [^js args] (str "app-shell__nav-link" (when (.-isActive args) " app-shell__nav-link--active")))]
    (d/header {:class-name "app-shell__header"}
                     (d/div {:class-name "app-shell__header-inner"}
                            (d/h1 {:class-name "app-shell__brand"} "Knoxx")
                            (d/nav {:class-name "app-shell__nav" :aria-label "Primary"}
                                   ;; IMPORTANT: when passing props to React components (NavLink), use :className
                                   ;; not :class-name (otherwise React receives an invalid DOM attribute).
                                   (hx/$ NavLink {:to routes/chat-route :className nav-class} "Chat")
                                   (hx/$ NavLink {:to routes/mail-route :className nav-class} "Mail")
                                   (hx/$ NavLink {:to routes/studio-route :className nav-class} "Studio")
                                   (when-not basic-user?
                                     (d/span
                                      (hx/$ NavLink {:to routes/cms-route :className nav-class} "Wiki")
                                      (hx/$ NavLink {:to routes/contracts-route :className nav-class} "Contracts")
                                      (hx/$ NavLink {:to routes/data-route :className nav-class} "Data")
                                      (hx/$ NavLink {:to routes/gardens-route :className nav-class} "Gardens")
                                      (hx/$ NavLink {:to routes/translations-route :className nav-class} "Translations")
                                      (hx/$ NavLink {:to routes/events-route :className nav-class} "Events")
                                      (hx/$ NavLink {:to routes/agents-route :className nav-class} "Agents")
                                      (hx/$ NavLink {:to (:admin routes/ops-routes) :className nav-class} "Admin")))
                                   (hx/$ UserMenu)))

                     )))

(hx/defnc AppShell "Compose navigation and the currently selected page." []
  (hooks/use-effect :once (.add (.-classList js/document.documentElement) "dark") js/undefined)
  (d/div {:class-name "app-shell"} (hx/$ AppHeader)
         (d/main {:class-name "app-shell__main"} (hx/$ MainRoutes))))

(hx/defnc Root "Compose browser routing with verified authentication." []
  (hx/$ BrowserRouter
     {:future #js {:v7_startTransition true
                   :v7_relativeSplatPath true}
      :children
      (hx/$ boundary/auth-boundary {:children (hx/$ AppShell)})}))

(defonce ^{:doc "Mounted React root retained during hot reload."} root-instance* (atom nil))

(defn mount! "Mount the authenticated application in the browser root." []
  (let [root-el (.getElementById js/document "root")]
    (when-not root-el
      (throw (js/Error. "Missing #root element")))
    (when-not @root-instance*
      (reset! root-instance* (.createRoot rdom root-el)))
    (.render @root-instance* (hx/$ Root))))
