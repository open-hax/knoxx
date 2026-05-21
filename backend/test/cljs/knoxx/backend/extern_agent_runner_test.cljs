(ns knoxx.backend.extern-agent-runner-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.agent-runner :as agent-runner]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.extern.json :as xjson]))

(deftest to-cljs-map-keywordizes-js-runner-inputs
  (testing "JS payload fragments become CLJS maps at the runner boundary"
    (is (= {:agent_spec {:contract_id "discord_mention_response"
                         :tool_policies [{:tool_id "discord.read"}]}}
           (agent-runner/to-cljs-map
            (xjs/object
             {:agent_spec {:contract_id "discord_mention_response"
                           :tool_policies [{:tool_id "discord.read"}]}}))))))

(deftest error-diagnostic-extracts-js-error-fields
  (testing "diagnostics read JS error fields without leaking aget to runner"
    (let [err (xjs/object {:name "SpawnError"
                           :message "boom"
                           :stack "stack trace"})
          diagnostic (agent-runner/error-diagnostic
                      {:run-id "run-1"
                       :conversation-id "conversation-1"
                       :session-id "session-1"
                       :model "model-1"
                       :agent-spec {:contract-id "contract-1"
                                    :actor-id "actor-1"}}
                      err)]
      (is (= {:message "boom"
              :name "SpawnError"
              :stack "stack trace"
              :runId "run-1"
              :conversationId "conversation-1"
              :sessionId "session-1"
              :model "model-1"
              :contractId "contract-1"
              :actorId "actor-1"}
             diagnostic)))))

(deftest safe-json-stringifies-cljs-values
  (testing "JSON stringify stays at the extern boundary"
    (is (= {:ok true :count 2}
           (xjson/parse-object
            (agent-runner/safe-json {:ok true :count 2}))))))
