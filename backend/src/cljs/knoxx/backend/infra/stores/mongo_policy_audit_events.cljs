(ns knoxx.backend.infra.stores.mongo-policy-audit-events
  "Mongo-backed audit-events storage — slice 6 of the PG policy DB migration
   (kanban 14-04): the audit_events table.

   Write-only twin: audit_events is append-only from the application's
   perspective. No SELECT queries exist in the backend. This twin provides
   insert-event! and mirrors the PG shape.db.audit/insert-event builder.

   Row-shape adapter contract: documents store snake_case keys matching the
   PG columns. No read adapter is needed since there are no consumers.

   Documents are stamped with :system_instance_id like other policy twins."
  (:require
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def AUDIT_EVENTS_COLLECTION "knoxx_audit_events")

(defn- audit-coll [db] (.collection db AUDIT_EVENTS_COLLECTION))

(defn ^:async setup-indexes!
  "Create indexes for audit event lookups. Idempotent.
   Indexes on org_id and actor_user_id support future audit queries
   even though the backend currently has none."
  [db]
  (let [coll (audit-coll db)]
    (await (.createIndex coll #js {"org_id" 1 "created_at" -1}))
    (await (.createIndex coll #js {"actor_user_id" 1 "created_at" -1}))
    (await (.createIndex coll #js {"action" 1 "created_at" -1}))
    true))

(defn ^:async insert-event!
  "Insert an audit event. Mirrors PG shape.db.audit/insert-event.
   before-json and after-json are already serialized strings from policy/append-audit!."
  ([event] (insert-event! (mongo-client/get-db) event))
  ([db {:keys [actor-user-id actor-membership-id org-id action
               resource-kind resource-id before-json after-json]}]
   (let [coll (audit-coll db)
         now (js/Date.)]
     (await (.insertOne coll
                        (clj->js {:actor_user_id       actor-user-id
                                  :actor_membership_id actor-membership-id
                                  :org_id              org-id
                                  :action              action
                                  :resource_kind       resource-kind
                                  :resource_id         resource-id
                                  :before_json         before-json
                                  :after_json          after-json
                                  :created_at          now
                                  :system_instance_id  (system-instance/current-id)}))))))
