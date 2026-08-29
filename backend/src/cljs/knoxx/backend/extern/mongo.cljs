(ns knoxx.backend.extern.mongo
  "Extern adapter owning MongoDB client, session, and collection boundaries.

   Callers inject native client or collection handles. All raw interop with
   those handles, native option encoding, result decoding, and transaction
   session lifecycle are born and die here. Store records and transaction
   operations upstream speak CLJS maps only."
  (:require [knoxx.backend.law.mongo :as law-mongo]))

(defn ^:async insert-one!
  "Insert one CLJS document into a native collection handle. Returns the doc."
  [collection-handle doc]
  (await (.insertOne collection-handle (clj->js doc)))
  doc)

(defn- native-update-options
  [session {:keys [upsert write-concern case-insensitive?]}]
  (let [options #js {}]
    (when (some? upsert)
      (aset options "upsert" (boolean upsert)))
    (when write-concern
      (aset options "writeConcern" #js {:w (str write-concern)}))
    (when case-insensitive?
      (aset options "collation" #js {:locale "en" :strength 2}))
    (when session
      (aset options "session" session))
    options))

(defn- update-result->cljs [result]
  {:matched-count (aget result "matchedCount")
   :modified-count (aget result "modifiedCount")
   :upserted-count (aget result "upsertedCount")})

(defn- ^:async update-one-native!
  [session collection-handle query update options]
  (try
    (-> (await (.updateOne
                collection-handle
                (clj->js query)
                (clj->js update)
                (native-update-options session options)))
        update-result->cljs)
    (catch :default err
      (throw (ex-info "Mongo updateOne failed"
                      {:mongo/code (aget err "code")}
                      err)))))

(defn- ^:async update-many-native!
  [session collection-handle query update options]
  (try
    (-> (await (.updateMany
                collection-handle
                (clj->js query)
                (clj->js update)
                (native-update-options session options)))
        update-result->cljs)
    (catch :default err
      (throw (ex-info "Mongo updateMany failed"
                      {:mongo/code (aget err "code")}
                      err)))))

(defn ^:async update-one!
  "Update one document from CLJS query/update maps and return decoded counts.

   `options` accepts :upsert, :write-concern, and :case-insensitive?. Native
   Mongo option names and result objects never escape this adapter."
  [collection-handle query update options]
  (await (update-one-native! nil collection-handle query update options)))

(defn- transaction-api [session]
  {:update-one!
   (fn [collection-handle query update options]
     (update-one-native! session collection-handle query update options))
   :update-many!
   (fn [collection-handle query update options]
     (update-many-native! session collection-handle query update options))})

(defn ^:async with-transaction!
  "Run `f` in a Mongo transaction through a CLJS-first operation map.

   The callback receives :update-one! and :update-many! functions. Neither the
   native ClientSession nor driver option/result shapes escape this adapter.
   Knoxx's deployment contract is a replica set, so the transaction uses
   snapshot read concern and majority write concern and lets the driver retry
   transient write conflicts. The session is closed on every exit path."
  [client f]
  (when-not client
    (throw (js/Error. "Mongo client is required for a transaction")))
  (let [session (.startSession client)]
    (try
      (await (.withTransaction
              session
              (^:async fn []
                (await (f (transaction-api session))))
              #js {:readConcern #js {:level "snapshot"}
                   :writeConcern #js {:w "majority"}}))
      (finally
        (await (.endSession session))))))

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
