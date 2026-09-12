(ns knoxx.backend.extern.mongo-run-store-fixture
  "Native Mongo boundary fixture with actual revision predicates and duplicate-key behavior."
  (:require [cljs.reader :as reader]))

(defn- matches? [doc query]
  (and (or (nil? (.-run_id query)) (= (:run_id doc) (.-run_id query)))
       (or (nil? (.-session_id query)) (= (:session_id doc) (.-session_id query)))
       (or (nil? (.-org_id query)) (= (:org_id doc) (.-org_id query)))
       (or (nil? (.-status query))
           (some #{(:status doc)} (array-seq (aget (.-status query) "$in"))))
       (or (nil? (.-persistence_revision query))
           (let [revision (.-persistence_revision query)]
             (if (number? revision) (= revision (:persistence_revision doc))
                 (not (contains? doc :persistence_revision)))))))

(defn create
  "Return an opaque database plus test-only persisted documents and acknowledgement injection."
  []
  (let [documents (atom {})
        writes (atom [])
        fail-after-commit? (atom false)
        collection #js
        {:findOne (fn [query] (js/Promise.resolve
                               (some #(when (matches? % query) (clj->js %)) (vals @documents))))
         :insertOne (fn [native options]
                      (let [doc (js->clj native :keywordize-keys true) id (:run_id doc)]
                        (swap! writes conj (js->clj options :keywordize-keys true))
                        (if (contains? @documents id)
                          (js/Promise.reject (doto (js/Error. "duplicate") (aset "code" 11000)))
                          (do (swap! documents assoc id doc) (js/Promise.resolve #js {:acknowledged true})))))
         :replaceOne (fn [query native options]
                       (let [doc (js->clj native :keywordize-keys true) id (:run_id doc)
                             current (get @documents id)]
                         (swap! writes conj (js->clj options :keywordize-keys true))
                         (if (and current (matches? current query))
                           (do (swap! documents assoc id doc)
                               (if (compare-and-set! fail-after-commit? true false)
                                 (js/Promise.reject (js/Error. "acknowledgement lost"))
                                 (js/Promise.resolve #js {:matchedCount 1})))
                           (js/Promise.resolve #js {:matchedCount 0}))))
         :find (fn [query]
                 #js {:toArray (fn [] (js/Promise.resolve
                                       (clj->js (vec (filter #(matches? % query) (vals @documents))))))})
         :createIndex (fn [_ _] (js/Promise.resolve "index"))}]
    {:db #js {:collection (fn [_] collection)}
     :documents documents :writes writes :fail-after-commit? fail-after-commit?}))

(defn persisted-state
  "Decode actual stored EDN to check native serialization and internal sequence integrity."
  [fixture id]
  (reader/read-string (get-in @(:documents fixture) [id :run_state_edn])))

(defn date
  "Construct an actual legacy Mongo Date field."
  [instant]
  (js/Date. instant))
