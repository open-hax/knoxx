(ns knoxx.backend.shape-agent-schemas-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.agent.message :as message]
            [knoxx.backend.shape.agent.provider :as provider]
            [knoxx.backend.shape.agent.recovery :as recovery]
            [knoxx.backend.shape.agent.runtime :as runtime]
            [knoxx.backend.shape.agent.turn :as turn]
            [knoxx.backend.shape.app-shapes :as app-shapes]
            [knoxx.backend.shape.session-persistence :as persistence]
            [malli.core :as m]))

(deftest normalized-chat-body-validates-as-agent-turn-input
  (testing "normalize-chat-body output is accepted as a turn input"
    (let [body (app-shapes/normalize-chat-body
                #js {:message "hello with image https://cdn.discordapp.com/a.png"
                     :conversationId "conv-1"
                     :sessionId "sess-1"
                     :runId "run-1"
                     :model "openai/gpt-4.1"
                     :thinkingLevel "medium"
                     :mode "agent"
                     :contentParts #js [#js {:type "image_url"
                                             :url "https://example.test/cat.png"
                                             :mimeType "image/png"}]
                     :agentSpec #js {:contractId "agent/default"
                                     :actorId "actor/default"
                                     :role "assistant"
                                     :toolPolicies #js [#js {:toolId "memory.search"
                                                            :effect "allow"}]}
                     :templateContext #js {:topic "schemas"}
                     :authContext #js {:user_id "user-1"}})]
      (is (m/validate agent/ChatBody body))
      (is (m/validate turn/AgentTurnInput body))
      (is (m/validate agent/AgentRequestSpec (:agent-spec body)))
      (is (= :image (get-in body [:content-parts 0 :type]))))))

(deftest normalized-chat-body-accepts-cljs-request-maps
  (testing "Fastify request-body normalizes JS bodies into CLJS maps before app-shape parsing"
    (let [body (app-shapes/normalize-chat-body
                {:message "hello from cljs map"
                 :conversation-id "conv-map"
                 :session-id "sess-map"
                 :run-id "run-map"
                 :thinking-level "low"
                 :content-parts [{:type "text" :text "attached text"}]
                 :template-context {:topic "maps"}
                 :auth-context {:user-id "user-map"}
                 :agent-spec {:actor-id "actor-map"}})]
      (is (= "hello from cljs map" (:message body)))
      (is (= "conv-map" (:conversation-id body)))
      (is (= "sess-map" (:session-id body)))
      (is (= "run-map" (:run-id body)))
      (is (= "low" (:thinking-level body)))
      (is (= "attached text" (get-in body [:content-parts 0 :text])))
      (is (= {:topic "maps"} (:template-context body)))
      (is (= {:user-id "user-map"} (:auth-context body)))
      (is (= "actor-map" (get-in body [:agent-spec :actor-id]))))))

(deftest normalized-control-body-validates
  (testing "normalize-control-body output has an explicit control shape"
    (let [body (app-shapes/normalize-control-body
                #js {:message "please stop after this sentence"
                     :conversationId "conv-2"
                     :sessionId "sess-2"
                     :runId "run-2"
                     :actorId "actor/default"
                     :metadata #js {:source "test"}})]
      (is (m/validate agent/ControlBody body))
      (is (= {:source "test"} (:metadata body))))))

(deftest message-turn-provider-and-recovery-fixtures-validate
  (testing "representative agent message and turn maps validate"
    (let [agent-message {:role "user"
                         :content "Summarize this"
                         :content-parts [{:type :text :text "Summarize this"}]}
          transcript-message {:role "assistant"
                              :content "Summary"
                              :at "2026-05-21T00:00:00Z"}
          turn-state {:session-id "sess-3"
                      :conversation-id "conv-3"
                      :run-id "run-3"
                      :status "running"
                      :has-active-stream true
                      :messages [agent-message]}
          turn-result {:status "completed"
                       :answer "Summary"
                       :messages [{:role "assistant" :content "Summary"}]}
          stream-event {:kind :text
                        :delta "Sum"
                        :at "2026-05-21T00:00:01Z"}
          provider-request {:model "openai/gpt-4.1"
                            :messages [agent-message]
                            :stream true}
          provider-response {:answer "Summary"
                             :usage {:input-tokens 10
                                     :output-tokens 3
                                     :total-tokens 13}}
          recovery-record {:session-id "sess-3"
                           :conversation-id "conv-3"
                           :run-id "run-3"
                           :status :resumed
                           :reason :startup}]
      (is (m/validate message/AgentMessage agent-message))
      (is (m/validate message/TranscriptMessage transcript-message))
      (is (m/validate turn/AgentTurnState turn-state))
      (is (m/validate turn/AgentTurnResult turn-result))
      (is (m/validate turn/StreamEvent stream-event))
      (is (m/validate provider/ProviderRequest provider-request))
      (is (m/validate provider/ProviderResponse provider-response))
      (is (m/validate recovery/RecoveryRecord recovery-record)))))

(deftest representative-session-store-run-still-validates
  (testing "KnoxxRun remains the persistence baseline"
    (let [run {:run_id "run-4"
               :session_id "sess-4"
               :conversation_id "conv-4"
               :status "running"
               :created_at "2026-05-21T00:00:00Z"
               :updated_at "2026-05-21T00:00:01Z"
               :model "openai/gpt-4.1"
               :messages [{:role "user" :content "hello"}
                          {:role "assistant" :content "hi"}]
               :tool_receipts [{:id "receipt-1"
                                :tool_name "memory.search"
                                :status "done"
                                :input_preview "query"
                                :result_preview "result"}
                               {:id "receipt-2"
                                :tool_name "memory.search"
                                :status "completed"
                                :result_preview "result"}]
               :trace_blocks [{:id "trace-1"
                               :kind :tool_call
                               :status "done"
                               :at "2026-05-21T00:00:01Z"}]
               :has_active_stream true
               :org_id "org-1"
               :user_id "user-1"}]
      (is (persistence/valid-run? run))
      (is (m/validate persistence/KnoxxRun run)))))

(deftest runtime-boundary-shapes-validate-representative-maps
  (testing "agent runtime request and policy shapes cover current route inputs"
    (is (m/validate runtime/AgentSpec
                    {:contract-id "agent/default"
                     :actor-id "actor/default"
                     :contract-actors [{:actorId "actor/default"}]
                     :role "assistant"
                     :model "openai/gpt-4.1"
                     :thinking-level "medium"
                     :tool-policies [{:toolId "memory.search" :effect "allow"}]
                     :context-policy {:max-messages 20}
                     :sub-agent-id "child-1"
                     :parent-agent-id "parent-1"
                     :parent-run-id "run-parent"
                     :spawn-kind "direct"}))
    (is (m/validate runtime/DirectStartPayload
                    {:message "go"
                     :conversationId "conv-5"
                     :sessionId "sess-5"
                     :runId "run-5"
                     :thinkingLevel "low"
                     :contentParts [{:type "text" :text "go"}]
                     :agentSpec {:contractId "agent/default"}}))
    (is (m/validate runtime/AuthContext
                    {:orgId "org-1"
                     :orgSlug "default"
                     :userId "user-1"
                     :userEmail "user@example.test"
                     :membershipId "member-1"
                     :actorId "actor/default"
                     :roleSlugs ["admin"]
                     :permissions ["agents:run"]
                     :toolPolicies [{:toolId "memory.search" :effect "allow"}]
                     :membershipToolPolicies [{:toolId "discord.send" :effect "deny"}]
                     :isSystemAdmin false
                     :user {:id "user-1"}
                     :org {:id "org-1"}
                     :membership {:id "member-1"}}))
    (is (m/validate runtime/ContextPolicy
                    {:max-messages 12
                     :maxChars 8192
                     :preserve_system true}))
    (is (m/validate runtime/PolicyConstraints
                    {:allowedModels ["openai/gpt-4.1"]
                     :maxRequests 100
                     :window-seconds 3600})))
  (testing "message/content/provider shapes cover stored and eta-mu payloads"
    (is (m/validate runtime/StoredMessage
                    {:role "compactionSummary"
                     :summary "Earlier context"
                     :tokensBefore 12000
                     :content-parts [{:type :text :text "Earlier context"}]}))
    (is (m/validate runtime/ProviderAgentMessage
                    {"role" "assistant"
                     "content" [{:type "image" :data "abc" :mimeType "image/png"}]
                     "text" "done"
                     "summary" "summary"
                     "usage" {:total_tokens 10}
                     "reasoning" "because"}))
    (is (m/validate runtime/EtaMuAttachment
                    {:type "image" :data "abc" :mimeType "image/png" :filename "cat.png"})))
  (testing "stream/run/session shapes cover current mutable runtime maps"
    (is (m/validate runtime/ActiveSessionEntry
                    {:session #js {}
                     :model-id "openai/gpt-4.1"
                     :tool-signature "sig"
                     :session-id "sess-6"
                     :actor-id "actor/default"
                     :last-accessed 42}))
    (is (m/validate runtime/RuntimeSetup
                    {:auth-storage #js {}
                     :model-registry #js {}
                     :settings-manager #js {}
                     :loader #js {}
                     :runtime-dir "/tmp/runtime"}))
    (is (m/validate runtime/VisibleSessionSignature
                    {:tools ["memory.search"]
                     :contract-id "agent/default"
                     :actor-id "actor/default"
                     :role "assistant"
                     :system-prompt "Be useful"
                     :task-prompt "Do the thing"}))
    (is (m/validate runtime/StreamState
                    {:run-id "run-6"
                     :conversation-id "conv-6"
                     :session-id "sess-6"
                     :started-at "2026-05-21T00:00:00Z"
                     :started-ms 1
                     :random-uuid! (fn [] "uuid")
                     :chunks (atom [])
                     :reasoning-chunks (atom [])
                     :ttft-recorded? (atom false)
                     :last-assistant-text* (atom "")
                     :last-reasoning-text* (atom "")
                     :replay-suppression* (atom {})
                     :think-tag-mode* (atom :off)
                     :aborting? (atom false)
                     :abort-reason* (atom nil)
                     :tool-loop* (atom {})
                     :seen-tool-lifecycle-events* (atom #{})})))
    (is (m/validate runtime/ProviderStreamEvent
                    {:type "tool_execution_end"
                     :toolName "memory.search"
                     :toolCallId "call-1"
                     :isError false
                     :result "ok"}))
    (is (m/validate runtime/CanonicalRunEvent
                    {:run_id "run-6"
                     :conversation_id "conv-6"
                     :session_id "sess-6"
                     :type "tool_end"
                     :at "2026-05-21T00:00:01Z"
                     :status "completed"
                     :tool_name "memory.search"
                     :tool_call_id "call-1"}))
    (is (m/validate runtime/TokenEvent
                    {:run_id "run-6"
                     :conversation_id "conv-6"
                     :session_id "sess-6"
                     :kind "assistant_message"
                     :token "hello"}))
    (is (m/validate runtime/SessionStoreRecord
                    {:session_id "sess-6"
                     :conversation_id "conv-6"
                     :run_id "run-6"
                     :status "running"
                     :model "openai/gpt-4.1"
                     :mode "direct"
                     :thinking_level "medium"
                     :created_at "2026-05-21T00:00:00Z"
                     :updated_at "2026-05-21T00:00:01Z"
                     :has_active_stream true
                     :messages [{:role "user" :content "hello"}]
                     :auth_snapshot {:userId "user-1"}
                     :agent_spec {:contractId "agent/default"}}))
  (testing "runtime response and recovery shapes cover route responses"
    (is (m/validate runtime/TurnResponse
                    {:answer "ok"
                     :run_id "run-7"
                     :runId "run-7"
                     :conversation_id "conv-7"
                     :conversationId "conv-7"
                     :session_id "sess-7"
                     :sessionId "sess-7"
                     :model "openai/gpt-4.1"
                     :content_parts [{:type "text" :text "ok"}]
                     :sources []
                     :message_parts [{:role "assistant" :content "ok"}]
                     :compare nil}))
    (is (m/validate runtime/AcceptedResponse
                    {:ok true
                     :queued true
                     :run_id "run-8"
                     :runId "run-8"
                     :conversation_id "conv-8"
                     :conversationId "conv-8"
                     :session_id "sess-8"
                     :sessionId "sess-8"
                     :model "openai/gpt-4.1"}))
    (is (m/validate runtime/RecoveryRequest
                    {:session-id "sess-9"
                     :conversation-id "conv-9"
                     :run-id "run-9"
                     :reason :manual
                     :wait-for 1000}))
    (is (m/validate runtime/RecoveryResult
                    {:session_id "sess-9"
                     :conversation_id "conv-9"
                     :resumed true
                     :wait_for 1000}))))
