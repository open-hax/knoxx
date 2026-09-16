(ns knoxx.frontend.infra.migration-shadow-text-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [cljs.tools.reader.edn :as edn]
            [knoxx.frontend.infra.migration-build :as build]))

(defn- inspect! [configuration]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-shadow-text-"))
        frontend-root (node-path/join root "frontend")]
    (try
      (fs/mkdirSync frontend-root)
      (fs/writeFileSync (node-path/join frontend-root "shadow-cljs.edn")
                        (pr-str configuration))
      (build/assert-configs! root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(defn- current-test-bootstrap []
  (get-in (edn/read-string (fs/readFileSync "shadow-cljs.edn" "utf8"))
          [:builds :test :prepend-js]))

(t/deftest shadow-build-and-module-text-cannot-add-uninspected-javascript
  (doseq [setting [:prepend :append :prepend-js :append-js]
          placement [[] [:builds :app] [:builds :app :modules :app]
                     [:builds :app :release] [:builds :app :dev]
                     [:builds :app :release :modules :app]
                     [:builds :app :dev :modules :app]
                     [:builds :test] [:builds :other]]]
    (t/is (thrown-with-msg?
            js/Error #"Shadow JavaScript text inputs require explicit migration inventory support"
            (inspect! (assoc-in {} (conj placement setting) "console.log('uninspected');"))))))

(t/deftest empty-base-text-cannot-hide-release-text
  (doseq [setting [:prepend :append :prepend-js :append-js]]
    (t/is (thrown-with-msg?
            js/Error #"Shadow JavaScript text inputs require explicit migration inventory support"
            (inspect! {:builds {:app {:modules {:app {setting ""}}
                                      :release {:modules {:app {setting "console.log('release');"}}}}}})))))

(t/deftest exact-existing-test-environment-remains-supported
  (let [bootstrap (current-test-bootstrap)]
    (t/is (string? bootstrap))
    (t/is (= {} (inspect! {:builds {:test {:target :node-test :prepend-js bootstrap}}})))
    (t/is (thrown-with-msg?
            js/Error #"Shadow JavaScript text inputs require explicit migration inventory support"
            (inspect! {:builds {:test {:prepend-js (str bootstrap "console.log('extra');")}}})))))

(t/deftest test-bootstrap-exception-does-not-apply-to-other-inputs
  (doseq [placement [[:builds :app :modules :app :prepend-js]
                     [:builds :test :append-js]
                     [:builds :test :release :prepend-js]
                     [:builds :other :prepend-js]]]
    (t/is (thrown-with-msg?
            js/Error #"Shadow JavaScript text inputs require explicit migration inventory support"
            (inspect! (assoc-in {} placement (current-test-bootstrap)))))))

(t/deftest absent-and-empty-shadow-text-remains-supported
  (t/is (= {} (inspect! {})))
  (doseq [setting [:prepend :append :prepend-js :append-js]
          value [nil ""]]
    (t/is (= {} (inspect! {:builds {:app {:modules {:app {setting value}}
                                        :release {setting value}}}})))))
