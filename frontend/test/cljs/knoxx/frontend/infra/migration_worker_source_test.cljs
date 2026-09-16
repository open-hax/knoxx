(ns knoxx.frontend.infra.migration-worker-source-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- inspect-source! [path source]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-cljs-worker-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/shadow-cljs.edn" "{:source-paths [\"src/cljs\" \"worker-sources\"]}"
               "frontend/src/bridge/app.ts" ""
               "frontend/src/bridge/index.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs" "(ns knoxx.frontend.app)"
               "frontend/public/legacy.js" "self.addEventListener('install', () => self.skipWaiting());"}]
    (try
      (doseq [[relative contents] (assoc files path (str "(ns fixture.worker)\n" source))]
        (let [absolute (node-path/join root relative)]
          (fs/mkdirSync (node-path/dirname absolute) #js {:recursive true})
          (fs/writeFileSync absolute contents)))
      (.chdir js/process root)
      (manifest/current-records)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest cljs-worker-inputs-cannot-move-implementation-into-public-assets
  (doseq [source ["(.register (.-serviceWorker js/navigator) \"/bridge/legacy.js\")"
                  "(let [workers (.-serviceWorker js/navigator)] (.register workers \"/bridge/legacy.js\"))"
                  "(.register (aget js/navigator \"serviceWorker\") \"/bridge/legacy.js\")"
                  "(-> js/navigator (aget \"serviceWorker\") (.register \"/bridge/legacy.js\"))"
                  "(-> js/navigator .-serviceWorker (.register \"/bridge/legacy.js\"))"
                  "(js/navigator.serviceWorker.register \"/bridge/legacy.js\")"
                  "(new js/Worker \"/bridge/legacy.js\")"
                  "(js/SharedWorker. \"/bridge/legacy.js\")"
                  "(def MakeWorker js/Worker) (new MakeWorker \"/bridge/legacy.js\")"
                  "(new (.-Worker js/window) \"/bridge/legacy.js\")"
                  "(new (aget js/window \"Worker\") \"/bridge/legacy.js\")"
                  "(goog.object/get js/navigator \"serviceWorker\")"
                  "(. js/navigator -serviceWorker)"]]
    (t/is (thrown-with-msg? js/Error #"CLJS browser workers require explicit migration inventory support"
                           (inspect-source! "frontend/src/cljs/knoxx/frontend/app.cljs" source)) source)))

(t/deftest configured-cljs-worker-source-roots-use-the-same-admission
  (t/is (thrown-with-msg? js/Error #"CLJS browser workers require explicit migration inventory support"
                         (inspect-source! "frontend/worker-sources/worker.cljs"
                                          "(.register (.-serviceWorker js/navigator) \"/bridge/legacy.js\")"))))

(t/deftest inert-worker-examples-and-ordinary-browser-apis-remain-supported
  (doseq [source ["(def documentation \"(js/Worker. \\\"/bridge/legacy.js\\\")\")"
                  "(comment (.register (.-serviceWorker js/navigator) \"/bridge/legacy.js\"))"
                  "'(new js/Worker \"/bridge/legacy.js\")"
                  "(.getItem js/localStorage \"worker-preference\")"
                  "(get {\"Worker\" \"documentation\"} \"Worker\")"
                  "(cljs.core/get {\"Worker\" \"documentation\"} \"Worker\")"
                  "(.register registry \"ordinary registry entry\")"]]
    (t/is (= 2 (count (inspect-source! "frontend/src/cljs/knoxx/frontend/app.cljs" source))) source)))

(t/deftest property-getter-aliases-use-their-declared-namespace
  (t/is (thrown-with-msg?
          js/Error #"CLJS browser workers require explicit migration inventory support"
          (inspect-source! "frontend/worker-sources/worker.cljs"
                           "(ns fixture.worker (:require [goog.object :as object])) (object/get js/navigator \"serviceWorker\")")))
  (t/is (= 2 (count (inspect-source!
                      "frontend/worker-sources/worker.cljs"
                      "(ns fixture.worker (:require [cljs.core :as core])) (core/get {\"Worker\" \"documentation\"} \"Worker\")")))))
