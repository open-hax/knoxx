(ns knoxx.backend.extern.mongo-cache-fixture
  "Disposable native Mongo rows and clock controls for cache boundary regressions."
  (:require [clojure.walk :as walk]))

(defn date "Construct an opaque BSON-style Date for a native fixture." [at] (js/Date. at))
(defn iso "Encode fixture epoch milliseconds as portable UTC text." [at] (.toISOString (date at)))
(defn invalid-date "Construct a malformed native expiry." [] (js/Date. "invalid"))
(defn native-id "Construct opaque driver identity metadata." [] #js {:opaque "driver-id"})

(defn- portable [value]
  (walk/postwalk #(if (instance? js/Date %) (.getTime %) %) value))

(defn ^:async with-clock!
  "Run one isolated test at a fixed wall clock and always restore the native clock."
  [at f]
  (let [original (.-now js/Date)]
    (set! (.-now js/Date) (fn [] at))
    (try (await (f)) (finally (set! (.-now js/Date) original)))))

(defn- matches? [row query honor-expiry?]
  (every? (fn [[field value]]
            (cond
              (= field :expiresAt) (or (not honor-expiry?)
                                      (and (instance? js/Date (:expiresAt row))
                                           (> (.getTime (:expiresAt row)) (.getTime (:$gt value)))))
              (and (map? value) (contains? value :$in)) (contains? (set (:$in value)) (get row field))
              :else (= value (get row field)))) query))

(defn- update-row! [state name query update options]
  (let [query (js->clj query :keywordize-keys true)
        update (js->clj update :keywordize-keys true)
        previous (first (filter #(matches? % query true) (get @state name [])))
        row (merge (when-not previous (:$setOnInsert update)) previous query (:$set update))]
    (if (or previous (aget options "upsert"))
      (do (swap! state update-in [name]
                 (fn [rows] (conj (filterv #(not (matches? % query true)) rows) row)))
          (js/Promise.resolve (clj->js row)))
      (js/Promise.resolve nil))))

(defn database
  "Provide native row responses and record queries; optional stale responses test decode-time expiry."
  ([rows] (database rows true))
  ([rows honor-expiry?]
   (let [state (atom rows) queries (atom [])
         find-rows (fn [name query]
                     (let [query (js->clj query :keywordize-keys true)]
                       (swap! queries conj {:collection name :query (portable query)})
                       (filterv #(matches? % query honor-expiry?) (get @state name []))))]
     {:queries queries
      :db #js {:collection
               (fn [name]
                 #js {:findOne (fn [query] (js/Promise.resolve (clj->js (first (find-rows name query)))))
                      :find (fn [query]
                              #js {:toArray (fn [] (js/Promise.resolve (clj->js (find-rows name query))))})
                      :findOneAndUpdate
                      (fn [query update options] (update-row! state name query update options))
                      :deleteOne (fn [query]
                                   (let [query (js->clj query :keywordize-keys true)]
                                     (swap! state update name #(filterv (fn [row] (not (matches? row query true))) %))
                                     (js/Promise.resolve #js {:deletedCount 1})))
                      :createIndex (fn [_keys _options] (js/Promise.resolve "fixture-index"))})}})))
