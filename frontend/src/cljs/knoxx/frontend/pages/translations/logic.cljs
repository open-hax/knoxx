(ns knoxx.frontend.pages.translations.logic
  "Pure logic for the translation review page. CLJS port of the helpers
   in src/pages/TranslationReviewPage.tsx."
  (:require [clojure.string :as str]))

(def ^:private lang-names
  {"en" "English" "es" "Español" "fr" "Français" "de" "Deutsch"
   "ja" "日本語" "zh" "中文" "ko" "한국어" "pt" "Português"
   "ru" "Русский" "it" "Italiano"})

(defn lang-name [code]
  (get lang-names code code))

(def default-label
  {:adequacy "good" :fluency "good" :terminology "correct"
   :risk "safe" :overall "approve" :corrected_text "" :editor_notes ""})

(def ^:private status-classes
  {"approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "in_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "pending" "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
   "fully_approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "pending_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "partial_review" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "fully_rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "mixed" "bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300"})

(def ^:private status-icons
  {"approved" "✅" "rejected" "❌" "in_review" "📝" "pending" "⏳"
   "fully_approved" "✅" "pending_review" "⏳" "partial_review" "🔄"
   "fully_rejected" "❌" "mixed" "🔀"})

(defn status-class [status]
  (get status-classes status (get status-classes "pending")))

(defn status-icon [status]
  (get status-icons status "⏳"))

(defn status-label [status]
  (str/replace status "_" " "))

(defn progress-pct [approved total]
  (if (pos? total) (* 100 (/ approved total)) 0))

(defn field-options [field]
  (case field
    :risk ["safe" "sensitive" "policy_violation"]
    :terminology ["correct" "minor_errors" "major_errors"]
    ["excellent" "good" "adequate" "poor" "unusable"]))

(defn prepare-label-payload
  "Label POST payload: trims corrected text and notes (omitting them when
   blank) and applies the chosen overall verdict."
  [form overall]
  (let [corrected (some-> (:corrected_text form) str/trim not-empty)
        notes (some-> (:editor_notes form) str/trim not-empty)]
    (cond-> (-> form
                (assoc :overall overall)
                (dissoc :corrected_text :editor_notes))
      corrected (assoc :corrected_text corrected)
      notes (assoc :editor_notes notes))))

(defn find-segment [detail segment-index]
  (when (and detail (some? segment-index))
    (first (filter #(= segment-index (:segment_index %)) (:segments detail)))))

(def ^:private fallback-langs ["es" "de" "ko" "fr" "ja" "zh" "it" "pt" "ru"])

(defn available-langs [manifest]
  (if manifest
    (mapv name (keys (:languages manifest)))
    fallback-langs))

(defn- doc-key [doc]
  [(:document_id doc) (:target_lang doc)])

(defn still-listed? [documents selected]
  (boolean (some #(= (doc-key %) (doc-key selected)) documents)))

(defn sft-filename [project target-lang]
  (str project "-" (or (not-empty target-lang) "all") "-translations.jsonl"))
