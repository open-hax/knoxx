(ns knoxx.frontend.domain.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.domain.migration :as migration]))

(t/deftest chooses-terminal-actions-from-migration-semantics
  (t/is (= :delete
           (migration/file-disposition
            "frontend/src/bridge/index.ts"
            "export {}")))
  (t/is (= :delete
           (migration/file-disposition
            "frontend/src/pages/SettingsPage.tsx"
            "window.knoxx.frontend.pages.settings")))
  (t/is (= :wrap
           (migration/file-disposition
            "frontend/src/components/GraphExplorer.tsx"
            "export function GraphExplorer() {}")))
  (t/is (= :port
           (migration/file-disposition
            "frontend/src/pages/OrdinaryPage.tsx"
            "export function OrdinaryPage() {}"))))

(t/deftest assigns-island-ownership-and-dependencies
  (t/is (= :chat-workspace
           (migration/classify-island
            "frontend/src/components/chat-page/useChatWorkspaceController.ts")))
  (t/is (= [:chat-workspace :codemirror-adapter]
           (get migration/island-blockers :contracts)))
  (t/is (try
          (migration/classify-island "frontend/src/new-module/Legacy.ts")
          false
          (catch js/Error error
            (boolean (re-find #"No migration island rule matches governed path"
                              (.-message error)))))))

(t/deftest assembled-route-ownership-follows-the-declared-dotted-bridge-alias
  (let [route-facts (mapv
                     (fn [[route implementation]]
                       {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
                        :route route
                        :implementation implementation
                        :bridge-alias "legacy.app"})
                     [["routes/legacy" "legacy.app/LegacyPage"]
                      ["routes/native" "native/Page"]
                      ["routes/prefix-lookalike" "legacy.application/Page"]
                      ["routes/unbound-alias" "app/Page"]])
        records (migration/assemble-records
                  {:sources [] :bridge-exports [] :routes route-facts})]
    (t/is (= {"legacy.app/LegacyPage" :legacy
               "native/Page" :native
               "legacy.application/Page" :native
               "app/Page" :native}
             (into {} (map (juxt :implementation :status)) records)))))

(t/deftest assembly-classifies-file-roles-and-associates-legacy-tests
  (doseq [[source-suffix test-suffix]
          [["ts" "test.ts"] ["tsx" "test.tsx"]
           ["mts" "test.mts"] ["cts" "spec.cts"] ["mts" "spec.cts"]]]
    (let [page-path (str "frontend/src/pages/OrdinaryPage." source-suffix)
          test-path (str "frontend/src/pages/OrdinaryPage." test-suffix)
          records (migration/assemble-records
                    {:sources [{:path page-path :source ""}
                               {:path test-path :source ""}]
                     :bridge-exports [] :routes []})
          file-records (filter :role records)]
      (t/is (= {page-path :route test-path :test}
               (into {} (map (juxt :path :role)) file-records)))
      (t/is (= [test-path]
               (:tests (first (filter #(= page-path (:path %)) file-records)))))
      (t/is (= [test-path]
               (mapv :path (filter #(= :legacy-test-suite (:kind %)) records)))))))
