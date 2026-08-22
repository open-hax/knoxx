(ns knoxx.backend.law.translation-evidence-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.law.translation-evidence :as law]
            [malli.core :as m]))

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-abc123def456"
   :translation/revision "sha256-abc123def456+es@batch-7"
   :translation/dispatch-key "key-1"
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
          (str "missing " field " was accepted")))))

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
      (is (not (law/supersedes? older older))))))
