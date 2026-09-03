(ns knoxx.frontend.pages.translations.split-review
  "Pure projection and request logic for resource-backed translation splits."
  (:require [clojure.string :as str]
            [knoxx.frontend.pages.translations.review-contract :as review-contract]))

(def review-statuses
  "The closed split-review vocabulary accepted by the publication endpoint."
  #{"in-review" "approved" "rejected"})

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn normalized-status
  "Normalize keyword and historical underscore spellings to the review wire form."
  [status]
  (some-> (if (keyword? status) (name status) status)
          str
          (str/replace "_" "-")))

(defn display-status
  "Return the underscore spelling consumed by existing status badges."
  [status]
  (some-> (normalized-status status) (str/replace "-" "_")))

(defn review
  "Return an admitted resource split-review aggregate from an inventory row."
  [row]
  (let [candidate (:split_review row)]
    (when (and (map? candidate)
               (nonblank-string? (:candidate_set_id candidate))
               (nonblank-string? (:manifest_id candidate))
               (vector? (:splits candidate))
               (seq (:splits candidate)))
      candidate)))

(defn progress
  "Count the latest review state for every split in one aggregate."
  [row]
  (let [splits (:splits (review row))
        counts (frequencies (map #(normalized-status (:review_status %)) splits))
        total (count splits)
        approved (get counts "approved" 0)
        in-review (get counts "in-review" 0)
        rejected (get counts "rejected" 0)
        reviewed (+ approved in-review rejected)]
    {:total total
     :approved approved
     :in-review in-review
     :rejected rejected
     :pending (- total reviewed)
     :reviewed reviewed
     :all-approved? (and (pos? total) (= approved total))}))

(defn all-approved?
  "Whether every ordered split has a latest approved receipt."
  [row]
  (:all-approved? (progress row)))

(defn next-split-index
  "Return the next ordered split index, without wrapping at the final split."
  [segments current-index]
  (->> segments
       (sort-by :segment_index)
       (drop-while #(not= current-index (:segment_index %)))
       second
       :segment_index))

(defn overall-status
  "Derive the inventory badge from actual per-split progress."
  [row]
  (let [{:keys [total approved reviewed rejected]} (progress row)]
    (cond
      (and (pos? total) (= approved total)) "fully_approved"
      (and (pos? total) (= rejected total)) "fully_rejected"
      (pos? reviewed) "partial_review"
      :else (or (some-> row review :status display-status)
                "pending_review"))))

(defn- split-segment-base
  [row candidate-set-id manifest-id
   {:keys [split_id split_index source_text candidate_text review_status]}]
  {:id split_id
   :split_id split_id
   :resource_split true
   :candidate_set_id candidate-set-id
   :manifest_id manifest-id
   :segment_index split_index
   :status (or (display-status review_status) "pending")
   :review_status (normalized-status review_status)
   :source_lang (:source_lang row)
   :target_lang (:target_lang row)
   :source_text source_text
   :translated_text candidate_text
   :candidate_text candidate_text})

(def ^:private history-label-fields
  (into [:review_id :id
         :segment_id
         :labeler_id :labeler_email
         :reviewed_at :ts
         :overall :review_status :status
         :corrected_text :editor_notes]
        review-contract/evaluation-fields))

(defn- review-history
  [labels]
  (if (vector? labels)
    (into []
          (keep (fn [label]
                  (when (map? label)
                    (select-keys label history-label-fields))))
          labels)
    []))

(defn- split->segment
  [row candidate-set-id manifest-id {:keys [corrected_text labels] :as split}]
  (let [history (review-history labels)]
    (cond-> (merge (split-segment-base row candidate-set-id manifest-id split)
                   (select-keys split review-contract/evaluation-fields)
                   (select-keys split [:review_id :reviewed_at :overall
                                       :editor_notes])
                   {:labels history
                    :label_count (count history)})
      (some? corrected_text)
      (assoc :corrected_text corrected_text))))

(defn detail
  "Project an inventory aggregate into the page's ordered annotation shape."
  [row]
  (let [{:keys [candidate_set_id manifest_id splits]} (review row)
        {:keys [total approved]} (progress row)]
    {:document {:title (:title row) :source_lang (:source_lang row)}
     :target_lang (:target_lang row)
     :summary {:total_segments total
               :approved approved
               :overall_status (overall-status row)}
     :segments
     (mapv #(split->segment row candidate_set_id manifest_id %) splits)}))

(defn review-form
  "Hydrate the historical granular review card without cross-split leakage."
  [split]
  (merge review-contract/default-form
         (select-keys split review-contract/review-form-fields)))

(defn- evaluation-evidence
  [form]
  (let [editor-notes (review-contract/trimmed-optional-text
                      (:editor_notes form))]
    (cond-> (review-contract/evaluation-values form)
      editor-notes (assoc :editor_notes editor-notes))))

(defn review-payload
  "Build the closed publication split-review request with granular evidence."
  [row split status form]
  (let [candidate-set-id (some-> row review :candidate_set_id)
        split-id (:split_id split)
        normalized (normalized-status status)
        evidence (evaluation-evidence form)
        corrected-text (review-contract/correction-text
                        (:corrected_text form))]
    (when-not (and (nonblank-string? candidate-set-id)
                   (nonblank-string? split-id)
                   (contains? review-statuses normalized))
      (throw (ex-info "resource split review request is incomplete"
                      {:candidate_set_id candidate-set-id
                       :split_id split-id
                       :status normalized})))
    (cond-> (merge {:candidate_set_id candidate-set-id
                    :split_id split-id
                    :status normalized}
                   evidence)
      corrected-text (assoc :corrected_text corrected-text))))

(defn bulk-review-payload
  "Build a candidate-set-bound review request without client-enumerated splits."
  [row status form]
  (let [candidate-set-id (some-> row review :candidate_set_id)
        normalized (normalized-status status)]
    (when-not (and (nonblank-string? candidate-set-id)
                   (contains? review-statuses normalized))
      (throw (ex-info "resource bulk review request is incomplete"
                      {:candidate_set_id candidate-set-id
                       :status normalized})))
    (merge {:candidate_set_id candidate-set-id
            :status normalized}
           (evaluation-evidence form))))
