(ns knoxx.frontend.infra.migration-manifest-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.frontend.infra.migration-manifest :as manifest]
            ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]))

(deftest file-walk-does-not-follow-symbolic-links
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-"))
        nested (node-path/join root "nested")
        source (node-path/join nested "source.ts")]
    (try
      (fs/mkdirSync nested)
      (fs/writeFileSync source "export const value = 1;\n")
      (fs/symlinkSync root (node-path/join root "cycle") "dir")
      (is (= [source] (vec (manifest/walk-files root))))
      (finally
        (fs/rmSync root #js {:recursive true :force true})))))

(deftest newline-edn-admits-exactly-one-canonical-form-per-line
  (let [line "{:record/id \"one\", :kind :route}"]
    (is (= [{:record/id "one" :kind :route}]
           (manifest/parse-records (str line "\n"))))
    (is (thrown-with-msg?
         js/Error
         #"canonical single-form EDN"
         (manifest/parse-records (str line " {:trailing true}\n"))))))

(deftest repository-inventory-round-trips-through-the-checked-in-ledger
  (let [generated (manifest/current-records)
        committed (manifest/parse-records (manifest/read-manifest))
        git-head (manifest/base-manifest "HEAD")]
    (is (= committed generated)
        "filesystem discovery, bridge/route parsing, and assembly match the ledger")
    (is (= committed git-head)
        "the Git adapter retrieves the exact committed ND-EDN records")
    (is (empty? (manifest/changed-paths "HEAD"))
        "the Git diff adapter reports no paths against the same revision")))
