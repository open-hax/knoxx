(ns knoxx.backend.domain.event.cron
  "Generic cron/ticker helpers for the events runtime.

   These helpers are parameterized over state atoms and callback functions so
   the legacy event-agents namespace can delegate to them while internals are
   extracted incrementally.")

(defn- env-positive-int
  [k default]
  (let [raw (aget js/process.env k)
        parsed (js/parseInt raw 10)]
    (if (and (js/Number.isFinite parsed) (pos? parsed))
      parsed
      default)))

(def ^:private default-cron-ticker-ms
  (env-positive-int "KNOXX_EVENTS_CRON_TICKER_MS" 60000))

(def ^:private startup-min-delay-ms
  (env-positive-int "KNOXX_EVENTS_CRON_STARTUP_MIN_DELAY_MS" 120000))

(def ^:private startup-spread-ms
  (env-positive-int "KNOXX_EVENTS_CRON_STARTUP_SPREAD_MS" 600000))

(defn- startup-jitter-ms
  [job-id]
  (if (pos? startup-spread-ms)
    (mod (js/Math.abs (hash (str job-id))) startup-spread-ms)
    0))

(defn- startup-delayed-run-at
  [now job-id]
  (+ now startup-min-delay-ms (startup-jitter-ms job-id)))

(defn cadence-label
  [minutes]
  (cond
    (= minutes 1) "Every minute"
    (< minutes 60) (str "Every " minutes " minutes")
    (= (mod minutes 60) 0) (str "Every " (/ minutes 60) " hours")
    :else (str "Every " minutes " minutes")))

(defn cron-job?
  [job]
  (and (:enabled job)
       (= "cron" (get-in job [:trigger :kind]))))

(defn- job-cadence-ms
  [job]
  (* 60 1000 (max 1 (or (get-in job [:trigger :cadenceMinutes]) 1))))

(defn- initialize-cron-job-state!
  [{:keys [update-job-state! normalize-job-state]} job]
  (let [job-id (:id job)
        cadence-ms (job-cadence-ms job)
        now (.now js/Date)]
    (update-job-state! job-id
                       (fn [state]
                         (let [state (normalize-job-state job-id state)
                               running? (boolean (:running state))
                               last-finished (:lastFinishedAt state)
                               candidate-next-run (or (:nextRunAt state)
                                                      (when last-finished (+ last-finished cadence-ms))
                                                      now)
                               first-init? (nil? (:cronInitializedAt state))
                               next-run (if (and first-init?
                                                 (pos? startup-min-delay-ms)
                                                 (<= candidate-next-run now))
                                          (startup-delayed-run-at now job-id)
                                          candidate-next-run)]
                           (assoc state
                                  :id job-id
                                  :name (:name job)
                                  :enabled (:enabled job)
                                  :running running?
                                  :cronInitializedAt (or (:cronInitializedAt state) now)
                                  :nextRunAt next-run))))))

(defn- due-cron-job?
  [{:keys [job-state* normalize-job-state]} now job]
  (let [job-id (:id job)
        state (normalize-job-state job-id (get @job-state* job-id))]
    (and (cron-job? job)
         (not (:running state))
         (<= (or (:nextRunAt state) 0) now))))

(defn trigger-due-cron-jobs!
  [{:keys [job-state* running?* control-config-fn run-job! update-job-state! normalize-job-state]} config]
  (when @running?*
    (let [control (control-config-fn config)
          now (.now js/Date)
          cron-jobs (filter cron-job? (:jobs control))]
      (doseq [job cron-jobs]
        (initialize-cron-job-state! {:update-job-state! update-job-state!
                                     :normalize-job-state normalize-job-state}
                                    job))
      (doseq [job cron-jobs]
        (when (due-cron-job? {:job-state* job-state*
                              :normalize-job-state normalize-job-state}
                             now job)
          (-> (run-job! (:id job))
              (.catch (fn [err]
                        (println "[events.cron] cron ticker job failed for" (:id job) ":" (.-message err))))))))))

(defn schedule-cron-ticker!
  [{:keys [scheduled-tasks* job-state* running?* control-config-fn run-job! update-job-state! normalize-job-state]
    :or {scheduled-tasks* (atom {})
         job-state* (atom {})}}
   config]
  (when-not (contains? @scheduled-tasks* :cron-ticker)
    (let [tick-env {:job-state* job-state*
                    :running?* running?*
                    :control-config-fn control-config-fn
                    :run-job! run-job!
                    :update-job-state! update-job-state!
                    :normalize-job-state normalize-job-state}
          tick! (fn [] (trigger-due-cron-jobs! tick-env config))
          id (doto (js/setInterval tick! default-cron-ticker-ms) (.unref))]
      (swap! scheduled-tasks* assoc :cron-ticker {:type :interval
                                                  :id id
                                                  :everyMs default-cron-ticker-ms})
      (println "[events.cron] scheduled single cron ticker every" default-cron-ticker-ms "ms")
      (tick!))))
