(ns knoxx.frontend.infra.migration-manifest-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- fixture-records
  "Read the real inventory from a synchronous, disposable repository fixture."
  [{:keys [route-source bridge-source] :or {route-source "" bridge-source ""}}]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/src/bridge/index.ts" bridge-source
               "frontend/src/bridge/app.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"@open-hax/knoxx-app-bridge\" :as app]))\n"
                    route-source)}]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (.chdir js/process root)
      (manifest/current-records)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest route-census-rejects-unparsed-invocations
  (doseq [route-source ["($ Route\n {:path \"/chat\"\n :element ($ app/ChatPage)})"
                        "($  Route {:path \"/chat\"\n :element ($ app/ChatPage)})"]]
    (t/testing route-source
      (t/is (thrown-with-msg? js/Error #"Unsupported Shadow route syntax"
                             (fixture-records {:route-source route-source}))))))

(t/deftest route-census-preserves-supported-routes-and-component-boundaries
  (let [records (fixture-records
                 {:route-source
                  (str "($ Routes {}\n"
                       " ($ Route {:path \"/native\"\n :element ($ native/Page)})\n"
                       " ($ Route {:path \"/legacy\"\n :element ($ app/ChatPage)}))\n"
                       "($ RouteGuard {})")})]
    (t/is (= #{["\"/native\"" "native/Page" :native]
               ["\"/legacy\"" "app/ChatPage" :legacy]}
             (->> records
                  (filter #(= :route (:kind %)))
                  (map (juxt :route :implementation :status))
                  set)))))

(t/deftest bridge-census-rejects-indented-unsupported-exports
  (doseq [indentation ["  " "\t"]]
    (t/testing (pr-str indentation)
      (t/is (thrown-with-msg?
              js/Error #"Unsupported bridge export syntax"
              (fixture-records
                {:bridge-source (str indentation "export const Foo = 1;\n")}))))))

(t/deftest bridge-census-preserves-indented-supported-exports
  (doseq [indentation ["  " "\t"]]
    (let [records (fixture-records
                    {:bridge-source
                     (str indentation "export { Foo } from 'fixture';\n")})]
      (t/is (= [{:bridge :frontend :symbol "Foo" :source "fixture" :status :legacy}]
               (->> records
                    (filter #(= :bridge-export (:kind %)))
                    (mapv #(select-keys % [:bridge :symbol :source :status]))))))))

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
