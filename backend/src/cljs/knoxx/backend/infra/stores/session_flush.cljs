(ns knoxx.backend.infra.stores.session-flush
  "Periodic TTL-based flush of stale Redis runs to OpenPlanner.

   Purpose: safety net for runs that were never explicitly completed
   (e.g. process crash mid-stream, network timeout).  These runs stay
   'running' in Redis until their TTL expires and are then lost.  This
   job detects them early and archives them as 'failed' before that
   happens."
  (:require [knoxx.backend.infra.stores.session-store-registry :as store-registry]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.shape.session-persistence :refer [put-run! list-active-runs]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.time :refer [now-iso]]))

(def ^:private INACTIVE_THRESHOLD_MS (* 30 60 1000)) ; 30 minutes without update
(def ^:private FLUSH_INTERVAL_MS     (* 20 60 1000)) ; scan every 20 minutes

(defonce ^:private interval-handle* (atom nil))

(defn- run-inactive?
  [run]
  (let [updated-ms (try (.getTime (js/Date. (or (:updated_at run) "")))
                        (catch :default _ 0))]
    (and (not (:has_active_stream run))
         (pos? updated-ms)
         (>= (- (.now js/Date) updated-ms) INACTIVE_THRESHOLD_MS))))

(defn- archive-stale-run!
  [store run]
  (let [archived (assoc run
                        :status "failed"
                        :error "session-ttl-expired"
                        :has_active_stream false
                        :updated_at (now-iso))]
    (-> (put-run! store archived)
        (.catch (fn [err]
                  (.warn js/console "[session-flush] stale run archive failed"
                         (clj->js {:run-id (:run_id run)
                                   :session-id (:session_id run)
                                   :error (ex-message err)})))))))

(defn flush-stale-runs!
  "Scan all active sessions in Redis for runs that have been inactive
   longer than INACTIVE_THRESHOLD_MS and archive them to OpenPlanner."
  [client]
  (when-let [store @store-registry/session-store*]
    (-> (redis/smembers client session-store/ACTIVE_SESSIONS_SET)
        (.then (fn [session-ids]
                 (let [ids (vec (js->clj session-ids))]
                   (js/Promise.all
                    (clj->js
                     (map (fn [session-id]
                            (-> (list-active-runs store session-id)
                                (.then (fn [runs]
                                         (js/Promise.all
                                          (clj->js
                                           (keep (fn [run]
                                                   (when (run-inactive? run)
                                                     (archive-stale-run! store run)))
                                                 runs)))))
                                (.catch (fn [_] nil))))
                          ids))))))
        (.catch (fn [err]
                  (.warn js/console "[session-flush] flush scan failed"
                         (clj->js {:error (ex-message err)})))))))

(defn start-periodic-flush!
  "Start the background flush job. Safe to call multiple times — guards
   against duplicate intervals created by shadow-cljs hot reload."
  [client]
  (when-not @interval-handle*
    (reset! interval-handle*
            (js/setInterval #(flush-stale-runs! client) FLUSH_INTERVAL_MS))
    (.info js/console "[session-flush] periodic stale-run flush started"
           (clj->js {:interval-ms FLUSH_INTERVAL_MS
                     :threshold-ms INACTIVE_THRESHOLD_MS}))))

(defn stop-periodic-flush!
  "Stop the background flush job. Called on hot reload before-load."
  []
  (when-let [h @interval-handle*]
    (js/clearInterval h)
    (reset! interval-handle* nil)))
