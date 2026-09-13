(ns knoxx.backend.infra.cms-store
  "Knoxx organization adapter for the shared Clio document history package."
  (:require [clojure.string :as str]
            [document-history.infra.store :as history]
            [knoxx.backend.domain.cms-document :as domain]
            [knoxx.backend.extern.cms-store :as files]
            [knoxx.backend.law.cms-document :as law]))

(defn- open! [org]
  (law/require-id! org)
  (history/open! (:history (files/paths org "index"))))

(defn- metadata [record]
  (select-keys record [:doc_id :title :visibility :garden_id :metadata :legacy_source_path]))

(defn- import-legacy! [store org id]
  (or (history/read! store id)
      (when-let [old (files/legacy-record org id)]
        (law/require-body! old)
        (history/seed! store
                       {:document/id id
                        :document/metadata (-> (domain/record org id nil old old)
                                               (assoc :metadata (or (:metadata old) {})
                                                      :legacy_source_path (:source (files/paths org id)))
                                               metadata)
                        :document/markdown (:content old)
                        :revision/parents []
                        :revision/actor "system:legacy-cms-import"}))))

(defn- record [projection]
  (when projection
    (merge (dissoc (:document/metadata projection) :legacy_source_path)
           {:doc_id (:document/id projection)
            :content (:document/markdown projection)
            :source_path (:snapshot/markdown-path projection)
            :revision (:revision/selected projection)
            :revision_heads (:revision/heads projection)
            :conflicted (:revision/conflicted? projection)})))

(defn- display-record [store projection]
  (when projection
    (assoc (record (history/read-revision! store (:document/id projection) (:revision/selected projection)))
           :revision_heads (:revision/heads projection)
           :conflicted (:revision/conflicted? projection))))

(defn- ensure-resources! [org projection]
  (let [value (record projection)
        id (:doc_id value)
        paths (files/paths org id)]
    (files/create-resource! (:garden paths) (domain/garden org))
    (files/create-resource! (:manifest paths)
                            (domain/manifest org id (:source_path value) (:title value)))
    projection))

(defn read! [org id]
  (law/require-id! id)
  (let [store (open! org)]
    (some->> (import-legacy! store org id)
             (ensure-resources! org) (display-record store))))

(defn- source-paths [store projection]
  (vec (distinct
        (remove nil?
                (cons (get-in projection [:document/metadata :legacy_source_path])
                      (map #(:snapshot/markdown-path
                             (history/read-revision! store (:document/id projection) (:revision/id %)))
                           (:revision/history projection)))))))

(defn list! [org]
  (let [store (open! org)]
    (doseq [id (files/legacy-ids org)]
      (law/require-id! id)
      (import-legacy! store org id))
    (mapv (fn [projection]
            (assoc (display-record store (ensure-resources! org projection))
                   :source_paths (source-paths store projection)))
          (history/list! store))))

(defn history! [org id]
  (law/require-id! id)
  (let [store (open! org)
        projection (or (import-legacy! store org id)
                       (throw (ex-info "CMS document not found" {:status 404})))]
    {:document (display-record store projection)
     :revisions (mapv (fn [revision]
                        (merge (record (history/read-revision! store id (:revision/id revision)))
                               {:actor (:revision/actor revision) :at (:revision/at revision)
                                :parents (:revision/parents revision)}))
                      (:revision/history projection))}))

(defn- revision-operation! [operation]
  (try (operation)
       (catch :default error
         (if (contains? #{:invalid-command :invalid-id :unknown-parent :cross-document-parent}
                        (:document-history/error (ex-data error)))
           (throw (ex-info "Invalid CMS revision or parent" {:status 400} error))
           (throw error)))))

(defn save! [org actor id body]
  (law/require-id! org)
  (law/require-body! body)
  (law/require-parents! id body)
  (let [store (open! org)
        previous (when id (or (import-legacy! store org (law/require-id! id))
                              (throw (ex-info "CMS document not found" {:status 404}))))
        observed (when id (revision-operation! #(history/read-revision! store id (first (:parents body)))))
        id (or id (str (random-uuid)))
        value (domain/record org id nil body (:document/metadata observed))
        projection (revision-operation!
                    #(history/commit! store
                                      {:document/id id
                                       :document/metadata (metadata (assoc value :legacy_source_path
                                                                          (get-in previous [:document/metadata :legacy_source_path])))
                                       :document/markdown (:content value)
                                       :revision/parents (or (:parents body) [])
                                       :revision/actor actor}))
        own (history/read-revision! store id (:commit/revision projection))]
    (ensure-resources! org projection)
    ;; Return the writer's branch while keeping all live heads visible.
    (assoc (record own) :revision_heads (:revision/heads projection)
                       :conflicted (:revision/conflicted? projection))))

(defn project-resource [resource]
  ;; Only our generated document declarations participate. Publication intent
  ;; retains its authority; document revisions never rewrite that file.
  (let [definition (:resource/definition resource)
        org (:document/org-id definition)
        document-id (:document/id definition)
        local-id (when (keyword? document-id) (name document-id))]
    (if (and (:ok? resource) (= :document (:resource/kind resource))
             (files/configured?) (string? org) (keyword? document-id)
             (= (namespace document-id) (str "cms." org))
             (str/starts-with? local-id "doc-")
             (= (:resource/file-path resource) (:manifest (files/paths org (subs local-id 4)))))
      (if-let [value (read! org (subs local-id 4))]
        (assoc resource :resource/definition
               (assoc definition :document/title (:title value)
                                 :document/source {:path (:source_path value)}))
        resource)
      resource)))

(defn require-resolved! [document]
  (let [org (:document/org-id document)
        id (:document/id document)]
    (when (and (files/configured?) (string? org) (keyword? id)
               (= (namespace id) (str "cms." org))
               (str/starts-with? (name id) "doc-"))
      (when (:conflicted (read! org (subs (name id) 4)))
        (throw (ex-info "Review and resolve concurrent document edits before publishing or translating"
                        {:status 409 :code "cms_revision_conflict"})))))
  document)
