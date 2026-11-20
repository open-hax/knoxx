(ns knoxx.backend.extern.openplanner-translation-mongo.graph-edge-identity
  "Guarded Mongo index transition for native aliases alongside SDK edge identities.")

(def index-name "translation_graph_id_unique_idx")
(def ^:private legacy-index-name "graph_id_unique_idx")

(defn- native-id-index? [index]
  (and (= {:id 1} (:key index)) (true? (:unique index))
       (= {:id {:$type "string"}} (:partialFilterExpression index))))

(defn- id-indexes [indexes]
  (let [current (some #(when (= index-name (:name %)) %) indexes)
        legacy (some #(when (= legacy-index-name (:name %)) %) indexes)]
    (when (and current (not (native-id-index? current)))
      (throw (js/Error. "translation_graph_id_unique_idx has an incompatible identity contract")))
    (when (and legacy (not (native-id-index? legacy))
               (not (and (= {:id 1} (:key legacy)) (true? (:unique legacy))
                         (nil? (:partialFilterExpression legacy)))))
      (throw (js/Error. "graph_id_unique_idx has an incompatible graph edge identity contract")))
    {:current current :legacy legacy}))

(defn- ^:async preserve-nonstring-identities! [collection]
  ;; Aggregation $type checks the scalar identity itself, including arrays.
  (when (await (.findOne collection
                        #js {"id" #js {"$exists" true}
                             "$expr" #js {"$ne" #js [#js {"$type" "$id"} "string"]}}))
    (throw (js/Error. "graph_edges has non-string legacy ids; preserve their identity index before migration"))))

(defn- ^:async retire-legacy-index! [collection]
  (try
    (await (.dropIndex collection legacy-index-name))
    (catch :default err
      ;; A concurrent initializer may retire it after ensuring the replacement.
      (when-not (or (= 27 (.-code err)) (= "IndexNotFound" (.-codeName err)))
        (throw err)))))

(defn ^:async ensure-id-index!
  "Build native string-alias protection before retiring a blanket id index.
  SDK edges without id retain authoritative primary and tuple indexes. Existing
  non-string aliases or incompatible named indexes require explicit repair."
  [collection indexes]
  (let [{:keys [current legacy]} (id-indexes indexes)]
    (if (native-id-index? legacy)
      true
      (do
        (when legacy (await (preserve-nonstring-identities! collection)))
        (when-not current
          (await (.createIndex collection #js {"id" 1}
                               #js {"unique" true "name" index-name
                                    "partialFilterExpression" #js {"id" #js {"$type" "string"}}})))
        (when legacy (await (retire-legacy-index! collection)))
        true))))
