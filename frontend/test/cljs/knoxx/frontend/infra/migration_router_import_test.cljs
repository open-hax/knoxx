(ns knoxx.frontend.infra.migration-router-import-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- fixture-routes [module import-options body]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-router-import-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/src/bridge/app.ts" ""
               "frontend/src/bridge/index.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"" module "\" " import-options "]"
                    " [\"@open-hax/knoxx-app-bridge\" :as app]))\n" body)}]
    (try
      (doseq [[path source] files]
        (let [absolute (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute) #js {:recursive true})
          (fs/writeFileSync absolute source)))
      (.chdir js/process root)
      (->> (manifest/current-records) (filter #(= :route (:kind %))) vec)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest renamed-router-constructors-cannot-hide-dynamic-route-props
  (doseq [module ["react-router" "react-router-dom"]
          local-reference ["RouterRoute" "fixture/RouterRoute"]
          constructor ["$" "react/createElement"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes module ":refer [Route] :rename {Route RouterRoute}"
                            (str "(def route-props " (when (not= "$" constructor) "#js ")
                                 "{:path \"/legacy\" :element ($ app/ChatPage)})\n"
                                 "(" constructor " " local-reference " "
                                 (if (= "$" constructor) "{& route-props}" "route-props") ")"))))))

(t/deftest renamed-route-object-apis-cannot-hide-their-inputs
  (doseq [api ["useRoutes" "createBrowserRouter" "createHashRouter" "createMemoryRouter"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes "react-router-dom"
                            (str ":refer [" api "] :rename {" api " build-routes}")
                            "(build-routes route-objects)")))))

(t/deftest unused-and-parked-renamed-imports-remain-supported
  (doseq [body ["" "(comment ($ RouterRoute route-props))"
                "(quote ($ RouterRoute route-props))"
                "(def example \"RouterRoute\")"]]
    (t/is (= [] (fixture-routes "react-router-dom"
                                ":refer [Route] :rename {Route RouterRoute}" body)))))

(t/deftest ordinary-renamed-components-and-hooks-remain-supported
  (doseq [[module options body]
          [["other-widgets" ":refer [Route] :rename {Route RouterRoute}" "($ RouterRoute props)"]
           ["react-router-dom" ":refer [Link] :rename {Link AppLink}" "($ AppLink props)"]
           ["react-router-dom" ":refer [useLocation] :rename {useLocation location}" "(location)"]]]
    (t/is (= [] (fixture-routes module options body)))))

(t/deftest canonical-referred-route-remains-in-the-census
  (let [routes (fixture-routes "react-router-dom" ":refer [Route]"
                               "($ Route {:path \"/native\"\n :element ($ native/Page)})")]
    (t/is (= 1 (count routes)))
    (t/is (= :native (:status (first routes))))
    (t/is (= "\"/native\"" (:route (first routes))))))
