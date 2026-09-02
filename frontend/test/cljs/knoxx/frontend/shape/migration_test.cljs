(ns knoxx.frontend.shape.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.shape.migration :as shape]))

(t/deftest classifies-keystone-and-widget-islands
  (t/testing "chat owns its controller subtree"
    (t/is (= :chat-workspace
             (shape/classify-island
              "frontend/src/components/chat-page/useChatWorkspaceController.ts"))))
  (t/testing "contracts expose their real blockers"
    (let [record (shape/legacy-file-record
                  {:path "frontend/src/pages/ContractsPage.tsx"
                   :source "export default function ContractsPage() {}"
                   :tests ["frontend/src/pages/ContractsPage.test.tsx"]})]
      (t/is (= :contracts (:island record)))
      (t/is (= [:chat-workspace :codemirror-adapter] (:blocked-by record)))
      (t/is (= :port (:disposition record)))))
  (t/testing "heavy widgets are adapted rather than rewritten"
    (t/is (= :wrap
             (:disposition
              (shape/legacy-file-record
               {:path "frontend/src/components/GraphExplorer.tsx"
                :source "export function GraphExplorer() {}"
                :tests []}))))))

(t/deftest loader-shims-delete-with-their-final-consumer
  (let [record (shape/legacy-file-record
                {:path "frontend/src/pages/SettingsPage.tsx"
                 :source "const load = () => window.knoxx.frontend.pages.settings"
                 :tests ["frontend/src/pages/SettingsPage.test.tsx"]})]
    (t/is (= :delete (:disposition record)))
    (t/is (= :route (:role record)))))

(t/deftest unknown-source-layouts-fail-with-the-governed-path
  (t/is (try
          (shape/classify-island "frontend/src/new-module/Legacy.ts")
          false
          (catch js/Error error
            (boolean (re-find #"No migration island rule matches governed path"
                              (.-message error)))))))
