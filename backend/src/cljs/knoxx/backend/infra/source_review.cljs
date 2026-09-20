(ns knoxx.backend.infra.source-review
  "Revision-bound source review against real resource provenance and canonical files."
  (:require [knoxx.backend.domain.document-admission :as admission]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.source-review :as review]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-authoring-store :as sources]
            [knoxx.backend.infra.source-projection-health :as health]
            [knoxx.backend.infra.source-review-store :as store]
            [knoxx.backend.law.source-review :as law]
            [knoxx.backend.shape.source-review :as shape]))

(defn- refuse! [status code message] (throw (ex-info message {:status status :code code})))
(defn- provider! [dependencies]
  (let [provider (:provider dependencies)]
    (when-not (satisfies? store/ISourceReviewStore provider)
      (refuse! 503 "source_review_provider_unavailable" "Source review persistence is not configured")) provider))
(defn- ^:async declared-source-in!
  [config scope records index]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (let [document (get-in index [:documents (:document scope)])]
    (when-not (and document (admission/document-visible-to-org? scope document))
      (refuse! 404 "source_document_not_found" "Source document was not found"))
    ;; Equal declarations share metadata; the last record supplies provenance,
    ;; matching the publication/translation root index rather than the first root.
    (let [record (last (filter #(and (:ok? %) (= :document (:resource/kind %))
                                     (= (:document scope) (:document/id (resolver/canonicalize-document
                                                                       (publications/single-kind-definition %))))) records))
          root (revisions/resource-source-root config (:resource/file-path record))
          file (await (revisions/canonical-document-path! root document))]
      {:document document :record record :root root :path file :index index})))
(defn- ^:async observe-declared-source!
  [scope {:keys [document path] :as declared}]
  (let [content (when path (await (files/read-text! path)))]
    (when-not content (refuse! 404 "source_content_not_found" "Source content was not found"))
    (assoc declared :snapshot
           (law/assert-valid! :source-review/snapshot law/Snapshot
                              {:document (:document scope) :title (:document/title document)
                               :source-locale (:document/source-locale document)
                               :revision (revisions/content-revision content) :content content}))))
(defn- ^:async observed-source-in!
  [config scope records index]
  (await (observe-declared-source! scope (await (declared-source-in! config scope records index)))))
(defn ^:async declared-source!
  "Validate current visibility and canonical provenance even if projected bytes are missing."
  [config scope]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (let [records (await (publications/resource-records! config))]
    (await (declared-source-in! config scope records (publications/publication-index records)))))
(defn ^:async observed-source!
  "Load one visible source through its actual resource file and canonical path guard."
  [config scope]
  (await (observe-declared-source! scope (await (declared-source! config scope)))))
(defn- ^:async review-observed!
  [scope provider dependencies {:keys [snapshot document]}]
  (when-let [owner (:document/org-id document)]
    (await (health/assert-current! (:source-provider dependencies) (assoc scope :org-id owner) snapshot)))
  (review/project (await (store/read-source-review-events! provider scope)) scope snapshot))
(defn ^:async read!
  "Project exact current source state and immutable review history under one scoped provider."
  [config scope dependencies]
  (let [provider (provider! dependencies)
        observed (await (observed-source! config scope))]
    (await (review-observed! scope provider dependencies observed))))
(defn ^:async read-recovered-source!
  "Read actual repaired bytes using an accepted creation's scoped document authority."
  [config scope dependencies]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (let [source-provider (:source-provider dependencies)]
    (when-not (satisfies? sources/ISourceAuthoringStore source-provider)
      (refuse! 503 "source_authoring_provider_unavailable" "Recovery requires accepted source history"))
    (let [events (sources/validated-history scope (await (sources/source-events! source-provider scope)))]
      (when-not (= :create (:source/action (first events)))
        (refuse! 409 "source_authoring_creation_required" "Recovery requires an accepted creation manifest"))
      (let [document (:source/document (peek events))
            path (await (revisions/canonical-document-path! (revisions/source-root config) document))
            observed (await (observe-declared-source! scope {:document document :path path}))]
        (await (review-observed! scope (provider! dependencies) dependencies observed))))))
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
  "Read selected source authority from one resource snapshot; failures remain failures."
  [config index scope dependencies]
  (let [accepted (atom {})
        documents (filter (fn [[_ document]] (admission/document-visible-to-org? scope document)) (:documents index))]
    (when (seq documents)
      (let [provider (provider! dependencies)
            records (await (publications/resource-records! config))
            resource-index (publications/publication-index records)]
        (doseq [[id _document] documents]
          (let [document-scope (shape/context->scope scope id)
                observed (await (observed-source-in! config document-scope records resource-index))
                current (await (review-observed! document-scope provider dependencies observed))]
            (swap! accepted assoc id current)))))
    {:source-accepted?
     (fn [intent revision]
       (let [current (get @accepted (:publication/document intent))
             document (get-in index [:documents (:publication/document intent)])]
         (boolean (and (:accepted current) (= revision (:revision current))
                       (= (:document/source-locale document) (:source-locale current))))))}))
