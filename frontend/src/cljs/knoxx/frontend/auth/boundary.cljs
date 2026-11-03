(ns knoxx.frontend.auth.boundary
  "Auth boundary. Helix port of src/pages/AuthContext.tsx: fetches
   /api/auth/context, renders login/signup when unauthenticated, and
   provides the auth value (a JS object, shared with TS consumers via
   the bridged context instance) when signed in."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.context :as context]
            [knoxx.frontend.auth.login :as login]
            [knoxx.frontend.auth.signup :as signup]))

(defn- auth-value
  "Builds the JS auth context value in the exact shape TS consumers expect."
  [^js data {:keys [loading error refresh logout]}]
  #js {:user (some-> data .-user)
       :actor (or (some-> data .-actor) nil)
       :org (some-> data .-org)
       :membership (some-> data .-membership)
       :roleSlugs (or (some-> data .-roleSlugs) #js [])
       :permissions (or (some-> data .-permissions) #js [])
       :isSystemAdmin (boolean (some-> data .-isSystemAdmin))
       :authProvider (or (some-> data .-authProvider) "")
       :loading (boolean loading)
       :error (or error nil)
       :refresh refresh
       :logout logout})

(defn- loading-screen []
  (d/div {:class-name "flex h-screen items-center justify-center bg-slate-950 text-slate-400"}
         (d/div {:class-name "text-center"}
                (d/div {:class-name "mb-4 h-8 w-8 animate-spin rounded-full border-2 border-slate-600 border-t-blue-500 mx-auto"})
                (d/p "Loading Knoxx…"))))

(defn- status-banner [message]
  (d/div {:role "alert"
          :class-name "fixed bottom-4 right-4 z-50 rounded-lg border border-amber-600 bg-slate-950 px-4 py-3 text-amber-100"}
         message))

(defn- failure-screen [message refresh]
  (d/div {:class-name "flex h-screen flex-col items-center justify-center gap-4 bg-slate-950 text-slate-100"}
         (d/p {:role "alert"} (str "Could not check your session: " message))
         (d/button {:type "button" :on-click refresh
                    :class-name "rounded bg-blue-600 px-4 py-2"}
                   "Try again")))

(defn- ^:async refresh-auth! [set-auth! set-loading! set-error!]
  (set-loading! true)
  (set-error! nil)
  (try
    (set-auth! (await (api/fetch-auth-context)))
    (catch :default ^js err
      ;; A transient failure cannot prove that an existing session expired.
      (set-error! (or (.-message err) "Authentication is unavailable")))
    (finally
      (set-loading! false))))

(defn- ^:async logout! [set-auth! set-error!]
  (try
    (await (api/logout))
    (set-auth! nil)
    (set-error! nil)
    (catch :default ^js err
      ;; Keep the actor visible until the server confirms the session ended.
      (set-error! (str "Sign out failed: " (or (.-message err) "Request failed"))))))

(hx/defnc auth-boundary
  "Keep verified context through temporary network failures."
  [{:keys [children]}]
  (let [[auth set-auth!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)
        refresh (hooks/use-callback :once
                 (fn [] (refresh-auth! set-auth! set-loading! set-error!)))
        logout (hooks/use-callback :once
                (fn [] (logout! set-auth! set-error!)))
        value (auth-value auth {:loading loading :error error
                                :refresh refresh :logout logout})
        Provider (.-Provider ^js (context/context-instance))]
    (hooks/use-effect [] (refresh) nil)
    (cond
      loading (loading-screen)

      (and error (nil? auth))
      (failure-screen error refresh)

      (nil? (some-> ^js auth .-user))
      (hx/$ Provider {:value value}
         (if (= "/signup" (.-pathname js/window.location))
           (hx/$ signup/signup-page {:error error :on-signup-success refresh})
           (hx/$ login/login-page {:error error :on-login-success refresh})))

      :else
      (hx/$ Provider {:value value} children
            (when error (status-banner error))))))
