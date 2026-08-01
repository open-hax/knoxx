(ns openplanner.translations.core
  "Pure translation domain logic used by Knoxx's direct Mongo boundary.

  This is the existing OpenPlanner ClojureScript translation core, carried in
  Knoxx so translation storage remains a single-repository CLJS change."
  (:require [clojure.string :as str]))

(def segment-statuses #{:pending :in-review :approved :rejected})
(def label-overalls #{:approve :needs-edit :reject})

(defn token
  [value]
  (some-> value str str/trim str/lower-case (str/replace #"_" "-") keyword))

(defn status-wire
  [status]
  (some-> status name (str/replace #"-" "_")))

(defn nonblank-string
  [value]
  (let [s (some-> value str str/trim)]
    (when-not (str/blank? s) s)))

(defn next-segment-status
  [{:keys [current-status overall corrected-text]}]
  (let [overall (token overall)
        current-status (or (token current-status) :pending)]
    (case overall
      :approve :approved
      :needs-edit (if (nonblank-string corrected-text) :approved :in-review)
      :reject :rejected
      current-status)))

(defn document-overall-status
  [{:keys [total approved rejected pending]}]
  (cond
    (and (pos? total) (= approved total)) :fully-approved
    (and (pos? total) (= rejected total)) :fully-rejected
    (and (pos? total) (= pending total)) :pending-review
    (pos? pending) :partial-review
    :else :mixed))

(defn summarize-segments
  [segments]
  (let [counts (frequencies (map #(token (:status %)) segments))
        total (count segments)
        approved (get counts :approved 0)
        pending (get counts :pending 0)
        rejected (get counts :rejected 0)
        in-review (get counts :in-review 0)]
    {:total-segments total
     :approved approved
     :pending pending
     :rejected rejected
     :in-review in-review
     :overall-status (document-overall-status {:total total
                                               :approved approved
                                               :pending pending
                                               :rejected rejected})}))

(defn normalize-segment
  [{:keys [source-text translated-text source-lang target-lang document-id segment-index status mt-model confidence domain content-type url-context garden-id org-id project]}]
  {:source-text (or (nonblank-string source-text) "")
   :translated-text (or (nonblank-string translated-text) "")
   :source-lang (or (nonblank-string source-lang) "en")
   :target-lang (or (nonblank-string target-lang) "")
   :document-id (or (nonblank-string document-id) "")
   :segment-index (long (or segment-index 0))
   :status (if (contains? segment-statuses (token status)) (token status) :pending)
   :mt-model (nonblank-string mt-model)
   :confidence (when (some? confidence)
                 (let [n (js/Number confidence)]
                   (when (js/Number.isFinite n) n)))
   :domain (nonblank-string domain)
   :content-type (nonblank-string content-type)
   :url-context (nonblank-string url-context)
   :garden-id (nonblank-string garden-id)
   :org-id (nonblank-string org-id)
   :project (nonblank-string project)})

(defn segment-errors
  [{:keys [source-text translated-text target-lang document-id]}]
  (cond-> []
    (str/blank? source-text) (conj {:path [:source-text] :error :required})
    (str/blank? translated-text) (conj {:path [:translated-text] :error :required})
    (str/blank? target-lang) (conj {:path [:target-lang] :error :required})
    (str/blank? document-id) (conj {:path [:document-id] :error :required})))

(defn graph-memory-plan
  [{:keys [segment-id source-text translated-text corrected-text source-lang target-lang document-id domain content-type]}]
  (let [target-text (or (nonblank-string corrected-text) translated-text)
        node-id (str "translation:" source-lang ":" target-lang ":" segment-id)]
    (if (and (nonblank-string source-text) (nonblank-string target-text))
      {:ok? true
       :node {:id node-id
              :kind "translation_example"
              :label (str source-lang "→" target-lang ": " (subs source-text 0 (min 50 (count source-text))) "...")
              :data {:source_text source-text
                     :target_text target-text
                     :source_lang source-lang
                     :target_lang target-lang
                     :document_id document-id
                     :domain domain
                     :content_type content-type
                     :quality "approved"
                     :segment_id segment-id}}
       :edge {:id (str "translation:doc:" document-id ":" segment-id)
              :source document-id
              :target node-id
              :kind "has_translation"
              :data {:source_lang source-lang
                     :target_lang target-lang}}}
      {:ok? false :error "Missing source or target text"})))

(defn sft-prompt
  [{:keys [source-lang target-lang source-text]}]
  (let [source-lang (or (nonblank-string source-lang) "English")
        target-lang (or (nonblank-string target-lang) "target language")]
    (str "Translate the following text from " source-lang " to " target-lang
         ". Preserve formatting, technical terms, and code examples.\n\nText:\n"
         (or source-text ""))))

(defn sft-row
  [{:keys [source-lang target-lang source-text translated-text corrected-text]}]
  {:prompt (sft-prompt {:source-lang source-lang
                        :target-lang target-lang
                        :source-text source-text})
   :target (or (nonblank-string corrected-text) translated-text "")})

(def job-statuses #{:queued :processing :complete :failed})

(defn normalize-target-languages
  [value]
  (let [langs (if (sequential? value)
                (->> value (keep nonblank-string) distinct vec)
                [])]
    (if (seq langs) langs ["es" "de"])))

(defn translation-job-plan
  [{:keys [document-id document-text target-languages garden-id project source-lang]}]
  (if (str/blank? (or document-text ""))
    {:ok? false :error "Document has no content to translate"}
    (let [langs (normalize-target-languages target-languages)]
      {:ok? true
       :document-id document-id
       :target-languages langs
       :jobs (mapv (fn [target-lang]
                     {:document_id document-id
                      :garden_id garden-id
                      :project project
                      :source_lang (or (nonblank-string source-lang) "en")
                      :target_language target-lang
                      :status "queued"})
                   langs)
       :message "Translation job(s) created. MT pipeline will process them."})))

(defn job-status-update-plan
  [{:keys [status error]}]
  (let [status (token status)]
    (if-not (contains? #{:processing :complete :failed} status)
      {:ok? false :error "Invalid status. Must be: processing, complete, or failed"}
      (cond-> {:ok? true :status (status-wire status)}
        (= :processing status) (assoc :started? true)
        (contains? #{:complete :failed} status) (assoc :completed? true)
        (and (= :failed status) (nonblank-string error)) (assoc :error (nonblank-string error))))))

(defn document-list-row
  [{:keys [document-id target-lang source-lang garden-id project total approved pending rejected in-review title visibility]}]
  {:document_id document-id
   :target_lang target-lang
   :source_lang source-lang
   :garden_id garden-id
   :project project
   :title (or (nonblank-string title) "Untitled")
   :document_status (or (nonblank-string visibility) "internal")
   :total_segments (or total 0)
   :approved (or approved 0)
   :pending (or pending 0)
   :rejected (or rejected 0)
   :in_review (or in-review 0)
   :overall_status (status-wire (document-overall-status {:total (or total 0)
                                                          :approved (or approved 0)
                                                          :pending (or pending 0)
                                                          :rejected (or rejected 0)}))})

(defn format-label
  [{:keys [id segment-id labeler-id labeler-email adequacy fluency terminology risk overall corrected-text editor-notes ts]}]
  {:id id
   :segment_id segment-id
   :labeler_id labeler-id
   :labeler_email labeler-email
   :adequacy adequacy
   :fluency fluency
   :terminology terminology
   :risk risk
   :overall overall
   :corrected_text corrected-text
   :editor_notes editor-notes
   :ts ts})

(defn format-segment
  [{:keys [id source-text translated-text source-lang target-lang document-id segment-index status confidence mt-model garden-id project labels ts]}]
  {:id id
   :source_text source-text
   :translated_text translated-text
   :source_lang source-lang
   :target_lang target-lang
   :document_id document-id
   :segment_index segment-index
   :status (status-wire (token status))
   :confidence confidence
   :mt_model mt-model
   :garden_id garden-id
   :project project
   :labels (mapv format-label labels)
   :ts ts})

(defn document-translation-shape
  [{:keys [document segments]}]
  (let [formatted (mapv format-segment segments)]
    {:document document
     :segments formatted
     :summary (summarize-segments formatted)}))

(defn document-review-label-plan
  [{:keys [segment-id labeler-id labeler-email overall corrected-text editor-notes]}]
  {:segment_id segment-id
   :labeler_id (or (nonblank-string labeler-id) "unknown")
   :labeler_email (or (nonblank-string labeler-email) "unknown")
   :adequacy (if (= :approve (token overall)) "good" "adequate")
   :fluency (if (= :approve (token overall)) "good" "adequate")
   :terminology (if (= :approve (token overall)) "correct" "minor_errors")
   :risk "safe"
   :overall (name (or (token overall) :needs-edit))
   :corrected_text (nonblank-string corrected-text)
   :editor_notes (nonblank-string editor-notes)
   :next_status (status-wire (next-segment-status {:overall overall :corrected-text corrected-text}))})

(defn manifest-shape
  [{:keys [project languages corrections-by-language labelers]}]
  (let [language-entries
        (map (fn [{:keys [target-lang total approved rejected pending in-review]}]
               (let [lang-key (or target-lang "unknown")
                     approved (or approved 0)]
                 [lang-key
                  {:total_segments (or total 0)
                   :approved approved
                   :rejected (or rejected 0)
                   :pending (or pending 0)
                   :in_review (or in-review 0)
                   :with_corrections (get corrections-by-language lang-key 0)
                   :avg_labels_per_segment 0}]))
             languages)
        export-entries
        (map (fn [{:keys [target-lang approved]}]
               (let [lang-key (or target-lang "unknown")
                     approved (or approved 0)]
                 [(str "sft_" lang-key)
                  {:rows approved
                   :bytes_estimate (* approved 500)}]))
             languages)]
    {:project (or (nonblank-string project) "all")
     :languages (into {} language-entries)
     :labelers (mapv (fn [{:keys [email segments-labeled]}]
                       {:email email
                        :segments_labeled (or segments-labeled 0)})
                     labelers)
     :export_sizes (into {} export-entries)}))
