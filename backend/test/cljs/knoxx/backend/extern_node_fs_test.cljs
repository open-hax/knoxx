(ns knoxx.backend.extern-node-fs-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.node-fs :as node-fs]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(def ^:private private-mode 448)

(defn- fixture-paths
  []
  (let [root (.join path (.tmpdir os)
                    (str "knoxx-extern-node-fs-" (.randomUUID crypto)))]
    {:root root
     :metadata-path (.join path root "sandbox.json")}))

(deftest ^:async sandbox-metadata-lifecycle-test
  (let [{:keys [root metadata-path]} (fixture-paths)
        metadata {:sandboxId "550e8400-e29b-41d4-a716-446655440000"
                  :expiresAt 42}]
    (try
      (testing "write creates a private directory and round-trips CLJS data"
        (is (= metadata
               (await (node-fs/write-sandbox-metadata!
                       root metadata-path private-mode metadata))))
        (is (= metadata (await (node-fs/read-sandbox-metadata! metadata-path))))
        (let [^js stat (await (.stat fs root))]
          (is (= private-mode (bit-and (aget stat "mode") 511)))))

      (testing "malformed and missing metadata are bounded nil reads"
        (await (.writeFile fs metadata-path "{" "utf8"))
        (is (nil? (await (node-fs/read-sandbox-metadata! metadata-path))))
        (await (node-fs/remove-sandbox-metadata! metadata-path))
        (is (nil? (await (node-fs/read-sandbox-metadata! metadata-path)))))

      (testing "metadata and directory cleanup are idempotent"
        (await (node-fs/remove-sandbox-metadata! metadata-path))
        (await (node-fs/remove-sandbox-directory! root))
        (await (node-fs/remove-sandbox-directory! root))
        (is (nil? (await (node-fs/read-sandbox-metadata! metadata-path)))))
      (finally
        (await (node-fs/remove-sandbox-directory! root))))))
