(ns knoxx.frontend.auth.signup
  "Signup page. Helix port of src/pages/SignupPage.tsx (testing-only
   basic chat account onboarding)."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.auth.api :as api]))

(def ^:private input-class
  "w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder:text-slate-500 focus:border-blue-500 focus:outline-none")

(defn- field [{:keys [id label type value on-change placeholder hint]}]
  (d/div
   (d/label {:html-for id :class-name "mb-1 block text-sm font-medium text-slate-300"} label)
   (d/input {:id id :type (or type "text") :value value
             :on-change on-change :placeholder placeholder
             :class-name input-class})
   (when hint (d/p {:class-name "mt-1 text-xs text-slate-500"} hint))))

(defnc signup-page [{:keys [error on-signup-success]}]
  (let [[email set-email!] (hooks/use-state "")
        [display-name set-display-name!] (hooks/use-state "")
        [password set-password!] (hooks/use-state "")
        [config set-config!] (hooks/use-state nil)
        [status set-status!] (hooks/use-state :idle)
        [signup-error set-signup-error!] (hooks/use-state "")
        password-enabled? (boolean (some-> ^js config .-localPasswordEnabled))
        submit! (fn []
                  (when (seq (str/trim email))
                    (set-status! :submitting)
                    (set-signup-error! "")
                    (if (and password-enabled? (< (count password) 8))
                      (do (set-status! :error)
                          (set-signup-error! "Password must be at least 8 characters"))
                      (-> (api/signup email display-name password)
                          (.then (fn [_]
                                   (set-status! :success)
                                   (js/setTimeout on-signup-success 300)))
                          (.catch (fn [^js err]
                                    (set-status! :error)
                                    (set-signup-error! (or (.-message err) "Signup failed"))))))))]
    (hooks/use-effect
     []
     (-> (api/fetch-auth-config) (.then set-config!) (.catch (fn [_] nil)))
     nil)
    (d/div {:class-name "flex min-h-screen items-center justify-center bg-slate-950"}
           (d/div {:class-name "w-full max-w-md space-y-8 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl"}
                  (d/div {:class-name "text-center"}
                         (d/h1 {:class-name "text-3xl font-bold text-white"} "Create your Knoxx account")
                         (d/p {:class-name "mt-2 text-sm text-slate-400"}
                              "Testing-only onboarding for basic chat users."))
                  (when (and (seq (or error "")) (not= error "Logged out"))
                    (d/div {:class-name "rounded-lg border border-red-800 bg-red-900/30 p-3 text-sm text-red-300"} error))
                  (d/div {:class-name "space-y-4"}
                         (field {:id "signup-email" :label "Email" :type "email"
                                 :value email :on-change #(set-email! (.. % -target -value))
                                 :placeholder "you@example.com"})
                         (field {:id "signup-display-name" :label "Display name"
                                 :value display-name :on-change #(set-display-name! (.. % -target -value))
                                 :placeholder "Optional"})
                         (when password-enabled?
                           (field {:id "signup-password" :label "Password" :type "password"
                                   :value password :on-change #(set-password! (.. % -target -value))
                                   :placeholder "At least 8 characters"
                                   :hint "Local development password auth is enabled."}))
                         (when (seq signup-error)
                           (d/div {:class-name "rounded-lg border border-red-800 bg-red-900/30 p-3 text-sm text-red-300"} signup-error))
                         (when (= :success status)
                           (d/div {:class-name "rounded-lg border border-green-800 bg-green-900/30 p-3 text-sm text-green-300"}
                                  "Account created. Redirecting…"))
                         (d/button {:on-click submit!
                                    :disabled (or (= :submitting status)
                                                  (str/blank? email)
                                                  (and password-enabled? (< (count password) 8)))
                                    :class-name "w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"}
                                   (if (= :submitting status) "Creating account…" "Create basic account")))
                  (d/div {:class-name "text-center text-xs text-slate-500"}
                         "Already have an account? "
                         (d/a {:href "/login" :class-name "text-blue-400 hover:text-blue-300"} "Sign in"))))))
