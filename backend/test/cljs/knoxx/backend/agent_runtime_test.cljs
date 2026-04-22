(ns knoxx.backend.agent-runtime-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.agent-runtime :as agent-runtime]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]))

(deftest rehydrate-session-manager-prefers-explicit-session-id-before-conversation-lookup
  (async done
    (let [appended* (atom [])
          session-manager #js {:appendMessage (fn [message]
                                               (swap! appended* conj (js->clj message :keywordize-keys true)))}]
      (with-redefs [agent-runtime/fetch-openplanner-session-messages!
                    (fn [_config _conversation-id]
                      (js/Promise.resolve []))
                    redis/get-client
                    (fn [] :redis-client)
                    session-store/get-conversation-active-session
                    (fn [_redis-client _conversation-id]
                      (js/Promise.resolve nil))
                    session-store/get-session
                    (fn [_redis-client session-id]
                      (js/Promise.resolve
                       (when (= "session-123" session-id)
                         {:messages [{:role "user"
                                      :content "Respect the selected contract."}]})))]
        (-> (agent-runtime/rehydrate-session-manager-from-redis!
             {}
             session-manager
             "conversation-123"
             "session-123"
             {:system-prompt "You are the creative music studio agent."})
            (.then
             (fn [result]
               (testing "fresh /api/chat/start requests restore the just-written session even before conversation->session Redis indices settle"
                 (is (true? (aget result "restored")))
                 (is (= 2 (count @appended*)))
                 (is (= {:role "system"
                         :content [{:type "text"
                                    :text "You are the creative music studio agent."}]}
                        (select-keys (first @appended*) [:role :content])))
                 (is (= {:role "user"
                         :content [{:type "text"
                                    :text "Respect the selected contract."}]}
                        (select-keys (second @appended*) [:role :content]))))))
            (.catch
             (fn [err]
               (is nil (str "unexpected promise rejection: " err))))
            (.finally
             (fn []
               (done))))))))
