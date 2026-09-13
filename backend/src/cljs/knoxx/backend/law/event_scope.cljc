(ns knoxx.backend.law.event-scope
  "Closed tenant coordinates carried by explicitly opted-in, trusted events."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  "A concrete identifier, never an absent or whitespace-only scope."
  [:and :string [:fn #(not (str/blank? %))]])

(def Scope
  "Admitted tenant and principal coordinates; payload authority is forbidden."
  [:map {:closed true}
   [:org-id NonBlankString]
   [:membership-id NonBlankString]
   [:project {:optional true} NonBlankString]])

(defn assert-scope!
  "Refuse missing scope, ambiguous identifiers and extra authority fields."
  [scope]
  (if (m/validate Scope scope)
    scope
    (throw (ex-info "event scope requires admitted organization and membership"
                    {:status 403 :code "invalid_event_scope"
                     :errors (me/humanize (m/explain Scope scope))}))))

(defn scoped-config
  "Replace ambient tenant coordinates with one validated event scope."
  [config scope]
  (let [{:keys [org-id project]} (assert-scope! scope)]
    (cond-> (assoc (dissoc config :session-project-name) :openplanner-org-id org-id)
      project (assoc :session-project-name project))))
