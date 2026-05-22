(ns knoxx.backend.domain.policy.sql-adapter
  "SQL projection/secret-state adapter for policy data.

   This adapter does not define canonical actor identity. It projects validated
   actor contracts into SQL and stores mutable credential state. Queries are
   expressed as HoneySQL data and formatted at the edge."
  (:require [clojure.string :as str]
            [honey.sql :as sql]
            [knoxx.backend.domain.policy.protocol :as policy]))

(defrecord SqlPolicyStore [query-one! query! find-org-by-slug! set-membership-roles! primary-org])

(defn create-store
  [{:keys [query-one! query! find-org-by-slug! set-membership-roles! primary-org]}]
  (->SqlPolicyStore query-one! query! find-org-by-slug! set-membership-roles! primary-org))

(defn format-sql
  [query-map]
  (sql/format query-map {:numbered true}))

(defn- execute-one!
  [store query-map]
  (let [[sql-str & params] (format-sql query-map)]
    ((:query-one! store) sql-str params)))

(defn- normalize-email
  [value]
  (some-> value str str/trim str/lower-case not-empty))

(defn actor-email-from-id
  [actor-id]
  (let [slug (-> (str actor-id)
                 str/trim
                 str/lower-case
                 (str/replace #"[^a-z0-9._+-]+" "-")
                 (str/replace #"^[-.]+|[-.]+$" ""))]
    (when-not (str/blank? slug)
      (str slug "@actors.local"))))

(defn actor-credentials-select-query
  [provider]
  {:select [:ac.* :m.actor_id :m.user_id :m.org_id [:o.slug :org_slug]]
   :from [[:actor_credentials :ac]]
   :join [[:memberships :m]
          [:and
           [:= :m.user_id :ac.user_id]
           [:= :m.org_id :ac.org_id]]
          [:orgs :o]
          [:= :o.id :ac.org_id]]
   :where [:and
           [:= :ac.provider provider]
           [:= :ac.status "active"]]
   :order-by [[:m.actor_id :asc] [:ac.updated_at :desc]]})

(defn actor-credential-select-query
  [actor-id provider]
  {:select [:ac.* :m.actor_id :m.user_id :m.org_id [:o.slug :org_slug]]
   :from [[:actor_credentials :ac]]
   :join [[:memberships :m]
          [:and
           [:= :m.user_id :ac.user_id]
           [:= :m.org_id :ac.org_id]]
          [:orgs :o]
          [:= :o.id :ac.org_id]]
   :where [:and
           [:= :m.actor_id actor-id]
           [:= :ac.provider provider]
           [:= :ac.status "active"]]
   :order-by [[:m.is_default :desc] [:ac.updated_at :desc]]
   :limit 1})

(defn actor-membership-select-query
  [{:keys [actor-id user-id org-id]}]
  {:select [:m.* [:o.slug :org_slug]]
   :from [[:memberships :m]]
   :join [[:orgs :o] [:= :o.id :m.org_id]]
   :where (cond-> [:and]
            actor-id (conj [:= :m.actor_id actor-id])
            user-id (conj [:= :m.user_id [:cast user-id :uuid]])
            org-id (conj [:= :m.org_id [:cast org-id :uuid]]))
   :order-by [[:m.is_default :desc] [:m.updated_at :desc]]
   :limit 1})

(defn actor-user-upsert-query
  [{:keys [email display-name auth-provider external-subject status]}]
  {:insert-into :users
   :columns [:email :display_name :auth_provider :external_subject :status]
   :values [[email display-name auth-provider external-subject (or status "active")]]
   :on-conflict [:email]
   :do-update-set {:display_name [:raw "EXCLUDED.display_name"]
                   :auth_provider [:raw "EXCLUDED.auth_provider"]
                   :external_subject [:raw "EXCLUDED.external_subject"]
                   :status [:raw "EXCLUDED.status"]
                   :updated_at [:raw "NOW()"]}
   :returning [:*]})

(defn actor-user-update-query
  [user-id {:keys [email display-name status auth-provider external-subject]}]
  {:update :users
   :set {:email [:coalesce email :email]
         :display_name [:coalesce display-name :display_name]
         :status [:coalesce status :status]
         :auth_provider [:coalesce auth-provider :auth_provider]
         :external_subject [:coalesce external-subject :external_subject]
         :updated_at [:raw "NOW()"]}
   :where [:= :id [:cast user-id :uuid]]
   :returning [:*]})

(defn actor-membership-upsert-query
  [{:keys [user-id org-id actor-id status is-default]}]
  {:insert-into :memberships
   :columns [:user_id :org_id :actor_id :status :is_default]
   :values [[[:cast user-id :uuid]
             [:cast org-id :uuid]
             actor-id
             (or status "active")
             (not= is-default false)]]
   :on-conflict [:user_id :org_id]
   :do-update-set {:actor_id [:raw "EXCLUDED.actor_id"]
                   :status [:raw "EXCLUDED.status"]
                   :is_default [:raw "EXCLUDED.is_default"]
                   :updated_at [:raw "NOW()"]}
   :returning [:*]})

(defn actor-membership-actor-update-query
  [membership-id actor-id]
  {:update :memberships
   :set {:actor_id actor-id
         :updated_at [:raw "NOW()"]}
   :where [:= :id [:cast membership-id :uuid]]
   :returning [:*]})

(defn actor-credential-upsert-query
  [{:keys [user-id org-id provider kind account-identifier secret-json status]}]
  {:insert-into :actor_credentials
   :columns [:user_id :org_id :provider :kind :account_identifier :secret_json :status]
   :values [[[:cast user-id :uuid]
             [:cast org-id :uuid]
             provider
             (or kind "credential")
             account-identifier
             [:cast secret-json :jsonb]
             (or status "active")]]
   :on-conflict [:user_id :org_id :provider :kind]
   :do-update-set {:account_identifier [:coalesce [:raw "EXCLUDED.account_identifier"] :actor_credentials.account_identifier]
                   :secret_json [:|| :actor_credentials.secret_json [:raw "EXCLUDED.secret_json"]]
                   :status [:raw "EXCLUDED.status"]
                   :updated_at [:raw "NOW()"]}
   :returning [:*]})

(defn user-memberships-query
  [user-ids org-id]
  (cond-> {:select [:m.* [:o.name :org_name] [:o.slug :org_slug]]
           :from [[:memberships :m]]
           :join [[:orgs :o] [:= :o.id :m.org_id]]
           :where [:= :m.user_id [:any [:cast (into-array user-ids) :uuid[]]]]
           :order-by [[:o.name :asc] [:m.created_at :asc]]}
    org-id
    (assoc :where [:and
                   [:= :m.user_id [:any [:cast (into-array user-ids) :uuid[]]]]
                   [:= :m.org_id [:cast org-id :uuid]]]
           :order-by [[:m.created_at :asc]])))

(defn- row->credential
  [row]
  (when row
    {:id                 (:id row)
     :actorId            (:actor_id row)
     :userId             (:user_id row)
     :orgId              (:org_id row)
     :orgSlug            (:org_slug row)
     :provider           (:provider row)
     :kind               (:kind row)
     :accountIdentifier  (:account_identifier row)
     :status             (:status row)
     :secretJson         (js->clj (or (:secret_json row) {}) :keywordize-keys true)
     :createdAt          (:created_at row)
     :updatedAt          (:updated_at row)}))

(defn- actor-role-slugs
  [actor]
  (->> (or (:actor/roles actor) [])
       (map (fn [role]
              (cond
                (keyword? role) (-> role name (str/replace #"_" "-"))
                (string? role) (-> role str/trim (str/replace #"_" "-"))
                :else nil)))
       (remove str/blank?)
       distinct
       vec))

(defn- project-actor!
  [store actor]
  (let [validated (policy/validate-actor! actor)
        actor-id (:actor/id validated)
        email (or (normalize-email (:actor/email validated))
                  (actor-email-from-id actor-id))
        display-name (or (some-> (:actor/label validated) str str/trim not-empty)
                         actor-id
                         email)
        auth-provider "actor-contract"
        role-slugs (actor-role-slugs validated)]
    (-> (execute-one! store (actor-user-upsert-query {:email email
                                                      :display-name display-name
                                                      :auth-provider auth-provider
                                                      :external-subject nil
                                                      :status "active"}))
        (.then
         (fn [user]
           (let [org-promise (if-let [org-slug (some-> (:actor/org validated) str str/trim not-empty)]
                               ((:find-org-by-slug! store) org-slug)
                               (js/Promise.resolve nil))]
             (-> org-promise
                 (.then
                  (fn [org]
                    (let [target-org (or org (:primary-org store))]
                      (when-not target-org
                        (throw (js/Error. "primary org is required for actor projection sync")))
                      (execute-one! store (actor-membership-upsert-query {:user-id (:id user)
                                                                          :org-id (:id target-org)
                                                                          :actor-id actor-id
                                                                          :status "active"
                                                                          :is-default true})))))
                 (.then
                  (fn [membership]
                    (if-let [set-roles! (:set-membership-roles! store)]
                      (-> (set-roles! (:id membership)
                                      {:org-id (:org_id membership)
                                       :role-slugs role-slugs
                                       :role-ids #js []
                                       :replace true
                                       :contract-projection true})
                          (.then (fn [_] membership))
                          (.catch
                           (fn [err]
                             (.warn js/console
                                    "[policy-sql] actor role projection failed; keeping actor membership"
                                    actor-id
                                    (.-message err))
                             membership)))
                      membership))))))))))

(extend-type SqlPolicyStore
  policy/PolicyStore
  (list-contracts [_store _contract-class]
    (throw (js/Error. "SQL policy adapter is projection-only; read canonical contracts through the EDN adapter.")))

  (get-contract [_store _contract-class _contract-id]
    (throw (js/Error. "SQL policy adapter is projection-only; read canonical contracts through the EDN adapter.")))

  (upsert-contract! [_store _contract-class _contract]
    (throw (js/Error. "SQL policy adapter is projection-only; write canonical contracts through the EDN adapter.")))

  (list-actors [_store]
    (policy/list-contracts _store :actors))

  (get-actor [_store actor-id]
    (policy/get-contract _store :actors actor-id))

  (upsert-actor! [_store actor]
    (policy/upsert-contract! _store :actors actor))

  policy/ActorCredentialStore
  (list-actor-credentials [store provider]
    (let [provider (some-> provider str str/trim not-empty)]
      (if (str/blank? (str provider))
        (js/Promise.reject (js/Error. "provider is required"))
        (let [[sql-str & params] (format-sql (actor-credentials-select-query provider))]
          (-> ((:query! store) sql-str params)
              (.then (fn [result]
                       (->> (:rows result)
                            (map row->credential)
                            (remove #(str/blank? (str (:actorId %))))
                            vec))))))))

  (get-actor-credential [store actor-id provider]
    (let [actor-id (some-> actor-id str str/trim not-empty)
          provider (some-> provider str str/trim not-empty)]
      (cond
        (str/blank? (str actor-id)) (js/Promise.reject (js/Error. "actorId is required"))
        (str/blank? (str provider)) (js/Promise.reject (js/Error. "provider is required"))
        :else
        (-> (execute-one! store (actor-credential-select-query actor-id provider))
            (.then row->credential)))))

  (upsert-actor-credential! [store actor-id provider credential]
    (let [actor-id (some-> actor-id str str/trim not-empty)
          provider (some-> provider str str/trim not-empty)
          user-id (some-> (or (:user-id credential) (:userId credential)) str str/trim not-empty)
          org-id (some-> (or (:org-id credential) (:orgId credential)) str str/trim not-empty)]
      (cond
        (str/blank? (str actor-id)) (js/Promise.reject (js/Error. "actorId is required"))
        (str/blank? (str provider)) (js/Promise.reject (js/Error. "provider is required"))
        (str/blank? (str user-id)) (js/Promise.reject (js/Error. "userId is required"))
        (str/blank? (str org-id)) (js/Promise.reject (js/Error. "orgId is required"))
        :else
        (-> (execute-one! store (actor-membership-select-query {:actor-id actor-id
                                                                :user-id user-id
                                                                :org-id org-id}))
            (.then
             (fn [membership]
               (if-not membership
                 (js/Promise.reject (js/Error. "actor membership not found"))
                 (execute-one! store (actor-credential-upsert-query
                                      {:user-id user-id
                                       :org-id org-id
                                       :provider provider
                                       :kind (or (:kind credential) "credential")
                                       :account-identifier (:accountIdentifier credential)
                                       :secret-json (js/JSON.stringify (clj->js (or (:secretJson credential) {})))
                                       :status (or (:status credential) "active")})))))
            (.then row->credential)))))

  policy/ActorProjectionStore
  (sync-actor-projections! [store actors]
    (-> (js/Promise.all (clj->js (mapv #(project-actor! store %) actors)))
        (.then (fn [_] nil)))))
