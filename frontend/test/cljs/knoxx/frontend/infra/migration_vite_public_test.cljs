(ns knoxx.frontend.infra.migration-vite-public-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-build :as build]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(def ^:private configurations
  {"vite.config.ts" nil
   "vite.bridge.config.ts" "src/bridge/index.ts"
   "vite.app-bridge.config.ts" "src/bridge/app.ts"})

(defn- with-configuration [config-name public-value inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-vite-public-"))
        entry (get configurations config-name)
        files {"frontend/package.json" "{}"
               "frontend/src/bridge/index.ts" ""
               "frontend/src/bridge/app.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs" "(ns fixture)"
               "frontend/index.html" "<iframe src='/legacy.html'></iframe>"
               "frontend/legacy-public/legacy.html" "<script>window.legacy = true;</script>"
               (str "frontend/" config-name)
               (str "import {defineConfig} from 'vite'; import path from 'node:path';"
                    "export default defineConfig({"
                    (when public-value (str "publicDir:" public-value ","))
                    (when entry (str "build:{lib:{entry:'" entry "'}}")) "});")}]
    (try
      (doseq [[path source] files]
        (let [absolute (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute) #js {:recursive true})
          (fs/writeFileSync absolute source)))
      (inspect root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest all-vite-builds-preserve-default-equivalent-or-disabled-public-directories
  (doseq [config-name (keys configurations)
          value [nil "false" "''" "'public'" "'./public/'" "'public/../public'"
                 "path.resolve(__dirname,'public')" "path.resolve(__dirname,'.','public')"]]
    (with-configuration config-name value
      (fn [root] (t/is (= {} (build/assert-configs! root)) (str config-name " " value))))))

(t/deftest all-vite-builds-reject-alternative-copied-public-directories
  (doseq [config-name (keys configurations)
          value ["'legacy-public'" "'../legacy-public'" "'public/nested'"
                 "path.resolve(__dirname,'legacy-public')"]]
    (with-configuration config-name value
      (fn [root]
        (t/is (thrown-with-msg? js/Error #"Vite public directory leaves governed HTML inventory"
                               (build/assert-configs! root)) (str config-name " " value))))))

(t/deftest unsupported-public-directory-values-fail-explicitly
  (doseq [value ["['public']" "{path:'public'}" "true"]]
    (with-configuration "vite.config.ts" value
      (fn [root]
        (t/is (thrown-with-msg? js/Error #"Unsupported Vite migration configuration"
                               (build/assert-configs! root)) value)))))

(t/deftest alternate-public-html-cannot-disappear-from-manifest-admission
  (with-configuration "vite.config.ts" "'legacy-public'"
    (fn [root]
      (let [original-cwd (.cwd js/process)]
        (try
          (.chdir js/process root)
          (t/is (thrown-with-msg? js/Error #"Vite public directory leaves governed HTML inventory"
                                 (manifest/current-records)))
          (finally (.chdir js/process original-cwd)))))))
