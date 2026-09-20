(ns knoxx.frontend.auth.signup
  "Password enrollment with separate username, email and display name fields."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.forms :as forms]))
(hx/defnc signup-page "Create an ordinary Axxium principal and refresh verified authorization." [{:keys [on-signup-success]}]
  (let [[draft set-draft!] (hooks/use-state {:username "" :email "" :display-name "" :password "" :confirmation ""})
        [busy set-busy!] (hooks/use-state false) [error set-error!] (hooks/use-state nil)]
    (d/main {:class-name "min-h-screen bg-slate-950 p-6 text-slate-200"}
            (d/form {:class-name "mx-auto max-w-lg space-y-4" :on-submit
                     (fn ^:async submit! [event]
                       (.preventDefault event) (set-error! nil)
                       (if (not= (:password draft) (:confirmation draft)) (set-error! "Passwords do not match.")
                           (do (set-busy! true)
                               (try (await (api/signup (:username draft) (:email draft) (:display-name draft) (:password draft)))
                                    (on-signup-success)
                                    (catch :default exception (set-error! (.-message exception)))
                                    (finally (set-busy! false))))))}
                    (d/h1 {:class-name "text-2xl font-semibold"} "Create your Knoxx account")
                    (for [[field label field-type] [[:username "Username" "text"] [:email "Email" "email"]
                                                   [:display-name "Display name" "text"] [:password "Password" "password"]
                                                   [:confirmation "Confirm password" "password"]]]
                      (d/div {:key (name field)}
                             (forms/field {:id (str "signup-" (name field)) :label label :type field-type :value (get draft field)
                                           :on-change #(set-draft! assoc field %)})))
                    (forms/error-box error)
                    (d/button {:type "submit" :class-name forms/submit-class
                               :disabled (or busy (some str/blank? (map draft [:username :email :password :confirmation])))} "Create account")
                    (d/a {:href "/login"} "Back to sign in")))))
