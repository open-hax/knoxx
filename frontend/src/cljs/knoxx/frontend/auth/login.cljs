(ns knoxx.frontend.auth.login
  "Login page. Helix port of src/pages/LoginPage.tsx (local password,
   GitHub OAuth redirect, invite redemption)."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.auth.api :as api]))

(def ^:private input-class
  "w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none")

(def ^:private submit-class
  "w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed")

(defn- error-box [text]
  (d/div {:class-name "rounded-lg bg-red-900/30 border border-red-800 p-3 text-sm text-red-300"} text))

(defn- success-box [text]
  (d/div {:class-name "rounded-lg bg-green-900/30 border border-green-800 p-3 text-sm text-green-300"} text))

(defn- labeled-input [{:keys [id label type value on-change on-key-down placeholder mono]}]
  (d/div
   (d/label {:html-for id :class-name "block text-sm font-medium text-slate-300 mb-1"} label)
   (d/input {:id id
             :type (or type "text")
             :value value
             :on-change on-change
             :on-key-down on-key-down
             :placeholder placeholder
             :class-name (str input-class (when mono " font-mono"))})))

(defnc local-login-form [{:keys [on-success]}]
  (let [[email set-email!] (hooks/use-state "")
        [password set-password!] (hooks/use-state "")
        [status set-status!] (hooks/use-state :idle)
        [error set-error!] (hooks/use-state "")
        submit! (fn []
                  (when (and (seq (str/trim email)) (seq password))
                    (set-status! :submitting)
                    (set-error! "")
                    (-> (api/local-login email password)
                        (.then (fn [_]
                                 (set-status! :success)
                                 (js/setTimeout on-success 300)))
                        (.catch (fn [^js err]
                                  (set-status! :error)
                                  (set-error! (or (.-message err) "Login failed")))))))]
    (d/div {:class-name "space-y-4"}
           (labeled-input {:id "login-local-email" :label "Email" :type "email"
                           :value email :on-change #(set-email! (.. % -target -value))
                           :placeholder "you@example.com"})
           (labeled-input {:id "login-local-password" :label "Password" :type "password"
                           :value password :on-change #(set-password! (.. % -target -value))
                           :on-key-down #(when (= "Enter" (.-key %)) (submit!))
                           :placeholder "Local development password"})
           (when (seq error) (error-box error))
           (when (= :success status) (success-box "Signed in. Redirecting…"))
           (d/button {:on-click submit!
                      :disabled (or (= :submitting status)
                                    (str/blank? email)
                                    (empty? password))
                      :class-name submit-class}
                     (if (= :submitting status) "Signing in…" "Sign in with password")))))

(defnc invite-form [{:keys [initial-code initial-email initial-error on-success]}]
  (let [[code set-code!] (hooks/use-state (or initial-code ""))
        [email set-email!] (hooks/use-state (or initial-email ""))
        [status set-status!] (hooks/use-state :idle)
        [error set-error!] (hooks/use-state (or initial-error ""))
        submit! (fn []
                  (when (seq (str/trim code))
                    (set-status! :submitting)
                    (set-error! "")
                    (-> (api/redeem-invite code email)
                        (.then (fn [_]
                                 (set-status! :success)
                                 (js/setTimeout on-success 500)))
                        (.catch (fn [^js err]
                                  (set-status! :error)
                                  (set-error! (or (.-message err) "Redemption failed")))))))]
    (d/div {:class-name "space-y-4"}
           (labeled-input {:id "login-invite-email" :label "Email" :type "email"
                           :value email :on-change #(set-email! (.. % -target -value))
                           :placeholder "you@example.com"})
           (labeled-input {:id "login-invite-code" :label "Invite code"
                           :value code :on-change #(set-code! (.. % -target -value))
                           :placeholder "Enter your invite code" :mono true})
           (when (seq error) (error-box error))
           (when (= :success status) (success-box "Invite accepted! Redirecting…"))
           (d/button {:on-click submit!
                      :disabled (or (= :submitting status) (str/blank? code))
                      :class-name submit-class}
                     (if (= :submitting status) "Redeeming…" "Redeem invite")))))

(defn- github-button [login-url]
  (d/button {:on-click #(set! (.-href js/window.location)
                              (str login-url "?redirect=" (js/encodeURIComponent (.-href js/window.location))))
             :class-name "flex w-full items-center justify-center gap-3 rounded-lg bg-slate-800 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-700 border border-slate-700"}
            "Continue with GitHub"))

(defn- divider [text]
  (d/div {:class-name "relative"}
         (d/div {:class-name "absolute inset-0 flex items-center"}
                (d/div {:class-name "w-full border-t border-slate-700"}))
         (d/div {:class-name "relative flex justify-center text-sm"}
                (d/span {:class-name "bg-slate-900 px-2 text-slate-500"} text))))

(defn- url-param [k]
  (.get (js/URLSearchParams. js/window.location.search) k))

(defnc login-page [{:keys [error on-login-success]}]
  (let [[config set-config!] (hooks/use-state nil)
        invite-code (url-param "invite")
        invite-email (url-param "email")
        not-whitelisted? (= "not_whitelisted" (url-param "error"))]
    (hooks/use-effect
     []
     (-> (api/fetch-auth-config)
         (.then set-config!)
         (.catch (fn [_] nil)))
     nil)
    (d/div {:class-name "flex min-h-screen items-center justify-center bg-slate-950"}
           (d/div {:class-name "w-full max-w-md space-y-8 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl"}
                  (d/div {:class-name "text-center"}
                         (d/h1 {:class-name "text-3xl font-bold text-white"} "Knoxx")
                         (d/p {:class-name "mt-2 text-sm text-slate-400"} "Knowledge operations platform"))
                  (when (and (seq (or error "")) (not= error "Logged out"))
                    (error-box error))
                  (when (some-> ^js config .-localPasswordEnabled)
                    ($ local-login-form {:on-success on-login-success}))
                  (if (some-> ^js config .-githubEnabled)
                    (github-button (some-> ^js config .-loginUrl))
                    (d/div {:class-name "rounded-lg bg-amber-900/30 border border-amber-800 p-3 text-sm text-amber-300"}
                           "GitHub OAuth is not configured. Contact your administrator."))
                  (divider "or redeem an invite")
                  ($ invite-form {:initial-code invite-code
                                  :initial-email invite-email
                                  :initial-error (when not-whitelisted?
                                                   "Your GitHub account is not on the allowlist. Enter an invite code below to gain access.")
                                  :on-success on-login-success})
                  (d/p {:class-name "text-center text-xs text-slate-600"}
                       "By signing in, you agree to the Knoxx terms of service.")
                  (d/p {:class-name "text-center text-xs text-slate-500"}
                       "Need a basic chat account for testing? "
                       (d/a {:href "/signup" :class-name "text-blue-400 hover:text-blue-300"} "Sign up"))))))
