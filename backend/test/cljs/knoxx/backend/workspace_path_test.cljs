(ns knoxx.backend.workspace-path-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.config :as config]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.shape.path :as path-shape]))

(def test-config
  {:workspace-root "/workspace/root"
   :extra-workspace-roots ["/mnt/shared-workspace"]
   :project-name "portable-workspace"
   :session-project-name "knoxx-session"})

(deftest normalize-workspace-path-strips-configured-roots
  (with-redefs [config/cfg (fn [] test-config)]
    (testing "primary and extra workspace roots are removable without knowing the host checkout path"
      (is (= "orgs/open-hax/knoxx/backend.cljs"
             (path-shape/normalize-workspace-path "/workspace/root/orgs/open-hax/knoxx/backend.cljs,")))
      (is (= "docs/guide.md"
             (path-shape/normalize-workspace-path "/mnt/shared-workspace/docs/guide.md"))))
    (testing "paths outside configured roots do not become workspace mentions by accident"
      (is (nil? (path-shape/normalize-workspace-path "/var/tmp/other/orgs/open-hax/knoxx/backend.cljs"))))
    (testing "legacy callers receive the same normalized value through the compatibility alias"
      (is (= "src/knoxx.cljs"
             (path-shape/normalize-devel-path "/workspace/root/src/knoxx.cljs"))))))

(deftest extract-mentioned-workspace-paths-uses-configured-project-node-prefix
  (with-redefs [config/cfg (fn [] test-config)]
    (let [text "Read orgs/open-hax/knoxx/backend.cljs and docs/architecture before editing."
          expected [{:path "orgs/open-hax/knoxx/backend.cljs"
                     :target_kind "file"
                     :target_node_id "portable-workspace:file:orgs/open-hax/knoxx/backend.cljs"}
                    {:path "docs/architecture"
                     :target_kind "dir"
                     :target_node_id "portable-workspace:dir:docs/architecture"}]]
      (is (= expected (core-memory/extract-mentioned-workspace-paths text)))
      (is (= expected (core-memory/extract-mentioned-devel-paths text))))))

(deftest session-graph-events-emit-workspace-edges
  (let [events (memory/session-text-graph-events
                test-config
                (fn [_]
                  [{:path "src/knoxx.cljs"
                    :target_kind "file"
                    :target_node_id "portable-workspace:file:src/knoxx.cljs"}])
                (fn [_] ["https://example.test/reference"])
                {:run-id "run-1"
                 :conversation-id "conversation-1"
                 :session-id "session-1"
                 :ts "2026-05-26T00:00:00.000Z"
                 :node-id "msg-1"
                 :node-type "message"
                 :text "See src/knoxx.cljs and https://example.test/reference"
                 :label "assistant"
                 :model "test-model"
                 :scope-extra {:contract_id "knoxx_default"}})
        path-edge (second events)
        web-edge (nth events 2)]
    (is (= 3 (count events)))
    (is (= "mentions_workspace_path" (get-in path-edge [:extra :edge_type])))
    (is (= "knoxx-session" (get-in path-edge [:extra :source_lake])))
    (is (= "portable-workspace" (get-in path-edge [:extra :target_lake])))
    (is (= "portable-workspace:file:src/knoxx.cljs" (get-in path-edge [:extra :target_node_id])))
    (is (re-find #"mentions_workspace" (get-in path-edge [:extra :edge_id])))
    (is (= "mentions_web_url" (get-in web-edge [:extra :edge_type])))))
