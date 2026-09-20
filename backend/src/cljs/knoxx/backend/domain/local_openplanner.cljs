(ns knoxx.backend.domain.local-openplanner
  "Finite replay vocabulary for canonical local OpenPlanner operations."
  (:require [knoxx.backend.domain.local-openplanner-batches :as batches]
            [knoxx.backend.domain.local-openplanner-events :as events]
            [knoxx.backend.domain.local-openplanner-translations :as translations]
            [knoxx.backend.law.local-openplanner :as law]
            [knoxx.backend.law.openplanner-translation :as contract]))

(defn transition
  "Replay one explicit operation against plain state; no transport effects."
  [state {:keys [kind events id input at vectors required]}]
  (case kind
    :events (let [[next-state result] (events/admit-events state events)
                  fresh (filterv #(not (contains? (:events state) (:id %))) events)]
              [(translations/project-events next-state fresh) result])
    :extra (events/repair-extra state id required)
    :vectors (events/project-vectors state vectors)
    :segment (translations/create-segment state input at)
    :label (translations/label-segment state (:segment-id input) (:request input) id at)
    :review (translations/review-document state (:document-id input) (:language input) (:request input) id at)
    :batch (batches/create state input id at)
    :claim (batches/claim state input at)
    :batch-status (batches/update-status state id input at)
    (throw (ex-info "Unknown local OpenPlanner operation" {:status 400 :kind kind}))))

(defn query
  "Read only named operations; a Mongo expression is never a local query."
  [state {:keys [kind id opts at language vector model dimensions]}]
  (case kind
    :event (do (law/assert-valid! law/NonBlank id) (get-in state [:events id]))
    :vector (get-in state [:vectors id])
    :events (events/rows state opts)
    :session (events/session state id opts)
    :sessions (events/sessions state opts)
    :search (events/vector-search state opts vector model dimensions)
    :segment (translations/segment state id opts)
    :segments (do
                (contract/assert-valid! :local/segments contract/TranslationSegmentsRequest opts)
                (let [all (translations/current-segments state opts)
                      offset (max 0 (or (:offset opts) 0))
                      limit (min 100 (max 1 (or (:limit opts) 50)))]
                  {:segments (vec (take limit (drop offset all))) :total (count all)
                   :has_more (< (+ offset limit) (count all))}))
    :document (translations/document state id language opts)
    :documents (translations/documents state opts)
    :manifest (translations/manifest state opts at)
    :sft (filterv #(= "approved" (:status %)) (translations/current-segments state opts))
    :batch (batches/batch state id opts)
    :batches (batches/batches state opts)
    (throw (ex-info "Unknown local OpenPlanner query" {:status 400 :kind kind}))))
