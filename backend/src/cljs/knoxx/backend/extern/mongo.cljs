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

;; ECMAScript's maximum time value: ±100,000,000 days from the epoch. A number
;; outside this cannot be an instant, and js/Date rejects it too.
(def ^:private max-time-value 8.64e15)

(defn- finite-instant
  "A time value only when it is a real, representable instant.

   Infinity is the dangerous one: it is a number, it compares greater than
   every clock reading, and unguarded it would leave a credential live for
   good. NaN and out-of-range values are rejected for the same reason —
   nothing that cannot name a moment may be treated as one."
  [ms]
  (when (and (js/Number.isFinite ms)
             (<= (- max-time-value) ms max-time-value))
    ms))

(defn instant-ms
  "Decode a stored BSON instant to epoch milliseconds, or nil if unreadable.

   Mongo hands a date back as a native js/Date, and inspecting that anywhere
   but here would put the driver's shape in an ordinary namespace. A number is
   accepted and an ISO string is parsed so hand-written and migrated documents
   still read.

   Every path goes through finite-instant, so anything that cannot name a real
   moment — an invalid Date, an unparseable string, Infinity, NaN, a value past
   the representable range — decodes to nil, and the caller decides what an
   unreadable instant means."
  [value]
  (cond
    (number? value)           (finite-instant value)
    (instance? js/Date value) (finite-instant (.getTime value))
    (string? value)           (finite-instant (.parse js/Date value))
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
