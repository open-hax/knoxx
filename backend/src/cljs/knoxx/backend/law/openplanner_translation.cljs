(ns knoxx.backend.law.openplanner-translation
  "Malli contracts for Knoxx's direct OpenPlanner translation SDK boundary."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  [:and string? [:fn {:error/message "must not be blank"} #(not (str/blank? %))]])

(def OptionalString
  [:maybe string?])

(def QueryNumber
  [:maybe [:or int? string?]])

(def SegmentStatus
  [:enum "pending" "in_review" "approved" "rejected"])

(def LabelOverall
  [:enum "approve" "needs_edit" "reject"])

(def TranslationSegmentsRequest
  [:map {:closed false}
   [:project {:optional true} OptionalString]
   [:org_id {:optional true} OptionalString]
   [:status {:optional true} OptionalString]
   [:source_lang {:optional true} OptionalString]
   [:target_lang {:optional true} OptionalString]
   [:domain {:optional true} OptionalString]
   [:document_id {:optional true} OptionalString]
   [:limit {:optional true} QueryNumber]
   [:offset {:optional true} QueryNumber]])

(def TranslationSegmentsResponse
  [:map {:closed false}
   [:segments [:vector [:map {:closed false}]]]
   [:total int?]
   [:has_more boolean?]])

(def TranslationSegmentResponse
  [:map {:closed false}
   [:id NonBlankString]
   [:labels [:vector [:map {:closed false}]]]])

(def CreateTranslationSegmentRequest
  [:map {:closed false}
   [:source_text NonBlankString]
   [:translated_text NonBlankString]
   [:target_lang NonBlankString]
   [:document_id NonBlankString]
   [:segment_index int?]
   [:source_lang {:optional true} OptionalString]
   [:status {:optional true} [:maybe SegmentStatus]]
   [:garden_id {:optional true} OptionalString]
   [:mt_model {:optional true} OptionalString]
   [:confidence {:optional true} [:maybe number?]]
   [:domain {:optional true} OptionalString]
   [:content_type {:optional true} OptionalString]
   [:url_context {:optional true} OptionalString]
   [:org_id {:optional true} OptionalString]
   [:project {:optional true} OptionalString]])

(def CreateTranslationSegmentResponse
  [:map {:closed false}
   [:ok boolean?]
   [:id NonBlankString]
   [:status SegmentStatus]
   [:upserted boolean?]
   [:modified boolean?]])

(def LabelTranslationSegmentRequest
  [:map {:closed false}
   [:adequacy [:enum "excellent" "good" "adequate" "poor" "unusable"]]
   [:fluency [:enum "excellent" "good" "adequate" "poor" "unusable"]]
   [:terminology [:enum "correct" "minor_errors" "major_errors"]]
   [:risk [:enum "safe" "sensitive" "policy_violation"]]
   [:overall LabelOverall]
   [:corrected_text {:optional true} OptionalString]
   [:editor_notes {:optional true} OptionalString]
   [:labeler_id {:optional true} OptionalString]
   [:labeler_email {:optional true} OptionalString]
   [:org_id {:optional true} OptionalString]])

(def LabelTranslationSegmentResponse
  [:map {:closed false}
   [:ok boolean?]
   [:label [:map {:closed false}]]
   [:new_status SegmentStatus]
   [:graph_memory {:optional true} [:maybe [:map {:closed false}]]]])

(def TranslationManifestRequest
  [:or
   NonBlankString
   [:map {:closed false}
    [:project {:optional true} OptionalString]
    [:org_id {:optional true} OptionalString]]])

(def ManifestLanguage
  [:map {:closed false}
   [:total_segments int?]
   [:approved int?]
   [:rejected int?]
   [:pending int?]
   [:in_review int?]
   [:with_corrections int?]
   [:avg_labels_per_segment number?]])

(def TranslationManifestResponse
  [:map {:closed false}
   [:project string?]
   [:languages [:map-of [:or keyword? string?] ManifestLanguage]]
   [:labelers [:vector [:map {:closed false}]]]
   [:export_sizes [:map-of [:or keyword? string?] [:map {:closed false}]]]
   [:generated_at {:optional true} string?]])

(def TranslationSftRequest
  [:map {:closed false}
   [:project {:optional true} OptionalString]
   [:target_lang {:optional true} OptionalString]
   [:org_id {:optional true} OptionalString]
   [:include_corrected {:optional true} [:maybe [:or boolean? string?]]]])

(def TranslationSftResponse string?)

(def CreateTranslationSegmentsBatchRequest
  [:map {:closed false}
   [:segments [:vector CreateTranslationSegmentRequest]]
   [:org_id {:optional true} OptionalString]
   [:project {:optional true} OptionalString]])

(def CreateTranslationSegmentsBatchResponse
  [:map {:closed false}
   [:ok boolean?]
   [:imported int?]
   [:errors int?]
   [:results [:vector [:map {:closed false}]]]
   [:errors_detail {:optional true} [:vector [:map {:closed false}]]]])

(def TranslationDocumentsRequest
  [:map {:closed false}
   [:project {:optional true} OptionalString]
   [:target_lang {:optional true} OptionalString]
   [:source_lang {:optional true} OptionalString]
   [:garden_id {:optional true} OptionalString]
   [:org_id {:optional true} OptionalString]])

(def TranslationDocumentsResponse
  [:map {:closed false}
   [:documents [:vector [:map {:closed false}]]]
   [:total int?]])

(def TranslationDocumentResponse
  [:map {:closed false}
   [:document [:map {:closed false}]]
   [:segments [:vector [:map {:closed false}]]]
   [:summary [:map {:closed false}
              [:total_segments int?]
              [:approved int?]
              [:pending int?]
              [:rejected int?]
              [:in_review int?]
              [:overall_status string?]]]])

(def ReviewTranslationDocumentRequest
  [:map {:closed false}
   [:overall LabelOverall]
   [:editor_notes {:optional true} OptionalString]
   [:labeler_email {:optional true} OptionalString]
   [:labeler_id {:optional true} OptionalString]
   [:segment_overrides {:optional true}
    [:map-of [:or string? keyword?]
     [:map {:closed false}
      [:overall {:optional true} LabelOverall]
      [:corrected_text {:optional true} OptionalString]
      [:editor_notes {:optional true} OptionalString]]]]])

(def ReviewTranslationDocumentResponse
  [:map {:closed false}
   [:ok boolean?]
   [:document_id NonBlankString]
   [:target_lang NonBlankString]
   [:segments_reviewed int?]
   [:overall LabelOverall]
   [:overrides_applied {:optional true} int?]
   [:failed_segments {:optional true} [:vector any?]]])

(def CreateTranslationBatchRequest
  [:map {:closed false}
   [:garden_id NonBlankString]
   [:target_lang NonBlankString]
   [:document_ids [:vector NonBlankString]]
   [:source_lang {:optional true} OptionalString]
   [:project {:optional true} OptionalString]])

(def CreateTranslationBatchResponse
  [:map {:closed false}
   [:ok boolean?]
   [:batch_id NonBlankString]
   [:id NonBlankString]
   [:status string?]
   [:document_ids [:vector NonBlankString]]])

(def TranslationBatchesRequest
  [:map {:closed false}
   [:garden_id {:optional true} OptionalString]
   [:target_lang {:optional true} OptionalString]
   [:status {:optional true} OptionalString]])

(def TranslationBatchesResponse
  [:map {:closed false}
   [:batches [:vector [:map {:closed false}]]]])

(def NextTranslationBatchResponse
  [:map {:closed false}
   [:batch [:maybe [:map {:closed false}]]]])

(def TranslationBatchResponse
  [:map {:closed false}
   [:id NonBlankString]
   [:batch_id NonBlankString]
   [:status string?]])

(def UpdateTranslationBatchRequest
  [:map {:closed false}
   [:status [:enum "processing" "complete" "partial" "failed"]]
   [:completed_document {:optional true} OptionalString]
   [:failed_document {:optional true} [:maybe [:map {:closed false}]]]
   [:agent_session_id {:optional true} OptionalString]
   [:agent_conversation_id {:optional true} OptionalString]
   [:agent_run_id {:optional true} OptionalString]
   [:error {:optional true} OptionalString]])

(def UpdateTranslationBatchResponse
  [:map {:closed false}
   [:ok boolean?]
   [:batch_id NonBlankString]
   [:status string?]])

(defn assert-valid!
  "Return value when it satisfies schema; otherwise throw a named boundary contract violation."
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "OpenPlanner translation contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))
