(ns knoxx.backend.cms-source-locale-review-test
  "CMS resource policy exercised through the real publication and evidence gates."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.cms-document :as cms]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-evidence :as evidence]
            [knoxx.backend.law.publication :as publication]
            [knoxx.backend.law.translation-evidence :as translation-law]))

(def ^:private source-content "# Publishing desk\n\nA reviewed translation reaches the website.")
(def ^:private source-revision (crypto/sha256-hex source-content))
(def ^:private translated-content "# Mesa de publicación\n\nUna traducción revisada llega al sitio web.")
(def ^:private output-revision (crypto/sha256-hex translated-content))

(defn- published-intents
  []
  (let [resources (:resources (cms/manifest "review-demo" "source-locale"
                                           "/trusted/source-locale.md" "Publishing desk"))
        garden (first (:resources (cms/garden "review-demo")))
        index (resolver/publication-index (into [garden] resources))]
    (into {}
          (map (fn [intent]
                 [(:publication/locale intent)
                  (publication/hydrate-publication-intent
                   index (assoc intent :publication/state :published))]))
          (:publications index))))

(defn- candidate
  [intent]
  {:receipt/type :translation/completed
   :translation/document (:publication/document intent)
   :translation/garden (:publication/garden intent)
   :translation/source-locale (:document/source-locale intent)
   :translation/locale (:publication/locale intent)
   :translation/source-revision source-revision
   :translation/revision output-revision
   :translation/content-digest (crypto/sha256-hex translated-content)
   :translation/dispatch-key "cms-source-locale-review-fixture"
   :translation/org-id "review-demo"
   :translation/at "2026-10-09T12:00:00.000Z"})

(defn- approved-candidate
  [receipt]
  (translation-law/approve
   {:review/document (:translation/document receipt)
    :review/garden (:translation/garden receipt)
    :review/locale (:translation/locale receipt)
    :review/revision (:translation/source-revision receipt)
    :review/translation-revision (:translation/revision receipt)}
   receipt {:principal/user-email "reviewer@example.test"}
   "2026-10-09T12:01:00.000Z"))

(defn- decision
  [intent current-revision receipts approvals]
  (gate/gate
   intent
   (merge (evidence/gate-facts (evidence/evidence {:receipts receipts :approvals approvals}))
          {:current-source-revision (constantly current-revision)
           :source-revision-superseded? (constantly false)})))

(test/deftest emitted-english-source-can-publish-without-a-translation
  (let [english (:en (published-intents))
        result (decision english source-revision [] [])]
    (test/is (true? (:admissible? result))
        "The emitted English source is admitted without a translation receipt or approval")
    (test/is (= source-revision (:concrete-revision result)))
    (test/is (empty? (:blockers result)))
    (test/is (nil? (:translation-work result))
        "Publishing the source must not enqueue a translation back into its own language")
    (test/is (false? (:admissible? (decision (assoc english :publication/state :withheld)
                                      source-revision [] [])))
        "Review-free source policy does not publish a withheld document")))

(test/deftest source-bytes-remain-a-precondition-for-every-locale
  (doseq [[locale intent] (published-intents)]
    (test/testing (str locale " with no concrete source revision")
      (let [result (decision intent nil [] [])]
        (test/is (false? (:admissible? result)))
        (test/is (= [:publication-revision-unresolved] (:blockers result)))
        (test/is (nil? (:translation-work result)))))))

(test/deftest spanish-needs-the-exact-source-and-candidate-approval
  (let [spanish (:es (published-intents))
        receipt (candidate spanish)
        approval (approved-candidate receipt)]
    (test/testing "Missing translation and review still block Spanish"
      (let [result (decision spanish source-revision [] [])]
        (test/is (false? (:admissible? result)))
        (test/is (= [:translation-missing :translation-review-required] (:blockers result)))
        (test/is (= source-revision (get-in result [:translation-work :action/with :revision])))))
    (test/testing "An output without a content digest is not an admitted candidate"
      (test/is (false? (:admissible? (decision spanish source-revision
                                        [(dissoc receipt :translation/content-digest)] [])))))
    (test/testing "A completed candidate still needs approval"
      (let [result (decision spanish source-revision [receipt] [])]
        (test/is (false? (:admissible? result)))
        (test/is (= [:translation-review-required] (:blockers result)))))
    (test/testing "Approval for other bytes cannot authorize this candidate"
      (doseq [unrelated [(assoc approval :review/revision "older-source")
                         (assoc approval :review/translation-revision "other-output")
                         (assoc approval :review/content-digest "other-content")]]
        (test/is (false? (:admissible? (decision spanish source-revision [receipt] [unrelated]))))))
    (test/testing "Only the matching source and output approval admits Spanish"
      (let [result (decision spanish source-revision [receipt] [approval])]
        (test/is (true? (:admissible? result)))
        (test/is (empty? (:blockers result)))
        (test/is (nil? (:translation-work result)))))
    (test/testing "Editing English invalidates the old Spanish approval"
      (let [changed-source (crypto/sha256-hex (str source-content "\n\nNew source paragraph."))
            result (decision spanish changed-source [receipt] [approval])]
        (test/is (false? (:admissible? result)))
        (test/is (= [:translation-missing :translation-review-required] (:blockers result)))
        (test/is (= changed-source (get-in result [:translation-work :action/with :revision])))))))
