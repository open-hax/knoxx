(ns knoxx.backend.infra.stores.mongo-policy-roles
  "Mongo-backed roles storage — slice 3 of the PG policy DB migration
   (kanban 14-04): the roles tables roles, role_permissions, membership_roles.

   This namespace is a mongo twin of the PG behavior in infra.db.policy:
   it lands as a parallel store and is NOT yet wired into any dispatch.
   Per the cutover model, durable-table slices ship as twins and the
   OPENPLANNER_KNOXX_POLICY_STORE flag only flips after the migration
   script (slice 7), so callers continue to see PG until then.

   Row-shape adapter contract (drop-in dispatch parity):
   PG rows come back keywordized with :id (gen_random_uuid()) plus snake_case
   columns. Mongo role documents store their primary id under :role_id so the
   field name stays descriptive in the collection, but every public function
   that returns a role re-presents that id as :id and keeps the remaining
   snake_case keys identical to PG. The link tables role_permissions and
   membership_roles are PG-keyless join rows (composite PRIMARY KEY, no
   gen_random_uuid id), so their documents carry no synthetic :id.

   DISPATCH SEAM (important): these functions are ROW-LEVEL twins of the
   shape.db.roles / shape.db.memberships query builders, not of the public
   infra.db.policy functions. policy/list-roles! wraps rows via
   hydrate-role-maps (camelCase-ish :scope-kind/:built-in maps with embedded
   :permissions/:tool-policies); policy/set-membership-roles! resolves slugs
   to ids then runs a transaction; policy/create-role! ensures the role then
   sets permissions + tool policies + audit. Future flag-dispatch must route
   at the query seam inside those policy functions and KEEP applying the
   existing hydrate/wrap/resolve layers — never swap these row-level
   functions in at the public-fn seam. Tool policies (role_tool_policies) and
   composite auth records (local-password-auth-record!) are out of scope for
   this slice (slices 4 and 5).

   Slugs are canonicalised to lower-case on BOTH write and read paths (PG
   achieves the same via the platform/org slug unique indexes; the policy
   layer slugifies before it calls these builders, this twin lower-cases
   defensively so write and read paths always agree).

   Temporal fields (created_at/updated_at) are real js/Date values, mirroring
   PG TIMESTAMPTZ; PG's gen_random_uuid() is mirrored with cljs random-uuid.
   Documents are stamped with :system_instance_id like the run store."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def ROLES_COLLECTION "knoxx_roles")
(def ROLE_PERMISSIONS_COLLECTION "knoxx_role_permissions")
(def MEMBERSHIP_ROLES_COLLECTION "knoxx_membership_roles")

(defn- roles-coll [db] (.collection db ROLES_COLLECTION))
(defn- role-permissions-coll [db] (.collection db ROLE_PERMISSIONS_COLLECTION))
(defn- membership-roles-coll [db] (.collection db MEMBERSHIP_ROLES_COLLECTION))

;; ---------------------------------------------------------------------------
;; Row-shape adapters: present role_id as :id like PG rows
;; ---------------------------------------------------------------------------

(defn role-doc->row
  "Adapt a knoxx_roles document into a PG-shaped roles row: rename role_id to
   :id, drop Mongo's _id. Link tables have no synthetic id and need no adapter."
  [doc]
  (when doc
    (-> doc
        (assoc :id (get doc :role_id))
        (dissoc :role_id :_id))))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn- lower [v] (str/lower-case (str v)))

;; ---------------------------------------------------------------------------
;; Indexes
;; ---------------------------------------------------------------------------

(defn- ^:async setup-role-indexes!
  "One compound unique index {org_id, slug} mirrors BOTH PG constraints:
   Mongo indexes a missing org_id as null and unique treats nulls as equal,
   so (null, slug) enforces roles_platform_slug_uniq and (org, slug)
   enforces roles_org_slug_uniq. partialFilterExpression is deliberately
   avoided — it rejects {$exists false} (server error 67, observed live),
   and the compound index also makes absent-vs-explicit-null org_id collide
   at the uniqueness layer instead of dodging it. Plus idx_roles_org_id."
  [roles]
  (await (.createIndex roles #js {"org_id" 1 "slug" 1} #js {"unique" true}))
  (await (.createIndex roles #js {"org_id" 1})))

(defn ^:async setup-indexes!
  "Create roles uniqueness + role/link FK lookup indexes. Idempotent.

   Mirrors the PG uniques exactly: roles via setup-role-indexes!;
   role_permissions unique on (role_id, permission_code)
   (role_permissions_role_code_uniq / PRIMARY KEY); membership_roles unique on
   (membership_id, role_id) (PRIMARY KEY). Lookup indexes on
   role_permissions.role_id and membership_roles.membership_id mirror the FK
   delete/replace access paths."
  [db]
  (let [role-permissions (role-permissions-coll db)
        membership-roles (membership-roles-coll db)]
    (await (setup-role-indexes! (roles-coll db)))
    (await (.createIndex role-permissions #js {"role_id" 1 "permission_code" 1} #js {"unique" true}))
    (await (.createIndex role-permissions #js {"role_id" 1}))
    (await (.createIndex membership-roles #js {"membership_id" 1 "role_id" 1} #js {"unique" true}))
    (await (.createIndex membership-roles #js {"membership_id" 1}))
    (await (.createIndex membership-roles #js {"role_id" 1}))
    true))

;; ---------------------------------------------------------------------------
;; Roles — mirrors q-roles by-slug / by-id / by-ids / list-all / list-by-org /
;; insert / update-role, fused into ensure-role! (find-then-insert/update)
;; ---------------------------------------------------------------------------

(defn- role-scope-query
  "Mongo filter for q-roles/by-slug scope semantics: a nil org-id targets the
   platform scope (org_id absent or null), otherwise the org-scoped row. Slug is
   lower-cased to match the write path."
  [{:keys [slug org-id]}]
  (if (nil? org-id)
    ;; {org_id: null} matches both an absent field (twin write path) and an
    ;; explicit null (rows migrated from PG, where org_id was SQL NULL) —
    ;; exactly PG's org_id IS NULL. {$exists false} misses explicit nulls.
    #js {"slug" (lower slug) "org_id" nil}
    #js {"slug" (lower slug) "org_id" (str org-id)}))

(defn ^:async find-role
  "Scope-aware slug lookup (q-roles/by-slug). Returns a PG-shaped row or nil."
  ([opts] (find-role (mongo-client/get-db) opts))
  ([db {:keys [slug] :as opts}]
   (when-not (str/blank? (str slug))
     (role-doc->row (keywordize (await (.findOne (roles-coll db) (role-scope-query opts))))))))

(defn ^:async get-role-by-id!
  "Fetch a single role by its id (q-roles/by-id). Returns a row or nil."
  ([role-id] (get-role-by-id! (mongo-client/get-db) role-id))
  ([db role-id]
   (when-not (str/blank? (str role-id))
     (role-doc->row (keywordize (await (.findOne (roles-coll db)
                                                 #js {"role_id" (str role-id)})))))))

(defn ^:async list-roles-by-ids!
  "Roles for a seq of ids, ordered by name (q-roles/by-ids). Backs the
   detailed-membership-roles seam: the policy fn collects role-ids from a
   membership then re-fetches the full rows in name order before hydrating."
  ([role-ids] (list-roles-by-ids! (mongo-client/get-db) role-ids))
  ([db role-ids]
   (let [ids (mapv str role-ids)]
     (if (empty? ids)
       []
       (->> (await (.toArray (.find (roles-coll db) #js {"role_id" #js {"$in" (clj->js ids)}})))
            keywordize
            (mapv role-doc->row)
            (sort-by :name)
            vec)))))

(defn ^:async list-roles!
  "List roles ordered by built_in desc then name (q-roles/list-all). When
   org-id is given, scope to that org (q-roles/list-by-org). Returns rows."
  ([opts] (list-roles! (mongo-client/get-db) opts))
  ([db {:keys [org-id]}]
   (let [query (if org-id #js {"org_id" (str org-id)} #js {})]
     (->> (await (.toArray (.find (roles-coll db) query)))
          keywordize
          (mapv role-doc->row)
          ;; built_in desc then name asc; sort-by is stable + ascending so
          ;; invert the boolean (true -> 0) to float built-ins first.
          (sort-by (juxt #(if (:built_in %) 0 1) :name))
          vec))))

(defn ^:async list-roles-by-slugs!
  "Return id + slug rows for a seq of slugs across platform OR the given org
   scope (q-roles/by-slugs-and-org). Backs resolve-role-ids: the policy fn
   builds alias slug variants, looks them up here, then maps slug -> id.
   Slugs are lower-cased to match the write path."
  ([slugs org-id] (list-roles-by-slugs! (mongo-client/get-db) slugs org-id))
  ([db slugs org-id]
   (let [wanted (set (map lower slugs))]
     (if (empty? wanted)
       []
       (->> (await (.toArray (.find (roles-coll db)
                                    ;; org_id null matches absent + explicit-null
                                    ;; (PG-migrated platform roles); see role-scope-query.
                                    #js {"$or" #js [#js {"org_id" nil}
                                                    #js {"org_id" (str org-id)}]})))
            keywordize
            (filterv #(contains? wanted (lower (:slug %))))
            (mapv (fn [r] {:id (:role_id r) :slug (:slug r)}))
            vec)))))

(defn ^:async ensure-role!
  "Find-or-write a role by scope+slug (infra.db.policy/ensure-role!, which
   fuses q-roles/by-slug + insert/update-role). On hit, update the mutable
   attrs (name/scope_kind/built_in/system_managed) + updated_at; on miss,
   insert with a fresh role_id. Slug is lower-cased so find-role agrees.
   Returns the resulting PG-shaped row."
  ([opts] (ensure-role! (mongo-client/get-db) opts))
  ([db {:keys [org-id name slug scope-kind built-in system-managed]}]
   (let [now (js/Date.)
         lower-slug (lower slug)
         existing (await (find-role db {:org-id org-id :slug lower-slug}))
         attrs {:name name :scope_kind scope-kind
                :built_in (boolean built-in) :system_managed (boolean system-managed)}]
     (if existing
       (do
         (await (.updateOne (roles-coll db)
                            #js {"role_id" (str (:id existing))}
                            #js {"$set" (clj->js (assoc attrs :updated_at now))}))
         (await (get-role-by-id! db (:id existing))))
       (let [doc (cond-> (assoc attrs
                                :role_id (str (random-uuid))
                                :slug lower-slug
                                :scope_kind (or scope-kind "org")
                                :system_instance_id (system-instance/current-id)
                                :created_at now :updated_at now)
                   (some? org-id) (assoc :org_id (str org-id)))]
         (await (.insertOne (roles-coll db) (clj->js doc)))
         (role-doc->row doc))))))

;; ---------------------------------------------------------------------------
;; Role permissions — mirrors q-roles delete-permissions /
;; insert-permission-modern (replace-set) and permissions-for-roles (read)
;; ---------------------------------------------------------------------------

(defn ^:async permissions-for-roles!
  "permission_code rows for a set of role ids, ordered by permission_code
   (q-roles/permissions-for-roles, modern schema). Returns rows shaped
   {:role_id .. :code ..} matching the alias the PG builder selects, so the
   policy grouped-role-permissions reducer hydrates identically. The legacy
   permission_id join (permissions-for-roles-legacy) has no Mongo twin: the
   twins are born on the modern permission_code schema."
  ([role-ids] (permissions-for-roles! (mongo-client/get-db) role-ids))
  ([db role-ids]
   (let [ids (mapv str role-ids)]
     (if (empty? ids)
       []
       (->> (await (.toArray (.find (role-permissions-coll db)
                                    #js {"role_id" #js {"$in" (clj->js ids)}})))
            keywordize
            (mapv (fn [p] {:role_id (:role_id p) :code (:permission_code p)}))
            (sort-by :code)
            vec)))))

(defn ^:async set-role-permissions!
  "Replace-set a role's permission codes (infra.db.policy/set-role-permissions-tx!:
   delete-permissions then insert-permission-modern per code, inside a PG
   transaction). Mongo twin: deleteMany then insertMany (effect always
   'allow', matching the modern builder). An empty code set therefore CLEARS
   all permissions for the role.

   PG TRANSACTION SEMANTICS: the PG path is atomic (delete+insert in one tx);
   this twin is NON-ATOMIC — there is a window after the deleteMany and before
   the insertMany where the role has no permissions. Acceptable under the
   single-writer-per-system-instance assumption (no concurrent writer races a
   reader through that window). No converge-style single statement exists
   here because the replacement is a SET of rows, not one computed field, so
   the directory slice's pipeline updateMany trick does not apply."
  ([role-id codes] (set-role-permissions! (mongo-client/get-db) role-id codes))
  ([db role-id codes]
   (let [rid (str role-id)
         unique-codes (->> codes (map str) distinct vec)]
     (await (.deleteMany (role-permissions-coll db) #js {"role_id" rid}))
     (when (seq unique-codes)
       (let [docs (mapv (fn [c] {:role_id rid :permission_code c :effect "allow"
                                 :system_instance_id (system-instance/current-id)})
                        unique-codes)]
         (await (.insertMany (role-permissions-coll db) (clj->js docs)))))
     nil)))

;; ---------------------------------------------------------------------------
;; Membership roles — mirrors q-memberships delete-roles / insert-role
;; (replace-set) and q-roles/roles-for-memberships (read join)
;; ---------------------------------------------------------------------------

(defn ^:async role-ids-for-membership!
  "Role ids linked to a membership. Backs the membership read paths; ordering
   is unspecified at this layer (callers re-fetch + order roles via
   list-roles-by-ids!)."
  ([membership-id] (role-ids-for-membership! (mongo-client/get-db) membership-id))
  ([db membership-id]
   (->> (await (.toArray (.find (membership-roles-coll db)
                                #js {"membership_id" (str membership-id)})))
        keywordize
        (mapv :role_id)
        vec)))

(defn ^:async roles-for-memberships!
  "Role rows joined via membership_roles for a set of membership ids, ordered
   by role name (q-roles/roles-for-memberships). Expressed as an app-level
   aggregation over the twins: load the link rows, load the referenced roles,
   then emit one row per (membership_id, role) carrying the join projection
   :membership_id + role :role_id/:slug/:name/:scope_kind/:org_id. Backs the
   policy grouped-membership-roles reducer (hydrate-memberships seam)."
  ([membership-ids] (roles-for-memberships! (mongo-client/get-db) membership-ids))
  ([db membership-ids]
   (let [mids (mapv str membership-ids)]
     (if (empty? mids)
       []
       (let [links (->> (await (.toArray (.find (membership-roles-coll db)
                                                #js {"membership_id" #js {"$in" (clj->js mids)}})))
                        keywordize)
             role-ids (->> links (map :role_id) distinct vec)
             roles-by-id (->> (await (list-roles-by-ids! db role-ids))
                              (reduce (fn [m r] (assoc m (:id r) r)) {}))]
         (->> links
              (keep (fn [{:keys [membership_id role_id]}]
                      (when-let [r (get roles-by-id role_id)]
                        {:membership_id membership_id
                         :role_id role_id
                         :slug (:slug r) :name (:name r)
                         :scope_kind (:scope_kind r) :org_id (:org_id r)})))
              (sort-by :name)
              vec))))))

(defn ^:async set-membership-roles!
  "Replace-set a membership's role ids (infra.db.policy/set-membership-roles-tx!:
   when replace, delete-roles then insert-role per id inside a PG transaction;
   insert-role is ON CONFLICT DO NOTHING). Mongo twin: optional deleteMany
   (only when replace? is true, matching the PG guard) then insertMany of the
   distinct ids. Returns the resolved id vector like the PG fn.

   PG TRANSACTION SEMANTICS: the PG path is atomic; this twin is NON-ATOMIC —
   a window exists after deleteMany and before the inserts. Acceptable under
   the single-writer-per-system-instance assumption. ON CONFLICT DO NOTHING is
   reproduced with per-id upserts ($setOnInsert) so re-adding an existing
   (membership_id, role_id) pair — e.g. the bootstrap allowlist re-running on
   every startup with replace? false — is a no-op instead of an E11000."
  ([membership-id replace? role-ids]
   (set-membership-roles! (mongo-client/get-db) membership-id replace? role-ids))
  ([db membership-id replace? role-ids]
   (let [mid (str membership-id)
         ids (->> role-ids (map str) distinct vec)
         coll (membership-roles-coll db)]
     (when replace?
       (await (.deleteMany coll #js {"membership_id" mid})))
     (doseq [r ids]
       (await (.updateOne coll
                          #js {"membership_id" mid "role_id" r}
                          #js {"$setOnInsert"
                               (clj->js {:membership_id mid :role_id r
                                         :system_instance_id (system-instance/current-id)})}
                          #js {"upsert" true})))
     ids)))
