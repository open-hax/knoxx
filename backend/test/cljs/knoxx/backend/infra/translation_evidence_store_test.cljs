(ns knoxx.backend.infra.translation-evidence-store-test
  "Focused transition tests shared by every in-memory evidence-store caller."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(def ^:private work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-aaa111bbb222"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"})

(defn- accepted-record
  [at & {:keys [attempt-id recovery-reason]}]
  (dispatch-law/dispatch-record
   work context :dispatch/accepted at
   :attempt-id (or attempt-id (str "dispatch-attempt-" at))
   :recovery-reason recovery-reason))

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.docs/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-aaa111bbb222"
   :translation/revision "sha256-aaa111bbb222+es@batch-1"
   :translation/content-digest "sha256-target-content"
   :translation/dispatch-key "dispatch-key-1"
   :translation/org-id "org-1"
   :translation/at "2026-08-22T09:00:00.000Z"})

(deftest ^:async completed-claims-require-explicit-recovery-in-memory
  (let [evidence-store (store/memory-store)
        original (accepted-record "2026-08-22T09:00:00.000Z")
        _ (await (store/reserve-dispatch! evidence-store original))
        bound (await (store/bind-dispatch-batch!
                      evidence-store original "batch-old"))
        _ (await (store/claim-dispatch-completion! evidence-store bound))
        _ (await (store/finish-dispatch-completion!
                  evidence-store bound "candidate was originally present"))
        ordinary (await (store/reserve-dispatch!
                         evidence-store
                         (accepted-record "2026-08-22T10:00:00.000Z")))
        recovery (accepted-record
                  "2026-08-22T11:00:00.000Z"
                  :recovery-reason :candidate-unavailable)
        reopened (await (store/reserve-dispatch! evidence-store recovery))
        repeated (await (store/reserve-dispatch! evidence-store recovery))]
    (testing "an ordinary duplicate cannot reopen completed"
      (is (= :done (:reservation/status ordinary)))
      (is (= :dispatch/completed
             (:dispatch/outcome (:record ordinary)))))

    (testing "the validated recovery marker replaces completed wholesale"
      (is (= :reserved (:reservation/status reopened)))
      (is (= recovery (:record reopened)))
      (is (= :candidate-unavailable
             (:dispatch/recovery-reason (:record reopened))))
      (is (not (contains? (:record reopened) :dispatch/batch-id)))
      (is (not (contains? (:record reopened) :dispatch/detail))))

    (testing "the atom admits the recovery once and keeps its new claim in flight"
      (is (= :in-flight (:reservation/status repeated)))
      (is (= recovery (:record repeated)))
      (is (= recovery
             (await (store/dispatch-for-key!
                     evidence-store (:dispatch/key original))))))))

(deftest ^:async stale-attempts-cannot-bind-or-resolve-a-replacement-in-memory
  (let [evidence-store (store/memory-store)
        admitted-at "2026-08-22T09:00:00.000Z"
        attempt-a (accepted-record admitted-at :attempt-id "dispatch-attempt-a")
        _ (await (store/reserve-dispatch! evidence-store attempt-a))
        bound-a (await (store/bind-dispatch-batch!
                        evidence-store attempt-a "batch-a"))
        _ (await (store/resolve-dispatch! evidence-store bound-a
                                          :dispatch/failed "attempt A failed"))
        attempt-b (accepted-record admitted-at :attempt-id "dispatch-attempt-b")
        _ (await (store/reserve-dispatch! evidence-store attempt-b))
        bound-b (await (store/bind-dispatch-batch!
                        evidence-store attempt-b "batch-b"))]
    (testing "a delayed A cannot settle or bind B despite key and millisecond reuse"
      (is (nil? (await (store/resolve-dispatch! evidence-store bound-a
                                                :dispatch/failed "late A"))))
      (is (nil? (await (store/bind-dispatch-batch!
                        evidence-store bound-a "batch-a-late")))))

    (testing "the replacement attempt remains authoritative"
      (is (= bound-b
             (await (store/dispatch-for-key!
                     evidence-store (:dispatch/key attempt-b))))))))

(deftest ^:async provisional-receipts-stay-hidden-until-their-attempt-finishes
  (let [evidence-store (store/memory-store)
        attempt (accepted-record "2026-08-22T09:00:00.000Z")
        _ (await (store/reserve-dispatch! evidence-store attempt))
        bound (await (store/bind-dispatch-batch! evidence-store attempt "batch-1"))
        completed-receipt (dispatch-law/translation-receipt
                           bound (dispatch-law/output-revision bound)
                           "2026-08-22T09:05:00.000Z"
                           "sha256-target-content")
        _ (await (store/claim-dispatch-completion! evidence-store bound))
        _ (await (store/record-translation! evidence-store completed-receipt))]
    (testing "a racing failure cannot invalidate an owned completion"
      (is (nil? (await (store/resolve-dispatch! evidence-store bound
                                                :dispatch/failed "late failure")))))

    (testing "crash-after-receipt evidence is not visible to production snapshots"
      (is (empty? (await (store/completed-translations!
                          evidence-store {:org-id "org-1" :project nil})))))

    (testing "the same completion claim resumes and makes its receipt visible"
      (is (= bound (await (store/claim-dispatch-completion!
                           evidence-store bound))))
      (let [completed (await (store/finish-dispatch-completion!
                              evidence-store bound nil))]
        (is (= :dispatch/completed (:dispatch/outcome completed)))
        (is (= completed
               (await (store/claim-dispatch-completion!
                       evidence-store bound)))))
      (is (= [completed-receipt]
             (await (store/completed-translations!
                     evidence-store {:org-id "org-1" :project nil})))))))

(deftest ^:async completed-receipt-retries-keep-the-first-fact-in-memory
  (let [evidence-store (store/memory-store)
        first-result (await (store/record-translation! evidence-store receipt))
        later-retry (assoc receipt :translation/at "2026-08-22T10:00:00.000Z")
        replay-result (await (store/record-translation! evidence-store later-retry))]
    (testing "timestamp-only replay returns the first stored receipt"
      (is (= receipt first-result))
      (is (= receipt replay-result))
      (is (= (:translation/at receipt) (:translation/at replay-result))))

    (testing "the stable coordinates expose one deterministic identity"
      (is (= (store/receipt-identity receipt)
             (store/receipt-identity later-retry)))
      (doseq [[field changed]
              [[:translation/org-id "org-2"]
               [:translation/project "project-2"]
               [:translation/garden :knoxx.docs/other-garden]
               [:translation/document :knoxx.docs/other-document]
               [:translation/source-locale :de]
               [:translation/locale :fr]
               [:translation/source-revision "source-2"]
               [:translation/revision "output-2"]
               [:translation/dispatch-key "dispatch-key-2"]]]
        (is (not= (store/receipt-identity receipt)
                  (store/receipt-identity (assoc receipt field changed)))
            (str "identity omitted " field))))

    (testing "changed facts on the same identity conflict without appending"
      (try
        (await (store/record-translation!
                evidence-store
                (assoc later-retry :translation/content-digest "sha256-forged")))
        (is false "changed receipt at one identity must fail")
        (catch :default error
          (is (= :translation-receipt-conflict (:cause (ex-data error))))))
      (is (= [receipt]
             (await (store/completed-translations!
                     evidence-store {:org-id "org-1" :project nil})))))))
