(ns knoxx.backend.extern.mongo-cache
  "Native Mongo transport for the finite cache and atomic rate-counter protocols."
  (:require [knoxx.backend.law.cache-store :as law]))

(def bucket-layout
  "Legacy collection/key coordinates retained during provider replacement."
  {:titles ["knoxx_thread_titles" "session_id"] :temp-memory ["knoxx_temp_memory" "key"]
   :memory-sessions ["knoxx_memory_threads" "cache_key"] :rate-limits ["knoxx_rate_limits" "key"]})

(defn- collection [db bucket] (.collection db (first (get bucket-layout bucket))))
(defn- query [bucket cache-key] (clj->js {(second (get bucket-layout bucket)) cache-key}))

(defn ^:async read!
  "Decode a live cache value, including the previous named-collection representation."
  [db bucket cache-key]
  (law/assert-value-key! bucket cache-key)
  (when-let [row (await (.findOne (collection db bucket) (query bucket cache-key)))]
    (let [expires (aget row "expiresAt")
          expiry (if (instance? js/Date expires) (.getTime expires) (.parse js/Date expires))
          value (js->clj row :keywordize-keys true)]
      (when (and (js/Number.isFinite expiry) (> expiry (.now js/Date)))
        (if (= "finite-v1" (:cacheFormat value)) (:value value)
          (if (= bucket :temp-memory) (:value value) (dissoc value :_id)))))))

(defn ^:async write!
  "Store one value with an explicit expiry in the named legacy collection."
  [db bucket cache-key value ttl-ms]
  (law/assert-operation! {:kind :put :bucket bucket :key cache-key :value value :ttl-ms ttl-ms :at (.now js/Date)})
  (await (.updateOne (collection db bucket) (query bucket cache-key)
                    (clj->js {"$set" {:value value :cacheFormat "finite-v1"
                                       :expiresAt (js/Date. (+ (.now js/Date) ttl-ms))}})
                    #js {:upsert true}))
  true)

(defn ^:async delete!
  "Delete the selected bucket entry idempotently."
  [db bucket cache-key]
  (law/assert-value-key! bucket cache-key)
  (await (.deleteOne (collection db bucket) (query bucket cache-key)))
  true)

(defn ^:async increment!
  "Atomically reset an expired window or increment without moving its existing expiry."
  [db cache-key ttl-ms]
  (let [now (.now js/Date)
        _ (law/assert-operation! {:kind :increment :bucket :rate-limits :key cache-key :ttl-ms ttl-ms :at now})
        instant (js/Date. now)
        expired {"$lte" [{"$ifNull" ["$expiresAt" (js/Date. 0)]} instant]}
        update [{"$set" {"count" {"$cond" [expired 1 {"$add" [{"$ifNull" ["$count" 0]} 1]}]}
                         "expiresAt" {"$cond" [expired (js/Date. (+ now ttl-ms)) "$expiresAt"]}}}]
        row (await (.findOneAndUpdate (collection db :rate-limits) (query :rate-limits cache-key)
                                      (clj->js update) #js {:upsert true :returnDocument "after"}))]
    (or (aget row "count") (throw (ex-info "Mongo did not return its admitted counter" {:status 503})))))

(defn ^:async setup-indexes!
  "Establish each named unique key plus eventual physical expiry cleanup."
  [db]
  (doseq [[bucket [_ field]] bucket-layout]
    (let [coll (collection db bucket)]
      (await (.createIndex coll (clj->js {field 1}) #js {:unique true}))
      (await (.createIndex coll #js {:expiresAt 1} #js {:expireAfterSeconds 0}))))
  true)
