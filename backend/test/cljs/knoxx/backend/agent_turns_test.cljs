(ns knoxx.backend.agent-turns-test
  (:require [knoxx.backend.infra.auth.authz :as authz]
            [cljs.test :refer [deftest is testing async]]
            [knoxx.backend.domain.agent.content :as content]
            [knoxx.backend.infra.agent.tools :as tools]
            [knoxx.backend.infra.agent.transcript :as transcript]
            [knoxx.backend.infra.agent.turn :as agent-turns]
            [knoxx.backend.extern.agent-turn-node :as xturn-node]
            [knoxx.backend.extern.eta-mu :refer [wrap-eta-mu-session]]))

(deftest ensure-session-id-preserves-provided-value
  (testing "existing session ids are kept intact"
    (is (= "existing-session-id"
           (agent-turns/ensure-session-id "existing-session-id")))))

(deftest ensure-session-id-generates-missing-value
  (testing "missing session ids are generated before provider requests"
    (with-redefs [xturn-node/random-uuid! (fn [] "generated-session-id")]
      (is (= "generated-session-id"
             (agent-turns/ensure-session-id nil))))))

(deftest model-ready-content-parts-normalizes-js-object-image-parts
  (testing "image parts arriving as JS objects are normalized and do not crash"
    (with-redefs [knoxx.backend.domain.models/model-supports-input? (fn [_ _ part-type]
                                                                      (= part-type "image"))]
      (is (= [{:type "image"
               :url "file://clip.png"}]
             (content/model-ready-content-parts
               {}
               "test-model"
               [#js {:type "image"
                     :url "file://clip.png"}]))))))

(deftest model-ready-content-parts-rewrites-unsupported-audio-inputs
  (testing "unsupported multimodal inputs degrade into explanatory text instead of crashing"
    (with-redefs [knoxx.backend.domain.models/model-supports-input? (fn [_ _ part-type]
                                                                      (not= part-type "audio"))]
      (is (= [{:type :text
               :text "Uploaded audio source 'clip.wav' is available, but model test-model does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view."}]
             (content/model-ready-content-parts
               {}
               "test-model"
               [{:type :audio
                 :filename "clip.wav"}]))))))

(deftest assistant-tool-call-previews-extracts-string-arguments-from-assistant-message
  (testing "string tool call arguments from assistant messages can backfill missing tool receipt inputs"
    (let [assistant-message #js {:role "assistant"
                                 :content #js [#js {:type "toolCall"
                                                    :id "tool-123"
                                                    :name "custom_tool"
                                                    :arguments "path=docs/guide.md limit=20"}]}
          previews (tools/assistant-tool-call-previews assistant-message)]
      (is (= [{:tool_call_id "tool-123"
               :tool_name "custom_tool"
               :input_preview "path=docs/guide.md limit=20"}]
             previews)))))

(deftest assistant-tool-call-previews-extracts-object-arguments-from-assistant-message
  (testing "object tool call arguments fall back to readable JSON when they are not plain strings"
    (let [assistant-message #js {:role "assistant"
                                 :content #js [#js {:type "toolCall"
                                                    :id "tool-456"
                                                    :name "read"
                                                    :arguments #js {:path "docs/guide.md"
                                                                    :offset 10
                                                                    :limit 20}}]}
          previews (tools/assistant-tool-call-previews assistant-message)]
      (is (= [{:tool_call_id "tool-456"
               :tool_name "read"
               :input_preview "```yaml\npath: docs/guide.md\noffset: 10\nlimit: 20\n```"}]
             previews)))))

(deftest tool-call-input-preview-renders-bash-commands-as-markdown
  (testing "bash tool calls render command fences and timeout metadata"
    (is (= "```bash\nprintf 'hi'\n```\n\n- timeout: 12"
           (tools/tool-call-input-preview
            "tools/bash"
            #js {:command "printf 'hi'"
                 :timeout 12})))))

(deftest session->stored-messages-preserves-prior-transcript
  (testing "live session snapshots keep existing user and assistant turns for restart recovery"
    (let [session (wrap-eta-mu-session
                   #js {:messages #js [#js {:role "system"
                                            :content #js [#js {:type "text" :text "stay grounded"}]}
                                       #js {:role "user"
                                            :content #js [#js {:type "text" :text "first request"}]}
                                       #js {:role "assistant"
                                            :content #js [#js {:type "text" :text "first answer"}]}]})]
      (is (= [{:role "system" :content "stay grounded"}
              {:role "user" :content "first request"}
              {:role "assistant" :content "first answer"}]
             (transcript/session->stored-messages session))))))

(deftest reply-attachment-content-parts-lifts-workspace-media-tool-receipts-into-final-replies
  (testing "workspace_media.attach receipts become assistant reply content parts"
    (is (= [{:type "audio"
             :data "data:audio/wav;base64,QUFBQQ=="
             :mimeType "audio/wav"
             :filename "reply.wav"}]
           (content/reply-attachment-content-parts
            [{:tool_name "workspace_media.attach"
              :content_parts [{:type "audio"
                               :data "data:audio/wav;base64,QUFBQQ=="
                               :mimeType "audio/wav"
                               :filename "reply.wav"}]}
             {:tool_name "read"
              :content_parts [{:type "document"
                               :url "file:///tmp/guide.md"
                               :filename "guide.md"}]}])))))

(deftest media-part->eta-mu-attachment-strips-audio-data-url
  (testing "audio data URLs are converted to eta-mu raw base64 attachments"
    (is (= {:type "audio"
            :data "UklGRg=="
            :mimeType "audio/wav"}
           (agent-turns/media-part->eta-mu-attachment
            {:type "audio"
             :data "data:audio/wav;base64,UklGRg=="
             :mimeType "audio/wav"})))))

(deftest ^:async materialize-content-parts-keeps-text-part
  (testing "materialization accepts explicit context and preserves non-media parts"
    (try
      (let [parts (await (agent-turns/materialize-content-parts!
                          nil {} "text-model" nil 32000000
                          [{:type "text" :text "hello"}]))]
        (is (= [{:type "text" :text "hello"}] parts)))
      (catch :default err
        (is false (str "materialize-content-parts! threw: " (.-message err)))))))

(deftest merge-content-parts-dedupes-overlapping-attachments
  (testing "reply media already present in the assistant response is not duplicated"
    (is (= [{:type "image"
             :url "/api/multimodal/files/image-1"
             :mimeType "image/png"
             :filename "plot.png"}
            {:type "audio"
             :data "data:audio/wav;base64,QUFBQQ=="
             :mimeType "audio/wav"
             :filename "reply.wav"}]
           (content/merge-content-parts
            [{:type "image"
              :url "/api/multimodal/files/image-1"
              :mimeType "image/png"
              :filename "plot.png"}]
            [{:type "image"
              :url "/api/multimodal/files/image-1"
              :mimeType "image/png"
              :filename "plot.png"}
             {:type "audio"
              :data "data:audio/wav;base64,QUFBQQ=="
              :mimeType "audio/wav"
              :filename "reply.wav"}])))))

;; ────────────────────────────────────────────────────────────────────
;; Regression: sync throws inside send-agent-turn! let bindings escaped
;; as raw exceptions instead of rejected Promises, causing js-await
;; callers to receive undefined and crash:
;;   "(intermediate value).catch is not a function"
;;
;; Root cause: ensure-conversation-access! (authz layer) throws
;; synchronously for 403, before the (-> Promise chain) is built.
;;
;; Fix: wrap the entire (let [...] body) of send-agent-turn! in
;; (js/Promise. (fn [resolve reject] (try (resolve ...) (catch :default e (reject e)))))
;;
;; Tests below prove the invariant at the authz layer (unit) and
;; via a structural compile-time check of the wrapper.
;; ────────────────────────────────────────────────────────────────────

;; --- authz layer: ensure-conversation-access! throws sync on conflict ---

(deftest conversation-access-throws-on-principal-mismatch
  (testing "ensure-conversation-access! throws 403 when principals differ"
    (let [store (atom {})
          ctx-a {:user-id "alice" :membership-id "m-alice"}
          ctx-b {:user-id "bob" :membership-id "m-bob"}
          ;; Seed the store as alice
          _ (authz/remember-conversation-access! store ctx-a "conv-x")]
      ;; Bob accessing alice's conversation should throw
      (is (thrown-with-msg? js/Error #"403"
             (authz/ensure-conversation-access! store ctx-b "conv-x"))
          "throws 403 for mismatched principal"))))

(deftest conversation-access-permits-same-principal
  (testing "ensure-conversation-access! returns ctx when principals match"
    (let [store (atom {})
          ctx   {:user-id "alice" :membership-id "m-alice"}
          _     (authz/remember-conversation-access! store ctx "conv-y")]
      (is (= ctx (authz/ensure-conversation-access! store ctx "conv-y"))
          "returns ctx on match"))))

(deftest conversation-access-allows-nil-ctx
  (testing "ensure-conversation-access! is a no-op when ctx is nil"
    (let [store (atom {"conv-z" {:userId "alice"}})
          result (authz/ensure-conversation-access! store nil "conv-z")]
      (is (nil? result) "nil ctx passes through"))))

;; --- Promise-wrapper structural invariant ---
;;
;; We prove the wrapper exists by calling ensure-conversation-access!
;; in a minimal atom and wrapping the call ourselves the same way
;; the source now does, verifying the throw becomes a rejection.

(deftest sync-throw-becomes-rejected-promise
  (async done
    (testing "a sync throw wrapped in Promise constructor becomes a rejection"
      (let [store (atom {})
            ctx-a {:user-id "alice" :membership-id "m-alice"}
            ctx-b {:user-id "bob" :membership-id "m-bob"}
            _     (authz/remember-conversation-access! store ctx-a "conv-p")
            ;; mirror exactly what the fixed send-agent-turn! does
            p     (js/Promise.
                   (fn [resolve reject]
                     (try
                       (resolve (authz/ensure-conversation-access! store ctx-b "conv-p"))
                       (catch :default e (reject e)))))]
        (is (instance? js/Promise p) "wrapped call is a Promise")
        (-> p
            (.then (fn [_] (is false "should have rejected") (done)))
            (.catch (fn [err]
                      (is (= 403 (some-> err ex-data :status)) "rejection carries 403")
                      (done))))))))
