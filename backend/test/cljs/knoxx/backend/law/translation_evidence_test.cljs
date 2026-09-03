(ns knoxx.backend.law.translation-evidence-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.law.translation-evidence :as law]
            [malli.core :as m]))

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.docs/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-abc123def456"
   :translation/revision "sha256-abc123def456+es@batch-7"
   :translation/content-digest "sha256-target-content"
   :translation/dispatch-key "key-1"
   :translation/org-id "org-1"
   :translation/at "2026-08-22T09:00:00.000Z"})

(deftest restated-primitives-agree-with-publication-law
  (testing "the duplicated string law accepts and refuses exactly what publication law does"
    ;; The portability duplication is only safe while the two agree. This is the
    ;; test the law's docstring promises.
    (doseq [value ["rev-1" "  padded  " "" "   " nil :keyword 7]]
      (is (= (m/validate publication-law/NonBlankString value)
             (m/validate law/NonBlankString value))
          (str "disagreement on " (pr-str value)))))

  (testing "the selector namespace is the same token"
    (is (= publication-law/revision-selector-namespace
           law/revision-selector-namespace))))

(deftest concrete-revision-refuses-selectors-in-both-shapes
  (testing "the keyword selector publication law already refused"
    (is (not (m/validate law/ConcreteRevision :source/current)))
    (is (not (m/validate law/ConcreteRevision :source/anything))))

  (testing "the string selector only this boundary can receive"
    ;; The reason this law exists: a worker or a request body carries strings,
    ;; and publication law's nonblank-string revision accepts this one.
    (is (m/validate publication-law/ConcreteRevision "source/current")
        "publication law accepts it, which is why the refusal is restated here")
    (is (not (m/validate law/ConcreteRevision "source/current")))
    (is (not (m/validate law/ConcreteRevision ":source/current")))
    (is (not (m/validate law/ConcreteRevision "  source/current  "))))

  (testing "an ordinary revision containing a slash is not a selector"
    (is (m/validate law/ConcreteRevision "refs/heads/main"))
    (is (m/validate law/ConcreteRevision "sha256-abc123def456"))))

(deftest instants-are-one-comparable-format
  (testing "what toISOString emits is admissible"
    (is (m/validate law/Instant "2026-08-22T09:00:00.000Z"))
    (is (m/validate law/Instant (.toISOString (js/Date. 0)))))

  (testing "formats that break lexicographic ordering are refused"
    (is (not (m/validate law/Instant "2026-08-22T09:00:00Z")) "no milliseconds")
    (is (not (m/validate law/Instant "2026-08-22T11:00:00.000+02:00")) "offset")
    (is (not (m/validate law/Instant "2026-08-22")) "date only"))

  (testing "a well-formed but calendar-impossible timestamp is refused"
    ;; The pattern alone admits these, and because instants are compared
    ;; lexically to decide which translation is newest, a nonsense value would
    ;; sort above every real one and win.
    (is (not (m/validate law/Instant "2026-19-99T99:99:99.999Z")))
    (is (not (m/validate law/Instant "2026-13-01T00:00:00.000Z")) "month 13")
    (is (not (m/validate law/Instant "2026-00-01T00:00:00.000Z")) "month 0")
    (is (not (m/validate law/Instant "2026-01-32T00:00:00.000Z")) "day 32")
    (is (not (m/validate law/Instant "2026-01-00T00:00:00.000Z")) "day 0")
    (is (not (m/validate law/Instant "2026-02-30T00:00:00.000Z")) "February 30")
    (is (not (m/validate law/Instant "2026-04-31T00:00:00.000Z")) "April 31")
    (is (not (m/validate law/Instant "2026-01-01T24:00:00.000Z")) "hour 24")
    (is (not (m/validate law/Instant "2026-01-01T00:60:00.000Z")) "minute 60"))

  (testing "leap years are calculated, not approximated"
    (is (m/validate law/Instant "2024-02-29T00:00:00.000Z") "2024 is a leap year")
    (is (not (m/validate law/Instant "2026-02-29T00:00:00.000Z")) "2026 is not")
    (is (m/validate law/Instant "2000-02-29T00:00:00.000Z") "2000: divisible by 400")
    (is (not (m/validate law/Instant "1900-02-29T00:00:00.000Z")) "1900: divisible by 100"))

  (testing "a leap second is admitted"
    ;; A real timestamp source can emit second 60, and refusing it would reject
    ;; a moment that actually happened.
    (is (m/validate law/Instant "2016-12-31T23:59:60.000Z")))

  (testing "later-instant? orders by time, with nil earliest"
    (is (law/later-instant? "2026-08-22T09:00:00.001Z" "2026-08-22T09:00:00.000Z"))
    (is (not (law/later-instant? "2026-08-22T09:00:00.000Z" "2026-08-22T09:00:00.001Z")))
    (is (not (law/later-instant? "2026-08-22T09:00:00.000Z" "2026-08-22T09:00:00.000Z"))
        "equal is not later, so an index keeps the first of two identical stamps")
    (is (law/later-instant? "2026-08-22T09:00:00.000Z" nil))
    (is (not (law/later-instant? nil "2026-08-22T09:00:00.000Z")))
    (is (not (law/later-instant? nil nil)))))

(deftest completed-receipt-contract
  (testing "a well-formed receipt validates"
    (is (= receipt (law/assert-receipt! receipt))))

  (testing "a selector revision cannot reach a receipt"
    (is (thrown? js/Error (law/assert-receipt!
                           (assoc receipt :translation/source-revision "source/current"))))
    (is (thrown? js/Error (law/assert-receipt!
                           (assoc receipt :translation/revision :source/current)))))

  (testing "a translation into its own source locale is not evidence"
    ;; publication-gate/translation-required? decides translation is needed
    ;; exactly when the locales differ, so this receipt could only satisfy a
    ;; gate that never asked for it.
    (is (thrown? js/Error (law/assert-receipt!
                           (assoc receipt :translation/locale :en)))))

  (testing "an unformatted timestamp is refused"
    (is (thrown? js/Error (law/assert-receipt!
                           (assoc receipt :translation/at "2026-08-22T09:00:00Z")))))

  (testing "every identity field is required"
    (doseq [field [:translation/document :translation/source-locale :translation/locale
                   :translation/source-revision :translation/revision
                   :translation/dispatch-key :translation/at]]
      (is (thrown? js/Error (law/assert-receipt! (dissoc receipt field)))
          (str "missing " field " was accepted"))))

  (testing "historical receipts without split lineage remain readable"
    (is (= receipt (law/assert-receipt! receipt))))

  (testing "split-backed receipts require the entire immutable lineage"
    (let [lineage {:translation/split-manifest-id "manifest-1"
                   :translation/candidate-claim-id "claim-1"
                   :translation/candidate-set-id "set-1"
                   :translation/candidate-set-digest "sha256-set"
                   :translation/split-count 3
                   :translation/split-turn-admitted-at
                   "2026-08-22T08:00:00.000Z"}
          split-backed (merge receipt lineage)]
      (is (= split-backed (law/assert-receipt! split-backed)))
      (doseq [field (keys lineage)]
        (is (thrown? js/Error
                     (law/assert-receipt! (dissoc split-backed field)))
            (str "partial lineage missing " field " was accepted")))
      (is (thrown? js/Error
                   (law/assert-receipt!
                    (assoc split-backed :translation/split-count 0)))
          "an empty manifest cannot produce a completed split receipt")
      (is (thrown? js/Error
                   (law/assert-receipt!
                    (assoc split-backed :translation/split-review-order
                           [["2026-08-22T09:00:00.000Z" "review-1"]])))
          "review coordinates must cover every manifest split")
      (testing "legacy two-coordinate rollout rows remain readable"
        (is (law/assert-receipt!
             (assoc split-backed :translation/split-review-order
                    [[nil nil]
                     ["2026-08-22T09:00:00.000Z" "review-1"]
                     ["2026-08-22T09:01:00.000Z" "review-2"]]))))
      (testing "new projections retain the operation rank used by composition"
        (is (law/assert-receipt!
             (assoc split-backed :translation/split-review-order
                    [[nil nil nil]
                     ["2026-08-22T09:00:00.000Z" "bulk-a:split:1" "review-z"]
                     ["2026-08-22T09:01:00.000Z" "bulk-b:split:2" "review-a"]])))))))

(deftest supersedes-is-a-total-order
  (let [older (assoc receipt :translation/at "2026-08-22T09:00:00.000Z")
        newer (assoc receipt :translation/at "2026-08-22T10:00:00.000Z")]
    (testing "later wins in both directions of comparison"
      (is (law/supersedes? newer older))
      (is (not (law/supersedes? older newer))))

    (testing "anything supersedes nothing"
      (is (law/supersedes? older nil)))

    (testing "an equal timestamp is broken on the output revision, not on order"
      ;; Strict comparison kept whichever arrived first, which is arrival-order
      ;; dependence smuggled back in through the tie. The tiebreak is arbitrary
      ;; but deterministic — two stores returning the same rows in different
      ;; orders must reach the same answer.
      (let [tie-a (assoc older :translation/revision "rev-a")
            tie-b (assoc older :translation/revision "rev-b")]
        (is (law/supersedes? tie-b tie-a))
        (is (not (law/supersedes? tie-a tie-b)))))

    (testing "identical receipts do not supersede each other"
      (is (not (law/supersedes? older older))))

    (testing "review order wins inside one set, independent of request wall time"
      (let [base (merge older
                        {:translation/candidate-set-id "set-1"
                         :translation/split-turn-admitted-at
                         "2026-08-22T08:00:00.000Z"})
            earlier-review (assoc base
                                  :translation/at "2026-08-22T12:00:00.000Z"
                                  :translation/split-review-order [[nil nil]])
            later-review (assoc base
                                :translation/at "2026-08-22T11:00:00.000Z"
                                :translation/split-review-order
                                [["2026-08-22T10:00:00.000Z" "review-2"]])]
        (is (law/supersedes? later-review earlier-review))
        (is (not (law/supersedes? earlier-review later-review)))))

    (testing "same-millisecond projection order follows operation before receipt id"
      ;; Deliberately oppose the coordinates: group B has the lexically smaller
      ;; receipt id. Composition chooses B by operation id, and completed evidence
      ;; must choose that same projection rather than the hash-shaped review id.
      (let [base (merge older
                        {:translation/candidate-set-id "set-1"
                         :translation/split-turn-admitted-at
                         "2026-08-22T08:00:00.000Z"})
            group-a (assoc base
                           :translation/revision "projection-a"
                           :translation/split-review-order
                           [["2026-08-22T10:00:00.000Z"
                             "bulk-group-a:split:1" "review-z"]])
            group-b (assoc base
                           :translation/revision "projection-b"
                           :translation/split-review-order
                           [["2026-08-22T10:00:00.000Z"
                             "bulk-group-b:split:1" "review-a"]])]
        (is (law/supersedes? group-b group-a))
        (is (not (law/supersedes? group-a group-b)))))

    (testing "a fresh candidate run wins over a delayed review of the old set"
      (let [old-reviewed (merge newer
                                {:translation/candidate-set-id "set-old"
                                 :translation/split-turn-admitted-at
                                 "2026-08-22T08:00:00.000Z"
                                 :translation/split-review-order
                                 [["2026-08-22T12:00:00.000Z" "review-old"]]})
            fresh-raw (merge older
                             {:translation/candidate-set-id "set-new"
                              :translation/split-turn-admitted-at
                              "2026-08-22T09:00:00.000Z"})]
        (is (law/supersedes? fresh-raw old-reviewed))
        (is (not (law/supersedes? old-reviewed fresh-raw)))))

    (testing "legacy and split receipts cannot form a comparison cycle"
      (let [old-reviewed (merge receipt
                                {:translation/revision "rev-a"
                                 :translation/at "2026-08-22T20:00:00.000Z"
                                 :translation/candidate-set-id "set-old"
                                 :translation/split-turn-admitted-at
                                 "2026-08-22T08:00:00.000Z"})
            legacy (assoc receipt
                          :translation/revision "rev-b"
                          :translation/at "2026-08-22T15:00:00.000Z")
            fresh-raw (merge receipt
                             {:translation/revision "rev-c"
                              :translation/at "2026-08-22T10:00:00.000Z"
                              :translation/candidate-set-id "set-new"
                              :translation/split-turn-admitted-at
                              "2026-08-22T09:00:00.000Z"})
            values [old-reviewed legacy fresh-raw]]
        (doseq [a values b values c values]
          (when (and (law/supersedes? a b)
                     (law/supersedes? b c))
            (is (law/supersedes? a c)
                (str "non-transitive order: "
                     (mapv law/receipt-order-key [a b c])))))))

    (testing "same-millisecond split generations have a stable set tiebreak"
      (let [base (merge receipt
                        {:translation/at "2026-08-22T12:00:00.000Z"
                         :translation/split-turn-admitted-at
                         "2026-08-22T08:00:00.000Z"})
            set-a (assoc base
                         :translation/candidate-set-id "set-a"
                         :translation/split-review-order
                         [["2026-08-22T13:00:00.000Z" "review-z"]])
            set-b (assoc base
                         :translation/candidate-set-id "set-b")]
        (is (law/supersedes? set-b set-a))
        (is (not (law/supersedes? set-a set-b)))))))
