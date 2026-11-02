(ns knoxx.backend.domain.graph-query-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.graph-query :as graph]
            [knoxx.backend.law.graph-query :as law]))

(deftest query-plan-is-literal-scoped-and-bounded
  (let [plan (graph/query-plan {:q "codex.*" :lakes ["linux-audit"]
                                :nodeTypes ["audit.harness"] :k 3 :edgeLimit 0})]
    (is (= "codex\\.\\*" (:pattern plan)))
    (is (= {"project" {"$in" ["linux-audit"]}} (:scope plan)))
    (is (= 3 (:limit plan)))
    (is (zero? (:edge-limit plan)))
    (is (= "graph.node" (get (:seed-filter plan) "kind")))
    (is (= {"$in" ["audit.harness"]} (get (:seed-filter plan) "extra.node_kind")))))

(deftest malformed-requests-never-reach-mongo
  (doseq [payload [{:q " "} {:q {:$ne nil}} {:q "x" :lakes [""]}
                   {:q "x" :k 100000} {:q "x" :edgeLimit -1}]]
    (testing (str payload)
      (is (thrown? js/Error (law/request! payload))))))

(deftest graph-response-is-a-closed-bounded-subgraph
  (let [seeds [{:extra {:node_id "harness" :node_kind "audit.harness" :label "codex"}
                :project "linux-audit"}]
        neighbors [{:extra {:node_id "principal" :node_kind "audit.linux_principal"
                            :label "UID 1002" :provenance {:evidence {:id "raw-id"}}}
                    :project "linux-audit"}]
        edges [{:source_node_id "principal" :target_node_id "harness"
                :edge_kind "mapped_to_harness" :project "linux-audit"}
               {:source_node_id "foreign" :target_node_id "harness"
                :edge_kind "unhydrated" :project "other"}]
        result (graph/query-result (graph/query-plan {:q "codex" :lakes ["linux-audit"] :k 2})
                                   seeds neighbors edges)]
    (is (= "structural" (:mode result)))
    (is (= #{"harness" "principal"} (set (map :id (:nodes result)))))
    (is (= 1 (count (:edges result))))
    (is (= "mapped_to_harness" (-> result :edges first :edgeType)))
    (is (= "raw-id" (get-in result [:nodes 1 :data :provenance :evidence :id])))))
