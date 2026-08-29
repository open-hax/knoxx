(ns knoxx.backend.domain.publication-receipts-test
  (:require [cljs.test :refer [deftest is testing]]
            [malli.core :as m]
            [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.domain.publication-receipts :as receipts]))

(def materialized
  {:receipt/type :publication/materialized
   :publication/id :knoxx.docs/probe-es
   :adapter/id :memory/target
   :idempotency/key "probe-key"
   :document/id :knoxx.docs/probe
   :target :knoxx.docs/promethean
   :locale :es
   :revision "abc123"
   :path "/docs/demo"
   :materialized/revision "abc123"
   :materialized/path "/docs/demo"})

;; ── 1 receipt completeness ────────────────────────────────────────────────

(deftest materialized-receipt-validates
  (is (true? (m/validate receipts/PublicationMaterializedReceipt materialized)))
  (testing "every identity field is required — a receipt must be explainable
            without joining it back to anything"
    (doseq [field [:idempotency/key :adapter/id :revision :path
                   :publication/id :document/id :target :locale
                   :materialized/revision :materialized/path]]
      (testing (str "omitting " field)
        (is (false? (m/validate receipts/PublicationMaterializedReceipt
                                (dissoc materialized field))))))))

;; ── 2 only concrete revisions ─────────────────────────────────────────────

(deftest materialized-receipt-rejects-selector-revision
  (doseq [field [:revision :materialized/revision]]
    (testing (str field " may not hold a selector token")
      (is (false? (m/validate receipts/PublicationMaterializedReceipt
                              (assoc materialized field :source/current))))
      (is (false? (m/validate receipts/PublicationMaterializedReceipt
                              (assoc materialized field ""))))))
  (testing "a malformed path is rejected too"
    (is (false? (m/validate receipts/PublicationMaterializedReceipt
                            (assoc materialized :path "docs/demo"))))))

;; ── 3 failure variants are distinct ───────────────────────────────────────

(deftest failed-blocked-noop-receipts-are-distinct
  (let [variants {:failed {:receipt/type :publication/failed
                           :failure/reason "adapter exploded"
                           :failure/drift? true}
                  :blocked {:receipt/type :publication/blocked
                            :blockers [:translation-missing]}
                  :noop {:receipt/type :publication/noop
                         :reason :garden-not-active}
                  :removed {:receipt/type :publication/removed
                            :removed/path "/docs/demo"}}]
    (doseq [[label receipt] variants]
      (testing label
        (is (false? (m/validate receipts/PublicationMaterializedReceipt receipt))
            "must not satisfy the materialization shape")
        (is (nil? (receipts/observed-materialization receipt))
            "and must never be mistaken for something being public")
        (is (false? (receipts/materialized? receipt)))))))

;; ── 4 the projection is exactly what the planner compares ─────────────────

(deftest observed-materialization-feeds-planner-drift
  (let [observed (receipts/observed-materialization materialized)]
    (is (= {:materialized/revision "abc123" :materialized/path "/docs/demo"} observed))
    (testing "the key set is the planner's own, not a parallel definition"
      (is (= (set receipts/drift-keys) (set (keys observed))))
      (let [intent {:publication/path "/docs/demo"}
            desired (plan/desired-materialization intent "abc123")]
        (is (= (set (keys desired)) (set (keys observed)))
            "desired and observed must be comparable by construction")
        (is (= desired observed))))))

(deftest observed-for-tracks-the-latest-receipt
  (let [removal {:receipt/type :publication/removed
                 :publication/id :knoxx.docs/probe-es
                 :removed/path "/docs/demo"}]
    (is (= {:materialized/revision "abc123" :materialized/path "/docs/demo"}
           (receipts/observed-for [materialized] :knoxx.docs/probe-es)))
    (testing "a removal after a publish leaves nothing observed"
      (is (nil? (receipts/observed-for [materialized removal] :knoxx.docs/probe-es))))
    (testing "another publication's receipts are ignored"
      (is (nil? (receipts/observed-for [materialized] :knoxx.docs/other))))))

;; ── a receipt that claims materialization must say what it materialized ────

(deftest a-partial-materialization-is-refused-not-projected
  (testing "checking only the discriminator projected {} — a truthy observation
            naming neither revision nor path, which then compared unequal to
            every desired state while asserting something was public"
    (doseq [partial-receipt [{:receipt/type :publication/materialized}
                             {:receipt/type :publication/materialized
                              :materialized/path "/docs/demo"}
                             {:receipt/type :publication/materialized
                              :materialized/revision "abc123"}
                             {:receipt/type :publication/materialized
                              :materialized/revision "abc123"
                              :materialized/path ""}]]
      (is (thrown? js/Error (receipts/observed-materialization partial-receipt))
          (str (pr-str partial-receipt) " must not project"))))
  (testing "while a complete one still projects exactly the planner's key set"
    (is (= (set receipts/drift-keys)
           (set (keys (receipts/observed-materialization materialized)))))))

