(ns knoxx.backend.infra.stores.mongo-translation-evidence
  "Durable `ITranslationEvidenceStore` over MongoDB.

  ## Why records are stored as EDN

  Every identity in this store is a namespaced keyword or a language-tag
  keyword: `:knoxx.docs/probe`, `:es`, `:dispatch/accepted`. JSON erases keyword
  namespaces, and that erasure has already produced a live defect in this
  constellation — Foresight's published-content manifest note records a PATCH
  validated against `:translation/model` silently leaving the model unchanged
  because a `clj->js` dropped the namespace and the validator accepted an
  ignored extra key. Persisting these records as BSON documents would repeat
  that failure in storage, where it is permanent.

  So the authoritative record is one EDN string, and the flat columns beside it
  exist only to be queried and indexed. A read never reconstructs a record from
  the columns; it parses the EDN and validates it. Column and record therefore
  cannot drift into disagreement without the contract catching it — and a
  sanitized-lookup collision, where two distinct revisions map onto one column
  value, cannot silently satisfy the wrong lookup.

  ## Immutable in EDN, mutable in columns

  A dispatch record has an immutable half — which document, which locales, which
  concrete revision, when it was asked — and a mutable half: its outcome, the
  batch the worker assigned, and a human-readable detail. Only the immutable
  half is in the EDN blob. The mutable half is columns, updated in place.

  That split is what keeps the updates race-free without a lock. Storing the
  whole record as one blob would make every outcome change a read-modify-write,
  and `bind-dispatch-batch!` racing `resolve-dispatch!` would clobber whichever
  field the loser had just written. Updating disjoint columns cannot.

  Translation receipts have no mutable half at all: they are append-only facts."
  (:require [clojure.edn :as edn]
            [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]))

(def DISPATCHES_COLLECTION
  "Collection holding dispatch claims — the revision bindings the worker cannot
   carry. SCREAMING_SNAKE to match the three neighbouring Mongo stores
   (`mongo-mcp-oauth`, `mongo-rate-limits`); consistency inside this directory
   beats idiom applied to one file."
  "knoxx_translation_dispatches")

(def RECEIPTS_COLLECTION
  "Collection holding completed-translation receipts. Append-only."
  "knoxx_translation_receipts")

(defn- dispatches-coll [db] (.collection db DISPATCHES_COLLECTION))
(defn- receipts-coll [db] (.collection db RECEIPTS_COLLECTION))

(defn ^:async setup-indexes!
  "Create required indexes. Idempotent.

   Every index goes through `extern.mongo/ensure-index!` rather than the driver
   directly: `#js` construction and driver calls belong to the extern adapter
   that owns the MongoDB boundary, and this namespace is `infra.*`.

   The unique index on `dispatch_key` is not an optimization — it IS the atomic
   claim. `reserve-dispatch!` relies on the insert being refused by this index
   to learn that another caller got there first. Without it, two concurrent
   reconciler runs both insert, both believe they reserved, and the same
   translation is dispatched twice."
  [db]
  (let [dispatches (dispatches-coll db)
        receipts (receipts-coll db)]
    (await (extern-mongo/ensure-index! dispatches [[:dispatch_key 1]] {:unique true}))
    ;; The join `dispatch-for-batch-document!` performs.
    (await (extern-mongo/ensure-index! dispatches [[:batch_id 1] [:document_wire_id 1]] {}))
    (await (extern-mongo/ensure-index! receipts [[:dispatch_key 1]] {}))
    (await (extern-mongo/ensure-index! receipts
                                       [[:document 1] [:garden 1] [:locale 1]
                                        [:source_revision 1]] {}))
    ;; The scope every evidence read narrows by.
    (await (extern-mongo/ensure-index! receipts [[:org_id 1] [:project 1]] {}))
    true))

;; ── Codecs ─────────────────────────────────────────────────────────────────

(def ^:private mutable-dispatch-keys
  "The dispatch fields held as columns rather than in the EDN blob.

   Named once and used by both the encoder and the decoder, so a field cannot be
   written as a column and then read back out of the blob — which would make it
   permanently stale."
  [:dispatch/outcome :dispatch/batch-id :dispatch/detail])

(def ^:private outcome-by-wire
  "Decode table for the outcome column.

   Derived from `dispatch-law/outcomes` rather than written out, so a new
   outcome cannot be introduced without becoming decodable. A table rather than
   `keyword` on the raw string: an unrecognized value must fail loudly, not
   become a keyword nothing will ever match."
  (into {} (map (fn [outcome]
                  [(str (namespace outcome) "/" (name outcome)) outcome]))
        dispatch-law/outcomes))

(def ^:private wire-by-outcome
  "Encode table, the exact inverse of `outcome-by-wire`."
  (into {} (map (fn [[wire outcome]] [outcome wire])) outcome-by-wire))

(defn- encode-dispatch
  "One dispatch record as the document to insert."
  [record]
  (let [immutable (apply dissoc record mutable-dispatch-keys)]
    (cond-> {:dispatch_key (:dispatch/key record)
             :document_wire_id (:dispatch/document-wire-id record)
             :outcome (get wire-by-outcome (:dispatch/outcome record))
             :binding_edn (pr-str immutable)}
      (some? (:dispatch/batch-id record))
      (assoc :batch_id (:dispatch/batch-id record))

      (some? (:dispatch/detail record))
      (assoc :detail (:dispatch/detail record)))))

(defn- decode-dispatch
  "Rebuild and validate one dispatch record from a stored document.

   The outcome is required to decode. A document whose outcome column holds
   something outside the contract is corruption, and surfacing it is the point:
   defaulting it would make an unreadable claim look like a fresh one and
   re-dispatch work that may already be running."
  [doc]
  (when doc
    (let [immutable (edn/read-string (:binding_edn doc))
          outcome (get outcome-by-wire (:outcome doc))]
      (when (nil? outcome)
        (throw (ex-info "unreadable translation dispatch outcome"
                        {:dispatch/key (:dispatch_key doc)
                         :outcome (:outcome doc)})))
      (dispatch-law/assert-record!
       (cond-> (assoc immutable :dispatch/outcome outcome)
         (some? (:batch_id doc)) (assoc :dispatch/batch-id (:batch_id doc))
         (some? (:detail doc)) (assoc :dispatch/detail (:detail doc)))))))

(def ^:private no-project
  "Stand-in stored for a receipt that names no project.

   A sentinel rather than an absent field, because the scope query is field
   equality: an unset project has to be *matchable*, and `{:project nil}` would
   not select rows where the key is simply missing. The value is deliberately not
   a legal project name."
  "\u0000none")

(defn- scope-value
  "The stored form of a nullable scope coordinate."
  [value]
  (or value no-project))

(defn- scope-query
  "The field-equality query for one evidence scope.

   Both coordinates always appear, so a nil project selects exactly the rows that
   name none rather than every row — an unset project is its own scope, as it is
   in the dispatch key."
  [{:keys [org-id project]}]
  {:org_id org-id
   :project (scope-value project)})

(defn- encode-receipt
  "One translation receipt as the document to insert. Wholly immutable, so the
   EDN blob is the whole record and the columns are purely for querying."
  [receipt]
  {:dispatch_key (:translation/dispatch-key receipt)
   :document (pr-str (:translation/document receipt))
   :garden (pr-str (:translation/garden receipt))
   :locale (name (:translation/locale receipt))
   :source_revision (:translation/source-revision receipt)
   ;; Scope columns, so a read narrows in the query rather than in memory.
   ;; `scope-value` keeps a nil project queryable — Mongo equality on a missing
   ;; field does not match the way an absent value should.
   :org_id (:translation/org-id receipt)
   :project (scope-value (:translation/project receipt))
   :receipt_edn (pr-str receipt)})

(defn- decode-receipt
  [doc]
  (when doc
    (evidence-law/assert-receipt! (edn/read-string (:receipt_edn doc)))))

;; ── Store ──────────────────────────────────────────────────────────────────

(defn- ^:async find-dispatch!
  [db dispatch-key]
  (decode-dispatch
   (first (await (extern-mongo/find-docs! (dispatches-coll db)
                                          {:dispatch_key dispatch-key
                                           :limit 1})))))

(defn- ^:async update-in-flight!
  "Apply `changes` to the claim for `dispatch-key`, but only while it is still
   in flight, and return the updated record or nil.

   The `outcome` predicate is part of the *query*, which is what makes this
   compare-and-set rather than read-then-write. Two callers racing to resolve
   one claim both issue the same conditional update; the first matches, and the
   second's query no longer does, so it reports nil instead of overwriting a
   terminal outcome with another one."
  [db dispatch-key changes]
  (let [accepted (get wire-by-outcome :dispatch/accepted)
        {:keys [matched-count]}
        (await (extern-mongo/update-one! (dispatches-coll db)
                                         {:dispatch_key dispatch-key
                                          :outcome accepted}
                                         {"$set" changes}))]
    (when (and (number? matched-count) (pos? matched-count))
      (await (find-dispatch! db dispatch-key)))))

(defn- ^:async replace-retriable!
  "Replace a failed or rejected claim with a fresh attempt, or report nil.

   Compare-and-set on the outcome we just read. The query names that exact
   outcome, so if another pass replaced or resolved the claim in between, this
   update matches nothing and the caller re-reads instead of overwriting a live
   attempt.

   The previous attempt's `batch_id` and `detail` are unset, not merely left. A
   stale batch id would let the old batch's completion report resolve the new
   attempt, minting a receipt for a translation this attempt never produced."
  [db record observed-outcome]
  (let [{:keys [matched-count]}
        (await (extern-mongo/update-one!
                (dispatches-coll db)
                {:dispatch_key (:dispatch/key record)
                 :outcome (get wire-by-outcome observed-outcome)}
                {"$set" {:outcome (get wire-by-outcome :dispatch/accepted)
                         :binding_edn (:binding_edn (encode-dispatch record))}
                 "$unset" {:batch_id "" :detail ""}}))]
    (when (and (number? matched-count) (pos? matched-count))
      record)))

(defn- ^:async reread-claim!
  "Report whatever the claim is now, after losing a compare-and-set race.

   Somebody else moved it between our read and our write. Re-reading rather than
   guessing is the point: whatever they did is the truth, and assuming our own
   intent would report a reservation we do not hold."
  [db dispatch-key]
  (let [current (await (find-dispatch! db dispatch-key))]
    {:reservation/status (if (= :dispatch/accepted (:dispatch/outcome current))
                           :in-flight
                           :done)
     :record current}))

(defn- ^:async or-retried-read!
  "Read the claim the unique index just told us exists, once more if needed.

   The index refused our insert, so a row with this key is there. A nil read
   means a transient inconsistency — read-your-writes lag on a secondary, or the
   row removed between the two operations — and the previous code fell through to
   `:done` with a nil record, which `dispatch-work!` reported as a duplicate. The
   work was then stranded silently: no claim to observe, no outcome to read, and
   nothing saying so.

   One re-read, then throw. Not a loop: a read that keeps not seeing a row the
   index insists on is a real inconsistency, and recursing on it would spin
   instead of surfacing it."
  [db dispatch-key]
  (or (await (find-dispatch! db dispatch-key))
      (await (find-dispatch! db dispatch-key))
      (throw (ex-info "translation dispatch claim exists but cannot be read"
                      {:dispatch/key dispatch-key
                       :cause :transient-store-inconsistency}))))

(defn- ^:async claim-dispatch!
  "Atomically claim `record`'s key, or report the existing claim.

   The insert IS the check: nothing reads the key first, so no await separates
   reading from claiming. A refusal from the unique index is the only way to
   learn the key was taken — which is why `setup-indexes!` describes that index
   as the claim rather than as a performance choice.

   A retriable existing claim is replaced rather than reported settled. See
   `law.translation-dispatch/retriable-outcomes`."
  [db record]
  (let [{:keys [inserted?]}
        (await (extern-mongo/insert-one-unique! (dispatches-coll db)
                                               (encode-dispatch record)))]
    (if inserted?
      {:reservation/status :reserved :record record}
      (let [existing (await (or-retried-read! db (:dispatch/key record)))
            outcome (:dispatch/outcome existing)]
        (cond
          (= :dispatch/accepted outcome)
          {:reservation/status :in-flight :record existing}

          (dispatch-law/retriable? outcome)
          (if-let [replaced (await (replace-retriable! db record outcome))]
            {:reservation/status :reserved :record replaced}
            (await (reread-claim! db (:dispatch/key record))))

          :else
          {:reservation/status :done :record existing})))))

(defn- ^:async find-batch-dispatch!
  [db batch-id document-wire-id]
  (decode-dispatch
   (first (await (extern-mongo/find-docs! (dispatches-coll db)
                                          {:batch_id batch-id
                                           :document_wire_id document-wire-id
                                           :limit 1})))))

(defn- ^:async find-batch-only-dispatch!
  [db batch-id]
  (decode-dispatch
   (first (await (extern-mongo/find-docs! (dispatches-coll db)
                                          {:batch_id batch-id
                                           :limit 1})))))

(defn- ^:async append-receipt!
  [db receipt]
  (await (extern-mongo/insert-one! (receipts-coll db) (encode-receipt receipt)))
  receipt)

(defn- ^:async read-receipts!
  "Every receipt in `scope`. Order-insensitive by contract:
   `domain.translation-evidence` resolves a re-translated revision by comparing
   timestamps, so no sort is required of the query."
  [db scope]
  (mapv decode-receipt
        (await (extern-mongo/find-docs! (receipts-coll db) (scope-query scope)))))

(defn create-store
  "A durable `ITranslationEvidenceStore` bound to `db`."
  [db]
  (reify store/ITranslationEvidenceStore
    (reserve-dispatch! [_ record]
      (claim-dispatch! db (dispatch-law/assert-record! record)))

    (resolve-dispatch! [_ dispatch-key outcome detail]
      (let [wire (get wire-by-outcome outcome)]
        (when (nil? wire)
          (throw (ex-info "unknown translation dispatch outcome" {:outcome outcome})))
        (update-in-flight! db dispatch-key
                           (cond-> {:outcome wire}
                             (some? detail) (assoc :detail detail)))))

    (bind-dispatch-batch! [_ dispatch-key batch-id]
      (update-in-flight! db dispatch-key {:batch_id batch-id}))

    (dispatch-for-key! [_ dispatch-key]
      (find-dispatch! db dispatch-key))

    (dispatch-for-batch-document! [_ batch-id document-wire-id]
      (find-batch-dispatch! db batch-id document-wire-id))

    (dispatch-for-batch! [_ batch-id]
      (find-batch-only-dispatch! db batch-id))

    (record-translation! [_ receipt]
      (append-receipt! db (evidence-law/assert-receipt! receipt)))

    (completed-translations! [_ scope]
      (read-receipts! db scope))))
