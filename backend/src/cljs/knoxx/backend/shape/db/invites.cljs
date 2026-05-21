(ns knoxx.backend.shape.db.invites
  "Pure HoneySQL query builders for invite management. No execution."
  (:require [honey.sql.helpers :as h]))

(defn insert
  [{:keys [org-id code email inviter-membership-id role-slugs-json expires-at]}]
  (-> (h/insert-into :invites)
      (h/values [{:org_id                [:cast org-id :uuid]
                  :code                  code
                  :email                 [:lower email]
                  :inviter_membership_id (when inviter-membership-id
                                           [:cast inviter-membership-id :uuid])
                  :role_slugs            [:raw role-slugs-json]
                  :status                "pending"
                  :expires_at            expires-at}])
      (h/returning :*)))

(defn pending-by-code
  [code]
  (-> (h/select :*)
      (h/from :invites)
      (h/where [:and
                [:= :code code]
                [:= :status "pending"]
                [:> :expires_at [:now]]])
      (h/limit 1)))

(defn redeem
  [invite-id]
  (-> (h/update :invites)
      (h/set {:status      "redeemed"
               :redeemed_at [:now]})
      (h/where [:= :id [:cast invite-id :uuid]])
      (h/returning :*)))

(defn list-by-org
  [org-id]
  (-> (h/select :*)
      (h/from :invites)
      (h/where [:= :org_id [:cast org-id :uuid]])
      (h/order-by [:created_at :desc])))

(defn list-by-org-and-status
  [org-id status]
  (-> (h/select :*)
      (h/from :invites)
      (h/where [:and
                [:= :org_id [:cast org-id :uuid]]
                [:= :status status]])
      (h/order-by [:created_at :desc])))
