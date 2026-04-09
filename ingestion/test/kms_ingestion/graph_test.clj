(ns kms-ingestion.graph-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.graph :as graph]))

(deftest collect-devel-graph-events-builds-doc-and-web-relations
  (testing "docs produce local markdown links and external web links"
    (let [file-metas [{:path "docs/INDEX.md"}
                      {:path "specs/plan.md"}]
          context (graph/index-context {} file-metas)
          events (graph/collect-devel-graph-events
                  {:tenant-id "devel"
                   :source-id "source-1"
                   :context context
                   :ts "2026-04-04T20:00:00Z"
                   :file {:path "docs/INDEX.md"
                          :content "See [plan](../specs/plan.md) and <https://example.com/>"
                          :content-hash "hash-1"}})
          node-ids (->> events
                        (filter #(= "graph.node" (:kind %)))
                        (map #(get-in % [:extra :node_id]))
                        set)
          edge-types (->> events
                          (filter #(= "graph.edge" (:kind %)))
                          (map #(get-in % [:extra :edge_type]))
                          set)]
      (is (contains? node-ids "devel:file:docs/INDEX.md"))
      (is (contains? node-ids "devel:file:specs/plan.md"))
      (is (contains? node-ids "web:url:https://example.com/"))
      (is (contains? edge-types "local_markdown_link"))
      (is (contains? edge-types "external_web_link")))))

(deftest collect-devel-graph-events-builds-code-dependencies
  (testing "code files produce internal code dependency edges"
    (let [file-metas [{:path "src/app.ts"}
                      {:path "src/lib/util.ts"}
                      {:path "src/server/core.cljc"}]
          context (graph/index-context {} file-metas)
          events (graph/collect-devel-graph-events
                  {:tenant-id "devel"
                   :source-id "source-2"
                   :context context
                   :ts "2026-04-04T20:10:00Z"
                   :file {:path "src/app.ts"
                          :content "import { util } from './lib/util'\n(require \"./lib/util\")\n[server.core :as core]"
                          :content-hash "hash-2"}})
          dependency-targets (->> events
                                  (filter #(= "graph.edge" (:kind %)))
                                  (filter #(= "code_dependency" (get-in % [:extra :edge_type])))
                                  (map #(get-in % [:extra :target_node_id]))
                                  set)]
      (is (contains? dependency-targets "devel:file:src/lib/util.ts")))))
