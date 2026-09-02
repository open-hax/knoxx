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

(deftest repository-inventory-round-trips-through-the-checked-in-ledger
  (let [generated (manifest/current-records)
        committed (manifest/parse-records (manifest/read-manifest))
        git-head (manifest/base-manifest "HEAD")]
    (is (= committed generated)
        "filesystem discovery, bridge/route parsing, and assembly match the ledger")
    (is (= committed git-head)
        "the Git adapter retrieves the exact committed ND-EDN records")
    (is (empty? (manifest/changed-paths "HEAD"))
        "the Git diff adapter reports no paths against the same revision")))
