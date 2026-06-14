(ns knoxx.frontend.auth.boundary
  "Auth boundary. Helix port of src/pages/AuthContext.tsx: fetches
   /api/auth/context, renders login/signup when unauthenticated, and
   provides the auth value (a JS object, shared with TS consumers via
   the bridged context instance) when signed in."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.context :as context]
            [knoxx.frontend.auth.login :refer [login-page]]
            [knoxx.frontend.auth.signup :refer [signup-page]]))

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

(defnc auth-boundary [{:keys [children]}]
  (let [[auth set-auth!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)
        refresh (hooks/use-callback
                 :once
                 (fn refresh-fn []
                   (set-loading! true)
                   (set-error! nil)
                   (-> (api/fetch-auth-context)
                       (.then (fn [data] (set-auth! data)))
                       (.catch (fn [^js err]
                                 (set-auth! nil)
                                 (set-error! (or (.-message err) "Not authenticated"))))
                       (.finally #(set-loading! false)))))
        logout (hooks/use-callback
                :once
                (fn []
                  (-> (api/logout)
                      (.finally (fn []
                                  (set-auth! nil)
                                  (set-error! "Logged out"))))))
        value (auth-value auth {:loading loading :error error
                                :refresh refresh :logout logout})
        Provider (.-Provider ^js (context/context-instance))]
    (hooks/use-effect [] (refresh) nil)
    (cond
      loading (loading-screen)

      (nil? (some-> ^js auth .-user))
      ($ Provider {:value value}
         (if (= "/signup" (.-pathname js/window.location))
           ($ signup-page {:error error :on-signup-success refresh})
           ($ login-page {:error error :on-login-success refresh})))

      :else
      ($ Provider {:value value} children))))
