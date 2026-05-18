(ns knoxx.backend.core-memory-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.core-memory :as core-memory]))

(deftest session-visible-for-page-actor-prefers-stored-contract-claims
  (let [rows [{:extra "{\"contract_actors\":[\"chat_primary\",\"cms_chat\"]}"}]]
    (is (true? (core-memory/session-visible-for-page-actor? {} rows "cms_chat")))
    (is (false? (core-memory/session-visible-for-page-actor? {} rows "contract_librarian")))))

(deftest session-visible-for-page-actor-falls-back-to-legacy-chat-page
  (let [rows [{:extra "{}"}]]
    (is (true? (core-memory/session-visible-for-page-actor? {} rows "chat_primary")))
    (is (false? (core-memory/session-visible-for-page-actor? {} rows "cms_chat")))))

(deftest session-matches-page-actor-filter-prefers-recorded-actor-id
  (let [rows [{:extra "{\"actor_id\":\"pi\",\"contract_actors\":[\"chat_primary\",\"cms_chat\"]}"}]]
    (is (true? (core-memory/session-matches-page-actor-filter? {} rows "pi" [])))
    (is (false? (core-memory/session-matches-page-actor-filter? {} rows "cms_chat" [])))
    (is (false? (core-memory/session-matches-page-actor-filter? {} rows nil ["pi"])))))

(deftest session-matches-contract-filter-uses-archived-agent-scope
  (let [rows [{:extra "{\"contract_id\":\"fork_tales_creative_director\",\"sub_agent_id\":\"fork_writer\",\"actor_id\":\"agent_librarian\"}"}]]
    (is (true? (core-memory/session-matches-contract-filter? {} rows "fork_tales_creative_director")))
    (is (true? (core-memory/session-matches-contract-filter? {} rows "fork_writer")))
    (is (= {:contract_id "fork_tales_creative_director"
            :actor_id "agent_librarian"
            :sub_agent_id "fork_writer"}
           (core-memory/session-summary-scope-from-rows rows)))
    (is (false? (core-memory/session-matches-contract-filter? {} rows "other_agent")))))
