(ns knoxx.backend.admin-routes
  (:require [clojure.string]))

(defn register-admin-routes!
  [app runtime {:keys [route!
                       json-response!
                       with-request-context!
                       ensure-permission!
                       ensure-any-permission!
                       ensure-org-scope!
                       policy-db
                       policy-db-promise
                       http-error]}]
  (route! app "GET" "/api/admin/bootstrap"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (.getBootstrapContext db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/permissions"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.roles.read"] "permission_denied" "Role permission metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (.listPermissions db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/tools"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.tool_policy.read" "org.user_policy.read"] "permission_denied" "Tool policy metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (.listTools db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (.listOrgs db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.create")
                  (policy-db-promise runtime reply 201 (.createOrg db (or (aget request "body") #js {})))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "query" "orgId")
                               (aget request "query" "org_id")
                               nil)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (if org-id
                      (ensure-org-scope! ctx org-id "org.users.read")
                      (ensure-permission! ctx "platform.org.read"))
                    (policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [body (or (aget request "body") #js {})
                    org-id (or (aget body "orgId") (aget body "org_id") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (when (clojure.string/blank? (str org-id))
                      (throw (http-error 400 "org_required" "orgId is required")))
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201 (.createUser db body)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.read")
                    (policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201 (.createUser db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/memberships"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.members.read")
                    (policy-db-promise runtime reply 200 (.listMemberships db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (or (aget request "params" "membershipId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (.getMembership db membership-id)
                                           (.then (fn [result]
                                                    (let [membership (js->clj (aget result "membership") :keywordize-keys true)]
                                                      (when-not membership
                                                        (throw (http-error 404 "membership_not_found" "membership not found")))
                                                      (ensure-org-scope! ctx (:orgId membership) "org.members.update")
                                                      (.setMembershipRoles db membership-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (or (aget request "params" "membershipId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (.getMembership db membership-id)
                                           (.then (fn [result]
                                                    (let [membership (js->clj (aget result "membership") :keywordize-keys true)]
                                                      (when-not membership
                                                        (throw (http-error 404 "membership_not_found" "membership not found")))
                                                      (ensure-org-scope! ctx (:orgId membership) "org.user_policy.update")
                                                      (.setMembershipToolPolicies db membership-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

  (route! app "GET" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.read")
                    (policy-db-promise runtime reply 200 (.listRoles db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.create")
                    (policy-db-promise runtime reply 201 (.createRole db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PATCH" "/api/admin/roles/:roleId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [role-id (or (aget request "params" "roleId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise runtime reply 200
                                       (-> (.getRole db role-id)
                                           (.then (fn [result]
                                                    (let [role (js->clj (aget result "role") :keywordize-keys true)]
                                                      (when-not role
                                                        (throw (http-error 404 "role_not_found" "role not found")))
                                                      (ensure-org-scope! ctx (:orgId role) "org.tool_policy.update")
                                                      (.setRoleToolPolicies db role-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"})))))

  (route! app "GET" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.read")
                    (policy-db-promise runtime reply 200 (.listDataLakes db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.create")
                    (policy-db-promise runtime reply 201 (.createDataLake db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))
  nil)
