(ns knoxx.backend.infra.db.policy
  "Public policy API, verified identity binding checks and provider-context adapters."
  (:require [knoxx.backend.infra.local-policy-api :as local-api]
            [knoxx.backend.infra.identity-bindings :as identity-bindings]
            [knoxx.backend.law.identity-binding :as identity-law]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]
            [knoxx.backend.infra.db.policy.hydration :as policy-hydration]
            [knoxx.backend.infra.db.policy.projection :as policy-projection]
            [knoxx.backend.infra.db.policy.bootstrap :as policy-bootstrap]
            [knoxx.backend.infra.db.policy.sessions :as policy-sessions]
            [knoxx.backend.infra.db.policy.credentials :as policy-credentials]
            [knoxx.backend.infra.db.policy.role-queries :as policy-role-queries]
            [knoxx.backend.infra.db.policy.organizations :as policy-organizations]
            [knoxx.backend.infra.db.policy.users :as policy-users]
            [knoxx.backend.infra.db.policy.invites :as policy-invites]))

(def promise-each policy-support/promise-each)

(def upsert-actor-contract-best-effort! policy-support/upsert-actor-contract-best-effort!)

(def db! policy-connection/db!)

(def find-org-by-id policy-roles/find-org-by-id)

(def find-org-by-slug policy-roles/find-org-by-slug)

(def find-role policy-roles/find-role)

(def ensure-role! policy-roles/ensure-role!)

(def set-role-tool-policies! policy-roles/set-role-tool-policies!)

(def resolve-role-ids policy-roles/resolve-role-ids)

(def canonicalize-contract-role-slugs! policy-roles/canonicalize-contract-role-slugs!)

(def resolved-membership-role-slugs! policy-roles/resolved-membership-role-slugs!)

(def set-membership-roles! policy-roles/set-membership-roles!)

(def set-membership-tool-policies! policy-roles/set-membership-tool-policies!)

(def set-membership-actor-id! policy-roles/set-membership-actor-id!)

(def hydrate-role-maps policy-hydration/hydrate-role-maps)

(def hydrate-memberships policy-hydration/hydrate-memberships)

(def detailed-membership-roles policy-hydration/detailed-membership-roles)

(def build-request-context policy-hydration/build-request-context)

(def project-actor-via-store! policy-projection/project-actor-via-store!)

(def sync-actor-projections! policy-projection/sync-actor-projections!)

(def sync-contract-role-record! policy-roles/sync-contract-role-record!)

(def sync-contract-role-projections! policy-roles/sync-contract-role-projections!)

(def ensure-primary-org! policy-bootstrap/ensure-primary-org!)

(def ensure-bootstrap-user! policy-bootstrap/ensure-bootstrap-user!)

(def ensure-bootstrap-local-password! policy-bootstrap/ensure-bootstrap-local-password!)

(def touch-session-best-effort! policy-sessions/touch-session-best-effort!)

(def resolve-request-context! policy-hydration/resolve-request-context!)

(def evaluate-tool-access! policy-hydration/evaluate-tool-access!)

(def list-actor-credentials! policy-credentials/list-actor-credentials!)

(def list-permissions! policy-role-queries/list-permissions!)

(def list-tools! policy-role-queries/list-tools!)

(def get-bootstrap-context! policy-bootstrap/get-bootstrap-context!)

(def org-data-lake-counts policy-organizations/org-data-lake-counts)

(def list-orgs! policy-organizations/list-orgs!)

(def create-org! policy-organizations/create-org!)

(def self-org-slug values/self-org-slug)

(def ensure-self-org! policy-organizations/ensure-self-org!)

(def list-roles! policy-role-queries/list-roles!)

(def get-role! policy-role-queries/get-role!)

(def create-role! policy-role-queries/create-role!)

(def list-users! policy-users/list-users!)

(def upsert-user-actor-contract-for-membership! policy-users/upsert-user-actor-contract-for-membership!)

(def create-user! policy-users/create-user!)

(def local-password-auth-record! policy-credentials/local-password-auth-record!)

(def list-memberships! policy-users/list-memberships!)

(def get-membership! policy-users/get-membership!)

(def set-membership-roles-public! policy-users/set-membership-roles-public!)

(def list-data-lakes! policy-organizations/list-data-lakes!)

(def create-data-lake! policy-organizations/create-data-lake!)

(def create-session! policy-sessions/create-session!)

(def get-session-by-token! policy-sessions/get-session-by-token!)

(def delete-session-by-token! policy-sessions/delete-session-by-token!)

(def cleanup-expired-sessions! policy-sessions/cleanup-expired-sessions!)

(def create-invite! policy-invites/create-invite!)

(def redeem-invite! policy-invites/redeem-invite!)

(def list-invites! policy-invites/list-invites!)

(def sync-actor-contracts! policy-projection/sync-actor-contracts!)

(def sync-user-from-actor-contract! policy-projection/sync-user-from-actor-contract!)

(def recover-session-secret! policy-sessions/recover-session-secret!)

(def update-user-actor! policy-users/update-user-actor!)

(def upsert-actor-credential! policy-credentials/upsert-actor-credential!)

(def get-actor-credential! policy-credentials/get-actor-credential!)

(def allowlist-best-effort! policy-bootstrap/allowlist-best-effort!)

(def sync-actor-contracts-best-effort! policy-bootstrap/sync-actor-contracts-best-effort!)

(def cleanup-expired-sessions-best-effort! policy-bootstrap/cleanup-expired-sessions-best-effort!)

(def initialise-policy-db! policy-bootstrap/initialise-policy-db!)

(def ensure-bootstrap-allowlist-role! policy-bootstrap/ensure-bootstrap-allowlist-role!)

(def ensure-bootstrap-allowlist-user! policy-bootstrap/ensure-bootstrap-allowlist-user!)

(def ensure-bootstrap-allowlist-users! policy-bootstrap/ensure-bootstrap-allowlist-users!)

(defn- assert-bound-row! [binding row]
  (when-not (and row (every? #(= "active" (get row %)) [:status :user_status :org_status]))
    (throw (ex-info "Bound membership, user or organization is inactive"
                    {:status 403 :code "identity_policy_inactive"})))
  (when-not (= [(:membership-id binding) (:user-id binding) (:org-id binding) (:actor-id binding)]
               [(:id row) (:user_id row) (:org_id row) (:actor_id row)])
    (throw (ex-info "Policy membership no longer matches the verified identity"
                    {:status 403 :code "identity_binding_conflict"})))
  row)

(defn- ^:async current-bound-row! [policy-context binding]
  (let [stored (await (identity-bindings/read! (:identity-bindings policy-context) (:principal-id binding)))
        row (await (mongo-directory/find-membership-row-with-user-org!
                    (await (db!)) (:membership-id binding)))]
    (when-not (= stored binding)
      (throw (ex-info "The stable identity binding has changed"
                      {:status 403 :code "identity_binding_conflict"})))
    (assert-bound-row! binding row)))

(defn ^:async resolve-bound-context!
  "Resolve Mongo roles only through a durable identity binding and recheck asynchronous drift."
  [policy-context binding]
  (identity-law/assert-binding! binding)
  (let [pool (:pool policy-context)
        before (await (current-bound-row! policy-context binding))
        first-context (await (build-request-context pool before))
        middle (await (current-bound-row! policy-context binding))
        final-context (await (build-request-context pool middle))
        after (await (current-bound-row! policy-context binding))]
    (when-not (and (= before middle after) (= first-context final-context))
      (throw (ex-info "Policy changed during identity authorization; retry the request"
                      {:status 409 :code "identity_policy_changed"})))
    final-context))

(defn acting-context
  "Carry the authenticated caller rather than granting the server bootstrap identity."
  [policy-context verified-context]
  (local-api/acting-context policy-context verified-context))

(defn context-pool
  "Keep the selected finite provider intact through legacy first-argument call sites."
  [policy-context]
  (if (local-api/selected? policy-context) policy-context (:pool policy-context)))

(defn configured? [policy-context] (boolean (or (local-api/selected? policy-context) (:mongo? policy-context) (:query! policy-context))))

(defn context-primary-org [policy-context] (:primary-org policy-context))

(defn context-bootstrap [policy-context] (:bootstrap policy-context))

(defn context-actor-user-id [policy-context]
  (if (:acting-context policy-context) (get-in policy-context [:acting-context :user :id])
      (when-not (local-api/selected? policy-context) (:bootstrap-user-id policy-context))))

(defn context-actor-membership-id [policy-context]
  (if (:acting-context policy-context) (get-in policy-context [:acting-context :membership :id])
      (when-not (local-api/selected? policy-context) (:bootstrap-membership-id policy-context))))

(defn close!
  "No-op for the Mongo policy store: the shared Mongo client is owned by
   infra.mongo-client and closed by the global shutdown path, not per
   policy-context. Retained so graceful-shutdown's call site is unchanged."
  [_policy-context]
  (js/Promise.resolve nil))

(defn query!
  "Deprecated raw-SQL entrypoint. The Mongo policy store no longer executes
   SQL, so a Mongo policy-context resolves nil here and legacy callers degrade
   to their empty-result fallbacks. A context that injects its own :query! fn
   (e.g. mailbox routes wiring a custom executor) still has it honored. Use the
   named policy DB functions instead."
  [policy-context sql-str params]
  (if-let [f (:query! policy-context)]
    (f sql-str params)
    (js/Promise.resolve nil)))

(defn bootstrap-context!
  [policy-context]
  (if (local-api/selected? policy-context)
    (local-api/bootstrap! policy-context)
    (if-let [f (:bootstrap-context! policy-context)]
    (f)
    (get-bootstrap-context! (context-pool policy-context)
                            (context-primary-org policy-context)
                            (context-bootstrap policy-context)))))

(defn resolve-context!
  [policy-context headers-like]
  (if (local-api/selected? policy-context)
    (local-api/unavailable! :header-authentication)
    (if-let [f (:resolve-context! policy-context)]
    (f headers-like)
    (resolve-request-context! (context-pool policy-context) headers-like))))

(defn sync-actor-contracts-for-context!
  [policy-context]
  (if (local-api/selected? policy-context)
    {:synced? false :provider :edn}
    (if-let [f (:sync-actor-contracts! policy-context)]
    (f)
    (sync-actor-contracts! (context-pool policy-context)
                           (context-primary-org policy-context)))))

(defn sync-user-from-actor-contract-for-context!
  [policy-context opts]
  (if (local-api/selected? policy-context)
    (local-api/unavailable! :contract-identity-projection)
    (if-let [f (:sync-user-from-actor-contract! policy-context)]
    (f opts)
    (sync-user-from-actor-contract! (context-pool policy-context)
                                    (context-primary-org policy-context)
                                    opts))))

(defn create-user-for-context!
  [policy-context payload]
  (create-user! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn local-password-auth-record-for-context!
  [policy-context email]
  (if (local-api/selected? policy-context)
    (local-api/unavailable! :password)
    (local-password-auth-record! (context-pool policy-context) email)))

(defn create-invite-for-context!
  [policy-context payload]
  (if (local-api/selected? policy-context)
    (local-api/unavailable! :invite)
    (create-invite! (context-pool policy-context)
                  (context-actor-user-id policy-context)
                  (context-actor-membership-id policy-context)
                  payload)))

(defn create-org-for-context!
  [policy-context payload]
  (create-org! (context-pool policy-context)
               (context-actor-user-id policy-context)
               (context-actor-membership-id policy-context)
               payload))

(defn create-role-for-context!
  [policy-context payload]
  (create-role! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn create-data-lake-for-context!
  [policy-context payload]
  (create-data-lake! (context-pool policy-context)
                     (context-actor-user-id policy-context)
                     (context-actor-membership-id policy-context)
                     payload))

(defn set-membership-roles-for-context!
  [policy-context membership-id payload]
  (set-membership-roles-public! (context-pool policy-context)
                                (context-actor-user-id policy-context)
                                (context-actor-membership-id policy-context)
                                membership-id
                                payload))

(defn update-user-actor-for-context!
  [policy-context user-id payload]
  (update-user-actor! (context-pool policy-context)
                      (context-actor-user-id policy-context)
                      (context-actor-membership-id policy-context)
                      user-id
                      payload))

(defn upsert-actor-credential-for-context!
  [policy-context user-id payload]
  (upsert-actor-credential! (context-pool policy-context)
                            (context-actor-user-id policy-context)
                            (context-actor-membership-id policy-context)
                            user-id
                            payload))

(defn ^:async create-policy-db
  "Initialise the Mongo-backed policy DB and return a CLJS policy context.

   Security-critical initialization failures propagate. Bootstrap must never
   compose protected routes around a nil policy context."
  [options]
  (let [opts (if (map? options)
               options
               (js->clj options :keywordize-keys true))]
    (await (initialise-policy-db! opts))))
