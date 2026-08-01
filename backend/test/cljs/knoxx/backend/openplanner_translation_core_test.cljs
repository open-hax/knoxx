(ns knoxx.backend.openplanner-translation-core-test
  (:require [cljs.test :refer [deftest is testing]]
            [openplanner.translations.core :as translation]))

(deftest segment-normalization-preserves-domain-semantics
  (testing "unknown confidence remains absent and invalid status becomes pending"
    (let [segment (translation/normalize-segment
                   {:source-text " Hello "
                    :translated-text " Hola "
                    :target-lang "es"
                    :document-id "doc-1"
                    :segment-index 0
                    :status "invented"
                    :confidence "not-a-number"
                    :org-id "org-1"})]
      (is (= "Hello" (:source-text segment)))
      (is (= "Hola" (:translated-text segment)))
      (is (= :pending (:status segment)))
      (is (nil? (:confidence segment)))
      (is (= "org-1" (:org-id segment))))))

(deftest sft-contract-remains-prompt-and-target
  (testing "training exports preserve the established row schema"
    (let [row (translation/sft-row
               {:source-lang "English"
                :target-lang "es"
                :source-text "Hello"
                :translated-text "Hola"
                :corrected-text "Buenas"})]
      (is (= #{:prompt :target} (set (keys row))))
      (is (= "Buenas" (:target row)))
      (is (re-find #"Translate the following text" (:prompt row))))))

(deftest document-review-labels-preserve-quality-semantics
  (testing "review plans preserve rubric and underscore wire values"
    (let [approved (translation/document-review-label-plan
                    {:segment-id "segment-1" :overall "approve"})
          needs-edit (translation/document-review-label-plan
                      {:segment-id "segment-2" :overall "needs_edit"})
          rejected (translation/document-review-label-plan
                    {:segment-id "segment-3" :overall "reject"})]
      (is (= "good" (:adequacy approved)))
      (is (= "good" (:fluency approved)))
      (is (= "correct" (:terminology approved)))
      (is (= "approved" (:next_status approved)))
      (is (= "needs_edit" (:overall needs-edit)))
      (is (= "in_review" (:next_status needs-edit)))
      (is (= "adequate" (:adequacy rejected)))
      (is (= "minor_errors" (:terminology rejected)))
      (is (= "rejected" (:next_status rejected))))))

(deftest manifest-and-document-shapes-remain-compatible
  (testing "language manifests stay keyed and document status stays derived"
    (let [manifest (translation/manifest-shape
                    {:project "knoxx"
                     :languages [{:target-lang "es"
                                  :total 3
                                  :approved 2
                                  :rejected 0
                                  :pending 1
                                  :in-review 0}]
                     :corrections-by-language {"es" 1}
                     :labelers [{:email "reviewer@example.test"
                                 :segments-labeled 2}]})
          document (translation/document-list-row
                    {:document-id "doc-1"
                     :target-lang "es"
                     :total 3
                     :approved 2
                     :pending 1
                     :rejected 0
                     :in-review 0
                     :title "Document"
                     :visibility "internal"})]
      (is (= 3 (get-in manifest [:languages "es" :total_segments])))
      (is (= 1 (get-in manifest [:languages "es" :with_corrections])))
      (is (= 2 (get-in manifest [:export_sizes "sft_es" :rows])))
      (is (= "partial_review" (:overall_status document)))
      (is (= "Document" (:title document))))))