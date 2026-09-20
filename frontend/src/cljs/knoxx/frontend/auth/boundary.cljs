(ns knoxx.frontend.auth.boundary
  "Verified authentication shared by native pages and the existing TypeScript bridge."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.context :as context]
            [knoxx.frontend.auth.login :as login]
            [knoxx.frontend.auth.signup :as signup]))
(defn- auth-value [^js data {:keys [loading error refresh logout]}]
  #js {:user (some-> data .-user) :actor (some-> data .-actor) :org (some-> data .-org)
       :membership (some-> data .-membership) :roleSlugs (or (some-> data .-roleSlugs) #js [])
       :permissions (or (some-> data .-permissions) #js []) :isSystemAdmin (boolean (some-> data .-isSystemAdmin))
       :authProvider (or (some-> data .-authProvider) "") :loading loading :error error :refresh refresh :logout logout})
(defn use-session "Refresh verified authority; a failed logout keeps the current session visible." []
  (let [[auth set-auth!] (hooks/use-state nil) [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)
        refresh (hooks/use-callback :once
                  (fn ^:async refresh-session []
                    (set-loading! true) (set-error! nil)
                    (try (set-auth! (await (api/fetch-auth-context)))
                         (catch :default exception (set-auth! nil) (set-error! (.-message exception)))
                         (finally (set-loading! false)))))
        logout (hooks/use-callback :once
                 (fn ^:async logout-session []
                   (try (await (api/logout)) (set-auth! nil) (set-error! "Logged out")
                        (catch :default exception (set-error! (.-message exception)) (throw exception)))))]
    (hooks/use-effect :once (refresh) nil)
    {:auth auth :loading loading :error error :refresh refresh :logout logout}))
(hx/defnc auth-boundary "Render credentials until the server returns an authenticated user." [{:keys [children]}]
  (let [{:keys [auth loading error refresh] :as session} (use-session)
        value (auth-value auth session) Provider (.-Provider ^js (context/context-instance))]
    (if loading
      (d/div {:class-name "flex h-screen items-center justify-center bg-slate-950 text-slate-400" :role "status"} "Loading Knoxx…")
      (hx/$ Provider {:value value}
            (if (some-> ^js auth .-user) children
                (if (= "/signup" (.-pathname js/window.location))
                  (hx/$ signup/signup-page {:error error :on-signup-success refresh})
                  (hx/$ login/login-page {:error error :on-login-success refresh})))))))
