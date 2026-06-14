(ns knoxx.frontend.components.agent-audit.session-list-interaction-test
  "Port of the component tests in AgentAuditSessionList.test.tsx to the
  node :test build — contract-scoped loading, search filtering, resume
  click, and 20-row infinite-scroll pagination. API ns mocked via set!."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.components.agent-audit.api :as api]
            [knoxx.frontend.components.agent-audit.session-list
             :refer [agent-audit-session-list]]))

;; jsdom globals come from the :test build's :prepend-js.

(defn memory-session [overrides]
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

(def active-run
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

(def memory-calls (atom []))
(def memory-pages (atom []))
(def operator-calls (atom 0))
(def resume-calls (atom []))

(def ^:private real-memory api/list-memory-sessions)
(def ^:private real-operator api/list-operator-active-agents)

(use-fixtures :each
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
  (rtl/render ($ agent-audit-session-list
                 {:controller (controller)
                  :built-in-contract-id "fork_tales_creative_director"})))

(deftest loads-filters-searches-and-resumes
  (async done
    (let [r (render-list)]
      (-> (wait-until "history row" #(some? (.queryByText r "Fork history")))
          (.then (fn []
                   (is (some? (.queryByText r "sub-agent fork_tales_creative_director"))
                       "active run card present")
                   (is (nil? (.queryByText r "Other history"))
                       "other-contract session filtered out")
                   (is (= [{:limit 20 :offset 0 :contract-id "fork_tales_creative_director"}]
                          @memory-calls))
                   (is (= 1 @operator-calls))
                   (.change rtl/fireEvent (.getByLabelText r "Search audit sessions")
                            #js {:target #js {:value "fork history"}})
                   (wait-until "search filters active card"
                               #(nil? (.queryByText r "sub-agent fork_tales_creative_director")))))
          (.then (fn []
                   (.click rtl/fireEvent (.getByText r "Fork history"))
                   (is (= ["conv-history"] @resume-calls))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest paginates-with-infinite-scroll
  (reset! memory-pages
          [{:ok true :rows [(memory-session {:session "first-page" :title "First page"})]
            :total 2 :offset 0 :limit 20 :has_more true}
           {:ok true :rows [(memory-session {:session "second-page" :title "Second page"})]
            :total 2 :offset 1 :limit 20 :has_more false}])
  (async done
    (let [r (render-list)]
      (-> (wait-until "first page" #(some? (.queryByText r "First page")))
          (.then (fn []
                   (let [list (.getByLabelText r "Audit sessions list")]
                     (js/Object.defineProperties list
                                                 #js {:scrollHeight #js {:configurable true :value 200}
                                                      :scrollTop #js {:configurable true :value 100}
                                                      :clientHeight #js {:configurable true :value 100}})
                     (.scroll rtl/fireEvent list))
                   (wait-until "second page" #(some? (.queryByText r "Second page")))))
          (.then (fn []
                   (is (= [{:limit 20 :offset 0 :contract-id "fork_tales_creative_director"}
                           {:limit 20 :offset 1 :contract-id "fork_tales_creative_director"}]
                          @memory-calls))
                   (is (some? (.queryByText r "First page")) "first page rows kept")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
