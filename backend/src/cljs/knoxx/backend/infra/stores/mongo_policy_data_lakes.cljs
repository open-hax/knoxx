(ns knoxx.backend.infra.stores.mongo-policy-data-lakes
  "Mongo-backed data-lake storage — slice 6 of the PG policy DB migration
   (kanban 14-04): the data_lakes table.

   Row-shape adapter contract: documents store snake_case keys matching the
   PG columns. data-lake-doc->row re-presents :lake_id as :id and drops
   the Mongo :_id, matching the PG data-lake-row->map adapter in policy.cljs.

   DISPATCH SEAM: these functions are ROW-LEVEL twins of the
   shape.db.orgs/data-lake-by-org and shape.db.orgs/insert-data-lake
   builders. Future flag-dispatch must route at the query seam inside
   policy/list-data-lakes! and policy/create-data-lake!.

   Documents are stamped with :system_instance_id like other policy twins."
  (:require
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def DATA_LAKES_COLLECTION "knoxx_data_lakes")

(defn- data-lakes-coll [db] (.collection db DATA_LAKES_COLLECTION))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn data-lake-doc->row
  "Adapt a knoxx_data_lakes document into a PG-shaped data_lakes row.
   :lake_id → :id, drops :_id."
  [doc]
  (when doc
    (-> doc
        (assoc :id (:lake_id doc))
        (dissoc :lake_id :_id))))

(defn ^:async setup-indexes!
  "Create data-lake indexes. Idempotent.
   Unique on (org_id, slug) mirrors PG's UNIQUE (org_id, slug)."
  [db]
  (let [coll (data-lakes-coll db)]
    (await (.createIndex coll #js {"org_id" 1 "slug" 1} #js {"unique" true}))
    (await (.createIndex coll #js {"org_id" 1 "name" 1}))
    true))

(defn ^:async list-data-lakes-by-org!
  "Return all data lakes for an org, ordered by name."
  ([org-id] (list-data-lakes-by-org! (mongo-client/get-db) org-id))
  ([db org-id]
   (let [coll (data-lakes-coll db)
         cursor (.find coll #js {"org_id" (str org-id)})
         docs (keywordize (await (.toArray cursor)))]
     (mapv data-lake-doc->row (sort-by :name docs)))))

(defn ^:async create-data-lake!
  "Insert a new data lake. Returns the created row."
  ([org-id lake] (create-data-lake! (mongo-client/get-db) org-id lake))
  ([db org-id {:keys [name slug kind config-json status]}]
   (let [coll (data-lakes-coll db)
         now (js/Date.)]
     (await (.insertOne coll
                        (clj->js {:lake_id     (str (random-uuid))
                                  :org_id      (str org-id)
                                  :name        name
                                  :slug        slug
                                  :kind        (or kind "workspace_docs")
                                  :config_json (when config-json
                                                 (if (string? config-json)
                                                   (js/JSON.parse config-json)
                                                   (clj->js config-json)))
                                  :status      (or status "active")
                                  :created_at  now
                                  :updated_at  now
                                  :system_instance_id (system-instance/current-id)})))
     (let [doc (keywordize (await (.findOne coll #js {"org_id" (str org-id)
                                                      "slug" slug})))]
       (data-lake-doc->row doc)))))
