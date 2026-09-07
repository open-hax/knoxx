(ns knoxx.frontend.infra.migration-config-execution-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-build :as build]
            [knoxx.frontend.infra.migration-vitest :as vitest]))

(def ^:private profiles
  {:vite {:file "vite.config.ts" :module "vite" :properties "" :scripts {}}
   :vitest {:file "vitest.config.ts" :module "vitest/config"
            :properties "test:{include:['src/**/*.test.{ts,tsx}']},"
            :scripts {"test" "NODE_ENV=test vitest run --config vitest.config.ts"}}})

(defn- with-fixture [profile {:keys [before properties config-file extra-files]} inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-config-execution-"))
        settings (get profiles profile)
        config (str "import { defineConfig } from '" (:module settings) "';\n"
                    before "\nexport default defineConfig({"
                    (:properties settings) properties "});")
        files (merge {"package.json" (js/JSON.stringify (clj->js {:scripts (:scripts settings)}))
                      "src/bridge/index.ts" "" "src/bridge/app.ts" ""
                      "src/test/helper.test.ts" ""
                      "legacy/test-source.ts" "test('restored legacy test', () => {});"
                      "restore-test.ts" (str "import fs from 'node:fs';"
                                             "fs.copyFileSync('legacy/test-source.ts',"
                                             "'src/test/helper.test.ts'); export default true;")
                      (or config-file (:file settings)) config}
                     extra-files)]
    (try
      (doseq [[path source] files]
        (let [absolute (node-path/join root "frontend" path)]
          (fs/mkdirSync (node-path/dirname absolute) #js {:recursive true})
          (fs/writeFileSync absolute source)))
      (inspect root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(defn- inspect-profile! [profile options]
  (with-fixture profile options (if (= profile :vite) build/assert-configs! vitest/assert-config!)))

(t/deftest current-static-configurations-remain-admissible
  (t/is (= {} (inspect-profile! :vite {})))
  (t/is (nil? (inspect-profile! :vitest {}))))

(t/deftest top-level-execution-cannot-restore-relocated-source
  (doseq [profile [:vite :vitest]
          before ["import fs from 'node:fs'; fs.copyFileSync('legacy/test-source.ts','src/test/helper.test.ts');"
                  "import './restore-test.ts';"
                  "import ignored from './restore-test.ts';"
                  "const restored = (() => { process.env.KNOXX_MIGRATION_MUTATION = 'yes'; return true; })();"
                  "(() => { process.env.KNOXX_MIGRATION_MUTATION = 'yes'; })();"
                  "process.env.KNOXX_MIGRATION_MUTATION = 'yes';"
                  "class Restore { static { process.env.KNOXX_MIGRATION_MUTATION = 'yes'; } }"]]
    (t/testing (str (name profile) ": " before)
      (with-fixture profile {:before before}
        (fn [root]
          (t/is (thrown? js/Error
                        ((if (= profile :vite) build/assert-configs! vitest/assert-config!) root)))
          (t/is (= "" (fs/readFileSync (node-path/join root "frontend/src/test/helper.test.ts")
                                      "utf8"))))))))

(t/deftest exported-property-values-cannot-hide-executable-expressions
  (doseq [profile [:vite :vitest]
          properties ["cacheDir:(() => { process.env.KNOXX_MIGRATION_MUTATION = 'yes'; return '.cache'; })()"
                      "cacheDir:(process.env.KNOXX_MIGRATION_MUTATION = '.cache')"]]
    (t/testing (str (name profile) ": " properties)
      (t/is (thrown? js/Error (inspect-profile! profile {:properties properties}))))))

(t/deftest current-vite-environment-and-proxy-constants-remain-admissible
  (t/is (= {} (inspect-profile!
                :vite
                {:before (str "const VITE_BACKEND_URL = process.env.VITE_KNOXX_BACKEND_URL || 'http://knoxx-backend:8000';"
                              "const KNOXX_PROXY = {'/api':{target:VITE_BACKEND_URL,changeOrigin:true},"
                              "'/ws':{target:VITE_BACKEND_URL,changeOrigin:true,ws:true}};")
                 :properties (str "server:{host:'0.0.0.0',port:5173,proxy:KNOXX_PROXY},"
                                  "preview:{host:'0.0.0.0',port:5173,proxy:KNOXX_PROXY}")}))))

(t/deftest static-vite-options-do-not-require-a-root-field-allowlist
  (t/is (= {} (inspect-profile! :vite {:properties "define:{__MIGRATION_LABEL__:'\"static\"'}"}))))

(t/deftest current-vite-framework-and-bridge-callbacks-remain-admissible
  (doseq [[config-file entry] [["vite.bridge.config.ts" "src/bridge/index.ts"]
                               ["vite.app-bridge.config.ts" "src/bridge/app.ts"]]]
    (t/is (= {} (inspect-profile!
                  :vite
                  {:config-file config-file
                   :before "import react from '@vitejs/plugin-react'; import path from 'node:path';"
                   :properties (str "plugins:[react()],build:{lib:{entry:path.resolve(__dirname,'" entry "'),"
                                    "fileName:(format)=>`knoxx-frontend-bridge.${format}.js`,formats:['es']}}")
                   :extra-files {entry ""}})))))
