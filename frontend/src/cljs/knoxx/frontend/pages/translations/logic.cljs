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

(defn- review-key [value]
  [(or (:document value) (:document_id value))
   (or (:garden value) (:garden_id value))
   (or (:locale value) (:target_lang value))])

(defn attach-publication-reviews
  "Join revision-bound publication evidence onto segment rows, and retain every
   CONTRACT-BACKED translation even when no worker document exists.

   Contract-backed, not authored. An agent-produced translation of a
   contract-backed document also has no worker document — `translation-agent-sink`
   writes content and a receipt and creates no Mongo segments, and
   `/api/translations/documents` aggregates over the segments collection. So
   keeping only `authored-contract` here dropped every agent translation on the
   floor: invisible, therefore unapprovable, therefore unpublishable under
   `:translation/review :required`.

   The server marks any review it hydrated with `:content_source`, so presence of
   that key is the test, and its value says which kind it is."
  [documents reviews]
  (let [by-key (into {} (map (juxt review-key identity)) reviews)
        attached (mapv #(assoc % :publication_review (get by-key (review-key %))) documents)
        existing (set (map review-key documents))
        authored (->> reviews
                      (filter #(and (some? (:content_source %))
                                    (not (contains? existing (review-key %)))))
                      (mapv (fn [review]
                              {:document_id (:document review)
                               :garden_id (:garden review)
                               :source_lang (:source_locale review)
                               :target_lang (:locale review)
                               :title (:title review)
                               :overall_status (if (:approved review)
                                                 "fully_approved"
                                                 "pending_review")
                               :approved (if (:approved review) 1 0)
                               :total_segments 1
                               ;; Why it is read-only, kept separate from where
                               ;; the bytes came from. The durable reason is that
                               ;; a contract-backed translation has no persisted
                               ;; segment for a label to attach to — true of
                               ;; authored and agent content alike. Which of the
                               ;; two it is stays in `:content_source`, because
                               ;; approving authored bytes and approving
                               ;; generated bytes are different acts and the
                               ;; reviewer should be told which they are doing.
                               :contract_content true
                               :content_source (:content_source review)
                               :publication_review review})))]
    (into attached authored)))

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

(defn approval-request
  "The exact immutable coordinates the server exposed for approval."
  [review]
  (select-keys review [:document :garden :locale :revision :translation_revision]))

(defn sft-filename [project target-lang]
  (str project "-" (or (not-empty target-lang) "all") "-translations.jsonl"))
