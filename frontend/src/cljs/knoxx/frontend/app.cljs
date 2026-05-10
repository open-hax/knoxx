(ns knoxx.frontend.app
  "Shadow-cljs owned UI entrypoint (routing + mounting).

   This replaces TS App.tsx + main.tsx as the runtime entry.
   Existing TS components are imported from the Vite-built app bridge.

   react-router-dom is imported directly by shadow-cljs (do NOT re-export it from TS)."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.app-routes :as routes]
            [knoxx.frontend.pages.agents :as agents-page]
            [knoxx.frontend.pages.events :as events-page]
            ["react-router-dom" :as rr]
            ["react-dom/client" :as rdom]
            ["@open-hax/knoxx-app-bridge" :as app]))

(def NavLink (.-NavLink rr))
(def Navigate (.-Navigate rr))
(def Route (.-Route rr))
(def Routes (.-Routes rr))
(def BrowserRouter (.-BrowserRouter rr))
(def useLocation (.-useLocation rr))

(defnc ProtectedSurface [{:keys [children]}]
  (let [auth (app/useAuth)
        location (useLocation)]
    (if (routes/can-access-path? (.-pathname location) (.-roleSlugs auth))
      children
      ($ Navigate {:to "/" :replace true}))))

(defnc LegacyOpsRedirect []
  (let [location (useLocation)]
    ($ Navigate {:to (routes/remap-legacy-ops-path (.-pathname location)
                                                  (.-search location)
                                                  (.-hash location))
                 :replace true})))

(defnc UserMenu []
  (let [[open? set-open] (hooks/use-state false)
        auth (app/useAuth)]
    (when (.-user auth)
      (let [display (or (.. auth -user -displayName) (.. auth -user -email) "")
            org-name (some-> auth .-org .-name)]
        (d/div {:class-name "app-shell__user-menu relative ml-4"}
               (d/button {:on-click #(set-open (not open?))
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
                          (d/button {:on-click (fn []
                                                (set-open false)
                                                (.then (.logout auth) (fn [_] nil)))
                                     :class-name "w-full px-3 py-2 text-left text-sm text-red-400 hover:bg-slate-800 transition"}
                                    "Sign out")))))))))

(defnc PlaceholderPage [{:keys [title]}]
  (d/div {:class-name "flex h-full items-center justify-center p-8 text-slate-300"}
         (d/div {:class-name "max-w-xl rounded-lg border border-slate-800 bg-slate-900/40 p-6"}
                (d/h2 {:class-name "text-lg font-semibold text-white"} title)
                (d/p {:class-name "mt-2 text-sm text-slate-400"}
                     "Route is now owned by shadow-cljs. Page implementation is pending migration."))))

(defnc AppShell []
  (let [auth (app/useAuth)
        basic-user? (routes/basic-user-role? (.-roleSlugs auth))
        nav-class (fn [^js args]
                    (str "app-shell__nav-link" (when (.-isActive args) " app-shell__nav-link--active")))]
    (hooks/use-effect
      :once
      (fn []
        (.add (.-classList js/document.documentElement) "dark")
        js/undefined))

    (d/div {:class-name "app-shell"}
           (d/header {:class-name "app-shell__header"}
                     (d/div {:class-name "app-shell__header-inner"}
                            (d/h1 {:class-name "app-shell__brand"} "Knoxx")
                            (d/nav {:class-name "app-shell__nav" :aria-label "Primary"}
                                   ;; IMPORTANT: when passing props to React components (NavLink), use :className
                                   ;; not :class-name (otherwise React receives an invalid DOM attribute).
                                   ($ NavLink {:to routes/chat-route :className nav-class} "Chat")
                                   ($ NavLink {:to routes/mail-route :className nav-class} "Mail")
                                   ($ NavLink {:to routes/studio-route :className nav-class} "Studio")
                                   (when-not basic-user?
                                     (d/span
                                       ($ NavLink {:to routes/cms-route :className nav-class} "CMS")
                                       ($ NavLink {:to routes/contracts-route :className nav-class} "Contracts")
                                       ($ NavLink {:to routes/data-route :className nav-class} "Data")
                                       ($ NavLink {:to routes/gardens-route :className nav-class} "Gardens")
                                       ($ NavLink {:to routes/translations-route :className nav-class} "Translations")
                                       ($ NavLink {:to routes/events-route :className nav-class} "Events")
                                       ($ NavLink {:to routes/agents-route :className nav-class} "Agents")
                                       ($ NavLink {:to (:admin routes/ops-routes) :className nav-class} "Admin")))
                            ($ UserMenu)))

           (d/main {:class-name "app-shell__main"}
                   ($ Routes
                      ;; All route definitions live here (shadow is router source-of-truth).
                      ($ Route {:path routes/chat-route
                                :element ($ app/ChatPage)})
                      ;; AuthBoundary owns /login and /signup rendering when unauthenticated.
                      ;; When authenticated, treat them as harmless redirects back home.
                      ($ Route {:path routes/login-route
                                :element ($ Navigate {:to routes/chat-route :replace true})})
                      ($ Route {:path routes/signup-route
                                :element ($ Navigate {:to routes/chat-route :replace true})})

                      ($ Route {:path routes/mail-route
                                :element ($ ProtectedSurface {:children ($ app/MailPage)})})
                      ($ Route {:path routes/studio-route
                                :element ($ ProtectedSurface {:children ($ app/BroadcastStudioPage)})})

                      ($ Route {:path routes/cms-route
                                :element ($ ProtectedSurface {:children ($ app/CmsPage)})})
                      ($ Route {:path (str routes/cms-editor-route "/*")
                                :element ($ ProtectedSurface {:children ($ app/VisualCmsEditorPage)})})

                      ($ Route {:path routes/contracts-route
                                :element ($ ProtectedSurface {:children ($ app/ContractsPage)})})

                      ($ Route {:path routes/data-route
                                :element ($ ProtectedSurface {:children ($ app/DataPage)})})
                      ($ Route {:path (str routes/data-route "/:tab")
                                :element ($ ProtectedSurface {:children ($ app/DataPage)})})

                      ($ Route {:path routes/gardens-route
                                :element ($ ProtectedSurface {:children ($ app/GardensPage)})})

                      ($ Route {:path routes/translations-route
                                :element ($ ProtectedSurface {:children ($ app/TranslationReviewPage)})})
                      ($ Route {:path (str routes/translations-route "/:documentId/:targetLang")
                                :element ($ ProtectedSurface {:children ($ app/TranslationReviewPage)})})

                      ($ Route {:path routes/events-route
                                :element ($ ProtectedSurface {:children ($ events-page/EventsPage)})})

                      ;; Agents is the first fully shadow-owned page.
                      ($ Route {:path routes/agents-route
                                :element ($ ProtectedSurface {:children ($ agents-page/AgentsPage)})})

                      ($ Route {:path routes/legacy-event-agents-route
                                :element ($ Navigate {:to routes/events-route :replace true})})

                      ($ Route {:path (str routes/ops-base-path "/*")
                                :element ($ ProtectedSurface {:children ($ app/OpsRoot)})})
                      ($ Route {:path (str routes/legacy-ops-base-path "/*")
                                :element ($ LegacyOpsRedirect)})

                      ($ Route {:path "*"
                                :element ($ Navigate {:to routes/chat-route :replace true})})))))))

(defnc Root []
  ($ BrowserRouter
     {:future #js {:v7_startTransition true
                   :v7_relativeSplatPath true}
      :children
      ($ app/AuthBoundary {:children ($ AppShell)})}))

(defonce root-instance* (atom nil))

(defn mount! []
  (let [root-el (.getElementById js/document "root")]
    (when-not root-el
      (throw (js/Error. "Missing #root element")))
    (when-not @root-instance*
      (reset! root-instance* (.createRoot rdom root-el)))
    (.render @root-instance* ($ Root))))

