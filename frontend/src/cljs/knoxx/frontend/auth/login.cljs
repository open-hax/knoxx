(ns knoxx.frontend.auth.login
  "Login methods come from Axxium's live registry."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.credentials :as credentials]
            [knoxx.frontend.auth.forms :as forms]
            [knoxx.frontend.auth.methods :as methods]))
(hx/defnc local-login-form "Authenticate by username or email without conflating their namespaces." [{:keys [on-success]}]
  (let [[identifier set-identifier!] (hooks/use-state "") [password set-password!] (hooks/use-state "")
        [busy set-busy!] (hooks/use-state false) [error set-error!] (hooks/use-state nil)]
    (d/form {:class-name "space-y-4" :on-submit (fn ^:async submit! [event]
                (.preventDefault event) (set-busy! true) (set-error! nil)
                (try (await (api/local-login identifier password)) (on-success)
                     (catch :default exception (set-error! (.-message exception))) (finally (set-busy! false))))}
            (forms/field {:id "login-identifier" :label "Username or email" :value identifier :on-change set-identifier! :auto-complete "username"})
            (forms/field {:id "login-password" :label "Password" :type "password" :value password :on-change set-password! :auto-complete "current-password"})
            (forms/error-box error)
            (d/button {:type "submit" :class-name forms/submit-class :disabled (or busy (str/blank? identifier) (empty? password))} "Sign in with password"))))
(hx/defnc invite-form "Retain the explicitly selected legacy invitation flow." [{:keys [on-success]}]
  (let [[email set-email!] (hooks/use-state "") [code set-code!] (hooks/use-state "")
        [error set-error!] (hooks/use-state nil) [notice set-notice!] (hooks/use-state nil)]
    (d/div {:class-name "space-y-3"}
           (forms/field {:id "invite-email" :label "Email" :value email :on-change set-email!})
           (forms/field {:id "invite-code" :label "Invite code" :value code :on-change set-code!})
           (forms/error-box error) (forms/success-box notice)
           (d/button {:type "button" :class-name forms/submit-class :disabled (str/blank? code)
                      :on-click (fn ^:async redeem! []
                                  (try (await (api/redeem-invite code email)) (set-notice! "Invite accepted! Redirecting…")
                                       (js/setTimeout on-success 300)
                                       (catch :default exception (set-error! (.-message exception)))))} "Redeem invite"))))
(hx/defnc configured-methods "Render advertised methods and explicit provider unavailability." [{:keys [config on-success]}]
  (if (= "axxium" (:identityProvider config))
    (d/div {:class-name "space-y-5"}
           (for [method (:methods config)]
             (case (:kind method)
               "password" (when (:available method) (hx/$ local-login-form {:key (:id method) :on-success on-success}))
               "pgp" (when (:available method) (hx/$ credentials/pgp-form {:key (:id method) :enroll? false :on-success on-success}))
               "passkey" (when (:available method) (hx/$ methods/passkey-button {:key (:id method) :enroll? false :on-success on-success}))
               (hx/$ methods/provider-login {:key (:id method) :method method}))))
    (d/div {:class-name "space-y-4"}
           (when (:localPasswordEnabled config) (hx/$ local-login-form {:on-success on-success}))
           (if (:githubEnabled config) (d/a {:href "/api/auth/login"} "Sign in with GitHub")
               (d/p "GitHub OAuth is not configured. Contact your administrator."))
           (hx/$ invite-form {:on-success on-success}))))
(hx/defnc login-page "Render login and fixed recovery messages; never reflect arbitrary URL text." [{:keys [error on-login-success]}]
  (let [{:keys [config] registry-error :error} (methods/use-registry)
        reason (.get (js/URLSearchParams. (.-search js/window.location)) "reason")]
    (d/main {:class-name "min-h-screen bg-slate-950 px-4 py-10 text-slate-200"}
            (d/div {:class-name "mx-auto max-w-lg space-y-5"}
                   (d/h1 {:class-name "text-2xl font-semibold"} "Sign in to Knoxx")
                   (when (= "axxium" (:identityProvider config)) (d/p "Identity managed by Axxium"))
                   (when (and (seq error) (not= error "Logged out")) (forms/error-box error))
                   (when (= reason "credentials-revoked") (forms/success-box "Axxium sessions ended; sign in again."))
                   (when (= reason "reauthentication-required") (d/p {:role "status"} "Sign in again to manage credentials."))
                   (forms/error-box registry-error)
                   (when config (hx/$ configured-methods {:config config :on-success on-login-success}))
                   (d/a {:href "/signup" :class-name "text-cyan-300"} "Create an account")))))
