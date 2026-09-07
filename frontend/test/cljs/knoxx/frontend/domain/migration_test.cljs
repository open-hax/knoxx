(ns knoxx.frontend.domain.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.domain.migration :as migration]))

(t/deftest route-ownership-prioritizes-live-legacy-dependencies-over-native-wrappers
  (let [wrapper {:name "react/Fragment" :namespace "react"}
        legacy {:name "app/ChatPage" :namespace "app"}
        facts {:bridge-alias "app" :live-references [wrapper legacy]
               :rendered-components [wrapper]}]
    (t/is (= "app/ChatPage" (migration/route-implementation facts)))
    (t/is (= "react/Fragment"
             (migration/route-implementation (assoc facts :live-references [wrapper]))))))

(t/deftest retired-bridges-and-native-controls-retain-their-ownership-fallbacks
  (t/is (= "native/Page"
           (migration/route-implementation
             {:bridge-alias nil :live-references [{:name "app/ChatPage" :namespace "app"}]
              :rendered-components [{:name "native/Page" :namespace "native"}]})))
  (t/is (= "Navigate"
           (migration/route-implementation
             {:bridge-alias "app" :live-references []
              :rendered-components [{:name "Navigate" :namespace nil}]}))))

(t/deftest trusted-local-components-retain-their-reachable-legacy-ownership
  (doseq [component ["LegacyOpsRedirect" "Navigate" "PlaceholderPage" "ProtectedSurface"]]
    (let [reference {:name component :namespace nil}
          legacy {:name "app/ChatPage" :namespace "app"}]
      (t/is (= "app/ChatPage"
               (migration/route-implementation
                 {:bridge-alias "app"
                  :live-references [reference]
                  :rendered-components [reference {:name "native/Page" :namespace "native"}]
                  :local-definitions {component [legacy]}}))))))

(t/deftest local-component-dependencies-follow-aliases-and-terminate-cycles
  (let [component {:name "LegacyOpsRedirect" :namespace nil}
        alias-reference {:name "page-alias" :namespace nil}
        native {:name "react/Fragment" :namespace "react"}
        legacy {:name "app/ChatPage" :namespace "app"}
        facts {:bridge-alias "app"
               :live-references [component]
               :rendered-components [component]
               :local-definitions {"LegacyOpsRedirect" [native alias-reference]
                                   "page-alias" [component legacy]}}]
    (t/is (= "app/ChatPage" (migration/route-implementation facts)))
    (t/is (= "LegacyOpsRedirect"
             (migration/route-implementation
               (assoc-in facts [:local-definitions "page-alias"] [component native]))))))

(t/deftest native-controls-ignore-unreachable-local-legacy-definitions
  (let [component {:name "LegacyOpsRedirect" :namespace nil}]
    (t/is (= "LegacyOpsRedirect"
             (migration/route-implementation
               {:bridge-alias "app" :live-references [component]
                :rendered-components [component]
                :local-definitions
                {"LegacyOpsRedirect" [{:name "Navigate" :namespace nil}]
                 "Navigate" [{:name "rr/Navigate" :namespace "rr"}]
                 "UnusedPage" [{:name "app/ChatPage" :namespace "app"}]}})))))

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
