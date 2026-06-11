(ns knoxx.backend.infra.routes.users.admin
  (:require [clojure.string :as str]
            [knoxx.backend.infra.db.policy :as db-policy]))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") #js {}) :keywordize-keys true))

(defn- vec-value
  [value]
  (vec (or value [])))

(defn- param-value
  [request key]
  (or (aget request "params" key) ""))

(defn- user-payload
  [body org-id]
  {:email             (:email body)
   :display-name      (or (:displayName body) (:email body))
   :auth-provider     (or (:authProvider body) "local")
   :external-subject  (:externalSubject body)
   :status            (or (:status body) "active")
   :membership-status (or (:membershipStatus body) "active")
   :org-id            org-id
   :role-slugs        (vec-value (or (:roleSlugs body) ["knowledge_worker"]))
   :role-ids          (vec-value (:roleIds body))
   :is-default        (not= (:isDefault body) false)
   :actor-id          (:actorId body)})

(defn- actor-update-payload
  [body]
  {:org-id     (:orgId body)
   :actor-id   (:actorId body)
   :role-slugs (vec-value (:roleSlugs body))})

(defn- credential-payload
  [body provider]
  (let [secret-json (:secretJson body)]
    (when-not (and secret-json (map? secret-json) (seq secret-json))
      (throw (js/Error. "secretJson is required and must be a non-empty object")))
    {:org-id             (:orgId body)
     :provider           provider
     :kind               (or (:kind body) "credential")
     :account-identifier (:accountIdentifier body)
     :secret-json        secret-json
     :status             (or (:status body) "active")}))

(defn- membership-roles-payload
  [body]
  {:org-id     (:orgId body)
   :role-ids   (vec-value (:roleIds body))
   :role-slugs (vec-value (:roleSlugs body))
   :actor-id   (:actorId body)
   :replace    (if (contains? body :replace) (:replace body) true)})

(defn- require-org-id!
  [http-error org-id]
  (when (str/blank? (str org-id))
    (throw (http-error 400 "org_required" "orgId is required"))))

(defn- unavailable!
  [json-response! reply]
  (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))

(defn- register-admin-user-index-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-permission! ensure-org-scope! policy-db policy-db-promise http-error]}]
  (route! app "GET" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (aget request "query" "orgId")]
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
                    org-id (:orgId body)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (require-org-id! http-error org-id)
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201
                                       (db-policy/create-user-for-context!
                                        db
                                        (user-payload body org-id))))))
              (unavailable! json-response! reply)))))

(defn- register-org-user-read-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise]}]
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
                                       ((^:async fn []
                                          (await (db-policy/sync-actor-contracts-for-context! db))
                                          (db-policy/list-users!
                                           (db-policy/context-pool db)
                                           {:org-id org-id})))))))
              (unavailable! json-response! reply)))))

(defn- register-org-user-create-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise]}]
  (doseq [[method path] [["POST" "/api/admin/orgs/:orgId/users"]
                         ["POST" "/api/admin/orgs/:orgId/actors"]]]
    (route! app method path
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
                (unavailable! json-response! reply))))))

(defn- register-user-actor-update-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise http-error]}]
  (doseq [[method path] [["PATCH" "/api/admin/users/:userId"]
                         ["PATCH" "/api/admin/actors/:userId"]]]
    (route! app method path
            (fn [request reply]
              (if-let [db (policy-db runtime)]
                (let [user-id (param-value request "userId")
                      body (body-map request)
                      org-id (:orgId body)]
                  (with-request-context! runtime request reply
                    (fn [ctx]
                      (require-org-id! http-error org-id)
                      (ensure-org-scope! ctx org-id "org.members.update")
                      (policy-db-promise runtime reply 200
                                         (db-policy/update-user-actor-for-context!
                                          db
                                          user-id
                                          (actor-update-payload body))))))
                (unavailable! json-response! reply))))))

(defn- register-user-credential-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise http-error]}]
  (doseq [[method path] [["PUT" "/api/admin/users/:userId/credentials/:provider"]
                         ["PUT" "/api/admin/actors/:userId/credentials/:provider"]]]
    (route! app method path
            (fn [request reply]
              (if-let [db (policy-db runtime)]
                (let [user-id (param-value request "userId")
                      provider (param-value request "provider")
                      body (body-map request)
                      org-id (:orgId body)]
                  (with-request-context! runtime request reply
                    (fn [ctx]
                      (require-org-id! http-error org-id)
                      (ensure-org-scope! ctx org-id "org.user_policy.update")
                      (policy-db-promise runtime reply 200
                                         (db-policy/upsert-actor-credential-for-context!
                                          db
                                          user-id
                                          (credential-payload body provider))))))
                (unavailable! json-response! reply))))))

(defn- register-membership-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise http-error]}]
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
                                       ((^:async fn []
                                          (let [result (await (db-policy/get-membership! (db-policy/context-pool db) membership-id))
                                                membership (:membership result)]
                                            (when-not membership
                                              (throw (http-error 404 "membership_not_found" "membership not found")))
                                            (ensure-org-scope! ctx (:org-id membership) "org.members.update")
                                            (db-policy/set-membership-roles-for-context!
                                             db
                                             membership-id
                                             (membership-roles-payload (body-map request))))))))))
              (unavailable! json-response! reply)))))

(defn- register-membership-policy-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise http-error]}]
  (route! app "PATCH" "/api/admin/memberships/:membershipId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (param-value request "membershipId")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       ((^:async fn []
                                          (let [result (await (db-policy/get-membership! (db-policy/context-pool db) membership-id))
                                                membership (:membership result)
                                                body (body-map request)]
                                            (when-not membership
                                              (throw (http-error 404 "membership_not_found" "membership not found")))
                                            (ensure-org-scope! ctx (:org-id membership) "org.user_policy.update")
                                            (db-policy/set-membership-tool-policies!
                                             (db-policy/context-pool db)
                                             membership-id
                                             (:toolPolicies body)))))))))
              (unavailable! json-response! reply)))))

(defn register-user-admin-routes!
  [app runtime handlers]
  (register-admin-user-index-routes! app runtime handlers)
  (register-org-user-read-routes! app runtime handlers)
  (register-org-user-create-routes! app runtime handlers)
  (register-user-actor-update-routes! app runtime handlers)
  (register-user-credential-routes! app runtime handlers)
  (register-membership-routes! app runtime handlers)
  (register-membership-policy-routes! app runtime handlers)
  nil)
