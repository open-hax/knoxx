(ns knoxx.backend.shape.db.roles
  "Pure HoneySQL query builders for roles, role-permissions, and
   role-tool-policies. No execution."
  (:require [honey.sql.helpers :as h]))

;; ---------------------------------------------------------------------------
;; Lookups
;; ---------------------------------------------------------------------------

(defn by-id
  [role-id]
  (-> (h/select :*)
      (h/from :roles)
      (h/where [:= :id [:cast role-id :uuid]])))

(defn by-slug
  "Platform-scoped (org_id IS NULL) or org-scoped role by slug."
  [{:keys [slug org-id]}]
  (-> (h/select :*)
      (h/from :roles)
      (h/where [:and
                [:= :slug slug]
                (if (nil? org-id)
                  [:= :org_id nil]
                  [:= :org_id [:cast org-id :uuid]])])
      (h/limit 1)))

(defn by-ids
  [role-ids]
  (-> (h/select :*)
      (h/from :roles)
      (h/where [:in :id (mapv (fn [id] [:cast id :uuid]) role-ids)])
      (h/order-by :name)))

(defn by-slugs-and-org
  "Return id + slug for a seq of slugs, checking both platform and org scope."
  [slugs org-id]
  (-> (h/select :id :slug)
      (h/from :roles)
      (h/where [:and
                [:in :slug slugs]
                [:or [:= :org_id nil]
                     [:= :org_id [:cast org-id :uuid]]]])))

(defn list-all
  []
  (-> (h/select :*)
      (h/from :roles)
      (h/order-by [:built_in :desc] :name)))

(defn list-by-org
  [org-id]
  (-> (h/select :*)
      (h/from :roles)
      (h/where [:= :org_id [:cast org-id :uuid]])
      (h/order-by [:built_in :desc] :name)))

;; ---------------------------------------------------------------------------
;; Mutations
;; ---------------------------------------------------------------------------

(defn insert
  [{:keys [org-id name slug scope-kind built-in system-managed]}]
  (-> (h/insert-into :roles)
      (h/values [{:org_id         (when org-id [:cast org-id :uuid])
                  :name           name
                  :slug           slug
                  :scope_kind     scope-kind
                  :built_in       built-in
                  :system_managed system-managed}])
      (h/returning :*)))

(defn update-role
  [role-id {:keys [name scope-kind built-in system-managed]}]
  (-> (h/update :roles)
      (h/set {:name           name
               :scope_kind     scope-kind
               :built_in       built-in
               :system_managed system-managed
               :updated_at     [:now]})
      (h/where [:= :id [:cast role-id :uuid]])
      (h/returning :*)))

;; ---------------------------------------------------------------------------
;; Permissions
;; ---------------------------------------------------------------------------

(defn permissions-for-roles
  "permission_code rows for a set of role ids (modern schema)."
  [role-ids]
  (-> (h/select :role_id [:permission_code :code])
      (h/from :role_permissions)
      (h/where [:in :role_id (mapv (fn [id] [:cast id :uuid]) role-ids)])
      (h/order-by :permission_code)))

(defn permissions-for-roles-legacy
  "permission_code via join on permissions table (legacy schema)."
  [role-ids]
  (-> (h/select :rp.role_id [:p.code :code])
      (h/from [:role_permissions :rp])
      (h/join [:permissions :p] [:= :p.id :rp.permission_id])
      (h/where [:in :rp.role_id (mapv (fn [id] [:cast id :uuid]) role-ids)])
      (h/order-by :p.code)))

(defn delete-permissions
  [role-id]
  (-> (h/delete-from :role_permissions)
      (h/where [:= :role_id [:cast role-id :uuid]])))

(defn insert-permission-modern
  [role-id code]
  (-> (h/insert-into :role_permissions)
      (h/values [{:role_id         [:cast role-id :uuid]
                  :permission_code code
                  :effect          "allow"}])
      (h/on-conflict :role_id :permission_code)
      (h/do-update-set {:effect [:raw "EXCLUDED.effect"]})))

;; ---------------------------------------------------------------------------
;; Tool policies
;; ---------------------------------------------------------------------------

(defn tool-policies-for-roles
  [role-ids]
  (-> (h/select :rtp.role_id :rtp.tool_id :rtp.effect :rtp.constraints_json)
      (h/from [:role_tool_policies :rtp])
      (h/where [:in :rtp.role_id (mapv (fn [id] [:cast id :uuid]) role-ids)])
      (h/order-by :rtp.tool_id)))

(defn delete-tool-policies
  [role-id]
  (-> (h/delete-from :role_tool_policies)
      (h/where [:= :role_id [:cast role-id :uuid]])))

(defn insert-tool-policy
  [role-id {:keys [tool-id effect constraints-json]}]
  (-> (h/insert-into :role_tool_policies)
      (h/values [{:role_id          [:cast role-id :uuid]
                  :tool_id          tool-id
                  :effect           effect
                  :constraints_json [:cast constraints-json :jsonb]}])
      (h/on-conflict :role_id :tool_id)
      (h/do-update-set {:effect           [:raw "EXCLUDED.effect"]
                         :constraints_json [:raw "EXCLUDED.constraints_json"]})))

;; ---------------------------------------------------------------------------
;; Membership-role linking
;; ---------------------------------------------------------------------------

(defn roles-for-memberships
  "Role rows joined via membership_roles for a set of membership ids."
  [membership-ids]
  (-> (h/select :mr.membership_id [:r.id :role_id] :r.slug :r.name :r.scope_kind :r.org_id)
      (h/from [:membership_roles :mr])
      (h/join [:roles :r] [:= :r.id :mr.role_id])
      (h/where [:in :mr.membership_id (mapv (fn [id] [:cast id :uuid]) membership-ids)])
      (h/order-by :r.name)))

(defn insert-membership-role
  [membership-id role-id]
  (-> (h/insert-into :membership_roles)
      (h/values [{:membership_id [:cast membership-id :uuid]
                  :role_id       [:cast role-id :uuid]}])
      (h/on-conflict :membership_id :role_id)
      (h/do-nothing)))
