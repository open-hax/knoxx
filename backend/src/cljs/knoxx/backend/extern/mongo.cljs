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

(defn- assert-query!
  "Refuse a query that does not satisfy law.mongo/FieldEqualityQuery.

   Shared by both delete paths: an empty query matches every document, so on
   either of them it is the difference between removing one record and
   removing the collection."
  [query]
  (when-not (law-mongo/valid-query? query)
    (throw (ex-info "refusing an unsafe field-equality query" {:query query}))))

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
   {value: doc}. Both are unwrapped here so the caller never has to know.

   The query is checked against law.mongo/FieldEqualityQuery before it is
   issued and the decoded document against DecodedDocument before it is
   returned. The input check matters most on this operation: an empty query
   matches every document, which on a delete would take the whole collection
   rather than the one code being claimed. The output check keeps an
   unexpected driver shape from reaching a caller that has already destroyed
   the record it is about to misread."
  [collection-handle query]
  (assert-query! query)
  (let [result (await (.findOneAndDelete collection-handle (clj->js query)))
        raw    (when result
                 (if (and (object? result) (.hasOwnProperty result "value"))
                   (aget result "value")
                   result))
        doc    (when raw (js->clj raw :keywordize-keys true))]
    (when-not (law-mongo/valid-document? doc)
      (throw (ex-info "mongo findOneAndDelete returned an undecodable document"
                      {:decoded doc})))
    doc))

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
  (assert-query! query)
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

(def duplicate-key-error-code
  "MongoDB's duplicate-key error code.

   Named rather than inlined because it is the only signal that distinguishes
   'another writer got there first' from 'the write failed'. Read as a failure,
   an atomic claim degrades into a retry loop that never claims anything; read
   as a success, two writers both believe they hold the same key."
  11000)

(defn duplicate-key-error?
  "True when `err` is Mongo refusing a write that would violate a unique index.

   Both spellings are read. The driver puts the code on `code`, but a write
   surfaced through a bulk path carries it on the first entry of
   `writeErrors`, and a caller that checked only one of them would treat a
   collided claim as an outage."
  [err]
  (let [code (or (aget err "code")
                 (some-> (aget err "writeErrors")
                         (aget 0)
                         (aget "code")))]
    (= duplicate-key-error-code code)))

(defn ^:async insert-one-unique!
  "Insert one CLJS document, reporting collision rather than throwing it.

   Returns `{:inserted? true :doc doc}` when this caller won the insert, and
   `{:inserted? false}` when a unique index refused it. This is the atomic
   claim: the insert IS the check, with no await between reading and writing,
   which is the property `infra.publication-effects/IIdempotencyStore` spells
   out as the reason a separate existence-check is not equivalent.

   Any other error propagates. A failed write whose cause is unknown must not
   be reported as a peaceful collision."
  [collection-handle doc]
  (try
    (await (.insertOne collection-handle (clj->js doc)))
    {:inserted? true :doc doc}
    (catch :default err
      (if (duplicate-key-error? err)
        {:inserted? false}
        (throw err)))))

(defn ^:async update-one!
  "Apply a CLJS update document to at most one match of a field-equality query.

   Returns `{:matched-count n :modified-count n}` with nil for a count the
   driver did not report, so a caller can tell 'no such document' from 'found
   it and changed nothing' — a distinction a boolean would erase."
  [collection-handle query update-doc]
  (assert-query! query)
  (let [result (await (.updateOne collection-handle
                                  (clj->js query)
                                  (clj->js update-doc)))]
    {:matched-count (let [n (aget result "matchedCount")]
                      (when (number? n) n))
     :modified-count (let [n (aget result "modifiedCount")]
                       (when (number? n) n))}))
