(ns knoxx.backend.infra.resources.index
  "Refresh the in-process resource index and coordinate native watcher ownership."
  (:require [clojure.set :as set]
            [knoxx.backend.domain.resources.editing :as editing]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.extern.resource-watcher :as watcher]
            [knoxx.backend.infra.event-runtime :as event-runtime]))

(defn- resource-id->index-key
  [resource-class resource-id]
  (str (editing/normalize-resource-class resource-class) "/" resource-id))

(defonce ^:private resource-index* (atom #{}))

(defn ^:async sync-resource-index!
  "Sync resource EDN files → in-memory resource index set.

   The index is a fast in-process cache; disk is canonical. Invalid resource
   files are omitted by the loader and must not block backend startup or the
   repair UI."
  [config]
  (try
    (let [records (await (resources/load-all-resources! config))
          ids (->> records
                   (map (fn [record]
                          (resource-id->index-key (:resource/class record)
                                                  (:resource/id record))))
                   distinct
                   sort
                   vec)
          existing-set @resource-index*
          desired-set (set ids)
          to-add (vec (sort (set/difference desired-set existing-set)))
          to-remove (vec (sort (set/difference existing-set desired-set)))]
      (reset! resource-index* desired-set)
      (println "[resources] synced resource index; add=" (count to-add) "remove=" (count to-remove))
      {:ok true
       :added to-add
       :removed to-remove
       :count (count ids)})
    (catch :default err
      (println "[resources] sync-resource-index! failed; startup continuing:" (ex-message err))
      {:ok false :error (ex-message err)})))

(defn sync-contract-index!
  "Compatibility alias for old contract route callers."
  [config]
  (sync-resource-index! config))

(defonce ^:private active-watcher* (atom nil))

(defn- ^:async resource-refresh! [config]
  (try
    (await (sync-resource-index! config))
    (event-runtime/debounced-reload!)
    (println "[resources] event runtime reload queued after resource change")
    (catch :default error
      (println "[resources] watcher refresh failed:" (ex-message error)))))

(defn stop-resource-watcher!
  "Close native watchers and cancel any pending resource refresh."
  []
  (when-let [close! (:close! @active-watcher*)] (close!))
  (reset! active-watcher* nil)
  nil)

(defn start-resource-watcher!
  "Start one resource watcher set; retry later when no existing root is watchable."
  [config]
  (when-not @active-watcher*
    (let [watching (watcher/start! (resources/resource-root-paths config) #(resource-refresh! config))]
      (when (pos? (:count watching))
        (reset! active-watcher* watching)
        (println "[resources] watching" (:count watching) "resource roots for live reload")))))
