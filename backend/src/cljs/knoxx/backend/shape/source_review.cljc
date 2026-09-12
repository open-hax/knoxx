(ns knoxx.backend.shape.source-review
  "Explicit JSON-shaped data conversion for human and agent review commands."
  (:require [knoxx.backend.law.source-review :as law]))

(def WireCommand
  "Closed wire vocabulary; principal, organization and project are forbidden."
  [:map {:closed true}
   [:operation_id law/NonBlank]
   [:revision law/NonBlank]
   [:source_locale law/NonBlank]
   [:expected_head [:maybe law/NonBlank]]
   [:action [:enum "comment" "submit" "request_changes" "accept"]]
   [:notes {:optional true} :string]
   [:corrections {:optional true} [:vector {:max 100} law/Correction]]
   [:lessons {:optional true} [:vector {:max 100} law/NonBlank]]])

(defn decode-command
  "Validate before translating wire keys, so unknown authority never disappears."
  [wire]
  (law/assert-valid! :source-review/wire-command WireCommand wire)
  (law/assert-valid!
   :source-review/command law/Command
   (cond-> {:operation-id (:operation_id wire)
            :revision (:revision wire)
            :source-locale (keyword (:source_locale wire))
            :expected-head (:expected_head wire)
            :action (case (:action wire)
                      "request_changes" :request-changes
                      (keyword (:action wire)))}
     (contains? wire :notes) (assoc :notes (:notes wire))
     (contains? wire :corrections) (assoc :corrections (:corrections wire))
     (contains? wire :lessons) (assoc :lessons (:lessons wire)))))

(defn- qualified-name
  [value]
  (if-let [prefix (namespace value)]
    (str prefix "/" (name value))
    (name value)))

(defn- actor->wire
  [actor]
  (update actor :kind name))

(defn event->wire
  "Expose attributed review history without internal scope duplication."
  [event]
  {:id (:review/id event)
   :actor (actor->wire (:review/actor event))
   :revision (:review/revision event)
   :source_locale (name (:review/source-locale event))
   :previous (:review/previous event)
   :action (case (:review/action event)
             :request-changes "request_changes"
             (name (:review/action event)))
   :notes (:review/notes event)
   :corrections (:review/corrections event)
   :lessons (:review/lessons event)
   :recorded_at (:review/recorded-at event)})

(defn projection->wire
  "Preserve qualified resource identity and explicit underscore wire vocabulary."
  [projection]
  (-> projection
      (dissoc :source-locale)
      (assoc :document (qualified-name (:document projection))
             :source_locale (name (:source-locale projection))
             :status (case (:status projection)
                       :in-review "in_review"
                       :needs-revision "needs_revision"
                       (name (:status projection))))
      (update :history #(mapv event->wire %))
      (update :lessons #(mapv (fn [lesson] (update lesson :actor actor->wire)) %))))

(defn result->wire
  "Encode the same admitted command result for browser and tool callers."
  [result]
  {:existing (:existing? result)
   :event (event->wire (:event result))
   :review (projection->wire (:review result))})
