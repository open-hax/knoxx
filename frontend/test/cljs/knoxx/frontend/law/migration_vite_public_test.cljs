(ns knoxx.frontend.law.migration-vite-public-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration-vite-public :as law]))

(t/deftest copied-public-assets-require-the-inspected-directory
  (let [facts {:path "vite.config.ts" :expected "/frontend/public"}]
    (doseq [actual [nil "/frontend/public"]]
      (let [allowed (assoc facts :actual actual)]
        (t/is (= allowed (law/assert-public-directory! allowed)))))
    (doseq [actual ["/frontend/legacy-public" "/frontend/public/nested" "/legacy/public"]]
      (t/is (thrown-with-msg? js/Error #"Vite public directory leaves governed HTML inventory"
                             (law/assert-public-directory! (assoc facts :actual actual)))))))
