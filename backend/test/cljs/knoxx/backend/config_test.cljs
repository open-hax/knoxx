(ns knoxx.backend.config-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.config :as config]))

(def ^:private missing-env-value (js-obj))

(defn- with-env!
  [bindings f]
  (let [env (.-env js/process)
        previous (into {}
                       (map (fn [[k _]]
                              [k (let [value (aget env k)]
                                   (if (some? value) value missing-env-value))]))
                       bindings)]
    (doseq [[k v] bindings]
      (if (nil? v)
        (js-delete env k)
        (aset env k v)))
    (try
      (f)
      (finally
        (doseq [[k old-value] previous]
          (if (identical? old-value missing-env-value)
            (js-delete env k)
            (aset env k old-value)))))))

(deftest cfg-defaults-use-workspace-neutral-names
  (with-env! {"WORKSPACE_ROOT" nil
              "WORKSPACE_PATH" nil
              "KNOXX_WORKSPACE_ROOT" nil
              "WORKSPACE_PROJECT_NAME" nil
              "KNOXX_WORKSPACE_PROJECT" nil
              "KNOXX_COLLECTION_NAME" nil
              "KNOXX_OPENPLANNER_PROJECT" nil
              "KNOXX_AGENT_SYSTEM_PROMPT" nil}
    (fn []
      (let [cfg (config/cfg)]
        (testing "filesystem and collection defaults do not depend on a developer checkout name"
          (is (= "/app/workspace" (:workspace-root cfg)))
          (is (= "workspace" (:project-name cfg)))
          (is (= "workspace_docs" (:collection-name cfg)))
          (is (= "workspace" (:openplanner-mcp-project cfg))))
        (testing "the default agent prompt describes the active workspace instead of a local corpus"
          (is (str/includes? (:agent-system-prompt cfg) "active workspace corpus"))
          (is (not (str/includes? (:agent-system-prompt cfg) (str "devel" " corpus")))))))))

(deftest cfg-prefers-first-nonblank-workspace-env-alias
  (with-env! {"WORKSPACE_ROOT" ""
              "WORKSPACE_PATH" "/workspace/from-workspace-path"
              "KNOXX_WORKSPACE_ROOT" "/workspace/from-knoxx-root"
              "WORKSPACE_PROJECT_NAME" ""
              "KNOXX_WORKSPACE_PROJECT" "portable-project"
              "KNOXX_OPENPLANNER_PROJECT" ""}
    (fn []
      (let [cfg (config/cfg)]
        (is (= "/workspace/from-workspace-path" (:workspace-root cfg)))
        (is (= "portable-project" (:project-name cfg)))
        (is (= "portable-project" (:openplanner-mcp-project cfg)))))))

(deftest cfg-openplanner-project-override-wins-over-workspace-project
  (with-env! {"WORKSPACE_PROJECT_NAME" "workspace-project"
              "KNOXX_WORKSPACE_PROJECT" "fallback-project"
              "KNOXX_OPENPLANNER_PROJECT" "openplanner-project"}
    (fn []
      (is (= "openplanner-project" (:openplanner-mcp-project (config/cfg)))))))
