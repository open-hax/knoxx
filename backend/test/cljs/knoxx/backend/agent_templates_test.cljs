(ns knoxx.backend.agent-templates-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.agent.agent-templates :as templates]))

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

(deftest render-prompt-renders-json-round-tripped-expression-vectors
  (testing "nested keyword calls, conditionals, map, and fn forms survive JSON/JS vectorization"
    (is (= "- TRUSTED_DISCORD_INPUT hello\n- UNTRUSTED_DISCORD_QUOTE world"
           (templates/render-prompt
            ["template" {:separator "\n"}
             [["if" ["=" 0 ["count" ["source" "messages" "ctx"]]]
               "empty"
               ["template" {:separator "\n"}
                ["map" ["source" "messages" "ctx"]
                 ["fn" ["msg"]
                  ["template" {:separator " "}
                   ["-"
                    ["if" ["trusted?" "msg"] "TRUSTED_DISCORD_INPUT" "UNTRUSTED_DISCORD_QUOTE"]
                    ["text" "msg"]]]]]]]]]
            {}
            nil
            {:source {:messages [{:trusted? true :text "hello"}
                                  {:trusted? false :text "world"}]}})))))

(deftest render-prompt-preserves-ordinary-vector-as-data-string
  (testing "non-template vectors are not treated as executable forms"
    (is (= "[\"not-template\" \"x\"]"
           (templates/render-prompt ["not-template" "x"] {} nil nil)))))
