(ns knoxx.backend.agent-templates-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agent-templates :as templates]))

(deftest render-prompt-renders-json-round-tripped-template-vector
  (testing "template forms survive JSON/JS round-trips that turn lists/symbols into vectors/strings"
    (is (= "alpha\n\nbeta\n\ngamma"
           (templates/render-prompt
            ["template" {:separator "\n\n"}
             ["alpha"
              ["template" {:separator "\n\n"} ["beta" "gamma"]]]]
            {}
            nil
            nil)))))

(deftest render-prompt-preserves-ordinary-vector-as-data-string
  (testing "non-template vectors are not treated as executable forms"
    (is (= "[\"not-template\" \"x\"]"
           (templates/render-prompt ["not-template" "x"] {} nil nil)))))
