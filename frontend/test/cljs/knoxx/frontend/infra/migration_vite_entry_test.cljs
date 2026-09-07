(ns knoxx.frontend.infra.migration-vite-entry-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-build :as build]))

(defn- inspect-build! [settings]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-vite-entry-"))
        frontend (node-path/join root "frontend")]
    (try
      (fs/mkdirSync frontend)
      (fs/writeFileSync (node-path/join frontend "vite.config.ts")
                        (str "import {defineConfig} from 'vite'; import path from 'node:path';"
                             "export default defineConfig({build:{" settings "}});"))
      (build/assert-configs! root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest default-browser-build-keeps-the-inspected-html-entry
  (doseq [settings ["" "outDir:'dist',emptyOutDir:false" "lib:false"
                    "rollupOptions:{input:'index.html'}"
                    "rollupOptions:{input:['./index.html']}"
                    "rollupOptions:{input:{main:'index.html'}}"
                    "rollupOptions:{input:path.resolve(__dirname,'index.html')}"
                    "rollupOptions:{input:['index.html','./index.html']}"
                    "lib:{entry:'index.html'}"
                    "lib:{entry:'legacy/page.ts'},rollupOptions:{input:'index.html'}"]]
    (t/is (= {} (inspect-build! settings)) settings)))

(t/deftest default-browser-build-cannot-add-uninspected-html-or-library-inputs
  (doseq [settings ["rollupOptions:{input:'legacy/entry.html'}"
                    "rollupOptions:{input:['index.html','legacy/entry.html']}"
                    "rollupOptions:{input:{main:'index.html',legacy:'../legacy/entry.html'}}"
                    "rollupOptions:{input:path.resolve(__dirname,'legacy/entry.html')}"
                    "rollupOptions:{input:[]}"
                    "rollupOptions:{input:{}}"
                    "lib:{entry:'../legacy/entry.ts'}"
                    "lib:{entry:['index.html','../legacy/entry.ts']}"
                    "lib:{entry:{main:'index.html',legacy:'../legacy/entry.ts'}}"
                    "ssr:'../legacy/entry.ts'"
                    "ssr:path.resolve(__dirname,'../legacy/entry.ts')"
                    "ssr:'../legacy/entry.ts',rollupOptions:{input:'index.html'}"]]
    (t/is (thrown-with-msg? js/Error #"Default Vite entry leaves governed HTML inventory"
                           (inspect-build! settings)) settings)))

(t/deftest uninspectable-default-entry-values-fail-explicitly
  (doseq [settings ["rollupOptions:{input:[42]}" "rollupOptions:{input:{main:['index.html']}}"
                    "ssr:['index.html'],rollupOptions:{input:'../legacy/page.ts'}"
                    "ssr:{main:'index.html'},rollupOptions:{input:'../legacy/page.ts'}"
                    "lib:{}"]]
    (t/is (thrown-with-msg? js/Error #"Unsupported Vite migration configuration"
                           (inspect-build! settings)) settings)))
