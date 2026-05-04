(ns kms-ingestion.server
  "Main entry point for the KMS Ingestion service."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]
   [kms-ingestion.api.routes :as routes]
   [kms-ingestion.contracts.loader :as contracts]
   [kms-ingestion.contracts.resolve :as cr]
   [kms-ingestion.db :as db]
   [kms-ingestion.drivers.local :as local]
   [kms-ingestion.jobs.worker :as worker]
   [kms-ingestion.translation.worker :as translation-worker]
   [kms-ingestion.config :as config])
  (:import
   [java.nio.file FileSystems Path StandardWatchEventKinds WatchEvent$Kind WatchKey WatchService]
   [java.util.concurrent TimeUnit])
  (:gen-class))

(defonce scheduler-thread (atom nil))
(defonce watcher-thread (atom nil))
(defonce watcher-state (atom {}))
(defonce watch-service (atom nil))
(defonce watch-keys (atom {}))
(defonce watched-sources (atom {}))

(defn- parse-jsonish
  [value]
  (cond
    (nil? value) nil
    (map? value) value
    (string? value) (when-not (str/blank? value) (json/parse-string value keyword))
    (instance? org.postgresql.util.PGobject value)
    (let [s (.getValue ^org.postgresql.util.PGobject value)]
      (when-not (str/blank? s)
        (json/parse-string s keyword)))
    :else nil))

(defn- source-sync-interval-minutes
  [source]
  (let [cfg (or (parse-jsonish (:config source)) {})
        raw (or (:sync_interval_minutes cfg) (:sync-interval-minutes cfg))]
    (when (and (number? raw) (pos? raw))
      (int raw))))

(defn- source-due?
  [source]
  (when-let [minutes (source-sync-interval-minutes source)]
    (let [last-scan (:last_scan_at source)
          last-ts (if last-scan (.toInstant ^java.sql.Timestamp last-scan) java.time.Instant/EPOCH)
          next-ts (.plus last-ts (java.time.Duration/ofMinutes minutes))]
      (.isAfter (java.time.Instant/now) next-ts))))

(defn- source-watch-enabled?
  [source]
  (let [cfg (or (parse-jsonish (:config source)) {})
        raw (or (:passive_watch cfg) (:passive-watch cfg))]
    (if (nil? raw)
      (config/passive-watch-enabled?)
      (boolean raw))))

(defn- source-scan-opts
  [source]
  {:file-types (or (parse-jsonish (:file_types source)) (:file_types source) (:file-types source))
   :include-patterns (or (parse-jsonish (:include_patterns source)) (:include_patterns source) (:include-patterns source))
   :exclude-patterns (or (parse-jsonish (:exclude_patterns source)) (:exclude_patterns source) (:exclude-patterns source))})

(defn- source-root-path
  [source]
  (let [cfg (or (parse-jsonish (:config source)) {})]
    (or (:root_path cfg) (:root-path cfg))))

(defn- diff-snapshot
  [prev current]
  (let [prev-keys (set (keys prev))
        current-keys (set (keys current))
        added (clojure.set/difference current-keys prev-keys)
        removed (clojure.set/difference prev-keys current-keys)
        maybe-modified (clojure.set/intersection prev-keys current-keys)
        modified (filter (fn [path]
                           (let [before (get prev path)
                                 after (get current path)]
                             (or (not= (:size before) (:size after))
                                 (not= (some-> (:modified-at before) str)
                                       (some-> (:modified-at after) str)))))
                         maybe-modified)]
    {:changed (vec (sort (concat added modified)))
     :deleted (vec (sort removed))}))

(defn- merge-watch-state
  [current changed deleted now-ms snapshot]
  {:snapshot snapshot
   :pending-paths (into (or (:pending-paths current) #{}) changed)
   :pending-deleted (into (or (:pending-deleted current) #{}) deleted)
   :last-event-ms (if (or (seq changed) (seq deleted)) now-ms (:last-event-ms current))})

(defn- queue-ready-watch-jobs!
  []
  (let [now-ms (System/currentTimeMillis)
        debounce-ms (config/passive-watch-debounce-ms)]
    (doseq [source (db/list-enabled-sources)]
      (let [source-id (str (:source_id source))
            {:keys [pending-paths pending-deleted last-event-ms]} (get @watcher-state source-id)
            ready? (and last-event-ms (>= (- now-ms last-event-ms) debounce-ms))]
        (when (and ready?
                   (or (seq pending-paths) (seq pending-deleted))
                   (not (db/source-has-active-job? source-id)))
          (let [job (db/create-job! source-id (:tenant_id source)
                                    {:watch true
                                     :watch_paths (vec (sort pending-paths))
                                     :deleted_paths (vec (sort pending-deleted))})
                job-id (str (:job_id job))]
            (println (str "[watcher] queueing source=" source-id
                          " changed=" (count pending-paths)
                          " deleted=" (count pending-deleted)
                          " job=" job-id))
            (worker/queue-job! job-id source)
            (swap! watcher-state assoc source-id {:pending-paths #{}
                                                  :pending-deleted #{}
                                                  :last-event-ms nil})))))))

(defn- watch-root-path
  [source]
  (when-let [root (source-root-path source)]
    (.toPath (.getAbsoluteFile (io/file root)))))

(defn- valid-watch-dir?
  [^java.io.File dir]
  (and (.exists dir)
       (.isDirectory dir)
       (not (local/skip-directory-name? (.getName dir)))))

(defn- register-watch-dir!
  [^WatchService ws source-id ^Path root-path ^Path dir-path]
  (let [key (.register dir-path
                       ws
                       (into-array WatchEvent$Kind
                                   [StandardWatchEventKinds/ENTRY_CREATE
                                    StandardWatchEventKinds/ENTRY_DELETE
                                    StandardWatchEventKinds/ENTRY_MODIFY]))]
    (swap! watch-keys assoc key {:source-id source-id
                                 :root-path root-path
                                 :dir-path dir-path})))

(defn- register-watch-tree!
  [^WatchService ws source-id ^Path source-root ^Path start-path]
  (doseq [file (file-seq (.toFile start-path))
          :when (valid-watch-dir? file)]
    (register-watch-dir! ws source-id source-root (.toPath ^java.io.File file))))

(defn- sync-watch-registrations!
  [^WatchService ws]
  (let [active-sources (filter (fn [source]
                                 (and (= "local" (:driver_type source))
                                      (source-watch-enabled? source)
                                      (source-root-path source)))
                               (db/list-enabled-sources))
        source-map (into {} (map (fn [source] [(str (:source_id source)) source]) active-sources))
        source-ids (set (keys source-map))
        existing-ids (set (keys @watched-sources))]
    (doseq [removed-id (set/difference existing-ids source-ids)]
      (doseq [[key ctx] @watch-keys
              :when (= removed-id (:source-id ctx))]
        (.cancel ^WatchKey key)
        (swap! watch-keys dissoc key))
      (swap! watched-sources dissoc removed-id)
      (swap! watcher-state dissoc removed-id))
    (doseq [[source-id source] source-map]
      (when-not (contains? @watched-sources source-id)
        (let [root-path (watch-root-path source)]
          (println (str "[watcher] registering source=" source-id " root=" root-path))
          (register-watch-tree! ws source-id root-path root-path)
          (swap! watched-sources assoc source-id {:root-path root-path}))))))

(defn- enqueue-watch-event!
  [source-id rel-path deleted?]
  (let [now-ms (System/currentTimeMillis)]
    (swap! watcher-state update source-id
           (fn [current]
             {:pending-paths (if deleted? (or (:pending-paths current) #{}) (conj (or (:pending-paths current) #{}) rel-path))
              :pending-deleted (if deleted? (conj (or (:pending-deleted current) #{}) rel-path) (or (:pending-deleted current) #{}))
              :last-event-ms now-ms}))))

(defn- handle-watch-key!
  [^WatchService ws ^WatchKey key]
  (when-let [{:keys [source-id root-path dir-path]} (get @watch-keys key)]
    (doseq [event (.pollEvents key)]
      (let [kind (.kind event)]
        (when-not (= kind StandardWatchEventKinds/OVERFLOW)
          (let [context (.context event)
                child-path (.resolve ^Path dir-path ^Path context)
                file (.toFile child-path)
                rel-path (str (.relativize ^Path root-path ^Path child-path))
                deleted? (= kind StandardWatchEventKinds/ENTRY_DELETE)]
            (when (and (not (str/blank? rel-path))
                       (not (local/skip-directory-name? (.getName file))))
              (when (and (= kind StandardWatchEventKinds/ENTRY_CREATE) (.isDirectory file) (valid-watch-dir? file))
                (register-watch-tree! ws source-id root-path child-path))
              (enqueue-watch-event! source-id rel-path deleted?))))))
    (when-not (.reset key)
      (swap! watch-keys dissoc key))))

(defn- maybe-queue-watched-jobs!
  []
  (let [now-ms (System/currentTimeMillis)
        debounce-ms (config/passive-watch-debounce-ms)
        active-sources (filter (fn [source]
                                 (and (= "local" (:driver_type source))
                                      (source-watch-enabled? source)
                                      (source-root-path source)))
                               (db/list-enabled-sources))
        source-ids (set (map #(str (:source_id %)) active-sources))]
    (swap! watcher-state #(select-keys % source-ids))
    (doseq [source active-sources]
      (let [source-id (str (:source_id source))
            snapshot (local/snapshot-files (source-root-path source) (source-scan-opts source))
            previous (get @watcher-state source-id)
            diff (if previous (diff-snapshot (:snapshot previous) snapshot) {:changed [] :deleted []})]
        (swap! watcher-state update source-id merge-watch-state (:changed diff) (:deleted diff) now-ms snapshot)
        (let [{:keys [pending-paths pending-deleted last-event-ms]} (get @watcher-state source-id)
              ready? (and last-event-ms (>= (- now-ms last-event-ms) debounce-ms))]
          (when (and ready?
                     (or (seq pending-paths) (seq pending-deleted))
                     (not (db/source-has-active-job? source-id)))
            (let [job (db/create-job! source-id (:tenant_id source)
                                      {:watch true
                                       :watch_paths (vec (sort pending-paths))
                                       :deleted_paths (vec (sort pending-deleted))})
                  job-id (str (:job_id job))]
              (println (str "[watcher] queueing source=" source-id
                            " changed=" (count pending-paths)
                            " deleted=" (count pending-deleted)
                            " job=" job-id))
              (worker/queue-job! job-id source)
              (swap! watcher-state assoc source-id {:snapshot snapshot
                                                    :pending-paths #{}
                                                    :pending-deleted #{}
                                                    :last-event-ms nil}))))))))

(defn- maybe-queue-scheduled-jobs!
  []
  (doseq [source (db/list-enabled-sources)]
    (let [source-id (str (:source_id source))]
      (when (and (source-due? source)
                 (not (db/source-has-active-job? source-id)))
        (let [job (db/create-job! source-id (:tenant_id source) {:scheduled true})
              job-id (str (:job_id job))]
          (db/mark-source-scanned! source-id)
          (println (str "[scheduler] queueing source=" source-id " job=" job-id))
          (worker/queue-job! job-id source))))))

(defn- queue-initial-jobs!
  []
  (doseq [source (db/list-enabled-sources)]
    (let [source-id (str (:source_id source))
          latest-job (db/latest-job-for-source source-id)
          needs-bootstrap? (or (not (db/source-has-file-state? source-id))
                               (= "failed" (:status latest-job))
                               (= "cancelled" (:status latest-job)))]
      (when (and needs-bootstrap?
                  (not (db/source-has-active-job? source-id)))
        (let [job (db/create-job! source-id (:tenant_id source)
                                  {:bootstrap true
                                   :retry_of (some-> latest-job :job_id str)})
              job-id (str (:job_id job))]
          (db/mark-source-scanned! source-id)
          (println (str "[bootstrap] queueing initial crawl source=" source-id " job=" job-id))
          (worker/queue-job! job-id source))))))

(defn- start-scheduler!
  []
  (when-not @scheduler-thread
    (reset! scheduler-thread
            (future
              (while true
                (try
                  (maybe-queue-scheduled-jobs!)
                  (catch Exception e
                    (println "[scheduler] error:" (.getMessage e))))
                (Thread/sleep (long (config/ingest-scheduler-poll-ms))))))))

(defn- ensure-source-from-contract!
  "Create or verify a database source from a loaded contract map.
  Uses the contract to derive tenant, driver type, name, config, collections,
  file-types, and exclude-patterns — no hardcoded values.
  If the source already exists, updates its config from the contract."
  [tenant-id source-id-str contract]
  (let [tenant-name (or (get contract :tenant/id) tenant-id)]
    (db/ensure-tenant! tenant-id tenant-name)
    (let [existing-sources (db/list-sources tenant-id)
          driver-type      (name (or (cr/source-driver contract) :local))
          source-name-str  (or (cr/source-name contract) source-id-str)
          existing         (first (filter #(= source-name-str (:name %)) existing-sources))
          cfg              (cr/source-config contract)
          root-p           (cr/root-path contract)
          file-types       (or (seq (cr/file-types contract)) [])
          exclude-pats     (or (seq (cr/exclude-patterns contract)) [])
          include-pats     (or (seq (cr/include-patterns contract)) [])
          sync-minutes     (cr/sync-interval-minutes contract)
          passive-w        (cr/passive-watch-enabled? contract)
          collections      (or (cr/sink-collections contract) [tenant-id])
          ;; Store contract source ID in config so the worker can map UUID -> contract file
          source-cfg       (cond-> (dissoc cfg :root_path)
                             root-p       (assoc :root_path root-p)
                             sync-minutes (assoc :sync_interval_minutes sync-minutes)
                             (some? passive-w) (assoc :passive_watch passive-w)
                             true         (assoc :contract_source_id source-id-str))
          contract-fields  {:config           source-cfg
                            :collections      collections
                            :file-types       (vec file-types)
                            :exclude-patterns (vec exclude-pats)
                            :include-patterns (vec include-pats)}]
      (if existing
        ;; Source exists — check if contract config has changed
        (let [existing-cfg (or (parse-jsonish (:config existing)) {})]
          (when (or (not= (:root_path existing-cfg) root-p)
                    (not= (vec (or (parse-jsonish (:file_types existing)) []))
                          (vec file-types))
                    (not= (set (or (parse-jsonish (:exclude_patterns existing)) #{}))
                          (set exclude-pats))
                    (not= (:contract_source_id existing-cfg) source-id-str))
            (println (str "[bootstrap] syncing source from contract: "
                          tenant-id "/" source-id-str
                          (when (not= (:root_path existing-cfg) root-p)
                            (str " root_path: " (:root_path existing-cfg) " -> " root-p))))
            (db/update-source! (:source_id existing) contract-fields)))
        ;; Source does not exist — create it
        (do
          (println (str "[bootstrap] creating source from contract: "
                        tenant-id "/" source-id-str " (driver=" driver-type ")"))
          (db/create-source!
           (assoc contract-fields
                  :tenant-id   tenant-id
                  :driver-type driver-type
                  :name        source-name-str)))))))

(defn- ensure-sources-from-contracts!
  "Scan the contracts directory and ensure all discovered sources exist in the DB.
  This replaces the old hardcoded ensure-default-workspace-source! and
  ensure-eta-mu-sessions-source! functions."
  []
  (let [root (contracts/contracts-root)]
    (if root
      (let [root-file (io/file root)]
        (println (str "[bootstrap] scanning contracts dir: " root))
        (doseq [^java.io.File tenant-dir (.listFiles root-file)
                :when (.isDirectory tenant-dir)
                :let [tenant-id (.getName tenant-dir)
                      sources-dir (io/file tenant-dir "sources")]
                :when (.isDirectory sources-dir)]
          (doseq [^java.io.File edn-file (.listFiles sources-dir)
                  :when (str/ends-with? (.getName edn-file) ".edn")
                  :let [source-id-str (str/replace (.getName edn-file) #"\.edn$" "")
                        contract (contracts/load-source-contract tenant-id source-id-str)]]
            (when contract
              (ensure-source-from-contract! tenant-id source-id-str contract)))))
      (do
        (println "[bootstrap] No contracts directory found — skipping contract-driven source creation")
        (println "[bootstrap] Set CONTRACTS_DIR env var or place contracts/ next to the ingestion package")))))

(defn- start-watcher!
  []
  (when (and (config/passive-watch-enabled?) (not @watcher-thread))
    (try
      (let [ws (.newWatchService (FileSystems/getDefault))]
        (reset! watch-service ws)
        (println "[watcher] WatchService active")
        (reset! watcher-thread
                (future
                  (let [resync-ms (config/passive-watch-poll-ms)]
                    (loop [last-sync 0]
                      (let [now-ms (System/currentTimeMillis)]
                        (when (>= (- now-ms last-sync) resync-ms)
                          (sync-watch-registrations! ws))
                        (when-let [key (.poll ws 1000 TimeUnit/MILLISECONDS)]
                          (handle-watch-key! ws key))
                        (queue-ready-watch-jobs!)
                        (recur (if (>= (- now-ms last-sync) resync-ms) now-ms last-sync)))))))
        (println "[watcher] WatchService active"))
      (catch Exception e
        (println "[watcher] WatchService unavailable, falling back to polling:" (.getMessage e))
        (reset! watcher-thread
                (future
                  (while true
                    (try
                      (maybe-queue-watched-jobs!)
                      (catch Exception inner
                        (println "[watcher] fallback error:" (.getMessage inner))))
                    (Thread/sleep (long (config/passive-watch-poll-ms))))))))))

(def app
  (ring/ring-handler
   (ring/router
    routes/routes
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})
   (ring/create-default-handler)))

(defn wrap-logging
  [handler]
  (fn [request]
    (let [start (System/nanoTime)
          response (handler request)
          elapsed (/ (- (System/nanoTime) start) 1e6)]
      (printf "[%s] %s %s -> %d (%.1fms)%n"
              (java.time.LocalDateTime/now)
              (:request-method request)
              (:uri request)
              (:status response)
              elapsed)
      (flush)
      response)))

(def wrapped-app
  (-> app
      wrap-logging
      wrap-params
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :patch :options]
                 :access-control-allow-headers ["Content-Type" "Authorization"])))

(defn -main
  [& _args]
  (println "Starting KMS Ingestion service...")
  (println (str "Config: " (config/config)))
  
  ;; Initialize database
  (db/init!)
  (db/reset-orphaned-jobs!)
  (ensure-sources-from-contracts!)
  (worker/init-executor!)
  (queue-initial-jobs!)
  (start-scheduler!)
  (start-watcher!)
  
  ;; Start translation worker if enabled
  (let [enabled? (config/translation-agent-enabled?)]
    (println "[server] Translation agent enabled:" enabled?)
    (when enabled?
      (println "[server] Starting translation worker")
      (translation-worker/start!)))
  
  ;; Start server
  (let [port (config/port)]
    (println (str "Server running on http://0.0.0.0:" port))
    (jetty/run-jetty #'wrapped-app {:port port :join? true})))
