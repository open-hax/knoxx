(ns knoxx.frontend.law.migration-vitest-modules-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration-vitest-modules :as law]))

(t/deftest builtin-selectors-are-admitted-as-pure-facts
  (let [facts {:path "vitest.config.ts"
               :settings {"environment" "jsdom" "reporters" ["default"]
                          "coverage" {"provider" "v8" "reporter" ["text" "json-summary" "lcov"]}}}]
    (t/is (= facts (law/assert-config! facts)))))

(t/deftest custom-module-rejections-identify-the-selector
  (let [error (try
                (law/assert-config! {:path "vitest.config.ts"
                                     :settings {"reporters" ["../legacy/reporter.ts"]}})
                nil
                (catch js/Error error error))]
    (t/is (= {:path "vitest.config.ts" :field "reporters" :value "../legacy/reporter.ts"}
             (ex-data error)))))
