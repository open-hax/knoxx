(ns knoxx.backend.infra.stores.mongo-policy-directory
  "Mongo-backed directory storage — slice 2 of the PG policy DB migration
   (kanban 14-04): the durable directory tables orgs, users, memberships.

   This namespace is a mongo twin of the PG behavior in infra.db.policy:
   it lands as a parallel store and is NOT yet wired into any dispatch.
   Per the cutover model, durable-table slices ship as twins and the
   OPENPLANNER_KNOXX_POLICY_STORE flag only flips after the migration
   script (slice 7), so callers continue to see PG until then.

   Row-shape adapter contract (drop-in dispatch parity):
   PG rows come back keywordized with :id (gen_random_uuid()) plus snake_case
   columns. Mongo documents store their primary id under {table}_id
   (:org_id / :user_id / :membership_id) so the field names stay descriptive
   in the collection, but every public function returns maps through a
   per-collection doc->row adapter that re-presents that id as :id and keeps
   the remaining snake_case keys identical to PG.

   DISPATCH SEAM (important): these functions are ROW-LEVEL twins of the
   shape.db.* query builders, not of the public infra.db.policy functions.
   policy/list-orgs! wraps rows as {:orgs [...]} and remaps via
   org-row->map (camelCase, js/Number coercion); policy/create-user!
   returns {:user .. :membership ..} plus role/actor side-effects. Future
   flag-dispatch must route at the query seam inside those policy functions
   and KEEP applying the existing row->map/wrapping layers — never swap
   these row-level functions in at the public-fn seam.

   Slugs and emails are canonicalised to lower-case on BOTH write and read
   paths (PG achieves the same with lower() in the query builders).

   Temporal fields (created_at/updated_at) are real js/Date values, mirroring
   PG TIMESTAMPTZ; PG's gen_random_uuid() is mirrored with cljs random-uuid.
   Documents are stamped with :system_instance_id like the run store."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def ORGS_COLLECTION "knoxx_orgs")
(def USERS_COLLECTION "knoxx_users")
(def MEMBERSHIPS_COLLECTION "knoxx_memberships")

(defn- orgs-coll [db] (.collection db ORGS_COLLECTION))
(defn- users-coll [db] (.collection db USERS_COLLECTION))
(defn- memberships-coll [db] (.collection db MEMBERSHIPS_COLLECTION))

;; ---------------------------------------------------------------------------
;; Row-shape adapters: present {table}_id as :id like PG rows
;; ---------------------------------------------------------------------------

(defn- doc->row
  "Generic doc->row: rename the stored id-key to :id, drop Mongo's _id."
  [id-key doc]
  (when doc
    (-> doc
        (assoc :id (get doc id-key))
        (dissoc id-key :_id))))

(defn org-doc->row
  "Adapt a knoxx_orgs document into a PG-shaped orgs row (:id + columns)."
  [doc]
  (doc->row :org_id doc))

(defn user-doc->row
  "Adapt a knoxx_users document into a PG-shaped users row (:id + columns)."
  [doc]
  (doc->row :user_id doc))

(defn membership-doc->row
  "Adapt a knoxx_memberships document into a PG-shaped memberships row."
  [doc]
  (doc->row :membership_id doc))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

;; ---------------------------------------------------------------------------
;; Indexes
;; ---------------------------------------------------------------------------

(defn ^:async setup-indexes!
  "Create directory uniqueness + FK-access lookup indexes. Idempotent.

   Unique: orgs.slug, users.email, memberships (user_id+org_id compound) —
   mirroring the PG UNIQUE constraints that back the ON CONFLICT upserts.
   Lookup: memberships.org_id and memberships.actor_id mirror the PG FK
   access patterns (idx_memberships_org_id / idx_memberships_actor_id)."
  [db]
  (let [orgs (orgs-coll db)
        users (users-coll db)
        memberships (memberships-coll db)]
    (await (.createIndex orgs #js {"slug" 1} #js {"unique" true}))
    (await (.createIndex users #js {"email" 1} #js {"unique" true}))
    (await (.createIndex memberships #js {"user_id" 1 "org_id" 1} #js {"unique" true}))
    (await (.createIndex memberships #js {"org_id" 1}))
    (await (.createIndex memberships #js {"actor_id" 1}))
    true))

;; ---------------------------------------------------------------------------
;; Orgs — mirrors q-orgs upsert-primary / insert / by-slug / list-with-counts
;; ---------------------------------------------------------------------------

(defn ^:async find-org-by-slug
  "Case-insensitive slug lookup. Mirrors q-orgs/by-slug; returns a row or nil."
  ([slug] (find-org-by-slug (mongo-client/get-db) slug))
  ([db slug]
   (when-not (str/blank? (str slug))
     (org-doc->row (keywordize (await (.findOne (orgs-coll db)
                                                #js {"slug" (str/lower-case (str slug))})))))))

(defn ^:async find-org-by-id!
  "Lookup an org by its id (mirrors q-orgs/by-id). Returns a row or nil."
  ([org-id] (find-org-by-id! (mongo-client/get-db) org-id))
  ([db org-id]
   (when-not (str/blank? (str org-id))
     (org-doc->row (keywordize (await (.findOne (orgs-coll db)
                                                #js {"org_id" (str org-id)})))))))

(defn ^:async ensure-primary-org!
  "Upsert the primary org by slug (q-orgs/upsert-primary): on insert sets
   defaults, on conflict updates name/kind and forces is_primary true.
   Slug is canonicalised to lower-case so write and read paths agree.
   The single-primary invariant is enforced with one pipeline updateMany
   computing is_primary from slug equality — atomic enough that concurrent
   calls always converge on exactly one primary (stronger than PG's
   two-statement upsert + clear-primary-except)."
  ([opts] (ensure-primary-org! (mongo-client/get-db) opts))
  ([db {:keys [slug name kind]}]
   (let [now (js/Date.)
         lower-slug (str/lower-case (str slug))
         result (await (.findOneAndUpdate
                        (orgs-coll db)
                        #js {"slug" lower-slug}
                        #js {"$set" #js {"name" name "kind" kind
                                         "is_primary" true "updated_at" now}
                             "$setOnInsert" #js {"org_id" (str (random-uuid))
                                                 "status" "active"
                                                 "system_instance_id" (system-instance/current-id)
                                                 "created_at" now}}
                        #js {"upsert" true "returnDocument" "after"}))]
     ;; Single statement: is_primary := (slug == lower-slug) for every org.
     (await (.updateMany (orgs-coll db)
                         #js {}
                         (clj->js [{"$set" {"is_primary" {"$eq" ["$slug" lower-slug]}}}])))
     (org-doc->row (keywordize result)))))

(defn ^:async create-org!
  "Insert a non-primary org (q-orgs/insert). Slug is canonicalised to
   lower-case so find-org-by-slug always agrees. Returns the inserted row."
  ([opts] (create-org! (mongo-client/get-db) opts))
  ([db {:keys [slug name kind status] :or {kind "customer" status "active"}}]
   (let [now (js/Date.)
         doc {:org_id (str (random-uuid))
              :slug (str/lower-case (str slug)) :name name :kind kind
              :is_primary false :status status
              :system_instance_id (system-instance/current-id)
              :created_at now :updated_at now}]
     (await (.insertOne (orgs-coll db) (clj->js doc)))
     (org-doc->row doc))))

(defn ^:async list-orgs!
  "List orgs with denormalised member_count, ordered is_primary desc then
   name (q-orgs/list-with-counts). role_count/data_lake_count are 0 here:
   roles + data_lakes are later migration slices, so this twin reports the
   columns it owns and zeroes the not-yet-migrated joins."
  ([] (list-orgs! (mongo-client/get-db)))
  ([db]
   (let [orgs (->> (await (.toArray (.find (orgs-coll db) #js {})))
                   keywordize
                   (mapv org-doc->row))
         memberships (keywordize (await (.toArray (.find (memberships-coll db) #js {}))))
         counts (frequencies (map :org_id memberships))]
     (->> orgs
          (mapv (fn [o] (assoc o
                               :member_count (get counts (:id o) 0)
                               :role_count 0 :data_lake_count 0)))
          (sort-by (juxt #(if (:is_primary %) 0 1) :name))
          vec))))

;; ---------------------------------------------------------------------------
;; Users — mirrors q-users upsert (ON CONFLICT email) / list-all / list-by-org
;; ---------------------------------------------------------------------------

(defn ^:async create-user!
  "Upsert a user by email (q-users/upsert): email is lower-cased; on conflict
   updates display_name/auth_provider/external_subject/status; on insert sets
   defaults + a fresh user_id. Returns the resulting row."
  ([opts] (create-user! (mongo-client/get-db) opts))
  ([db {:keys [email display-name auth-provider external-subject status]
        :or {status "active"}}]
   (let [now (js/Date.)
         lower-email (str/lower-case (str email))
         result (await (.findOneAndUpdate
                        (users-coll db)
                        #js {"email" lower-email}
                        #js {"$set" #js {"display_name" display-name
                                         "auth_provider" auth-provider
                                         "external_subject" external-subject
                                         "status" status "updated_at" now}
                             "$setOnInsert" #js {"user_id" (str (random-uuid))
                                                 "email" lower-email
                                                 "system_instance_id" (system-instance/current-id)
                                                 "created_at" now}}
                        #js {"upsert" true "returnDocument" "after"}))]
     (user-doc->row (keywordize result)))))

(defn ^:async find-user-by-email!
  "Case-insensitive active-or-any user lookup by email. Returns a PG-shaped
   row or nil. Email is lower-cased to match the write path."
  ([email] (find-user-by-email! (mongo-client/get-db) email))
  ([db email]
   (when-not (str/blank? (str email))
     (user-doc->row (keywordize (await (.findOne (users-coll db)
                                                 #js {"email" (str/lower-case (str email))})))))))

(defn ^:async list-users!
  "List users ordered by display_name then email (q-users/list-all). When
   org-id is given, only users with a membership in that org (q-users/
   list-by-org). Returns PG-shaped rows."
  ([opts] (list-users! (mongo-client/get-db) opts))
  ([db {:keys [org-id]}]
   (let [users (->> (await (.toArray (.find (users-coll db) #js {})))
                    keywordize
                    (mapv user-doc->row))
         scoped (if (str/blank? (str org-id))
                  users
                  (let [members (keywordize (await (.toArray
                                                    (.find (memberships-coll db)
                                                           #js {"org_id" (str org-id)}))))
                        member-ids (set (map :user_id members))]
                    (filterv #(contains? member-ids (:id %)) users)))]
     (vec (sort-by (juxt :display_name :email) scoped)))))

;; ---------------------------------------------------------------------------
;; Memberships — mirrors q-memberships upsert (ON CONFLICT user_id,org_id)
;; ---------------------------------------------------------------------------

(defn ^:async upsert-membership!
  "Upsert a membership by (user_id, org_id) (q-memberships/upsert): on
   conflict updates status/is_default; on insert sets defaults + a fresh
   membership_id and a null actor_id (PG actor_id is nullable, filled later
   by set-membership-actor-id!). Returns the resulting row."
  ([opts] (upsert-membership! (mongo-client/get-db) opts))
  ([db {:keys [user-id org-id status is-default]
        :or {status "active" is-default false}}]
   (let [now (js/Date.)
         result (await (.findOneAndUpdate
                        (memberships-coll db)
                        #js {"user_id" (str user-id) "org_id" (str org-id)}
                        #js {"$set" #js {"status" status "is_default" is-default
                                         "updated_at" now}
                             "$setOnInsert" #js {"membership_id" (str (random-uuid))
                                                 "user_id" (str user-id)
                                                 "org_id" (str org-id)
                                                 "actor_id" nil
                                                 "system_instance_id" (system-instance/current-id)
                                                 "created_at" now}}
                        #js {"upsert" true "returnDocument" "after"}))]
     (membership-doc->row (keywordize result)))))

(defn ^:async get-membership!
  "Fetch a single membership by its id (mirrors q-memberships/bare-by-id).
   Returns a PG-shaped row or nil."
  ([membership-id] (get-membership! (mongo-client/get-db) membership-id))
  ([db membership-id]
   (when-not (str/blank? (str membership-id))
     (membership-doc->row
      (keywordize (await (.findOne (memberships-coll db)
                                   #js {"membership_id" (str membership-id)})))))))

(defn ^:async list-memberships!
  "List memberships for an org ordered by created_at asc
   (q-memberships/list-by-org). Returns PG-shaped rows."
  ([opts] (list-memberships! (mongo-client/get-db) opts))
  ([db {:keys [org-id]}]
   (if (str/blank? (str org-id))
     (throw (js/Error. "org-id is required"))
     (->> (await (.toArray (.find (memberships-coll db) #js {"org_id" (str org-id)})))
          keywordize
          (mapv membership-doc->row)
          ;; Numeric sort: js/Date does not implement IComparable reliably.
          (sort-by #(some-> ^js (:created_at %) .getTime))
          vec))))

(defn ^:async find-membership-by-user-and-org!
  "Fetch the membership row for a (user_id, org_id) pair (mirrors
   q-memberships/by-user-and-org). Returns a PG-shaped row or nil."
  ([user-id org-id] (find-membership-by-user-and-org! (mongo-client/get-db) user-id org-id))
  ([db user-id org-id]
   (membership-doc->row
    (keywordize (await (.findOne (memberships-coll db)
                                 #js {"user_id" (str user-id) "org_id" (str org-id)}))))))

(defn ^:async set-membership-actor-id!
  "Set actor_id on a membership (q-memberships/set-actor-id). Defaults a blank
   actor-id to \"workspace_user\" like infra.db.policy/set-membership-actor-id!.
   Returns the resolved actor-id string."
  ([membership-id actor-id] (set-membership-actor-id! (mongo-client/get-db) membership-id actor-id))
  ([db membership-id actor-id]
   (let [resolved (or (some-> actor-id str str/trim not-empty) "workspace_user")]
     (await (.updateOne (memberships-coll db)
                        #js {"membership_id" (str membership-id)}
                        #js {"$set" #js {"actor_id" resolved "updated_at" (js/Date.)}}))
     resolved)))

(defn- ^:async attach-org-columns
  "Attach :org_name + :org_slug to each membership row from the orgs
   collection, mirroring the PG list-by-org / memberships-for-users joins."
  [db memberships]
  (let [orgs-by-id (->> (await (.toArray (.find (orgs-coll db) #js {})))
                        keywordize
                        (mapv org-doc->row)
                        (reduce (fn [m o] (assoc m (:id o) o)) {}))]
    (mapv (fn [m]
            (let [o (get orgs-by-id (:org_id m))]
              (assoc m :org_name (:name o) :org_slug (:slug o))))
          memberships)))

(defn ^:async list-memberships-with-org!
  "list-memberships! enriched with the :org_name/:org_slug columns the PG
   q-memberships/list-by-org join carried (consumed by hydrate-membership-row)."
  ([opts] (list-memberships-with-org! (mongo-client/get-db) opts))
  ([db opts]
   (await (attach-org-columns db (await (list-memberships! db opts))))))

(defn ^:async memberships-for-users-with-org!
  "Membership rows for a set of user ids (optionally org-scoped), enriched with
   :org_name/:org_slug. Mirrors q-users/memberships-for-users /
   all-memberships-for-users joins. Ordered by created_at asc."
  ([user-ids org-id] (memberships-for-users-with-org! (mongo-client/get-db) user-ids org-id))
  ([db user-ids org-id]
   (let [uids (set (map str user-ids))
         query (cond-> {}
                 (not (str/blank? (str org-id))) (assoc "org_id" (str org-id)))
         members (->> (await (.toArray (.find (memberships-coll db) (clj->js query))))
                      keywordize
                      (mapv membership-doc->row)
                      (filterv #(contains? uids (str (:user_id %))))
                      (sort-by #(some-> ^js (:created_at %) .getTime))
                      vec)]
     (await (attach-org-columns db members)))))

;; ---------------------------------------------------------------------------
;; Composed membership rows — app-level joins replacing the PG
;; shape.db.memberships/base-query (m.* + user + org denormalised columns).
;; ---------------------------------------------------------------------------

(defn- merge-membership-user-org
  "Flatten a membership row + its user row + its org row into one PG-shaped
   row with the exact snake_case keys shape.db.memberships/base-query selects:
   :email :display_name :user_status :org_slug :org_name :org_status
   :is_primary :org_kind on top of the membership columns. Returns nil if the
   membership has no resolvable user or org."
  [membership user org]
  (when (and membership user org)
    (assoc membership
           :email (:email user)
           :display_name (:display_name user)
           :user_status (:status user)
           :org_slug (:slug org)
           :org_name (:name org)
           :org_status (:status org)
           :is_primary (:is_primary org)
           :org_kind (:kind org))))

(defn ^:async find-membership-row-with-user-org!
  "Composed lookup by membership-id mirroring q-memberships/by-id over
   base-query: fetch the membership, its user, and its org, then merge into a
   single flat row carrying the joined columns. Returns the row or nil."
  ([membership-id] (find-membership-row-with-user-org! (mongo-client/get-db) membership-id))
  ([db membership-id]
   (when-not (str/blank? (str membership-id))
     (when-let [membership (await (get-membership! db membership-id))]
       (let [user (keywordize (await (.findOne (users-coll db)
                                               #js {"user_id" (str (:user_id membership))})))
             org  (keywordize (await (.findOne (orgs-coll db)
                                               #js {"org_id" (str (:org_id membership))})))]
         (merge-membership-user-org membership
                                    (user-doc->row user)
                                    (org-doc->row org)))))))

(defn ^:async find-membership-row-by-email-and-org!
  "Composed lookup by user email (+ optional org scope) mirroring
   q-memberships/by-email-and-org over base-query. Resolves the user by email,
   collects active+matching memberships, picks the default-first / primary-org
   one, and merges into a flat joined row. Returns the row or nil."
  ([opts] (find-membership-row-by-email-and-org! (mongo-client/get-db) opts))
  ([db {:keys [user-email org-id org-slug]}]
   (when-let [user (await (find-user-by-email! db user-email))]
     (let [scoped-org (cond
                        (not (str/blank? (str org-id))) (await (find-org-by-id! db org-id))
                        (not (str/blank? (str org-slug))) (await (find-org-by-slug db org-slug))
                        :else nil)
           members (->> (await (.toArray (.find (memberships-coll db)
                                                #js {"user_id" (str (:id user))})))
                        keywordize
                        (mapv membership-doc->row))
           orgs-by-id (->> (await (.toArray (.find (orgs-coll db) #js {})))
                           keywordize
                           (mapv org-doc->row)
                           (reduce (fn [m o] (assoc m (:id o) o)) {}))
           candidates (cond->> members
                        scoped-org (filterv #(= (:org_id %) (:id scoped-org))))
           pick (->> candidates
                     (sort-by (juxt #(if (:is_default %) 0 1)
                                    #(if (get-in orgs-by-id [(:org_id %) :is_primary]) 0 1)
                                    #(some-> ^js (:created_at %) .getTime)))
                     first)]
       (when pick
         (merge-membership-user-org pick user (get orgs-by-id (:org_id pick))))))))
