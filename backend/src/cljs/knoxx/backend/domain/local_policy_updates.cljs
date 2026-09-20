(ns knoxx.backend.domain.local-policy-updates
  "Pure scoped grant, membership and credential transitions."
  (:require [knoxx.backend.domain.local-policy :as policy]
            [knoxx.backend.law.local-policy :as law]
            [knoxx.backend.domain.local-policy-projection :as projection]))

(defn- matching-org!
  [actual requested]
  (when (and requested (not= actual requested))
    (law/refuse! 403 "local_policy_org_mismatch" "Request organization differs from the actual membership")))

(defn- actor-change!
  [state member requested]
  (let [user (law/present! state :users (:user-id member))]
    (when (and (:principal-id user) requested (not= (:actor-id member) requested))
      (law/refuse! 403 "local_policy_actor_immutable" "Axxium principal binding cannot be reassigned"))
    (when (and requested (not= (:actor-id member) requested))
      (law/nonblank! requested)
      (law/unique! (vals (:memberships state))
                   #(and (not= (:id member) (:id %))
                         (= [(:org-id member) requested] [(:org-id %) (:actor-id %)]))))
    (or requested (:actor-id member))))

(defn membership-roles
  "Apply exact scoped role IDs; replacing with an empty vector revokes all role grants."
  [state actor member-id payload at]
  (let [member (law/present! state :memberships member-id)
        org-id (:org-id member)
        ctx (policy/authorize! state actor org-id "org.members.update")
        _ (matching-org! org-id (:org-id payload))
        selected (policy/resolve-roles state org-id (:role-ids payload) (:role-slugs payload))
        roles (if (not= false (:replace payload)) selected
                  (policy/ordered (vals (into {} (map (juxt :id identity))
                                              (concat (policy/membership-roles state member (get-in state [:users (:user-id member) :identity-profile])) selected)))))
        _ (policy/grantable! ctx (conj roles {:permissions [] :tool-policies (:tool-policies member)}))
        next-member (assoc member :role-ids (mapv :id roles) :actor-id (actor-change! state member (:actor-id payload)) :updated-at at)
        next (assoc-in state [:memberships member-id] next-member)]
    [next {:membership (projection/membership next next-member)}]))

(defn tool-policies
  "Replace a scoped role or membership policy with validated, non-escalating grants."
  [state actor collection id items at]
  (let [object (law/present! state collection id)
        permission (if (= collection :roles) "org.tool_policy.update" "org.user_policy.update")
        ctx (policy/authorize! state actor (:org-id object) permission)
        tools (law/tool-policies items)
        roles (if (= collection :roles) [(assoc object :tool-policies tools)]
                  (conj (policy/membership-roles state object (get-in state [:users (:user-id object) :identity-profile]))
                        {:permissions [] :tool-policies tools}))
        _ (policy/grantable! ctx roles)
        next-object (assoc object :tool-policies tools :updated-at at)
        next (assoc-in state [collection id] next-object)]
    [next (if (= collection :roles) {:role next-object}
              {:membership (projection/membership next next-object)})]))

(defn- update-profile
  [state actor user-id payload at]
  (let [user (law/present! state :users user-id)
        ctx (policy/actor-context state actor)]
    (when (seq (:identity-fields payload))
      (law/refuse! 400 "identity_owned_by_axxium" "Change identity aliases and login providers through Axxium"))
    (when (and (contains? payload :status) (not (policy/administrator? ctx)))
      (law/refuse! 403 "local_policy_global_user_status" "Only a platform administrator may change global user status"))
    (cond-> (assoc user :updated-at at)
      (contains? payload :display-name) (assoc :display-name (law/nonblank! (:display-name payload)))
      (contains? payload :status) (assoc :status (law/status! (:status payload))))))

(defn update-user
  "Update local profile and scoped membership without touching identity aliases."
  [state actor user-id payload at]
  (let [org-id (:org-id payload)
        _ (policy/authorize! state actor org-id "org.members.update")
        member (or (policy/membership-for state user-id org-id)
                   (law/refuse! 404 "local_policy_membership_not_found" "User has no membership in this organization"))
        user (update-profile state actor user-id payload at)
        member (cond-> (assoc member :actor-id (actor-change! state member (:actor-id payload)) :updated-at at)
                 (contains? payload :membership-status) (assoc :status (law/status! (:membership-status payload))))
        next (-> state (assoc-in [:users user-id] user) (assoc-in [:memberships (:id member)] member))
        next (if (contains? payload :role-slugs)
               (first (membership-roles next actor (:id member) (assoc payload :replace true) at)) next)
        next (if (contains? payload :tool-policies)
               (first (tool-policies next actor :memberships (:id member) (:tool-policies payload) at)) next)]
    [next {:ok true :user (projection/user next user org-id)
           :membership (projection/membership next (get-in next [:memberships (:id member)]))}]))

(defn upsert-credential
  "One user/org/provider identity owns a credential; directory responses redact its secret."
  [state actor user-id payload id at]
  (let [org-id (:org-id payload)
        _ (policy/authorize! state actor org-id "org.user_policy.update")
        provider (law/nonblank! (:provider payload))
        member (or (policy/membership-for state user-id org-id)
                   (law/refuse! 404 "local_policy_membership_not_found" "Credential owner has no membership in this organization"))
        existing (some #(when (= [user-id org-id provider] [(:user-id %) (:org-id %) (:provider %)]) %) (vals (:credentials state)))
        secret (law/assert-schema! [:and :map [:fn seq]] (:secret-json payload))
        row {:id (or (:id existing) id) :user-id user-id :org-id org-id :actor-id (:actor-id member)
             :principal-id (get-in state [:users user-id :principal-id])
             :provider provider :kind (or (:kind payload) "credential") :secret-json secret
             :account-identifier (:account-identifier payload) :status (law/status! (or (:status payload) "active"))
             :created-at (or (:created-at existing) at) :updated-at at}]
    (law/nonblank! (:actor-id member))
    (when (= provider "local")
      (law/refuse! 400 "identity_owned_by_axxium" "Password credentials are owned by Axxium"))
    [(assoc-in state [:credentials (:id row)] row) {:credential (projection/credential row false)}]))
