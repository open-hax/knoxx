(ns knoxx.frontend.infra.migration-manifest-write-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]
            [knoxx.frontend.shape.migration :as shape]))

(defn- route-record [route]
  (shape/route-record {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
                      :route (pr-str route) :implementation "native/Page" :legacy? false}))

(defn- with-ledger [initial inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-manifest-write-"))
        original-cwd (.cwd js/process)
        path (node-path/join root manifest/manifest-relative-path)]
    (try
      (fs/mkdirSync (node-path/join root "frontend"))
      (fs/writeFileSync (node-path/join root "frontend/package.json") "{}")
      (when initial
        (fs/mkdirSync (node-path/dirname path) #js {:recursive true})
        (fs/writeFileSync path initial))
      (.chdir js/process root)
      (inspect path)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest canonical-valid-ledgers-can-replace-existing-bytes
  (let [text (manifest/render-records [(route-record "/a") (route-record "/b")])]
    (with-ledger "old ledger" (fn [_path]
                               (manifest/write-manifest! text)
                               (t/is (= text (manifest/read-manifest)))))))

(t/deftest invalid-writes-preserve-the-existing-ledger
  (let [record (route-record "/a")
        line (pr-str record)]
    (doseq [text ["{" "{}\n"
                  (manifest/render-records [record record])
                  (str " " line "\n") line (str line "\n\n")
                  (manifest/render-records [(route-record "/b") record])]]
      (with-ledger "original bytes"
        (fn [_path]
          (t/is (thrown? js/Error (manifest/write-manifest! text)))
          (t/is (= "original bytes" (manifest/read-manifest))))))))

(t/deftest invalid-writes-do-not-create-the-ledger-directory
  (with-ledger nil
    (fn [path]
      (t/is (thrown? js/Error (manifest/write-manifest! "{}\n")))
      (t/is (not (fs/existsSync (node-path/dirname path)))))))

(t/deftest complete-retirement-can-write-the-canonical-empty-ledger
  (with-ledger "old ledger"
    (fn [_path]
      (manifest/write-manifest! "\n")
      (t/is (= "\n" (manifest/read-manifest))))))
