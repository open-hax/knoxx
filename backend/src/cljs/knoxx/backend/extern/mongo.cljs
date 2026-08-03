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

(defn instant-ms
  "Decode a stored BSON instant to epoch milliseconds, or nil if unreadable.

   Mongo hands a date back as a native js/Date, and inspecting that anywhere
   but here would put the driver's shape in an ordinary namespace. A number is
   passed through and an ISO string is parsed so hand-written and migrated
   documents still read; anything else — including a date that parses to NaN —
   is nil, leaving the caller to decide what an unreadable instant means."
  [value]
  (cond
    (number? value)           value
    (instance? js/Date value) (let [ms (.getTime value)] (when-not (js/isNaN ms) ms))
    (string? value)           (let [ms (.parse js/Date value)] (when-not (js/isNaN ms) ms))
    :else                     nil))

(defn ^:async delete-one!
  "Delete at most one document matching a CLJS field-equality query.

   Returns {:deleted-count n} as CLJS data. The driver's native DeleteResult is
   decoded here and never escapes, so callers can act on the count without
   knowing the SDK shape — a caller that reads .deletedCount itself has moved
   the boundary upstream.

   Decodes faithfully rather than defensively: a handle that reports no numeric
   deletedCount yields {:deleted-count nil}, not zero. Substituting zero would
   claim the driver said nothing was deleted when it actually said nothing at
   all, and callers validating a required count would accept the fabrication
   and carry on — the boundary must fail closed, not invent an answer."
  [collection-handle query]
  (let [result (await (.deleteOne collection-handle (clj->js query)))
        count  (aget result "deletedCount")]
    {:deleted-count (when (number? count) count)}))

(defn ^:async find-docs!
  "Run a field-equality query against a native collection handle.
   The :limit key caps results. Returns a CLJS vector of documents."
  [collection-handle query]
  (let [limit (:limit query)
        cursor (cond-> (.find collection-handle (clj->js (dissoc query :limit)))
                 limit (.limit limit))
        rows (await (.toArray cursor))]
    (vec (js->clj rows :keywordize-keys true))))
