(ns knoxx.backend.infra.db.policy.bootstrap
  "Initialize explicit Mongo policy state and reconcile bootstrap credentials."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.password :as password]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as mongo-actor-creds]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]
            [knoxx.backend.infra.db.policy.projection :as policy-projection]
            [knoxx.backend.infra.db.policy.sessions :as policy-sessions]))

(declare ensure-bootstrap-allowlist-users!)

(defn ^:async ensure-primary-org!
  [_pool opts]
  (let [primary-org-slug (or (:primaryOrgSlug opts) (:primary-org-slug opts) "open-hax")
        primary-org-name (or (:primaryOrgName opts) (:primary-org-name opts) "Open Hax")
        primary-org-kind (or (:primaryOrgKind opts) (:primary-org-kind opts) "platform_owner")
        slug (values/slugify primary-org-slug "open-hax")]
    (await (mongo-directory/ensure-primary-org! (await (policy-connection/db!))
                                                {:slug slug
                                                 :name (str primary-org-name)
                                                 :kind (str primary-org-kind)}))))

(defn ^:async ensure-bootstrap-user!
  [pool primary-org opts]
  (let [db (await (policy-connection/db!))
        email (str/lower-case (str (or (:bootstrapSystemAdminEmail opts)
                                       (:bootstrap-system-admin-email opts)
                                       "system-admin@open-hax.local")))
        dn (str (or (:bootstrapSystemAdminName opts)
                    (:bootstrap-system-admin-name opts)
                    "Knoxx System Admin"))
        user (await (mongo-directory/create-user! db {:email email :display-name dn
                                                      :auth-provider "bootstrap"
                                                      :external-subject nil :status "active"}))
        membership (await (mongo-directory/upsert-membership! db {:user-id (:id user)
                                                                  :org-id (:id primary-org)
                                                                  :status "active"
                                                                  :is-default true}))]
    (await (policy-roles/set-membership-roles! pool (:id membership) {:org-id (:id primary-org)
                                                         :role-slugs ["system-admin"]
                                                         :replace true}))
    (await (policy-roles/set-membership-actor-id! pool (:id membership) "system_admin"))
    {:user user :membership membership}))

(defn ^:async ensure-bootstrap-local-password!
  "Idempotently project the environment-owned bootstrap password into the
   local credential store. Blank passwords revoke any previously provisioned
   local bootstrap credential."
  ([db primary-org bootstrap opts]
   (ensure-bootstrap-local-password!
    db primary-org bootstrap opts
    {:encode-password password/hash-password
     :reconcile-bootstrap-credential!
     mongo-actor-creds/reconcile-bootstrap-local-password!}))
  ([db primary-org bootstrap opts
    {:keys [encode-password reconcile-bootstrap-credential!]}]
   (let [configured-password (some-> (or (:bootstrapSystemAdminPassword opts)
                                         (:bootstrap-system-admin-password opts))
                                     str not-empty)
         user-id (get-in bootstrap [:user :id])
         user-email (get-in bootstrap [:user :email])
         org-id (:id primary-org)
         previous-emails (values/previous-bootstrap-system-admin-emails opts user-email)]
     (await (reconcile-bootstrap-credential!
             db {:user-id user-id
                 :org-id org-id
                 :account-identifier user-email
                 :previous-account-identifiers previous-emails
                 :secret-json (when configured-password
                                (assoc (encode-password configured-password)
                                       :bootstrap-system-admin true))}))
     nil)))

(defn get-bootstrap-context!
  [_pool primary-org bootstrap]
  (js/Promise.resolve
   {"primaryOrg"    {"id"        (:id primary-org)
                     "slug"      (:slug primary-org)
                     "name"      (:name primary-org)
                     "kind"      (:kind primary-org)
                     "isPrimary" (:is_primary primary-org)
                     "status"    (:status primary-org)}
    "bootstrapUser" {"id"           (get-in bootstrap [:user :id])
                     "email"        (get-in bootstrap [:user :email])
                     "displayName"  (get-in bootstrap [:user :display_name])
                     "membershipId" (get-in bootstrap [:membership :id])}}))

(defn ^:async allowlist-best-effort! [pool primary-org opts]
  (when (seq (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts)))
    (try
      (await (ensure-bootstrap-allowlist-users! pool primary-org opts))
      (catch :default err
        (.warn js/console "[policy-db] allowlist failed:" (.-message err))))))

(defn ^:async sync-actor-contracts-best-effort! [pool primary-org]
  (try
    (await (policy-projection/sync-actor-contracts! pool primary-org))
    (catch :default err
      (.warn js/console "[policy-db] actor sync failed:" (.-message err)))))

(defn ^:async cleanup-expired-sessions-best-effort! [pool]
  (try
    (await (policy-sessions/cleanup-expired-sessions! pool))
    (catch :default _ nil)))

(defn- policy-context-map [primary-org bootstrap]
  {:pool nil
   :mongo? true
   :primary-org primary-org
   :bootstrap bootstrap
   :bootstrap-user-id (get-in bootstrap [:user :id])
   :bootstrap-membership-id (get-in bootstrap [:membership :id])})

(defn ^:async initialise-policy-db!
  "Mongo-backed initialisation: connect + ensure twin indexes (guarded so a
   bad spec never crash-loops startup), seed the primary org + contract role
   projections + bootstrap user, run best-effort allowlist/actor-sync/cleanup,
   then return the policy context map."
  [opts]
  (let [db (await (policy-connection/ensure-mongo-policy-db!))]
    (when-not db
      (throw (js/Error. "Mongo policy store unavailable")))
    (await (mongo-client/require-transaction-capable-topology! db))
    (let [primary-org (await (ensure-primary-org! nil opts))]
      (await (policy-roles/sync-contract-role-projections! nil))
      (let [bootstrap (await (ensure-bootstrap-user! nil primary-org opts))]
        (await (ensure-bootstrap-local-password! db primary-org bootstrap opts))
        (await (allowlist-best-effort! nil primary-org opts))
        (await (sync-actor-contracts-best-effort! nil primary-org))
        (await (cleanup-expired-sessions-best-effort! nil))
        (policy-context-map primary-org bootstrap)))))

(defn ^:async ensure-bootstrap-allowlist-role! [pool org-id membership-id slug]
  (let [db (await (policy-connection/db!))
        role (or (await (policy-roles/find-role pool {:slug slug :org-id org-id}))
                 (await (policy-roles/find-role pool {:slug slug :org-id nil})))]
    (when role
      ;; replace? false: additive link, mirroring the PG insert-membership-role
      ;; (ON CONFLICT DO NOTHING) used during allowlist bootstrap.
      (await (mongo-roles/set-membership-roles! db membership-id false [(:id role)])))))

(defn ^:async ensure-bootstrap-allowlist-user! [pool org-id role-slugs email]
  (let [db (await (policy-connection/db!))
        user (await (mongo-directory/create-user! db {:email email :display-name email
                                                      :auth-provider "bootstrap"
                                                      :external-subject nil :status "active"}))
        ms (await (mongo-directory/upsert-membership! db {:user-id (:id user) :org-id org-id
                                                          :status "active" :is-default false}))]
    (await (policy-support/promise-each role-slugs
                         (partial ensure-bootstrap-allowlist-role! pool org-id (:id ms))))
    (await (policy-roles/set-membership-actor-id!
            pool (:id ms) (policy-support/default-membership-actor-id role-slugs)))))

(defn ^:async ensure-bootstrap-allowlist-users! [pool primary-org opts]
  (let [emails (values/bootstrap-allowlist-emails opts)
        role-slugs (values/bootstrap-allowlist-role-slugs opts)
        org-id (:id primary-org)]
    (when (seq emails)
      (await (js/Promise.all
              (into-array
               (mapv (partial ensure-bootstrap-allowlist-user! pool org-id role-slugs)
                     emails)))))))
