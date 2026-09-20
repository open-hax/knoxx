(ns knoxx.backend.infra.db.policy.users
  "Provider-selected users, memberships and their actor-directory updates."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as mongo-actor-creds]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]
            [knoxx.backend.infra.db.policy.hydration :as policy-hydration]))

(declare create-user!)

(defn- credential-row->map
  [{:keys [id provider kind account_identifier status secret_json created_at updated_at]}]
  {:id                 id
   :provider           provider
   :kind               kind
   :accountIdentifier  account_identifier
   :status             status
   :configuredFields   (vec (remove str/blank? (map str (keys (js->clj (or secret_json {}) :keywordize-keys true)))))
   :createdAt          created_at
   :updatedAt          updated_at})

(defn- user-row->map
  [memberships-by-user credentials-by-user {:keys [id email display_name auth_provider external_subject status created_at updated_at]}]
  {:id id :email email :displayName display_name
   :authProvider auth_provider :externalSubject external_subject
   :status status :createdAt created_at :updatedAt updated_at
   :credentials (or (get credentials-by-user id) [])
   :memberships (or (get memberships-by-user id) [])})

(defn- memberships-by-user
  [memberships]
  (reduce (fn [acc m]
            (update acc (:userId m) (fnil conj []) m))
          {}
          memberships))

(defn- credentials-by-user
  [credentials]
  (reduce (fn [acc c]
            (update acc (:user_id c) (fnil conj []) (credential-row->map c)))
          {}
          credentials))

(defn ^:async list-users!
  [pool {:keys [org-id]}]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/users [{:org-id org-id}])
    (let [db (await (policy-connection/db!))
        users (await (mongo-directory/list-users! db {:org-id org-id}))
        user-ids (mapv :id users)
        mem-rows (await (mongo-directory/memberships-for-users-with-org! db user-ids org-id))
        by-user (memberships-by-user (await (policy-hydration/hydrate-memberships pool mem-rows)))
        cred-rows (if org-id
                    (await (mongo-actor-creds/list-credentials-for-users-org! db user-ids org-id))
                    [])
        by-cred-user (credentials-by-user cred-rows)]
    {:users (mapv #(user-row->map by-user by-cred-user %) users)})))

(defn- require-not-blank!
  [value message]
  (when (str/blank? value)
    (throw (js/Error. message))))

(defn ^:async upsert-user-actor-contract-for-membership!
  [pool org-id resolved-actor email display-name role-slugs]
  (let [org-row (await (policy-roles/find-org-by-id pool org-id))]
    (await (policy-support/upsert-actor-contract-best-effort!
            {:actor-id resolved-actor
             :email email
             :display-name display-name
             :org-slug (:slug org-row)
             :role-slugs role-slugs
             :kind :agent}))))

(defn- ^:async mongo-create-user!
  [pool uid mid {:keys [email display-name auth-provider external-subject status
                         membership-status org-id role-slugs role-ids is-default actor-id]
                  :or {auth-provider "local" status "active"
                       membership-status "active" is-default true}}]
  (require-not-blank! email "email is required")
  (require-not-blank! org-id "org-id is required")
  (let [dn (or display-name email)
        resolved-slugs (or role-slugs ["knowledge-worker"])
        actor-contract (policy-support/find-user-actor-contract-by-email email)
        resolved-actor (or (values/normalize-actor-id actor-id) (:id actor-contract)
                           (policy-support/user-actor-id-from-email email)
                           (policy-support/default-membership-actor-id resolved-slugs))
        db (await (policy-connection/db!))
        user (await (mongo-directory/create-user! db {:email email :display-name dn
                                                      :auth-provider auth-provider
                                                      :external-subject external-subject
                                                      :status status}))
        ms (await (mongo-directory/upsert-membership! db {:user-id (:id user)
                                                          :org-id org-id
                                                          :status membership-status
                                                          :is-default is-default}))]
    (await (policy-roles/set-membership-roles! pool (:id ms) {:org-id org-id :role-ids (or role-ids [])
                                                 :role-slugs resolved-slugs :replace true}))
    (await (policy-roles/set-membership-actor-id! pool (:id ms) resolved-actor))
    (await (upsert-user-actor-contract-for-membership! pool org-id resolved-actor email dn resolved-slugs))
    (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                :org-id org-id :action "user.create_or_update"
                                :resource-kind "user" :resource-id (:id user)}))
    {:user user :membership ms}))

(defn ^:async create-user!
  "Create a directory user with the selected provider and verified acting caller."
  [pool uid mid payload]
  (if (local-api/selected? pool)
    (await (local-api/create-user! pool payload))
    (await (mongo-create-user! pool uid mid payload))))

(defn ^:async list-memberships!
  [pool {:keys [org-id]}]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/memberships [{:org-id org-id}])
    (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (let [rows (await (mongo-directory/list-memberships-with-org! (await (policy-connection/db!)) {:org-id org-id}))]
      {:memberships (await (policy-hydration/hydrate-memberships pool rows))}))))

(defn ^:async get-membership!
  [pool membership-id]
  (if (local-api/selected? pool)
    (local-api/read! pool :policy/membership [membership-id])
    (if-let [row (await (mongo-directory/find-membership-row-with-user-org! (await (policy-connection/db!)) membership-id))]
    {:membership (first (await (policy-hydration/hydrate-memberships pool [row])))}
    {:membership nil})))

(defn ^:async set-membership-roles-public!
  [pool uid mid membership-id {:keys [org-id role-ids role-slugs actor-id replace]
                                 :or {replace true} :as opts}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/membership-roles [membership-id opts])
    (let [ms (await (mongo-directory/get-membership! (await (policy-connection/db!)) membership-id))]
    (when-not ms (throw (js/Error. "membership not found")))
    (let [resolved-actor (or (values/normalize-actor-id actor-id)
                             (values/normalize-actor-id (:actor_id ms))
                             (policy-support/default-membership-actor-id (or role-slugs [])))]
      (await (policy-roles/set-membership-roles! pool membership-id {:org-id (or org-id (:org_id ms))
                                                        :role-ids (or role-ids [])
                                                        :role-slugs (or role-slugs [])
                                                        :replace replace}))
      (await (policy-roles/set-membership-actor-id! pool membership-id resolved-actor))
      (let [row (await (mongo-directory/find-membership-row-with-user-org! (await (policy-connection/db!)) membership-id))]
        (await (policy-support/upsert-actor-contract-best-effort!
                {:actor-id resolved-actor :email (:email row)
                 :display-name (:display_name row) :org-slug (:org_slug row)
                 :role-slugs (or role-slugs [])})))
      (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                  :org-id (:org_id ms) :action "membership.roles.update"
                                  :resource-kind "membership" :resource-id membership-id}))
      {:membership nil}))))

(defn ^:async update-user-actor!
  "Update a membership's actor-id and optionally its roles."
  [pool uid mid user-id {:keys [org-id actor-id role-slugs] :as payload}]
  (if (local-api/selected? pool)
    (local-api/command! pool :policy/update-user [user-id payload])
    (let [ms (await (mongo-directory/find-membership-by-user-and-org! (await (policy-connection/db!)) user-id org-id))]
    (if-not ms
      (throw (js/Error. "membership not found"))
      (let [membership-id (:id ms)
            resolved-actor (or (values/normalize-actor-id actor-id)
                               (values/normalize-actor-id (:actor_id ms))
                               (policy-support/default-membership-actor-id (or role-slugs [])))]
        (when (seq role-slugs)
          (await (policy-roles/set-membership-roles! pool membership-id
                                        {:org-id org-id :role-slugs role-slugs :replace true})))
        (await (policy-roles/set-membership-actor-id! pool membership-id resolved-actor))
        (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                    :org-id org-id :action "user.update_actor"
                                    :resource-kind "user" :resource-id user-id}))
        {:ok true})))))
