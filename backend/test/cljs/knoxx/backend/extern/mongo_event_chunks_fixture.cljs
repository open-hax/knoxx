(ns knoxx.backend.extern.mongo-event-chunks-fixture
  "Observe real standalone Mongo command sizes and own every fault-proof resource."
  (:require ["mongodb" :refer [BSON MongoClient]]
            ["node:buffer" :refer [Buffer]]
            ["node:crypto" :as crypto]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.mongo-run-store :as store]))

(defn- size [native] (.calculateObjectSize BSON native))

(defn- observe! [metrics event]
  (let [command (.-command event) name (.-commandName event)]
    (when (#{"insert" "update"} name)
      (let [collection (aget command name)
            records (if (= "insert" name) (array-seq (.-documents command))
                        (map #(.-u %) (array-seq (.-updates command))))]
        (swap! metrics update-in [collection :commands] (fnil inc 0))
        (swap! metrics update-in [collection :max-command-bytes] (fnil max 0) (size command))
        (doseq [record records]
          (swap! metrics update-in [collection :records] (fnil inc 0))
          (swap! metrics update-in [collection :record-bytes] (fnil + 0) (size record))
          (swap! metrics update-in [collection :max-record-bytes] (fnil max 0) (size record)))
        (when-not (= {:w "majority" :j true} (js->clj (.-writeConcern command) :keywordize-keys true))
          (swap! metrics update :non-durable-writes (fnil inc 0)))))))

(defn- ^:async attach! [owned metrics]
  (let [client (MongoClient. (str "mongodb://127.0.0.1:" (:port owned) "/?directConnection=true")
                             #js {:monitorCommands true :readPreference "secondaryPreferred"})]
    (try
      (.on client "commandStarted" (partial observe! metrics))
      (await (.connect client))
      {:native owned :client client :db (.db client "knoxx_event_chunks_proof") :metrics metrics}
      (catch :default cause (await (.close client)) (await (native/close! owned)) (throw cause)))))

(defn ^:async open!
  "Start only the explicit fixture binary and a monitored client, never an application database."
  []
  (await (attach! (await (native/open!)) (atom {}))))

(defn ^:async restart!
  "Reopen the owned Mongo data directory with a new monitored client."
  [fixture]
  (await (.close (:client fixture)))
  (await (attach! (await (native/restart! (:native fixture))) (:metrics fixture))))

(defn ^:async close!
  "Remove this fixture's process and directory after closing both owned clients."
  [fixture]
  (try (await (.close (:client fixture)))
       (finally (await (native/close! (:native fixture))))))

(defn ^:async document
  "Return stored test evidence with native object identities removed."
  [db collection query]
  (some-> (await (.findOne (.collection db collection) (clj->js query) #js {:readPreference "primary"}))
          (js->clj :keywordize-keys true) (dissoc :_id)))

(defn ^:async head
  "Decode the current authoritative envelope for bounded-head evidence."
  [db run-id]
  (store/read-snapshot (:run_state_edn (await (document db store/collection-name {:run_id run-id})))))

(defn ^:async insert!
  "Seed one actual old-format record in this owned database."
  [db collection doc]
  (await (.insertOne (.collection db collection) (clj->js doc)
                     #js {:writeConcern #js {:w "majority" :j true}})))

(defn ^:async change!
  "Inject a deterministic native mutation into only this fixture's records."
  [db collection query fields]
  (await (.updateOne (.collection db collection) (clj->js query) #js {"$set" (clj->js fields)}
                     #js {:writeConcern #js {:w "majority" :j true}})))

(defn ^:async remove!
  "Remove an owned test fragment to prove fail-closed recovery."
  [db collection query]
  (await (.deleteOne (.collection db collection) (clj->js query)
                     #js {:writeConcern #js {:w "majority" :j true}})))

(defn ^:async count-documents
  "Count actual native preparations, including unaccepted candidates."
  [db collection]
  (await (.countDocuments (.collection db collection) #js {} #js {:readPreference "primary"})))

(defn fingerprint
  "Hash exact UTF16 bytes so large/unpaired-surrogate assertions stay concise."
  [text]
  (-> (crypto/createHash "sha256") (.update (.from Buffer text "utf16le")) (.digest "hex")))

(defn utf8-bytes
  "Measure the original single-document EDN ceiling in actual encoded bytes."
  [text]
  (.byteLength Buffer text "utf8"))

(defn date
  "An actual BSON Date in the native revisionless legacy fixture."
  [text]
  (js/Date. text))

(defn ^:async replace!
  "Emulate an old canonical writer: its complete replacement discards an unfamiliar token."
  [db collection query doc]
  (await (.replaceOne (.collection db collection) (clj->js query) (clj->js doc)
                      #js {:writeConcern #js {:w "majority" :j true}})))

(defn ^:async document-fingerprint
  "Hash the entire native BSON row, including ObjectId and Date, to prove refusal preserves it."
  [db collection query]
  (let [doc (await (.findOne (.collection db collection) (clj->js query) #js {:readPreference "primary"}))]
    (-> (crypto/createHash "sha256") (.update (.serialize BSON doc)) (.digest "hex"))))
