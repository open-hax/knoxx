(ns knoxx.backend.shape.db.users
  "Pure HoneySQL query builders for users and actor-credentials.
   No execution."
  (:require [honey.sql.helpers :as h]))

;; ---------------------------------------------------------------------------
;; Lookups
;; ---------------------------------------------------------------------------

(defn by-id
  [user-id]
  (-> (h/select :*)
      (h/from :users)
      (h/where [:= :id [:cast user-id :uuid]])))

(defn by-email
  [email]
  (-> (h/select :*)
      (h/from :users)
      (h/where [:= [:lower :email] [:lower email]])
      (h/limit 1)))

(defn list-all
  []
  (-> (h/select :*)
      (h/from :users)
      (h/order-by :display_name :email)))

(defn list-by-org
  [org-id]
  (-> (h/select-distinct :u.*)
      (h/from [:users :u])
      (h/join [:memberships :m] [:= :m.user_id :u.id])
      (h/where [:= :m.org_id [:cast org-id :uuid]])
      (h/order-by :u.display_name :u.email)))

(defn memberships-for-users
  "Membership + org info for a set of user-ids, within one org."
  [user-ids org-id]
  (-> (h/select :m.* [:o.name :org_name] [:o.slug :org_slug])
      (h/from [:memberships :m])
      (h/join [:orgs :o] [:= :o.id :m.org_id])
      (h/where [:and
                [:in :m.user_id (mapv (fn [id] [:cast id :uuid]) user-ids)]
                [:= :m.org_id [:cast org-id :uuid]]])
      (h/order-by [:m.created_at :asc])))

(defn all-memberships-for-users
  "All memberships for a set of user-ids across all orgs."
  [user-ids]
  (-> (h/select :m.* [:o.name :org_name] [:o.slug :org_slug])
      (h/from [:memberships :m])
      (h/join [:orgs :o] [:= :o.id :m.org_id])
      (h/where [:in :m.user_id (mapv (fn [id] [:cast id :uuid]) user-ids)])
      (h/order-by :o.name [:m.created_at :asc])))

;; ---------------------------------------------------------------------------
;; Mutations
;; ---------------------------------------------------------------------------

(defn upsert
  [{:keys [email display-name auth-provider external-subject status]}]
  (-> (h/insert-into :users)
      (h/values [{:email            [:lower email]
                  :display_name     display-name
                  :auth_provider    auth-provider
                  :external_subject external-subject
                  :status           status}])
      (h/on-conflict :email)
      (h/do-update-set {:display_name     [:excluded :display_name]
                         :auth_provider    [:excluded :auth_provider]
                         :external_subject [:excluded :external_subject]
                         :status           [:excluded :status]
                         :updated_at       [:now]})
      (h/returning :*)))

(defn update-user
  [user-id {:keys [email display-name status auth-provider external-subject]}]
  (-> (h/update :users)
      (h/set (cond-> {:updated_at [:now]}
               email            (assoc :email [:lower email])
               display-name     (assoc :display_name display-name)
               status           (assoc :status status)
               auth-provider    (assoc :auth_provider auth-provider)
               external-subject (assoc :external_subject external-subject)))
      (h/where [:= :id [:cast user-id :uuid]])
      (h/returning :*)))

;; ---------------------------------------------------------------------------
;; Actor credentials
;; ---------------------------------------------------------------------------

(defn credentials-for-users
  [user-ids org-id]
  (-> (h/select :*)
      (h/from :actor_credentials)
      (h/where [:and
                [:in :user_id (mapv (fn [id] [:cast id :uuid]) user-ids)]
                [:= :org_id [:cast org-id :uuid]]])
      (h/order-by :provider :kind)))
