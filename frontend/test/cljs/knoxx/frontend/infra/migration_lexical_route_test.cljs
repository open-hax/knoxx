(ns knoxx.frontend.infra.migration-lexical-route-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(defn- fixture-routes [source]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-lexical-route-"))
        original-cwd (.cwd js/process)
        files {"frontend/package.json" "{}"
               "frontend/src/bridge/app.ts" ""
               "frontend/src/bridge/index.ts" ""
               "frontend/src/cljs/knoxx/frontend/app.cljs"
               (str "(ns fixture (:require [\"@open-hax/knoxx-app-bridge\" :as app]))\n" source)}]
    (try
      (doseq [[path contents] files]
        (let [file (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname file) #js {:recursive true})
          (fs/writeFileSync file contents)))
      (.chdir js/process root)
      (->> (manifest/current-records) (filter #(= :route (:kind %)))
           (mapv #(select-keys % [:implementation :status])))
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(def ^:private placeholder-route
  "($ Route {:path \"/studio\"\n :element ($ PlaceholderPage)})")

(t/deftest lexical-component-aliases-cannot-establish-native-ownership
  (doseq [binding-form ["[PlaceholderPage app/BroadcastStudioPage]"
                  "[page app/BroadcastStudioPage PlaceholderPage page]"
                  "[PlaceholderPage supplied-component]"]
          constructor ["let" "cljs.core/let" "loop"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes (str "(defnc App [] (" constructor " " binding-form " " placeholder-route "))"))))))

(t/deftest destructured-component-bindings-are-not-trusted-global-names
  (doseq [bindings ["[[PlaceholderPage] components]"
                   "[{:keys [PlaceholderPage]} components]"
                   "[{:component/keys [PlaceholderPage]} components]"
                   "[{:strs [PlaceholderPage]} components]"
                   "[{PlaceholderPage :page} components]"
                   "[{:as PlaceholderPage} component]"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes (str "(let " bindings " " placeholder-route ")"))))))

(t/deftest component-parameters-cannot-inherit-trusted-global-ownership
  (doseq [function-prefix ["(fn [PlaceholderPage] "
                          "(fn Render [PlaceholderPage] "
                          "(defn App [PlaceholderPage] "
                          "(defnc App [{:keys [PlaceholderPage]}] "
                          "(defn App \"Route factory\" {:private true} [PlaceholderPage] "]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes (str function-prefix placeholder-route ")")))))
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Shadow route implementation"
          (fixture-routes (str "(defn App ([] nil) ([PlaceholderPage] " placeholder-route "))")))))

(t/deftest nested-element-bindings-and-local-functions-retain-the-ownership-boundary
  (doseq [source [(str "($ Route {:path \"/studio\"\n :element (let [PlaceholderPage supplied-component]"
                       " ($ PlaceholderPage))})")
                  (str "(letfn [(PlaceholderPage [] nil)] " placeholder-route ")")
                  (str "(when-let [PlaceholderPage component] " placeholder-route ")")
                  (str "(for [PlaceholderPage components] " placeholder-route ")")
                  (str "(as-> component PlaceholderPage " placeholder-route ")")]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes source)))))

(t/deftest ordinary-bindings-and-bridge-hooks-do-not-change-native-components
  (doseq [source [(str "(defnc App [{:keys [title]}] (let [auth (app/useChatWorkspaceController)] "
                       placeholder-route "))")
                  (str "(let [data {:PlaceholderPage app/BroadcastStudioPage}"
                       " props {:keys [PlaceholderPage]}] " placeholder-route ")")
                  (str "(defn unrelated [PlaceholderPage] ($ PlaceholderPage))\n" placeholder-route)
                  (str "(let [PlaceholderPage app/BroadcastStudioPage] nil)\n" placeholder-route)
                  (str "(let [element " placeholder-route " PlaceholderPage app/BroadcastStudioPage] element)")]]
    (t/is (= [{:implementation "PlaceholderPage" :status :native}]
             (fixture-routes source)))))

(t/deftest conditional-bindings-do-not-escape-into-the-else-branch
  (t/is (= [{:implementation "native/Page" :status :native}
            {:implementation "PlaceholderPage" :status :native}]
           (fixture-routes
             (str "(if-let [PlaceholderPage component]"
                  " ($ Route {:path \"/native\"\n :element ($ native/Page)}) "
                  placeholder-route ")")))))

(t/deftest parked-lexical-aliases-and-qualified-components-remain-supported
  (doseq [wrapper ["comment" "quote"]]
    (t/is (= [{:implementation "PlaceholderPage" :status :native}]
             (fixture-routes
               (str "(" wrapper " (let [PlaceholderPage app/BroadcastStudioPage] nil))\n"
                    placeholder-route)))))
  (t/is (= [{:implementation "native/PlaceholderPage" :status :native}]
           (fixture-routes
             "(let [PlaceholderPage app/BroadcastStudioPage] ($ Route {:path \"/native\"\n :element ($ native/PlaceholderPage)}))"))))

(t/deftest explicit-legacy-elements-cannot-become-native-through-a-shadowed-sibling
  (t/is (= [{:implementation "app/BroadcastStudioPage" :status :legacy}]
           (fixture-routes
             (str "(let [PlaceholderPage supplied-component]"
                  " ($ Route {:path \"/studio\"\n :element ($ react/Fragment"
                  " ($ PlaceholderPage) ($ app/BroadcastStudioPage))}))")))))

(t/deftest core-binding-macros-cannot-launder-component-ownership
  (doseq [[prefix suffix] [["(when-first [PlaceholderPage [app/BroadcastStudioPage]] " ")"]
                           ["(when-first [[PlaceholderPage] [[app/BroadcastStudioPage]]] " ")"]
                           ["(this-as PlaceholderPage " ")"]
                           ["(dotimes [PlaceholderPage 1] " ")"]
                           ["(amap source PlaceholderPage result " ")"]
                           ["(areduce source index PlaceholderPage nil " ")"]
                           ["(simple-benchmark [PlaceholderPage app/BroadcastStudioPage] " " 1)"]
                           ["(letfn* [PlaceholderPage (fn [] app/BroadcastStudioPage)] " ")"]
                           ["(defmethod render :default [PlaceholderPage] " ")"]
                           ["(defmethod render :default render-page [PlaceholderPage] " ")"]
                           ["(reify IRender (render [this PlaceholderPage] " "))"]
                           ["(deftype View [PlaceholderPage] IRender (render [this] " "))"]
                           ["(defrecord View [PlaceholderPage] IRender (render [this] " "))"]]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes (str prefix placeholder-route suffix))))))

(t/deftest var-redefinitions-retain-qualified-and-unqualified-identities
  (doseq [constructor ["with-redefs" "binding"]
          reference ["PlaceholderPage" "fixture/PlaceholderPage"]
          component ["PlaceholderPage" "fixture/PlaceholderPage"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Shadow route implementation"
            (fixture-routes
              (str "(def ^:dynamic PlaceholderPage nil) (" constructor " [" reference
                   " app/BroadcastStudioPage] ($ Route {:path \"/studio\"\n :element ($ "
                   component ")}))"))))))

(t/deftest unrelated-core-bindings-and-var-redefinitions-do-not-taint-native-routes
  (doseq [source [(str "(when-first [props [{}]] " placeholder-route ")")
                  (str "(with-redefs [unrelated-value 1] " placeholder-route ")")
                  (str "(with-redefs [PlaceholderPage app/BroadcastStudioPage] nil) " placeholder-route)
                  (str "(with-redefs [PlaceholderPage " placeholder-route "] nil)")
                  (str "(comment (with-redefs [PlaceholderPage app/BroadcastStudioPage] nil)) "
                       placeholder-route)
                  (str "(let [PlaceholderPage app/BroadcastStudioPage]"
                       " (deftype View [] IRender (render [this] " placeholder-route ")))" )]]
    (t/is (= [:native] (mapv :status (fixture-routes source))) source)))
