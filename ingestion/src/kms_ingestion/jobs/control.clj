(ns kms-ingestion.jobs.control
  "Execution, pacing, and backpressure helpers for ingestion jobs."
  (:require
   [kms-ingestion.config :as config])
  (:import
   [java.lang.management ManagementFactory]
   [java.util.concurrent ExecutorService Executors]))

(defonce ^:private executor (atom nil))
(defonce ^:private cpu-stats (atom {:usage 0 :periods 0 :timestamp 0}))
(defonce ^:private control-state (atom {:ema-cores 0.0}))
(defonce ^:private openplanner-backpressure* (atom {:until-ms 0 :streak 0 :reason nil}))

(defn executor-ready?
  []
  (some? @executor))

(defn init-executor!
  "Initialize the job executor thread pool."
  []
  (reset! executor (Executors/newFixedThreadPool 4))
  (println "Job executor initialized with 4 threads"))

(defn submit-task!
  "Submit a task to the executor."
  [f]
  (when @executor
    (.submit ^ExecutorService @executor f)))

(defn bounded-future-mapv
  "Run at most `parallelism` items at once and preserve input order."
  [parallelism f coll]
  (let [limit (max 1 (int parallelism))]
    (loop [remaining (seq coll)
           out []]
      (if-not remaining
        out
        (let [chunk (vec (take limit remaining))
              futures (mapv (fn [item]
                              (future
                                (f item)))
                            chunk)
              results (mapv deref futures)]
          (recur (seq (drop limit remaining))
                 (into out results)))))))

(defn log!
  [& xs]
  (apply println xs)
  (flush))

(defn- read-cgroup-cpu-stats
  "Read cgroup CPU stats. Returns {:usage_usec :nr_periods :nr_throttled} or nil."
  []
  (try
    (let [cpu-stat (slurp "/sys/fs/cgroup/cpu.stat")
          usage (when-let [m (re-find #"usage_usec (\d+)" cpu-stat)]
                  (Long/parseLong (second m)))
          periods (when-let [m (re-find #"nr_periods (\d+)" cpu-stat)]
                    (Long/parseLong (second m)))
          throttled (when-let [m (re-find #"nr_throttled (\d+)" cpu-stat)]
                      (Long/parseLong (second m)))]
      (when (and usage periods)
        {:usage_usec usage :nr_periods periods :nr_throttled throttled}))
    (catch Exception _ nil)))

(defn container-cpu-cores
  "Calculate container CPU usage in cores using cgroup stats.
   Returns raw core count (e.g. 3.75 means 375% of 1 core).
   Falls back to host load average if cgroup stats unavailable."
  []
  (if-let [{:keys [usage_usec]} (read-cgroup-cpu-stats)]
    (let [{:keys [usage timestamp]} @cpu-stats
          now (System/currentTimeMillis)
          delta-us (- usage_usec usage)
          delta-ms (- now timestamp)]
      (swap! cpu-stats assoc :usage usage_usec :timestamp now)
      (if (and (pos? delta-us) (pos? delta-ms) (number? timestamp) (pos? timestamp))
        (/ delta-us (* delta-ms 1000.0))
        (do
          (swap! cpu-stats assoc :usage usage_usec :timestamp now)
          0.0)))
    (try
      (let [bean (ManagementFactory/getOperatingSystemMXBean)
            load (.getSystemLoadAverage bean)]
        (when (and (number? load) (not (neg? load)))
          load))
      (catch Exception _ nil))))

(defn- host-core-count
  "Read total physical core count from /proc/cpuinfo.
   Falls back to Runtime/availableProcessors."
  []
  (try
    (let [lines (line-seq (java.io.BufferedReader. (java.io.FileReader. "/proc/cpuinfo")))
          procs (count (filter #(.startsWith ^String % "processor") lines))]
      (if (pos? procs) procs (.availableProcessors (Runtime/getRuntime))))
    (catch Exception _ (.availableProcessors (Runtime/getRuntime)))))

(defonce ^:private host-cores (delay (host-core-count)))

(defn available-cores
  []
  @host-cores)

(defn- smoothed-cpu-cores
  []
  (let [current (or (container-cpu-cores) 0.0)
        alpha 0.25
        previous (:ema-cores @control-state)
        ema (+ (* alpha current) (* (- 1.0 alpha) previous))]
    (swap! control-state assoc :ema-cores ema)
    ema))

(defn control-delay-ms
  "Smoothed pacing controller.
   Keeps a small baseline spacing to avoid burstiness even when under target,
   then ramps up delay steeply as CPU approaches/exceeds the target.
   Target = max-load-per-core * host cores."
  [cpu-cores]
  (let [cores (available-cores)
        target (* (config/ingest-max-load-per-core) cores)
        ratio (if (pos? target) (/ cpu-cores target) 0)]
    (cond
      (< ratio 0.25) 8
      (< ratio 0.50) 15
      (< ratio 0.70) 30
      (< ratio 0.85) 60
      (< ratio 1.00) 120
      (< ratio 1.20) 250
      (< ratio 1.50) 500
      (< ratio 2.00) 1000
      :else 2000)))

(defn maybe-throttle!
  [job-id]
  (when (config/ingest-throttle-enabled?)
    (let [cpu-cores (smoothed-cpu-cores)
          target-cores (* (config/ingest-max-load-per-core) (available-cores))
          delay (control-delay-ms cpu-cores)]
      (when (and cpu-cores (>= cpu-cores (* 0.7 target-cores)))
        (log! (str "[JOB " job-id "] Throttling: cpu="
                   (format "%.1f" cpu-cores) " cores"
                   " > "
                   (format "%.1f" target-cores) " cores"
                   ", delay=" delay "ms")))
      (when (pos? delay)
        (Thread/sleep (long delay))))))

(defn note-openplanner-success!
  []
  (swap! openplanner-backpressure* assoc :until-ms 0 :streak 0 :reason nil))

(defn note-openplanner-failure!
  [job-id reason]
  (let [state (swap! openplanner-backpressure*
                     (fn [{:keys [streak] :as current}]
                       (let [next-streak (inc (int (or streak 0)))
                             delay-ms (min 60000 (* 1000 (bit-shift-left 1 (min 5 (dec next-streak)))))
                             until-ms (+ (System/currentTimeMillis) delay-ms)]
                         (assoc current :streak next-streak :until-ms until-ms :reason reason))))
        wait-ms (max 0 (- (:until-ms state) (System/currentTimeMillis)))]
    (log! (str "[JOB " job-id "] OpenPlanner backpressure engaged: wait=" wait-ms "ms, streak=" (:streak state) ", reason=" reason))))

(defn respect-openplanner-backpressure!
  [job-id]
  (let [{:keys [until-ms reason]} @openplanner-backpressure*
        now (System/currentTimeMillis)]
    (when (> (long (or until-ms 0)) now)
      (let [wait-ms (- (long until-ms) now)]
        (log! (str "[JOB " job-id "] Waiting for OpenPlanner recovery: " wait-ms "ms" (when reason (str " (" reason ")"))))
        (Thread/sleep wait-ms)))))
