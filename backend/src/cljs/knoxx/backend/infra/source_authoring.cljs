(ns knoxx.backend.infra.source-authoring
  "Durable source facts followed by repairable canonical filesystem projection."
  (:require [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.domain.source-authoring :as domain]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-authoring-store :as store]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.law.source-authoring :as law]
            [knoxx.backend.law.source-review :as review-law]))

(defn- refuse! [status code message] (throw (ex-info message {:status status :code code})))
(defn- provider! [dependencies]
  (let [provider (:source-provider dependencies)]
    (when-not (satisfies? store/ISourceAuthoringStore provider)
      (refuse! 503 "source_authoring_provider_unavailable" "Source authoring persistence is not configured")) provider))
(defn- own-document! [scope document]
  (when-not (and (:document/org-id document) (= (:org-id scope) (:document/org-id document)))
    (refuse! 403 "source_authoring_owner_required" "Writing a source requires explicit document ownership")) document)
(defn- existing-operation [events command]
  (some #(when (= [:command (:operation-id command)] (law/operation-key %)) %) events))
(defn- assert-save-retry! [event actor command]
  (when-not (= [actor (:expected-revision command) (:content command) :save]
               [(:source/actor event) (:source/previous-revision event) (:source/content event) (:source/action event)])
    (refuse! 409 "source_authoring_operation_conflict" "Source operation identity has different content")))
(defn- ^:async install-source! [root event allowed-revisions]
  (let [relative (get-in event [:source/document :document/source :path])
        file (await (files/contained-path! root relative)) current (await (files/read-text! file))
        current-revision (revisions/content-revision current)]
    (when-not (or (nil? current) (= current-revision (:source/revision event)) (contains? allowed-revisions current-revision))
      (refuse! 409 "source_projection_conflict" "Source projection was independently modified; preserve it before repair"))
    (when-not (= current-revision (:source/revision event))
      (await (files/write-text! root relative (:source/content event))))))
(defn- ^:async project-latest! [root events]
  (let [latest (peek events)]
    (await (install-source! root latest (set (map :source/revision events))))))
(defn- ^:async result! [config scope dependencies admission]
  (assoc admission :review (await (review/read! config scope dependencies))))
(defn- ^:async observe-if-needed! [provider scope actor observed dependencies]
  (let [events (await (store/source-events! provider scope))]
    (when (empty? events)
      (let [snapshot (:snapshot observed)
            event (domain/source-event scope actor (str "observe/" (:revision snapshot)) :observe nil
                                       (:document observed) (:content snapshot) (:revision snapshot) ((:now! dependencies)))]
        (await (store/admit-source! provider scope nil event))))))
(defn- ^:async save-locked! [config scope actor command dependencies]
  (let [provider (provider! dependencies) declared (await (review/declared-source! config scope))
        _ (own-document! scope (:document declared)) events (await (store/source-events! provider scope))]
    (if-let [existing (existing-operation events command)]
      (do (assert-save-retry! existing actor command)
          (when-not (= (:document declared) (:source/document (peek events)))
            (refuse! 409 "source_authoring_document_conflict" "Retry cannot replace changed resource metadata"))
          (await (project-latest! (:root declared) events))
          (await (result! config scope dependencies {:existing? true :event existing})))
      (let [observed (await (review/observed-source! config scope))
            _ (own-document! scope (:document observed))
            snapshot (:snapshot observed) latest (peek events)]
        (when-not (= (:expected-revision command) (:revision snapshot))
          (refuse! 409 "source_authoring_stale_revision" "Source changed; refresh before saving"))
        (when (and latest (not= (:source/revision latest) (:revision snapshot)))
          (refuse! 503 "source_projection_repair_required" "Retry the pending source command before another edit"))
        (await (observe-if-needed! provider scope actor observed dependencies))
        (let [event (domain/source-event scope actor (:operation-id command) :save (:expected-revision command)
                                         (:document observed) (:content command) (revisions/content-revision (:content command))
                                         ((:now! dependencies)))
              admitted (await (store/admit-source! provider scope (:expected-revision command) event))]
          (await (install-source! (:root observed) (:event admitted) #{(:expected-revision command)}))
          (await (result! config scope dependencies admitted)))))))
(defn ^:async save!
  "Save only source bytes with a caller-stable operation and exact expected revision."
  [config scope actor command dependencies]
  (review-law/assert-valid! :source-authoring/scope review-law/Scope scope)
  (review-law/assert-valid! :source-authoring/actor review-law/Actor actor)
  (review-law/assert-valid! :source-authoring/save law/SaveCommand command)
  (await (files/with-document-lock! scope #(save-locked! config scope actor command dependencies))))
(defn- assert-create-retry! [event actor command]
  (let [document (:source/document event) publications (rest (get-in event [:source/manifest :resources]))]
    (when-not (= [actor (:content command) (:title command) (:source-locale command) #{(:garden command)}
                  (set (cons (:source-locale command) (:target-locales command))) :create]
                 [(:source/actor event) (:source/content event) (:document/title document) (:document/source-locale document)
                  (set (map :publication/garden publications)) (set (map :publication/locale publications)) (:source/action event)])
      (refuse! 409 "source_authoring_operation_conflict" "Create identity has different content or destinations"))))
(defn- ^:async project-creation! [config resources events]
  (let [root (revisions/source-root config) contract-root (contracts/contracts-dir-path config)
        relative (:manifest-path resources) manifest-file (await (files/contained-path! contract-root relative))
        current (await (files/read-text! manifest-file))]
    (when (and current (not (domain/creation-manifest-compatible? (:manifest resources) (files/parse-manifest current))))
      (refuse! 409 "source_manifest_conflict" "Creation manifest has different identity or resource siblings"))
    (await (project-latest! root events))
    (when-not current (await (files/write-text! contract-root relative (files/manifest-text (:manifest resources)))))))
(defn- ^:async create-locked! [config scope actor command identity dependencies]
  (let [provider (provider! dependencies) events (await (store/source-events! provider scope))
        existing (existing-operation events command)
        resources (if existing
                    (do (assert-create-retry! existing actor command)
                        (assoc identity :resource (:source/document existing) :manifest (:source/manifest existing)))
                    (do (when (seq events)
                          (refuse! 409 "source_authoring_identity_conflict" "This document already has source history"))
                        (let [index (await (publications/publication-index! config))]
                          (domain/creation-resources scope command (get-in index [:gardens (:garden command)])))))
        admission (if existing {:existing? true :event existing}
                      (let [event (domain/source-event scope actor (:operation-id command) :create nil
                                                       (:resource resources) (:content command)
                                                       (revisions/content-revision (:content command))
                                                       ((:now! dependencies)) (:manifest resources))]
                        (await (store/admit-source! provider scope nil event))))
        current (await (store/source-events! provider scope))]
    (await (project-creation! config resources current))
    (if existing
      (assoc admission :review (await (review/read-recovered-source! config scope dependencies)))
      (await (result! config scope dependencies admission)))))
(defn ^:async create!
  "Choose an explicit garden/locales and admit full bytes before projecting its resource."
  [config scope actor command dependencies]
  (review-law/assert-valid! :source-authoring/actor review-law/Actor actor)
  (review-law/assert-valid! :source-authoring/create law/CreateCommand command)
  (let [identity (domain/creation-identity scope command)
        scope (assoc scope :document (:document identity))]
    (review-law/assert-valid! :source-authoring/scope review-law/Scope scope)
    (await (files/with-document-lock! scope #(create-locked! config scope actor command identity dependencies)))))
(defn list! "List the actual scoped resource document inventory." [config scope _dependencies]
  (publications/list-publication-documents! config scope))
(defn ^:async assist-input!
  "Prepare actual current content and attributed accepted lessons for the selected model port."
  [config scope dependencies]
  (select-keys (await (review/read! config scope dependencies))
               [:document :revision :source-locale :title :content :lessons]))
