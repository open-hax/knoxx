(ns knoxx.backend.agents.content-history-tooling-ports-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.content-codec :as content-codec]
            [knoxx.backend.infra.agent.history :as history]
            [knoxx.backend.infra.agent.tool-catalog :as tool-catalog]
            [knoxx.backend.infra.stores.message-source :refer [IMessageSource]]))

(deftest ^:async history-prunes-context-and-rehydrates-through-message-source
  (testing "context pruning preserves system messages and keeps latest body messages"
    (is (= [{:role "system" :content "rules"}
            {:role "assistant" :content "two"}
            {:role "user" :content "three"}]
           (history/prune-session-messages
            {:context-policy {:max-messages 2 :preserve-system true}}
            [{:role "system" :content "rules"}
             {:role "user" :content "one"}
             {:role "assistant" :content "two"}
             {:role "user" :content "three"}]))))
  (testing "rehydration loads through IMessageSource and appends provider messages"
    (let [appended* (atom [])
          message-source (reify IMessageSource
                           (fetch-messages! [_ conversation-id]
                             (is (= "conv-ports" conversation-id))
                             (js/Promise.resolve [{:role "user" :content "hello"}])))
          session-manager #js {:appendMessage (fn [message]
                                                (swap! appended* conj (js->clj message :keywordize-keys true)))}]
      (let [result (await (history/rehydrate-session-manager! message-source
                                                               session-manager
                                                               "conv-ports"
                                                               {:system-prompt "system"}))]
        (is (true? (:restored result)))
        (is (= ["system" "user"] (mapv :role @appended*)))))))

(deftest ^:async history-rehydrate-rejects-when-message-source-is-down
  (testing "history restore is a hard dependency for transcript correctness"
    (let [appended* (atom [])
          message-source (reify IMessageSource
                           (fetch-messages! [_ _conversation-id]
                             (js/Promise.reject (js/Error. "OpenPlanner request failed"))))
          session-manager #js {:appendMessage (fn [message]
                                                (swap! appended* conj (js->clj message :keywordize-keys true)))}]
      (try
        (await (history/rehydrate-session-manager! message-source
                                                   session-manager
                                                   "conv-fail-closed"
                                                   {:system-prompt "system"}))
        (is false "expected history restore to reject")
        (catch :default err
          (is (re-find #"OpenPlanner request failed" (.-message err)))
          (is (= [] @appended*)))))))

(deftest ^:async content-codec-materializes-existing-media-shapes
  (testing "data URLs and raw base64 preserve eta-mu media shape"
    (let [materializer content-codec/default-media-materializer
          image-media (await (content-codec/materialize-media! materializer
                                                               {:type "image"
                                                                :data "data:image/png;base64,abc"
                                                                :mimeType "image/png"}
                                                               nil))
          audio-media (await (content-codec/materialize-media! materializer
                                                               {:type "audio"
                                                                :data "raw-audio"
                                                                :mimeType "audio/mpeg"}
                                                               nil))]
      (is (= {:type "image" :data "abc" :mimeType "image/png"} image-media))
      (is (= {:type "audio"
              :data "raw-audio"
              :mimeType "audio/mpeg"
              :format "mp3"}
             audio-media)))))

(deftest tool-policy-and-catalog-ports-preserve-visibility
  (testing "allowed tool ids and auth context are resolved behind ports"
    (with-redefs [tool-catalog/allowed-tool-ids (fn [_config auth-context agent-spec]
                                                  (is (= {:userId "user-1"} auth-context))
                                                  (is (= {:role "assistant"
                                                          :contract-id "agent/default"
                                                          :actor-id "actor/default"}
                                                         agent-spec))
                                                  #{"read" "memory.search"})
                  tool-catalog/builtin-tools (fn [_runtime _config tool-auth-context agent-spec]
                                               (is (= [{:toolId "memory.search" :effect "allow"}
                                                       {:toolId "read" :effect "allow"}]
                                                      (:toolPolicies tool-auth-context)))
                                               (is (= "assistant" (:role agent-spec)))
                                               ["read"])
                  tool-catalog/custom-tools (fn [_runtime _config _tool-auth-context _agent-spec _allowed-tool-ids]
                                              nil)]
      (let [agent-spec {:role "assistant"
                        :contract-id "agent/default"
                        :actor-id "actor/default"}
            resolver (tool-catalog/tool-policy-resolver {})
            catalog (tool-catalog/tool-catalog #js {} {})
            allowed (tool-catalog/allowed-tools resolver {:userId "user-1"} agent-spec nil)
            visible (tool-catalog/available-tools catalog {:auth-context {:userId "user-1"}
                                                           :agent-spec agent-spec})
            signature (tool-catalog/visible-session-signature #js {} {} {:userId "user-1"} agent-spec)]
        (is (= #{"read" "memory.search"} allowed))
        (is (= ["read"] (:builtin-tools visible)))
        (is (= #{"read" "memory.search"} (:allowed-tool-ids visible)))
        (is (re-find #":tools \[\"read\"\]" signature))))))
