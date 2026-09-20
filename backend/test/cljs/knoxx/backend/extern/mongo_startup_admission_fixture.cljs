(ns knoxx.backend.extern.mongo-startup-admission-fixture
  "Instrument only real writes in an owned Mongo database; never emulate stored state."
  (:require ["mongodb" :refer [BSON]]
            ["node:crypto" :as crypto]))

(defn gate
  "Own a completion promise for one deterministic native-call interleaving."
  []
  (let [complete* (atom nil)
        promise (js/Promise. (fn [resolve _] (reset! complete* resolve)))]
    {:promise promise :complete! #(@complete* %)}))

(defn- call [object method args] (.apply (aget object method) object (to-array args)))

(defn- collection-proxy [collection mode armed* entered release calls*]
  (js/Proxy.
   collection
   #js {:get
        (fn [target property]
          (let [value (aget target property)]
            (if (contains? #{"insertOne" "replaceOne"} property)
              (^:async fn [& args]
                (swap! calls* conj {:method property :options (js->clj (last args) :keywordize-keys true)})
                (if (compare-and-set! armed* true false)
                  (do
                    ((:complete! entered) true)
                    (when (= mode :delay) (await (:promise release)))
                    (let [result (await (call target property args))]
                      (if (= mode :lost-ack)
                        (throw (ex-info "Injected lost acknowledgment after actual durable write"
                                        {:status 503 :code "fixture_startup_ack_lost"}))
                        result)))
                  (await (call target property args))))
              (if (fn? value) (.bind value target) value))))}))

(defn intercept
  "Delay the first real claim or lose its acknowledgment after Mongo has accepted it."
  [db collection-name mode]
  (let [entered (gate) release (gate) armed* (atom true) calls* (atom [])
        wrapped (collection-proxy (.collection db collection-name) mode armed* entered release calls*)]
    {:db #js {:collection (fn [name] (if (= name collection-name) wrapped (.collection db name)))}
     :entered (:promise entered) :release! #((:complete! release) true) :calls calls*}))

(defn ^:async raw-document
  "Read evidence with native BSON types intact under a primary read preference."
  [db collection id-field id]
  (await (.findOne (.collection db collection) (clj->js {id-field id}) #js {:readPreference "primary"})))

(defn fingerprint
  "Hash exact BSON bytes, including types and native identity, for unchanged-row assertions."
  [document]
  (-> (crypto/createHash "sha256") (.update (.serialize BSON document)) (.digest "hex")))

(defn field
  "Inspect only a named fixture-owned native field."
  [document key] (aget document key))

(defn ^:async replace-date-type!
  "Change an existing Date to its identical JSON text, exposing lossy preimage comparisons."
  [db id]
  (let [coll (.collection db "knoxx_threads")
        document (await (.findOne coll #js {:session_id id}))]
    (await (.updateOne coll #js {:session_id id}
                       #js {"$set" #js {"createdAt" (.toISOString (.-createdAt document))}}
                       #js {:writeConcern #js {:w "majority" :j true}}))))

(defn large-text
  "Create a real nine-MiB payload whose duplicated BSON CAS command requires refusal."
  [] (.repeat "x" (* 9 1024 1024)))

(defn ^:async retire-failed-row!
  "Model physical TTL retirement of exactly this fixture's failed owner, without waiting an hour."
  [db record]
  (let [result (await (.deleteOne (.collection db "knoxx_threads")
                                 (clj->js (select-keys (assoc record :startup_failure "initial_admission_failed")
                                                      [:session_id :startup_token :startup_failure]))
                                 #js {:writeConcern #js {:w "majority" :j true}}))]
    (= 1 (.-deletedCount result))))
