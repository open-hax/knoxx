(ns knoxx.backend.infra.resources.writes
  "Serialize resource writes and preserve their reversible and durable admission boundaries."
  (:require [knoxx.backend.domain.resources.editing :as editing]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.extern.resource-files :as io]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.resources.index :as index]))

(def ^:private translation-admission-permission
  "org.translations.manage")

(def ^:private publication-admission-permission
  "org.publications.manage")

(defn admission-document-id
  "Return the document whose translation inventory changed after a resource write."
  [resource-class resource]
  (case (editing/normalize-resource-class resource-class)
    "documents" (:document/id resource)
    "publications" (:publication/document resource)
    nil))

(defn admission-scope
  "Project an authenticated write context into the trusted admission scope."
  [config ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)
        membership-id (some-> (authz/ctx-membership-id ctx) str not-empty)]
    (when-not (and org-id membership-id)
      (throw (ex-info "automatic document admission requires an organization and membership context"
                      {:status 403
                       :code "document_admission_context_required"})))
    (cond-> {:org-id org-id :membership-id membership-id}
      (some-> (:session-project-name config) str not-empty)
      (assoc :project (str (:session-project-name config))))))

(defn ^:async admit-saved-publication-resource!
  "Admit a saved document/publication through the app-owned internal hook."
  [config ctx resource-class resource admit!]
  (when-let [document-id (admission-document-id resource-class resource)]
    (when ctx
      (authz/ensure-permission! ctx translation-admission-permission)
      (authz/ensure-permission! ctx publication-admission-permission))
    (let [result (await (admit! (admission-scope config ctx)
                                {:document document-id}))]
      (when-not (and (true? (:ok result))
                     (zero? (or (:failed result) 0))
                     (pos? (or (:admitted result) 0)))
        (throw (ex-info "automatic document admission did not complete"
                        {:status 503
                         :code "document_admission_failed"
                         :document/id document-id
                         :admission result})))
      result)))

(defn- missing-file-error?
  [err]
  (= "ENOENT" (or (io/error-code err) (:code (ex-data err)))))

(defn- ^:async require-index-sync!
  [sync-index! config file-path phase]
  (let [result (await (sync-index! config))]
    (when-not (true? (:ok result))
      (throw (ex-info "resource index synchronization failed"
                      {:status 503
                       :code "resource_index_sync_failed"
                       :resource/path file-path
                       :resource/index-sync-phase phase
                       :resource/index-sync-result result})))
    result))

(defonce ^:private resource-write-tails* (atom {}))

(defn- ^:async run-after-resource-write!
  [previous task-fn]
  (when previous
    (try
      (await previous)
      ;; knoxx-lint/allow-silent-catch — an earlier write must not poison this resource queue.
      (catch :default _
        nil)))
  (await (task-fn)))

(defn- ^:async recover-resource-write-tail!
  [task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — only the stored recovery tail consumes this rejection.
    (catch :default _
      nil)))

(defn- ^:async retire-resource-write-tail!
  [file-path tail]
  (await tail)
  (swap! resource-write-tails*
         (fn [tails]
           (if (identical? tail (get tails file-path))
             (dissoc tails file-path)
             tails))))

(defn- enqueue-resource-write!
  "Run one complete write transaction after earlier work for the exact path."
  [file-path task-fn]
  ;; ClojureScript runs this deref/create/assoc sequence without yielding. The
  ;; task itself first yields only after the tail has been installed, so another
  ;; request cannot observe a stale predecessor on the single JS event loop.
  (let [task (run-after-resource-write!
              (get @resource-write-tails* file-path) task-fn)
        tail (recover-resource-write-tail! task)]
    (swap! resource-write-tails* assoc file-path tail)
    (retire-resource-write-tail! file-path tail)
    task))

(defn- ^:async rollback-resource-write!
  [config file-path previous write-file! delete-file! sync-index!]
  (if (:exists? previous)
    (await (write-file! file-path (:text previous)))
    (try
      (await (delete-file! file-path))
      (catch :default delete-err
        (when-not (missing-file-error? delete-err)
          (throw delete-err)))))
  (await (require-index-sync! sync-index! config file-path :rollback)))

(defn- resource-write-rollback-error
  [file-path write-error rollback-error]
  (ex-info "resource write/index synchronization failed and its file rollback also failed"
           {:status 500
            :code "resource_write_rollback_failed"
            :resource/path file-path
            :write/error (or (ex-message write-error)
                             (str write-error))
            :rollback/error (or (ex-message rollback-error)
                                (str rollback-error))}
           rollback-error))

(defn- ^:async write-resource-and-admit-once!
  "Perform one resource write/admission transaction while its path is owned."
  [config file-path edn-text admission!
   {:keys [read-file! write-file! delete-file! sync-index!] :as _deps}]
  (let [read-file! (or read-file! (fn [path] (io/read-text! path)))
        write-file! (or write-file! resources/write-edn-file!)
        delete-file! (or delete-file! (fn [path] (io/delete-file! path)))
        sync-index! (or sync-index! index/sync-resource-index!)
        previous (try
                   {:exists? true :text (await (read-file! file-path))}
                   (catch :default err
                     (if (missing-file-error? err)
                       {:exists? false}
                       (throw err))))]
    (try
      (await (write-file! file-path edn-text))
      (await (require-index-sync! sync-index! config file-path :forward))
      (catch :default err
        (try
          (await (rollback-resource-write!
                  config file-path previous write-file! delete-file! sync-index!))
          (catch :default rollback-err
            (throw (resource-write-rollback-error file-path err rollback-err))))
        (throw err)))
    ;; Invoking admission crosses into durable event and queue effects that the
    ;; filesystem cannot roll back. Preserve the exact entered bytes if that
    ;; work fails so a retry or reconciliation pass still has its source.
    (when admission! (await (admission!)))))

(defn ^:async write-resource-and-admit!
  "Persist one resource and run its admission action as one serialized write.

  Filesystem and translation/event persistence cannot share a transaction. This
  boundary therefore treats writing plus forward index synchronization as its
  reversible phase: either failure restores the previous file (or removes a
  newly created one), refreshes the index, and rethrows the original failure.

  Invoking admission begins an irreversible phase because it may append events
  or enqueue agents before failing. An admission failure is rethrown without
  restoring or deleting the entered file. Its exact bytes remain available to
  retry or reconciliation instead of leaving durable effects whose source has
  vanished.

  The snapshot, write, sync, admission, and any pre-admission rollback are
  serialized by exact file path. A newer request therefore observes the bytes
  actually left by the preceding request before publishing its own.

  The optional dependency map exists for focused failure-path tests; production
  uses the real resource writer, filesystem, and index sync functions."
  ([config file-path edn-text admission!]
   (write-resource-and-admit! config file-path edn-text admission! {}))
  ([config file-path edn-text admission!
    deps]
   (await
    (enqueue-resource-write!
     file-path
     (fn []
       (write-resource-and-admit-once!
        config file-path edn-text admission! deps))))))
