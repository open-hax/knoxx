(ns knoxx.backend.extern.mongo-run-native-fixture
  "Own a real temporary Mongo process; never connect to an application database."
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

(defn- ^:async start! [directory binary port]
  (let [output (atom "")
        child (process/spawn binary
                             #js ["--dbpath" directory "--port" (str port)
                                  "--bind_ip" "127.0.0.1" "--noauth" "--nounixsocket"
                                  "--wiredTigerCacheSizeGB" "0.25"
                                  "--setParameter" "diagnosticDataCollectionEnabled=false"])
        exited (js/Promise. (fn [resolve reject]
                             (.once child "exit" resolve)
                             (.once child "error" reject)))]
    (.catch exited (fn [_] nil))
    (swap! owned-fixtures assoc directory {:directory directory :child child :exited exited})
    (doseq [stream [(.-stdout child) (.-stderr child)]]
      (.on stream "data" (fn [chunk]
                          (swap! output #(let [text (str % chunk)]
                                           (subs text (max 0 (- (count text) 16000))))))))
    (try
      (let [client (await (connect! port child output))
            fixture {:directory directory :binary binary :port port :child child :exited exited
                     :client client :db (.db client "knoxx_run_events_proof")}]
        (swap! owned-fixtures assoc directory fixture)
        fixture)
      (catch :default cause
        (.kill child "SIGKILL")
        (await exited)
        (throw cause)))))

(defn ^:async open!
  "Start the binary explicitly selected by KNOXX_TEST_MONGOD in a new private directory."
  []
  (force cleanup-installed?)
  (let [binary (aget js/process.env "KNOXX_TEST_MONGOD")]
    (when-not (and binary (fs/existsSync binary))
      (throw (ex-info "Set KNOXX_TEST_MONGOD to an existing mongod executable" {})))
    (let [directory (fs/mkdtempSync (path/join (os/tmpdir) "knoxx-native-run-events-"))]
      (try (await (start! directory binary (await (port!))))
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
  (await (start! (:directory fixture) (:binary fixture) (:port fixture))))

(defn ^:async close!
  "Stop only the fixture's process and remove only its owned temporary files."
  [fixture]
  (try (await (stop! fixture))
       (finally (swap! owned-fixtures dissoc (:directory fixture))
                (fs/rmSync (:directory fixture) #js {:recursive true :force true}))))
