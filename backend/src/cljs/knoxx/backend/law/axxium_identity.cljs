(ns knoxx.backend.law.axxium-identity
  "Contracts for identities authenticated by the operator-configured Axxium authority."
  (:require [clojure.string :as str]
            [malli.core :as m]))
(def Actor
  "The minimum authenticated profile required to bind a local user."
  [:map [:id [:re #"^actor_[a-zA-Z0-9-]+$"]]
   [:email [:and string? [:fn #(and (not (str/blank? %)) (str/includes? % "@"))]]]
   [:display_name [:string {:min 1 :max 200}]] [:status [:= "active"]]])
(defn require-actor! "Reject malformed authority responses before persistence." [actor]
  (when-not (m/validate Actor actor)
    (throw (ex-info "Identity provider returned an invalid account" {:status 502})))
  actor)
(defn same-binding? "An email collision is never sufficient to link an existing account." [user subject]
  (and (= "axxium" (:auth_provider user)) (= subject (:external_subject user)) (= "active" (:status user))))
