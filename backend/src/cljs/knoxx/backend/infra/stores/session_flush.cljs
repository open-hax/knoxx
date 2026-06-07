(ns knoxx.backend.infra.stores.session-flush
  "Periodic flush of stale active runs to OpenPlanner.

   Purpose: safety net for runs that were never explicitly completed
   (e.g. process crash mid-stream, network timeout).  These runs stay
   'running' in Mongo until something archives them.  This job detects
   them early and archives them as 'failed'."
  (:require [knoxx.backend.infra.stores.session-store-registry :as store-registry]
            [knoxx.backend.infra.stores.mongo-session-store :as mongo-session-store]
            [knoxx.backend.shape.session-persistence :refer [put-run! list-active-runs]]
            [knoxx.backend.domain.time :refer [now-iso]]))

(def ^:private DEFAULT_INACTIVE_THRESHOLD_MS (* 12 60 60 1000)) ; 12h without update
(def ^:private FLUSH_INTERVAL_MS             (* 20 60 1000))    ; scan every 20 minutes

(defonce ^:private interval-handle* (atom nil))

(defn- run-inactive?
  [run threshold-ms]
  (let [updated-ms (try (.getTime (js/Date. (or (:updated_at run) "")))
                        (catch :default _ 0))]
    (and (not (:has_active_stream run))
         (pos? updated-ms)
         (>= (- (.now js/Date) updated-ms) threshold-ms))))

(defn ^:async archive-stale-run!
  [store run]
  (let [archived (assoc run
                        :status "failed"
                        :error "session-ttl-expired"
                        :has_active_stream false
                        :updated_at (now-iso))]
    (try
      (await (put-run! store archived))
      (catch :default err
        (.warn js/console "[session-flush] stale run archive failed"
               (clj->js {:run-id (:run_id run)
                         :session-id (:session_id run)
                         :error (ex-message err)}))))))

(defn ^:async archive-stale-session-runs!
  [store session-id threshold-ms]
  (try
    (let [runs (await (list-active-runs store session-id))]
      (await (js/Promise.all
              (clj->js
               (keep (fn [run]
                       (when (run-inactive? run threshold-ms)
                         (archive-stale-run! store run)))
                     runs)))))
    (catch :default _
      nil)))

(defn ^:async flush-stale-runs!
  "Scan all active sessions in Mongo for runs that have been inactive
   longer than `threshold-ms` and archive them to OpenPlanner."
  [threshold-ms]
  (when-let [store @store-registry/session-store*]
    (try
      (let [ids (vec (await (mongo-session-store/list-active-session-ids)))]
        (await (js/Promise.all
                (clj->js (map #(archive-stale-session-runs! store % threshold-ms) ids)))))
      (catch :default err
        (.warn js/console "[session-flush] flush scan failed"
               (clj->js {:error (ex-message err)}))))))

(defn start-periodic-flush!
  "Start the background flush job. Safe to call multiple times — guards
   against duplicate intervals created by shadow-cljs hot reload.

   `threshold-ms` (from config :run-stale-flush-ms) is how long a run may go
   without updates, with no active stream, before it is archived as dead. It
   should be well above any expected real turn duration so genuinely active
   long runs are never wrongly failed."
  ([] (start-periodic-flush! DEFAULT_INACTIVE_THRESHOLD_MS))
  ([threshold-ms]
   (let [threshold-ms (if (and (number? threshold-ms) (pos? threshold-ms))
                        threshold-ms
                        DEFAULT_INACTIVE_THRESHOLD_MS)]
     (when-not @interval-handle*
       (reset! interval-handle*
               (js/setInterval #(flush-stale-runs! threshold-ms) FLUSH_INTERVAL_MS))
       (.info js/console "[session-flush] periodic stale-run flush started"
              (clj->js {:interval-ms FLUSH_INTERVAL_MS
                        :threshold-ms threshold-ms}))))))

(defn stop-periodic-flush!
  "Stop the background flush job. Called on hot reload before-load."
  []
  (when-let [h @interval-handle*]
    (js/clearInterval h)
    (reset! interval-handle* nil)))
