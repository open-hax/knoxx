(ns knoxx.backend.extern-tools-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.tools :as tools]))

(defn- sample-execute
  [_runtime _config _tool-call-id params]
  {:params params})

(deftest create-tool-obj-exports-runtime-js-tool-shape
  (testing "domain tool factory delegates JS runtime shape construction to extern adapter"
    (let [tool (tools/create-tool-obj "demo.tool"
                                      "Demo Tool"
                                      "Description"
                                      "Prompt snippet"
                                      ["first guideline"]
                                      [:map [:query :string]]
                                      sample-execute
                                      {:runtime true}
                                      {:config true})]
      (is (= "demo.tool" (aget tool "name")))
      (is (= "Demo Tool" (aget tool "label")))
      (is (= "Prompt snippet" (aget tool "promptSnippet")))
      (is (= "first guideline" (aget tool "promptGuidelines" 0)))
      (is (= "object" (aget tool "parameters" "type")))
      (is (fn? (aget tool "execute"))))))

(deftest maybe-tool-update-sends-runtime-content-payload
  (testing "update callbacks still receive the expected JS content array"
    (let [seen* (atom nil)]
      (tools/maybe-tool-update! #(reset! seen* %) "working")
      (is (= "text" (aget @seen* "content" 0 "type")))
      (is (= "working" (aget @seen* "content" 0 "text"))))))

(deftest custom-tool-sanitization-preserves-canonical-name
  (testing "unsafe tool names are sanitized with originalName metadata"
    (let [tool #js {:name "contract.read/v1"
                    :description "Read a contract."
                    :promptSnippet "Use contract.read/v1 carefully."
                    :promptGuidelines #js ["contract.read/v1 is for contract reads."]}
          sanitized-tools (tools/sanitize-custom-tools #js [tool])
          sanitized (aget sanitized-tools 0)]
      (is (= "contract_read_v1" (aget sanitized "name")))
      (is (= "contract.read/v1" (aget sanitized "originalName")))
      (is (re-find #"Canonical tool id: `contract.read/v1`" (aget sanitized "description")))
      (is (re-find #"canonical `contract.read/v1`" (aget sanitized "promptSnippet")))
      (is (re-find #"canonical contract.read/v1" (aget sanitized "promptGuidelines" 0))))))

(deftest custom-tool-filter-allows-runtime-or-original-name
  (testing "allow set can match sanitized runtime id or canonical original id"
    (let [tools-array #js [#js {:name "safe_one" :originalName "safe/one"}
                           #js {:name "blocked"}]
          by-original (tools/filter-custom-tools-by-allow-set tools-array #{"safe/one"})
          by-runtime (tools/filter-custom-tools-by-allow-set tools-array #{"safe_one"})]
      (is (= 1 (.-length by-original)))
      (is (= "safe_one" (aget by-original 0 "name")))
      (is (= 1 (.-length by-runtime)))
      (is (= "safe_one" (aget by-runtime 0 "name"))))))

(deftest json-parse-keeps-keywordized-tool-data
  (testing "tool JSON string params parse to CLJS keyword maps"
    (is (= {:event_kind "note" :payload {:ok true}}
           (tools/json-parse "{\"event_kind\":\"note\",\"payload\":{\"ok\":true}}")))))
