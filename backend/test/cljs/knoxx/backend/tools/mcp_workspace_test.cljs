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

(deftest ^:async file-read-is-bounded-and-pages-utf8
  (let [dir (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-mcp-pages-")))
        file (.join path dir "large.txt")
        input (str (apply str (repeat 19999 "a")) "🙂" "tail")
        tools (workspace/create-workspace-tools nil {:workspace-root dir}
                                                {:toolPolicies [{:toolId "read" :effect "allow"}]})
        invoke (fn [offset] ((aget (by-name tools "read") "execute") "test"
                             #js {:path "large.txt" :offset_bytes offset} nil nil nil))]
    (try
      (await (.writeFile fs file input "utf8"))
      (let [first-page (aget (await (invoke 0)) "details")
            offset (aget first-page "next_offset_bytes")
            second-page (aget (await (invoke offset)) "details")]
        (is (= 19999 offset))
        (is (true? (aget first-page "truncated")))
        (is (false? (aget second-page "truncated")))
        (is (= input (str (aget first-page "content") (aget second-page "content")))))
      ;; Invalid UTF-8 must not trap a client at offset zero.
      (await (.writeFile fs file (.alloc js/Buffer 20010 128)))
      (let [page (aget (await (invoke 0)) "details")]
        (is (= 20000 (aget page "next_offset_bytes"))))
      (finally (await (.rm fs dir #js {:recursive true :force true}))))))
