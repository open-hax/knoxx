(ns knoxx.backend.shape.db.memberships
  "Pure HoneySQL query builders for memberships, membership-roles, and
   user-tool-policies. No execution — callers pass results to honey-query!."
  (:require [clojure.string :as str]
            [honey.sql.helpers :as h]))

;; ---------------------------------------------------------------------------
;; Base join
;; ---------------------------------------------------------------------------

(def base-query
  "Full membership row joined with user, org, and denormalised columns."
  (-> (h/select
        :m.*
        [:u.email :email]
        [:u.display_name :display_name]
        [:u.status :user_status]
        [:o.slug :org_slug]
        [:o.name :org_name]
        [:o.status :org_status]
        :o.is_primary
        [:o.kind :org_kind])
      (h/from [:memberships :m])
      (h/join [:users :u] [:= :u.id :m.user_id])
      (h/join [:orgs :o] [:= :o.id :m.org_id])))

;; ---------------------------------------------------------------------------
;; Lookups
;; ---------------------------------------------------------------------------

(defn by-id
  [membership-id]
  (-> base-query
      (h/where [:= :m.id [:cast membership-id :uuid]])))

(defn by-email-and-org
  [{:keys [user-email org-id org-slug]}]
  (let [base (-> base-query
                 (h/where [:= [:lower :u.email] [:lower user-email]]))]
    (cond
      (not (str/blank? (str org-id)))
      (-> base
          (h/where [:= :o.id [:cast org-id :uuid]])
          (h/order-by [:m.is_default :desc] [:m.created_at :asc])
          (h/limit 1))

      (not (str/blank? (str org-slug)))
      (-> base
          (h/where [:= [:lower :o.slug] [:lower org-slug]])
          (h/order-by [:m.is_default :desc] [:m.created_at :asc])
          (h/limit 1))

      :else
      (-> base
          (h/order-by [:m.is_default :desc] [:o.is_primary :desc] [:m.created_at :asc])
          (h/limit 1)))))

(defn by-user-and-org
  "Simple membership row (no joins) for a given user/org pair."
  [user-id org-id]
  (-> (h/select :m.* [:o.slug :org_slug])
      (h/from [:memberships :m])
      (h/join [:orgs :o] [:= :o.id :m.org_id])
      (h/where [:and
                [:= :m.user_id [:cast user-id :uuid]]
                [:= :m.org_id [:cast org-id :uuid]]])))

(defn bare-by-id
  "Raw memberships row only (no joins) for id lookup."
  [membership-id]
  (-> (h/select :*)
      (h/from :memberships)
      (h/where [:= :id [:cast membership-id :uuid]])))

(defn list-by-org
  [org-id]
  (-> (h/select :m.* [:o.name :org_name] [:o.slug :org_slug])
      (h/from [:memberships :m])
      (h/join [:orgs :o] [:= :o.id :m.org_id])
      (h/where [:= :m.org_id [:cast org-id :uuid]])
      (h/order-by [:m.created_at :asc])))

(defn role-slugs
  "Slugs of roles assigned to a membership."
  [membership-id]
  (-> (h/select :r.slug)
      (h/from [:membership_roles :mr])
      (h/join [:roles :r] [:= :r.id :mr.role_id])
      (h/where [:= :mr.membership_id [:cast membership-id :uuid]])
      (h/order-by :r.slug)))

(defn with-user-and-org
  "membership + user email/display_name + org slug for audit upserts."
  [membership-id]
  (-> (h/select :m.id [:u.email :email] [:u.display_name :display_name] [:o.slug :org_slug])
      (h/from [:memberships :m])
      (h/join [:users :u] [:= :u.id :m.user_id])
      (h/join [:orgs :o] [:= :o.id :m.org_id])
      (h/where [:= :m.id [:cast membership-id :uuid]])))

;; ---------------------------------------------------------------------------
;; Mutations
;; ---------------------------------------------------------------------------

(defn upsert
  [{:keys [user-id org-id status is-default]}]
  (-> (h/insert-into :memberships)
      (h/values [{:user_id  [:cast user-id :uuid]
                  :org_id   [:cast org-id :uuid]
                  :status   status
                  :is_default is-default}])
      (h/on-conflict :user_id :org_id)
      (h/do-update-set {:status     [:excluded :status]
                         :is_default [:excluded :is_default]
                         :updated_at [:now]})
      (h/returning :*)))

(defn set-actor-id
  [membership-id actor-id]
  (-> (h/update :memberships)
      (h/set {:actor_id actor-id :updated_at [:now]})
      (h/where [:= :id [:cast membership-id :uuid]])))

(defn delete-roles
  [membership-id]
  (-> (h/delete-from :membership_roles)
      (h/where [:= :membership_id [:cast membership-id :uuid]])))

(defn insert-role
  [membership-id role-id]
  (-> (h/insert-into :membership_roles)
      (h/values [{:membership_id [:cast membership-id :uuid]
                  :role_id       [:cast role-id :uuid]}])
      (h/on-conflict :membership_id :role_id)
      (h/do-nothing)))

;; ---------------------------------------------------------------------------
;; Tool policies
;; ---------------------------------------------------------------------------

(defn tool-policies-for-ids
  "user_tool_policies rows for a set of membership ids."
  [membership-ids]
  (-> (h/select :membership_id :tool_id :effect :constraints_json)
      (h/from :user_tool_policies)
      (h/where [:in :membership_id (mapv (fn [id] [:cast id :uuid]) membership-ids)])
      (h/order-by :tool_id)))

(defn delete-tool-policies
  [membership-id]
  (-> (h/delete-from :user_tool_policies)
      (h/where [:= :membership_id [:cast membership-id :uuid]])))

(defn insert-tool-policy
  [membership-id {:keys [tool-id effect constraints-json]}]
  (-> (h/insert-into :user_tool_policies)
      (h/values [{:membership_id  [:cast membership-id :uuid]
                  :tool_id        tool-id
                  :effect         effect
                  :constraints_json [:raw constraints-json]}])
      (h/on-conflict :membership_id :tool_id)
      (h/do-update-set {:effect           [:excluded :effect]
                         :constraints_json [:excluded :constraints_json]})))

;; ---------------------------------------------------------------------------
;; Backfill
;; ---------------------------------------------------------------------------

(def backfill-actor-ids
  "Set actor_id on memberships that have no actor_id based on role presence."
  {:update :memberships
   :set    {:actor_id
            [:case
             [:exists {:select [1]
                       :from   [[:membership_roles :mr]]
                       :join   [[:roles :r] [:= :r.id :mr.role_id]]
                       :where  [:and
                                [:= :mr.membership_id :memberships.id]
                                [:= :r.slug "system_admin"]]}]
             "system_admin"
             :else "workspace_user"]
            :updated_at [:now]}
   :where  [:= [:coalesce [:nullif [:trim :actor_id] ""] ""] ""]})
