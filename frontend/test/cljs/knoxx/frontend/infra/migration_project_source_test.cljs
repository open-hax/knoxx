(ns knoxx.frontend.infra.migration-project-source-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- configured-wrapper-routes [wrapper-source]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-configured-routes-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/shadow-cljs.edn" "{:source-paths [\"src/cljs\" \"route-wrappers\"]}"
               "frontend/src/bridge/index.ts" ""
               "frontend/src/bridge/app.ts"
               "export { ChatPage } from '../pages/ChatPage'; export { ChatWorkspacePane } from '../components/ChatWorkspacePane';"
               "frontend/src/pages/ChatPage.ts" "export const ChatPage = () => null;"
               "frontend/src/components/ChatWorkspacePane.ts" "export const ChatWorkspacePane = () => null;"
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"@open-hax/knoxx-app-bridge\" :as app] [fixture.wrapper :as wrapper]))\n"
                    "($ Route {:path \"/chat\"\n :element ($ wrapper/Page)})")
               "frontend/route-wrappers/fixture/wrapper.cljs"
               (str "(ns fixture.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy]))\n" wrapper-source)}]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (.chdir js/process root)
      (->> (manifest/current-records) (filter #(= :route (:kind %)))
           (mapv #(select-keys % [:route :implementation :status])))
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest configured-source-roots-preserve-legacy-wrapper-ownership
  (t/is (thrown-with-msg? js/Error #"Unsupported Shadow route implementation"
                          (configured-wrapper-routes "(defnc Page [] ($ legacy/ChatPage))"))))

(t/deftest configured-source-roots-cannot-hide-route-declarations
  (t/is (thrown-with-msg? js/Error #"Unsupported Shadow route location"
                          (configured-wrapper-routes
                            "($ Route {:path \"/hidden\" :element ($ legacy/ChatPage)})"))))

(t/deftest configured-source-roots-preserve-native-widget-composition
  (t/is (= [{:route "\"/chat\"" :implementation "wrapper/Page" :status :native}]
           (configured-wrapper-routes "(defnc Page [] ($ legacy/ChatWorkspacePane))"))))
