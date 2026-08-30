(ns knoxx.backend.infra.stores.mongo-translation-split
  "Durable MongoDB adapter for atomic translation turns and child evidence.

  One turn row is the pre-provider commit point. Its authoritative EDN value
  contains the dispatch/run binding, manifest, claim, execution policy, and
  pinned memory snapshot. Flat BSON fields exist only for exact selectors and
  indexes; reads authenticate the EDN and require every selector to agree.

  Turn identity and run identity are unique indexes on that one row, so an
  insert atomically admits the whole provider-visible aggregate. Candidate
  splits, complete sets, and review receipts are monotonic child facts. Their
  unique inserts are compare-and-set operations: an equal retry returns the
  index winner and changed reuse of an identity conflicts."
  (:require [clojure.edn :as edn]
            [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.translation-split-store :as store]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-schema :as schema]))

(def TURNS_COLLECTION
  "Collection holding atomic pre-provider translation turns."
  "knoxx_translation_turns")

(def CANDIDATE_SPLITS_COLLECTION
  "Collection holding immutable output for admitted split attempts."
  "knoxx_translation_candidate_splits")

(def CANDIDATE_SETS_COLLECTION
  "Collection holding complete candidate revisions, one per turn."
  "knoxx_translation_candidate_sets")

(def REVIEW_RECEIPTS_COLLECTION
  "Collection holding immutable, candidate-bound review history."
  "knoxx_translation_split_reviews")

(def ^:private no-project
  "Queryable sentinel for a manifest that names no project."
  "\u0000none")

(defn- turns-coll
  "Return the opaque turns collection handle through the Mongo extern."
  [db]
  (extern-mongo/collection db TURNS_COLLECTION))

(defn- candidate-splits-coll
  "Return the opaque candidate-split collection handle."
  [db]
  (extern-mongo/collection db CANDIDATE_SPLITS_COLLECTION))

(defn- candidate-sets-coll
  "Return the opaque candidate-set collection handle."
  [db]
  (extern-mongo/collection db CANDIDATE_SETS_COLLECTION))

(defn- review-receipts-coll
  "Return the opaque split-review collection handle."
  [db]
  (extern-mongo/collection db REVIEW_RECEIPTS_COLLECTION))

(defn- ^:async setup-turn-indexes!
  "Create the aggregate identity and future-memory scope indexes."
  [db]
  (let [turns (turns-coll db)]
    (await (extern-mongo/ensure-index! turns [[:turn_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index! turns [[:run_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index!
            turns
            [[:org_id 1] [:project 1]
             [:source_locale 1] [:target_locale 1]]
            {}))))

(defn- ^:async setup-child-indexes!
  "Create immutable child identity and relationship indexes."
  [db]
  (let [candidates (candidate-splits-coll db)
        candidate-sets (candidate-sets-coll db)
        reviews (review-receipts-coll db)]
    (await (extern-mongo/ensure-index! candidates [[:attempt_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index!
            candidates [[:claim_id 1] [:split_index 1]] {}))
    (await (extern-mongo/ensure-index!
            candidate-sets [[:candidate_set_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index!
            candidate-sets [[:turn_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index! reviews [[:review_id 1]] {:unique true}))
    (await (extern-mongo/ensure-index!
            reviews
            [[:candidate_set_id 1] [:split_id 1]
             [:recorded_at 1] [:operation_id 1] [:review_id 1]]
            {}))))

(defn ^:async setup-indexes!
  "Create correctness and exact-selector indexes. Idempotent.

  The unique turn and run indexes jointly make one inserted row the atomic
  pre-provider claim. Other unique indexes make child facts immutable without
  a read-before-write race."
  [db]
  (await (setup-turn-indexes! db))
  (await (setup-child-indexes! db))
  true)

;; ── Authoritative EDN codecs ───────────────────────────────────────────────

(defn- scope-value
  "Return a queryable flat selector for an optional project."
  [project]
  (or project no-project))

(defn- keyword-wire
  "Keep keyword namespaces intact in a scalar selector."
  [value]
  (pr-str value))

(defn- read-record!
  "Decode one required authoritative EDN column or surface corruption."
  [kind row record-column]
  (let [encoded (get row record-column)]
    (when-not (string? encoded)
      (throw (ex-info (str "stored " kind " has no authoritative EDN record")
                      {:translation-split-store/corrupt kind
                       :record-column record-column})))
    (try
      (edn/read-string encoded)
      (catch :default cause
        (throw (ex-info (str "stored " kind " EDN is unreadable")
                        {:translation-split-store/corrupt kind
                         :record-column record-column}
                        cause))))))

(defn- assert-selector-columns!
  "Require query columns to agree with their authoritative EDN fact."
  [kind row expected]
  (let [actual (select-keys row (keys expected))]
    (when-not (= expected actual)
      (throw (ex-info (str "stored " kind " selectors disagree with its EDN record")
                      {:translation-split-store/corrupt kind
                       :expected expected
                       :actual actual})))))

(defn- turn-manifest
  "Return the nested authenticated manifest from a turn."
  [turn]
  (:translation-turn/manifest turn))

(defn- turn-claim
  "Return the nested authenticated candidate claim from a turn."
  [turn]
  (:translation-turn/candidate-claim turn))

(defn- turn-selector-columns
  "Project every exact point, relationship, and memory selector from a turn."
  [turn]
  (let [manifest (turn-manifest turn)
        claim (turn-claim turn)]
    {:turn_id (:translation-turn/id turn)
     :run_id (:translation-turn/run-id turn)
     :dispatch_key (:translation-turn/dispatch-key turn)
     :admitted_at (:translation-turn/admitted-at turn)
     :org_id (:split-manifest/org-id manifest)
     :project (scope-value (:split-manifest/project manifest))
     :garden (keyword-wire (:split-manifest/garden manifest))
     :document (keyword-wire (:split-manifest/document manifest))
     :source_locale (keyword-wire (:split-manifest/source-locale manifest))
     :target_locale (keyword-wire (:split-manifest/target-locale manifest))
     :source_revision (:split-manifest/source-revision manifest)
     :manifest_id (:split-manifest/id manifest)
     :claim_id (:candidate-claim/id claim)
     :candidate_revision (:candidate-claim/revision claim)
     :execution_digest
     (get-in turn [:translation-turn/execution :translation-execution/digest])}))

(defn- encode-turn
  "Encode one atomic turn as query columns plus authoritative EDN."
  [turn]
  (assoc (turn-selector-columns turn) :turn_edn (pr-str turn)))

(defn- decode-turn
  "Decode, authenticate, and cross-check one stored turn."
  [digest-hex row]
  (when row
    (let [turn (->> (read-record! "translation turn" row :turn_edn)
                    (split/assert-turn-integrity! digest-hex))]
      (assert-selector-columns! "translation turn" row
                                (turn-selector-columns turn))
      turn)))

(defn- candidate-selector-columns
  "Project immutable candidate selectors with their admitted claim."
  [turn-id claim-id candidate]
  {:attempt_id (:candidate/attempt-id candidate)
   :turn_id turn-id
   :claim_id claim-id
   :split_id (:candidate/split-id candidate)
   :split_index (:candidate/split-index candidate)})

(defn- encode-candidate
  "Encode one immutable candidate and its claim relationship."
  [turn-id claim-id candidate]
  (assoc (candidate-selector-columns turn-id claim-id candidate)
         :candidate_edn (pr-str candidate)))

(defn- decode-candidate-record
  "Decode a candidate and cross-check its flat relationship selectors."
  [row]
  (when row
    (let [candidate (read-record! "candidate split" row :candidate_edn)
          turn-id (:turn_id row)
          claim-id (:claim_id row)]
      (assert-selector-columns!
       "candidate split" row
       (candidate-selector-columns turn-id claim-id candidate))
      {:turn-id turn-id :claim-id claim-id :candidate candidate})))

(defn- candidate-set-selector-columns
  "Project global set identity and its owning turn relationship."
  [turn-id candidate-set]
  {:candidate_set_id (:candidate-set/id candidate-set)
   :turn_id turn-id
   :manifest_id (:candidate-set/manifest-id candidate-set)
   :claim_id (:candidate-set/claim-id candidate-set)
   :candidate_revision (:candidate-set/revision candidate-set)})

(defn- encode-candidate-set
  "Encode one complete candidate set and its owning turn."
  [turn-id candidate-set]
  (assoc (candidate-set-selector-columns turn-id candidate-set)
         :candidate_set_edn (pr-str candidate-set)))

(defn- decode-candidate-set-record
  "Decode one set and cross-check selectors derivable from its EDN."
  [row]
  (when row
    (let [candidate-set (read-record! "candidate set" row :candidate_set_edn)
          turn-id (:turn_id row)]
      (assert-selector-columns!
       "candidate set" row
       (candidate-set-selector-columns turn-id candidate-set))
      {:turn-id turn-id :candidate-set candidate-set})))

(defn- review-selector-columns
  "Project exact review identity and history selectors."
  [receipt]
  {:review_id (:review/id receipt)
   :candidate_set_id (:review/candidate-set-id receipt)
   :split_id (:review/split-id receipt)
   :recorded_at (:review/recorded-at receipt)
   :operation_id (:review/operation-id receipt)})

(defn- encode-review
  "Encode one immutable review receipt as authoritative EDN."
  [receipt]
  (assoc (review-selector-columns receipt) :review_edn (pr-str receipt)))

(defn- decode-review-record
  "Decode one review receipt and cross-check its history selectors."
  [row]
  (when row
    (let [receipt (read-record! "split review receipt" row :review_edn)]
      (assert-selector-columns! "split review receipt" row
                                (review-selector-columns receipt))
      receipt)))

;; ── Unique-index compare-and-set support ──────────────────────────────────

(defn- ^:async one-row!
  "Return one exact row, nil for absence, and refuse violated uniqueness."
  [collection-handle query kind]
  (let [rows (await (extern-mongo/find-docs!
                     collection-handle (assoc query :limit 2)))]
    (when (> (count rows) 1)
      (throw (ex-info (str "multiple stored " kind " rows match one identity")
                      {:translation-split-store/corrupt kind
                       :query query})))
    (first rows)))

(defn- ^:async existing-after-collision!
  "Re-read a unique-index collision twice, then surface inconsistency."
  [read-existing kind identity]
  (or (await (read-existing))
      (await (read-existing))
      (throw (ex-info (str kind " identity exists but cannot be read")
                      {:translation-split-store/cause
                       :transient-store-inconsistency
                       :translation-split-store/kind kind
                       :translation-split-store/id identity}))))

(defn- ^:async put-immutable!
  "Insert one child fact or authenticate the unique-index winner."
  [collection-handle doc value kind identity read-existing equal-retry?]
  (let [{:keys [inserted?]}
        (await (extern-mongo/insert-one-unique! collection-handle doc))]
    (if inserted?
      value
      (let [existing (await (existing-after-collision!
                             read-existing kind identity))]
        (if (equal-retry? existing value)
          existing
          (store/immutable-conflict! kind identity existing value))))))

(defn- exact-retry?
  "Whether a child retry is byte-for-byte the admitted immutable fact."
  [existing attempted]
  (= existing attempted))

;; ── Atomic turn reads and admission ───────────────────────────────────────

(defn- ^:async turn-by-identity!
  "Read one authenticated turn by exact turn identity."
  [db digest-hex turn-id]
  (decode-turn digest-hex
               (await (one-row! (turns-coll db)
                                {:turn_id turn-id}
                                "translation turn"))))

(defn- ^:async turn-by-run!
  "Read one authenticated turn by exact provider run identity."
  [db digest-hex run-id]
  (decode-turn digest-hex
               (await (one-row! (turns-coll db)
                                {:run_id run-id}
                                "translation turn"))))

(defn- ^:async required-turn!
  "Read an authenticated turn or refuse an orphaned child fact."
  [db digest-hex turn-id]
  (or (await (turn-by-identity! db digest-hex turn-id))
      (throw (ex-info "translation turn is not persisted"
                      {:translation-turn/id turn-id}))))

(defn- ^:async turn-collision-once!
  "Resolve both unique turn selectors and require them to agree."
  [db digest-hex turn-id run-id]
  (let [by-id (await (turn-by-identity! db digest-hex turn-id))
        by-run (await (turn-by-run! db digest-hex run-id))]
    (when (and by-id by-run (not= by-id by-run))
      (store/inconsistent-index! "turn" [turn-id run-id]))
    (or by-id by-run)))

(defn- ^:async turn-after-collision!
  "Re-read a turn/run unique collision with one bounded retry."
  [db digest-hex turn-id run-id]
  (or (await (turn-collision-once! db digest-hex turn-id run-id))
      (await (turn-collision-once! db digest-hex turn-id run-id))
      (throw (ex-info "translation turn identity exists but cannot be read"
                      {:translation-split-store/cause
                       :transient-store-inconsistency
                       :translation-turn/id turn-id
                       :translation-turn/run-id run-id}))))

(defn- ^:async admit-turn-in-mongo!
  "Atomically claim both aggregate and provider run identity with one insert."
  [db digest-hex turn]
  (let [checked (split/assert-turn-integrity! digest-hex turn)
        turn-id (:translation-turn/id checked)
        run-id (:translation-turn/run-id checked)
        {:keys [inserted?]}
        (await (extern-mongo/insert-one-unique!
                (turns-coll db) (encode-turn checked)))]
    (if inserted?
      checked
      (let [existing (await (turn-after-collision!
                             db digest-hex turn-id run-id))]
        (if (= existing checked)
          existing
          (store/immutable-conflict! "translation turn" [turn-id run-id]
                                     existing checked))))))

;; ── Candidate members and complete sets ───────────────────────────────────

(defn- index-candidates!
  "Index candidates while refusing conflicting duplicate attempt rows."
  [candidates]
  (reduce (fn [by-attempt candidate]
            (let [attempt-id (:candidate/attempt-id candidate)]
              (if-let [existing (get by-attempt attempt-id)]
                (if (= existing candidate)
                  by-attempt
                  (store/immutable-conflict! "candidate split" attempt-id
                                             existing candidate))
                (assoc by-attempt attempt-id candidate))))
          {}
          candidates))

(defn- ^:async candidate-by-attempt!
  "Read and authenticate one candidate against its stored claim relationship."
  [db digest-hex attempt-id]
  (when-let [row (await (one-row! (candidate-splits-coll db)
                                  {:attempt_id attempt-id}
                                  "candidate split"))]
    (let [{:keys [turn-id claim-id candidate]} (decode-candidate-record row)
          candidate-turn (await (required-turn! db digest-hex turn-id))]
      (when-not candidate-turn
        (throw (ex-info "candidate split has no admitted turn authority"
                        {:candidate/attempt-id attempt-id
                         :candidate-claim/id claim-id})))
      (when-not (= claim-id (:candidate-claim/id (turn-claim candidate-turn)))
        (throw (ex-info "candidate split claim selector disagrees with its turn"
                        {:candidate/attempt-id attempt-id
                         :candidate-claim/id claim-id
                         :translation-turn/id turn-id})))
      (store/checked-candidate-for-turn digest-hex candidate-turn candidate))))

(defn- ^:async candidates-for-turn!
  "Read present candidates in stable admitted claim order."
  [db digest-hex turn]
  (let [claim (turn-claim turn)
        claim-id (:candidate-claim/id claim)
        rows (await (extern-mongo/find-docs!
                     (candidate-splits-coll db) {:claim_id claim-id}))
        candidates
        (mapv (fn [row]
                (let [{stored-claim-id :claim-id candidate :candidate}
                      (decode-candidate-record row)]
                  (when-not (= claim-id stored-claim-id)
                    (throw (ex-info "candidate query escaped its exact claim"
                                    {:expected-claim/id claim-id
                                     :actual-claim/id stored-claim-id})))
                  (store/checked-candidate-for-turn digest-hex turn candidate)))
              rows)
        by-attempt (index-candidates! candidates)]
    (into []
          (keep #(get by-attempt (:candidate-claim-member/attempt-id %)))
          (:candidate-claim/members claim))))

(defn- ^:async append-candidate-in-mongo!
  "Append one immutable candidate bound to an admitted turn."
  [db digest-hex turn-id candidate]
  (let [turn (await (required-turn! db digest-hex turn-id))
        claim-id (:candidate-claim/id (turn-claim turn))
        checked (store/checked-candidate-for-turn digest-hex turn candidate)
        attempt-id (:candidate/attempt-id checked)]
    (await (put-immutable!
            (candidate-splits-coll db)
            (encode-candidate turn-id claim-id checked)
            checked "candidate split" attempt-id
            (fn [] (candidate-by-attempt! db digest-hex attempt-id))
            exact-retry?))))

(defn- ^:async checked-candidate-set-for-turn!
  "Authenticate a complete set through its turn and durable member evidence."
  [db digest-hex turn candidate-set]
  (let [shaped (schema/assert-valid! :translation-split/candidate-set
                                     schema/CandidateSet candidate-set)
        manifest (turn-manifest turn)
        claim (turn-claim turn)
        persisted (await (candidates-for-turn! db digest-hex turn))]
    (when-not (= [(:split-manifest/id manifest)
                  (:candidate-claim/id claim)
                  (:candidate-claim/revision claim)]
                 [(:candidate-set/manifest-id shaped)
                  (:candidate-set/claim-id shaped)
                  (:candidate-set/revision shaped)])
      (throw (ex-info "complete candidate set does not match its admitted turn"
                      {:translation-turn/id (:translation-turn/id turn)
                       :candidate-set/id (:candidate-set/id shaped)})))
    (store/assert-persisted-candidate-coverage! shaped persisted)
    (split/assert-candidate-set-integrity! digest-hex manifest shaped)))

(defn- ^:async checked-candidate-set-record!
  "Authenticate a decoded set through the exact owning turn in its row."
  [db digest-hex row]
  (when row
    (let [{:keys [turn-id candidate-set]} (decode-candidate-set-record row)
          turn (await (required-turn! db digest-hex turn-id))]
      {:turn turn
       :candidate-set
       (await (checked-candidate-set-for-turn!
               db digest-hex turn candidate-set))})))

(defn- ^:async candidate-set-record-by-id!
  "Read one authenticated set record by exact global set identity."
  [db digest-hex candidate-set-id]
  (await (checked-candidate-set-record!
          db digest-hex
          (await (one-row! (candidate-sets-coll db)
                           {:candidate_set_id candidate-set-id}
                           "candidate set")))))

(defn- ^:async candidate-set-record-for-turn!
  "Read one authenticated set record by exact owning turn."
  [db digest-hex turn-id]
  (await (checked-candidate-set-record!
          db digest-hex
          (await (one-row! (candidate-sets-coll db)
                           {:turn_id turn-id}
                           "candidate set")))))

(defn- ^:async required-candidate-set-record!
  "Read an authenticated set and owning turn or refuse missing authority."
  [db digest-hex candidate-set-id]
  (or (await (candidate-set-record-by-id!
              db digest-hex candidate-set-id))
      (throw (ex-info "complete translation candidate set is not persisted"
                      {:candidate-set/id candidate-set-id}))))

(defn- ^:async set-collision-once!
  "Resolve both unique set selectors and require their rows to agree."
  [db digest-hex turn-id candidate-set-id]
  (let [by-id (await (candidate-set-record-by-id!
                      db digest-hex candidate-set-id))
        by-turn (await (candidate-set-record-for-turn!
                        db digest-hex turn-id))]
    (when (and by-id by-turn (not= by-id by-turn))
      (store/inconsistent-index! "candidate set" [turn-id candidate-set-id]))
    (or by-id by-turn)))

(defn- ^:async set-after-collision!
  "Re-read a set/turn unique collision with one bounded retry."
  [db digest-hex turn-id candidate-set-id]
  (or (await (set-collision-once!
              db digest-hex turn-id candidate-set-id))
      (await (set-collision-once!
              db digest-hex turn-id candidate-set-id))
      (throw (ex-info "candidate set identity exists but cannot be read"
                      {:translation-split-store/cause
                       :transient-store-inconsistency
                       :translation-turn/id turn-id
                       :candidate-set/id candidate-set-id}))))

(defn- ^:async complete-set-in-mongo!
  "Admit one complete set after authenticating exact durable coverage."
  [db digest-hex turn-id candidate-set]
  (let [turn (await (required-turn! db digest-hex turn-id))
        checked (await (checked-candidate-set-for-turn!
                        db digest-hex turn candidate-set))
        candidate-set-id (:candidate-set/id checked)
        {:keys [inserted?]}
        (await (extern-mongo/insert-one-unique!
                (candidate-sets-coll db)
                (encode-candidate-set turn-id checked)))]
    (if inserted?
      checked
      (let [{existing-turn :turn existing :candidate-set}
            (await (set-after-collision!
                    db digest-hex turn-id candidate-set-id))]
        (if (and (= turn-id (:translation-turn/id existing-turn))
                 (= existing checked))
          existing
          (store/immutable-conflict! "candidate set"
                                     [turn-id candidate-set-id]
                                     existing checked))))))

;; ── Review history and applicable memory ──────────────────────────────────

(defn- ^:async review-by-id!
  "Read and authenticate one review receipt by exact immutable identity."
  [db digest-hex review-id]
  (when-let [row (await (one-row! (review-receipts-coll db)
                                  {:review_id review-id}
                                  "review receipt"))]
    (let [receipt (decode-review-record row)
          {:keys [turn candidate-set]}
          (await (required-candidate-set-record!
                  db digest-hex (:review/candidate-set-id receipt)))]
      (split/assert-review-receipt-integrity!
       digest-hex (turn-manifest turn) candidate-set receipt))))

(defn- ^:async append-review-in-mongo!
  "Append one review; an equal operation retry keeps the first timestamp."
  [db digest-hex receipt]
  (let [shaped (schema/assert-valid! :translation-split/review-receipt
                                     schema/SplitReviewReceipt receipt)
        candidate-set-id (:review/candidate-set-id shaped)
        {:keys [turn candidate-set]}
        (await (required-candidate-set-record!
                db digest-hex candidate-set-id))
        checked (split/assert-review-receipt-integrity!
                 digest-hex (turn-manifest turn) candidate-set shaped)
        review-id (:review/id checked)]
    (await (put-immutable!
            (review-receipts-coll db) (encode-review checked) checked
            "review receipt" review-id
            (fn [] (review-by-id! db digest-hex review-id))
            store/same-review-operation?))))

(defn- unique-review-history!
  "Dedupe equal rows and refuse conflicting duplicate review identities."
  [receipts]
  (vals
   (reduce (fn [by-id receipt]
             (let [review-id (:review/id receipt)]
               (if-let [existing (get by-id review-id)]
                 (if (= existing receipt)
                   by-id
                   (store/immutable-conflict! "review receipt" review-id
                                              existing receipt))
                 (assoc by-id review-id receipt))))
           {}
           receipts)))

(defn- ^:async review-history!
  "Read complete authenticated history for one exact candidate-set split."
  [db digest-hex turn candidate-set split-id]
  (store/assert-candidate-split-selector! candidate-set split-id)
  (let [rows (await (extern-mongo/find-docs!
                     (review-receipts-coll db)
                     {:candidate_set_id (:candidate-set/id candidate-set)
                      :split_id split-id}))]
    (->> rows
         (map decode-review-record)
         (map #(split/assert-review-receipt-integrity!
                digest-hex (turn-manifest turn) candidate-set %))
         unique-review-history!
         (sort-by (juxt :review/recorded-at :review/operation-id :review/id))
         vec)))

(defn- memory-scope-query
  "Return the exact flat Mongo query for tenant and language scope."
  [{:keys [org-id project source-locale target-locale]}]
  {:org_id org-id
   :project (scope-value project)
   :source_locale (keyword-wire source-locale)
   :target_locale (keyword-wire target-locale)})

(defn- ^:async examples-for-set!
  "Project every currently approved split example in one complete set."
  [db digest-hex turn candidate-set]
  (loop [remaining (:candidate-set/members candidate-set)
         examples []]
    (if-let [candidate (first remaining)]
      (let [split-id (:candidate/split-id candidate)
            history (await (review-history!
                            db digest-hex turn candidate-set split-id))
            example (split/approved-memory-example
                     digest-hex (turn-manifest turn)
                     candidate-set split-id history)]
        (recur (next remaining) (cond-> examples example (conj example))))
      examples)))

(defn- ^:async scoped-turns!
  "Read and authenticate all turns matching one exact memory scope query."
  [db digest-hex scope]
  (mapv #(decode-turn digest-hex %)
        (await (extern-mongo/find-docs!
                (turns-coll db) (memory-scope-query scope)))))

(defn- ^:async applicable-memory-in-mongo!
  "Derive bounded current approved examples for a future translation."
  [db digest-hex scope]
  (let [{:keys [garden exclude-manifest-id current-candidate-set-ids limit]
         :as checked} (store/checked-memory-scope scope)
        turns (await (scoped-turns! db digest-hex checked))]
    (loop [remaining turns
           examples []]
      (if-let [turn (first remaining)]
        (let [manifest-id (:split-manifest/id (turn-manifest turn))
              set-record (when-not (= exclude-manifest-id manifest-id)
                           (await (candidate-set-record-for-turn!
                                   db digest-hex (:translation-turn/id turn))))
              turn-examples
              (if (and set-record
                       (contains? current-candidate-set-ids
                                  (get-in set-record
                                          [:candidate-set :candidate-set/id])))
                (await (examples-for-set!
                        db digest-hex turn (:candidate-set set-record)))
                [])]
          (recur (next remaining) (into examples turn-examples)))
        (->> examples
             distinct
             (sort-by (juxt #(if (= garden
                                    (:translation-memory/garden %))
                               0 1)
                            :translation-memory/id))
             (take limit)
             vec)))))

(defn- ^:async candidates-for-turn-id!
  "Resolve a turn id and return its present candidates in admitted order."
  [db digest-hex turn-id]
  (let [turn (await (required-turn! db digest-hex turn-id))]
    (await (candidates-for-turn! db digest-hex turn))))

(defn- ^:async candidate-set-value-for-turn!
  "Return one turn's authenticated candidate set without adapter metadata."
  [db digest-hex turn-id]
  (some-> (await (candidate-set-record-for-turn! db digest-hex turn-id))
          :candidate-set))

(defn- ^:async candidate-set-value-by-id!
  "Return one authenticated candidate set by exact global identity."
  [db digest-hex candidate-set-id]
  (some-> (await (candidate-set-record-by-id!
                  db digest-hex candidate-set-id))
          :candidate-set))

(defn- ^:async turn-value-for-candidate-set!
  "Return the authenticated owning turn for one exact global set identity."
  [db digest-hex candidate-set-id]
  (some-> (await (candidate-set-record-by-id!
                  db digest-hex candidate-set-id))
          :turn))

(defn- ^:async review-history-by-selector!
  "Resolve a complete set and read one exact split history."
  [db digest-hex candidate-set-id split-id]
  (let [{:keys [turn candidate-set]}
        (await (required-candidate-set-record!
                db digest-hex candidate-set-id))]
    (await (review-history! db digest-hex turn candidate-set split-id))))

;; ── Public adapter ─────────────────────────────────────────────────────────

(defrecord MongoTranslationSplitStore [db digest-hex]
  store/ITranslationSplitStore
  (admit-turn! [_ turn]
    (admit-turn-in-mongo! db digest-hex turn))

  (turn-for-run! [_ run-id]
    (turn-by-run! db digest-hex run-id))

  (turn-by-id! [_ turn-id]
    (turn-by-identity! db digest-hex turn-id))

  (append-candidate-split! [_ turn-id candidate]
    (append-candidate-in-mongo! db digest-hex turn-id candidate))

  (candidate-splits-for-turn! [_ turn-id]
    (candidates-for-turn-id! db digest-hex turn-id))

  (complete-candidate-set! [_ turn-id candidate-set]
    (complete-set-in-mongo! db digest-hex turn-id candidate-set))

  (candidate-set-for-turn! [_ turn-id]
    (candidate-set-value-for-turn! db digest-hex turn-id))

  (candidate-set-by-id! [_ candidate-set-id]
    (candidate-set-value-by-id! db digest-hex candidate-set-id))

  (turn-for-candidate-set! [_ candidate-set-id]
    (turn-value-for-candidate-set! db digest-hex candidate-set-id))

  (append-review-receipt! [_ receipt]
    (append-review-in-mongo! db digest-hex receipt))

  (review-history-for-split! [_ candidate-set-id split-id]
    (review-history-by-selector! db digest-hex candidate-set-id split-id))

  (applicable-memory! [_ scope]
    (applicable-memory-in-mongo! db digest-hex scope)))

(defn create-store
  "Create a durable Mongo translation split store using `digest-hex`."
  [db digest-hex]
  (when-not db
    (throw (ex-info "translation split store requires a Mongo database" {})))
  (when-not (fn? digest-hex)
    (throw (ex-info "translation split store requires a digest function" {})))
  (->MongoTranslationSplitStore db digest-hex))
