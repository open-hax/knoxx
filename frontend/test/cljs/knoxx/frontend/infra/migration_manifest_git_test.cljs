(ns knoxx.frontend.infra.migration-manifest-git-test
  (:require ["node:child_process" :as child-process]
            ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(t/deftest unreadable-git-baselines-fail-closed
  (t/is (try
          (manifest/base-manifest "definitely-not-a-git-revision")
          false
          (catch js/Error error
            (boolean (re-find #"cannot resolve the migration baseline"
                              (.-message error)))))))

(defn- fixture-git!
  "Run Git with a fixture identity inside a disposable repository."
  [root arguments]
  (child-process/execFileSync
    "git" (into-array (concat ["-C" root
                              "-c" "user.name=Migration Fixture"
                              "-c" "user.email=migration-fixture@example.invalid"
                              "-c" "commit.gpgsign=false"]
                             arguments))
    #js {:encoding "utf8" :stdio #js ["ignore" "pipe" "pipe"]}))

(t/deftest git-baselines-distinguish-absent-and-invalid-manifests
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-git-"))
        original-cwd (.cwd js/process)
        manifest-path (node-path/join root manifest/manifest-relative-path)
        valid-record {:record/id "route:\"/native\""
                      :path "frontend/src/cljs/knoxx/frontend/app.cljs"
                      :kind :route
                      :route "\"/native\""
                      :implementation "native/Page"
                      :status :native}]
    (try
      (fs/mkdirSync (node-path/dirname manifest-path) #js {:recursive true})
      (fs/writeFileSync (node-path/join root "frontend" "package.json") "{}")
      (fixture-git! root ["init" "--quiet" "--initial-branch=main"])
      (fixture-git! root ["commit" "--quiet" "--allow-empty" "-m" "Absent ledger"])
      (.chdir js/process root)
      (t/is (nil? (manifest/base-manifest "HEAD")))
      (fs/writeFileSync manifest-path " {:record/id \"one\", :kind :route}\n")
      (fixture-git! root ["add" manifest/manifest-relative-path])
      (fixture-git! root ["commit" "--quiet" "-m" "Noncanonical ledger"])
      (t/is (thrown-with-msg? js/Error #"canonical single-form EDN"
                             (manifest/base-manifest "HEAD")))
      (fs/writeFileSync manifest-path (manifest/render-records [valid-record]))
      (fixture-git! root ["add" manifest/manifest-relative-path])
      (fixture-git! root ["commit" "--quiet" "-m" "Valid ledger"])
      (t/is (= [valid-record] (manifest/base-manifest "HEAD")))
      (doseq [[description records message]
              [["Missing field" [(dissoc valid-record :status)] #"violates its contract"]
               ["Extra field" [(assoc valid-record :extra true)] #"violates its contract"]
               ["Invalid field" [(assoc valid-record :status :unknown)] #"violates its contract"]
               ["Duplicate identities" [valid-record valid-record] #"identities must be unique"]]]
        (t/testing description
          (fs/writeFileSync manifest-path (manifest/render-records records))
          (fixture-git! root ["add" manifest/manifest-relative-path])
          (fixture-git! root ["commit" "--quiet" "-m" description])
          (t/is (thrown-with-msg? js/Error message
                                 (manifest/base-manifest "HEAD")))))
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))
