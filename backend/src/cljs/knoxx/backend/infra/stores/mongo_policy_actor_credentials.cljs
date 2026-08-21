(ns knoxx.backend.infra.stores.mongo-policy-actor-credentials
  "Mongo-backed actor credential storage — slice 5 of the PG policy DB migration
   (kanban 14-04): the actor_credentials table.

   This namespace is a mongo twin of the PG behavior in infra.db.policy:
   it lands as a parallel store and is NOT yet wired into any dispatch.
   Per the cutover model, durable-table slices ship as twins and the
   OPENPLANNER_KNOXX_POLICY_STORE flag only flips after the migration
   script (slice 7), so callers continue to see PG until then.

   Row-shape adapter contract (drop-in dispatch parity):
   PG queries join actor_credentials + memberships + orgs to produce rows
   with :id, :actor_id, :user_id, :org_id, :org_slug, :provider, :kind,
   :account_identifier, :status, :secret_json, :created_at, :updated_at.
   Mongo documents store the credential fields directly; the join fields
   (actor_id, org_slug) are resolved at read time by querying the
   memberships and orgs collections. This is the standard Mongo approach
   for what PG does with JOINs.

   DISPATCH SEAM (important): these functions are ROW-LEVEL twins of the
   SQL adapter's ActorCredentialStore implementation, not of the public
   infra.db.policy functions. Future flag-dispatch must route at the query
   seam inside policy/list-actor-credentials! and policy/get-actor-credential!
   and KEEP applying the existing row->credential adapter — never swap these
   row-level functions in at the public-fn seam.

   Documents are stamped with :system_instance_id like the run store."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def ACTOR_CREDENTIALS_COLLECTION "knoxx_actor_credentials")
(def ^:private MEMBERSHIPS_COLLECTION "knoxx_memberships")

(defn- credentials-coll [db] (.collection db ACTOR_CREDENTIALS_COLLECTION))

;; ---------------------------------------------------------------------------
;; Row-shape adapters
;; ---------------------------------------------------------------------------

(defn credential-doc->row
  "Adapt a knoxx_actor_credentials document into a PG-shaped actor_credentials
   row. The PG query joins memberships + orgs to add :actor_id and :org_slug;
   those are resolved separately at read time by the calling functions."
  [doc]
  (when doc
    (-> doc
        (assoc :id (:credential_id doc))
        (dissoc :credential_id :_id))))

(defn credential-row->response
  "Convert a snake_case credential row into the camelCase response shape
   matching sql-adapter/row->credential. Used by dispatch seams in policy.cljs."
  [row]
  (when row
    {:id                 (:id row)
     :actorId            (:actor_id row)
     :userId             (:user_id row)
     :orgId              (:org_id row)
     :orgSlug            (:org_slug row)
     :provider           (:provider row)
     :kind               (:kind row)
     :accountIdentifier  (:account_identifier row)
     :status             (:status row)
     :secretJson         (js->clj (or (:secret_json row) {}) :keywordize-keys true)
     :createdAt          (:created_at row)
     :updatedAt          (:updated_at row)}))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

;; ---------------------------------------------------------------------------
;; Indexes
;; ---------------------------------------------------------------------------

(defn ^:async setup-indexes!
   "Create actor-credential uniqueness + lookup indexes. Idempotent.

   Mirrors the PG uniques exactly: actor_credentials UNIQUE (user_id, org_id,
   provider, kind). Lookup indexes on user_id, org_id, and provider mirror the
   FK delete/replace access paths and the list-by-provider query."
   [db]
   (let [coll (credentials-coll db)]
     (await (.createIndex coll #js {"user_id" 1 "org_id" 1 "provider" 1 "kind" 1} #js {"unique" true}))
     (await (.createIndex coll #js {"user_id" 1}))
     (await (.createIndex coll #js {"org_id" 1}))
     (await (.createIndex coll #js {"provider" 1 "status" 1}))
     true))

;; ---------------------------------------------------------------------------
;; Membership + org resolution (app-level join for Mongo)
;; ---------------------------------------------------------------------------

(defn- ^:async resolve-membership-for-credential
   "Resolve the membership + org join for a credential document.
   Returns the credential row with :actor_id and :org_slug filled in,
   or nil if the membership is missing."
   [db cred]
   (let [memberships (.collection db "knoxx_memberships")
         orgs (.collection db "knoxx_orgs")
         membership (keywordize (await (.findOne memberships
                                                 #js {"user_id" (:user_id cred)
                                                      "org_id" (:org_id cred)})))]
     (when membership
       (let [org (keywordize (await (.findOne orgs #js {"org_id" (:org_id cred)})))]
         (assoc (credential-doc->row cred)
                :actor_id (:actor_id membership)
                :org_slug (:slug org))))))

;; ---------------------------------------------------------------------------
;; Read operations — mirrors sql_adapter ActorCredentialStore
;; ---------------------------------------------------------------------------

(defn ^:async list-actor-credentials-by-provider!
  "Active actor credential rows for a provider, joined with memberships + orgs.
   Mirrors the PG actor-credentials-select-query: SELECT ac.*, m.actor_id,
   m.user_id, m.org_id, o.slug AS org_slug FROM actor_credentials ac
   JOIN memberships m ... JOIN orgs o ... WHERE ac.provider = ? AND ac.status = 'active'
   ORDER BY m.actor_id ASC, ac.updated_at DESC."
  ([provider] (list-actor-credentials-by-provider! (mongo-client/get-db) provider))
  ([db provider]
   (let [coll (credentials-coll db)
         cursor (.find coll #js {"provider" (str provider) "status" "active"})
         rows (keywordize (await (.toArray cursor)))
         ;; resolve-membership-for-credential is async — await the whole batch
         ;; before filtering, or the filters see pending Promises and drop
         ;; every credential.
         resolved (await (js/Promise.all
                          (clj->js (mapv #(resolve-membership-for-credential db %) rows))))]
     (->> (vec (array-seq resolved))
          (filter some?)
          (filterv #(not (str/blank? (str (:actor_id %)))))
          vec))))

(defn ^:async resolve-actor-membership!
  "The membership an actor id names, or nil.

   Takes {:actor-id, :org-id, :membership-id}; the last two are optional and both
   narrow the answer. Never chooses between candidates: an actor id that names
   more than one membership throws, because the alternative is returning one
   member's user_id and reading their Discord or Bluesky secret for somebody
   else's request — silently, unrepeatably, and identically to success.

   Why choosing is unsafe at every level of scoping: knoxx_memberships is unique
   only on (user_id, org_id). actor_id carries a plain lookup index and
   set-membership-actor-id! writes whatever it is given, so two memberships in
   *one* org can share an actor id. Scoping to the org narrows the ambiguity
   without removing it.

   :membership-id is the exact answer and the one to prefer. It is unique, an MCP
   token carries it, and using it turns a search into a lookup. The actor is then
   verified against that membership rather than used to find it: a token whose
   actor and membership disagree is refused, not reconciled."
  [db {:keys [actor-id org-id membership-id]}]
  (let [memberships (.collection db MEMBERSHIPS_COLLECTION)
        actor       (some-> actor-id str str/trim not-empty)
        org         (some-> org-id str str/trim not-empty)
        membership  (some-> membership-id str str/trim not-empty)]
    (when actor
      (if membership
        (let [doc (keywordize (await (.findOne memberships #js {"membership_id" membership})))]
          (when doc
            (when-not (= (str (:actor_id doc)) actor)
              (throw (js/Error.
                      (str "Membership " membership " acts as "
                           (or (not-empty (str (:actor_id doc))) "no actor")
                           ", not " actor
                           "; refusing to resolve credentials for a mismatch."))))
            doc))
        (let [query   (cond-> {"actor_id" actor}
                        org (assoc "org_id" org))
              matches (keywordize (await (.toArray (.find memberships (clj->js query)))))]
          (cond
            (empty? matches)      nil
            (= 1 (count matches)) (first matches)
            :else
            (throw (js/Error.
                    (str "Actor " actor " names " (count matches) " memberships"
                         (if org (str " in org " org) " across orgs")
                         "; a membership id is required to resolve its"
                         " credentials unambiguously.")))))))))

(defn ^:async get-actor-credential-by-actor-and-provider!
  "Single active credential for an actor + provider, joined with memberships + orgs.
   Mirrors the PG actor-credential-select-query: ... WHERE m.actor_id = ?
   AND ac.provider = ? AND ac.status = 'active' ... LIMIT 1.

   scope narrows the membership lookup: {:org-id, :membership-id}. Pass the
   membership id whenever the caller has one — it is exact — and the org
   otherwise. See resolve-actor-membership! for why an unnarrowed lookup refuses
   an ambiguous actor rather than choosing."
  ([actor-id provider] (get-actor-credential-by-actor-and-provider! (mongo-client/get-db) actor-id provider nil))
  ([db actor-id provider scope]
   (let [membership (await (resolve-actor-membership!
                            db (assoc (or scope {}) :actor-id actor-id)))]
     (when membership
       (let [coll (credentials-coll db)
             cursor (.find coll #js {"user_id" (:user_id membership)
                                     "org_id" (:org_id membership)
                                     "provider" (str provider)
                                     "status" "active"})
             docs (keywordize (await (.toArray cursor)))
             sorted (sort-by :updated_at > docs)
             best (first sorted)]
         (when best
           (let [orgs (.collection db "knoxx_orgs")
                 org (keywordize (await (.findOne orgs #js {"org_id" (:org_id best)})))]
             (assoc (credential-doc->row best)
                    :actor_id actor-id
                    :org_slug (:slug org)))))))))

(defn ^:async list-credentials-for-users-org!
  "Raw credential rows for a set of user ids within an org, ordered by
   provider then kind. Mirrors q-users/credentials-for-users (the user-listing
   credential join); rows keep :user_id + :secret_json for the policy
   credentials-by-user grouping. Returns PG-shaped rows (no membership join)."
  ([user-ids org-id] (list-credentials-for-users-org! (mongo-client/get-db) user-ids org-id))
  ([db user-ids org-id]
   (let [uids (set (map str user-ids))]
     (->> (await (.toArray (.find (credentials-coll db) #js {"org_id" (str org-id)})))
          keywordize
          (mapv credential-doc->row)
          (filterv #(contains? uids (str (:user_id %))))
          (sort-by (juxt :provider :kind))
          vec))))

(defn ^:async get-credential-by-user-org-provider-kind!
  "Single active credential for a (user, org, provider, kind) tuple. Backs the
   local-password auth path in infra.db.policy/local-password-auth-record!,
   which reads the active local/password credential for a resolved membership.
   Returns a PG-shaped row (no membership/org join) or nil."
  ([user-id org-id provider kind]
   (get-credential-by-user-org-provider-kind! (mongo-client/get-db) user-id org-id provider kind))
  ([db user-id org-id provider kind]
   (credential-doc->row
    (keywordize (await (.findOne (credentials-coll db)
                                 #js {"user_id" (str user-id)
                                      "org_id" (str org-id)
                                      "provider" (str provider)
                                      "kind" (str kind)
                                      "status" "active"}))))))

;; ---------------------------------------------------------------------------
;; Write operations — mirrors sql_adapter upsert-actor-credential!
;; ---------------------------------------------------------------------------

(defn ^:async upsert-actor-credential!
  "Upsert an actor credential by user-id + org-id + provider + kind.
   Mirrors the PG ON CONFLICT (user_id, org_id, provider, kind) DO UPDATE SET
   account_identifier = COALESCE(EXCLUDED.account_identifier, ...),
   secret_json = secret_json || EXCLUDED.secret_json (merge),
   status = EXCLUDED.status, updated_at = NOW().

   Mongo twin: findOneAndUpdate with upsert. secret_json is merged via
   $set with the new keys overlaid on the existing map (matching PG's
   jsonb || operator)."
  ([user-id org-id provider credential]
   (upsert-actor-credential! (mongo-client/get-db) user-id org-id provider credential))
  ([db user-id org-id provider {:keys [kind account-identifier secret-json status]}]
   (let [coll (credentials-coll db)
         kind (or kind "credential")
         now (js/Date.)
         existing (keywordize (await (.findOne coll #js {"user_id" (str user-id)
                                                        "org_id" (str org-id)
                                                        "provider" (str provider)
                                                        "kind" (str kind)})))
         existing-secret (or (:secret_json existing) {})
         merged-secret (merge existing-secret (or secret-json {}))
         resolved-account-id (or account-identifier (:account_identifier existing))
         new-credential-id (when-not existing (str (random-uuid)))]
     (await (.updateOne coll
                        #js {"user_id" (str user-id)
                             "org_id" (str org-id)
                             "provider" (str provider)
                             "kind" (str kind)}
                        #js {"$set" (clj->js (cond-> {:account_identifier resolved-account-id
                                                      :secret_json merged-secret
                                                      :status (or status "active")
                                                      :updated_at now
                                                      :system_instance_id (system-instance/current-id)}
                                               (not existing) (assoc :created_at now)))
                             "$setOnInsert" (clj->js (cond-> {:user_id (str user-id)
                                                             :org_id (str org-id)
                                                             :provider (str provider)
                                                             :kind (str kind)}
                                                      new-credential-id (assoc :credential_id new-credential-id)))}
                        #js {"upsert" true}))
     (let [doc (keywordize (await (.findOne coll #js {"user_id" (str user-id)
                                                      "org_id" (str org-id)
                                                      "provider" (str provider)
                                                      "kind" (str kind)})))]
       (credential-doc->row doc)))))

(defn ^:async deactivate-actor-credential!
  "Deactivate an existing credential tuple without creating one when absent."
  ([user-id org-id provider kind]
   (deactivate-actor-credential! (mongo-client/get-db) user-id org-id provider kind))
  ([db user-id org-id provider kind]
   (await (.updateOne (credentials-coll db)
                      #js {"user_id" (str user-id)
                           "org_id" (str org-id)
                           "provider" (str provider)
                           "kind" (str kind)}
                      #js {"$set" #js {"status" "inactive"
                                       "updated_at" (js/Date.)
                                       "system_instance_id" (system-instance/current-id)}}))
   nil))

(defn ^:async deactivate-other-bootstrap-local-passwords!
  "Deactivate bootstrap-managed local passwords that belong to an earlier
   configured bootstrap identity."
  ([current-user-id]
   (deactivate-other-bootstrap-local-passwords! (mongo-client/get-db) current-user-id))
  ([db current-user-id]
   (await (.updateMany
           (credentials-coll db)
           #js {"provider" "local"
                "kind" "password"
                "status" "active"
                "secret_json.bootstrap-system-admin" true
                "user_id" #js {"$ne" (str current-user-id)}}
           #js {"$set" #js {"status" "inactive"
                             "updated_at" (js/Date.)
                             "system_instance_id" (system-instance/current-id)}}))
   nil))
