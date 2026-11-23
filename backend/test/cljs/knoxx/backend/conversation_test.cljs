(ns knoxx.backend.conversation-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as runs]
            [knoxx.backend.domain.social.conversation :as conversation]
            [knoxx.backend.domain.voice.turn-control :as turns]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.clients.openplanner :as planner]
            [knoxx.backend.infra.openplanner.knowledge :as knowledge]
            [knoxx.backend.infra.openplanner.knowledge-scope :as identity]
            [knoxx.backend.law.conversation :as law]))

(def principal {:actor-id "hax" :org-slug "open-hax" :org-id "org-1"})
(def own-uri "at://did:plc:self/app.bsky.feed.post/new")
(def root-uri "at://did:plc:self/app.bsky.feed.post/old")
(def publication
  {:id "publish-call" :tool_name "bluesky_publish" :status "completed" :is_error false
   :input {:text "A new movement" :replyTo root-uri}
   :result {:details {:uri own-uri :cid "new-cid" :reply_to root-uri
                      :replyRoot {:uri root-uri :cid "root-cid"}
                      :replyParent {:uri root-uri :cid "root-cid"}
                      :actionOutcome {:status "confirmed"}}}})
(def run {:run_id "run-1" :conversation_id "conversation" :actor_id "hax" :status "running" :tool_receipts [publication]})
(def annotation {:topic "Workshop" :state "closed"})
(def observed-at "2026-10-27T04:00:00.000Z")

(deftest cursor-derives-identifiers-and-server-time-instead-of-taking-model-values
  (let [cursor (conversation/derive-cursor run annotation observed-at)]
    (is (= {:id own-uri :cid "new-cid"} (:latest cursor)))
    (is (= {:id root-uri :cid "root-cid"} (:root cursor) (:parent cursor)))
    (is (= observed-at (:observed-at cursor)))
    (is (= "publish-call" (:source-call cursor)))
    (is (= "hax" (:actor-id cursor)))
    (is (= "provisional" (:annotation-tier cursor)))
    (is (= law/registry (reader/read-string (pr-str law/registry))))
    (doseq [field [:actor-id :run-id :observed-at :latest :root :visibility]]
      (is (thrown? js/Error (conversation/derive-cursor run (assoc annotation field "invented") observed-at))))))

(deftest missing-confirmation-root-and-later-uncertainty-refuse-persistence
  (doseq [changed [(assoc run :status "completed") (assoc run :tool_receipts [])
                  (assoc-in run [:tool_receipts 0 :is_error] true)
                  (assoc-in run [:tool_receipts 0 :result :details :cid] "")
                  (assoc-in run [:tool_receipts 0 :result :details :replyRoot] nil)
                  (assoc-in run [:tool_receipts 0 :result :details :actionOutcome :status] "unknown")
                  (update run :tool_receipts conj (assoc-in publication [:result :details :actionOutcome :status] "partial"))]]
    (is (thrown? js/Error (conversation/derive-cursor changed annotation observed-at)))))

(deftest discord-chunks-and-platform-selection-use-actual-receipts
  (let [discord {:id "discord-call" :tool_name "discord_send" :status "completed" :is_error false
                 :input {:reply_to "11"} :result {:details {:channelId "10" :messageId "13" :messageIds ["12" "13"]
                                                           :actionOutcome {:status "confirmed"}}}}
        mixed (assoc run :tool_receipts [publication discord])
        cursor (conversation/derive-cursor mixed annotation observed-at)]
    (is (= "discord" (:platform cursor)))
    (is (= {:id "10"} (:root cursor)))
    (is (= {:id "13"} (:latest cursor)))
    (is (= ["12" "13"] (:message-ids cursor)))
    (is (= {:id "11"} (:parent cursor)))
    (is (= "bluesky" (:platform (conversation/derive-cursor mixed (assoc annotation :platform "bluesky") observed-at))))))

(deftest generic-posts-keep-the-submitted-strong-root-reference
  (let [generic {:id "api-call" :tool_name "social_request" :status "completed" :is_error false
                 :result {:details {:platform "bluesky" :actionOutcomes [{:status "confirmed"}]
                                    :publications [{:uri own-uri :cid "new-cid" :reply_to root-uri
                                                    :replyRoot {"uri" root-uri "cid" "root-cid"}
                                                    :replyParent {"uri" root-uri "cid" "root-cid"}}]}}}]
    (is (= {:id root-uri :cid "root-cid"}
           (:root (conversation/derive-cursor (assoc run :tool_receipts [generic]) annotation observed-at))))))

(deftest ^:async remember-persists-private-original-and-projection-and-refuses-direct-mcp
  (let [events (atom {}) invoke (knowledge/remember-execute nil)]
    (with-redefs [identity/principal (fn [& _] principal)
                  runs/runs* (atom {"run-1" run}) turns/active-turn (constantly {:run_id "run-1"})
                  planner/assert-event-projection-repair-supported! identity
                  planner/ingest-events-awaiting-projections! (fn [_ rows] (swap! events into (map (juxt :id identity) rows)))
                  planner/events! (fn [_ rows] (swap! events into (map (juxt :id identity) rows)))
                  planner/mongo-query! (fn [_ q] {:rows [(get @events (get-in q [:filter :id]))]})]
      (let [raw (await (tool-context/run-with! {:conversation-id "conversation" :agent-spec {:actor-id "hax"}}
                                             #(invoke nil {:social-knowledge-client :private-client}
                                                      "remember-call" annotation nil nil nil)))
            row (:details (js->clj raw :keywordize-keys true))]
        (is (= "private" (:visibility row)))
        (is (= "hax" (:owner row)))
        (is (= {:id own-uri :cid "new-cid"} (get-in row [:conversation :latest])))
        (is (= (:conversation row) (get-in @events [(:id row) :extra :conversation_cursor])))
        (is (= 2 (count @events))))
      (try (await (invoke nil {} "direct-call" annotation nil nil nil))
           (is false "Direct MCP cannot borrow live agent evidence")
           (catch :default e (is (= "conversation_agent_turn_required" (:code (ex-data e))))))
      (is (= 2 (count @events))))))

(deftest ^:async listing-is-owner-scoped-and-keeps-pagination-explicit
  (let [cursor (conversation/derive-cursor run annotation observed-at)
        own {:id "knoxx-knowledge:entry:own" :kind "knoxx.knowledge" :ts observed-at
             :extra {:knowledge_kind "entry" :conversation_cursor cursor
                     :memory_access {:version 1 :visibility "private" :actor_id "hax" :org_slug "open-hax" :org_id "org-1"}}}
        query (atom nil) invoke (knowledge/conversations-execute nil)]
    (with-redefs [identity/principal (fn [& _] principal)
                  planner/mongo-query! (fn [_ q] (reset! query q)
                                         {:total 10 :rows [own (assoc-in own [:extra :memory_access :actor_id] "duck")
                                                          (assoc-in own [:extra :memory_access :org_id] "foreign-org")
                                                          (assoc-in own [:extra :conversation_cursor :actor-id] "foreign")]})]
      (let [raw (await (invoke nil {:social-knowledge-client :private-client}
                               "list" {:platform "bluesky" :root root-uri :limit 4 :offset 0} nil nil nil))
            response (:details (js->clj raw :keywordize-keys true))]
        (is (= 1 (count (:snapshots response))))
        (is (= 4 (:nextOffset response)))
        (is (= "hax" (get-in @query [:filter "extra.memory_access.actor_id"])))
        (is (= root-uri (get-in @query [:filter "extra.conversation_cursor.root.id"])))
        (is (= {:ts -1 :id -1} (:sort @query)))))))

(deftest ^:async absent-isolated-store-refuses-before-querying-the-generic-data-plane
  (with-redefs [identity/principal (constantly principal)
                planner/client (fn [& _] (throw (ex-info "Generic data plane was selected" {})))
                planner/mongo-query! (fn [& _] (throw (ex-info "Unexpected query" {})))]
    (try
      (await ((knowledge/conversations-execute nil) nil {} "list" {} nil nil nil))
      (is false "A missing isolated store must refuse conversation reads")
      (catch :default error
        (is (= "social_knowledge_unavailable" (:code (ex-data error))))))))
