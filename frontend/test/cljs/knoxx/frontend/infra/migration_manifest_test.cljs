(ns knoxx.frontend.infra.migration-manifest-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(deftest newline-edn-admits-exactly-one-canonical-form-per-line
  (let [line "{:record/id \"one\", :kind :route}"]
    (is (= [{:record/id "one" :kind :route}]
           (manifest/parse-records (str line "\n"))))
    (is (thrown-with-msg?
         js/Error
         #"canonical single-form EDN"
         (manifest/parse-records (str line " {:trailing true}\n"))))))
