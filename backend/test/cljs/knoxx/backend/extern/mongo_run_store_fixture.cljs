(ns knoxx.backend.extern.mongo-run-store-fixture
  "Native Mongo boundary fixture with actual revision predicates and duplicate-key behavior."
  (:require [cljs.reader :as reader]))

(defn- matches? [doc query]
  (and (or (nil? (.-persistence_token query)) (= (:persistence_token doc) (.-persistence_token query)))
       (or (nil? (.-run_state_edn query)) (= (:run_state_edn doc) (.-run_state_edn query)))
       (or (nil? (aget query "$expr"))
           (= doc (js->clj (aget (aget (aget query "$expr") "$eq") 1 "$literal") :keywordize-keys true)))
       (or (nil? (.-_id query)) (= (:_id doc) (.-_id query)))
       (or (nil? (.-run_key query)) (= (:run_key doc) (.-run_key query)))
       (or (nil? (.-event_key query)) (= (:event_key doc) (.-event_key query)))
       (or (nil? (.-run_id query)) (= (:run_id doc) (.-run_id query)))
       (or (nil? (.-session_id query)) (= (:session_id doc) (.-session_id query)))
       (or (nil? (.-org_id query)) (= (:org_id doc) (.-org_id query)))
       (or (nil? (.-status query))
           (some #{(:status doc)} (array-seq (aget (.-status query) "$in"))))
       (or (nil? (.-persistence_revision query))
           (let [revision (.-persistence_revision query)]
             (if (number? revision) (= revision (:persistence_revision doc))
                 (not (contains? doc :persistence_revision)))))))

(defn- replace-one [documents writes fail-after-commit? query native options]
                       (let [doc (js->clj native :keywordize-keys true) id (:run_id doc)
                             current (get @documents id)]
                         (swap! writes conj (js->clj options :keywordize-keys true))
                         (if (and current (matches? current query))
                           (do (swap! documents assoc id doc)
                               (if (compare-and-set! fail-after-commit? true false)
                                 (js/Promise.reject (js/Error. "acknowledgement lost"))
                                 (js/Promise.resolve #js {:matchedCount 1})))
                           (js/Promise.resolve #js {:matchedCount 0}))))

(defn- collection [documents writes reads fail-after-commit?]
  #js {:findOne (fn [query options]
                  (swap! reads conj (js->clj options :keywordize-keys true))
                  (js/Promise.resolve (some #(when (matches? % query) (clj->js %)) (vals @documents))))
       :insertOne (fn [native options]
                    (let [doc (js->clj native :keywordize-keys true) id (or (:_id doc) (:run_id doc))]
                      (swap! writes conj (js->clj options :keywordize-keys true))
                      (if (contains? @documents id)
                        (js/Promise.reject (doto (js/Error. "duplicate") (aset "code" 11000)))
                        (do (swap! documents assoc id doc) (js/Promise.resolve #js {:acknowledged true})))))
       :updateOne (fn [query change options]
                    (swap! writes conj (js->clj options :keywordize-keys true))
                    (if-let [[id doc] (some (fn [[id doc]] (when (matches? doc query) [id doc])) @documents)]
                      (do (swap! documents assoc id
                                 (reduce-kv (fn [doc key amount] (update doc key (fnil + 0) amount))
                                            (merge doc (js->clj (aget change "$set") :keywordize-keys true))
                                            (js->clj (aget change "$inc") :keywordize-keys true)))
                          (js/Promise.resolve #js {:matchedCount 1}))
                      (js/Promise.resolve #js {:matchedCount 0})))
       :replaceOne (partial replace-one documents writes fail-after-commit?)
       :find (fn [query options]
               (swap! reads conj (js->clj options :keywordize-keys true))
               #js {:toArray (fn [] (js/Promise.resolve
                                     (clj->js (vec (filter #(matches? % query) (vals @documents))))))})
       :createIndex (fn [_ _] (js/Promise.resolve "index"))})

(defn create
  "Return isolated collections plus persisted documents and head acknowledgement injection."
  []
  (let [documents (atom {}) headers (atom {}) fragments (atom {})
        writes (atom []) reads (atom []) fail-after-commit? (atom false)
        collections {"knoxx_runs" (collection documents writes reads fail-after-commit?)
                     "knoxx_run_event_headers" (collection headers writes reads fail-after-commit?)
                     "knoxx_run_event_fragments" (collection fragments writes reads fail-after-commit?)}]
    {:db #js {:collection (fn [name] (or (get collections name)
                                         (throw (ex-info "Unexpected fixture collection" {:name name}))))}
     :documents documents :headers headers :fragments fragments
     :writes writes :reads reads :fail-after-commit? fail-after-commit?}))

(defn persisted-state
  "Decode actual stored EDN to check native serialization and internal sequence integrity."
  [fixture id]
  (reader/read-string (get-in @(:documents fixture) [id :run_state_edn])))

(defn date
  "Construct an actual legacy Mongo Date field."
  [instant]
  (js/Date. instant))
