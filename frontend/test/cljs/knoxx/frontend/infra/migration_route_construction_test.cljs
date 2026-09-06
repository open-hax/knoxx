(ns knoxx.frontend.infra.migration-route-construction-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- fixture-routes [route-source]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-route-construction-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/src/bridge/app.ts" ""
               "frontend/src/bridge/index.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"react\" :as react]\n"
                    " [\"react-router-dom\" :as rr]\n"
                    " [\"@open-hax/knoxx-app-bridge\" :as app]))\n"
                    "(def Route (.-Route rr))\n"
                    "(def RouterRoute Route)\n"
                    "(def createElement react/createElement)\n" route-source)}]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (.chdir js/process root)
      (->> (manifest/current-records)
           (filter #(= :route (:kind %)))
           (mapv #(select-keys % [:route :implementation :status])))
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest direct-react-route-construction-cannot-disappear-from-the-census
  (doseq [constructor ["react/createElement" "createElement"]
          component ["Route" "rr/Route" "RouterRoute"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(" constructor " " component
                   " #js {:path \"/legacy\" :element (react/createElement app/ChatPage nil)})"))))))

(t/deftest direct-react-routes-cannot-hide-beside-supported-routes
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route syntax"
          (fixture-routes
            (str "($ Route {:path \"/native\"\n :element ($ native/Page)})\n"
                 "(react/createElement Route #js {:path \"/legacy\""
                 " :element (react/createElement app/ChatPage nil)})")))))

(t/deftest direct-route-bindings-are-detected-without-literal-route-props
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route syntax"
          (fixture-routes
            "(react/createElement Route nil (react/createElement app/ChatPage nil))"))))

(t/deftest supported-route-construction-and-declarations-preserve-ownership
  (t/is (= [{:route "\"/legacy\"" :implementation "app/ChatPage" :status :legacy}
            {:route "\"/native\"" :implementation "native/Page" :status :native}]
           (fixture-routes
             (str "($ Route {:path \"/legacy\"\n :element ($ app/ChatPage)})\n"
                  "($ Route {:path \"/native\"\n :element ($ native/Page)})")))))

(t/deftest parked-direct-react-routes-do-not-declare-live-routes
  (doseq [wrapper ["comment" "quote"]]
    (t/is (= [] (fixture-routes
                  (str "(" wrapper " (react/createElement Route #js {:path \"/parked\"}))"))))))
