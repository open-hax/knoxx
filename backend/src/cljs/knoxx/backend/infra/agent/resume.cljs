(ns knoxx.backend.infra.agent.resume
  "Isolated module for session resumption across backend restarts.

   Startup:
   - Scan Redis for sessions that were active when the previous process exited.
   - Recent sessions (< 10 min) are resumed in the background (non-blocking).
   - Stale sessions (>= 10 min) are aborted so the UI does not show ghost
     'active' sessions forever.

   Shutdown:
   - Wait for in-flight proxx SDK turns to finish.
   - Give Redis a grace window to persist final state.
   - If turns time out, mark their sessions resumable for the next startup."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.recovery :as agent-recovery]
            [knoxx.backend.infra.agent.session :as agent-session]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.domain.time :refer [now-iso]]
            [knoxx.backend.shape.agent :refer [streaming?]]))

;; ─── Config ───────────────────────────────────────────────────────────

(def STALE_THRESHOLD_MS (* 10 60 1000)) ; 10 minutes
(def POST_DRAIN_GRACE_MS 1000)          ; let Redis writes flush after turns complete
(def RECOVERY_INTERVAL_MS 15000)        ; periodic recovery tick
(def STARTUP_RESUME_CONCURRENCY 2)      ; keep HTTP/event loop responsive after restart
(def RECOVERY_COOLDOWN_MS 60000)        ; skip sessions touched within last 60s
                                        ; prevents resuming runs orphaned by reload!
(def PROCESS_STARTUP_RESUME_WINDOW_MS 120000)
                                        ; only treat session recovery as process-startup work
                                        ; during the first 2 minutes of the Node process.
                                        ; shadow-cljs hot reload must not create duplicate jobs.

;; ─── State ────────────────────────────────────────────────────────────

(defonce interval-handle* (atom nil))
(defonce startup-resume-state* (atom {:attempted? false
                                      :skipped-reason nil}))

;; ─── Logging helpers ──────────────────────────────────────────────────

(defn- log-info!
  ([app msg] (log-info! app msg nil))
  ([app msg err]
   (let [^js log (.-log app)]
     (if err
       (.error log msg err)
       (.info log msg)))))

(defn- log-warn!
  [app msg]
  (let [^js log (.-log app)]
    (.warn log msg)))

;; ─── Session classification ───────────────────────────────────────────

(defn- auto-resume-enabled?
  [config]
  (true? (:agent-auto-resume-sessions? config)))

(defn- session-last-updated-ms
  [session]
  (let [ts (or (:updated_at session)
               (:created_at session)
               (:shutdown_requested_at session)
               (:recovered_at session))]
    (cond
      (number? ts) ts
      (string? ts) (try (.getTime (js/Date. ts))
                        (catch :default _ 0))
      :else 0)))

(defn- session-stale?
  [session]
  (let [last-ms (session-last-updated-ms session)]
    (and (pos? last-ms)
         (>= (- (.now js/Date) last-ms) STALE_THRESHOLD_MS))))

(defn- runtime-processing-session?
  [conversation-id]
  (let [active (agent-session/active-agent-session conversation-id)
        active-streaming? (and active (streaming? active))
        active-turn? (and active
                          (try
                            (some? (knoxx.backend.shape.agent/current-turn active))
                            (catch js/Error _ false)))
        registered-turn? (some? (turn-control/active-turn conversation-id))]
    (or active-streaming? active-turn? registered-turn?)))

(defn- session-hot?
  "A session is 'hot' if it was updated very recently. Recovery skips hot
   sessions so that in-flight runs (e.g. those orphaned by event-agents/reload!)
   have time to finish naturally instead of being duplicated."
  [session]
  (let [last-ms (session-last-updated-ms session)]
    (and (pos? last-ms)
         (< (- (.now js/Date) last-ms) RECOVERY_COOLDOWN_MS))))

(defn- session-resumable?
  [session]
  (let [conversation-id (str (or (:conversation_id session) ""))]
    (and (not (session-hot? session))
         (not (runtime-processing-session? conversation-id)))))

;; ─── Actions ──────────────────────────────────────────────────────────

(defn- abort-stale-session!
  [session]
  (let [session-id (str (or (:session_id session) ""))
        conversation-id (str (or (:conversation_id session) ""))
        client (redis/get-client)]
    (if (or (str/blank? session-id) (nil? client))
      (js/Promise.resolve {:session_id session-id
                           :action "abort_skipped"
                           :reason "missing session_id or redis"})
      (-> (session-store/complete-session!
           client session-id conversation-id
           {:status "failed"
            :error "Session aborted automatically: stale (> 10 min)"
            :messages (:messages session)})
          (.then (fn [_]
                   {:session_id session-id
                    :conversation_id conversation-id
                    :action "aborted"
                    :reason "stale"}))
          (.catch (fn [err]
                    {:session_id session-id
                     :action "abort_failed"
                     :error (str err)}))))))

(defn- resume-session!
  [runtime config session]
  (let [session-id (str (or (:session_id session) ""))
        conversation-id (str (or (:conversation_id session) ""))]
    (if (session-resumable? session)
      (-> (agent-recovery/resume-recovered-session!
           runtime config session {:wait-for :kickoff})
          (.then (fn [result]
                   (when-not (:resumed result)
                     (js/console.warn "[agent-resume] session did not resume"
                                      #js {:sessionId session-id
                                           :conversationId conversation-id
                                           :reason (:reason result)}))
                   result))
          (.catch (fn [err]
                    (js/console.error "[agent-resume] resume failed"
                                      #js {:sessionId session-id
                                           :conversationId conversation-id
                                           :error (str err)})
                    {:session_id session-id
                     :conversation_id conversation-id
                     :resumed false
                     :error (str err)})))
      (js/Promise.resolve
       {:session_id session-id
        :conversation_id conversation-id
        :resumed false
        :reason "already_processing"}))))

(defn- run-limited!
  "Run promise-returning item-fn over items with bounded concurrency.
   This keeps startup recovery active without launching hundreds of agent turns at once."
  [items limit item-fn]
  (let [queue (atom (vec items))
        results (array)
        worker (fn worker []
                 (if-let [item (first @queue)]
                   (do
                     (swap! queue subvec 1)
                     (-> (item-fn item)
                         (.then (fn [result]
                                  (.push results result)
                                  (worker)))
                         (.catch (fn [err]
                                   (.push results {:error (str err)})
                                   (worker)))))
                   (js/Promise.resolve results)))
        worker-count (min (max 1 limit) (count items))]
    (if (zero? worker-count)
      (js/Promise.resolve results)
      (-> (.all js/Promise (clj->js (repeatedly worker-count worker)))
          (.then (fn [_] results))))))

;; ─── Batch processing ─────────────────────────────────────────────────

(defn- process-sessions!
  [runtime app config sessions]
  (let [{stale true recent false} (group-by session-stale? sessions)
        resumable-recent (if (auto-resume-enabled? config)
                           (filter session-resumable? recent)
                           [])]
    (when (seq stale)
      (log-warn! app (str "[agent-resume] aborting " (count stale) " stale session(s)")))
    (when (and (seq recent) (not (auto-resume-enabled? config)))
      (log-warn! app (str "[agent-resume] auto resume disabled; leaving " (count recent)
                          " recent running session(s) untouched")))
    (when (seq resumable-recent)
      (log-info! app (str "[agent-resume] resuming " (count resumable-recent) " recent session(s)")))
    (-> (.all js/Promise
              #js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))
                       (.catch (fn [err]
                                 (log-info! app "[agent-resume] abort batch error" err)
                                 nil)))
                   (-> (run-limited! resumable-recent STARTUP_RESUME_CONCURRENCY #(resume-session! runtime config %))
                       (.catch (fn [err]
                                 (log-info! app "[agent-resume] resume batch error" err)
                                 nil)))])
        (.then (fn [_]
                 {:stale (count stale)
                  :resumed (count resumable-recent)
                  :skipped (- (count recent) (count resumable-recent))})))))

;; ─── Public API: startup ──────────────────────────────────────────────

(defn- process-uptime-ms
  []
  (js/Math.round (* 1000 (.uptime js/process))))

(defn resume-on-startup!
  "Fire-and-forget scan of Redis active sessions on startup.
   Returns a promise for testability, but callers should not await it
   on the critical startup path."
  [runtime app config]
  (if-let [client (redis/get-client)]
    (-> (session-store/recover-sessions! client)
        (.then (fn [sessions]
                 (let [running (vec (filter #(= "running" (:status %)) sessions))]
                   (if (seq running)
                     (process-sessions! runtime app config running)
                     {:stale 0 :resumed 0 :skipped 0}))))
        (.then (fn [result]
                 (log-info! app (str "[agent-resume] startup scan complete: " (pr-str result)))
                 result))
        (.catch (fn [err]
                  (log-info! app "[agent-resume] startup scan failed" err)
                  {:error (str err)})))
    (do
      (log-warn! app "[agent-resume] Redis unavailable; skipping startup scan")
      (js/Promise.resolve {:skipped true :reason "redis_not_connected"}))))

(defn resume-on-process-startup!
  "Run startup recovery once per Node process, not once per shadow-cljs reload.

   Failure mode this prevents: a source edit triggers before/after-load, the HTTP
   app restarts in-process, and recovery treats that hot reload like a process
   crash, spawning duplicate recovered agent jobs. Real PM2/process restarts still
   run recovery because process uptime is near zero."
  [runtime app config]
  (let [uptime-ms (process-uptime-ms)]
    (cond
      (:attempted? @startup-resume-state*)
      (do
        (log-info! app (str "[agent-resume] startup scan skipped: already attempted in this Node process "
                            (pr-str @startup-resume-state*)))
        (js/Promise.resolve {:skipped true
                             :reason "already_attempted"}))

      (> uptime-ms PROCESS_STARTUP_RESUME_WINDOW_MS)
      (do
        (reset! startup-resume-state* {:attempted? true
                                       :skipped-reason "process_uptime_exceeded"
                                       :uptime_ms uptime-ms})
        (log-warn! app (str "[agent-resume] startup scan skipped: process uptime " uptime-ms
                            "ms indicates shadow-cljs hot reload, not process startup"))
        (js/Promise.resolve {:skipped true
                             :reason "process_uptime_exceeded"
                             :uptime_ms uptime-ms}))

      :else
      (do
        (reset! startup-resume-state* {:attempted? true
                                       :skipped-reason nil
                                       :uptime_ms uptime-ms})
        (resume-on-startup! runtime app config)))))

;; ─── Public API: periodic recovery ────────────────────────────────────

(defn- attempt-recovery!
  [runtime app config]
  (if-let [client (redis/get-client)]
    (-> (session-store/recover-sessions! client)
        (.then (fn [sessions]
                 (let [running (vec (filter #(= "running" (:status %)) sessions))
                       {stale true recent false} (group-by session-stale? running)
                       resumable (if (auto-resume-enabled? config)
                                   (filter session-resumable? recent)
                                   [])]
                   (when (seq stale)
                     (log-warn! app (str "[agent-resume] aborting " (count stale) " stale session(s)")))
                   (when (and (seq recent) (not (auto-resume-enabled? config)))
                     (log-warn! app (str "[agent-resume] auto resume disabled; periodic recovery leaving "
                                         (count recent) " recent running session(s) untouched")))
                   (when (seq resumable)
                     (log-info! app (str "[agent-resume] resuming " (count resumable) " recent session(s)")))
                   (-> (.all js/Promise
                             #js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))
                                      (.catch (fn [err]
                                                (log-info! app "[agent-resume] abort batch error" err)
                                                nil)))
                                  (-> (.all js/Promise (clj->js (mapv #(agent-recovery/resume-recovered-session!
                                                                       runtime config % {:wait-for :kickoff})
                                                                      resumable)))
                                      (.catch (fn [err]
                                                (log-info! app "[agent-resume] resume batch error" err)
                                                nil)))])
                       (.then (fn [_]
                                {:stale (count stale)
                                 :resumed (count resumable)
                                 :skipped (- (count recent) (count resumable))}))))))
        (.catch (fn [err]
                  (log-info! app "[agent-resume] recovery tick error" err)
                  (log-info! app "[agent-resume] redis:" (nil? (redis/get-client)))
                  {:error (str err)})))
    (js/Promise.resolve {:skipped true :reason "redis_not_connected"})))

(defn start-periodic-recovery!
  [runtime app config]
  (if @interval-handle*
    (log-info! app "[agent-resume] periodic recovery already running; keeping existing interval")
    (log-info! app "[agent-resume] starting periodic recovery interval"))
  (when-not @interval-handle*
    (reset! interval-handle*
            (js/setInterval
             (fn []
               (when (nil? (redis/get-client))
                 (-> (redis/init-redis! (:redis-url config))
                     (.catch (fn [_] nil))))
               (-> (attempt-recovery! runtime app config)
                   (.catch (fn [_] nil))))
             RECOVERY_INTERVAL_MS))))

(defn stop-periodic-recovery!
  []
  (when-let [id @interval-handle*]
    (js/clearInterval id)
    (reset! interval-handle* nil)))

;; ─── Public API: shutdown helpers ─────────────────────────────────────

(defn mark-sessions-resumable!
  "Called by graceful-shutdown when active turns time out."
  [client active-turns signal]
  (let [stamp (now-iso)]
    (if (empty? active-turns)
      (js/Promise.resolve 0)
      (-> (.all js/Promise
                (clj->js
                 (mapv (fn [{:keys [session_id conversation_id run_id]}]
                         (if (str/blank? (str (or session_id "")))
                           (js/Promise.resolve nil)
                           (session-store/update-session! client session_id
                                                          {:status "running"
                                                           :conversation_id conversation_id
                                                           :run_id run_id
                                                           :has_active_stream false
                                                           :shutdown_requested_at stamp
                                                           :shutdown_signal signal})))
                       active-turns)))
          (.then (fn [_] (count active-turns)))))))

(defn wait-for-turns-and-flush!
  "Wait for turn-control to drain, then give Redis a grace window to persist.
   Returns a promise."
  [app config]
  (let [grace-ms (let [v (:shutdown-grace-ms config)]
                   (if (and (number? v) (pos? v)) v 25000))
        poll-ms (let [v (:shutdown-poll-ms config)]
                  (if (and (number? v) (pos? v)) v 250))
        deadline (+ (.now js/Date) grace-ms)]
    (js/Promise.
     (fn [resolve]
       (letfn [(poll []
                 (let [remaining (turn-control/active-turn-count)]
                   (cond
                     (zero? remaining)
                     (do (log-info! app (str "[agent-resume] turns drained; waiting "
                                             POST_DRAIN_GRACE_MS "ms for Redis flush"))
                         (js/setTimeout #(resolve #js {:timed_out false :remaining 0})
                                        POST_DRAIN_GRACE_MS))

                     (>= (.now js/Date) deadline)
                     (resolve #js {:timed_out true :remaining remaining})

                     :else
                     (js/setTimeout poll poll-ms))))]
         (let [initial (turn-control/active-turn-count)]
           (when (pos? initial)
             (log-info! app (str "[agent-resume] waiting for " initial " active turn(s)")))
           (poll)))))))
