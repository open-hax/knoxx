(ns knoxx.backend.domain.local-policy-directory
  "Pure directory transitions admitted as immutable Clio operation facts."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.local-policy :as policy]
            [knoxx.backend.law.identity-binding :as bindings]
            [knoxx.backend.law.local-policy :as law]
            [knoxx.backend.domain.local-policy-projection :as projection]))

(defn initialize
  "Install explicit org/role/tool catalogs once; replay never overwrites later administration."
  [state {:keys [org roles tools]}]
  (let [org (assoc org :status (law/status! (:status org)))
        state (if (get-in state [:orgs (:id org)]) state
                (do (law/unique! (vals (:orgs state)) #(= (:slug org) (:slug %)))
                    (assoc-in state [:orgs (law/nonblank! (:id org))] org)))
        state (reduce-kv (fn [result slug role]
                          (let [id (str "local_role_" slug)]
                            (if (get-in result [:roles id]) result
                              (assoc-in result [:roles id]
                                        (merge role {:id id :slug slug :org-id nil :scope-kind "platform"
                                                     :name (or (:name role) slug) :built-in true :system-managed true
                                                     :tool-policies (law/tool-policies (:tool-policies role))})))))
                        state roles)]
    [(update state :tools #(merge (into {} (map (juxt :id identity)) tools) %)) {:initialized? true}]))

(defn- principal-user
  [binding principal at]
  {:id (:user-id binding) :principal-id (:principal/id principal)
   :entity-id (:principal/entity-id principal) :kind (:principal/kind principal)
   :username (:principal/username principal) :email (:principal/email principal)
   :display-name (:principal/display-name principal) :auth-provider "axxium"
   :external-subject (:principal/id principal) :status "active"
   :identity-profile principal :created-at at :updated-at at})

(defn observe
  "Observe an already verified actor binding. Email is never an adoption key."
  [state binding principal at]
  (bindings/assert-principal-binding! principal binding)
  (law/present! state :orgs (:org-id binding))
  (let [user (get-in state [:users (:user-id binding)])
        member (get-in state [:memberships (:membership-id binding)])]
    (when (and user (not= [(:principal-id user) (:entity-id user) (:kind user)]
                          [(:principal/id principal) (:principal/entity-id principal) (:principal/kind principal)]))
      (law/refuse! 409 "local_policy_identity_conflict" "Directory user is bound to another actor"))
    (when (and member (not= [(:user-id binding) (:org-id binding) (:actor-id binding)]
                            [(:user-id member) (:org-id member) (:actor-id member)]))
      (law/refuse! 409 "local_policy_identity_conflict" "Directory membership has a different binding"))
    (when-not member
      (law/unique! (vals (:memberships state))
                   #(= [(:org-id binding) (:actor-id binding)] [(:org-id %) (:actor-id %)])))
    [(-> state
         (assoc-in [:users (:user-id binding)]
                   (assoc (or user (principal-user binding principal at)) :identity-profile principal
                          :username (:principal/username principal) :email (:principal/email principal)))
         (assoc-in [:memberships (:membership-id binding)]
                   (or member {:id (:membership-id binding) :user-id (:user-id binding)
                               :org-id (:org-id binding) :actor-id (:actor-id binding)
                               :status "active" :is-default true :created-at at :updated-at at})))
     {:user-id (:user-id binding) :membership-id (:membership-id binding)}]))

(defn create-org
  "Create a unique organization without converting a create into an update."
  [state actor payload id at]
  (policy/authorize! state actor nil "platform.org.create")
  (let [name (law/nonblank! (:name payload))
        slug (law/slug (or (:slug payload) name))
        org {:id id :name name :slug slug :kind (or (:kind payload) "customer")
             :status (law/status! (or (:status payload) "active")) :is-primary false
             :created-at at :updated-at at}]
    (law/unique! (vals (:orgs state)) #(= slug (:slug %)))
    [(assoc-in state [:orgs id] org) {:org org}]))

(defn- new-user
  [payload user-id at]
  (let [email (some-> (:email payload) str/trim not-empty)]
    (when-not (or email (some-> (:display-name payload) str/trim not-empty))
      (law/refuse! 400 "local_policy_identity_required" "An unbound directory user needs a display name or email"))
    {:id user-id :email email :display-name (or (:display-name payload) email)
     :auth-provider "unbound" :external-subject nil :status (law/status! (or (:status payload) "active"))
     :created-at at :updated-at at}))

(defn- user-candidate
  [state payload ids at]
  (if-let [principal (:verified-principal payload)]
    (let [binding (:verified-binding payload)
          existing (get-in state [:users (:user-id binding)])]
      (bindings/assert-principal-binding! principal binding)
      (when (and existing (not= [(:principal-id existing) (:entity-id existing) (:kind existing)]
                                [(:principal/id principal) (:principal/entity-id principal) (:principal/kind principal)]))
        (law/refuse! 409 "local_policy_identity_conflict" "The user already has a different stable identity"))
      (or existing (principal-user binding principal at)))
    (new-user payload (:user-id ids) at)))

(defn- unique-user-membership!
  [state user org-id actor-id]
  (when (policy/membership-for state (:id user) org-id)
    (law/refuse! 409 "local_policy_membership_exists" "This user already has an organization membership"))
  (when (and (:email user) (nil? (:principal-id user)))
    (law/unique! (vals (:users state))
                 #(and (nil? (:principal-id %)) (= (some-> (:email %) str/lower-case) (str/lower-case (:email user)))
                       (policy/membership-for state (:id %) org-id))))
  (when actor-id
    (law/unique! (vals (:memberships state)) #(= [org-id actor-id] [(:org-id %) (:actor-id %)]))))

(defn create-user
  "Create an unbound row or explicitly bind a verified actor; never merge on email."
  [state actor payload ids at]
  (let [org-id (:org-id payload)
        ctx (policy/authorize! state actor org-id "org.users.create")
        _ (law/present! state :orgs org-id)
        user (user-candidate state payload ids at)
        roles (policy/resolve-roles state org-id (:role-ids payload) (:role-slugs payload))
        tools (law/tool-policies (:tool-policies payload))
        binding (:verified-binding payload)
        actor-id (or (:actor-id binding) (:actor-id payload))
        member-id (or (:membership-id binding) (:membership-id ids))
        member {:id member-id :user-id (:id user) :org-id org-id :actor-id actor-id
                :status (law/status! (or (:membership-status payload) "active"))
                :is-default (not= false (:is-default payload)) :role-ids (mapv :id roles)
                :tool-policies tools :created-at at :updated-at at}]
    (when (and binding (:actor-id payload) (not= (:actor-id binding) (:actor-id payload)))
      (law/refuse! 403 "local_policy_actor_immutable" "A bound Axxium actor cannot be reassigned"))
    (policy/grantable! ctx (conj roles {:permissions [] :tool-policies tools}))
    (unique-user-membership! state user org-id actor-id)
    (let [next (-> state (assoc-in [:users (:id user)] user) (assoc-in [:memberships member-id] member))]
      [next {:user (projection/user next user org-id) :membership (projection/membership next member)}])))

(defn create-role
  "Create a unique scoped role with grants no broader than the admitting administrator."
  [state actor payload id at]
  (let [org-id (:org-id payload)
        ctx (policy/authorize! state actor org-id "org.roles.create")
        _ (law/present! state :orgs org-id)
        name (law/nonblank! (:name payload))
        slug (law/slug (or (:slug payload) name))
        role {:id id :org-id org-id :name name :slug slug :scope-kind "org"
              :built-in false :system-managed false :created-at at :updated-at at
              :permissions (vec (sort (set (map law/nonblank! (or (:permission-codes payload) [])))))
              :tool-policies (law/tool-policies (:tool-policies payload))}]
    (when (= "system-admin" slug)
      (law/refuse! 409 "local_policy_reserved_role" "The platform administrator role name is reserved"))
    (law/unique! (vals (:roles state)) #(= [org-id slug] [(:org-id %) (:slug %)]))
    (policy/grantable! ctx [role])
    [(assoc-in state [:roles id] role) {:role role}]))

(defn create-data-lake
  "Store a named data-lake configuration in the same tenant-scoped directory."
  [state actor payload id at]
  (let [org-id (:org-id payload)
        _ (policy/authorize! state actor org-id "org.datalakes.create")
        _ (law/present! state :orgs org-id)
        name (law/nonblank! (:name payload))
        slug (law/slug (or (:slug payload) name))
        lake {:id id :org-id org-id :name name :slug slug :kind (or (:kind payload) "workspace_docs")
              :config (law/assert-schema! :map (or (:config payload) {}))
              :status (law/status! (or (:status payload) "active")) :created-at at :updated-at at}]
    (law/unique! (vals (:data-lakes state)) #(= [org-id slug] [(:org-id %) (:slug %)]))
    [(assoc-in state [:data-lakes id] lake) {:data-lake lake}]))
