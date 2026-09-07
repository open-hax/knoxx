(ns knoxx.frontend.infra.migration-manifest-git-test
  (:require ["node:child_process" :as child-process]
            ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.frontend.infra.migration-git :as git]
            [knoxx.frontend.infra.migration-manifest :as manifest]
            [knoxx.frontend.law.migration :as law]))

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

(t/deftest changed-git-paths-preserve-quoted-and-multiline-filenames
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-paths-"))
        paths ["frontend/src/pages/Écran.tsx"
               "frontend/src/pages/quoted\"name.tsx"
               "frontend/src/pages/back\\slash.tsx"
               "frontend/src/pages/line\nbreak.tsx"
               "frontend/src/cljs/tab\tand\nline.cljc"]]
    (try
      (fixture-git! root ["init" "--quiet" "--initial-branch=main"])
      (doseq [path paths]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path "initial")))
      (fixture-git! root ["add" "--all"])
      (fixture-git! root ["commit" "--quiet" "-m" "Initial sources"])
      (let [base-sha (str/trim (fixture-git! root ["rev-parse" "HEAD"]))]
        (doseq [path paths]
          (fs/writeFileSync (node-path/join root path) "changed"))
        (fixture-git! root ["add" "--all"])
        (fixture-git! root ["commit" "--quiet" "-m" "Change unusual source paths"])
        (let [changed (git/changed-paths root base-sha)]
          (t/is (= (set paths) (set changed)))
          (doseq [path changed]
            (t/testing (pr-str path)
              (t/is (law/migration-surface-path? path))
              (t/is (= [:migration-slice/must-progress]
                       (mapv :law (law/ratchet-violations
                                   {:baseline [] :current []
                                    :changed-paths [path]
                                    :infrastructure? false})))))))
        (t/is (= [] (git/changed-paths root "HEAD"))))
      (finally
        (fs/rmSync root #js {:recursive true :force true})))))

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

(t/deftest native-cljs-gains-become-the-next-floor
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-native-count-"))
        original-cwd (.cwd js/process)
        source-dir (node-path/join root "frontend/src")
        removed-path (node-path/join source-dir "file65.cljs")]
    (try
      (fs/mkdirSync source-dir #js {:recursive true})
      (fs/writeFileSync (node-path/join root "frontend/package.json") "{}")
      (doseq [index (range 66)]
        (fs/writeFileSync (node-path/join source-dir (str "file" index ".cljs")) ""))
      (doseq [path ["backend/ignored.cljs" "frontend/src/folder.cljs/readme.txt"
                    "frontend/src/shared.cljc" "frontend/src/line\nbreak.cljs"]]
        (fs/mkdirSync (node-path/dirname (node-path/join root path)) #js {:recursive true})
        (fs/writeFileSync (node-path/join root path) ""))
      (fixture-git! root ["init" "--quiet" "--initial-branch=main"])
      (fixture-git! root ["add" "--all"])
      (fixture-git! root ["commit" "--quiet" "-m" "67 native files"])
      (.chdir js/process root)
      (let [base (str/trim (fixture-git! root ["rev-parse" "HEAD"]))]
        (t/is (= {:before 67 :after 67} (manifest/native-source-counts base)))
        (fs/unlinkSync removed-path)
        (t/is (= {:before 67 :after 66} (manifest/native-source-counts base)))
        (t/is (thrown-with-msg? js/Error #"Native CLJS source count regressed"
                               (law/assert-native-source-counts! (manifest/native-source-counts base))))
        (fs/writeFileSync (node-path/join source-dir "replacement.cljs") "")
        (fs/renameSync (node-path/join source-dir "file0.cljs")
                       (node-path/join source-dir "renamed.cljs"))
        (t/is (= {:before 67 :after 67}
                 (law/assert-native-source-counts! (manifest/native-source-counts base))))
        (fs/writeFileSync (node-path/join source-dir "gain.cljs") "")
        (t/is (= {:before 67 :after 68}
                 (law/assert-native-source-counts! (manifest/native-source-counts base))))
        (fixture-git! root ["add" "--all"])
        (fixture-git! root ["commit" "--quiet" "-m" "68 native files"])
        (fs/unlinkSync (node-path/join source-dir "gain.cljs"))
        (t/is (= {:before 68 :after 67} (manifest/native-source-counts "HEAD")))
        (t/is (thrown-with-msg? js/Error #"Native CLJS source count regressed"
                               (law/assert-native-source-counts! (manifest/native-source-counts "HEAD"))))
        (t/is (thrown? js/Error (manifest/native-source-counts "missing-revision"))))
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))
