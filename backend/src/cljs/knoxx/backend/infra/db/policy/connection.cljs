(ns knoxx.backend.infra.db.policy.connection
  "Mongo policy connection, index setup and audit persistence."
  (:require [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.stores.mongo-policy-store :as mongo-policy]
            [knoxx.backend.infra.stores.mongo-policy-audit-events :as mongo-audit]))

(declare ensure-mongo-policy-db!)

(defn ^:async db!
  "Resolve the connected Mongo db, ensuring twin indexes once. Throws when
   Mongo is unavailable so write paths surface the failure rather than
   silently no-op."
  []
  (if-let [db (await (ensure-mongo-policy-db!))]
    db
    (throw (js/Error. "Mongo policy store unavailable"))))

(defn ^:async append-audit! [_pool {:keys [before after] :as opts}]
  (when-let [db (await (mongo-client/init-mongo!))]
    (await (mongo-audit/insert-event!
            db (assoc opts
                      :before-json (when before (js/JSON.stringify (clj->js before)))
                      :after-json  (when after  (js/JSON.stringify (clj->js after))))))))

(defn ^:async ensure-mongo-policy-db!
  "Connect to Mongo (idempotent) and ensure the policy-store indexes exist.
   Returns the db, or nil when Mongo is unavailable. Index setup is guarded by
   ensure-indexes! so a bad spec never crash-loops startup."
  []
  (let [db (await (mongo-client/init-mongo!))]
    (when db
      (await (mongo-policy/ensure-indexes! db)))
    db))
