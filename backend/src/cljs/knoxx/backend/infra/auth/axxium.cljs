(ns knoxx.backend.infra.auth.axxium
  "Authenticate with this host's Axxium and persist an independent Knoxx identity binding."
  (:require [knoxx.backend.domain.auth.axxium :as identity]
            [knoxx.backend.extern.axxium-authority :as authority]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.extern.mongo :as mongo]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.stores.mongo-policy-directory :as directory]
            [knoxx.backend.infra.system-instance :as system-instance]
            [knoxx.backend.law.axxium-identity :as law]))
(defn enabled? "Whether this instance delegates password authentication to Axxium." []
  (boolean (authority/configured-origin)))
(defn ^:async authenticate-with!
  "Verify credentials and revoke Axxium's transient session through a supplied client."
  [origin request! email password]
  (let [_ (when-not origin (throw (ex-info "Axxium authentication is not configured" {:status 503})))
        response (await (request!
                         {:url (str origin "/api/auth/login") :method "POST" :redirect "error"
                          :timeout-ms 10000 :json {:email email :password password}}))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Axxium sign-in failed" {:status (if (= 401 (:status response)) 401 503)})))
    ;; Axxium issues a session during password verification. Knoxx keeps only
    ;; the verified actor and revokes that transient provider session before
    ;; creating its own independent session.
    (let [token (get-in response [:body :token])]
      (when-not (and (string? token) (seq token))
        (throw (ex-info "Axxium did not return a revocable session" {:status 503})))
      (let [revoke (await (request!
                           {:url (str origin "/api/auth/logout") :method "POST"
                            :redirect "error" :timeout-ms 10000
                            :headers {"Authorization" (str "Bearer " token)}}))]
        (when-not (= 200 (:status revoke))
          (throw (ex-info "Axxium verification session could not be revoked" {:status 503}))))
      {:issuer origin :actor (law/require-actor! (get-in response [:body :actor]))})))
(defn ^:async authenticate!
  "Authenticate only against the configured HTTPS origin; never follow redirects."
  [email password]
  (await (authenticate-with! (authority/configured-origin)
                             #(fetch/json! fetch/default-client %)
                             email password)))
(defn- ^:async claim-user! [issuer actor]
  (let [db (await (policy/db!))
        users (mongo/collection db directory/USERS_COLLECTION)
        _ (await (mongo/ensure-index! users [[:email 1]] {:unique true}))
        _ (await (mongo/ensure-index! users [[:auth_provider 1] [:external_subject 1]]
                      {:unique true :partialFilterExpression {:auth_provider "axxium"}}))
        _ (await (mongo/insert-one-unique! users
                   (identity/local-user issuer actor (str (random-uuid)) (system-instance/current-id))))
        user (await (directory/find-user-by-email! db (:email actor)))
        _ (when-not (law/same-binding? user (identity/subject issuer actor))
            (throw (ex-info "This email is already bound to a different local identity" {:status 409})))]
    {:db db :user user}))

(defn ^:async context! "Claim an exact authority/subject binding without overwriting accounts or roles." [policy-context {:keys [issuer actor]}]
  (let [actor (identity/normalize-actor actor)
        {:keys [db user]} (await (claim-user! issuer actor))
        existing (await (directory/find-membership-row-by-email-and-org!
                         db {:user-email (:email actor) :active-only true}))]
    (when-not existing
      (let [members (await (mongo/find-docs! (mongo/collection db directory/MEMBERSHIPS_COLLECTION)
                            {:user_id (:id user) :limit 1}))]
        (when (seq members)
          (throw (ex-info "This local membership is inactive" {:status 403}))))
    (let [org (when-not existing
                (await (policy/ensure-self-org! (policy/context-pool policy-context) (:email actor) (:display_name actor))))]
      (when org
        (await (policy/create-user-for-context! policy-context
                 {:email (:email actor) :display-name (:display_name actor) :org-id (:id org)
                  :role-slugs ["basic-user"] :auth-provider "axxium"
                  :external-subject (identity/subject issuer actor) :actor-id (:id actor)
                  :status "active" :membership-status "active" :is-default true})))
      (await (policy/resolve-context! policy-context
               (cond-> {"x-knoxx-user-email" (:email actor)}
                 existing (assoc "x-knoxx-membership-id" (:id existing))
                 org (assoc "x-knoxx-org-slug" (:slug org))))))))
)
