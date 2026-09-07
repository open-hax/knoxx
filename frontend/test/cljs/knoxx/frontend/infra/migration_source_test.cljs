(ns knoxx.frontend.infra.migration-source-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- with-source-fixture [inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-source-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/tsconfig.json"
               "{\"compilerOptions\":{\"allowJs\":true,\"moduleResolution\":\"bundler\"},\"include\":[\"src\"]}"
               "frontend/src/bridge/app.ts" "export { ChatPage } from '../pages/ChatPage';"
               "frontend/src/pages/ChatPage.tsx" "export const ChatPage = () => null;"
               "frontend/src/cljs/knoxx/frontend/app.cljs" "(ns fixture)"}]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (.chdir js/process root)
      (inspect root)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest javascript-renaming-cannot-count-as-typescript-retirement
  (doseq [extension ["js" "jsx" "mjs" "cjs"]]
    (t/testing extension
      (with-source-fixture
        (fn [root]
          (t/is (some #(= "frontend/src/pages/ChatPage.tsx" (:path %))
                      (manifest/current-records)))
          (let [renamed (str "frontend/src/pages/ChatPage." extension)]
            (fs/renameSync (node-path/join root "frontend/src/pages/ChatPage.tsx")
                           (node-path/join root renamed))
            (fs/writeFileSync (node-path/join root "frontend/src/bridge/app.ts")
                              (str "export { ChatPage } from '../pages/ChatPage." extension "';"))
            (t/is (thrown-with-msg?
                    js/Error #"Ungoverned JavaScript source in migration source tree"
                    (manifest/current-records)))))))))

(t/deftest source-assets-and-governed-typescript-remain-supported
  (with-source-fixture
    (fn [root]
      (doseq [path ["frontend/src/pages/style.css"
                    "frontend/src/pages/data.json"
                    "frontend/src/pages/notes.js.txt"
                    "frontend/src/pages/types.d.ts"]]
        (fs/writeFileSync (node-path/join root path) ""))
      (t/is (= #{"frontend/src/bridge/app.ts" "frontend/src/pages/ChatPage.tsx"
                  "frontend/src/pages/types.d.ts"}
               (->> (manifest/current-records)
                    (filter #(contains? #{:ts :tsx} (:kind %)))
                    (map :path)
                    set))))))
