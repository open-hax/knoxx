(ns knoxx.backend.e2e.workspace-tools-e2e
  "Direct workspace tools through the actual MCP transport, in a disposable root."
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(deftest ^:async direct-workspace-round-trip
  (let [dir (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-mcp-e2e-")))
        original-config harness/e2e-config
        started (with-redefs [harness/e2e-config #(assoc (original-config) :workspace-root dir :extra-workspace-roots [] :music-library-root nil)]
                  (await (harness/start!)))]
    (try
      (let [client (harness/client started)
            _ (await (mcp/initialize! client))
            write (await (mcp/call-tool! client "write" {:path "probe.txt" :content "hello"}))
            read (await (mcp/call-tool! client "read" {:path "probe.txt"}))
            bash (await (mcp/call-tool! client "bash" {:command "cat probe.txt"}))
            failed (await (mcp/call-tool! client "bash" {:command "exit 7"}))]
        (is (= :ok (:status (mcp/call-outcome write))))
        (is (= "hello" (:detail (mcp/call-outcome read))))
        (is (= "hello" (:detail (mcp/call-outcome bash))))
        (is (= :tool-error (:status (mcp/call-outcome failed))))
        (is (= "hello" (await (.readFile fs (.join path dir "probe.txt") "utf8"))))
        (await (.writeFile fs (.join path dir "long.txt") (str (apply str (repeat 19999 "a")) "🙂tail") "utf8"))
        (let [first-page (mcp/call-outcome (await (mcp/call-tool! client "read" {:path "long.txt"})))
              next-page (mcp/call-outcome (await (mcp/call-tool! client "read" {:path "long.txt" :offset_bytes 19999})))]
          (is (str/includes? (:detail first-page) "offset_bytes=19999"))
          (is (= "🙂tail" (:detail next-page)))))
      (finally
        (await (harness/stop! started))
        (await (.rm fs dir #js {:recursive true :force true}))))))

(deftest ^:async denied-workspace-tools-are-not-exposed
  (let [started (await (harness/start! (harness/policy-context
                                       (harness/context-with-tool-policies ["read"]))))]
    (try
      (let [client (harness/client started)
            _ (await (mcp/initialize! client))
            listed (await (mcp/list-tools! client))
            names (set (map :name (get-in listed [:result :tools])))
            denied (await (mcp/call-tool! client "bash" {:command "printf denied"}))]
        (is (contains? names "read"))
        (is (not (contains? names "write")))
        (is (not (contains? names "bash")))
        (is (not= :ok (:status (mcp/call-outcome denied)))))
      (finally (await (harness/stop! started))))))
