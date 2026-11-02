(ns knoxx.backend.law.membership-actor
  "An assigned membership actor is an immutable identity coordinate.
   Reassignment requires an explicit identity migration, never a policy patch."
  (:require [knoxx.backend.law.policy-values :as values]))

(defn assert-assignment!
  "Allow initial assignment or the same normalized actor; refuse rebinds with 409."
  [current requested]
  (let [assigned (values/normalize-actor-id current)
        desired (values/normalize-actor-id requested)]
    (when-not desired
      (throw (ex-info "A membership actor is required"
                      {:status 400 :code "membership_actor_required"})))
    (when (and assigned (not= assigned desired))
      (throw (ex-info "Assigned actor coordinates require an explicit identity migration"
                      {:status 409 :code "membership_actor_immutable"})))
    desired))
