(ns knoxx.backend.law.route-registration-proof
  "Contracts for the isolated production-route verification fixture."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank
  "A required fixture string, never a missing or whitespace-only option."
  [:fn #(and (string? %) (not (str/blank? %)))])

(defn- loopback-origin? [value]
  (boolean
   (when (string? value)
     (when-let [[_ port] (re-matches #"http://127\.0\.0\.1:([1-9][0-9]{0,4})" value)]
       (<= 1 (parse-long port) 65535)))))

(def Options
  "The complete decoded ESM fixture options and its owned loopback upstream."
  [:map {:closed true}
   [:workspace-root NonBlank]
   [:contracts-dir NonBlank]
   [:project-name NonBlank]
   [:session-project-name NonBlank]
   [:openplanner-client-mode [:= "rest"]]
   [:openplanner-base-url [:fn loopback-origin?]]
   [:openplanner-api-key NonBlank]])

(defn assert-options!
  "Reject malformed fixture options before allocating or registering an app."
  [options]
  (when-not (m/validate Options options)
    (throw (ex-info "Invalid route registration proof options"
                    {:status 400 :code "route_registration_proof_options_invalid"})))
  options)
