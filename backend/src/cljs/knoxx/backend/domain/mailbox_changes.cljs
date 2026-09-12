(ns knoxx.backend.domain.mailbox-changes
  "Actor and tenant visibility of accepted mailbox invalidations."
  (:require [knoxx.backend.law.mailbox-changes :as law]
            [knoxx.backend.law.mailbox-store :as mailbox]))

(defn visible?
  "Only this tenant's participants or operator may observe a mailbox change."
  [scope change]
  (mailbox/checked! :mailbox/scope mailbox/Scope scope)
  (law/checked! change)
  (and (= (:org-id scope) (:org-id change))
       (or (:admin? scope) (contains? (set (:actor-ids change)) (:actor-id scope)))))

(defn accepted-change
  "Remove content, identifiers and delivery details from one accepted operation."
  [operation]
  (let [{:keys [kind scope]} (first (:operation/args operation))
        result (:operation/result operation)]
    (when (and (= :mailbox/operation (:operation/method operation))
               (contains? #{:create :claim :mark :ack} kind))
      (let [entries (if (= kind :claim) (:entries result) [result])]
        (law/checked!
         {:org-id (:org-id scope)
          :actor-ids (->> entries
                          (mapcat #(vector (get-in % [:mailbox/source :actor-id])
                                           (get-in % [:mailbox/target :actor-id])))
                          (filter string?) distinct sort vec)})))))
