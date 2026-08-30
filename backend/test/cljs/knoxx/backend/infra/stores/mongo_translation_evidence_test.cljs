(ns knoxx.backend.infra.stores.mongo-translation-evidence-test
  "The durable store, against a fake collection handle.

  What is worth testing here is not Mongo — it is the two decisions this store
  makes that a database cannot make for it: that namespaced keywords survive
  persistence, and that a unique-index refusal is read as 'somebody else
  claimed it' rather than as an outage."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-translation-evidence :as mongo-store]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(def ^:private at "2026-08-22T09:00:00.000Z")

(def ^:private approval-scope
  "The scope the approval fixtures in this suite belong to."
  {:org-id "org-1" :project "knoxx-session"})

(def ^:private evidence-scope
  "The tenant and project every fixture record here belongs to."
  {:org-id "org-1" :project nil})

(def ^:private dispatch-work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-aaa111bbb222"
   :replace-stale? false})

(def ^:private dispatch-context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"})

(def ^:private record
  (dispatch-law/dispatch-record
   dispatch-work dispatch-context :dispatch/accepted at))

(defn- recovery-record
  []
  (dispatch-law/dispatch-record
   dispatch-work dispatch-context :dispatch/accepted
   "2026-08-22T10:00:00.000Z"
   :recovery-reason :candidate-unavailable))

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.docs/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-aaa111bbb222"
   :translation/revision "sha256-aaa111bbb222+es@batch-1"
   :translation/content-digest "sha256-target-content"
   :translation/dispatch-key (:dispatch/key record)
   :translation/org-id "org-1"
   :translation/at at})

(defn- duplicate-key-error []
  (let [err (js/Error. "E11000 duplicate key error")]
    (aset err "code" 11000)
    err))

(defn- fake-collection
  "A collection handle over one atom, honoring a unique index on `unique-field`.

   A nil `unique-field` means no unique index, which is what the receipts
   collection actually has: receipts are append-only facts and several of them
   legitimately share one dispatch key, so enforcing uniqueness there would test
   a constraint the real store does not declare.

   Only the driver surface `extern.mongo` actually calls: insertOne, updateOne,
   and find returning a cursor with toArray."
  [rows unique-field]
  (letfn [(matches? [query row]
            (every? (fn [[k v]] (= v (get row k))) query))]
    #js {:insertOne
         (fn [doc]
           (let [row (js->clj doc :keywordize-keys true)]
             (if (and unique-field
                      (some #(= (get % unique-field) (get row unique-field)) @rows))
               (js/Promise.reject (duplicate-key-error))
               (do (swap! rows conj row)
                   (js/Promise.resolve #js {})))))
         :updateOne
         ;; Named `update-doc`, not `update`: the latter shadows
         ;; `clojure.core/update`, which this body calls one line down.
         (fn [query update-doc]
           (let [q (js->clj query :keywordize-keys true)
                 update-map (js->clj update-doc :keywordize-keys true)
                 changes (get update-map :$set)
                 unset-fields (keys (get update-map :$unset))
                 index (first (keep-indexed (fn [i row] (when (matches? q row) i)) @rows))]
             (if index
               (do (swap! rows update index
                          (fn [row]
                            (apply dissoc (merge row changes) unset-fields)))
                   (js/Promise.resolve #js {"matchedCount" 1 "modifiedCount" 1}))
               (js/Promise.resolve #js {"matchedCount" 0 "modifiedCount" 0}))))
         :find
         (fn [query]
           (let [q (dissoc (js->clj query :keywordize-keys true) :limit)]
             #js {:limit (fn [_] (js-obj "toArray"
                                         (fn [] (js/Promise.resolve
                                                 (clj->js (filterv #(matches? q %) @rows))))))
                  :toArray (fn [] (js/Promise.resolve
                                   (clj->js (filterv #(matches? q %) @rows))))}))}))

(defn- fixture []
  (let [dispatches (atom [])
        receipts (atom [])
        approvals (atom [])
        db #js {:collection
                (fn [name]
                  (if (= name mongo-store/DISPATCHES_COLLECTION)
                    (fake-collection dispatches :dispatch_key)
                    (if (= name mongo-store/APPROVALS_COLLECTION)
                      (fake-collection approvals :approval_key)
                      (fake-collection receipts nil))))}]
    {:dispatches dispatches
     :receipts receipts
     :approvals approvals
     :store (mongo-store/create-store db)}))

(deftest ^:async namespaced-keywords-survive-persistence
  (let [{:keys [store dispatches]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        read-back (await (store/dispatch-for-key! store (:dispatch/key record)))]
    (testing "the record comes back identical, keyword namespaces intact"
      ;; The failure this guards is recorded in the store's docstring: JSON
      ;; erases keyword namespaces, and that erasure has already produced one
      ;; live defect in this constellation.
      (is (= record read-back))
      (is (= :knoxx.docs/probe (:dispatch/document read-back)))
      (is (= :es (:dispatch/locale read-back)))
      (is (= :dispatch/accepted (:dispatch/outcome read-back))))

    (testing "the queryable columns exist beside the authoritative EDN"
      (let [row (first @dispatches)]
        (is (= (:dispatch/key record) (:dispatch_key row)))
        (is (= "knoxx.docs/probe" (:document_wire_id row)))
        (is (= "dispatch/accepted" (:outcome row)))
        (is (string? (:binding_edn row)))))))

(deftest ^:async a-unique-index-refusal-is-a-claim-not-an-outage
  (let [{:keys [store dispatches]} (fixture)
        first-claim (await (store/reserve-dispatch! store record))
        second-claim (await (store/reserve-dispatch! store record))]
    (testing "the first caller reserves"
      (is (= :reserved (:reservation/status first-claim))))

    (testing "the second is told the claim is in flight, not that the write failed"
      (is (= :in-flight (:reservation/status second-claim)))
      (is (= record (:record second-claim))))

    (testing "only one row exists"
      (is (= 1 (count @dispatches))))))

(deftest ^:async an-ordinary-proposal-cannot-reopen-a-completed-claim
  (let [{:keys [store]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        _ (await (store/resolve-dispatch! store (:dispatch/key record)
                                          :dispatch/completed nil))
        again (await (store/reserve-dispatch! store record))]
    (testing "completed remains terminal when no recovery reason was established"
      (is (= :done (:reservation/status again)))
      (is (= :dispatch/completed (:dispatch/outcome (:record again)))))))

(deftest ^:async candidate-unavailable-recovery-is-an-atomic-completed-cas
  (let [{:keys [store dispatches]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        _ (await (store/bind-dispatch-batch!
                  store (:dispatch/key record) "batch-old"))
        _ (await (store/resolve-dispatch!
                  store (:dispatch/key record) :dispatch/completed "old detail"))
        recovery (recovery-record)
        ;; Start both calls before awaiting either. Both are allowed to observe
        ;; completed, but the outcome predicate in updateOne lets exactly one
        ;; compare-and-set it back to accepted.
        first-promise (store/reserve-dispatch! store recovery)
        second-promise (store/reserve-dispatch! store recovery)
        first-result (await first-promise)
        second-result (await second-promise)
        stored (await (store/dispatch-for-key! store (:dispatch/key record)))]
    (testing "exactly one recovery caller owns the fresh attempt"
      (is (= {:reserved 1 :in-flight 1}
             (frequencies (map :reservation/status
                               [first-result second-result])))))

    (testing "the replacement is the marked accepted record"
      (is (= :dispatch/accepted (:dispatch/outcome stored)))
      (is (= :candidate-unavailable (:dispatch/recovery-reason stored)))
      (is (= (:dispatch/at recovery) (:dispatch/at stored))))

    (testing "recovery clears mutable evidence belonging to the old attempt"
      (is (not (contains? stored :dispatch/batch-id)))
      (is (not (contains? stored :dispatch/detail))))

    (testing "the unique key still has exactly one durable row"
      (is (= 1 (count @dispatches))))))

(deftest ^:async mutable-fields-are-columns-so-two-writers-cannot-clobber
  (let [{:keys [store]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        bound (await (store/bind-dispatch-batch! store (:dispatch/key record) "batch-7"))
        resolved (await (store/resolve-dispatch! store (:dispatch/key record)
                                                 :dispatch/completed "done"))]
    (testing "binding a batch does not disturb the immutable binding"
      (is (= "batch-7" (:dispatch/batch-id bound)))
      (is (= "sha256-aaa111bbb222" (:dispatch/revision bound))))

    (testing "resolving keeps the batch id the other write had already set"
      ;; Stored as one EDN blob, this is where a read-modify-write would have
      ;; dropped whichever field the loser wrote.
      (is (= "batch-7" (:dispatch/batch-id resolved)))
      (is (= :dispatch/completed (:dispatch/outcome resolved)))
      (is (= "done" (:dispatch/detail resolved))))))

(deftest ^:async only-an-in-flight-claim-can-be-resolved
  (let [{:keys [store]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        _ (await (store/resolve-dispatch! store (:dispatch/key record)
                                          :dispatch/completed nil))
        second-resolve (await (store/resolve-dispatch! store (:dispatch/key record)
                                                       :dispatch/failed "late"))]
    (testing "the loser of the race learns it lost instead of overwriting"
      (is (nil? second-resolve)))

    (testing "the terminal outcome stands"
      (is (= :dispatch/completed
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        store (:dispatch/key record)))))))))

(deftest ^:async an-unknown-outcome-is-refused-rather-than-defaulted
  (let [{:keys [store]} (fixture)]
    (testing "a store cannot be asked to write an outcome outside the contract"
      (is (thrown? js/Error (store/resolve-dispatch! store "any-key"
                                                     :dispatch/invented nil))))))

(deftest ^:async the-batch-document-join-finds-the-binding
  (let [{:keys [store]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        _ (await (store/bind-dispatch-batch! store (:dispatch/key record) "batch-7"))]
    (testing "the join the worker's report is resolved through"
      (is (= "sha256-aaa111bbb222"
             (:dispatch/revision (await (store/dispatch-for-batch-document!
                                         store "batch-7" "knoxx.docs/probe"))))))

    (testing "a different batch or document does not match"
      (is (nil? (await (store/dispatch-for-batch-document!
                        store "batch-9" "knoxx.docs/probe"))))
      (is (nil? (await (store/dispatch-for-batch-document!
                        store "batch-7" "knoxx.docs/other")))))))

(deftest ^:async receipts-round-trip-as-append-only-facts
  (let [{:keys [store]} (fixture)
        _ (await (store/record-translation! store receipt))
        read-back (await (store/completed-translations! store evidence-scope))]
    (testing "the receipt comes back identical"
      (is (= [receipt] read-back)))

    (testing "a second receipt is appended, not merged"
      (await (store/record-translation!
              store (assoc receipt
                           :translation/revision "sha256-aaa111bbb222+es@batch-2"
                           :translation/at "2026-08-22T10:00:00.000Z")))
      (is (= 2 (count (await (store/completed-translations! store evidence-scope))))))))

(def ^:private approval
  {:review/state :approved
   :review/document :knoxx.docs/probe
   :review/garden :knoxx.docs/promethean
   :review/source-locale :en
   :review/locale :es
   :review/revision "sha256-aaa111bbb222"
   :review/translation-revision "sha256-aaa111bbb222+es@batch-1"
   :review/content-digest "sha256-target-content"
   :review/org-id "org-1"
   :review/project "knoxx-session"
   :review/principal {:principal/user-email "reviewer@open-hax.local"}
   :review/at at})

(deftest ^:async approvals-round-trip-with-namespaced-keywords-intact
  (let [{:keys [store approvals]} (fixture)
        recorded (await (store/record-approval! store approval))]
    (testing "the approval is recorded and comes back identical"
      (is (= :recorded (:approval/status recorded)))
      (is (= [approval] (await (store/approvals! store approval-scope)))))

    (testing "the principal survives persistence"
      ;; A principal flattened by JSON would be attribution that no longer names
      ;; anybody.
      (is (= "reviewer@open-hax.local"
             (:principal/user-email (:review/principal
                                     (first (await (store/approvals! store approval-scope))))))))

    (testing "the queryable columns exist beside the authoritative EDN"
      (let [row (first @approvals)]
        (is (string? (:approval_key row)))
        (is (= "es" (:locale row)))
        (is (string? (:approval_edn row)))))))

(deftest ^:async a-unique-index-makes-recording-an-approval-idempotent
  (let [{:keys [store approvals]} (fixture)
        first-result (await (store/record-approval! store approval))
        second-result (await (store/record-approval! store approval))]
    (testing "the second attempt is recognized rather than appended"
      (is (= :recorded (:approval/status first-result)))
      (is (= :existing (:approval/status second-result)))
      (is (= approval (:approval second-result))))

    (testing "only one row exists"
      (is (= 1 (count @approvals))))))

(deftest ^:async approving-a-different-output-is-a-different-approval
  (let [{:keys [store approvals]} (fixture)
        _ (await (store/record-approval! store approval))
        other (await (store/record-approval!
                      store (assoc approval :review/translation-revision
                                   "sha256-aaa111bbb222+es@batch-2")))]
    (testing "a re-translation is a new act of review over different bytes"
      (is (= :recorded (:approval/status other)))
      (is (= 2 (count @approvals))))))

(deftest ^:async approvals-in-different-scopes-do-not-collide
  (let [{:keys [store approvals]} (fixture)
        _ (await (store/record-approval! store approval))
        other-org (await (store/record-approval!
                          store (assoc approval :review/org-id "org-2")))
        other-project (await (store/record-approval!
                              store (assoc approval :review/project "other")))]
    (testing "the tenant and project are part of the approval identity"
      (is (= :recorded (:approval/status other-org)))
      (is (= :recorded (:approval/status other-project)))
      (is (= 3 (count @approvals))))))

(deftest ^:async a-claim-the-index-refuses-but-cannot-be-read-is-surfaced
  ;; The unique index said a row with this key exists. A nil read means a
  ;; transient inconsistency, and falling through to `:done` with a nil record
  ;; made `dispatch-work!` report a duplicate — stranding the work silently with
  ;; no claim to observe and no outcome to read.
  (let [dispatches (atom [])
        db #js {:collection
                (fn [_name]
                  ;; Refuses every insert and returns no rows: the pathological
                  ;; combination the branch exists for.
                  #js {:insertOne (fn [_] (js/Promise.reject (duplicate-key-error)))
                       :find (fn [_] #js {:limit (fn [_] (js-obj "toArray"
                                                                 (fn [] (js/Promise.resolve #js []))))
                                          :toArray (fn [] (js/Promise.resolve #js []))})})}
        store (mongo-store/create-store db)]
    (testing "the inconsistency is thrown rather than reported as a duplicate"
      (try
        (await (store/reserve-dispatch! store record))
        (is false "an unreadable claim must not be reported as settled")
        (catch :default err
          (is (= :transient-store-inconsistency (:cause (ex-data err))))
          (is (= (:dispatch/key record) (:dispatch/key (ex-data err)))))))
    (is (empty? @dispatches))))

(deftest ^:async receipts-are-narrowed-in-the-query-not-in-memory
  ;; Receipts are append-only, so reading them all and filtering afterwards made
  ;; every dispatch pass grow with the global history of every tenant — and left
  ;; the collection's own indexes unused.
  (let [{:keys [store receipts]} (fixture)
        scoped (fn [org project]
                 (cond-> (assoc receipt :translation/org-id org)
                   project (assoc :translation/project project)
                   (nil? project) (dissoc :translation/project)))]
    (await (store/record-translation! store (scoped "org-1" nil)))
    (await (store/record-translation! store (assoc (scoped "org-2" nil)
                                                   :translation/dispatch-key "k2")))
    (await (store/record-translation! store (assoc (scoped "org-1" "other")
                                                   :translation/dispatch-key "k3")))

    (testing "a tenant sees only its own receipts"
      (is (= ["org-1"] (mapv :translation/org-id
                             (await (store/completed-translations!
                                     store {:org-id "org-1" :project nil}))))))

    (testing "a project is its own scope, not a wildcard"
      ;; The nil-project read must not pick up the row naming a project, and the
      ;; named-project read must not pick up the row naming none.
      (is (= 1 (count (await (store/completed-translations!
                              store {:org-id "org-1" :project nil})))))
      (is (= 1 (count (await (store/completed-translations!
                              store {:org-id "org-1" :project "other"}))))))

    (testing "the scope reaches the stored row as queryable columns"
      (is (every? #(contains? % :org_id) @receipts))
      (is (every? #(contains? % :project) @receipts)))

    (testing "an unrelated tenant sees nothing"
      (is (empty? (await (store/completed-translations!
                          store {:org-id "org-3" :project nil})))))))
