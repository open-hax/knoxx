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
