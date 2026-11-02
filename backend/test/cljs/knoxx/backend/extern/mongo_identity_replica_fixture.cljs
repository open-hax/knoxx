(ns knoxx.backend.extern.mongo-identity-replica-fixture
  "Own a private one-node Mongo replica set for actual identity bootstrap transactions."
  (:require ["mongodb" :refer [MongoClient]]
            ["node:child_process" :as process]
            ["node:fs" :as fs]
            ["node:net" :as net]
            ["node:os" :as os]
            ["node:path" :as path]))

(declare close!)
(defonce ^:private owned-fixtures (atom {}))

(defn- ^:async interrupt! [code]
  (doseq [fixture (vals @owned-fixtures)]
    (try (await (close! fixture))
         (catch :default cause (js/console.error "Mongo fixture cleanup failed" cause))))
  (.exit js/process code))

(defonce ^:private cleanup-installed?
  (delay (.once js/process "SIGINT" #(interrupt! 130))
         (.once js/process "SIGTERM" #(interrupt! 143))
         true))

(defn- delay! [milliseconds]
  (js/Promise. (fn [resolve _] (js/setTimeout resolve milliseconds))))

(defn- port! []
  (js/Promise.
   (fn [resolve reject]
     (let [server (net/createServer)]
       (.once server "error" reject)
       (.listen server 0 "127.0.0.1"
                (fn [] (let [port (.-port (.address server))]
                         (.close server (fn [] (resolve port))))))))))

(defn- ^:async connect! [port child output]
  (let [uri (str "mongodb://127.0.0.1:" port "/?directConnection=true")]
    (loop [attempt 0]
      (when (or (>= attempt 50) (some? (.-exitCode child)))
        (throw (ex-info (str "Owned Mongo fixture did not become ready: " @output) {})))
      (let [client (MongoClient. uri #js {:serverSelectionTimeoutMS 200 :connectTimeoutMS 200})
            ready? (try (await (.connect client)) true
                        (catch :default _ (await (.close client)) false))]
        (if ready? client (do (await (delay! 100)) (recur (inc attempt))))))))

(defn- ^:async observe-exit! [exited]
  ;; Attach immediately while readiness is pending; start!/stop! still await the
  ;; original rejecting promise and report spawn failures to their callers.
  (try (await exited) (catch :default _ nil)))

(defn- ^:async primary! [client port initialize?]
  (let [admin (.db client "admin")]
    (when initialize?
      (await (.command admin
                        (clj->js {:replSetInitiate
                                  {:_id "knoxx-identity-proof"
                                   :members [{:_id 0 :host (str "127.0.0.1:" port)}]
                                   :settings {:electionTimeoutMillis 1000 :heartbeatIntervalMillis 500}}}))))
    (loop [attempt 0]
      (when (>= attempt 100)
        (throw (ex-info "Owned identity replica set did not elect its primary" {})))
      (let [hello (await (.command admin #js {:hello 1}))]
        (when-not (true? (.-isWritablePrimary hello))
          (await (delay! 100))
          (recur (inc attempt)))))))

(defn- ^:async stop-failed-start! [directory child exited]
  (try
    (when-let [client (:client (get @owned-fixtures directory))]
      (await (.close client)))
    (finally (.kill child "SIGKILL") (await exited))))

(defn- ^:async start! [directory binary port initialize?]
  (let [output (atom "")
        child (process/spawn binary
                             #js ["--dbpath" directory "--port" (str port)
                                  "--bind_ip" "127.0.0.1" "--noauth" "--nounixsocket"
                                  "--replSet" "knoxx-identity-proof"
                                  "--wiredTigerCacheSizeGB" "0.25"
                                  "--setParameter" "diagnosticDataCollectionEnabled=false"])
        exited (js/Promise. (fn [resolve reject]
                             (.once child "exit" resolve)
                             (.once child "error" reject)))]
    (observe-exit! exited)
    (swap! owned-fixtures assoc directory {:directory directory :child child :exited exited})
    (doseq [stream [(.-stdout child) (.-stderr child)]]
      (.on stream "data" (fn [chunk]
                          (swap! output #(let [text (str % chunk)]
                                           (subs text (max 0 (- (count text) 16000))))))))
    (try
      (let [client (await (connect! port child output))
            _ (swap! owned-fixtures update directory assoc :client client)
            _ (await (primary! client port initialize?))
            fixture {:directory directory :binary binary :port port :child child :exited exited
                     :client client :db (.db client "knoxx_identity_bootstrap_proof")}]
        (swap! owned-fixtures assoc directory fixture)
        fixture)
      (catch :default cause
        (await (stop-failed-start! directory child exited))
        (throw cause)))))

(defn ^:async open!
  "Start an owned one-node replica set using the explicitly selected KNOXX_TEST_MONGOD."
  []
  (force cleanup-installed?)
  (let [binary (aget js/process.env "KNOXX_TEST_MONGOD")]
    (when-not (and binary (fs/existsSync binary))
      (throw (ex-info "Set KNOXX_TEST_MONGOD to an existing mongod executable" {})))
    (let [directory (fs/mkdtempSync (path/join (os/tmpdir) "knoxx-native-identity-replica-"))]
      (try (await (start! directory binary (await (port!)) true))
           (catch :default cause
             (swap! owned-fixtures dissoc directory)
             (fs/rmSync directory #js {:recursive true :force true})
             (throw cause))))))

(defn- ^:async stop! [fixture]
  (when-let [client (:client fixture)] (await (.close client)))
  (when (nil? (.-exitCode (:child fixture)))
    (.kill (:child fixture) "SIGTERM"))
  (let [timer (js/setTimeout #(.kill (:child fixture) "SIGKILL") 15000)]
    (try (await (:exited fixture)) (finally (js/clearTimeout timer)))))

(defn ^:async restart!
  "Stop and reopen the actual Mongo process using exactly the same database directory."
  [fixture]
  (await (stop! fixture))
  (await (start! (:directory fixture) (:binary fixture) (:port fixture) false)))

(defn ^:async close!
  "Stop only the fixture's process and remove only its owned temporary files."
  [fixture]
  (try (await (stop! fixture))
       (finally (swap! owned-fixtures dissoc (:directory fixture))
                (fs/rmSync (:directory fixture) #js {:recursive true :force true}))))
