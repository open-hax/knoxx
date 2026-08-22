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
