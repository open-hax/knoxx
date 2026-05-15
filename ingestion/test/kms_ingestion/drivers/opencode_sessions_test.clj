(ns kms-ingestion.drivers.opencode-sessions-test
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.drivers.opencode-sessions :as opencode]
   [kms-ingestion.drivers.protocol :as protocol]))

(defn- json-response
  [body & [headers]]
  {:status 200
   :headers (or headers {})
   :body (json/generate-string body)})

(def ^:private sample-session
  {:id "ses_123"
   :slug "test-session"
   :projectID "proj_1"
   :directory "/home/err/devel/orgs/open-hax/openplanner"
   :title "Test OpenCode session"
   :time {:created 1770000000000
          :updated 1770000005000}})

(def ^:private sample-messages
  [{:info {:id "msg_user"
           :sessionID "ses_123"
           :role "user"
           :time {:created 1770000001000}
           :agent "build"
           :model {:providerID "anthropic"
                   :modelID "claude-sonnet-4"}}
    :parts [{:id "part_text_user"
             :sessionID "ses_123"
             :messageID "msg_user"
             :type "text"
             :text "please fix the bug"}]}
   {:info {:id "msg_assistant"
           :sessionID "ses_123"
           :role "assistant"
           :parentID "msg_user"
           :time {:created 1770000002000
                  :completed 1770000004000}
           :providerID "openai"
           :modelID "gpt-5.1-codex-max"
           :agent "build"
           :mode "build"
           :path {:cwd "/home/err/devel" :root "/home/err/devel"}
           :cost 0.01
           :tokens {:input 10 :output 20 :reasoning 0 :cache {:read 0 :write 0}}
           :finish "stop"}
    :parts [{:id "part_text_assistant"
             :sessionID "ses_123"
             :messageID "msg_assistant"
             :type "text"
             :text "fixed"}
            {:id "part_reasoning"
             :sessionID "ses_123"
             :messageID "msg_assistant"
             :type "reasoning"
             :text "looked at the stack trace"
             :time {:start 1770000002500}}
            {:id "part_tool"
             :sessionID "ses_123"
             :messageID "msg_assistant"
             :type "tool"
             :callID "call_1"
             :tool "bash"
             :state {:status "completed"
                     :input {:command "npm test"}
                     :output "ok"
                     :title "npm test"
                     :metadata {}
                     :time {:start 1770000003000
                            :end 1770000003500}}}]}])

(deftest discover-uses-opencode-global-session-list
  (testing "OpenCode sessions are represented as virtual ingest files keyed by session id"
    (let [unchanged (assoc sample-session :id "ses_old" :title "Old" :time {:created 100 :updated 100})
          changed sample-session
          driver (opencode/create-driver {:base-url "http://opencode.test"
                                          :max-sessions 10})
          old-hash (#'opencode/session-content-hash unchanged)
          captured (atom [])]
      (with-redefs [http/get (fn [url opts]
                               (swap! captured conj {:url url :opts opts})
                               (json-response [unchanged changed]))]
        (let [result (protocol/discover driver {:existing-state {(str "opencode-session:" (:id unchanged))
                                                                  {:content_hash old-hash}}})]
          (is (= 2 (:total-files result)))
          (is (= 1 (:unchanged-files result)))
          (is (= 1 (:new-files result)))
          (is (= [(str "opencode-session:" (:id changed))]
                 (mapv :id (:files result))))
          (is (= "http://opencode.test/experimental/session" (:url (first @captured))))
          (is (= true (get-in (first @captured) [:opts :query-params :archived]))))))))

(deftest extract-maps-opencode-messages-to-openplanner-events
  (testing "session, message, reasoning, and tool parts become OpenPlanner events"
    (let [driver (opencode/create-driver {:base-url "http://opencode.test"
                                          :session-project "knoxx-session"})]
      (with-redefs [http/get (fn [url _opts]
                               (cond
                                 (.endsWith url "/session/ses_123/message")
                                 (json-response sample-messages)

                                 (.endsWith url "/session/ses_123")
                                 (json-response sample-session)

                                 :else
                                 {:status 404 :body "not found"}))]
        (let [result (protocol/extract driver "opencode-session:ses_123")
              parsed (json/parse-string (:content result) keyword)
              kinds (set (map :kind (:events parsed)))]
          (is (= "ses_123" (:session-id parsed)))
          (is (= (#'opencode/session-content-hash sample-session) (:content-hash result)))
          (is (contains? kinds "opencode.session_start"))
          (is (contains? kinds "opencode.message"))
          (is (contains? kinds "opencode.reasoning"))
          (is (contains? kinds "opencode.tool_call"))
          (is (some #(= "opencode-session-ingester" (:source %)) (:events parsed)))
          (is (some #(= "bash" (get-in % [:extra :tool_name])) (:events parsed))))))))
