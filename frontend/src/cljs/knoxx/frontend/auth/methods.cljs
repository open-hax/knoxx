(ns knoxx.frontend.auth.methods
  "Display only server-advertised identity capabilities."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.forms :as forms]
            [knoxx.frontend.auth.webauthn :as webauthn]))
(defn use-registry "Load the current registry and expose failures." []
  (let [[config set-config!] (hooks/use-state nil) [error set-error!] (hooks/use-state nil)]
    (hooks/use-effect :once
      (let [active? (atom true)]
        ((fn ^:async load! []
           (try (let [value (await (api/config-data))] (when @active? (set-config! value)))
                (catch :default exception (when @active? (set-error! (.-message exception)))))))
        #(reset! active? false)))
    {:config config :error error}))
(hx/defnc passkey-button "Ask the browser authenticator to enroll or sign in." [{:keys [enroll? on-success]}]
  (let [[busy set-busy!] (hooks/use-state false) [error set-error!] (hooks/use-state nil)]
    (d/div {:class-name "space-y-3"}
           (d/button {:type "button" :class-name forms/submit-class :disabled busy
                      :on-click (fn ^:async perform []
                                  (set-busy! true) (set-error! nil)
                                  (try (await (webauthn/perform! enroll?)) (on-success)
                                       (catch :default exception (set-error! (.-message exception)))
                                       (finally (set-busy! false))))}
                     (if enroll? "Add a passkey" "Sign in with passkey"))
           (forms/error-box error))))
(hx/defnc provider-login "Initiate a configured provider login, with ATProto discovery input." [{:keys [method]}]
  (let [[handle set-handle!] (hooks/use-state "") atproto? (= "atproto" (:id method))
        available? (and (:available method) (= "GET" (:loginMethod method))
                        (= (:loginUrl method) (str "/api/auth/providers/" (:id method) "/login")))
        href (str (:loginUrl method) (when atproto? (str "?handle=" (js/encodeURIComponent (str/trim handle)))))]
    (d/div {:class-name "space-y-3"}
           (when atproto? (forms/field {:id "atproto-handle" :label "Bluesky handle or DID" :value handle :on-change set-handle!}))
           (if (and available? (or (not atproto?) (not (str/blank? handle))))
             (d/a {:href href :class-name forms/submit-class} (str "Sign in with " (:label method)))
             (d/button {:type "button" :disabled true :class-name forms/submit-class} (str "Sign in with " (:label method))))
           (when-not (:available method) (d/p {:class-name "text-xs text-slate-400"} (str (:label method) " is not configured on this instance."))))))
