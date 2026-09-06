(ns knoxx.frontend.infra.migration-build-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-build :as build]
            [knoxx.frontend.infra.migration-imports :as imports]))

(defn- with-configs [configs inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-vite-migration-"))]
    (try
      (fs/mkdirSync (node-path/join root "frontend/src/bridge") #js {:recursive true})
      (doseq [[config-name source] configs :when source]
        (fs/writeFileSync (node-path/join root "frontend" config-name) source))
      (inspect root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(defn- inspect-configs! [configs]
  (with-configs configs
    (fn [root]
      (into {} (map (fn [[alias-name target]] [alias-name (node-path/relative root target)]))
            (build/assert-configs! root)))))

(defn- inspect-config! [config-name source]
  (inspect-configs! {config-name source}))

(defn- vite-config [properties]
  (str "import { defineConfig } from 'vite'; import path from 'node:path';\n"
       "export default defineConfig({" properties "});"))

(defn- resolve-alias! [specifier]
  (with-configs
    {"vite.config.ts" (vite-config (str "resolve:{alias:{'@':path.resolve(__dirname,'src'),"
                                        "'@page':path.resolve(__dirname,'src/page.ts')}}"))}
    (fn [root]
      (fs/writeFileSync (node-path/join root "frontend/src/page.ts") "export const Page = 1;")
      (let [resolution (imports/resolver root (build/assert-configs! root))
            source (node-path/join root "frontend/src/bridge/app.ts")]
        (some->> (imports/resolve-target resolution source specifier)
                 (node-path/relative root))))))

(t/deftest missing-vite-configs-allow-retired-builds
  (t/is (= {} (inspect-config! "vite.config.ts" nil))))

(t/deftest bridge-entry-must-match-the-governed-inventory
  (doseq [[config-name expected] [["vite.app-bridge.config.ts" "app"]
                                 ["vite.bridge.config.ts" "index"]]]
    (t/is (= {} (inspect-config!
                  config-name
                  (vite-config (str "build:{lib:{entry:path.resolve(__dirname,'src/bridge/"
                                    expected ".ts')}}"))))))
  (doseq [entry ["path.resolve(__dirname,'src/bridge/app.js')"
                 "path.resolve(__dirname,'src/renamed.ts')"
                 "'src/bridge/app.js'"]]
    (t/is (thrown-with-msg?
            js/Error #"Vite bridge entry leaves governed inventory"
            (inspect-config! "vite.app-bridge.config.ts"
                             (vite-config (str "build:{lib:{entry:" entry "}}")))))))

(t/deftest rollup-input-cannot-replace-the-governed-bridge-entry
  (t/is (thrown-with-msg?
          js/Error #"Vite bridge entry leaves governed inventory"
          (inspect-config!
            "vite.app-bridge.config.ts"
            (vite-config (str "build:{lib:{entry:'src/bridge/app.ts'},"
                              "rollupOptions:{input:'src/other.ts'}}"))))))

(t/deftest vite-aliases-keep-source-within-the-governed-tree
  (doseq [alias ["{'@':path.resolve(__dirname,'src')}"
                 "[{find:'@',replacement:path.resolve(__dirname,'src')}]" ]]
    (t/is (= {"@" "frontend/src"}
             (inspect-config! "vite.config.ts"
                              (vite-config (str "resolve:{alias:" alias "}"))))))
  (doseq [alias ["{'@legacy':path.resolve(__dirname,'../legacy')}"
                 "[{find:'@legacy',replacement:path.resolve(__dirname,'../legacy')}]"
                 "{'@legacy':'/outside/source'}"]]
    (t/is (thrown-with-msg?
            js/Error #"Vite alias leaves governed frontend source tree"
            (inspect-config! "vite.config.ts"
                             (vite-config (str "resolve:{alias:" alias "}")))))))

(t/deftest vite-alias-resolution-preserves-the-actual-typescript-source
  (doseq [specifier ["@/page" "@page"]]
    (t/is (= "frontend/src/page.ts" (resolve-alias! specifier)))))

(t/deftest vite-alias-suffixes-cannot-leave-the-governed-tree
  (t/is (thrown-with-msg?
          js/Error #"Local import leaves governed frontend source tree"
          (resolve-alias! "@/../../legacy/Page"))))

(t/deftest unsupported-vite-configuration-fails-closed
  (doseq [properties ["...extra"
                      "resolve:settings"
                      "resolve:{alias:aliases}"
                      "resolve:{alias:{...aliases}}"
                      "resolve:{alias:[{find:/^@/,replacement:'/outside'}]}"
                      "resolve:{alias:[{find:'@',replacement:'/outside',customResolver:resolver}]}"
                      "resolve:{alias:{'@':process.env.SOURCE}}"
                      "resolve:{alias:{'':path.resolve(__dirname,'src')}}"
                      "resolve:{alias:[{find:'',replacement:path.resolve(__dirname,'src')}]}"
                      "build:{lib:{entry:entry}}"
                      "build:{lib:{entry:['src/bridge/app.ts']}}"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vite migration configuration"
            (inspect-config! "vite.app-bridge.config.ts" (vite-config properties)))))
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Vite migration configuration"
          (inspect-config! "vite.config.ts" "export default () => ({resolve:{}});"))))

(t/deftest vite-configs-must-agree-on-alias-targets
  (let [app-config (vite-config
                     (str "build:{lib:{entry:'src/bridge/app.ts'}},"
                          "resolve:{alias:{'@':path.resolve(__dirname,'src')}}"))]
    (t/is (= {"@" "frontend/src"}
             (inspect-configs!
               {"vite.app-bridge.config.ts" app-config
                "vite.config.ts" (vite-config "resolve:{alias:{'@':path.resolve(__dirname,'src')}}")})))
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vite migration configuration"
            (inspect-configs!
              {"vite.app-bridge.config.ts" app-config
               "vite.config.ts" (vite-config "resolve:{alias:{'@':path.resolve(__dirname,'src/bridge')}}")})))))
