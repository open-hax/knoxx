(ns knoxx.backend.infra.routes.admin
  (:require [knoxx.backend.infra.db.policy :as db-policy]
            [knoxx.backend.infra.routes.users.admin :as users-admin]))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") #js {}) :keywordize-keys true))


(defn- register-admin-bootstrap-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-permission! ensure-any-permission! policy-db policy-db-promise]}]
  (route! app "GET" "/api/admin/bootstrap"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (db-policy/bootstrap-context! db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "GET" "/api/admin/permissions"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.roles.read"] "permission_denied" "Role permission metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (db-policy/list-permissions! (db-policy/context-pool db)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "GET" "/api/admin/tools"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.tool_policy.read" "org.user_policy.read"] "permission_denied" "Tool policy metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (db-policy/list-tools! (db-policy/context-pool db)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

(defn- register-admin-org-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-permission! policy-db policy-db-promise]}]
  (route! app "GET" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (db-policy/list-orgs! (db-policy/context-pool db)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "POST" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.create")
                  (policy-db-promise runtime reply 201 (db-policy/create-org-for-context! db (body-map request)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

(defn- register-admin-role-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise http-error]}]
  (route! app "GET" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.read")
                    (policy-db-promise runtime reply 200 (db-policy/list-roles! (db-policy/context-pool db) {:org-id org-id})))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "POST" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    payload (assoc (body-map request) :org-id org-id)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.create")
                    (policy-db-promise runtime reply 201 (db-policy/create-role-for-context! db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "PATCH" "/api/admin/roles/:roleId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [role-id (or (aget request "params" "roleId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (db-policy/get-role! (db-policy/context-pool db) role-id)
                                           (.then (fn [result]
                                                    (let [role (:role result)]
                                                      (when-not role
                                                        (throw (http-error 404 "role_not_found" "role not found")))
                                                      (ensure-org-scope! ctx (:org-id role) "org.tool_policy.update")
                                                      (db-policy/set-role-tool-policies!
                                                       (db-policy/context-pool db)
                                                       role-id
                                                       (let [body (body-map request)]
                                                         (or (:tool-policies body)
                                                             (:toolPolicies body)
                                                             (:tool_policies body))))))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

(defn- register-admin-data-lake-routes!
  [app runtime {:keys [route! json-response! with-request-context! ensure-org-scope! policy-db policy-db-promise]}]
  (route! app "GET" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.read")
                    (policy-db-promise runtime reply 200 (db-policy/list-data-lakes! (db-policy/context-pool db) {:org-id org-id})))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  (route! app "POST" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    payload (assoc (body-map request) :org-id org-id)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.create")
                    (policy-db-promise runtime reply 201 (db-policy/create-data-lake-for-context! db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

(defn register-admin-routes!
  [app runtime deps]
  (register-admin-bootstrap-routes! app runtime deps)
  (register-admin-org-routes! app runtime deps)
  (users-admin/register-user-admin-routes! app runtime deps)
  (register-admin-role-routes! app runtime deps)
  (register-admin-data-lake-routes! app runtime deps)
  nil)
