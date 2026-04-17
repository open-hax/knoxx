(ns knoxx.backend.session-recovery
  "Bootstrap and maintain Redis-backed session persistence + automatic recovery.

   Why this exists:
   - server.mjs uses registerAppRoutes() (not core/start!), so the Redis init + recovery
     in core/start! was never executed.
   - Without Redis init, sessions were never persisted, so pm2 restarts dropped active turns.
   - We also want periodic recovery so sessions can resume once dependencies become healthy."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-turns :as agent-turns]
            [knoxx.backend.agent-runtime :as agent-runtime]
            [knoxx.backend.http :as http]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]))

(defonce started?* (atom false))
(defonce interval-handle* (atom nil))
(defonce last-boot-at* (atom nil))
(defonce last-recovery-at* (atom nil))
(defonce recovery-inflight?* (atom false))

(def RECOVERY_INTERVAL_MS 15000)

(defn- proxx-configured?
  [config]
  (and (not (str/blank? (:proxx-base-url config)))
       (not (str/blank? (:proxx-auth-token config)))))

(defn- openplanner-configured?
  [config]
  (http/openplanner-enabled? config))

(defn- check-url-ok!
  [url opts]
  (-> (http/fetch-json url opts)
      (.then (fn [resp]
               (boolean (aget resp "ok"))))
      (.catch (fn [_]
                false))))

(defn- deps-healthy?
  "Return a Promise<boolean> indicating whether recovery should run.

   We gate recovery on deps to prevent marking sessions failed due to transient outages.
   If a dependency is not configured, we treat it as unhealthy (recovery is meaningless)."
  [config]
  (let [proxx? (proxx-configured? config)
        openplanner? (openplanner-configured? config)]
    (if-not (and proxx? openplanner?)
      (js/Promise.resolve false)
      (-> (.all js/Promise
                #js [(check-url-ok! (str (:proxx-base-url config) "/health")
                                   #js {:headers (http/bearer-headers (:proxx-auth-token config))})
                     (check-url-ok! (http/openplanner-url config "/v1/health")
                                   #js {:headers (http/openplanner-headers config)})])
          (.then (fn [parts]
                   (let [proxx-ok (boolean (aget parts 0))
                         openplanner-ok (boolean (aget parts 1))]
                     (and proxx-ok openplanner-ok))))))))

(defn- attempt-recovery!
  [runtime app config]
  (cond
    @recovery-inflight?*
    (js/Promise.resolve #js {:ok true :skipped true :reason "recovery_inflight"})

    (nil? (redis/get-client))
    (js/Promise.resolve #js {:ok false :skipped true :reason "redis_not_connected"})

    :else
    (do
      (reset! recovery-inflight?* true)
      (-> (deps-healthy? config)
          (.then (fn [healthy?]
                   (if-not healthy?
                     #js {:ok false :skipped true :reason "deps_unhealthy"}
                     (let [recovery
                           (-> (session-store/recover-sessions! (redis/get-client))
                               (.then (fn [sessions]
                                        (let [resumable (->> (vec sessions)
                                                             (filter (fn [session]
                                                                       (let [conversation-id (str (or (:conversation_id session) ""))
                                                                             active (agent-runtime/active-agent-session conversation-id)]
                                                                         (not (and active (true? (aget active "isStreaming")))))))
                                                             vec)]
                                          (if (seq resumable)
                                            (-> (.all js/Promise
                                                      (clj->js (mapv #(agent-turns/resume-recovered-session! runtime config %) resumable)))
                                                (.then (fn [results]
                                                         (let [items (vec (js->clj results :keywordize-keys true))
                                                               resumed (count (filter :resumed items))]
                                                           (reset! last-recovery-at* (.toISOString (js/Date.)))
                                                           (.log.info app (str "[knoxx] session recovery tick: found " (count resumable) ", resumed " resumed))
                                                           #js {:ok true
                                                                :found (count resumable)
                                                                :resumed resumed}))))
                                            #js {:ok true :found 0 :resumed 0}))))]
                       (.catch recovery
                               (fn [err]
                                 (.log.error app "[knoxx] session recovery tick failed" err)
                                 #js {:ok false :error (str err)}))))))
          (.finally (fn []
                      (reset! recovery-inflight?* false)
                      nil))))))

(defn- ensure-redis!
  [app config]
  (let [redis-url (:redis-url config)]
    (-> (redis/init-redis! redis-url)
        (.then (fn [client]
                 (if client
                   (do
                     (.log.info app "[knoxx] Redis connected; session persistence enabled")
                     client)
                   (do
                     (when-not (str/blank? (str redis-url))
                       (.log.warn app "[knoxx] Redis not connected; session persistence disabled"))
                     nil)))))))

(defn start!
  "Idempotent bootstrap: connect Redis, then periodically attempt to recover running sessions.

   Returns a Promise that resolves once the first Redis init attempt completes and a first
   recovery tick is scheduled (it may be skipped if deps are unhealthy)."
  [runtime app config]
  (if @started?*
    (js/Promise.resolve #js {:ok true :started true :already true})
    (do
      (reset! started?* true)
      (reset! last-boot-at* (.toISOString (js/Date.)))
      (-> (ensure-redis! app config)
          (.then (fn [_]
                   ;; Fire one tick immediately, then continue on interval.
                   (attempt-recovery! runtime app config)))
          (.then (fn [_]
                   (when-not @interval-handle*
                     (reset! interval-handle*
                             (js/setInterval
                              (fn []
                                ;; Keep trying to connect to Redis until it succeeds.
                                (when (nil? (redis/get-client))
                                  (-> (ensure-redis! app config) (.catch (fn [_] nil))))
                                (-> (attempt-recovery! runtime app config)
                                    (.catch (fn [_] nil))))
                              RECOVERY_INTERVAL_MS)))
                   #js {:ok true :started true :interval_ms RECOVERY_INTERVAL_MS}))))))
