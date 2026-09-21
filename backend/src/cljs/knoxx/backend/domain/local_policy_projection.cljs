(ns knoxx.backend.domain.local-policy-projection
  "Tenant-scoped directory projection before administrative wire encoding."
  (:require [knoxx.backend.domain.local-policy :as policy]
            [knoxx.backend.shape.local-policy :as shape]))

(def credential
  "Explicit public/secret credential response boundary."
  shape/credential)

(defn membership
  "Resolve current roles for the exact directory membership."
  [state member]
  (when member
    (shape/membership member (get-in state [:orgs (:org-id member)])
                      (policy/membership-roles state member (get-in state [:users (:user-id member) :identity-profile])))))

(defn user
  "Scope membership and safe credential metadata before encoding a user row."
  [state row org-id]
  (let [members (filter #(and (= (:id row) (:user-id %))
                               (or (nil? org-id) (= org-id (:org-id %))))
                         (vals (:memberships state)))
        credentials (when org-id
                      (filter #(= [(:id row) org-id] [(:user-id %) (:org-id %)])
                              (vals (:credentials state))))]
    (shape/user row (mapv #(membership state %) (policy/ordered members))
                (mapv #(shape/credential % false) (policy/ordered credentials)))))

(defn org
  "Count directory objects in one exact organization."
  [state row]
  (when row
    (shape/org row {:member-count (count (filter #(= (:id row) (:org-id %)) (vals (:memberships state))))
                    :role-count (count (filter #(= (:id row) (:org-id %)) (vals (:roles state))))
                    :data-lake-count (count (filter #(= (:id row) (:org-id %)) (vals (:data-lakes state))))})))
