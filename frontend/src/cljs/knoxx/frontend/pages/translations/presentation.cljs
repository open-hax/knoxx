(ns knoxx.frontend.pages.translations.presentation
  "Pure locale, status, form and detail projections for translation views."
  (:require [clojure.string :as str]
            [knoxx.frontend.pages.translations.review-contract :as review-contract]))

(def ^:private lang-names
  {"en" "English" "es" "Español" "fr" "Français" "de" "Deutsch"
   "ja" "日本語" "zh" "中文" "ko" "한국어" "pt" "Português"
   "ru" "Русский" "it" "Italiano"})

(defn lang-name
  "Return the display name for a locale code, preserving unknown codes."
  [code]
  (get lang-names code code))

(def default-label
  "Empty review form values shown before a persisted judgment is selected."
  (assoc review-contract/default-form :overall "approve"))

(def ^:private status-classes
  {"approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "in_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "pending" "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
   "fully_approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "pending_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "partial_review" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "fully_rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "mixed" "bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300"
   "missing" "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
   "queued" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "running" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "in_flight" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "failed" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "stale" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "evidence_missing" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "evidence_unbound" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "revision_unresolved" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "ready" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "changes_requested" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "published" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"})

(def ^:private status-icons
  {"approved" "✅" "rejected" "❌" "in_review" "📝" "pending" "⏳"
   "fully_approved" "✅" "pending_review" "⏳" "partial_review" "🔄"
   "fully_rejected" "❌" "mixed" "🔀" "missing" "⏳" "queued" "⏳"
   "running" "🔄" "in_flight" "🔄" "failed" "❌" "stale" "❌"
   "evidence_missing" "❌" "evidence_unbound" "❌" "revision_unresolved" "❌" "ready" "📝" "changes_requested" "📝"
   "published" "✅"})

(defn status-class
  "Return presentation classes for a review or work status."
  [status]
  (get status-classes status (get status-classes "pending")))

(defn status-icon
  "Return the compact icon for a review or work status."
  [status]
  (get status-icons status "⏳"))

(defn status-label
  "Turn an underscore-delimited wire status into a display label."
  [status]
  (str/replace status "_" " "))

(defn progress-pct
  "Return a safe completion percentage for a progress summary."
  [approved total]
  (if (pos? total) (* 100 (/ approved total)) 0))

(defn field-options
  "Return the closed option vocabulary for one review score field."
  [field]
  (review-contract/field-options field))

(defn prepare-label-payload
  "Label POST payload: preserves nonblank correction bytes, trims notes,
   omits blank optional text, and applies the chosen overall verdict."
  [form overall]
  (let [corrected (review-contract/correction-text (:corrected_text form))
        notes (review-contract/trimmed-optional-text (:editor_notes form))]
    (cond-> (-> form
                (assoc :overall overall)
                (dissoc :corrected_text :editor_notes))
      corrected (assoc :corrected_text corrected)
      notes (assoc :editor_notes notes))))

(defn find-segment
  "Find one split projection by its manifest ordinal."
  [detail segment-index]
  (when (and detail (some? segment-index))
    (first (filter #(= segment-index (:segment_index %)) (:segments detail)))))

(def ^:private fallback-langs ["es" "de" "ko" "fr" "ja" "zh" "it" "pt" "ru"])

(defn available-langs
  "Return manifest locales, or the historical fallback locale list."
  [manifest]
  (if manifest
    (mapv name (keys (:languages manifest)))
    fallback-langs))

(defn authored-detail
  "Synthesize a reviewable detail from a contract-backed review's text.

   Named for the case it was written for; it now serves agent-produced content
   too, which arrives through the same wire fields."
  [selected]
  (let [review (:publication_review selected)
        source-blocks (str/split (or (:source_text review) "") #"\n\s*\n")
        translated-blocks (str/split (or (:translated_text review) "") #"\n\s*\n")
        segments (mapv (fn [index translated]
                         {:id (str (:document_id selected) "-" (:target_lang selected) "-" index)
                          :segment_index index
                          :status (if (:approved review) "approved" "pending")
                          :source_lang (:source_lang selected)
                          :target_lang (:target_lang selected)
                          :source_text (get source-blocks index "")
                          :translated_text translated})
                       (range (count translated-blocks))
                       translated-blocks)]
    {:document {:title (:title selected)
                :source_lang (:source_lang selected)}
     :target_lang (:target_lang selected)
     :summary {:total_segments (count segments)
               :approved (if (:approved review) (count segments) 0)
               :overall_status (if (:approved review)
                                 "fully_approved"
                                 "pending_review")}
     :segments segments}))

(defn sft-filename
  "Return the stable filename for one translation export selection."
  [project target-lang]
  (str project "-" (or (not-empty target-lang) "all") "-translations.jsonl"))
