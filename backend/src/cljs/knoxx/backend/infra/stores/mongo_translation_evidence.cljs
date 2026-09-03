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

  Translation receipts have no mutable half at all: each stable identity is an
  immutable fact, atomically admitted once."
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
  "Collection holding immutable completed-translation receipts."
  "knoxx_translation_receipts")

(def APPROVALS_COLLECTION
  "Collection holding review approvals. One row per approved output, enforced by
   a unique index rather than by a read-then-write."
  "knoxx_translation_approvals")

;; Acquired through the extern adapter rather than by calling `.collection` on
;; the database handle here. `db` is an opaque JavaScript handle and
;; `.collection` is a driver method on it, so reaching for it from `infra.*`
;; would leave this namespace owning handle acquisition while `extern.mongo`
;; owned every read, write and index built from the result — the split that
;; adapter exists to prevent.
(defn- dispatches-coll [db] (extern-mongo/collection db DISPATCHES_COLLECTION))
(defn- receipts-coll [db] (extern-mongo/collection db RECEIPTS_COLLECTION))
(defn- approvals-coll [db] (extern-mongo/collection db APPROVALS_COLLECTION))

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
        receipts (receipts-coll db)
        approvals (approvals-coll db)]
    (await (extern-mongo/ensure-index! dispatches [[:dispatch_key 1]] {:unique true}))
    ;; The join `dispatch-for-batch-document!` performs.
    (await (extern-mongo/ensure-index! dispatches [[:batch_id 1] [:document_wire_id 1]] {}))
    ;; One immutable receipt per exact resource/source/output/dispatch identity.
    ;; Sparse admits historical rows that predate `receipt_key`. It guarantees
    ;; uniqueness among new writers, but cannot serialize a mixed deployment in
    ;; which an old writer inserts an unkeyed row concurrently. Read and claim
    ;; paths therefore collapse compatible legacy/keyed duplicates and fail
    ;; closed on disagreement; full first-write atomicity starts only after old
    ;; writers are drained and historical rows are backfilled.
    (await (extern-mongo/ensure-index! receipts [[:receipt_key 1]]
                                       {:unique true :sparse true}))
    (await (extern-mongo/ensure-index! receipts [[:dispatch_key 1]] {}))
    (await (extern-mongo/ensure-index! receipts
                                       [[:document 1] [:garden 1] [:locale 1]
                                        [:source_revision 1]] {}))
    ;; The scope every evidence read narrows by.
    (await (extern-mongo/ensure-index! receipts [[:org_id 1] [:project 1]] {}))
    (await (extern-mongo/ensure-index! approvals [[:org_id 1] [:project 1]] {}))
    ;; Unique, and for the same reason as dispatch_key: it is what makes
    ;; recording an approval idempotent rather than append-once-per-click.
    (await (extern-mongo/ensure-index! approvals [[:approval_key 1]] {:unique true}))
    true))

;; ── Codecs ─────────────────────────────────────────────────────────────────

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
  (let [immutable (dispatch-law/attempt-binding record)]
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

(defn- receipt-key
  "The unique-index value for one completed translation.

   Derived from the shared store identity so memory and Mongo cannot disagree
   about what is an equal retry. `pr-str` preserves keyword namespaces."
  [receipt]
  (pr-str (store/receipt-identity receipt)))

(defn- encode-receipt
  "One translation receipt as the document to insert. Wholly immutable, so the
   EDN blob is the whole record and the columns are purely for querying."
  [receipt]
  {:receipt_key (receipt-key receipt)
   :dispatch_key (:translation/dispatch-key receipt)
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

(defn- attempt-query
  "Mongo equality coordinates for one exact immutable dispatch attempt."
  [record]
  {:dispatch_key (:dispatch/key record)
   :binding_edn (pr-str (dispatch-law/attempt-binding record))})

(defn- completion-owner
  "Stable store-private owner for one immutable dispatch attempt."
  [record]
  (pr-str (dispatch-law/attempt-binding record)))

(defn- ^:async update-in-flight!
  "Apply `changes` to `expected-record`, but only while that exact attempt is
   active and no completion owns it. Return `updated-record` or nil.

   Both the accepted outcome and immutable binding are query predicates. The
   latter prevents ABA: a delayed attempt A cannot settle replacement B merely
   because B deliberately reuses the same logical dispatch key. The absent
   completion owner prevents a racing failure from invalidating an attempt after
   receipt persistence has begun."
  [db expected-record changes updated-record]
  (let [accepted (get wire-by-outcome :dispatch/accepted)
        {:keys [matched-count]}
        (await (extern-mongo/update-one! (dispatches-coll db)
                                         (assoc (attempt-query expected-record)
                                                :outcome accepted
                                                :completion_owner {:$exists false})
                                         {"$set" changes}))]
    (when (and (number? matched-count) (pos? matched-count))
      ;; Re-read to include a disjoint batch binding that may have landed just
      ;; before this update. Never return a replacement attempt if one raced the
      ;; read: the known updated value is safer than attributing B to A.
      (let [current (await (find-dispatch! db (:dispatch/key expected-record)))]
        (if (and current (dispatch-law/same-attempt? current expected-record))
          current
          (dispatch-law/assert-record! updated-record))))))

(defn- ^:async claim-completion!
  "Acquire or idempotently resume completion ownership for one exact attempt."
  [db expected-record]
  (let [accepted (get wire-by-outcome :dispatch/accepted)
        owner (completion-owner expected-record)
        query (assoc (attempt-query expected-record)
                     :outcome accepted
                     :$or [{:completion_owner {:$exists false}}
                           {:completion_owner owner}])
        {:keys [matched-count]}
        (await (extern-mongo/update-one! (dispatches-coll db)
                                         query
                                         {"$set" {:completion_owner owner}}))]
    (if (and (number? matched-count) (pos? matched-count))
      expected-record
      ;; Equal callbacks can both read accepted before one finishes. Confirm an
      ;; already-completed same attempt so the delayed equal callback remains an
      ;; idempotent receipt replay; never admit a replacement attempt.
      (let [current (await (find-dispatch! db (:dispatch/key expected-record)))]
        (when (and (= :dispatch/completed (:dispatch/outcome current))
                   (dispatch-law/same-attempt? current expected-record))
          current)))))

(defn- ^:async finish-completion!
  "Finish an exact completion-owned attempt after its receipt is durable."
  [db expected-record detail]
  (let [owner (completion-owner expected-record)
        completed (cond-> (assoc expected-record
                                 :dispatch/outcome :dispatch/completed)
                    (some? detail) (assoc :dispatch/detail detail))
        changes (cond-> {:outcome (get wire-by-outcome :dispatch/completed)}
                  (some? detail) (assoc :detail detail))
        {:keys [matched-count]}
        (await (extern-mongo/update-one!
                (dispatches-coll db)
                (assoc (attempt-query expected-record)
                       :outcome (get wire-by-outcome :dispatch/accepted)
                       :completion_owner owner)
                {"$set" changes
                 "$unset" {:completion_owner ""}}))]
    (if (and (number? matched-count) (pos? matched-count))
      (dispatch-law/assert-record! completed)
      ;; Two equal callbacks may finish concurrently. The loser still confirms
      ;; that this same attempt, rather than a replacement, is now completed.
      (let [current (await (find-dispatch! db (:dispatch/key expected-record)))]
        (when (and (= :dispatch/completed (:dispatch/outcome current))
                   (dispatch-law/same-attempt? current expected-record))
          current)))))

(defn- ^:async replace-claim!
  "Replace an admitted claim with a fresh attempt, or report nil.

   Compare-and-set on the outcome we just read. The query names that exact
   outcome, so if another pass replaced or resolved the claim in between, this
   update matches nothing and the caller re-reads instead of overwriting a live
   attempt. Admission is decided before this call by
   `law.translation-dispatch/replaceable-claim?`: ordinarily only failed or
   rejected claims qualify, with explicit candidate-unavailable recovery as the
   exception for completed or duplicate candidate-terminal claims.

   The previous attempt's `batch_id` and `detail` are unset, not merely left. A
   stale batch id would let the old batch's completion report resolve the new
   attempt, minting a receipt for a translation this attempt never produced."
  [db record observed-record]
  (let [{:keys [matched-count]}
        (await (extern-mongo/update-one!
                (dispatches-coll db)
                (assoc (attempt-query observed-record)
                       :outcome (get wire-by-outcome
                                     (:dispatch/outcome observed-record)))
                {"$set" {:outcome (get wire-by-outcome :dispatch/accepted)
                         :binding_edn (:binding_edn (encode-dispatch record))}
                 "$unset" {:batch_id "" :detail "" :completion_owner ""}}))]
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

   A replaceable existing claim is replaced rather than reported settled. See
   `law.translation-dispatch/replaceable-claim?`."
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

          (dispatch-law/replaceable-claim? record existing)
          (if-let [replaced (await (replace-claim! db record existing))]
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

(defn- approval-key
  "The unique-index value for one approval.

   Derived from `store/approval-identity` rather than restated, so durable and
   in-memory uniqueness cannot disagree about what counts as the same approval.
   `pr-str` keeps `:es` distinct from the string \"es\"."
  [approval]
  (pr-str (store/approval-identity approval)))

(defn- encode-approval
  [approval]
  {:approval_key (approval-key approval)
   :document (pr-str (:review/document approval))
   :garden (pr-str (:review/garden approval))
   :locale (name (:review/locale approval))
   :revision (:review/revision approval)
   :org_id (:review/org-id approval)
   :project (scope-value (:review/project approval))
   :approval_edn (pr-str approval)})

(defn- decode-approval
  [doc]
  (when doc
    (evidence-law/assert-approval! (edn/read-string (:approval_edn doc)))))

(defn- ^:async claim-approval!
  "Record `approval` unless its exact output is already approved.

   The insert IS the check, exactly as in `claim-dispatch!`. Read-then-write would
   let two reviewers clicking together both insert, leaving two records free to
   disagree about who approved."
  [db approval]
  (let [{:keys [inserted?]}
        (await (extern-mongo/insert-one-unique! (approvals-coll db)
                                               (encode-approval approval)))]
    (if inserted?
      {:approval/status :recorded :approval approval}
      {:approval/status :existing
       :approval (decode-approval
                  (first (await (extern-mongo/find-docs!
                                 (approvals-coll db)
                                 {:approval_key (approval-key approval)
                                  :limit 1}))))})))

(defn- ^:async read-approvals!
  [db scope]
  (mapv decode-approval
        (await (extern-mongo/find-docs! (approvals-coll db) (scope-query scope)))))

(defn- ^:async find-batch-only-dispatch!
  [db batch-id]
  (decode-dispatch
   (first (await (extern-mongo/find-docs! (dispatches-coll db)
                                          {:batch_id batch-id
                                           :limit 1})))))

(defn- ^:async find-receipt!
  [db unique-key]
  (decode-receipt
   (first (await (extern-mongo/find-docs! (receipts-coll db)
                                          {:receipt_key unique-key
                                           :limit 1})))))

(defn- ^:async find-existing-receipt!
  "Find a matching identity, including a row written before `receipt_key`.

   The dispatch key and scope use existing indexes to bound the legacy scan;
   the authoritative EDN is then checked against the complete shared identity.
   If historical retries already produced duplicates, their comparable receipt
   timestamps select the first fact deterministically."
  [db receipt]
  (let [query (assoc (scope-query {:org-id (:translation/org-id receipt)
                                  :project (:translation/project receipt)})
                     :dispatch_key (:translation/dispatch-key receipt))
        identity (store/receipt-identity receipt)]
    (->> (await (extern-mongo/find-docs! (receipts-coll db) query))
         (map decode-receipt)
         (filter #(= identity (store/receipt-identity %)))
         store/collapse-receipts!
         first)))

(defn- ^:async read-claimed-receipt!
  "Read the receipt a unique-index refusal says exists, once more if needed."
  [db receipt]
  (let [unique-key (receipt-key receipt)]
    (or (await (find-receipt! db unique-key))
        (await (find-receipt! db unique-key))
        (throw (ex-info "completed translation receipt exists but cannot be read"
                        {:cause :transient-store-inconsistency
                         :translation/receipt-identity
                         (store/receipt-identity receipt)})))))

(defn- ^:async claim-receipt!
  "Atomically admit `receipt`, returning the first equal fact on replay."
  [db receipt]
  (if-let [existing (await (find-existing-receipt! db receipt))]
    (store/first-receipt-or-conflict! existing receipt)
    (let [{:keys [inserted?]}
          (await (extern-mongo/insert-one-unique! (receipts-coll db)
                                                 (encode-receipt receipt)))]
      (if inserted?
        receipt
        (store/first-receipt-or-conflict!
         (await (read-claimed-receipt! db receipt))
         receipt)))))

(defn- ^:async visible-receipt!
  "Return `receipt` only when its attempt has fully settled."
  [db receipt]
  (if (:translation/dispatch-attempt-id receipt)
    (when (store/receipt-visible-for-dispatch?
           receipt
           (await (find-dispatch! db (:translation/dispatch-key receipt))))
      receipt)
    receipt))

(defn- ^:async read-receipts!
  "Every dispatch-admitted receipt in `scope`. Order-insensitive by contract:
   `domain.translation-evidence` resolves a re-translated revision with the
   evidence law's context-free total order, so no sort is required of the query.

   New attempt-bound receipts are joined to the current dispatch before they
   cross the store boundary. A receipt written just before a crash remains
   private until the same attempt finishes; historical receipts without an
   attempt id retain compatibility."
  [db scope]
  (let [receipts (mapv decode-receipt
                       (await (extern-mongo/find-docs!
                               (receipts-coll db) (scope-query scope))))
        visible-promises (mapv #(visible-receipt! db %) receipts)
        visible (await (js/Promise.all (to-array visible-promises)))]
    (store/collapse-receipts!
     (into [] (remove nil?) (array-seq visible)))))

(defn create-store
  "A durable `ITranslationEvidenceStore` bound to `db`."
  [db]
  (reify store/ITranslationEvidenceStore
    (reserve-dispatch! [_ record]
      (claim-dispatch! db (dispatch-law/assert-record! record)))

    (resolve-dispatch! [_ expected-record outcome detail]
      (let [checked-outcome (store/assert-ordinary-resolution-outcome! outcome)
            wire (get wire-by-outcome checked-outcome)
            checked (dispatch-law/assert-record! expected-record)
            updated (cond-> (assoc checked :dispatch/outcome checked-outcome)
                      (some? detail) (assoc :dispatch/detail detail))]
        (update-in-flight! db checked
                           (cond-> {:outcome wire}
                             (some? detail) (assoc :detail detail))
                           updated)))

    (bind-dispatch-batch! [_ expected-record batch-id]
      (let [checked (dispatch-law/assert-record! expected-record)]
        (update-in-flight! db checked {:batch_id batch-id}
                           (assoc checked :dispatch/batch-id batch-id))))

    (claim-dispatch-completion! [_ expected-record]
      (claim-completion! db (dispatch-law/assert-record! expected-record)))

    (finish-dispatch-completion! [_ expected-record detail]
      (finish-completion! db (dispatch-law/assert-record! expected-record) detail))

    (dispatch-for-key! [_ dispatch-key]
      (find-dispatch! db dispatch-key))

    (dispatch-for-batch-document! [_ batch-id document-wire-id]
      (find-batch-dispatch! db batch-id document-wire-id))

    (dispatch-for-batch! [_ batch-id]
      (find-batch-only-dispatch! db batch-id))

    (record-translation! [_ receipt]
      (claim-receipt! db (evidence-law/assert-receipt! receipt)))

    (completed-translations! [_ scope]
      (read-receipts! db scope))

    (record-approval! [_ approval]
      (claim-approval! db (evidence-law/assert-approval! approval)))

    (approvals! [_ scope]
      (read-approvals! db scope))))
