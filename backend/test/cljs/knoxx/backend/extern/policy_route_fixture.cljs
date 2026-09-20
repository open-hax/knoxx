(ns knoxx.backend.extern.policy-route-fixture
  "Native Fastify registration and private Mongo seeds for preceding policy routes."
  (:require ["fastify" :default Fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.routes.auth :as auth]
            [knoxx.backend.infra.routes.users.admin :as admin]))

(defn app "Create a fixture-owned Fastify app without a public listener." []
  (Fastify #js {:logger false}))

(defn close! "Close only this fixture's Fastify app." [app] (.close app))

(defn ^:async inject!
  "Exercise the registered native route and decode only public status/body."
  [app request]
  (let [response (await (.inject app (clj->js request)))]
    {:status (.-statusCode response) :body (js->clj (.json response) :keywordize-keys true)}))

(defn register-admin!
  "Use unchanged preceding authz/HTTP helpers with the actual users/admin route registrar."
  [app]
  (admin/register-user-admin-routes!
   app nil
   {:route! (fn [app method path handler] (.route app #js {:method method :url path :handler handler}))
    :json-response! http/json-response! :with-request-context! authz/with-request-context!
    :ensure-permission! authz/ensure-permission! :ensure-org-scope! authz/ensure-org-scope!
    :policy-db authz/policy-db :policy-db-promise authz/policy-db-promise :http-error http/http-error}))

(defn register-redeem!
  "Register the actual legacy redemption route without unrelated login/session setup."
  [app context]
  (#'auth/register-invite-redeem-route! app context "http://localhost"))

(defn ^:async grant-member-update!
  "Give the seeded existing member exactly the update permission exercised by the route."
  [db]
  (await (.insertOne (.collection db "knoxx_role_permissions")
                    #js {:role_id "existing-role" :permission_code "org.members.update"})))

(defn ^:async member-role-ids
  "Inspect native role bindings after the real route/provider call."
  [db]
  (let [rows (await (.toArray (.find (.collection db "knoxx_membership_roles")
                                   #js {:membership_id "existing-member"})))]
    (mapv #(aget % "role_id") (array-seq rows))))
