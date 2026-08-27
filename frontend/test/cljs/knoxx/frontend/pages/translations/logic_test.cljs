(ns knoxx.frontend.pages.translations.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/pages/TranslationReviewPage.tsx."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.translations.logic :as logic]))

(deftest lang-name-lookup
  (is (= "Español" (logic/lang-name "es")))
  (is (= "English" (logic/lang-name "en")))
  (is (= "xx" (logic/lang-name "xx")) "unknown codes pass through"))

(deftest default-label-shape
  (is (= {:adequacy "good" :fluency "good" :terminology "correct"
          :risk "safe" :overall "approve" :corrected_text "" :editor_notes ""}
         logic/default-label)))

(deftest status-badge-data
  (is (clojure.string/includes? (logic/status-class "approved") "emerald"))
  (is (clojure.string/includes? (logic/status-class "fully_rejected") "rose"))
  (is (clojure.string/includes? (logic/status-class "mixed") "purple"))
  (is (= (logic/status-class "pending") (logic/status-class "unknown-status"))
      "unknown statuses fall back to pending styling")
  (is (= "✅" (logic/status-icon "approved")))
  (is (= "⏳" (logic/status-icon "whatever")))
  (is (= "pending review" (logic/status-label "pending_review"))))

(deftest progress-pct-contract
  (is (= 50 (logic/progress-pct 1 2)))
  (is (= 0 (logic/progress-pct 0 0)) "no segments → 0, not NaN"))

(deftest field-options-per-field
  (is (= ["safe" "sensitive" "policy_violation"] (logic/field-options :risk)))
  (is (= ["correct" "minor_errors" "major_errors"] (logic/field-options :terminology)))
  (is (= ["excellent" "good" "adequate" "poor" "unusable"] (logic/field-options :adequacy)))
  (is (= (logic/field-options :adequacy) (logic/field-options :fluency))))

(deftest prepare-label-payload-trims-and-omits
  (testing "blank corrected/notes are omitted entirely"
    (let [payload (logic/prepare-label-payload logic/default-label "approve")]
      (is (= "approve" (:overall payload)))
      (is (not (contains? payload :corrected_text)))
      (is (not (contains? payload :editor_notes)))))
  (testing "non-blank values are trimmed and kept; overall overridden"
    (let [payload (logic/prepare-label-payload
                   (assoc logic/default-label
                          :corrected_text "  fixed  "
                          :editor_notes "note"
                          :overall "approve")
                   "reject")]
      (is (= "reject" (:overall payload)))
      (is (= "fixed" (:corrected_text payload)))
      (is (= "note" (:editor_notes payload)))
      (is (= "good" (:adequacy payload)) "scores carried through"))))

(deftest find-segment-by-index
  (let [detail {:segments [{:segment_index 0 :id "a"} {:segment_index 3 :id "b"}]}]
    (is (= "b" (:id (logic/find-segment detail 3))))
    (is (nil? (logic/find-segment detail 1)))
    (is (nil? (logic/find-segment nil 0)))
    (is (nil? (logic/find-segment detail nil)))))

(deftest available-langs-from-manifest
  (is (= ["es" "de" "ko" "fr" "ja" "zh" "it" "pt" "ru"]
         (logic/available-langs nil))
      "fallback list without a manifest")
  (is (= #{"es" "fr"}
         (set (logic/available-langs {:languages {:es {:total_segments 1} :fr {:total_segments 2}}})))
      "manifest keys win (keywordized json)"))

(deftest selection-preservation
  (let [docs [{:document_id "d1" :target_lang "es"} {:document_id "d1" :target_lang "fr"}]]
    (is (logic/still-listed? docs {:document_id "d1" :target_lang "fr"}))
    (is (not (logic/still-listed? docs {:document_id "d2" :target_lang "es"})))
    (is (not (logic/still-listed? docs {:document_id "d1" :target_lang "de"}))
        "same doc id but different lang is a different review")))

(deftest publication-review-joins-on-document-garden-and-locale
  (let [review {:publication "publications/doc-1-es"
                :document "docs/doc-1" :garden "gardens/promethean"
                :locale "es" :revision "source-sha"
                :translation_revision "translation-sha" :approved false}
        [joined] (logic/attach-publication-reviews
                  [{:document_id "docs/doc-1" :garden_id "gardens/promethean"
                    :target_lang "es"}]
                  [review])]
    (is (= review (:publication_review joined)))
    (is (= {:document "docs/doc-1" :garden "gardens/promethean"
            :locale "es" :revision "source-sha"
            :translation_revision "translation-sha"}
           (logic/approval-request review)))))

(deftest authored-contract-reviews-remain-visible-without-worker-documents
  (let [review {:publication "publications/doc-1-es"
                :document "docs/doc-1" :garden "gardens/promethean"
                :locale "es" :source_locale "en"
                :title "Promethean" :content_source "authored-contract"
                :source_text "Hello\n\nGarden"
                :translated_text "Hola\n\nJardín"
                :revision "source-sha"
                :translation_revision "translation-sha"
                :approved false}
        [document] (logic/attach-publication-reviews [] [review])
        detail (logic/authored-detail document)]
    (is (:contract_content document))
    (is (= "authored-contract" (:content_source document))
        "which kind of bytes these are survives onto the row, so the reviewer
         can be told what they are approving")
    (is (= "Promethean" (:title document)))
    (is (= "pending_review" (:overall_status document)))
    (is (= 2 (count (:segments detail))))
    (is (= "Hello" (get-in detail [:segments 0 :source_text])))
    (is (= "Jardín" (get-in detail [:segments 1 :translated_text])))))

(deftest agent-produced-reviews-remain-visible-without-worker-documents
  (testing "an agent translation of a contract-backed document has no worker
            document either — `translation-agent-sink` writes content and a
            receipt and creates no Mongo segments, while
            /api/translations/documents aggregates over the segments collection.
            Keeping only authored-contract here dropped every agent translation:
            invisible, so unapprovable, so unpublishable under
            :translation/review :required."
    (let [review {:publication "publications/doc-1-fr"
                  :document "docs/doc-1" :garden "gardens/promethean"
                  :locale "fr" :source_locale "en"
                  :title "Promethean" :content_source "agent"
                  :source_text "Hello\n\nGarden"
                  :translated_text "Bonjour\n\nJardin"
                  :revision "source-sha"
                  :translation_revision "agent-output-sha"
                  :approved false}
          [document] (logic/attach-publication-reviews [] [review])
          detail (logic/authored-detail document)]
      (is (some? document) "an agent-produced review is not dropped")
      (is (:contract_content document)
          "read-only for the same reason authored content is: no persisted
           segment for a label to attach to")
      (is (= "agent" (:content_source document))
          "and distinguishable from authored bytes, because approving generated
           text is a different act")
      (is (= 2 (count (:segments detail))))
      (is (= "Bonjour" (get-in detail [:segments 0 :translated_text]))))))

(deftest a-review-the-server-did-not-hydrate-is-still-dropped
  (testing "presence of :content_source is the test, so a review carrying no
            text cannot reach the page and offer an approval control over
            nothing — which is what revision-specific approval exists to prevent"
    (is (empty? (logic/attach-publication-reviews
                 []
                 [{:publication "publications/doc-9-de"
                   :document "docs/doc-9" :garden "gardens/promethean"
                   :locale "de" :revision "r" :translation_revision "t"
                   :approved false}])))))

(deftest sft-filename-contract
  (is (= "devel-es-translations.jsonl" (logic/sft-filename "devel" "es")))
  (is (= "devel-all-translations.jsonl" (logic/sft-filename "devel" ""))))
