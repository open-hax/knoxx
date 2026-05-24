(ns knoxx.backend.infra.routes.users.admin
  (:require [clojure.string :as str]
            [knoxx.backend.infra.db.policy :as db-policy]))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") #js {}) :keywordize-keys true))

(defn- map-value
  [m & ks]
  (some #(get m %) ks))

(defn- vec-value
  [value]
  (vec (or value [])))

(defn- query-org-id
  [request]
  (or (aget request "query" "orgId")
      (aget request "query" "org_id")))

(defn- param-value
  [request key]
  (or (aget request "params" key) ""))

(defn- body-org-id
  [body]
  (or (map-value body :org-id :orgId :org_id) ""))

(defn- tool-policies
  [body]
  (map-value body :tool-policies :toolPolicies :tool_policies))

(defn- user-payload
  [body org-id]
  {:email             (:email body)
   :display-name      (or (map-value body :display-name :displayName :display_name)
                          (:email body))
   :auth-provider     (or (map-value body :auth-provider :authProvider :auth_provider) "local")
   :external-subject  (map-value body :external-subject :externalSubject :external_subject)
   :status            (or (:status body) "active")
   :membership-status (or (map-value body :membership-status :membershipStatus :membership_status) "active")
   :org-id            org-id
   :role-slugs        (vec-value (or (map-value body :role-slugs :roleSlugs :role_slugs)
                                     ["knowledge_worker"]))
   :role-ids          (vec-value (map-value body :role-ids :roleIds :role_ids))
   :is-default        (not= (map-value body :is-default :isDefault :is_default) false)
   :actor-id          (map-value body :actor-id :actorId :actor_id)})

(defn- actor-update-payload
  [body]
  {:org-id     (body-org-id body)
   :actor-id   (map-value body :actor-id :actorId :actor_id)
   :role-slugs (vec-value (map-value body :role-slugs :roleSlugs :role_slugs))})

(defn- credential-payload
  [body provider]
  {:org-id             (body-org-id body)
   :provider           provider
   :kind               (or (:kind body) "credential")
   :account-identifier (map-value body :account-identifier :accountIdentifier :account_identifier)
   :secret-json        (or (map-value body :secret-json :secretJson :secret_json) {})
   :status             (or (:status body) "active")})

(defn- membership-roles-payload
  [body]
  {:org-id     (body-org-id body)
   :role-ids   (vec-value (map-value body :role-ids :roleIds :role_ids))
   :role-slugs (vec-value (map-value body :role-slugs :roleSlugs :role_slugs))
   :actor-id   (map-value body :actor-id :actorId :actor_id)
   :replace    (if (contains? body :replace) (:replace body) true)})

(defn- require-org-id!
  [http-error org-id]
  (when (str/blank? (str org-id))
    (throw (http-error 400 "org_required" "orgId is required"))))

(defn- unavailable!
  [json-response! reply]
  (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))

(defn register-user-admin-routes!
  [app runtime {:keys [route!
                       json-response!
                       with-request-context!
                       ensure-permission!
                       ensure-org-scope!
                       policy-db
                       policy-db-promise
                       http-error]}]
  (route! app "GET" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (query-org-id request)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (if org-id
                      (ensure-org-scope! ctx org-id "org.users.read")
                      (ensure-permission! ctx "platform.org.read"))
                    (policy-db-promise runtime reply 200
                                       (db-policy/list-users! (db-policy/context-pool db)
                                                              {:org-id org-id})))))
              (unavailable! json-response! reply))))

  (route! app "POST" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [body (body-map request)
                    org-id (body-org-id body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201
                                       (db-policy/create-user-for-context!
                                        db
                                        (user-payload body org-id))))))
              (unavailable! json-response! reply))))

  (route! app "GET" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (param-value request "orgId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.read")
                    (policy-db-promise runtime reply 200
                                       (db-policy/list-users! (db-policy/context-pool db)
                                                              {:org-id org-id})))))
              (unavailable! json-response! reply))))

  (route! app "GET" "/api/admin/orgs/:orgId/actors"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (param-value request "orgId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.read")
                    (policy-db-promise runtime reply 200
                                       (-> (db-policy/sync-actor-contracts-for-context! db)
                                           (.then (fn [_]
                                                    (db-policy/list-users!
                                                     (db-policy/context-pool db)
                                                     {:org-id org-id}))))))))
              (unavailable! json-response! reply))))

  (route! app "POST" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (param-value request "orgId")
                    body (body-map request)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201
                                       (db-policy/create-user-for-context!
                                        db
                                        (user-payload body org-id))))))
              (unavailable! json-response! reply))))

  (route! app "POST" "/api/admin/orgs/:orgId/actors"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (param-value request "orgId")
                    body (body-map request)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201
                                       (db-policy/create-user-for-context!
                                        db
                                        (user-payload body org-id))))))
              (unavailable! json-response! reply))))

  (route! app "PATCH" "/api/admin/users/:userId"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (param-value request "userId")
                    body (body-map request)
                    org-id (body-org-id body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.members.update")
                    (policy-db-promise runtime reply 200
                                       (db-policy/update-user-actor-for-context!
                                        db
                                        user-id
                                        (actor-update-payload body))))))
              (unavailable! json-response! reply))))

  (route! app "PATCH" "/api/admin/actors/:userId"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (param-value request "userId")
                    body (body-map request)
                    org-id (body-org-id body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.members.update")
                    (policy-db-promise runtime reply 200
                                       (db-policy/update-user-actor-for-context!
                                        db
                                        user-id
                                        (actor-update-payload body))))))
              (unavailable! json-response! reply))))

  (route! app "PUT" "/api/admin/users/:userId/credentials/:provider"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (param-value request "userId")
                    provider (param-value request "provider")
                    body (body-map request)
                    org-id (body-org-id body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.user_policy.update")
                    (policy-db-promise runtime reply 200
                                       (db-policy/upsert-actor-credential-for-context!
                                        db
                                        user-id
                                        (credential-payload body provider))))))
              (unavailable! json-response! reply))))

  (route! app "PUT" "/api/admin/actors/:userId/credentials/:provider"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (param-value request "userId")
                    provider (param-value request "provider")
                    body (body-map request)
                    org-id (body-org-id body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.user_policy.update")
                    (policy-db-promise runtime reply 200
                                       (db-policy/upsert-actor-credential-for-context!
                                        db
                                        user-id
                                        (credential-payload body provider))))))
              (unavailable! json-response! reply))))

  (route! app "GET" "/api/admin/orgs/:orgId/memberships"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (param-value request "orgId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.members.read")
                    (policy-db-promise runtime reply 200
                                       (db-policy/list-memberships! (db-policy/context-pool db)
                                                                    {:org-id org-id})))))
              (unavailable! json-response! reply))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (param-value request "membershipId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (db-policy/get-membership! (db-policy/context-pool db) membership-id)
                                           (.then (fn [result]
                                                    (let [membership (:membership result)]
                                                      (when-not membership
                                                        (throw (http-error 404 "membership_not_found" "membership not found")))
                                                      (ensure-org-scope! ctx (:org-id membership) "org.members.update")
                                                      (db-policy/set-membership-roles-for-context!
                                                       db
                                                       membership-id
                                                       (membership-roles-payload (body-map request)))))))))))
              (unavailable! json-response! reply))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (param-value request "membershipId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (db-policy/get-membership! (db-policy/context-pool db) membership-id)
                                           (.then (fn [result]
                                                    (let [membership (:membership result)
                                                          body (body-map request)]
                                                      (when-not membership
                                                        (throw (http-error 404 "membership_not_found" "membership not found")))
                                                      (ensure-org-scope! ctx (:org-id membership) "org.user_policy.update")
                                                      (db-policy/set-membership-tool-policies!
                                                       (db-policy/context-pool db)
                                                       membership-id
                                                       (tool-policies body))))))))))
              (unavailable! json-response! reply))))

  nil)
