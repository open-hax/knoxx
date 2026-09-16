(ns knoxx.frontend.law.migration-vite-entry-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration-vite-entry :as law]))

(t/deftest default-entry-admission-requires-the-inspected-html-path
  (let [facts {:path "vite.config.ts" :expected "/frontend/index.html"
               :entries ["/frontend/index.html"]}]
    (t/is (= facts (law/assert-default-entry! facts)))
    (doseq [entries [[] ["/frontend/legacy/index.html"]
                     ["/frontend/index.html" "/frontend/legacy/index.html"]]]
      (t/is (thrown-with-msg? js/Error #"Default Vite entry leaves governed HTML inventory"
                             (law/assert-default-entry! (assoc facts :entries entries)))))))
