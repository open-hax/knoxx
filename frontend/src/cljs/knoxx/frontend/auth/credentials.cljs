(ns knoxx.frontend.auth.credentials
  "Proof-of-possession enrollment and management for the authenticated principal."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.credential-inventory :as inventory]
            [knoxx.frontend.auth.forms :as forms]
            [knoxx.frontend.auth.methods :as methods]
            [knoxx.frontend.auth.oauth-link :as oauth-link]))
(defn- multiline [label value on-change read-only?]
  (d/label {:class-name "block space-y-2 text-xs text-slate-300"} label
           (d/textarea {:aria-label label :value value :rows 5 :read-only read-only? :class-name forms/input-class
                        :on-change #(on-change (.. % -target -value))})))
(defn- ^:async operation! [state operation]
  ((:set-busy! state) true) ((:set-error! state) nil)
  (try (await (operation)) (catch :default exception ((:set-error! state) (.-message exception)))
       (finally ((:set-busy! state) false))))
(defn- ^:async challenge! [{:keys [enroll? fingerprint set-challenge!]}]
  (set-challenge! (await (api/command "pgp/challenge" (if enroll? {:purpose "enroll"}
                             {:purpose "login" :fingerprint (str/upper-case (str/replace fingerprint #"\s" ""))})))))
(defn- ^:async verify! [{:keys [enroll? challenge signature public-key on-success set-challenge! set-signature! set-public-key!]}]
  (await (api/command (if enroll? "pgp/enroll" "pgp/verify")
                      (cond-> {:challenge-id (:challenge-id challenge) :signature signature} enroll? (assoc :public-key public-key))))
  (set-challenge! nil) (set-signature! "") (when enroll? (set-public-key! "")) (on-success))
(defn use-pgp "Keep challenge text distinct from user-supplied proof." [enroll? on-success]
  (let [[fingerprint set-fingerprint!] (hooks/use-state "") [public-key set-public-key!] (hooks/use-state "")
        [challenge set-challenge!] (hooks/use-state nil) [signature set-signature!] (hooks/use-state "")
        [busy set-busy!] (hooks/use-state false) [error set-error!] (hooks/use-state nil)]
    {:fingerprint fingerprint :set-fingerprint! #(do (set-fingerprint! %) (set-challenge! nil))
     :public-key public-key :set-public-key! #(do (set-public-key! %) (set-challenge! nil))
     :challenge challenge :set-challenge! set-challenge! :signature signature :set-signature! set-signature!
     :busy busy :set-busy! set-busy! :error error :set-error! set-error! :enroll? enroll? :on-success on-success}))
(hx/defnc pgp-form "Submit a detached signature of the exact server challenge." [{:keys [enroll? on-success]}]
  (let [{:keys [fingerprint set-fingerprint! public-key set-public-key! challenge signature set-signature! busy error] :as state}
        (use-pgp enroll? on-success)]
    (d/fieldset {:disabled busy :class-name "space-y-4 rounded-xl border border-slate-700 p-4"}
                (d/legend (if enroll? "Add a PGP key" "Sign in with PGP"))
                (if enroll? (multiline "Armored public key" public-key set-public-key! false)
                    (forms/field {:id "pgp-fingerprint" :label "PGP key fingerprint" :value fingerprint :on-change set-fingerprint!}))
                (d/button {:type "button" :class-name forms/submit-class :disabled (str/blank? (if enroll? public-key fingerprint))
                           :on-click #(operation! state (fn [] (challenge! state)))} "Get signing challenge")
                (when challenge
                  (d/div {:class-name "space-y-4"}
                         (d/p {:class-name "text-xs text-slate-400"} "Sign these exact bytes using your own PGP tool. Your private key stays with you.")
                         (multiline "Exact signing challenge" (:challenge challenge) identity true)
                         (multiline "Armored detached signature" signature set-signature! false)
                         (d/button {:type "button" :class-name forms/submit-class :disabled (str/blank? signature)
                                    :on-click #(operation! state (fn [] (verify! state)))} "Verify PGP signature")))
                (forms/error-box error))))
(hx/defnc credential-enrollment "Expose only advertised enrollment and account-management capabilities." []
  (let [{:keys [config error]} (methods/use-registry) [notice set-notice!] (hooks/use-state nil)
        [revision set-revision!] (hooks/use-state 0) enrolled! (fn [message] (set-notice! message) (set-revision! inc))
        available (set (map :id (filter :available (:methods config))))]
    (d/section {:aria-label "Account credentials" :class-name "space-y-5"}
               (d/h2 {:class-name "text-lg font-semibold"} "Your sign-in credentials")
               (forms/error-box error) (forms/success-box notice)
               (when (contains? available "passkey") (hx/$ methods/passkey-button {:enroll? true :on-success #(enrolled! "Passkey added to this account.")}))
               (when (contains? available "pgp") (hx/$ pgp-form {:enroll? true :on-success #(enrolled! "PGP key added to this account.")}))
               (when (:credentialListUrl config) (hx/$ inventory/inventory-controls {:config config :refresh-key revision}))
               (hx/$ oauth-link/linking-controls {:methods (:methods config)}))))
