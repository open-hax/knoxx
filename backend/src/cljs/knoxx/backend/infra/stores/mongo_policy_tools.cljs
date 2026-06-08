(ns knoxx.backend.infra.stores.mongo-policy-tools
  "Mongo-backed tool-policy storage — slice 4 of the PG policy DB migration
   (kanban 14-04): the tool tables tool_definitions, role_tool_policies,
   user_tool_policies (the user/membership tool-policy table; PG names it
   user_tool_policies even though it keys on membership_id).

   This namespace is a mongo twin of the PG behavior in infra.db.policy:
   it lands as a parallel store and is NOT yet wired into any dispatch.
   Per the cutover model, durable-table slices ship as twins and the
   OPENPLANNER_KNOXX_POLICY_STORE flag only flips after the migration
   script (slice 7), so callers continue to see PG until then.

   Row-shape adapter contract (drop-in dispatch parity):
   tool_definitions rows use a natural TEXT id (the tool id) — PG selects it
   as :id, so the document stores it under :tool_id and tool-def-doc->row
   re-presents that as :id (plus :label/:description/:risk_level), matching
   the PG tool_row->map source columns. The link tables role_tool_policies
   and user_tool_policies are PG-keyless join rows (composite PRIMARY KEY,
   no gen_random_uuid id) so their documents carry no synthetic :id; their
   read projections return the exact snake_case keys the policy grouping
   reducers destructure.

   CONSTRAINTS_JSON (design decision, do not re-litigate): constraints_json
   is stored as the STRINGIFIED JSON string exactly as the PG path round-trips
   it. infra.db.policy/policy-with-constraints-json computes
   (js/JSON.stringify (clj->js constraints)) before calling the q-builder, and
   constraints-json->clj re-parses on read; this twin persists and returns the
   identical string so both dispatch-time helpers work unchanged. Native
   nested docs can come post-cutover.

   DISPATCH SEAM (important): these functions are ROW-LEVEL twins of the
   shape.db.roles / shape.db.memberships tool-policy query builders, not of
   the public infra.db.policy functions. policy/set-role-tool-policies-tx!
   deletes, ensures tool definitions, then inserts each normalized policy;
   the grouped-role-tool-policies / grouped-membership-tool-policies reducers
   hydrate the read rows into {:tool-id :effect :constraints} maps via
   constraints-json->clj. Future flag-dispatch must route at the query seam
   inside those policy functions and KEEP applying the existing
   ensure/normalize/group layers — never swap these row-level functions in at
   the public-fn seam. Tool-policy writes assume definitions are ensured
   first, same as PG (set-*-tool-policies-tx! calls ensure-tool-definitions!
   between the delete and the inserts); ensure-tool-definitions! here mirrors
   that idempotent upsert by tool id. Actor credentials + mailbox are out of
   scope for this slice (slice 5).

   Documents are stamped with :system_instance_id like the run store."
  (:require
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.registry.tools :as tool-registry]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def TOOL_DEFINITIONS_COLLECTION "knoxx_tool_definitions")
(def ROLE_TOOL_POLICIES_COLLECTION "knoxx_role_tool_policies")
(def USER_TOOL_POLICIES_COLLECTION "knoxx_user_tool_policies")

(defn- tool-definitions-coll [db] (.collection db TOOL_DEFINITIONS_COLLECTION))
(defn- role-tool-policies-coll [db] (.collection db ROLE_TOOL_POLICIES_COLLECTION))
(defn- user-tool-policies-coll [db] (.collection db USER_TOOL_POLICIES_COLLECTION))

;; ---------------------------------------------------------------------------
;; Row-shape adapters
;; ---------------------------------------------------------------------------

(defn tool-def-doc->row
  "Adapt a knoxx_tool_definitions document into a PG-shaped tool_definitions
   row: present the natural :tool_id as :id, drop Mongo's _id. Mirrors the
   columns infra.db.policy/tool_row->map reads (id/label/description/
   risk_level)."
  [doc]
  (when doc
    (-> doc
        (assoc :id (get doc :tool_id))
        (dissoc :tool_id :_id))))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

;; ---------------------------------------------------------------------------
;; Indexes
;; ---------------------------------------------------------------------------

(defn ^:async setup-indexes!
  "Create tool-definition uniqueness + tool-policy uniqueness/lookup indexes.
   Idempotent.

   Mirrors the PG uniques exactly: tool_definitions PRIMARY KEY (id) -> unique
   on :tool_id; role_tool_policies PRIMARY KEY (role_id, tool_id) -> compound
   unique; user_tool_policies PRIMARY KEY (membership_id, tool_id) -> compound
   unique. Lookup indexes on role_tool_policies.role_id and
   user_tool_policies.membership_id mirror the FK delete/replace access paths
   (tool-policies-for-roles / tool-policies-for-ids / delete-tool-policies).

   partialFilterExpression is deliberately avoided everywhere — it rejects
   {$exists false} (server error 67, observed live 2026-06-06) and crash-loops
   the live backend at bootstrap. None of these uniques need it: tool_id,
   role_id and membership_id are all NOT NULL in PG, so plain compound uniques
   are exact mirrors."
  [db]
  (let [tool-definitions (tool-definitions-coll db)
        role-tool-policies (role-tool-policies-coll db)
        user-tool-policies (user-tool-policies-coll db)]
    (await (.createIndex tool-definitions #js {"tool_id" 1} #js {"unique" true}))
    (await (.createIndex role-tool-policies #js {"role_id" 1 "tool_id" 1} #js {"unique" true}))
    (await (.createIndex role-tool-policies #js {"role_id" 1}))
    (await (.createIndex user-tool-policies #js {"membership_id" 1 "tool_id" 1} #js {"unique" true}))
    (await (.createIndex user-tool-policies #js {"membership_id" 1}))
    true))

;; ---------------------------------------------------------------------------
;; Tool definitions — mirrors infra.db.policy/ensure-tool-definitions! and the
;; schema/insert-tool-seeds! upsert (ON CONFLICT (id) DO UPDATE) + list-tools!
;; ---------------------------------------------------------------------------

(defn- tool-definition-doc
  "Build the upsert $set body for a tool id, resolved through the registry
   exactly like the PG path: label defaults to the id, description to \"\",
   risk_level to \"low\"."
  [tool-id]
  (let [{:keys [label description risk-level]} (tool-registry/get-tool tool-id)]
    {:label (or label tool-id)
     :description (or description "")
     :risk_level (or risk-level "low")
     :system_instance_id (system-instance/current-id)}))

(defn ^:async ensure-tool-definitions!
  "Idempotent upsert of tool definitions by tool id (infra.db.policy/
   ensure-tool-definitions!: INSERT ... ON CONFLICT (id) DO UPDATE SET label,
   description, risk_level). Tool ids are normalized through the registry and
   deduped, matching the PG path; empty in is a no-op. Mongo twin: updateOne
   with upsert per id, $set on the registry-resolved attrs so re-running
   refreshes label/description/risk_level just like EXCLUDED.* does."
  ([tool-ids] (ensure-tool-definitions! (mongo-client/get-db) tool-ids))
  ([db tool-ids]
   (let [ids (->> tool-ids (keep tool-registry/normalize-tool-id) distinct vec)
         coll (tool-definitions-coll db)]
     (doseq [tid ids]
       (await (.updateOne coll
                          #js {"tool_id" tid}
                          #js {"$set" (clj->js (assoc (tool-definition-doc tid) :tool_id tid))}
                          #js {"upsert" true})))
     nil)))

(defn ^:async list-tools!
  "All tool definitions ordered by id asc (infra.db.policy/list-tools! SELECT
   id, label, description, risk_level FROM tool_definitions ORDER BY id ASC).
   Returns PG-shaped rows; the policy layer maps these via tool_row->map."
  ([] (list-tools! (mongo-client/get-db)))
  ([db]
   (->> (await (.toArray (.find (tool-definitions-coll db) #js {})))
        keywordize
        (mapv tool-def-doc->row)
        (sort-by :id)
        vec)))

;; ---------------------------------------------------------------------------
;; Role tool policies — mirrors q-roles tool-policies-for-roles (read) and
;; delete-tool-policies + insert-tool-policy (replace-set)
;; ---------------------------------------------------------------------------

(defn ^:async tool-policies-for-roles!
  "role_tool_policies rows for a set of role ids, ordered by tool_id
   (q-roles/tool-policies-for-roles). Returns rows shaped
   {:role_id .. :tool_id .. :effect .. :constraints_json ..} matching the
   exact columns the PG builder selects, so the policy
   grouped-role-tool-policies reducer hydrates identically. constraints_json
   is the stored JSON string, re-parsed by constraints-json->clj at hydrate."
  ([role-ids] (tool-policies-for-roles! (mongo-client/get-db) role-ids))
  ([db role-ids]
   (let [ids (mapv str role-ids)]
     (if (empty? ids)
       []
       (->> (await (.toArray (.find (role-tool-policies-coll db)
                                    #js {"role_id" #js {"$in" (clj->js ids)}})))
            keywordize
            (mapv (fn [p] {:role_id (:role_id p) :tool_id (:tool_id p)
                           :effect (:effect p) :constraints_json (:constraints_json p)}))
            (sort-by :tool_id)
            vec)))))

(defn ^:async set-role-tool-policies!
  "Replace-set a role's tool policies (infra.db.policy/set-role-tool-policies-tx!:
   unconditional delete-tool-policies then, after ensure-tool-definitions!,
   insert-tool-policy per normalized policy inside a PG transaction). Mongo
   twin: deleteMany then insertMany. Each policy carries the already-computed
   :constraints-json STRING (policy-with-constraints-json upstream), persisted
   verbatim so tool-policies-for-roles! returns the identical string. An empty
   policy set therefore CLEARS all tool policies for the role.

   PG TRANSACTION SEMANTICS: PG is atomic (delete+inserts in one tx); this twin
   is NON-ATOMIC — a window exists after deleteMany and before insertMany where
   the role has no tool policies. Acceptable under the single-writer-per-
   system-instance assumption. No converge single statement applies (set
   replacement, not a computed field). Definition pre-condition matches PG
   (callers ensure-tool-definitions! first); this fn writes policy rows only."
  ([role-id normalized] (set-role-tool-policies! (mongo-client/get-db) role-id normalized))
  ([db role-id normalized]
   (let [rid (str role-id)]
     (await (.deleteMany (role-tool-policies-coll db) #js {"role_id" rid}))
     (when (seq normalized)
       (let [docs (mapv (fn [{:keys [tool-id effect constraints-json]}]
                          {:role_id rid :tool_id tool-id :effect effect
                           :constraints_json constraints-json
                           :system_instance_id (system-instance/current-id)})
                        normalized)]
         (await (.insertMany (role-tool-policies-coll db) (clj->js docs)))))
     nil)))

;; ---------------------------------------------------------------------------
;; User (membership) tool policies — mirrors q-memberships tool-policies-for-ids
;; (read) and delete-tool-policies + insert-tool-policy (replace-set). PG table
;; is user_tool_policies keyed on membership_id.
;; ---------------------------------------------------------------------------

(defn ^:async tool-policies-for-memberships!
  "user_tool_policies rows for a set of membership ids, ordered by tool_id
   (q-memberships/tool-policies-for-ids). Returns rows shaped
   {:membership_id .. :tool_id .. :effect .. :constraints_json ..} matching the
   exact columns the PG builder selects, so the policy
   grouped-membership-tool-policies reducer hydrates identically.
   constraints_json is the stored JSON string, re-parsed at hydrate."
  ([membership-ids] (tool-policies-for-memberships! (mongo-client/get-db) membership-ids))
  ([db membership-ids]
   (let [ids (mapv str membership-ids)]
     (if (empty? ids)
       []
       (->> (await (.toArray (.find (user-tool-policies-coll db)
                                    #js {"membership_id" #js {"$in" (clj->js ids)}})))
            keywordize
            (mapv (fn [p] {:membership_id (:membership_id p) :tool_id (:tool_id p)
                           :effect (:effect p) :constraints_json (:constraints_json p)}))
            (sort-by :tool_id)
            vec)))))

(defn ^:async set-membership-tool-policies!
  "Replace-set a membership's tool policies (infra.db.policy/
   set-membership-tool-policies-tx!: delete-tool-policies then, after
   ensure-tool-definitions!, insert-tool-policy per normalized policy inside a
   PG transaction). Mongo twin: deleteMany then insertMany. Each policy carries
   the already-computed :constraints-json STRING; it is persisted verbatim so
   tool-policies-for-memberships! returns the identical string. An empty policy
   set therefore CLEARS all tool policies for the membership.

   PG TRANSACTION SEMANTICS: the PG path is atomic; this twin is NON-ATOMIC —
   a window exists after deleteMany and before insertMany. Acceptable under the
   single-writer-per-system-instance assumption. No converge single statement
   applies (set replacement, not a computed field).

   Definition pre-condition matches PG (ensure-tool-definitions! first); this
   fn writes the policy rows only."
  ([membership-id normalized]
   (set-membership-tool-policies! (mongo-client/get-db) membership-id normalized))
  ([db membership-id normalized]
   (let [mid (str membership-id)]
     (await (.deleteMany (user-tool-policies-coll db) #js {"membership_id" mid}))
     (when (seq normalized)
       (let [docs (mapv (fn [{:keys [tool-id effect constraints-json]}]
                          {:membership_id mid :tool_id tool-id :effect effect
                           :constraints_json constraints-json
                           :system_instance_id (system-instance/current-id)})
                        normalized)]
         (await (.insertMany (user-tool-policies-coll db) (clj->js docs)))))
     nil)))
