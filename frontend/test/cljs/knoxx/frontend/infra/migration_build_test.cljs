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

(defn- package-with-scripts [scripts]
  (js/JSON.stringify (clj->js {:scripts scripts})))

(defn- bridge-configs [scripts]
  {"package.json" (package-with-scripts scripts)
   "vite.bridge.config.ts" (vite-config "build:{lib:{entry:'src/bridge/index.ts'}}")
   "vite.app-bridge.config.ts" (vite-config "build:{lib:{entry:'src/bridge/app.ts'}}")})

(defn- shadow-config [modules]
  (pr-str {:builds {:app {:target :browser
                          :js-options
                          {:resolve (into {} (map (fn [module]
                                                   [module {:target :file
                                                            :file "dist/bridge/output.es.js"}]))
                                          modules)}}}}))

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
  (t/is (= {} (inspect-config! "vite.config.ts" nil)))
  (t/is (= {} (inspect-configs! {"package.json" "{}"})))
  (t/is (= {} (inspect-configs!
               {"package.json" (package-with-scripts {"build" "shadow-cljs release app"
                                                       "dev" "shadow-cljs watch app"})}))))

(t/deftest active-package-scripts-use-the-inspected-bridge-configs
  (let [bridge "vite build --config vite.bridge.config.ts"
        app-bridge "vite build --config vite.app-bridge.config.ts"]
    (t/is (= {} (inspect-configs!
                 (bridge-configs
                   {"build:bridge" bridge "build:app-bridge" app-bridge
                    "build" (str bridge " && " app-bridge " && shadow-cljs release app")
                    "dev" (str "concurrently -k -n BRIDGE,APP_BRIDGE \"" bridge
                                " --watch\" \"" app-bridge " --watch\"")
                    "test:e2e" (str "npm --prefix e2e install && " bridge " && " app-bridge)}))))))

(t/deftest active-config-renaming-cannot-hide-a-bridge-build
  (with-configs
    (bridge-configs {"build:app-bridge" "vite build --config vite.app-bridge.config.ts"})
    (fn [root]
      (t/is (= {} (build/assert-configs! root)))
      (fs/renameSync (node-path/join root "frontend/vite.app-bridge.config.ts")
                     (node-path/join root "frontend/renamed.config.ts"))
      (fs/writeFileSync (node-path/join root "frontend/package.json")
                        (package-with-scripts
                          {"build:app-bridge" "vite build --config renamed.config.ts"}))
      (t/is (thrown-with-msg? js/Error #"Unsupported Vite migration configuration"
                             (build/assert-configs! root))))))

(t/deftest missing-active-configs-and-aggregate-replacements-fail-closed
  (doseq [scripts [{"build:bridge" "vite build --config vite.bridge.config.ts"}
                   {"build" "vite build --config renamed.config.ts && shadow-cljs release app"}
                   {"dev" "concurrently \"vite build --config renamed.config.ts --watch\""}
                   {"build:app-bridge" "node scripts/renamed-build.mjs"}
                   {"build" "vite build"}]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vite migration configuration"
            (inspect-configs! {"package.json" (package-with-scripts scripts)})))))

(t/deftest one-bridge-can-retire-with-its-active-scripts
  (t/is (= {} (inspect-configs!
               (dissoc (bridge-configs
                         {"build:bridge" "vite build --config vite.bridge.config.ts"
                          "build" "vite build --config vite.bridge.config.ts && shadow-cljs release app"})
                       "vite.app-bridge.config.ts")))))

(t/deftest shadow-bridge-consumers-prevent-wrapper-builds-from-hiding-retirement
  (doseq [module ["@open-hax/knoxx-app-bridge" "@open-hax/knoxx-frontend-bridge"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vite migration configuration"
            (inspect-configs!
              {"shadow-cljs.edn" (shadow-config [module])
               "package.json" (package-with-scripts
                                {"build" "node build-legacy.mjs && shadow-cljs release app"})
               "build-legacy.mjs"
               "import { build } from 'vite'; await build({configFile:'renamed.config.ts'});"})))))

(t/deftest active-shadow-bridges-retain-their-governed-builds
  (t/is (= {} (inspect-configs!
               (assoc (bridge-configs
                        {"build:bridge" "vite build --config vite.bridge.config.ts"
                         "build:app-bridge" "vite build --config vite.app-bridge.config.ts"})
                      "shadow-cljs.edn"
                      (shadow-config ["@open-hax/knoxx-app-bridge"
                                      "@open-hax/knoxx-frontend-bridge"]))))))

(t/deftest shadow-bridge-retirement-removes-the-corresponding-build-obligation
  (t/is (= {} (inspect-configs!
               {"shadow-cljs.edn" (shadow-config [])
                "package.json" (package-with-scripts {"build" "shadow-cljs release app"})})))
  (t/is (= {} (inspect-configs!
               (-> (bridge-configs
                     {"build:bridge" "vite build --config vite.bridge.config.ts"})
                   (dissoc "vite.app-bridge.config.ts")
                   (assoc "shadow-cljs.edn"
                          (shadow-config ["@open-hax/knoxx-frontend-bridge"])))))))

(t/deftest later-config-flags-cannot-override-the-inspected-build
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Vite migration configuration"
          (inspect-configs!
            (bridge-configs
              {"build:app-bridge"
               "vite build --config vite.app-bridge.config.ts --config renamed.config.ts"})))))

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
