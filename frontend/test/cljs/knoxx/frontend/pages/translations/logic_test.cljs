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

(deftest sft-filename-contract
  (is (= "devel-es-translations.jsonl" (logic/sft-filename "devel" "es")))
  (is (= "devel-all-translations.jsonl" (logic/sft-filename "devel" ""))))
