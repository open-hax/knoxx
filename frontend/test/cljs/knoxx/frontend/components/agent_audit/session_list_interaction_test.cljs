(ns knoxx.frontend.components.agent-audit.session-list-interaction-test
  "Port of the component tests in AgentAuditSessionList.test.tsx to the
  node :test build — contract-scoped loading, search filtering, resume
  click, and 20-row infinite-scroll pagination. API ns mocked via set!."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.components.agent-audit.api :as api]
            [knoxx.frontend.components.agent-audit.session-list :as session-list]))

;; jsdom globals come from the :test build's :prepend-js.

(defn- memory-session [overrides]
  (merge {:project "knoxx-session"
          :session "conv-history"
          :title "Fork history"
          :last_ts "2026-05-14T00:00:00.000Z"
          :event_count 7
          :contract_id "fork_tales_creative_director"
          :actor_id "fork_tales_creative_director"
          :trigger_id "fork_tales_creative_director_cron"
          :event_type "schedule/fork-tales-creative-director"
          :is_active false
          :active_status "completed"
          :has_active_stream false}
         overrides))

(def ^:private active-run
  {:run_id "run-1"
   :session_id "sid-active"
   :conversation_id "conv-active"
   :status "running"
   :model "gemma4:31b"
   :created_at "2026-05-14T00:00:00.000Z"
   :updated_at "2026-05-14T00:01:00.000Z"
   :latest_user_message "Advance Fork Tales lore."
   :agent_spec {:contractId "fork_tales_creative_director"
                :subAgentId "fork_tales_creative_director"
                :triggerId "fork_tales_creative_director_cron"
                :eventType "schedule/fork-tales-creative-director"}})

(def ^:private memory-calls (atom []))
(def ^:private memory-pages (atom []))
(def ^:private operator-calls (atom 0))
(def ^:private resume-calls (atom []))

(def ^:private real-memory api/list-memory-sessions)
(def ^:private real-operator api/list-operator-active-agents)

(t/use-fixtures :each
  {:before (fn []
             (reset! memory-calls [])
             (reset! operator-calls 0)
             (reset! resume-calls [])
             (reset! memory-pages
                     [{:ok true
                       :rows [(memory-session {})
                              (memory-session {:session "conv-other" :title "Other history"
                                               :contract_id "other_agent" :actor_id "other_agent"})]
                       :total 2 :offset 0 :limit 20 :has_more false}])
             (set! api/list-memory-sessions
                   (fn [params]
                     (swap! memory-calls conj params)
                     (let [page (or (first @memory-pages) {:ok true :rows [] :has_more false})]
                       (swap! memory-pages #(vec (rest %)))
                       (js/Promise.resolve page))))
             (set! api/list-operator-active-agents
                   (fn []
                     (swap! operator-calls inc)
                     (js/Promise.resolve [active-run]))))
   :after (fn []
            (rtl/cleanup)
            (set! api/list-memory-sessions real-memory)
            (set! api/list-operator-active-agents real-operator))})

(defn- wait-until [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn- controller []
  #js {:conversationId nil
       :sessionId "sid-active"
       :loadingMemorySessionId nil
       :resumeMemorySession (fn [session] (swap! resume-calls conj session))})

(defn- render-list []
  (rtl/render (hx/$ session-list/agent-audit-session-list
                 {:controller (controller)
                  :built-in-contract-id "fork_tales_creative_director"})))

(t/deftest ^:async loads-filters-searches-and-resumes
  (let [r (render-list)]
    (await (wait-until "history row" #(some? (.queryByText r "Fork history"))))
    (t/is (some? (.queryByText r "sub-agent fork_tales_creative_director"))
          "active run card present")
    (t/is (nil? (.queryByText r "Other history")) "other-contract session filtered out")
    (t/is (= [{:limit 20 :offset 0 :contract-id "fork_tales_creative_director"}]
             @memory-calls))
    (t/is (= 1 @operator-calls))
    (.change rtl/fireEvent (.getByLabelText r "Search audit sessions")
             #js {:target #js {:value "fork history"}})
    (await (wait-until "search filters active card"
                      #(nil? (.queryByText r "sub-agent fork_tales_creative_director"))))
    (.click rtl/fireEvent (.getByText r "Fork history"))
    (t/is (= ["conv-history"] @resume-calls))))

(t/deftest ^:async paginates-with-infinite-scroll
  (reset! memory-pages
          [{:ok true :rows [(memory-session {:session "first-page" :title "First page"})]
            :total 2 :offset 0 :limit 20 :has_more true}
           {:ok true :rows [(memory-session {:session "second-page" :title "Second page"})]
            :total 2 :offset 1 :limit 20 :has_more false}])
  (let [r (render-list)]
    (await (wait-until "first page" #(some? (.queryByText r "First page"))))
    (let [scroll-list (.getByLabelText r "Audit sessions list")]
      (js/Object.defineProperties scroll-list
                                  #js {:scrollHeight #js {:configurable true :value 200}
                                       :scrollTop #js {:configurable true :value 100}
                                       :clientHeight #js {:configurable true :value 100}})
      (.scroll rtl/fireEvent scroll-list))
    (await (wait-until "second page" #(some? (.queryByText r "Second page"))))
    (t/is (= [{:limit 20 :offset 0 :contract-id "fork_tales_creative_director"}
              {:limit 20 :offset 1 :contract-id "fork_tales_creative_director"}]
             @memory-calls))
    (t/is (some? (.queryByText r "First page")) "first page rows kept")))

(t/deftest ^:async failed-load-can-be-refreshed
  (set! api/list-memory-sessions (fn [_params] (js/Promise.reject (js/Error. "Audit temporarily unavailable"))))
  (let [r (render-list)]
    (await (wait-until "load error" #(some? (.queryByText r "Audit temporarily unavailable"))))
    (t/is (nil? (.queryByText r "Loading sessions…")) "failure releases initial busy state")
    (set! api/list-memory-sessions
          (fn [_params] (js/Promise.resolve {:rows [(memory-session {})] :has_more false})))
    (.click rtl/fireEvent (.getByText r "↻"))
    (await (wait-until "refresh recovers" #(some? (.queryByText r "Fork history"))))
    (t/is (nil? (.queryByText r "Audit temporarily unavailable")))
    (t/is (= 2 @operator-calls))))

(t/deftest ^:async obsolete-contract-response-does-not-replace-current-page
  (let [release-old (atom nil)
        old-page (js/Promise. (fn [resolve-page _reject] (reset! release-old resolve-page)))]
    (set! api/list-memory-sessions
          (fn [{:keys [contract-id]}]
            (if (= contract-id "fork_tales_creative_director")
              old-page
              (js/Promise.resolve {:rows [(memory-session {:session "other" :title "New scope"
                                                            :contract_id "other-agent"})]
                                    :has_more false}))))
    (let [r (render-list)]
      (.rerender r (hx/$ session-list/agent-audit-session-list
                         {:controller (controller) :built-in-contract-id "other-agent"}))
      (await (wait-until "new scope" #(some? (.queryByText r "New scope"))))
      (await (rtl/act (fn ^:async complete-old []
                        (@release-old {:rows [(memory-session {})] :has_more false})
                        (await old-page))))
      (t/is (some? (.queryByText r "New scope")))
      (t/is (nil? (.queryByText r "Fork history")) "late prior scope cannot replace the current page"))))
