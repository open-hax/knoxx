(ns knoxx.backend.shape.source-authoring
  "Closed wire conversion for source authoring shared by human and agent ports."
  (:require [knoxx.backend.law.source-authoring :as law]
            [knoxx.backend.law.source-review :as review]
            [knoxx.backend.shape.source-review :as review-shape]))

(def WireSave
  "Source save authority is never accepted from the request body."
  [:map {:closed true} [:operation_id review/NonBlank]
   [:expected_revision review/NonBlank] [:content review/NonBlank]])

(def WireCreate
  "Explicit resource placement and retry identity for a new wiki document."
  [:map {:closed true} [:operation_id review/NonBlank] [:title review/NonBlank]
   [:content review/NonBlank] [:source_locale review/NonBlank]
   [:garden review/NonBlank] [:target_locales [:vector {:min 1} review/NonBlank]]])

(defn decode-save
  "Reject unknown keys before renaming wire fields."
  [wire]
  (review/assert-valid! :source-authoring/wire-save WireSave wire)
  (review/assert-valid! :source-authoring/save law/SaveCommand
                       {:operation-id (:operation_id wire)
                        :expected-revision (:expected_revision wire) :content (:content wire)}))

(defn decode-create
  "Decode an explicit creation command with qualified garden identity."
  [wire]
  (review/assert-valid! :source-authoring/wire-create WireCreate wire)
  (review/assert-valid! :source-authoring/create law/CreateCommand
                       {:operation-id (:operation_id wire) :title (:title wire)
                        :content (:content wire) :source-locale (keyword (:source_locale wire))
                        :garden (keyword (:garden wire))
                        :target-locales (mapv keyword (:target_locales wire))}))

(defn event->wire
  "Expose source history without redundant tenant authority or manifest internals."
  [event]
  {:id (:source/id event) :actor (update (:source/actor event) :kind name)
   :action (name (:source/action event)) :previous_revision (:source/previous-revision event)
   :revision (:source/revision event) :content (:source/content event)
   :recorded_at (:source/recorded-at event)})

(defn result->wire
  "One source command receipt and current source-review snapshot."
  [result]
  {:existing (:existing? result) :event (event->wire (:event result))
   :review (review-shape/projection->wire (:review result))})
