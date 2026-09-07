(ns knoxx.frontend.infra.migration-shadow-source-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]
            [knoxx.frontend.law.migration-shadow :as shadow-law]))

(defn- source-root-records [source-path files]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-shadow-source-"))
        original-cwd (.cwd js/process)
        files (merge {"frontend/package.json" "{}"
                      "frontend/shadow-cljs.edn" (pr-str {:source-paths ["src/cljs" source-path]})
                      "frontend/src/cljs/knoxx/frontend/app.cljs"
                      "(ns fixture (:require [\"/Page.js\" :as page]))\n($ Route {:path \"/legacy\"\n :element ($ page/Page)})"}
                     files)]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (.chdir js/process root)
      (manifest/current-records)
      (finally
        (.chdir js/process original-cwd)
        (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest configured-source-roots-cannot-hide-uncounted-javascript
  (doseq [extension ["js" "jsx" "mjs" "cjs"]
          [source-path directory] [["legacy" "frontend/legacy"]
                                   ["../shared/legacy" "shared/legacy"]]]
    (t/is (thrown-with-msg?
            js/Error #"Ungoverned JavaScript source in migration source tree"
            (source-root-records source-path
              {(str directory "/Page." extension) "export function Page() { return null; }"})))))

(t/deftest configured-clojurescript-sources-can-contain-inert-javascript-examples
  (t/is (= :native
           (->> (source-root-records "native"
                   {"frontend/src/cljs/knoxx/frontend/app.cljs"
                    "(ns fixture (:require [fixture.page :as page]))\n($ Route {:path \"/native\"\n :element ($ page/Page)})"
                    "frontend/native/fixture/page.cljs"
                    "(ns fixture.page) (def example \"export function Page() { return null; }\") (defn Page [] nil)"})
                (filter #(= :route (:kind %))) first :status))))

(t/deftest shadow-file-aliases-cannot-hide-javascript-outside-source-roots
  (t/is (thrown-with-msg?
          js/Error #"Shadow file resolution requires explicit migration inventory support"
          (source-root-records "src/cljs"
            {"frontend/shadow-cljs.edn"
             (pr-str {:source-paths ["src/cljs"]
                      :builds {:app {:js-options
                                     {:resolve {"outside-page" {:target :file :file "legacy/Page.js"}}}}}})
             "frontend/src/cljs/knoxx/frontend/app.cljs"
             "(ns fixture (:require [\"outside-page\" :as page]))\n($ Route {:path \"/legacy\"\n :element ($ page/Page)})"
             "frontend/legacy/Page.js" "export function Page() { return null; }"}))))

(t/deftest file-resolution-contract-preserves-bridges-and-non-file-resolutions
  (doseq [resolutions [[]
                       [["react" {:target :global :global "React"}]]
                       [["@open-hax/knoxx-app-bridge"
                         {:target :file :file "dist/bridge/knoxx-app-bridge.es.js"}]
                        ["@open-hax/knoxx-frontend-bridge"
                         {:target :file :file "dist/bridge/knoxx-frontend-bridge.es.js"}]]]]
    (let [facts {:path "shadow-cljs.edn" :resolutions resolutions}]
      (t/is (= facts (shadow-law/assert-file-resolutions! facts))))))

(t/deftest clojurescript-package-imports-cannot-hide-local-javascript
  (t/is (thrown-with-msg?
          js/Error #"Local import leaves governed frontend source tree"
          (source-root-records "src/cljs"
            {"frontend/package.json" "{\"dependencies\":{\"outside-page\":\"file:./legacy\"}}"
             "frontend/src/cljs/knoxx/frontend/app.cljs"
             "(ns fixture (:require [\"outside-page\" :as page]))\n($ Route {:path \"/legacy\"\n :element ($ page/Page)})"
             "frontend/legacy/package.json" "{\"name\":\"outside-page\",\"main\":\"Page.js\"}"
             "frontend/legacy/Page.js" "export function Page() { return null; }"}))))

(t/deftest registry-package-imports-retain-their-current-ownership-behavior
  (t/is (= :native
           (->> (source-root-records "src/cljs"
                   {"frontend/package.json" "{\"dependencies\":{\"react\":\"18.3.1\"}}"
                    "frontend/src/cljs/knoxx/frontend/app.cljs"
                    "(ns fixture (:require [\"react\" :as react]))\n($ Route {:path \"/native\"\n :element ($ react/Fragment)})"})
                (filter #(= :route (:kind %))) first :status))))

(t/deftest shadow-build-hooks-cannot-change-source-after-the-census
  (doseq [placement [[:build-hooks]
                     [:builds :app :build-hooks]
                     [:builds :app :release :build-hooks]
                     [:builds :app :dev :build-hooks]
                     [:builds :other :release :build-hooks]]]
    (t/is (thrown-with-msg?
            js/Error #"Shadow build hooks require explicit migration inventory support"
            (source-root-records "hooks"
              {"frontend/shadow-cljs.edn"
               (pr-str (assoc-in {:source-paths ["src/cljs" "hooks"]}
                                 placement ['fixture.restore/restore]))
               "frontend/hooks/fixture/restore.clj"
               "(ns fixture.restore) (defn restore {:shadow.build/stage :flush} [state] (spit \"dist/legacy.js\" \"console.log('legacy')\") state)"})))))

(t/deftest empty-base-hooks-cannot-hide-executable-release-overrides
  (t/is (thrown-with-msg?
          js/Error #"Shadow build hooks require explicit migration inventory support"
          (source-root-records "src/cljs"
            {"frontend/shadow-cljs.edn"
             (pr-str {:source-paths ["src/cljs"]
                      :builds {:app {:build-hooks []
                                     :release {:build-hooks [['fixture.restore/restore "legacy.js"]]}}}})}))))

(t/deftest absent-and-empty-build-hooks-preserve-static-builds
  (doseq [hooks [[] [nil] [[]] [[] nil []]]]
    (let [facts {:path "shadow-cljs.edn" :build-hooks hooks}]
      (t/is (= facts (shadow-law/assert-build-hooks! facts)))))
  (t/is (= :native
           (->> (source-root-records "src/cljs"
                   {"frontend/shadow-cljs.edn"
                    (pr-str {:source-paths ["src/cljs"]
                             :build-hooks []
                             :builds {:app {:build-hooks nil :release {:build-hooks []}}}})
                    "frontend/src/cljs/knoxx/frontend/app.cljs"
                    "(ns fixture)\n($ Route {:path \"/native\"\n :element ($ Navigate)})"})
                (filter #(= :route (:kind %))) first :status))))
