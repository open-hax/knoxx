---
title: "μ Driver-Agnostic Migration Protocol"
category: architecture
created: 2026-04-27
original: 2026.04.27.13.35.15.md
status: note
---

## #μ Driver-Agnostic Migration Protocol

The core model: a **migration** is a named, versioned, pure transformation over the contract data graph. It is not a SQL `ALTER TABLE`. It is not tied to any storage format. It describes **what changed in the domain** and provides a `up` fn that transforms the old shape to the new shape — the driver then projects that transformation into whatever store it manages.

```
MigrationSpec (EDN)
  :migration/id        — unique, sortable, e.g. "2026-04-27-001-rename-actor-kind"
  :migration/version   — integer (monotonic)
  :migration/entity    — which entity kind is affected (:org :role :actor :capability :tool-policy ...)
  :migration/transform — pure fn: old-shape → new-shape (SCI-eval'd from :migration/expr)
  :migration/expr      — EDN form of the transform fn (portable, storable)
  :migration/predicate — optional: old-shape → bool (which records to migrate)
  :migration/reversible — bool
  :migration/down-expr  — optional reverse transform
```

The driver receives a `MigrationSpec` and applies it to every matching record in its store. SQL gets `UPDATE`s. Atoms get `swap!`s. XTDB gets transactions. The migration is agnostic to all of them.

***

## #μ Malli Schemas

```clojure
(ns knoxx.contracts.migration
  "Migration protocol — driver-agnostic contract evolution.

   A migration is a pure data-to-data transformation over a named entity kind.
   The driver applies it to its storage medium.
   The migration log is itself a contract (migrations/*.edn).")

(def EntityKind
  [:enum :org :role :capability :actor :tool-policy :permission :session :data-lake])

(def MigrationId
  "ISO-date + sequence + slug. Sorts lexicographically = chronologically."
  [:re #"^\d{4}-\d{2}-\d{2}-\d{3}-.+$"])

(def MigrationExpr
  "An EDN form of a 1-arity fn: (fn [record] ...).
   Evaluated with SCI — no side effects, no I/O, pure data → data."
  :any)

(def MigrationSpec
  [:map
   [:migration/id          MigrationId]
   [:migration/version     :int]
   [:migration/entity      EntityKind]
   [:migration/description :string]
   [:migration/expr        MigrationExpr]         ;; up transform
   [:migration/predicate   {:optional true} MigrationExpr]  ;; filter: which records
   [:migration/reversible  {:optional true} :boolean]
   [:migration/down-expr   {:optional true} MigrationExpr]  ;; down transform
   [:migration/created-at  {:optional true} :string]])

(def MigrationRecord
  "Stored in the driver's migration log to track applied migrations."
  [:map
   [:migration/id         MigrationId]
   [:migration/version    :int]
   [:migration/applied-at :string]   ;; ISO-8601
   [:migration/status     [:enum :ok :failed :rolled-back]]
   [:migration/error      {:optional true} :string]])
```

***

## #μ The Protocol

```clojure
(ns knoxx.contracts.migration-driver
  "Migration extension to ContractDriver.
   Every driver that wants contract-driven migrations implements this.")

(defprotocol MigrationDriver
  (applied-migrations  [driver]
    "Returns Promise<[MigrationRecord]> — what has already run.")

  (apply-migration!    [driver spec]
    "Apply one MigrationSpec to the store.
     Fetches all records matching :migration/entity (and :migration/predicate if set),
     runs :migration/expr over each, writes the results back.
     Returns Promise<{:migration/id id :applied int :skipped int}>.")

  (rollback-migration! [driver spec]
    "Apply :migration/down-expr if :migration/reversible is true.
     Returns Promise<{:migration/id id :rolled-back int}>.")

  (record-migration!   [driver record]
    "Persist a MigrationRecord to the migration log.
     Returns Promise<MigrationRecord>."))
```

***

## #μ `db/migration-runner.cljs` — The Engine

```clojure
(ns knoxx.backend.db.migration-runner
  "Loads migrations/*.edn, diffs against applied log, runs pending ones in order.
   Pure orchestration — all storage is delegated to the MigrationDriver."
  (:require [knoxx.contracts.migration-driver :refer [MigrationDriver
                                                       applied-migrations
                                                       apply-migration!
                                                       record-migration!
                                                       rollback-migration!]]
            [knoxx.backend.contracts.loader :as loader]
            [sci.core :as sci]
            [shadow.cljs.modern :refer [js-await]]))

(defn- eval-expr
  "Safely evaluate a migration EDN expr via SCI. Returns the fn."
  [expr]
  (sci/eval-form (sci/init {:namespaces {'clojure.string (sci/copy-ns clojure.string)}})
                 expr))

(defn- matches-predicate?
  [pred-fn record]
  (if pred-fn (boolean (pred-fn record)) true))

(defn- pending
  "Return migrations not yet in the applied log, sorted by :migration/version."
  [all-specs applied-records]
  (let [applied-ids (into #{} (map :migration/id applied-records))]
    (->> all-specs
         (remove #(contains? applied-ids (:migration/id %)))
         (sort-by :migration/version))))

(defn run-pending!
  "Load all migrations from contracts/migrations/, apply pending ones in order.
   Returns Promise<{:applied [id] :skipped [id] :failed [id]}>"
  [driver config]
  (js-await [specs   (loader/load-all config "migrations")
             applied (applied-migrations driver)]
    (let [to-run (pending specs applied)]
      (if (empty? to-run)
        (do (println "[migration-runner] nothing to run")
            {:applied [] :skipped [] :failed []})
        (-> (reduce
              (fn [acc-p spec]
                (.then acc-p
                  (fn [acc]
                    (js-await [result (apply-migration! driver spec)]
                      (js-await [_ (record-migration! driver
                                     {:migration/id         (:migration/id spec)
                                      :migration/version    (:migration/version spec)
                                      :migration/applied-at (.toISOString (js/Date.))
                                      :migration/status     :ok})]
                        (update acc :applied conj (:migration/id spec)))))))
              (js/Promise.resolve {:applied [] :skipped [] :failed []})
              to-run))))))
```

***

## #μ `db/pg-migration-driver.cljs` — SQL Projection

```clojure
(ns knoxx.backend.db.pg-migration-driver
  "MigrationDriver implementation for PostgreSQL.
   Applies transform fns to rows fetched from the entity table,
   then writes them back via upsert."
  (:require [knoxx.contracts.migration-driver :refer [MigrationDriver]]
            [knoxx.backend.db.pg :as pg]
            [knoxx.backend.db.pg-driver :as pd]
            [sci.core :as sci]
            [shadow.cljs.modern :refer [js-await]]))

(def ^:private entity->table
  {:org          :orgs
   :role         :roles
   :actor        :memberships
   :tool-policy  :role_tool_policies
   :permission   :role_permissions
   :session      :sessions
   :data-lake    :data_lakes})

(defn- safe-eval [expr]
  (sci/eval-form (sci/init {}) expr))

(defn- ensure-migration-log! [pool]
  (pg/query! pool
    "CREATE TABLE IF NOT EXISTS contract_migrations (
       id TEXT PRIMARY KEY,
       version INTEGER NOT NULL,
       applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
       status TEXT NOT NULL DEFAULT 'ok',
       error TEXT
     )" []))

(defrecord PgMigrationDriver [pool pg-driver]

  MigrationDriver

  (applied-migrations [_]
    (-> (ensure-migration-log! pool)
        (.then (fn [_]
                 (pg/hq! pool {:select [:*] :from :contract_migrations
                               :order-by [:version]})))))

  (apply-migration! [_ spec]
    (let [table     (entity->table (:migration/entity spec))
          xform-fn  (safe-eval (:migration/expr spec))
          pred-fn   (when (:migration/predicate spec)
                      (safe-eval (:migration/predicate spec)))]
      (js-await [res (pg/hq! pool {:select [:*] :from table})]
        (let [rows    (:rows res)
              targets (filter #(if pred-fn (pred-fn %) true) rows)
              xformed (mapv xform-fn targets)]
          (js-await [_ (js/Promise.all
                         (into-array
                           (map (fn [row]
                                  ;; Delegate back to the ContractDriver for
                                  ;; type-safe upsert — never raw SQL here
                                  (case (:migration/entity spec)
                                    :org    (pd/upsert-org!  pg-driver row)
                                    :role   (pd/upsert-role! pg-driver row)
                                    :actor  (pd/upsert-actor! pg-driver row)
                                    (pg/hq! pool {:insert-into table
                                                  :values [row]
                                                  :on-conflict [:id]
                                                  :do-update-set (keys row)})))
                                xformed)))]
            {:migration/id (:migration/id spec)
             :applied      (count xformed)
             :skipped      (- (count rows) (count targets))})))))

  (rollback-migration! [_ spec]
    (if-not (:migration/reversible spec)
      (js/Promise.reject (js/Error. (str "Migration not reversible: " (:migration/id spec))))
      (let [table    (entity->table (:migration/entity spec))
            down-fn  (safe-eval (:migration/down-expr spec))
            pred-fn  (when (:migration/predicate spec)
                       (safe-eval (:migration/predicate spec)))]
        (js-await [res (pg/hq! pool {:select [:*] :from table})]
          (let [targets (filter #(if pred-fn (pred-fn %) true) (:rows res))
                xformed (mapv down-fn targets)]
            (js-await [_ (js/Promise.all
                           (into-array
                             (map #(pg/hq! pool {:insert-into table :values [%]
                                                 :on-conflict [:id]
                                                 :do-update-set (keys %)})
                                  xformed)))]
              {:migration/id     (:migration/id spec)
               :rolled-back      (count xformed)}))))))

  (record-migration! [_ record]
    (pg/hq1! pool
      {:insert-into :contract_migrations
       :values [{:id         (:migration/id record)
                 :version    (:migration/version record)
                 :applied_at (:migration/applied-at record)
                 :status     (name (:migration/status record))
                 :error      (:migration/error record)}]
       :on-conflict [:id]
       :do-update-set [:status :error]})))
```

***

## #μ `db/mem-migration-driver.cljs` — Atom Projection

```clojure
(ns knoxx.backend.db.mem-migration-driver
  (:require [knoxx.contracts.migration-driver :refer [MigrationDriver]]
            [sci.core :as sci]))

(defn- safe-eval [expr] (sci/eval-form (sci/init {}) expr))

(defrecord MemMigrationDriver [state* log*]

  MigrationDriver

  (applied-migrations [_]
    (js/Promise.resolve (vec (sort-by :migration/version (vals @log*)))))

  (apply-migration! [_ spec]
    (let [kind     (:migration/entity spec)
          xform-fn (safe-eval (:migration/expr spec))
          pred-fn  (when (:migration/predicate spec)
                     (safe-eval (:migration/predicate spec)))]
      (let [bucket  (get @state* kind {})
            targets (filter #(if pred-fn (pred-fn %) true) (vals bucket))
            xformed (mapv xform-fn targets)]
        (doseq [row xformed]
          (let [id (or (:id row) (:actor/id row) (:role/slug row) (:org/slug row))]
            (swap! state* assoc-in [kind id] row)))
        (js/Promise.resolve {:migration/id (:migration/id spec)
                             :applied      (count xformed)
                             :skipped      (- (count (vals bucket)) (count targets))}))))

  (rollback-migration! [_ spec]
    (if-not (:migration/reversible spec)
      (js/Promise.reject (js/Error. "Not reversible"))
      (let [kind    (:migration/entity spec)
            down-fn (safe-eval (:migration/down-expr spec))
            pred-fn (when (:migration/predicate spec) (safe-eval (:migration/predicate spec)))
            bucket  (get @state* kind {})
            targets (filter #(if pred-fn (pred-fn %) true) (vals bucket))
            xformed (mapv down-fn targets)]
        (doseq [row xformed]
          (let [id (or (:id row) (:actor/id row))]
            (swap! state* assoc-in [kind id] row)))
        (js/Promise.resolve {:migration/id (:migration/id spec) :rolled-back (count xformed)}))))

  (record-migration! [_ record]
    (swap! log* assoc (:migration/id record) record)
    (js/Promise.resolve record)))

(defn make [] (->MemMigrationDriver (atom {}) (atom {})))
```

***

## #μ Example Migration Contracts

### Rename a field across all actors

```edn
;; contracts/migrations/2026-04-27-001-actor-kind-agent-to-system.edn
{:migration/id          "2026-04-27-001-actor-kind-agent-to-system"
 :migration/version     1
 :migration/entity      :actor
 :migration/description "Rename :actor/kind :agent → :system for non-human actors"
 :migration/predicate   (fn [r] (= :agent (:actor/kind r)))
 :migration/expr        (fn [r] (assoc r :actor/kind :system))
 :migration/reversible  true
 :migration/down-expr   (fn [r] (assoc r :actor/kind :agent))}
```

### Add a default field to all roles

```edn
;; contracts/migrations/2026-04-27-002-role-add-scope.edn
{:migration/id          "2026-04-27-002-role-add-scope"
 :migration/version     2
 :migration/entity      :role
 :migration/description "Backfill :role/scope :org on all roles lacking it"
 :migration/predicate   (fn [r] (nil? (:role/scope r)))
 :migration/expr        (fn [r] (assoc r :role/scope :org))
 :migration/reversible  false}
```

### Promote permissions to explicit tool policies

```edn
;; contracts/migrations/2026-04-27-003-permissions-to-tool-policies.edn
{:migration/id          "2026-04-27-003-permissions-to-tool-policies"
 :migration/version     3
 :migration/entity      :role
 :migration/description "Derive :role/tool-policies from :role/permissions tool.* entries"
 :migration/predicate   (fn [r] (empty? (:role/tool-policies r)))
 :migration/expr
 (fn [r]
   (let [tool-perms (->> (:role/permissions r)
                         (filter #(clojure.string/starts-with? % "tool."))
                         (map (fn [p]
                                {:toolId (-> p
                                            (clojure.string/replace-first #"^tool\." "")
                                            (clojure.string/replace #"\." "."))
                                 :effect "allow"})))]
     (assoc r :role/tool-policies (vec tool-perms))))
 :migration/reversible false}
```

***

## Execution Order at Boot

```clojure
;; In bootstrap.cljs / entrypoint.cljs
(js-await [_       (driver/ensure-schema! pg-driver)
           _       (migration-runner/run-pending! pg-migration-driver config)
           result  (contract-sync/sync-all! pg-driver config)]
  (println "[boot] migrations + sync complete" result))
```

The sequence is:

1. **`ensure-schema!`** — DDL, creates `contract_migrations` table
2. **`run-pending!`** — applies any new migration EDN files not yet in the log
3. **`sync-all!`** — upserts current contract state (roles, actors, orgs, caps)

Migrations run **before** sync, so the transform shapes the data before the fresh contract values land on top of it .

***

## Invariants

| Rule | Enforcement |
|---|---|
| Migration exprs are pure — no I/O, no side effects | SCI sandbox; no `js/fetch`, no atoms |
| Migration IDs are immutable once applied | `record-migration!` uses `ON CONFLICT DO NOTHING` |
| Contracts are additive — sync never deletes | Only migrations explicitly delete |
| Down migrations only run on explicit rollback call | `run-pending!` never calls `rollback-migration!` |
| Driver decides *how* to write — migration decides *what* to change | Protocol boundary is `apply-migration!` |
