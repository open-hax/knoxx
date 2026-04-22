(ns knoxx.backend.tooling-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.models :as models]
            [knoxx.backend.tooling :as tooling]))

(def test-config
  (models/enrich-config {:contracts-dir "contracts"
                         :workspace-root "/tmp/knoxx-test-workspace"
                         :proxx-base-url "http://127.0.0.1:8787"
                         :proxx-default-model "glm-5"}))

(deftest allowed-tool-id-set-follows-agent-contract-instead-of-full-runtime
  (testing "contract librarian is limited to its contract tools and loses general write/bash access"
    (let [tool-ids (tooling/allowed-tool-id-set test-config "contract_librarian" nil "contract_librarian" "contract_librarian")]
      (is (contains? tool-ids "read"))
      (is (contains? tool-ids "contract.write"))
      (is (contains? tool-ids "memory_search"))
      (is (not (contains? tool-ids "write")))
      (is (not (contains? tool-ids "edit")))
      (is (not (contains? tool-ids "bash")))
      (is (not (contains? tool-ids "discord.publish"))))))

(deftest create-runtime-tools-only-installs-builtins-allowed-by-contract
  (testing "manual chat agents no longer receive unrestricted write/edit/bash builtins by default"
    (let [runtime #js {:sdk #js {:createReadTool (fn [_cwd] #js {:name "read"})
                                 :createWriteTool (fn [_cwd] #js {:name "write"})
                                 :createEditTool (fn [_cwd] #js {:name "edit"})
                                 :createBashTool (fn [_cwd] #js {:name "bash"})}}
          tool-names (->> (tooling/create-runtime-tools runtime test-config nil "contract_librarian" "contract_librarian" "contract_librarian")
                          (map #(aget % "name"))
                          set)]
      (is (= #{"read"} tool-names)))))