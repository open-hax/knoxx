(ns knoxx.backend.tools.mcp-workspace-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mcp-workspace-tools :as workspace]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(defn- by-name [tools name]
  (some #(when (= name (aget % "name")) %) (array-seq tools)))

(deftest caller-policy-filters-workspace-tools
  (let [ctx {:toolPolicies [{:toolId "read" :effect "allow"}
                            {:toolId "write" :effect "deny"}]}
        tools (workspace/create-workspace-tools nil {:workspace-root "/tmp"} ctx)]
    (is (some? (by-name tools "read")))
    (is (nil? (by-name tools "write")))
    (is (nil? (by-name tools "bash")))))

(deftest ^:async workspace-tools-round-trip
  (let [dir (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-mcp-workspace-")))
        config {:workspace-root dir}
        ctx {:toolPolicies (mapv #(hash-map :toolId % :effect "allow") ["read" "write" "bash"])}
        tools (workspace/create-workspace-tools nil config ctx)
        invoke (fn [name params] ((aget (by-name tools name) "execute") "test" params nil nil nil))]
    (try
      (await (invoke "write" #js {:path "probe.txt" :content "original"}))
      (is (= "original" (aget (aget (await (invoke "read" #js {:path "probe.txt"})) "details") "content")))
      (let [denied? (try (await (invoke "write" #js {:path "probe.txt" :content "replacement"})) false
                         (catch :default _ true))]
        (is denied?)
        (is (= "original" (await (.readFile fs (.join path dir "probe.txt") "utf8")))))
      (await (invoke "write" #js {:path "probe.txt" :content "replacement" :overwrite true}))
      (let [result (await (invoke "bash" #js {:command "printf ok"}))]
        (is (= "ok" (aget (aget result "details") "stdout"))))
      (let [result (await (invoke "bash" #js {:command "exit 7"}))]
        (is (true? (aget result "isError"))))
      (finally (await (.rm fs dir #js {:recursive true :force true}))))))
