---
title: "policy_db.cljs Decomposition — Eight Jobs in One Namespace"
category: architecture
created: 2026-04-27
original: 2026.04.27.13.24.25.md
status: note
---

## The Problem, Named Precisely

`policy_db.cljs` conflates:

1. **SQL plumbing** — raw `pg` pool, `query!`, HoneySQL wrappers
2. **Schema DDL** — `ensure-schema!` with inline SQL strings
3. **Contract-file I/O** — actor EDN read/write, capability scanning
4. **Seeding** — org seeds, role seeds, tool definition upserts
5. **Domain logic: orgs** — find, create, upsert
6. **Domain logic: users + memberships** — sync from contracts, set roles, backfill actors
7. **Domain logic: roles + permissions** — merge, prioritize, legacy schema detection
8. **Domain logic: tool policies** — normalize, merge, FK-safe upsert
9. **Session/auth helpers** — `header-value`, `http-error` living here for no reason
10. **The public factory** — `create-policy-db` returning a JS object with 30+ methods

The decomposition target:

```
db/
  pg.cljs              ← pool, query!, honey wrappers — nothing else
  schema.cljs          ← ensure-schema!, migrations
  seed.cljs            ← org/role/tool seeds, one-time bootstrap
  orgs.cljs            ← org domain fns
  users.cljs           ← user domain fns
  memberships.cljs     ← membership + actor-id domain fns
  roles.cljs           ← role domain fns (replaces partial overlap with contracts.roles)
  permissions.cljs     ← permission domain fns + legacy shim
  tool_policies.cljs   ← tool policy merge/normalize/upsert
  sessions.cljs        ← session domain fns (pulled from auth_session.cljs overlap)
  contract_sync.cljs   ← actor EDN ↔ DB sync, contract-file I/O
policy_db.cljs         ← thin façade: (require all above), re-exports create-policy-db
```

***

## `db/pg.cljs` — The Only Place That Touches `pg`

```clojure
(ns knoxx.backend.db.pg
  "Single seam for all PostgreSQL I/O.
   Everything above this layer speaks Clojure data, not pg objects."
  (:require [honey.sql :as sql]
            ["pg" :as pg]))

(defn make-pool
  [{:keys [connection-string max-connections]}]
  (new (.-Pool pg)
       #js {:connectionString connection-string
            :max (or max-connections 10)}))

(defn query!
  "Returns Promise<{:rows [...clj maps...]}>"
  [pool sql-str params]
  (-> (.query pool sql-str (when (seq params) (into-array params)))
      (.then (fn [res]
               {:rows (->> (array-seq (.-rows res))
                           (mapv #(js->clj % :keywordize-keys true)))}))))

(defn query-one!
  [pool sql-str params]
  (-> (query! pool sql-str params)
      (.then #(first (:rows %)))))

(defn honey->sql [m] (sql/format m {:numbered true}))

(defn hq!  [pool m] (let [[s & p] (honey->sql m)] (query!     pool s p)))
(defn hq1! [pool m] (let [[s & p] (honey->sql m)] (query-one! pool s p)))
```

**What changed:** `query!` now returns keywordized Clojure maps. Every caller above this line never touches `aget` or `(aget result "rows")` again .

***

## `db/schema.cljs` — DDL Only

```clojure
(ns knoxx.backend.db.schema
  (:require [knoxx.backend.db.pg :as pg]))

(def ^:private ddl
  "All CREATE TABLE / INDEX / ALTER statements as a single idempotent string.
   Intentionally kept as a raw SQL string — DDL doesn't benefit from HoneySQL."
  "
  CREATE EXTENSION IF NOT EXISTS pgcrypto;

  CREATE TABLE IF NOT EXISTS orgs ( ... );
  CREATE TABLE IF NOT EXISTS users ( ... );
  CREATE TABLE IF NOT EXISTS memberships ( ... );
  CREATE TABLE IF NOT EXISTS roles ( ... );
  CREATE TABLE IF NOT EXISTS role_permissions ( ... );
  CREATE TABLE IF NOT EXISTS membership_roles ( ... );
  CREATE TABLE IF NOT EXISTS tool_definitions ( ... );
  CREATE TABLE IF NOT EXISTS role_tool_policies ( ... );
  CREATE TABLE IF NOT EXISTS user_tool_policies ( ... );
  CREATE TABLE IF NOT EXISTS data_lakes ( ... );
  CREATE TABLE IF NOT EXISTS audit_events ( ... );
  CREATE TABLE IF NOT EXISTS sessions ( ... );
  ")

(def ^:private migrations
  "Idempotent ALTER TABLE backfills for schema drift."
  "
  ALTER TABLE sessions     ADD COLUMN IF NOT EXISTS token_prefix TEXT NOT NULL DEFAULT '';
  ALTER TABLE memberships  ADD COLUMN IF NOT EXISTS actor_id TEXT;
  ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS permission_code TEXT;
  -- legacy permission_id → permission_code backfill
  DO $$ ... $$;
  ")

(defn ensure!
  [pool]
  (-> (pg/query! pool ddl [])
      (.then (fn [_] (pg/query! pool migrations [])))))
```

***

## `db/tool_policies.cljs` — Policy Merge Logic, Isolated

```clojure
(ns knoxx.backend.db.tool-policies
  (:require [knoxx.backend.db.pg :as pg]
            [knoxx.backend.tools.registry :as registry]))

(defn normalize
  "String → canonical {:toolId s :effect s :constraints m}"
  [policy]
  (let [raw (if (string? policy)
              {:toolId policy}
              policy)
        tool-id (some-> (or (:toolId raw) (:tool_id raw) (:id raw))
                        registry/normalize-tool-id)]
    (when tool-id
      {:toolId     tool-id
       :effect     (if (= "deny" (:effect raw)) "deny" "allow")
       :constraints (or (:constraints raw) {})})))

(defn merge-policies
  "Role policies < membership overrides. deny always wins within same source."
  [role-policies membership-policies]
  (->> (concat (keep normalize role-policies)
               (keep normalize membership-policies))
       (reduce (fn [acc {:keys [toolId] :as p}]
                 (let [existing (get acc toolId)]
                   (if (or (nil? existing)
                           (= "deny" (:effect p))
                           (not= "deny" (:effect existing)))
                     (assoc acc toolId p)
                     acc)))
               {})
       vals
       (sort-by :toolId)
       vec))

(defn ensure-tool-definitions!
  [pool tool-ids]
  (let [ids (->> tool-ids (keep registry/normalize-tool-id) distinct vec)]
    (if (empty? ids)
      (js/Promise.resolve nil)
      (js/Promise.all
        (into-array
          (for [id ids]
            (let [{:keys [label description risk-level]} (registry/get-tool id)]
              (pg/hq! pool
                {:insert-into :tool_definitions
                 :values      [{:id id :label (or label id)
                                :description (or description "")
                                :risk_level  (or risk-level "low")}]
                 :on-conflict [:id]
                 :do-update-set [:label :description :risk_level]}))))))))

(defn upsert-role-tool-policies!
  [pool role-id policies]
  (let [rows (for [{:keys [toolId effect constraints]} (keep normalize policies)]
               {:role_id         role-id
                :tool_id         toolId
                :effect          effect
                :constraints_json constraints})]
    (when (seq rows)
      (pg/hq! pool
        {:insert-into :role_tool_policies
         :values      (vec rows)
         :on-conflict [:role_id :tool_id]
         :do-update-set [:effect :constraints_json]}))))
```

***

## `db/roles.cljs` — Role Domain

```clojure
(ns knoxx.backend.db.roles
  (:require [knoxx.backend.db.pg :as pg]
            [knoxx.backend.db.tool-policies :as tp]
            [knoxx.backend.contracts.roles :as contracts-roles]))

(defn- priority [slug]
  ({"system_admin" 100 "org_admin" 90 "developer" 80
    "data_analyst" 70  "knowledge_worker" 60} slug 0))

(defn find-by-slug [pool org-id slug]
  (pg/hq1! pool
    {:select [:*] :from :roles
     :where  (if org-id
               [:and [:= :org_id org-id] [:= :slug slug]]
               [:and [:= :slug slug] [:= :org_id nil]])}))

(defn upsert!
  [pool {:keys [org-id slug name scope built-in system-managed]}]
  (pg/hq1! pool
    {:insert-into :roles
     :values [{:org_id         org-id
               :slug           slug
               :name           name
               :scope_kind     (or scope "org")
               :built_in       (boolean built-in)
               :system_managed (boolean system-managed)}]
     :on-conflict (if org-id [:org_id :slug] [:slug])
     :do-update-set [:name :scope_kind]
     :returning [:*]}))

(defn set-membership-roles!
  "Replace or append roles on a membership. Returns Promise<nil>."
  [pool membership-id {:keys [org-id role-slugs replace]}]
  (let [config {:contracts-dir nil}]   ;; resolved from env inside contracts-roles
    (-> (js/Promise.all
          (into-array
            (for [slug role-slugs]
              (upsert! pool {:org-id org-id :slug slug
                             :name   slug :built-in true}))))
        (.then
          (fn [role-rows]
            (let [role-ids (mapv #(get % :id) role-rows)]
              (-> (when replace
                    (pg/hq! pool {:delete-from :membership_roles
                                  :where [:= :membership_id membership-id]}))
                  (.then
                    (fn [_]
                      (pg/hq! pool
                        {:insert-into :membership_roles
                         :values (mapv #(hash-map :membership_id membership-id
                                                  :role_id %) role-ids)
                         :on-conflict [:membership_id :role_id]
                         :do-nothing true}))))))))))
```

***

## `db/contract_sync.cljs` — EDN ↔ DB, Nothing Else

```clojure
(ns knoxx.backend.db.contract-sync
  "Syncs actor EDN contracts into the database.
   Reads contracts/ from disk; writes to users + memberships + roles.
   No raw SQL — delegates to domain namespaces."
  (:require [knoxx.backend.db.users :as users]
            [knoxx.backend.db.memberships :as memberships]
            [knoxx.backend.db.roles :as roles]
            [knoxx.backend.contracts.actors :as actors]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(defn sync-actor!
  "Upsert one actor EDN contract into DB. Returns Promise<membership>."
  [pool primary-org actor-contract]
  (when-let [email (:email actor-contract)]
    (-> (users/upsert! pool {:email        email
                             :display-name (or (:label actor-contract) email)
                             :auth-provider "contract"})
        (.then
          (fn [user]
            (memberships/upsert! pool
              {:user-id  (:id user)
               :org-id   (or (:org actor-contract) (:id primary-org))
               :actor-id (:id actor-contract)
               :role-slugs (:role-slugs actor-contract)}))))))

(defn sync-all!
  "Sync every :user actor contract file into the DB."
  [pool primary-org]
  (-> (js/Promise.all
        (clj->js (mapv #(sync-actor! pool primary-org %)
                       (actors/list-user-actors))))
      (.then (fn [_] nil))))
```

***

## `policy_db.cljs` — The Surviving Façade

```clojure
(ns knoxx.backend.policy-db
  "Thin public façade. Assembles the DB subsystem and exposes create-policy-db.
   All logic lives in knoxx.backend.db.*"
  (:require [knoxx.backend.db.pg            :as pg]
            [knoxx.backend.db.schema        :as schema]
            [knoxx.backend.db.seed          :as seed]
            [knoxx.backend.db.orgs          :as orgs]
            [knoxx.backend.db.users         :as users]
            [knoxx.backend.db.memberships   :as memberships]
            [knoxx.backend.db.roles         :as roles]
            [knoxx.backend.db.permissions   :as permissions]
            [knoxx.backend.db.tool-policies :as tp]
            [knoxx.backend.db.sessions      :as sessions]
            [knoxx.backend.db.contract-sync :as contract-sync]))

(defn create-policy-db
  [connection-string opts]
  (let [pool (pg/make-pool {:connection-string connection-string})]
    (-> (schema/ensure! pool)
        (.then #(seed/run! pool opts))
        (.then
          (fn [_]
            #js {:query             (fn [s p] (pg/query! pool s (js->clj p)))
                 :findOrgBySlug     (fn [s]   (orgs/find-by-slug pool s))
                 :upsertUser        (fn [p]   (users/upsert! pool (js->clj p :keywordize-keys true)))
                 :upsertMembership  (fn [p]   (memberships/upsert! pool (js->clj p :keywordize-keys true)))
                 :setMembershipRoles (fn [id p] (roles/set-membership-roles! pool id (js->clj p :keywordize-keys true)))
                 :syncActors        (fn [org] (contract-sync/sync-all! pool org))
                 :ensureToolDefs    (fn [ids] (tp/ensure-tool-definitions! pool (js->clj ids)))
                 :mergePolicies     (fn [rp mp] (tp/merge-policies (js->clj rp) (js->clj mp)))})))))
```

***

## Responsibility Map (before → after)

| Concern | Before | After |
|---|---|---|
| `pg` pool + `query!` | `policy_db.cljs` | `db/pg.cljs` |
| DDL / migrations | `policy_db.cljs` inline strings | `db/schema.cljs` |
| Seeding | `policy_db.cljs` | `db/seed.cljs` |
| Org domain | `policy_db.cljs` | `db/orgs.cljs` |
| User domain | `policy_db.cljs` | `db/users.cljs` |
| Membership + actor-id | `policy_db.cljs` | `db/memberships.cljs` |
| Role domain | `policy_db.cljs` + `contracts.roles` overlap | `db/roles.cljs` |
| Permission domain + legacy shim | `policy_db.cljs` | `db/permissions.cljs` |
| Tool policy normalize/merge/upsert | `policy_db.cljs` | `db/tool_policies.cljs` |
| Contract EDN ↔ DB sync | `policy_db.cljs` | `db/contract_sync.cljs` |
| `http-error`, `header-value` | `policy_db.cljs` (wrong) | `util/http.cljs` (already exists) |
| Public façade | `policy_db.cljs` (1400 lines) | `policy_db.cljs` (~40 lines) |

The `aget result "rows"` pattern dies at `db/pg.cljs` — everything above it speaks keywordized maps .
