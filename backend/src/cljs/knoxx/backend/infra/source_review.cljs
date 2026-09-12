(ns knoxx.backend.infra.source-review
  "Revision-bound source review against real resource provenance and canonical files."
  (:require [knoxx.backend.domain.document-admission :as admission]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.source-review :as review]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-projection-health :as health]
            [knoxx.backend.infra.source-review-store :as store]
            [knoxx.backend.law.source-review :as law]))

(defn- refuse! [status code message] (throw (ex-info message {:status status :code code})))
(defn- provider! [dependencies]
  (let [provider (:provider dependencies)]
    (when-not (satisfies? store/ISourceReviewStore provider)
      (refuse! 503 "source_review_provider_unavailable" "Source review persistence is not configured")) provider))
(defn ^:async observed-source!
  "Load one visible source through its actual resource file and canonical path guard."
  [config scope]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (let [records (await (publications/resource-records! config)) index (publications/publication-index records)
        document (get-in index [:documents (:document scope)])]
    (when-not (and document (admission/document-visible-to-org? scope document))
      (refuse! 404 "source_document_not_found" "Source document was not found"))
    (let [record (some #(when (and (:ok? %) (= :document (:resource/kind %))
                                   (= (:document scope) (:document/id (resolver/canonicalize-document
                                                                     (publications/single-kind-definition %))))) %) records)
          root (revisions/resource-source-root config (:resource/file-path record))
          file (await (revisions/canonical-document-path! root document))
          content (when file (await (files/read-text! file)))]
      (when-not content (refuse! 404 "source_content_not_found" "Source content was not found"))
      {:document document :record record :root root :path file :index index
       :snapshot (law/assert-valid! :source-review/snapshot law/Snapshot
                                    {:document (:document scope) :title (:document/title document)
                                     :source-locale (:document/source-locale document)
                                     :revision (revisions/content-revision content) :content content})})))
(defn ^:async read!
  "Project exact current source state and immutable review history under one scoped provider."
  [config scope dependencies]
  (let [provider (provider! dependencies) {:keys [snapshot document]} (await (observed-source! config scope))]
    (when-let [owner (:document/org-id document)]
      (await (health/assert-current! (:source-provider dependencies) (assoc scope :org-id owner) snapshot)))
    (review/project (await (store/read-source-review-events! provider scope)) scope snapshot)))
(defn ^:async command!
  "Validate the real current revision, then let the store atomically check its review head."
  [config scope actor command dependencies]
  (await (files/with-document-lock!
          scope
          (^:async fn []
            (let [provider (provider! dependencies) {:keys [snapshot document]} (await (observed-source! config scope))
                  _ (when-let [owner (:document/org-id document)]
      (await (health/assert-current! (:source-provider dependencies) (assoc scope :org-id owner) snapshot)))
                  events (await (store/read-source-review-events! provider scope))
                  decision (review/decide events scope actor snapshot command ((:now! dependencies)))
                  result (if (:existing? decision) decision
                           (await (store/admit-source-review! provider scope (:expected-head command) (:event decision))))]
              (assoc result :review (review/project (await (store/read-source-review-events! provider scope)) scope snapshot)))))))
(defn ^:async accepted-source?
  "Return false for missing source acceptance; provider or projection failures remain failures."
  [config scope revision locale dependencies]
  (let [current (await (read! config scope dependencies))]
    (and (:accepted current) (= revision (:revision current)) (= locale (:source-locale current)))))

(defn ^:async acceptance-facts!
  "Read exact source authority once for the gate; unrelated provider failures are never truthy."
  [config index scope dependencies]
  (let [accepted (atom {})]
    (doseq [[id document] (:documents index)]
      (when (admission/document-visible-to-org? scope document)
        (let [current (await (read! config (assoc scope :document id) dependencies))]
          (swap! accepted assoc id current))))
    {:source-accepted?
     (fn [intent revision]
       (let [current (get @accepted (:publication/document intent))
             document (get-in index [:documents (:publication/document intent)])]
         (boolean (and (:accepted current) (= revision (:revision current))
                       (= (:document/source-locale document) (:source-locale current))))))}))
