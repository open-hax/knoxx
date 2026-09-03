(ns knoxx.frontend.pages.translations.review-contract
  "Shared form vocabulary and validation for granular translation reviews."
  (:require [clojure.string :as str]))

(def evaluation-fields
  "Score fields retained by both legacy labels and resource split reviews."
  [:adequacy :fluency :terminology :risk])

(def review-form-fields
  "All reviewer-authored fields shared by the two granular review surfaces."
  (into evaluation-fields [:corrected_text :editor_notes]))

(def default-form
  "Historical neutral-positive defaults shown for a split without a review."
  {:adequacy "good"
   :fluency "good"
   :terminology "correct"
   :risk "safe"
   :corrected_text ""
   :editor_notes ""})

(def ^:private quality-options
  ["excellent" "good" "adequate" "poor" "unusable"])

(def ^:private options-by-field
  {:adequacy quality-options
   :fluency quality-options
   :terminology ["correct" "minor_errors" "major_errors"]
   :risk ["safe" "sensitive" "policy_violation"]})

(defn field-options
  "Return the closed wire vocabulary for one review score field."
  [field]
  (get options-by-field field []))

(defn evaluation-values
  "Return a complete, valid evaluation map or refuse an invented wire value."
  [form]
  (let [evaluations (select-keys form evaluation-fields)]
    (when-not
     (every? (fn [field]
               (contains? (set (field-options field)) (get evaluations field)))
             evaluation-fields)
      (throw (ex-info "translation review evaluations are incomplete or invalid"
                      {:evaluations evaluations})))
    evaluations))

(defn trimmed-optional-text
  "Trim optional reviewer text and return nil when it contains only whitespace."
  [value]
  (some-> value str str/trim not-empty))

(defn correction-text
  "Return a nonblank correction exactly as authored, including whitespace that
   may separate it from the next split; omit absent or all-whitespace values."
  [value]
  (let [text (some-> value str)]
    (when (some-> text str/trim not-empty)
      text)))
