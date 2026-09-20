(ns knoxx.backend.extern.mongo-run-events
  "Fresh journaled immutable event preparations; the run-head CAS alone accepts them."
  (:require ["mongodb" :refer [BSON]]
            ["node:buffer" :refer [Buffer]]
            ["node:crypto" :as crypto]
            [knoxx.backend.extern.mongo-run-store :as snapshot]
            [knoxx.backend.law.mongo-run-events :as law]
            [knoxx.backend.law.run-store :as run]))

(def header-collection "knoxx_run_event_headers")
(def fragment-collection "knoxx_run_event_fragments")
(def ^:private write-options #js {:writeConcern #js {:w "majority" :j true}})

(defn- digest [value]
  (-> (crypto/createHash "sha256") (.update value) (.digest "hex")))

(defn identity-key
  "A fixed-size lookup hint; complete reconstructed identities still decide acceptance."
  [id]
  (run/require! run/NonBlank id)
  (digest (.from Buffer id "utf16le")))

(defn- record! [native limit]
  (when (> (.calculateObjectSize BSON native) limit)
    (throw (ex-info "Durable Mongo record exceeds its declared byte bound"
                    {:status 413 :code "run_store_record_too_large"})))
  native)

(defn- encode [event]
  (let [encoded (pr-str event)
        bytes (.from Buffer encoded "utf16le")]
    (try
      (when-not (= event (snapshot/read-snapshot (.toString bytes "utf16le")))
        (throw (ex-info "Event did not round-trip" {})))
      (catch :default _
        (throw (ex-info "Run event must contain portable EDN values"
                        {:status 400 :code "run_event_not_portable"}))))
    bytes))

(defn ^:async insert-prepared!
  "Journal one fresh bounded native preparation; callers must not infer success after rejection."
  [db collection native limit]
  (await (.insertOne (.collection db collection) (record! native limit) write-options)))

(defn ^:async prepare!
  "Persist fresh bounded fragments and their authenticated header before head publication.
   A failed or ambiguous preparation is never reused; unreferenced candidates reserve no ID."
  [db event previous]
  (let [bytes (encode event) length (.-length bytes)
        count (js/Math.ceil (/ length law/fragment-bytes))
        nonce (crypto/randomUUID)
        ids (mapv (fn [_] (crypto/randomUUID)) (range count))
        header {:nonce nonce :run-key (identity-key (:run_id event)) :event-key (identity-key (:event_id event))
                :sequence (:sequence event) :previous previous :first-fragment (first ids)
                :fragment-count count :encoded-bytes length :payload-digest (digest bytes)}
        _ (law/header! header (:run-key header) (:sequence event))
        encoded (pr-str header) id (digest encoded)]
    (doseq [ordinal (range count)]
      (let [start (* ordinal law/fragment-bytes)
            doc (clj->js {:_id (nth ids ordinal) :event-nonce nonce :ordinal ordinal
                           :next (get ids (inc ordinal))
                           :data (.toString (.subarray bytes start (min length (+ start law/fragment-bytes))) "base64")})]
        (await (insert-prepared! db fragment-collection doc law/fragment-record-bytes))))
    (await (insert-prepared! db header-collection
                            #js {:_id id :run_key (:run-key header) :event_key (:event-key header)
                                 :header_edn encoded} law/header-record-bytes))
    {:tail id :last-sequence (:sequence event)}))

(defn ^:async candidate-exists?
  "An indexed bounded hint only; a matching uncommitted candidate is never authority."
  [db run-key event-key]
  (some? (await (.findOne (.collection db header-collection)
                          #js {:run_key run-key :event_key event-key}
                          #js {:projection #js {:_id 1} :readPreference "primary"}))))

(defn ^:async read-header!
  "Verify the content-addressed link before exposing any chronology or fragment references."
  [db reference run-key sequence]
  (law/require! law/Digest reference)
  (let [doc (await (.findOne (.collection db header-collection) #js {:_id reference} #js {:readPreference "primary"}))
        encoded (some-> doc .-header_edn)]
    (when-not (and (string? encoded) (= reference (digest encoded))) (law/corrupt!))
    (let [header (snapshot/read-snapshot encoded)]
      (law/header! header run-key sequence)
      (when-not (and (= (.-run_key doc) (:run-key header)) (= (.-event_key doc) (:event-key header)))
        (law/corrupt!))
      header)))

(defn ^:async read-event!
  "Reassemble exact UTF-16LE bytes, verify geometry and digest, then decode one complete EDN value."
  [db header]
  (let [pieces (loop [ordinal 0 reference (:first-fragment header) buffers []]
                 (if (= ordinal (:fragment-count header))
                   (do (when reference (law/corrupt!)) buffers)
                   (let [native (await (.findOne (.collection db fragment-collection) #js {:_id reference} #js {:readPreference "primary"}))
                         doc (js->clj native :keywordize-keys true)
                         _ (law/require! law/Fragment doc)
                         bytes (.from Buffer (:data doc) "base64")
                         expected (min law/fragment-bytes (- (:encoded-bytes header) (* ordinal law/fragment-bytes)))]
                     (when-not (and (= reference (:_id doc)) (= ordinal (:ordinal doc))
                                    (= (:nonce header) (:event-nonce doc))
                                    (= expected (.-length bytes)) (= (:data doc) (.toString bytes "base64")))
                       (law/corrupt!))
                     (recur (inc ordinal) (:next doc) (conj buffers bytes)))))
        bytes (.concat Buffer (into-array pieces))]
    (when-not (and (= (:encoded-bytes header) (.-length bytes))
                   (even? (.-length bytes)) (= (:payload-digest header) (digest bytes)))
      (law/corrupt!))
    (let [encoded (.toString bytes "utf16le")]
      (when-not (.equals bytes (.from Buffer encoded "utf16le")) (law/corrupt!))
      (let [event (snapshot/read-snapshot encoded)]
        (when-not (and (string? (:run_id event)) (string? (:event_id event))
                       (= (:run-key header) (identity-key (:run_id event)))
                       (= (:event-key header) (identity-key (:event_id event))))
          (law/corrupt!))
        event))))

(defn ^:async setup-indexes!
  "Index candidate existence without reserving unaccepted event identities. No event TTL is installed."
  [db]
  (await (.createIndex (.collection db header-collection)
                       #js {:run_key 1 :event_key 1} #js {:name "run_event_candidate_lookup"})))
