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
   :translation/source-locale :en
   :translation/locale locale
   :translation/source-revision source-revision
   :translation/revision revision
   :translation/dispatch-key "key-1"
   :translation/org-id "org-1"
   :translation/at at})

(deftest translated-revision-is-keyed-by-all-three-coordinates
  (let [evidence (domain/evidence {:receipts [(receipt)]})]
    (testing "the exact triple is found"
      (is (domain/translated-revision? evidence :knoxx.docs/probe :es "sha256-aaa111bbb222")))

    (testing "no coordinate is ignored"
      (is (not (domain/translated-revision? evidence :knoxx.docs/other :es "sha256-aaa111bbb222")))
      (is (not (domain/translated-revision? evidence :knoxx.docs/probe :fr "sha256-aaa111bbb222")))
      (is (not (domain/translated-revision? evidence :knoxx.docs/probe :es "sha256-different"))))))

(deftest a-re-translation-wins-by-timestamp-not-arrival-order
  (let [older (receipt :revision "sha256-aaa111bbb222+es@batch-1"
                       :at "2026-08-22T09:00:00.000Z")
        newer (receipt :revision "sha256-aaa111bbb222+es@batch-2"
                       :at "2026-08-22T10:00:00.000Z")]
    (testing "newest wins when it arrives last"
      (is (= "sha256-aaa111bbb222+es@batch-2"
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [older newer]})
                                  :knoxx.docs/probe :es "sha256-aaa111bbb222")))))

    (testing "newest still wins when it arrives first"
      ;; The property arrival-order indexing would get wrong. A store is free to
      ;; return rows in whatever order its query produced.
      (is (= "sha256-aaa111bbb222+es@batch-2"
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [newer older]})
                                  :knoxx.docs/probe :es "sha256-aaa111bbb222")))))))

(deftest a-store-returning-garbage-is-refused-not-indexed
  (testing "an invalid receipt fails on the way in rather than becoming a fact"
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(dissoc (receipt) :translation/revision)]})))
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt :source-revision "source/current")]})))))

(defn- approval
  [& {:keys [locale revision translation-revision at]
      :or {locale :es
           revision "sha256-aaa111bbb222"
           translation-revision "sha256-aaa111bbb222+es@batch-1"
           at "2026-08-22T09:30:00.000Z"}}]
  {:review/state :approved
   :review/document :knoxx.docs/probe
   :review/locale locale
   :review/revision revision
   :review/translation-revision translation-revision
   :review/org-id "org-1"
   :review/principal {:principal/user-email "reviewer@open-hax.local"}
   :review/at at})

(deftest gate-facts-supply-both-evidential-halves-and-nothing-else
  (let [facts (domain/gate-facts (domain/evidence {:receipts [(receipt)]
                                                   :approvals [(approval)]}))]
    (testing "both evidential predicates are present and answer"
      (is (fn? (:translated-revision? facts)))
      (is (fn? (:approved? facts)))
      (is ((:translated-revision? facts) :knoxx.docs/probe :es "sha256-aaa111bbb222"))
      (is ((:approved? facts) :knoxx.docs/probe :es "sha256-aaa111bbb222")))

    (testing "the source-revision facts are not fabricated"
      ;; Defaulting :current-source-revision here would let a caller forget to
      ;; supply the real thing and still get a gate that answers — admitting a
      ;; publication on evidence nobody produced.
      (is (= #{:translated-revision? :approved?} (set (keys facts)))))))

(deftest approval-alone-is-never-enough
  (testing "an approval with no receipt behind it counts for nothing"
    ;; Approval is evidence for the gate, not a blanket permission. With no
    ;; translation recorded there is nothing it can attest to.
    (is (not (domain/approved? (domain/evidence {:receipts [] :approvals [(approval)]})
                               :knoxx.docs/probe :es "sha256-aaa111bbb222")))))

(deftest an-approval-is-specific-to-one-document-locale-and-revision
  (let [evidence (domain/evidence {:receipts [(receipt)] :approvals [(approval)]})]
    (testing "the approved triple is approved"
      (is (domain/approved? evidence :knoxx.docs/probe :es "sha256-aaa111bbb222")))

    (testing "it does not satisfy another document, locale, or revision"
      (is (not (domain/approved? evidence :knoxx.docs/other :es "sha256-aaa111bbb222")))
      (is (not (domain/approved? evidence :knoxx.docs/probe :fr "sha256-aaa111bbb222")))
      (is (not (domain/approved? evidence :knoxx.docs/probe :es "sha256-different"))))))

(deftest a-re-translation-supersedes-its-approval
  (let [older (receipt :revision "sha256-aaa111bbb222+es@batch-1"
                       :at "2026-08-22T09:00:00.000Z")
        newer (receipt :revision "sha256-aaa111bbb222+es@batch-2"
                       :at "2026-08-22T10:00:00.000Z")
        approved-older (approval :translation-revision "sha256-aaa111bbb222+es@batch-1")]
    (testing "the approval satisfies the translation it was given for"
      (is (domain/approved? (domain/evidence {:receipts [older]
                                              :approvals [approved-older]})
                            :knoxx.docs/probe :es "sha256-aaa111bbb222")))

    (testing "it stops satisfying the gate once the translation is replaced"
      ;; Not deleted and not an error — it simply stops being current. This is
      ;; what keeps an approval from authorizing bytes nobody reviewed, and the
      ;; reason the output revision is batch-specific.
      (is (not (domain/approved? (domain/evidence {:receipts [older newer]
                                                   :approvals [approved-older]})
                                 :knoxx.docs/probe :es "sha256-aaa111bbb222"))))

    (testing "approving the replacement restores admissibility"
      (is (domain/approved?
           (domain/evidence
            {:receipts [older newer]
             :approvals [approved-older
                         (approval :translation-revision "sha256-aaa111bbb222+es@batch-2")]})
           :knoxx.docs/probe :es "sha256-aaa111bbb222")))))

(deftest a-store-returning-invalid-approvals-is-refused
  (testing "an approval missing its translation revision cannot be indexed"
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt)]
                                   :approvals [(dissoc (approval)
                                                       :review/translation-revision)]}))))

  (testing "an approval attributed to nobody is refused"
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt)]
                                   :approvals [(assoc (approval) :review/principal {})]}))))

  (testing "an approval naming no tenant is refused"
    ;; Review evidence that named no organization would be admissible in every
    ;; one of them.
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt)]
                                   :approvals [(dissoc (approval) :review/org-id)]}))))

  (testing "a non-approved review state cannot masquerade as an approval"
    (is (thrown? js/Error
                 (domain/evidence {:receipts [(receipt)]
                                   :approvals [(assoc (approval)
                                                      :review/state :rejected)]})))))

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
        source-facts {:current-source-revision (constantly "sha256-aaa111bbb222")
                      :source-revision-superseded? (constantly false)}
        facts-with (merge source-facts
                          (domain/gate-facts (domain/evidence {:receipts [(receipt)]})))
        facts-without (merge source-facts
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
                                  :knoxx.docs/probe :es "sha256-aaa111bbb222"))
             (:translation/revision
              (domain/receipt-for (domain/evidence {:receipts [b a]})
                                  :knoxx.docs/probe :es "sha256-aaa111bbb222")))))))
