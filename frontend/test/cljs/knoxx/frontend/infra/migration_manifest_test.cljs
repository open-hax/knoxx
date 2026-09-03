(ns knoxx.frontend.infra.migration-manifest-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(t/deftest file-walk-retains-in-root-file-symlinks
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-"))
        nested (node-path/join root "nested")
        source (node-path/join nested "source.ts")
        linked-source (node-path/join root "linked-source.ts")]
    (try
      (fs/mkdirSync nested)
      (fs/writeFileSync source "export const value = 1;\n")
      (fs/symlinkSync source linked-source "file")
      (t/is (= (sort [linked-source source])
               (manifest/walk-files root)))
      (finally
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest file-walk-rejects-external-and-special-file-symlinks
  (let [outer (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-"))
        root (node-path/join outer "root")
        special-root (node-path/join outer "special")
        external-source (node-path/join outer "external.ts")]
    (try
      (fs/mkdirSync root)
      (fs/mkdirSync special-root)
      (fs/writeFileSync external-source "export const external = true;\n")
      (fs/symlinkSync external-source (node-path/join root "external.ts") "file")
      (fs/symlinkSync "/dev/zero" (node-path/join special-root "blocked.ts") "file")
      (t/is (try
              (manifest/walk-files root)
              false
              (catch js/Error error
                (boolean (re-find #"Unsafe symbolic link" (.-message error))))))
      (t/is (try
              (manifest/walk-files special-root)
              false
              (catch js/Error error
                (boolean (re-find #"Unsafe symbolic link" (.-message error))))))
      (finally
        (fs/rmSync outer #js {:recursive true :force true})))))

(t/deftest route-ownership-follows-the-declared-bridge-alias
  (let [source "(ns example (:require [\"@open-hax/knoxx-app-bridge\" :as legacy-app]))"
        bridge-alias (manifest/app-bridge-alias source)]
    (t/is (= "legacy-app" bridge-alias))
    (t/is (manifest/bridge-owned-implementation? bridge-alias
                                                 "legacy-app/ChatPage"))
    (t/is (not (manifest/bridge-owned-implementation? bridge-alias
                                                      "app/ChatPage")))))

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

(t/deftest unreadable-git-baselines-fail-closed
  (t/is (try
          (manifest/base-manifest "definitely-not-a-git-revision")
          false
          (catch js/Error error
            (boolean (re-find #"cannot resolve the migration baseline"
                              (.-message error)))))))
