(ns knoxx.backend.routes.users.admin
  (:require [clojure.string :as str]))


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
                    (when (str/blank? (str org-id))
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

  (route! app "PATCH" "/api/admin/users/:userId"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (or (aget request "params" "userId") "")
                    body (or (aget request "body") #js {})
                    org-id (or (aget body "orgId") (aget body "org_id") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (when (str/blank? (str org-id))
                      (throw (http-error 400 "org_required" "orgId is required")))
                    (ensure-org-scope! ctx org-id "org.members.update")
                    (policy-db-promise runtime reply 200 (.updateUserActor db user-id body)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PUT" "/api/admin/users/:userId/credentials/:provider"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [user-id (or (aget request "params" "userId") "")
                    provider (or (aget request "params" "provider") "")
                    body (or (aget request "body") #js {})
                    org-id (or (aget body "orgId") (aget body "org_id") "")
                    payload (.assign js/Object #js {} body (clj->js {:provider provider}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (when (str/blank? (str org-id))
                      (throw (http-error 400 "org_required" "orgId is required")))
                    (ensure-org-scope! ctx org-id "org.user_policy.update")
                    (policy-db-promise runtime reply 200 (.upsertActorCredential db user-id payload)))))
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

  nil)
