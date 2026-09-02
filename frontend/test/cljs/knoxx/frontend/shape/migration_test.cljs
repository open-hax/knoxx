(ns knoxx.frontend.shape.migration-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.shape.migration :as shape]))

(deftest classifies-keystone-and-widget-islands
  (testing "chat owns its controller subtree"
    (is (= :chat-workspace
           (shape/classify-island
            "frontend/src/components/chat-page/useChatWorkspaceController.ts"))))
  (testing "contracts expose their real blockers"
    (let [record (shape/legacy-file-record
                  {:path "frontend/src/pages/ContractsPage.tsx"
                   :source "export default function ContractsPage() {}"
                   :tests ["frontend/src/pages/ContractsPage.test.tsx"]})]
      (is (= :contracts (:island record)))
      (is (= [:chat-workspace :codemirror-adapter] (:blocked-by record)))
      (is (= :port (:disposition record)))))
  (testing "heavy widgets are adapted rather than rewritten"
    (is (= :wrap
           (:disposition
            (shape/legacy-file-record
             {:path "frontend/src/components/GraphExplorer.tsx"
              :source "export function GraphExplorer() {}"
              :tests []}))))))

(deftest loader-shims-delete-with-their-final-consumer
  (let [record (shape/legacy-file-record
                {:path "frontend/src/pages/SettingsPage.tsx"
                 :source "const load = () => window.knoxx.frontend.pages.settings"
                 :tests ["frontend/src/pages/SettingsPage.test.tsx"]})]
    (is (= :delete (:disposition record)))
    (is (= :route (:role record)))))
