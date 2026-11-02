(ns knoxx.backend.lens-topics-test
  (:require [cljs.test :refer [async deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.contracts.resolve :as contract-resolve]
            [knoxx.backend.domain.lens.topics :as lens-topics]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.lens.tools :as lens-tools]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            [knoxx.backend.law.lens-topics :as lens-topics-law]
            [knoxx.backend.law.mcp-tool-annotations :as annotations]))

(deftest normalize-refinement-keeps-only-nouns-and-proper-nouns
  (testing "rejects verbs, prepositions, adjectives, and filler words while keeping crisp nouns/proper nouns"
    (let [result (lens-topics/normalize-refinement
                  {:channelId "12345"
                   :channelName "general"
                   :topics [{:term "OpenPlanner" :kind "proper_noun" :salience 0.95}
                            {:term "found" :kind "noun" :salience 0.9}
                            {:term "work" :kind "noun" :salience 0.88}
                            {:term "using" :kind "noun" :salience 0.85}
                            {:term "via" :kind "noun" :salience 0.82}
                            {:term "facinating" :kind "noun" :salience 0.80}
                            {:term "BetterDiscord" :kind "proper_noun" :salience 0.91}
                            {:term "WebSocket relay" :kind "noun" :salience 0.84}
                            {:term "openplanner" :kind "proper_noun" :salience 0.5}]
                   :droppedTerms ["still" "free"]})
          formatted (lens-topics/format-refinement-result-text result)
          envelope (second (re-find #"<refined_topics>([\s\S]*?)</refined_topics>" formatted))
          decoded (xtools/parse-json envelope)]
      (is (= ["OpenPlanner" "BetterDiscord" "WebSocket relay"]
             (:topicTerms result)))
      (is (every? #(contains? (set (:droppedTerms result)) %)
                  ["found" "work" "using" "via" "facinating" "still" "free"]))
      (is (= "12345" (:channelId decoded)))
      (is (= ["OpenPlanner" "BetterDiscord" "WebSocket relay"]
             (:topicTerms decoded))))))

(deftest lens-refine-topics-contract-and-tool-execution
  (testing "schema, MCP annotations, registry, and knoxx_lens_companion contract expose lens_refine_topics"
    (is (lens-topics-law/valid-params?
         {:topics [{:term "OpenPlanner" :kind "proper_noun" :salience 0.9}]
          :droppedTerms ["using" "via"]}))
    (is (= {:readOnlyHint true :openWorldHint false}
           (annotations/for-tool "lens_refine_topics")))
    (is (= "lens_refine_topics"
           (:id (tool-registry/get-tool "lens_refine_topics"))))
    (let [resolved (contract-resolve/resolve-agent-contract
                    {:contracts-dir "../contracts"}
                    "knoxx_lens_companion"
                    "knoxx_lens")]
      (is (contains? (set (:tool-ids resolved)) "lens_refine_topics")))))

(deftest lens-refine-topics-tool-runs-end-to-end
  (async done
    ((^:async fn []
       (try
         (let [tools (lens-tools/create-tools {} {} {:toolPolicies [{:toolId "lens_refine_topics" :effect "allow"}]})
               tool (first tools)
               execute (aget tool "execute")
               res (await (execute "call-1"
                                   #js {:topics #js [#js {:term "KnoxxLens" :kind "proper_noun" :salience 0.96}
                                                     #js {:term "using" :kind "noun" :salience 0.5}]
                                        :droppedTerms #js ["via"]}
                                   nil nil nil))
               text (aget (aget (aget res "content") 0) "text")]
           (is (= 1 (count tools)))
           (is (str/includes? text "KnoxxLens"))
           (is (str/includes? text "<refined_topics>"))
           (is (not (str/includes? text "\"topicTerms\":[\"KnoxxLens\",\"using\"]"))))
         (finally
           (done)))))))
