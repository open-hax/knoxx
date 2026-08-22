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

(def ^:private record
  (dispatch-law/dispatch-record
   {:document :knoxx.docs/probe
    :locale :es
    :revision "sha256-aaa111bbb222"
    :replace-stale? false}
   {:dispatch/garden "knoxx.docs/promethean"
    :dispatch/document-wire-id "knoxx.docs/probe"
    :dispatch/source-locale :en
    :dispatch/org-id "org-1"
    :dispatch/membership-id "member-1"}
   :dispatch/accepted
   at))

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-aaa111bbb222"
   :translation/revision "sha256-aaa111bbb222+es@batch-1"
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
                 changes (get (js->clj update-doc :keywordize-keys true) :$set)
                 index (first (keep-indexed (fn [i row] (when (matches? q row) i)) @rows))]
             (if index
               (do (swap! rows update index merge changes)
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

(deftest ^:async a-resolved-claim-reports-done-rather-than-in-flight
  (let [{:keys [store]} (fixture)
        _ (await (store/reserve-dispatch! store record))
        _ (await (store/resolve-dispatch! store (:dispatch/key record)
                                          :dispatch/completed nil))
        again (await (store/reserve-dispatch! store record))]
    (testing "the distinction the gate needs between running and finished"
      (is (= :done (:reservation/status again)))
      (is (= :dispatch/completed (:dispatch/outcome (:record again)))))))

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
        read-back (await (store/completed-translations! store))]
    (testing "the receipt comes back identical"
      (is (= [receipt] read-back)))

    (testing "a second receipt is appended, not merged"
      (await (store/record-translation!
              store (assoc receipt
                           :translation/revision "sha256-aaa111bbb222+es@batch-2"
                           :translation/at "2026-08-22T10:00:00.000Z")))
      (is (= 2 (count (await (store/completed-translations! store))))))))

(def ^:private approval
  {:review/state :approved
   :review/document :knoxx.docs/probe
   :review/locale :es
   :review/revision "sha256-aaa111bbb222"
   :review/translation-revision "sha256-aaa111bbb222+es@batch-1"
   :review/org-id "org-1"
   :review/project "knoxx-session"
   :review/principal {:principal/user-email "reviewer@open-hax.local"}
   :review/at at})

(deftest ^:async approvals-round-trip-with-namespaced-keywords-intact
  (let [{:keys [store approvals]} (fixture)
        recorded (await (store/record-approval! store approval))]
    (testing "the approval is recorded and comes back identical"
      (is (= :recorded (:approval/status recorded)))
      (is (= [approval] (await (store/approvals! store)))))

    (testing "the principal survives persistence"
      ;; A principal flattened by JSON would be attribution that no longer names
      ;; anybody.
      (is (= "reviewer@open-hax.local"
             (:principal/user-email (:review/principal
                                     (first (await (store/approvals! store))))))))

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
