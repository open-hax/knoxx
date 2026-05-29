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
                                                (swap! appended* conj (js->clj message :keywordize-keys true)))}
        result (await (history/rehydrate-session-manager! message-source
                                                          session-manager
                                                          "conv-ports"
                                                          {:system-prompt "system"}))]
      (is (true? (:restored result)))
      (is (= ["system" "user"] (mapv :role @appended*))))))

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

(deftest ^:async content-codec-materializes-remote-media-and-empty-parts
  (testing "remote fetches are converted to provider media maps and failed fetches reject"
    (let [original-fetch js/fetch
          bytes (js/Uint8Array.from #js [102 101 116 99 104 101 100 45 105 109 97 103 101])
          response-buffer (.-buffer bytes)
          expected-b64 (.toString (js/Buffer.from response-buffer) "base64")]
      (try
        (set! js/fetch
              (fn [url]
                (js/Promise.resolve
                 (if (= "https://example.test/image.png" url)
                   #js {:ok true
                        :arrayBuffer (fn [] (js/Promise.resolve response-buffer))}
                   #js {:ok false
                        :status 503
                        :arrayBuffer (fn [] (js/Promise.resolve response-buffer))}))))
        (is (= {:type "image"
                :data expected-b64
                :mimeType "image/png"}
               (await (content-codec/materialize! {:type "image"
                                                   :url "https://example.test/image.png"}))))
        (is (nil? (await (content-codec/materialize! {:type "image"}))))
        (try
          (await (content-codec/materialize! {:type "audio"
                                             :url "https://example.test/down.mp3"
                                             :mimeType "audio/mpeg"}))
          (is false "expected remote media fetch to reject")
          (catch :default err
            (is (re-find #"audio/mpeg fetch failed: 503" (.-message err)))))
        (finally
          (set! js/fetch original-fetch))))))

(deftest content-codec-converts-provider-content-parts
  (let [codec content-codec/default-content-codec
        stored [{:type "text" :text "hello"}
                {:type "audio" :data "raw" :mimeType "audio/wav"}]
        provider (content-codec/content-parts->provider codec stored)]
    (is (= 2 (count provider)))
    (is (= stored (content-codec/provider->content-parts codec stored)))
    (is (= [] (content-codec/content-parts->provider codec nil)))
    (is (= [] (content-codec/provider->content-parts codec nil)))))

(deftest tool-auth-context-and-runtime-names-are-deterministic
  (is (nil? (tool-catalog/effective-tool-auth-context nil #{"read"})))
  (is (= {:userId "u"
          :toolPolicies [{:toolId "memory.search" :effect "allow"}
                         {:toolId "read" :effect "allow"}]}
         (tool-catalog/effective-tool-auth-context {:userId "u"} #{"read" "memory.search"})))
  (is (= ["read" "write" "memory.search"]
         (tool-catalog/tool-runtime-names [" read " #js {:name "write"}]
                                          #js [#js {:id "memory.search"}
                                               #js {:label "write"}]))))

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
