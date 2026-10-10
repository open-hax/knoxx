(ns knoxx.backend.extern.translation-graph-mongo-test
  "Native Mongo graph edges coexist with SDK identities without incumbent adoption."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.extern.openplanner-translation-mongo.graph-memory :as memory]
            [knoxx.backend.extern.translation-graph-mongo-fixture :as fixture]
            ["mongodb" :refer [ObjectId]]))

(defn- ^:async index-error [collection]
  (try (await (common/ensure-graph-edge-id-index! (:native collection))) nil
       (catch :default error (.-message error))))

(defn- ^:async approve! [graph segment]
  (await (memory/upsert-graph-memory! (:collections graph) (clj->js segment) "Reviewed fixture")))

(deftest ^:async sdk-missing-aliases-and-distinct-native-edges-coexist
  (let [sdk-rows [fixture/sdk-edge (assoc fixture/sdk-edge :_id "sdk-other||sdk-target||contains_sentence"
                                        :source_node_id "sdk-other")]
        edges (fixture/collection sdk-rows [fixture/primary-index fixture/sdk-index])
        graph (fixture/graph edges)]
    (is (true? (await (common/ensure-graph-edge-id-index! (:native edges)))))
    (is (= [[:create common/graph-edge-id-index-name]] (fixture/operations edges)))
    (is (= {:success true} (await (approve! graph fixture/segment))))
    (is (= {:success true} (await (approve! graph fixture/segment))))
    (is (= {:success true} (await (approve! graph (assoc fixture/segment :_id "segment-two")))))
    (is (= 2 (count (fixture/rows (:nodes graph)))))
    (is (= sdk-rows (filterv #(not (contains? % :id)) (fixture/rows edges))))
    (let [native-rows (filterv :id (fixture/rows edges))]
      (is (= 2 (count native-rows)))
      (is (= #{fixture/canonical-edge-id "document-one||translation:en:es:segment-two||has_translation"}
             (set (map :_id native-rows))))
      (is (every? #(= [(:source %) (:target %) (:kind %)]
                      [(:source_node_id %) (:target_node_id %) (:edge_kind %)]) native-rows)))))

(deftest ^:async blanket-alias-index-is-retired-only-after-replacement
  (let [edges (fixture/collection [fixture/sdk-edge]
                                  [fixture/primary-index fixture/sdk-index fixture/legacy-index])]
    (is (nil? (await (index-error edges))))
    (is (= [[:create common/graph-edge-id-index-name] [:drop common/graph-id-index-name]]
           (fixture/operations edges)))
    (is (some #(= fixture/native-index %) @(:indexes* edges)))
    (is (not-any? #(= common/graph-id-index-name (:name %)) @(:indexes* edges)))))

(deftest ^:async rejected-build-keeps-legacy-alias-protection
  (let [edges (fixture/collection [] [fixture/legacy-index]
                                  {:create-error (fixture/mongo-error 85 "IndexOptionsConflict" "build refused")})]
    (is (= "build refused" (await (index-error edges))))
    (is (= [[:create common/graph-edge-id-index-name]] (fixture/operations edges)))
    (is (= [fixture/legacy-index] @(:indexes* edges)))))

(deftest ^:async duplicate-string-aliases-have-no-nonunique-fallback
  (let [rows [{:_id "one" :id "same"} {:_id "two" :id "same"}]
        edges (fixture/collection rows [fixture/primary-index])]
    (is (re-find #"E11000" (await (index-error edges))))
    (is (= rows (fixture/rows edges)))
    (is (= [fixture/primary-index] @(:indexes* edges)))))

(deftest ^:async named-index-contracts-must-match-before-migration
  (doseq [index [(assoc fixture/native-index :unique false)
                 (assoc fixture/native-index :partialFilterExpression {:id {:$exists true}})
                 (assoc fixture/legacy-index :key {:other 1})
                 (assoc fixture/legacy-index :partialFilterExpression {:id {:$exists true}})]]
    (let [edges (fixture/collection [] [index])]
      (is (re-find #"incompatible" (await (index-error edges))))
      (is (empty? (fixture/operations edges)))
      (is (= [index] @(:indexes* edges))))))

(deftest ^:async retiring-legacy-never-discards-nonstring-identities
  (doseq [id [42 nil ["array-member"]]]
    (let [row {:_id "native-nonstring" :id id}
          edges (fixture/collection [row] [fixture/primary-index fixture/legacy-index])]
      (is (re-find #"non-string legacy ids" (await (index-error edges))))
      (is (empty? (fixture/operations edges)))
      (is (= [row] (fixture/rows edges))))))

(deftest ^:async concurrent-retirement-ignores-only-index-not-found
  (doseq [error [(fixture/mongo-error 27 "other" "already retired")
                 (fixture/mongo-error 0 "IndexNotFound" "already retired")]]
    (let [edges (fixture/collection [] [fixture/legacy-index] {:drop-error error})]
      (is (nil? (await (index-error edges))))
      (is (= [[:create common/graph-edge-id-index-name] [:drop common/graph-id-index-name]]
             (fixture/operations edges)))
      (is (some #(= fixture/native-index %) @(:indexes* edges)))))
  (let [edges (fixture/collection [] [fixture/legacy-index]
                                  {:drop-error (fixture/mongo-error 13 "Unauthorized" "drop refused")})]
    (is (= "drop refused" (await (index-error edges))))
    (is (some #(= fixture/native-index %) @(:indexes* edges)))))

(deftest ^:async equivalent-legacy-partial-index-needs-no-replacement
  (let [index (assoc fixture/native-index :name common/graph-id-index-name)
        edges (fixture/collection [] [index])]
    (is (nil? (await (index-error edges))))
    (is (empty? (fixture/operations edges)))))

(defn- foreign-edge-cases []
  [(assoc fixture/sdk-edge :_id fixture/canonical-edge-id)
   (assoc fixture/sdk-edge :_id "other-sdk-primary"
          :source_node_id (:source fixture/native-edge) :target_node_id (:target fixture/native-edge)
          :edge_kind (:kind fixture/native-edge))])

(deftest ^:async foreign-primary-and-tuple-collisions-never-adopt-sdk-rows
  (doseq [incumbent (foreign-edge-cases)]
    (let [edges (fixture/collection [incumbent] [fixture/primary-index fixture/sdk-index fixture/native-index])
          graph (fixture/graph edges) result (await (approve! graph fixture/segment))]
      (is (false? (:success result)))
      (is (re-find #"E11000" (:error result)))
      (is (= [incumbent] (fixture/rows edges)))
      (is (empty? (fixture/rows (:nodes graph))))
      (let [selectors (mapv second (fixture/operations edges))]
        (is (= 2 (count selectors)))
        (is (= (first selectors) (second selectors)))
        (is (= (:id fixture/native-edge) (get-in (first selectors) [:id :$eq])))))))

(deftest ^:async failed-edge-restores-the-prior-node
  (let [incumbent (first (foreign-edge-cases))
        edges (fixture/collection [incumbent] [fixture/primary-index fixture/sdk-index fixture/native-index])
        graph (fixture/graph edges)
        prior {:_id (ObjectId.) :id "translation:en:es:segment-one" :data {:accepted "old"}}
        _ (reset! (:rows* (:nodes graph)) [prior])]
    (is (false? (:success (await (approve! graph fixture/segment)))))
    (is (= [prior] (fixture/rows (:nodes graph))))
    (is (= [incumbent] (fixture/rows edges)))))

(deftest ^:async native-objectid-identity-is-preserved-at-unchanged-endpoints
  (let [primary-id (ObjectId.) incumbent (assoc fixture/native-edge :_id primary-id)
        edges (fixture/collection [incumbent] [fixture/primary-index fixture/sdk-index fixture/native-index])
        graph (fixture/graph edges)]
    (is (= {:success true} (await (approve! graph fixture/segment))))
    (is (= {:success true} (await (approve! graph fixture/segment))))
    (is (= primary-id (:_id (first (fixture/rows edges)))))
    (is (= (:target fixture/native-edge) (:target_node_id (first (fixture/rows edges)))))
    (is (= 1 (count (fixture/rows edges))))))

(deftest ^:async incompatible-native-primary-or-present-tuple-is-not-rewritten
  (doseq [incumbent [(assoc fixture/native-edge :_id "noncanonical-string")
                     (assoc fixture/native-edge :_id (ObjectId.) :target_node_id "conflicting-sdk-target")
                     (assoc fixture/native-edge :_id (ObjectId.) :id [(:id fixture/native-edge)])
                     (assoc fixture/native-edge :_id (ObjectId.) :source [(:source fixture/native-edge)])
                     (assoc fixture/native-edge :_id (ObjectId.) :target_node_id [(:target fixture/native-edge)])]]
    (let [edges (fixture/collection [incumbent] [fixture/primary-index fixture/sdk-index fixture/native-index])
          graph (fixture/graph edges)]
      (is (false? (:success (await (approve! graph fixture/segment)))))
      (is (= [incumbent] (fixture/rows edges)))
      (is (empty? (fixture/rows (:nodes graph)))))))

(deftest ^:async existing-native-edge-cannot-silently-change-canonical-endpoint
  (let [edges (fixture/collection [] [fixture/primary-index fixture/sdk-index fixture/native-index])
        graph (fixture/graph edges)]
    (is (= {:success true} (await (approve! graph fixture/segment))))
    (let [prior-edges (fixture/rows edges) prior-nodes (fixture/rows (:nodes graph))
          result (await (approve! graph (assoc fixture/segment :source_lang "fr")))]
      (is (false? (:success result)))
      (is (= prior-edges (fixture/rows edges)))
      (is (= prior-nodes (fixture/rows (:nodes graph)))))))

(deftest ^:async every-array-alias-conflict-preserves-the-incumbent-and-rolls-back
  (doseq [aliases [["unrelated-alias" (:id fixture/native-edge)]
                   [(:id fixture/native-edge) "unrelated-alias"]
                   ["unrelated-alias" (:id fixture/native-edge) (:id fixture/native-edge)]]]
    (let [incumbent (assoc fixture/native-edge :_id (ObjectId.) :id aliases)
          edges (fixture/collection [incumbent]
                                    [fixture/primary-index fixture/sdk-index fixture/native-index])
          graph (fixture/graph edges)
          result (await (approve! graph fixture/segment))]
      (is (false? (:success result)))
      (is (re-find #"E11000" (or (:error result) "")))
      (is (= [incumbent] (fixture/rows edges)))
      (is (empty? (fixture/rows (:nodes graph))))
      (let [selectors (mapv second (fixture/operations edges))]
        (is (= 2 (count selectors)))
        (is (= (first selectors) (second selectors)))))))

(defn- ^:async alias-upsert-error [collection row]
  (try
    (await (.findOneAndUpdate (:native collection)
                             #js {"_id" (:_id row)}
                             #js {"$set" (clj->js (dissoc row :_id))
                                  "$setOnInsert" #js {"_id" (:_id row)}}
                             #js {"upsert" true}))
    nil
    (catch :default error (.-message error))))

(deftest ^:async native-alias-multikey-upserts-refuse-collisions-in-either-direction
  (doseq [[incumbent candidate]
          [[{:_id "array-owner" :id ["other" "shared"]} {:_id "scalar-owner" :id "shared"}]
           [{:_id "scalar-owner" :id "shared"} {:_id "array-owner" :id ["other" "shared"]}]
           [{:_id "array-owner" :id ["first" "shared"]}
            {:_id "other-array-owner" :id ["second" "shared"]}]]]
    (let [edges (fixture/collection [incumbent] [fixture/primary-index fixture/native-index])]
      (is (re-find #"E11000" (or (await (alias-upsert-error edges candidate)) "")))
      (is (= [incumbent] (fixture/rows edges))))))

(deftest ^:async native-alias-multikey-index-build-refuses-every-overlapping-key
  (doseq [rows [[{:_id "array-owner" :id ["other" "shared"]} {:_id "scalar-owner" :id "shared"}]
                [{:_id "scalar-owner" :id "shared"} {:_id "array-owner" :id ["other" "shared"]}]
                [{:_id "array-owner" :id ["first" "shared"]}
                 {:_id "other-array-owner" :id ["second" "shared"]}]]]
    (let [edges (fixture/collection rows [fixture/primary-index])]
      (is (re-find #"E11000" (or (await (index-error edges)) "")))
      (is (= rows (fixture/rows edges)))
      (is (= [fixture/primary-index] @(:indexes* edges))))))

(deftest ^:async native-alias-multikey-keeps-intra-document-repeats-and-excluded-rows
  (let [rows [{:_id "array-owner" :id ["first" "second" "second"]}
              {:_id "scalar-owner" :id "independent"}
              {:_id "numeric-array-one" :id [1 2]}
              {:_id "numeric-array-two" :id [1 2]}
              {:_id "numeric-one" :id 1}
              {:_id "numeric-two" :id 1}
              {:_id "missing-one"}
              {:_id "missing-two"}]
        edges (fixture/collection rows [fixture/primary-index])]
    (is (nil? (await (index-error edges))))
    (is (some #(= fixture/native-index %) @(:indexes* edges)))
    (is (nil? (await (alias-upsert-error edges {:_id "array-owner" :id ["second" "first" "second"]}))))
    (is (= (assoc (first rows) :id ["second" "first" "second"])
           (last (fixture/rows edges))))
    (is (= (rest rows) (butlast (fixture/rows edges))))))
