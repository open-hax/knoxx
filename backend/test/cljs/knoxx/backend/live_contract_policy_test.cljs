(ns knoxx.backend.live-contract-policy-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is testing]]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(defn- fixture
  [relative-path]
  (reader/read-string (.readFileSync fs (.join path "test" "fixtures" "contracts" relative-path) "utf8")))

(deftest ussyverse-social-replies-keeps-visible-output-budget
  (testing "Discord reply agents must not burn the whole turn in hidden reasoning"
    (let [agent (fixture "agents/ussyverse_social_replies.edn")]
      (is (= "gemma4:31b" (get-in agent [:agent :model])))
      (is (= :medium (get-in agent [:agent :thinking]))))))
