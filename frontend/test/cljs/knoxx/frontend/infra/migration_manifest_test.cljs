(ns knoxx.frontend.infra.migration-manifest-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(t/deftest file-walk-does-not-follow-symbolic-links
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-"))
        nested (node-path/join root "nested")
        source (node-path/join nested "source.ts")
        linked-source (node-path/join root "linked-source.ts")]
    (try
      (fs/mkdirSync nested)
      (fs/writeFileSync source "export const value = 1;\n")
      (fs/symlinkSync root (node-path/join root "cycle") "dir")
      (fs/symlinkSync source linked-source "file")
      (t/is (= (sort [linked-source source])
               (manifest/walk-files root)))
      (finally
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest newline-edn-admits-exactly-one-canonical-form-per-line
  (let [line "{:record/id \"one\", :kind :route}"]
    (t/is (= [{:record/id "one" :kind :route}]
             (manifest/parse-records (str line "\n"))))
    (t/is (try
            (manifest/parse-records (str line " {:trailing true}\n"))
            false
            (catch js/Error error
              (boolean (re-find #"canonical single-form EDN"
                                (.-message error))))))))

(t/deftest repository-inventory-round-trips-through-the-checked-in-ledger
  (let [generated (manifest/current-records)
        committed (manifest/parse-records (manifest/read-manifest))
        git-head (manifest/base-manifest "HEAD")]
    (t/is (= committed generated)
          "filesystem discovery, bridge/route parsing, and assembly match the ledger")
    (t/is (= committed git-head)
          "the Git adapter retrieves the exact committed ND-EDN records")
    (t/is (empty? (manifest/changed-paths "HEAD"))
          "the Git diff adapter reports no paths against the same revision")))
