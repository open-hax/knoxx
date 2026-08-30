(ns knoxx.frontend.pages.translations.split-review-panel-test
  "Rendered history parity for the resource-backed split review card."
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.translations.split-review-panel
             :refer [split-review-panel]]))

(use-fixtures :each
  {:after rtl/cleanup})

(defn- render-panel
  [labels]
  (rtl/render
   ($ split-review-panel
      {:split {:split_id "split/0"
               :segment_index 0
               :status "approved"
               :source_lang "en"
               :target_lang "es"
               :source_text "Hello"
               :candidate_text "Hola"
               :labels labels}
       :form {:adequacy "good"
              :fluency "good"
              :terminology "correct"
              :risk "safe"
              :corrected_text ""
              :editor_notes ""}
       :saving false
       :on-change (fn [_])
       :on-submit (fn [_])
       :on-skip (fn [])})))

(deftest split-panel-renders-immutable-existing-label-history
  (let [r (render-panel
           [{:id "review/2"
             :review_id "review/2"
             :segment_id "split/0"
             :labeler_id "reviewers/alex"
             :labeler_email "alex@example.test"
             :ts "2026-08-29T15:04:05.000Z"
             :reviewed_at "2026-08-29T15:04:05.000Z"
             :review_status "approved"
             :overall "approve"
             :adequacy "excellent"
             :fluency "good"
             :terminology "correct"
             :risk "safe"
             :corrected_text "Hola, mundo"
             :editor_notes "Keep the product name literal."}])
        text (.-textContent (.-container r))]
    (doseq [fragment ["Existing labels"
                      "alex@example.test"
                      "2026"
                      "approve"
                      "excellent adequacy"
                      "good fluency"
                      "correct terminology"
                      "safe risk"
                      "Hola, mundo"
                      "Keep the product name literal."]]
      (is (str/includes? text fragment) fragment))))

(deftest split-panel-keeps-history-shape-visible-before-first-label
  (let [r (render-panel [])]
    (is (some? (.queryByText r "Existing labels")))
    (is (some? (.queryByText r "No labels yet.")))))
