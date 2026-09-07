(ns knoxx.frontend.infra.migration-route-construction-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- fixture-routes [route-source & [router-alias extra-files extra-requires]]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-route-construction-"))
        original-cwd (.cwd js/process)
        router-alias (or router-alias "rr")
        files (merge {"frontend/package.json" "{}"
               "frontend/src/bridge/app.ts" ""
               "frontend/src/bridge/index.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"react\" :as react]\n"
                    " [\"react-router-dom\" :as " router-alias "]\n"
                    " [\"@open-hax/knoxx-app-bridge\" :as app]" extra-requires "))\n"
                    "(def Route (.-Route " router-alias "))\n"
                    "(def createElement react/createElement)\n" route-source)} extra-files)]
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
              (str (when (= component "RouterRoute") "(def RouterRoute Route)\n")
                   "(" constructor " " component
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

(t/deftest constructor-aliases-cannot-hide-route-references
  (doseq [invocation ["(make-element Route {:path \"/chat\" :element ($ app/ChatPage)})"
                      "((identity make-element) Route {:path \"/chat\" :element ($ app/ChatPage)})"
                      "(apply make-element Route [#js {:path \"/chat\" :element ($ app/ChatPage)}])"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes (str "(def make-element react/createElement)\n" invocation))))))

(t/deftest constructor-aliases-cannot-hide-inside-definition-bodies
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route syntax"
          (fixture-routes
            (str "(def make-element react/createElement)\n"
                 "(def app-route (make-element Route {:path \"/chat\" :element ($ app/ChatPage)}))")))))

(t/deftest parked-route-references-do-not-become-call-candidates
  (doseq [wrapper ["comment" "quote"]
          body ["Route" "(make-element Route {:path \"/parked\"})"]]
    (t/is (= [] (fixture-routes (str "(" wrapper " " body ")"))))))

(t/deftest component-aliases-cannot-hide-routes-with-computed-props
  (doseq [declaration ["(def RouterRoute Route)"
                       "(defonce RouterRoute Route)"
                       "(def RouterRoute (.-Route rr))"
                       "(def RouterRoute rr/Route)"]
          constructor ["react/createElement" "make-element"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str declaration "\n(def make-element react/createElement)\n"
                   "(def route-props #js {:path \"/chat\" :element ($ app/ChatPage)})\n"
                   "(" constructor " RouterRoute route-props)"))))))

(t/deftest lexical-component-aliases-cannot-hide-route-values
  (doseq [binding-form ["[RouterRoute Route]"
                        "[RouterRoute (.-Route rr)]"
                        "[route-values [Route] RouterRoute (first route-values)]"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(def make-element react/createElement)\n"
                   "(def route-props #js {:path \"/chat\" :element ($ app/ChatPage)})\n"
                   "(let " binding-form
                   " (make-element RouterRoute route-props))"))))))

(t/deftest parked-component-aliases-remain-inert
  (doseq [wrapper ["comment" "quote"]]
    (t/is (= [] (fixture-routes
                  (str "(" wrapper " (do (def RouterRoute Route)"
                       " (let [LocalRoute Route] (make-element LocalRoute nil))))"))))))

(t/deftest route-object-apis-cannot-consume-untracked-route-definitions
  (doseq [api ["useRoutes" "createBrowserRouter" "createHashRouter" "createMemoryRouter"]
          call [(str "(rr/" api " app-route-objects)")
                (str "(." api " rr app-route-objects)")
                (str "(def build-routes (.-" api " rr))\n(build-routes app-route-objects)")]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(def app-route-objects #js [#js {:path \"/chat\" :element ($ app/ChatPage)}])\n"
                   call))))))

(t/deftest route-object-apis-cannot-hide-beside-supported-routes
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route syntax"
          (fixture-routes
            (str "($ Route {:path \"/native\"\n :element ($ native/Page)})\n"
                 "(rr/createBrowserRouter #js [#js {:path \"/chat\" :element ($ app/ChatPage)}])")))))

(t/deftest route-object-api-examples-remain-inert
  (doseq [source ["(comment (rr/useRoutes routes))"
                  "(quote (rr/createBrowserRouter routes))"
                  "(def example \"rr/createHashRouter rr/createMemoryRouter\")"]]
    (t/is (= [] (fixture-routes source)))))

(t/deftest computed-router-components-cannot-hide-behind-constructor-and-props-aliases
  (doseq [getter ["aget" "cljs.core/aget"]
          router-alias ["rr" "router"]
          key-form ["\"Route\"" "route-key" "(str \"Ro\" \"ute\")"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(def route-key \"Route\")\n"
                   "(def RouterRoute (" getter " " router-alias " " key-form "))\n"
                   "(def route-props #js {:path \"/chat\" :element ($ app/ChatPage)})\n"
                   "($ RouterRoute route-props)")
              router-alias)))))

(t/deftest computed-router-api-getters-remain-visible-to-the-census
  (doseq [getter ["goog.object/get" "gobj/get" "js/Reflect.get"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(def route-key \"useRoutes\")\n"
                   "(def create-routes (" getter " rr route-key))\n"
                   "(create-routes route-definitions)"))))))

(t/deftest computed-access-examples-and-unrelated-objects-remain-inert
  (doseq [source ["(comment (def RouterRoute (aget rr route-key)))"
                  "(quote (aget rr \"Route\"))"
                  "(def example \"(aget rr \\\"Route\\\")\")"
                  "(def label \"Route\")"
                  "(aget payload \"Route\")"
                  "(cljs.core/aget payload route-key)"]]
    (t/is (= [] (fixture-routes source)))))

(t/deftest mixed-element-constructors-preserve-live-legacy-route-dependencies
  (doseq [child ["(react/createElement app/ChatPage nil)"
                 "(createElement app/ChatPage nil)"
                 "(.createElement react app/ChatPage nil)"
                 "(make-element app/ChatPage nil)"
                 "((aget react \"createElement\") app/ChatPage nil)"
                 "(let [page app/ChatPage] (make-element page nil))"]]
    (t/is (= [{:route "\"/mixed\"" :implementation "app/ChatPage" :status :legacy}]
             (fixture-routes
               (str "(def make-element react/createElement)\n"
                    "($ Route {:path \"/mixed\"\n"
                    " :element ($ react/Fragment " child ")})"))))))

(t/deftest direct-react-elements-preserve-native-route-ownership
  (doseq [element ["(react/createElement native/Page nil)"
                   "(createElement native/Page nil)"
                   "(.createElement react native/Page nil)"]]
    (t/is (= [{:route "\"/native\"" :implementation "native/Page" :status :native}]
             (fixture-routes
               (str "($ Route {:path \"/native\"\n :element " element "})"))))))

(t/deftest inert-legacy-references-do-not-change-native-element-ownership
  (doseq [wrapper ["comment" "quote"]]
    (t/is (= [{:route "\"/native\"" :implementation "native/Page" :status :native}]
             (fixture-routes
               (str "($ Route {:path \"/native\"\n :element (do (" wrapper
                    " (react/createElement app/ChatPage nil))"
                    " (react/createElement native/Page nil))})"))))))

(t/deftest trusted-local-component-definitions-preserve-legacy-provenance
  (doseq [declarations ["(def LegacyOpsRedirect app/ChatPage)"
                        "(defonce LegacyOpsRedirect app/ChatPage)"
                        "(defn LegacyOpsRedirect [] ($ app/ChatPage))"
                        "(defnc LegacyOpsRedirect [] ($ app/ChatPage))"
                        "(def helper app/ChatPage) (def LegacyOpsRedirect helper)"
                        "(def LegacyOpsRedirect native/Page) (set! LegacyOpsRedirect app/ChatPage)"
                        "(def LegacyOpsRedirect native/Page) (set! fixture/LegacyOpsRedirect app/ChatPage)"
                        "(def LegacyOpsRedirect app/ChatPage) (def LegacyOpsRedirect native/Page)"]]
    (t/is (= [{:route "\"/chat\"" :implementation "app/ChatPage" :status :legacy}]
             (fixture-routes
               (str declarations "\n($ Route {:path \"/chat\"\n :element ($ LegacyOpsRedirect)})"))))))

(t/deftest local-native-controls-and-parked-aliases-retain-native-ownership
  (doseq [declarations ["(def Navigate (.-Navigate rr)) (defnc LegacyOpsRedirect [] ($ Navigate))"
                        "(comment (def LegacyOpsRedirect app/ChatPage))"
                        "(quote (def LegacyOpsRedirect app/ChatPage))"
                        "(defnc LegacyOpsRedirect [] (comment app/ChatPage) ($ Navigate))"]]
    (t/is (= [{:route "\"/redirect\"" :implementation "LegacyOpsRedirect" :status :native}]
             (fixture-routes
               (str declarations "\n($ Route {:path \"/redirect\"\n :element ($ LegacyOpsRedirect)})"))))))

(t/deftest self-qualified-local-components-preserve-legacy-provenance
  (t/is (= [{:route "\"/chat\"" :implementation "app/ChatPage" :status :legacy}]
           (fixture-routes
             (str "(def helper app/ChatPage) (def LegacyOpsRedirect fixture/helper)\n"
                  "($ Route {:path \"/chat\"\n :element ($ fixture/LegacyOpsRedirect)})")))))

(t/deftest computed-bridge-access-cannot-classify-a-local-component-as-native
  (doseq [definition ["(def LegacyOpsRedirect (.-ChatPage app))"
                      "(def LegacyOpsRedirect (aget app \"ChatPage\"))"
                      "(def LegacyOpsRedirect (aget app component-name))"
                      "(def LegacyOpsRedirect (some-> app (aget \"ChatPage\")))"
                      "(def bridge-module app) (def LegacyOpsRedirect (aget bridge-module \"ChatPage\"))"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes
              (str definition "\n($ Route {:path \"/chat\"\n :element ($ LegacyOpsRedirect)})")))))
  (t/is (= [{:route "\"/redirect\"" :implementation "LegacyOpsRedirect" :status :native}]
           (fixture-routes
             (str "(defnc LegacyOpsRedirect [] (comment (aget app \"ChatPage\")) ($ Navigate))\n"
                  "($ Route {:path \"/redirect\"\n :element ($ LegacyOpsRedirect)})")))))

(t/deftest threaded-router-values-cannot-conceal-aliased-route-construction
  (doseq [expression ["(-> rr (aget \"Route\"))" "(some-> rr (aget \"Route\"))"
                      "(cljs.core/some-> rr (cljs.core/aget \"Route\"))"
                      "(->> rr (goog.object/get \"Route\"))" "(some->> rr identity)"
                      "(cond-> rr true (aget \"Route\"))" "(cond->> rr true identity)"
                      "(as-> rr router-value (aget router-value \"Route\"))"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route syntax"
            (fixture-routes
              (str "(def RouterComponent " expression ")\n"
                   "(def route-props #js {:path \"/chat\" :element ($ app/ChatPage)})\n"
                   "($ RouterComponent route-props)"))))))

(t/deftest parked-threaded-router-values-and-unrelated-threading-remain-inert
  (doseq [source ["(comment (some-> rr (aget \"Route\")))"
                  "(quote (-> rr (aget \"Route\")))"
                  "(some-> payload (aget \"Route\"))"]]
    (t/is (= [] (fixture-routes source)))))

(def ^:private project-bridge-files
  {"frontend/src/bridge/app.ts"
   "export { ChatPage } from '../pages/ChatPage'; export { ChatWorkspacePane } from '../components/ChatWorkspacePane';"
   "frontend/src/pages/ChatPage.ts" "export const ChatPage = () => null;"
   "frontend/src/components/ChatWorkspacePane.ts" "export const ChatWorkspacePane = () => null;"})

(t/deftest project-page-wrappers-cannot-conceal-legacy-route-ownership
  (doseq [wrapper ["(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (defnc ForwardedPage [] ($ legacy/ChatPage))"
                   "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :refer [ChatPage]])) (def ForwardedPage ChatPage)"
                   "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (def ForwardedPage (aget legacy \"ChatPage\"))"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes
              "($ Route {:path \"/chat\"\n :element ($ wrapper/ForwardedPage)})" nil
              (assoc project-bridge-files "frontend/src/cljs/knoxx/frontend/pages/wrapper.cljs" wrapper)
              " [knoxx.frontend.pages.wrapper :as wrapper]")))))

(t/deftest transitive-project-wrappers-retain-the-legacy-route-obligation
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route implementation"
          (fixture-routes
            "($ Route {:path \"/chat\"\n :element ($ wrapper/ChatPage)})" nil
            (assoc project-bridge-files
                   "frontend/src/cljs/knoxx/frontend/pages/wrapper.cljs"
                   "(ns knoxx.frontend.pages.wrapper (:require [knoxx.frontend.pages.inner :as inner])) (def ChatPage inner/ChatPage)"
                   "frontend/src/cljs/knoxx/frontend/pages/inner.cljs"
                   "(ns knoxx.frontend.pages.inner (:require [\"@open-hax/knoxx-app-bridge\" :as legacy] [knoxx.frontend.pages.wrapper :as wrapper])) (def ChatPage legacy/ChatPage) (def cycle-reference wrapper/ChatPage)")
            " [knoxx.frontend.pages.wrapper :as wrapper]"))))

(t/deftest native-project-pages-can-compose-bridge-widgets
  (doseq [wrapper ["(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (defnc ChatPage [] ($ legacy/ChatWorkspacePane))"
                   "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :refer [ChatWorkspacePane]])) (defnc ChatPage [] ($ ChatWorkspacePane))"
                   "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (comment (def ChatPage legacy/ChatPage)) (defnc ChatPage [] ($ legacy/ChatWorkspacePane))"]]
    (t/is (= [{:route "\"/chat\"" :implementation "wrapper/ChatPage" :status :native}]
             (fixture-routes
               "($ Route {:path \"/chat\"\n :element ($ wrapper/ChatPage)})" nil
               (assoc project-bridge-files "frontend/src/cljs/knoxx/frontend/pages/wrapper.cljs" wrapper)
               " [knoxx.frontend.pages.wrapper :as wrapper]")))))

(t/deftest removing-the-main-bridge-alias-cannot-hide-a-project-page-wrapper
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route implementation"
          (fixture-routes
            "" nil
            (assoc project-bridge-files
                   "frontend/src/cljs/knoxx/frontend/app.cljs"
                   "(ns fixture (:require [\"react-router-dom\" :as rr] [knoxx.frontend.pages.wrapper :as wrapper])) (def Route (.-Route rr))\n($ Route {:path \"/chat\"\n :element ($ wrapper/ChatPage)})"
                   "frontend/src/cljs/knoxx/frontend/pages/wrapper.cljs"
                   "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (defnc ChatPage [] ($ legacy/ChatPage))")))))

(t/deftest self-qualified-native-controls-do-not-inherit-unrelated-route-dependencies
  (t/is (= [{:route "\"/legacy\"" :implementation "app/ChatPage" :status :legacy}
            {:route "\"/native\"" :implementation "fixture/LegacyOpsRedirect" :status :native}]
           (fixture-routes
             (str "(def Navigate (.-Navigate rr)) (defnc LegacyOpsRedirect [] ($ Navigate))\n"
                  "($ Route {:path \"/legacy\"\n :element ($ app/ChatPage)})\n"
                  "($ Route {:path \"/native\"\n :element ($ fixture/LegacyOpsRedirect)})")))))

(t/deftest project-macros-cannot-remove-live-routes-from-the-census
  (doseq [[path source] [["frontend/src/cljs/fixture/macros.clj"
                         "(ns fixture.macros) (defmacro hidden-route [] '($ Route {:path \"/chat\" :element ($ app/ChatPage)}))"]
                        ["frontend/src/cljs/fixture/macros.cljc"
                         "(ns fixture.macros) #?(:clj (defmacro hidden-route [] '($ Route {:path \"/chat\" :element ($ app/ChatPage)})))"]
                        ["frontend/src/cljs/fixture/macros.clj"
                         "(ns fixture.macros) (defn ^:macro hidden-route [form env] '($ Route {:path \"/chat\" :element ($ app/ChatPage)}))"]
                        ["frontend/src/cljs/fixture/macros.clj"
                         "(ns fixture.macros) (defn hidden-route \"Route macro\" {:macro true} [form env] '($ Route {:path \"/chat\" :element ($ app/ChatPage)}))"]
                        ["frontend/src/cljs/fixture/macros.clj"
                         "(ns fixture.macros) (defn hidden-route ([form env] '($ Route {:path \"/chat\" :element ($ app/ChatPage)})) {:macro true})"]
                        ["frontend/test/cljs/fixture/macros.cljc"
                         "(ns fixture.macros) #?(:clj (defmacro hidden-route [] '($ Route {:path \"/chat\" :element ($ app/ChatPage)})))"]]]
    (t/is (thrown-with-msg?
            js/Error #"Project macros require explicit migration inventory support"
            (fixture-routes "(hidden-route)" nil
                            {path source "frontend/shadow-cljs.edn" "{:source-paths [\"src/cljs\" \"test/cljs\"]}"})))))

(t/deftest explicit-macro-imports-require-inventory-support
  (doseq [require-clause ["(:require-macros [fixture.macros :refer [hidden-route]])"
                         "(:require [fixture.macros :refer-macros [hidden-route]])"
                         "(:require [fixture.macros :include-macros true])"]]
    (t/is (thrown-with-msg?
            js/Error #"Project macros require explicit migration inventory support"
            (fixture-routes "" nil
                            {"frontend/src/cljs/knoxx/frontend/app.cljs"
                             (str "(ns fixture " require-clause ") (hidden-route)")})))))

(t/deftest inert-macro-examples-and-ordinary-clojure-sources-remain-supported
  (t/is (= [] (fixture-routes "" nil
                             {"frontend/src/cljs/fixture/macros.clj"
                              "(ns fixture.macros) (comment (defmacro parked [] nil)) (quote (defmacro parked [] nil)) (defn ordinary [] {:macro true}) (def data {:macro true})"}))))

(t/deftest bridge-reexport-adapters-retain-terminal-page-ownership
  (doseq [adapter ["export { ChatPage as ForwardedPage } from '../pages/ChatPage';"
                   "import { ChatPage as OriginalPage } from '../pages/ChatPage'; export { OriginalPage as ForwardedPage };"
                   "export { default as ForwardedPage } from './default-page';"
                   "export { ChatPage as ForwardedPage } from './barrel';"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes
              "($ Route {:path \"/chat\"\n :element ($ wrapper/ChatPage)})" nil
              (assoc project-bridge-files
                     "frontend/src/bridge/app.ts" "export { ForwardedPage as ChatPage } from '../components/Adapter';"
                     "frontend/src/components/Adapter.ts" adapter
                     "frontend/src/components/barrel.ts" "export * from '../pages/ChatPage';"
                     "frontend/src/components/default-page.ts" "export { ChatPage as default } from '../pages/ChatPage';"
                     "frontend/src/cljs/knoxx/frontend/pages/wrapper.cljs"
                     "(ns knoxx.frontend.pages.wrapper (:require [\"@open-hax/knoxx-app-bridge\" :as legacy])) (defnc ChatPage [] ($ legacy/ChatPage))")
              " [knoxx.frontend.pages.wrapper :as wrapper]")))))

(t/deftest referred-bridge-exports-retain-root-local-wrapper-ownership
  (doseq [[binding-source local-name] [[":refer [ChatPage]" "ChatPage"]
                                       [":refer [ChatPage] :rename {ChatPage ForwardedPage}" "ForwardedPage"]]]
    (t/is (= [{:route "\"/chat\"" :implementation "app/ChatPage" :status :legacy}]
             (fixture-routes
               "" nil
               (assoc project-bridge-files "frontend/src/cljs/knoxx/frontend/app.cljs"
                      (str "(ns fixture (:require [\"@open-hax/knoxx-app-bridge\" :as app " binding-source "]))\n"
                           "(defnc WrappedPage [] ($ " local-name "))\n"
                           "($ Route {:path \"/chat\"\n :element ($ fixture/WrappedPage)})")))))))
