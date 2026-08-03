(ns knoxx.backend.extern.mongo
  "Extern adapter owning the MongoDB collection-handle boundary.

   Knoxx does not ship a MongoDB driver; callers inject a native collection
   handle (any object exposing insertOne/find with a toArray cursor). All raw
   interop with that handle is born and dies here — store records upstream
   speak CLJS maps only."
  (:require [knoxx.backend.law.mongo :as law-mongo]))

(defn ^:async insert-one!
  "Insert one CLJS document into a native collection handle. Returns the doc."
  [collection-handle doc]
  (await (.insertOne collection-handle (clj->js doc)))
  doc)

(defn- decoded-instant
  "A time value only when it satisfies law.mongo/EpochMillis.

   The admissible shape is named by the contract rather than re-derived here,
   so this boundary and every caller that consumes the decoded value cannot
   drift apart on what counts as an instant."
  [ms]
  (when (law-mongo/valid-epoch-ms? ms) ms))

(defn instant-ms
  "Decode a stored BSON instant to epoch milliseconds, or nil if unreadable.

   Mongo hands a date back as a native js/Date, and inspecting that anywhere
   but here would put the driver's shape in an ordinary namespace. A number is
   accepted and an ISO string is parsed so hand-written and migrated documents
   still read.

   Every path is checked against law.mongo/EpochMillis, so anything that cannot
   name a real moment — an invalid Date, an unparseable string, Infinity, NaN,
   a value past the representable range — decodes to nil, and the caller
   decides what an unreadable instant means."
  [value]
  (cond
    (number? value)           (decoded-instant value)
    (instance? js/Date value) (decoded-instant (.getTime value))
    (string? value)           (decoded-instant (.parse js/Date value))
    :else                     nil))

(defn ^:async find-one-and-delete!
  "Atomically remove one document matching a CLJS field-equality query and
   return it as CLJS data, or nil when nothing matched.

   The atomicity is the point: exactly one concurrent caller can receive the
   document, so a caller can use this to make a single-use credential single
   use. A read followed by a separate delete cannot promise that.

   Driver v6 returns the document itself; earlier versions wrapped it as
   {value: doc}. Both are unwrapped here so the caller never has to know."
  [collection-handle query]
  (let [result (await (.findOneAndDelete collection-handle (clj->js query)))]
    (when result
      (let [doc (if (and (object? result) (.hasOwnProperty result "value"))
                  (aget result "value")
                  result)]
        (when doc (js->clj doc :keywordize-keys true))))))

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
