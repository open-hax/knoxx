(ns knoxx.backend.domain.local-openplanner-translations
  "Tenant-scoped translation projections with immutable candidate generations."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.law.local-openplanner :as law]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn- checked [schema value] (contract/assert-valid! :local/translation schema value))
(defn- exact-scope? [scope row]
  (= (select-keys scope [:org_id :project :garden_id])
     (select-keys row [:org_id :project :garden_id])))
(defn- matches? [opts row]
  (and (= (:org_id opts) (:org_id row))
       (every? (fn [key] (or (nil? (get opts key)) (= (get opts key) (get row key))))
               [:project :garden_id :status :source_lang :target_lang :domain :document_id])))

(defn- identity-key [input]
  (mapv input [:org_id :project :garden_id :document_id :target_lang :segment_index]))

(defn- segment-input [input]
  (checked contract/CreateTranslationSegmentRequest input)
  (when (neg? (:segment_index input))
    (throw (ex-info "Segment index must be nonnegative" {:status 400})))
  (merge {:source_lang "en" :status "pending" :project nil :garden_id nil}
         (into {} (remove (comp nil? val)) input)))

(defn create-segment
  "Select a fresh generation for new candidate bytes; historical labels stay bound."
  [state input timestamp]
  (let [input (segment-input input)
        key (identity-key input)
        generation (crypto/sha256-hex
                    (pr-str (mapv input [:org_id :project :garden_id :document_id :target_lang
                                         :segment_index :source_text :translated_text :source_revision
                                         :candidate_set_id :candidate_digest])))
        id (str "segment-" generation)
        existing (get-in state [:segments id])
        current-id (get-in state [:segment-current key])
        row (or existing (assoc input :id id :generation generation :created_at timestamp
                                      :updated_at timestamp :ts timestamp :labels []))
        next-state (-> state (assoc-in [:segments id] row) (assoc-in [:segment-current key] id))]
    (when (and existing current-id (not= current-id id))
      (throw (ex-info "An old candidate cannot replace the current generation"
                      {:status 409 :code "openplanner_translation_stale_candidate"})))
    [next-state {:ok true :id id :status (:status row)
                 :upserted (nil? current-id) :modified (boolean (and current-id (not= current-id id)))}]))

(defn project-events
  "Build translation candidates from immutable translation.segment envelopes."
  [state events]
  (reduce (fn [acc event]
            (if (= "translation.segment" (:kind event))
              (let [extra (:extra event)
                    ref (:source_ref event)
                    input (merge (:meta event) extra
                                 {:translated_text (:text event) :source_text (:source_text extra)
                                  :org_id (:org_id extra) :project (:project ref)
                                  :document_id (:document_id ref) :segment_index (:segment_index ref)})]
                (first (create-segment acc input (:ts event))))
              acc)) state events))

(defn current-segments
  "Read only current generations in an explicit tenant scope."
  [state opts]
  (law/scope! opts)
  (->> (vals (:segment-current state)) (map #(get-in state [:segments %]))
       (filter #(matches? opts %)) (sort-by (juxt :document_id :target_lang :segment_index :id)) vec))

(defn segment
  "Read one exact scoped candidate, including its own immutable label history."
  [state id scope]
  (law/scope! scope)
  (let [row (get-in state [:segments id])]
    (when-not (and row (matches? scope row))
      (throw (ex-info "Translation segment not found" {:status 404})))
    (assoc row :label_count (count (:labels row)))))

(defn label-segment
  "Atomically bind a review to the current generation and apply its correction."
  [state id request label-id timestamp]
  (checked contract/LabelTranslationSegmentRequest request)
  (let [row (segment state id request)]
    (when-not (and (exact-scope? (merge {:project nil :garden_id nil} request) row)
                   (= id (get-in state [:segment-current (identity-key row)])))
      (throw (ex-info "Translation candidate changed; reload before reviewing"
                      {:status 409 :code "openplanner_translation_stale_candidate"})))
    (let [corrected (translation/nonblank-string (:corrected_text request))
          status (translation/status-wire
                  (translation/next-segment-status {:current-status (:status row)
                                                    :overall (:overall request) :corrected-text corrected}))
          label (assoc request :id label-id :segment_id id :label_version (inc (count (:labels row)))
                               :generation (:generation row) :corrected_text corrected :ts timestamp)
          next-row (cond-> (-> row (update :labels conj label) (assoc :status status :updated_at timestamp))
                     corrected (assoc :translated_text corrected))]
      [(assoc-in state [:segments id] next-row)
       {:ok true :label_id label-id :label label :new_status status
        :graph_memory {:success false :reason "local_graph_projection_unsupported"}}])))

(defn summary
  "Convert the existing pure translation summary to the service wire schema."
  [segments]
  (let [value (translation/summarize-segments segments)]
    {:total_segments (:total-segments value) :approved (:approved value)
     :pending (:pending value) :rejected (:rejected value) :in_review (:in-review value)
     :overall_status (translation/status-wire (:overall-status value))}))

(defn- document-meta [state id scope segments]
  (let [source (last (filter #(and (= id (or (get-in % [:extra :document_id]) (:id %)))
                                    (= (:org_id scope) (get-in % [:extra :org_id]))
                                    (not= "translation.segment" (:kind %)))
                             (map #(get-in state [:events %]) (:event-order state))))
        extra (:extra source)]
    {:id id :title (or (:title extra) "Untitled")
     :content (or (:content extra) (:text source) (str/join "\n" (map :source_text segments)))
     :source_lang (or (:language extra) (:source_lang (first segments)) "en")
     :visibility (or (:visibility extra) "internal")
     :source_path (or (:sourcePath extra) (:source_path extra))}))

(defn document
  "Return exact document/project/garden/locale segments and source metadata."
  [state id language scope]
  (checked contract/TenantScopeRequest scope)
  (let [exact (merge {:project nil :garden_id nil} scope)
        segments (filterv #(exact-scope? exact %) (current-segments state (assoc scope :document_id id :target_lang language)))]
    {:document (document-meta state id scope segments) :segments segments :summary (summary segments)}))

(defn documents
  "Group current translation relations within the authorized tenant."
  [state opts]
  (checked contract/TranslationDocumentsRequest opts)
  (let [groups (group-by #(select-keys % [:document_id :target_lang :source_lang :garden_id :project])
                        (current-segments state opts))
        rows (mapv (fn [[key segments]]
                     (let [counts (summary segments)
                           metadata (document-meta state (:document_id key) opts segments)]
                       (merge key counts {:title (:title metadata) :document_status (:visibility metadata)}))) groups)]
    {:documents (vec (sort-by (juxt :document_id :target_lang :garden_id) rows)) :total (count rows)}))

(defn review-document
  "Apply one accepted document review and all its split labels atomically."
  [state id language request operation-id timestamp]
  (checked contract/ReviewTranslationDocumentRequest request)
  (let [segments (:segments (document state id language request))
        overrides (or (:segment_overrides request) {})]
    (when (empty? segments) (throw (ex-info "Translation document not found" {:status 404})))
    [(reduce (fn [acc row]
               (let [override (or (get overrides (:id row)) (get overrides (keyword (:id row))) {})
                     plan (translation/document-review-label-plan
                           {:segment-id (:id row) :overall (or (:overall override) (:overall request))
                            :corrected-text (:corrected_text override)
                            :editor-notes (or (:editor_notes override) (:editor_notes request))
                            :labeler-id (:labeler_id request) :labeler-email (:labeler_email request)})
                     label (merge (select-keys request [:org_id :project :garden_id])
                                  (dissoc plan :next_status :segment_id))]
                 (first (label-segment acc (:id row) label (str operation-id "/" (:id row)) timestamp)))) state segments)
     {:ok true :document_id id :target_lang language :segments_reviewed (count segments)
      :segments_failed 0 :overall (:overall request) :overrides_applied (count overrides)
      :graph_memory_failures (count segments)}]))

(defn manifest
  "Compute translation inventory from current generations, not historical approvals."
  [state opts timestamp]
  (checked contract/TranslationManifestRequest opts)
  (let [groups (group-by :target_lang (current-segments state opts))
        languages (mapv (fn [[language rows]]
                          (let [counts (summary rows)]
                            {:target-lang language :total (count rows) :approved (:approved counts)
                             :rejected (:rejected counts) :pending (:pending counts) :in-review (:in_review counts)})) groups)
        corrections (into {} (map (fn [[language rows]]
                                    [language (count (filter #(some :corrected_text (:labels %)) rows))])) groups)
        labels (mapcat :labels (mapcat val groups))
        labelers (mapv (fn [[email rows]] {:email email :segments-labeled (count rows)}) (group-by :labeler_email labels))]
    (assoc (translation/manifest-shape {:project (:project opts) :languages languages
                                        :corrections-by-language corrections :labelers labelers})
           :generated_at timestamp)))
