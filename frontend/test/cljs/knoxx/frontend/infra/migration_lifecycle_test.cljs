(ns knoxx.frontend.infra.migration-lifecycle-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-build :as build]))

(defn- inspect! [scripts]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-script-lifecycle-"))
        frontend-root (node-path/join root "frontend")]
    (try
      (fs/mkdirSync frontend-root)
      (fs/writeFileSync (node-path/join frontend-root "package.json")
                        (js/JSON.stringify (clj->js {:scripts scripts})))
      (build/assert-configs! root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest package-build-hooks-cannot-hide-implicit-production-commands
  (doseq [hook ["prebuild" "postbuild"]]
    (t/is (thrown-with-msg?
            js/Error #"Package script lifecycle hooks require explicit migration inventory support"
            (inspect! {"build" "shadow-cljs release app" hook "node --version"}))))
  (doseq [script-name ["build:cljs" "build:bridge" "build:app-bridge"
                       "migration:check" "migration:write" "test" "test:coverage"]
          prefix ["pre" "post"]]
    (t/is (thrown-with-msg?
            js/Error #"Package script lifecycle hooks require explicit migration inventory support"
            (inspect! {script-name "node --version"
                       (str prefix script-name) "node --version"})))))

(t/deftest explicit-commands-and-empty-hooks-retain-current-behavior
  (doseq [scripts [{}
                   {"build" "shadow-cljs release app"}
                   {"build" "shadow-cljs release app" "prebuild" "" "postbuild" " \n\t"}
                   {"preview" "vite preview"}
                   {"retired" "node --version" "postbuild" "node --version"}]]
    (t/is (= {} (inspect! scripts)) scripts)))
