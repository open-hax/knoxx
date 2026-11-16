(ns knoxx.backend.extern.native-subscription-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.agent-turn-result :as result]
            [knoxx.backend.extern.native-subscription :as subscription]
            [knoxx.backend.infra.agent.provider.native-subscription :as native]
            [knoxx.backend.infra.agent.stream.provider-events :as events]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(deftest native-worker-uses-the-existing-stream-and-usage-shapes
  (let [event (events/normalize (subscription/text-delta "partial text"))
        message (subscription/transcript-message
                 {:role "assistant" :text "terminal text" :usage {:input 17 :output 5}})]
    (is (= {:type "message_update" :assistant-event-type "text_delta" :delta "partial text"}
           (select-keys event [:type :assistant-event-type :delta])))
    (is (= 17 (:input-tokens (result/usage-tokens message))))
    (is (= 5 (:output-tokens (result/usage-tokens message))))))

(deftest matching-identity-fields-need-verified-mcp-authority
  (let [owner {:org-id "org-1" :membership-id "membership-1" :actor-id "actor-1"}
        matching-ctx (assoc owner :actor-binding "actor-1")
        home "/private/fixture"
        config {:native-subscription-owner owner :native-claude-home home}
        authorized (native/with-mcp-authorization matching-ctx)
        model "claude-code/haiku"]
    (is (thrown? cljs.core/ExceptionInfo (native/resolve-worker! config matching-ctx model nil)))
    (doseq [[field value] {:org-id "org-2"
                           :membership-id "membership-2"
                           :actor-id "actor-2"
                           :actor-binding "actor-2"}]
      (is (thrown? cljs.core/ExceptionInfo
                   (native/resolve-worker! config (assoc authorized field value) model nil))))
    (is (thrown? cljs.core/ExceptionInfo
                 (native/resolve-worker! config authorized model {:actor-id "actor-2"})))
    (is (thrown? cljs.core/ExceptionInfo
                 (native/resolve-worker! (dissoc config :native-claude-home) authorized model nil)))
    (is (thrown? cljs.core/ExceptionInfo
                 (native/resolve-worker! config authorized "claude-code/unknown" nil)))
    (is (= {:provider :claude :model "haiku" :home home}
           (native/resolve-worker! config authorized model nil)))
    (is (nil? (native/resolve-worker! config authorized "openai/gpt-6.1-sol" nil)))))

(deftest ^:async claude-worker-requires-an-isolated-private-home
  (let [root (await (fs/mkdtemp (path/join (os/tmpdir) "knoxx-claude-profile-test.")))
        accessible (path/join root "accessible")
        link (path/join root "link")]
    (try
      (await (fs/mkdir accessible))
      (await (fs/chmod accessible 493)) ; 0755
      (await (fs/symlink root link))
      (doseq [home [nil "relative" accessible link (os/homedir)]]
        (let [failure (try
                        (await (subscription/run-turn! {:model "haiku" :home home}
                                                       (subscription/abort-controller)
                                                       "Fixture prompt" identity identity))
                        nil
                        (catch :default error (.-code error)))]
          (is (= "native_claude_home_invalid" failure))))
      (finally
        (await (fs/rm root #js {:recursive true :force true}))))))
