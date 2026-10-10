(ns knoxx.backend.extern.translation-graph-mongo-fixture
  "In-memory Mongo boundary enforcing primary, native-alias, and SDK tuple indexes."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            ["mongodb" :refer [ObjectId]]))

(def primary-index {:name "_id_" :key {:_id 1} :unique true})
(def sdk-index {:name "source_node_id_1_target_node_id_1_edge_kind_1"
                :key {:source_node_id 1 :target_node_id 1 :edge_kind 1} :unique true})
(def native-index {:name common/graph-edge-id-index-name :key {:id 1} :unique true
                   :partialFilterExpression {:id {:$type "string"}}})
(def legacy-index {:name common/graph-id-index-name :key {:id 1} :unique true})

(defn mongo-error [code code-name message]
  (doto (js/Error. message) (aset "code" code) (aset "codeName" code-name)))

(defn- identity-value [value]
  (if (instance? ObjectId value) (.toHexString value) value))

(defn- bson-type? [value type-name]
  (case type-name
    "string" (string? value)
    "objectId" (instance? ObjectId value)
    "array" (vector? value)
    false))

(defn- mongo-equality? [value expected]
  (or (= (identity-value expected) (identity-value value))
      (and (vector? value) (some #(= expected %) value))))

(declare field-matches?)

(defn- field-matches? [row field expected]
  (if (map? expected)
    (every? (fn [[operator value]]
              (case operator
                :$exists (= value (contains? row field))
                :$type (bson-type? (get row field) value)
                :$eq (mongo-equality? (get row field) value)
                :$not (not (field-matches? row field value))
                false)) expected)
    (mongo-equality? (get row field) expected)))

(declare matches?)

(defn- expression-matches? [row field expected]
  (case field
    :$and (every? #(matches? row %) expected)
    :$or (boolean (some #(matches? row %) expected))
    ;; Migration uses aggregation $type so arrays containing a string are
    ;; still recognized as non-string identities.
    :$expr (not (string? (:id row)))
    (field-matches? row field expected)))

(defn- matches? [row selector]
  (every? (fn [[field expected]] (expression-matches? row field expected)) selector))

(defn- unique-member? [index row]
  (and (:unique index)
       (or (nil? (:partialFilterExpression index))
           (string? (:id row))
           (and (vector? (:id row)) (some string? (:id row))))))

(defn- indexed-key [index row]
  (mapv #(let [value (get row %)]
           (identity-value (if (vector? value) (first value) value))) (keys (:key index))))

(defn- assert-unique! [indexes rows candidate]
  (doseq [index indexes]
    (when (and (unique-member? index candidate)
               (some #(and (unique-member? index %)
                           (= (indexed-key index candidate) (indexed-key index %))) rows))
      (throw (mongo-error 11000 "DuplicateKey" (str "E11000 " (:name index)))))))

(defn- assert-index-build! [index rows]
  (doseq [[position row] (map-indexed vector rows)]
    (assert-unique! [index] (subvec rows 0 position) row)))

(defn- without-row [rows row]
  (filterv #(not= (identity-value (:_id row)) (identity-value (:_id %))) rows))

(defn- upsert! [{:keys [rows* indexes* operations*]} raw-selector raw-update]
  (let [selector (js->clj raw-selector :keywordize-keys true)
        update (js->clj raw-update :keywordize-keys true)
        prior (some #(when (matches? % selector) %) @rows*)
        candidate (merge (or prior (assoc (:$setOnInsert update) :_id
                                         (or (get-in update [:$setOnInsert :_id]) (ObjectId.))))
                         (:$set update))]
    (swap! operations* conj [:upsert selector])
    (assert-unique! @indexes* (if prior (without-row @rows* prior) @rows*) candidate)
    (swap! rows* #(conj (if prior (without-row % prior) %) candidate))
    (js/Promise.resolve #js {"value" (when prior (clj->js prior))})))

(defn- replace! [{:keys [rows* indexes*]} raw-selector raw-replacement]
  (let [selector (js->clj raw-selector :keywordize-keys true)
        prior (some #(when (matches? % selector) %) @rows*)]
    (when prior
      (let [replacement (assoc (js->clj raw-replacement :keywordize-keys true) :_id (:_id prior))]
        (assert-unique! @indexes* (without-row @rows* prior) replacement)
        (swap! rows* #(conj (without-row % prior) replacement))))
    (js/Promise.resolve #js {"matchedCount" (if prior 1 0)})))

(defn- delete! [{:keys [rows*]} raw-selector]
  (let [selector (js->clj raw-selector :keywordize-keys true)
        prior (some #(when (matches? % selector) %) @rows*)]
    (when prior (swap! rows* without-row prior))
    (js/Promise.resolve #js {"deletedCount" (if prior 1 0)})))

(defn- create-index! [{:keys [rows* indexes* operations* options]} raw-key raw-options]
  (let [index (assoc (js->clj raw-options :keywordize-keys true)
                     :key (js->clj raw-key :keywordize-keys true))]
    (swap! operations* conj [:create (:name index)])
    (when-let [error (:create-error options)] (throw error))
    (assert-index-build! index @rows*)
    (swap! indexes* conj index)
    (js/Promise.resolve (:name index))))

(defn- drop-index! [{:keys [indexes* operations* options]} name]
  (swap! operations* conj [:drop name])
  (when-let [error (:drop-error options)] (throw error))
  (swap! indexes* #(filterv (fn [index] (not= name (:name index))) %))
  (js/Promise.resolve true))

(defn collection
  "Return a native collection and inspectable state; never opens a Mongo client."
  ([rows indexes] (collection rows indexes {}))
  ([rows indexes options]
   (let [state {:rows* (atom (vec rows)) :indexes* (atom (vec indexes))
                :operations* (atom []) :options options}
         native #js {"indexes" (fn [] (js/Promise.resolve (clj->js @(:indexes* state))))
                     "findOne" (fn [query]
                                 (js/Promise.resolve
                                  (some #(when (matches? % (js->clj query :keywordize-keys true))
                                           (clj->js %)) @(:rows* state))))
                     "createIndex" #(create-index! state %1 %2)
                     "dropIndex" #(drop-index! state %)
                     "findOneAndUpdate" (fn [selector update _] (upsert! state selector update))
                     "replaceOne" #(replace! state %1 %2)
                     "deleteOne" #(delete! state %)}]
     (assoc state :native native))))

(defn graph [edges]
  (let [nodes (collection [] [primary-index legacy-index])]
    {:nodes nodes :edges edges
     :collections {:graph-nodes (:native nodes) :graph-edges (:native edges)}}))

(defn rows [collection] @(:rows* collection))
(defn operations [collection] @(:operations* collection))

(def segment {:_id "segment-one" :document_id "document-one" :source_text "Fixture source"
              :translated_text "Fixture translation" :source_lang "en" :target_lang "es"
              :project "fixture-project"})

(def native-edge {:id "translation:doc:document-one:segment-one" :source "document-one"
                  :target "translation:en:es:segment-one" :kind "has_translation"
                  :data {:source_lang "en" :target_lang "es"}})

(def sdk-edge {:_id "sdk-source||sdk-target||contains_sentence"
               :source_node_id "sdk-source" :target_node_id "sdk-target"
               :edge_kind "contains_sentence" :layer "fixture-layer" :project "fixture-project"
               :source {:kind "fixture"} :data {:canonical_id "sdk-owned-identity"}})

(def canonical-edge-id "document-one||translation:en:es:segment-one||has_translation")
