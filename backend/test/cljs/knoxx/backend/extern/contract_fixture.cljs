(ns knoxx.backend.extern.contract-fixture
  "Test-only extern adapter for contract fixture files.

   Raw Node filesystem/path values are born and consumed here. Callers receive
   only Clojure data and UTF-8 strings."
  (:require ["node:fs" :as node-fs]
            ["node:path" :as node-path]))

(defn read-contract
  [relative-path]
  (let [file-path (.join node-path ".." "contracts" relative-path)]
    {:path file-path
     :content (.readFileSync node-fs file-path "utf8")}))
