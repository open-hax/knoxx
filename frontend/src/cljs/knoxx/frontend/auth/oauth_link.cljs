(ns knoxx.frontend.auth.oauth-link
  "Explicit authenticated provider linking controls."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.forms :as forms]))
(defn navigate! "Navigate after the service confirms a safe issuer destination." [url] (set! (.-href js/window.location) url))
(hx/defnc provider-link "Keep ATProto input and refused-link errors visible." [{:keys [method]}]
  (let [[handle set-handle!] (hooks/use-state "") [busy set-busy!] (hooks/use-state false)
        [error set-error!] (hooks/use-state nil) atproto? (= "atproto" (:id method))]
    (d/div {:class-name "space-y-3"}
           (when atproto? (forms/field {:id "atproto-link-handle" :label "Bluesky handle or DID to link" :value handle :on-change set-handle!}))
           (d/button {:type "button" :class-name forms/submit-class
                      :disabled (or busy (not (:available method)) (and atproto? (str/blank? handle)))
                      :on-click (fn ^:async link! []
                                  (set-busy! true) (set-error! nil)
                                  (try (navigate! (await (api/link-provider method handle)))
                                       (catch :default exception (set-error! (.-message exception)))
                                       (finally (set-busy! false))))}
                     (str "Link " (:label method)))
           (when-not (:available method) (d/p {:class-name "text-xs text-slate-400"} (str (:label method) " linking is not configured on this instance.")))
           (forms/error-box error))))
(hx/defnc linking-controls "Expose only advertised OAuth linking endpoints." [{advertised-methods :methods}]
  (d/section {:aria-label "Link identity providers" :class-name "space-y-4"}
             (for [method advertised-methods :when (and (:linkUrl method) (contains? #{"github" "discord" "google" "atproto"} (:id method)))]
               (hx/$ provider-link {:key (:id method) :method method}))))
