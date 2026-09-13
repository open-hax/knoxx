(ns knoxx.backend.domain.auth.axxium
  "Identity bindings preserve the subject and leave roles under local control."
  (:require [clojure.string :as str]
            [knoxx.backend.law.axxium-identity :as law]))
(defn normalize-actor "Use the same email key as the local directory." [actor]
  (update (law/require-actor! actor) :email #(-> % str/trim str/lower-case)))
(defn subject "Bind an authenticated subject to its configured authority." [issuer actor]
  (law/require-actor! actor)
  (str issuer "#" (:id actor)))
(defn local-user "A new local directory record contains no remote password or privileges." [issuer actor user-id instance-id]
  {:user_id user-id :email (:email (normalize-actor actor)) :display_name (:display_name actor)
   :auth_provider "axxium" :external_subject (subject issuer actor)
   :status "active" :system_instance_id instance-id})
