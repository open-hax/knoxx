(ns knoxx.backend.extern.mongo
  "Extern adapter owning the MongoDB collection-handle boundary.

   Knoxx does not ship a MongoDB driver; callers inject a native collection
   handle (any object exposing insertOne/find with a toArray cursor). All raw
   interop with that handle is born and dies here — store records upstream
   speak CLJS maps only.")

(defn ^:async insert-one!
  "Insert one CLJS document into a native collection handle. Returns the doc."
  [collection-handle doc]
  (await (.insertOne collection-handle (clj->js doc)))
  doc)

(defn ^:async delete-one!
  "Delete at most one document matching a CLJS field-equality query.

   Returns {:deleted-count n} as CLJS data. The driver's native DeleteResult is
   decoded here and never escapes, so callers can act on the count without
   knowing the SDK shape — a caller that reads .deletedCount itself has moved
   the boundary upstream."
  [collection-handle query]
  (let [result (await (.deleteOne collection-handle (clj->js query)))]
    {:deleted-count (or (aget result "deletedCount") 0)}))

(defn ^:async find-docs!
  "Run a field-equality query against a native collection handle.
   The :limit key caps results. Returns a CLJS vector of documents."
  [collection-handle query]
  (let [limit (:limit query)
        cursor (cond-> (.find collection-handle (clj->js (dissoc query :limit)))
                 limit (.limit limit))
        rows (await (.toArray cursor))]
    (vec (js->clj rows :keywordize-keys true))))
