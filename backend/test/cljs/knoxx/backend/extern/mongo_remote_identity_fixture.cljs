(ns knoxx.backend.extern.mongo-remote-identity-fixture
  "Seed and inspect only the native identity proof's private Mongo database."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]))

(def ^:private collections
  ["knoxx_users" "knoxx_orgs" "knoxx_memberships" "knoxx_roles"
   "knoxx_membership_roles" "knoxx_role_permissions"])

(defn ^:async seed!
  "Seed an existing account, immutable membership and local role without policy upserts."
  [db {:keys [issuer auth-provider user-status member-status org-status]
       :or {auth-provider "axxium" user-status "active" member-status "active" org-status "active"}}]
  (doseq [name collections] (await (.deleteMany (.collection db name) #js {})))
  (let [now (js/Date.)
        documents
        [["knoxx_users" {:user_id "existing-user" :email "remote@example.test"
                         :display_name "Existing local profile" :auth_provider auth-provider
                         :external_subject (str issuer "#actor_remote123") :status user-status
                         :created_at now :updated_at now}]
         ["knoxx_orgs" {:org_id "existing-org" :slug "existing-org" :name "Existing workspace"
                        :kind "customer" :status org-status :is_primary true :created_at now :updated_at now}]
         ["knoxx_memberships" {:membership_id "existing-member" :user_id "existing-user" :org_id "existing-org"
                               :actor_id "existing-local-actor" :status member-status :is_default true
                               :created_at now :updated_at now}]
         ["knoxx_roles" {:role_id "existing-role" :slug "local-reviewer" :name "Local reviewer"
                         :org_id "existing-org" :scope_kind "org" :created_at now :updated_at now}]
         ["knoxx_membership_roles" {:membership_id "existing-member" :role_id "existing-role"}]
         ["knoxx_role_permissions" {:role_id "existing-role" :permission_code "org.publications.read"}]]]
    (doseq [[name document] documents]
      (await (.insertOne (.collection db name) (clj->js document))))))

(defn ^:async directory-state
  "Read portable snapshots including timestamps to detect any login-time policy mutation."
  [db]
  (loop [names collections result {}]
    (if-let [name (first names)]
      (let [documents (await (.toArray (.find (.collection db name) #js {} #js {:projection #js {:_id 0}})))
            portable (js->clj (js/JSON.parse (js/JSON.stringify documents)) :keywordize-keys true)]
        (recur (rest names) (assoc result name portable)))
      result)))

(defn ^:async membership-status!
  "Change this fixture membership's current activity while preserving its identity."
  [db status]
  (await (.updateOne (.collection db "knoxx_memberships") #js {:membership_id "existing-member"}
                    (clj->js {:$set {:status status}}))))

(defn ^:async with-bootstrap-contracts!
  "Provide isolated actual contract files and restore the process setting on every exit."
  [directory run!]
  (let [contracts (path/join directory "contracts")
        roles (path/join contracts "roles")
        previous (aget js/process.env "CONTRACTS_DIR")]
    (fs/mkdirSync roles #js {:recursive true})
    (fs/writeFileSync (path/join roles "system_admin.edn")
                      (pr-str {:role/id :role/system-admin :role/capabilities []
                               :role/permissions ["org.users.invite"]}))
    (fs/writeFileSync (path/join roles "basic_user.edn")
                      (pr-str {:role/id :role/basic-user :role/capabilities []
                               :role/permissions ["agent.chat.use"]}))
    (aset js/process.env "CONTRACTS_DIR" contracts)
    (try (await (run!))
         (finally (if (some? previous)
                    (aset js/process.env "CONTRACTS_DIR" previous)
                    (js-delete js/process.env "CONTRACTS_DIR"))))))
