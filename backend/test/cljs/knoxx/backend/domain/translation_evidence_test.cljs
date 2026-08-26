(ns knoxx.backend.domain.translation-evidence-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.domain.translation-evidence :as domain]))

(defn- receipt
  [& {:keys [locale source-revision revision at]
      :or {locale :es
           source-revision "sha256-aaa111bbb222"
           revision "sha256-aaa111bbb222+es@batch-1"
           at "2026-08-22T09:00:00.000Z"}}]
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.docs/promethean
   :translation/source-locale :en
   :translation/locale locale
   :translation/source-revision source-revision
   :translation/revision revision
   :translation/dispatch-key "key-1"
   :translation/org-id "org-1"
   :translation/at at})

(deftest translated-revision-is-keyed-by-all-four-coordinates
  (let [evidence (domain/evidence {:receipts [(receipt)]})]
    (testing "the exact triple is found"
      (is (domain/translated-revision? evidence :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222")))

    (testing "no coordinate is ignored"
      (is (not (domain/translated-revision? evidence :knoxx.docs/other :knoxx.docs/promethean :es "sha256-aaa111bbb222")))
      (is (not (domain/translated-revision? evidence :knoxx.docs/probe :knoxx.docs/other :es "sha256-aaa111bbb222")))
      (is (not (domain/translated-revision? evidence :knoxx.docs/probe :knoxx.docs/promethean :fr "sha256-aaa111bbb222")))
      (is (not (domain/translated-revision? evidence :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-different"))))))

(deftest a-re-translation-wins-by-timestamp-not-arrival-order
  (let [older (receipt :revision "sha256-aaa111bbb222+es@batch-1"
                       :at "2026-08-22T09:00:00.000Z")
        newer (receipt :revision "sha256-aaa111bbb222+es@batch-2"
                       :at "2026-08-22T10:00:00.000Z")]
    (testing "newest wins when it arrives last"
      (is (= "sha256-aaa111bbb222+es@batch-2"
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [older newer]})
                                  :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222")))))

    (testing "newest still wins when it arrives first"
      ;; The property arrival-order indexing would get wrong. A store is free to
      ;; return rows in whatever order its query produced.
      (is (= "sha256-aaa111bbb222+es@batch-2"
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [newer older]})
                                  :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222")))))))

(deftest a-store-returning-garbage-is-refused-not-indexed
  (testing "an invalid receipt fails on the way in rather than becoming a fact"
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(dissoc (receipt) :translation/revision)]})))
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt :source-revision "source/current")]})))))

(deftest gate-facts-supply-only-the-half-this-card-owns
  (let [facts (domain/gate-facts (domain/evidence {:receipts [(receipt)]}))]
    (testing "the translation predicate is present and answers"
      (is (fn? (:translated-revision? facts)))
      (is ((:translated-revision? facts) :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222")))

    (testing "nothing else is fabricated"
      ;; Defaulting :approved? or :current-source-revision here would let a
      ;; caller forget to supply the real thing and still get a gate that
      ;; answers — admitting a publication on evidence nobody produced.
      (is (= [:translated-revision?] (keys facts))))))

(deftest the-gate-actually-consumes-these-facts
  ;; The integration that matters: these predicates are shaped for
  ;; domain.publication-gate, and nothing else proves that but calling it.
  (let [intent {:publication/id :knoxx.docs/probe-es
                :publication/document :knoxx.docs/probe
                :publication/garden :knoxx.docs/promethean
                :publication/locale :es
                :publication/revision "sha256-aaa111bbb222"
                :publication/state :published
                :publication/path "/probe"
                :translation/review :none
                :document/source-locale :en}
        facts-with (merge {:current-source-revision (constantly "sha256-aaa111bbb222")
                           :approved? (constantly false)
                           :source-revision-superseded? (constantly false)}
                          (domain/gate-facts (domain/evidence {:receipts [(receipt)]})))
        facts-without (merge {:current-source-revision (constantly "sha256-aaa111bbb222")
                              :approved? (constantly false)
                              :source-revision-superseded? (constantly false)}
                             (domain/gate-facts (domain/evidence {:receipts []})))]
    (testing "a translated revision clears the translation blocker"
      (let [decision (gate/gate intent facts-with)]
        (is (empty? (:blockers decision)))
        (is (:admissible? decision))
        (is (nil? (:translation-work decision)) "nothing left to translate")))

    (testing "an untranslated revision blocks and derives work"
      (let [decision (gate/gate intent facts-without)]
        (is (= [:translation-missing] (:blockers decision)))
        (is (not (:admissible? decision)))
        (is (= "sha256-aaa111bbb222"
               (get-in decision [:translation-work :action/with :revision]))
            "derived work carries the concrete revision, not a selector")))))

(deftest equal-timestamp-receipts-resolve-the-same-way-in-any-order
  ;; Two receipts recorded in the same millisecond used to leave the winner to
  ;; arrival order, which is exactly the store-order dependence indexing by
  ;; timestamp exists to remove.
  (let [same-ms "2026-08-22T09:00:00.000Z"
        a (receipt :revision "sha256-aaa111bbb222+es@batch-a" :at same-ms)
        b (receipt :revision "sha256-aaa111bbb222+es@batch-b" :at same-ms)]
    (testing "the same pair in either order picks the same receipt"
      (is (= (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [a b]})
                                  :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222"))
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [b a]})
                                  :knoxx.docs/probe :knoxx.docs/promethean :es "sha256-aaa111bbb222")))))))
