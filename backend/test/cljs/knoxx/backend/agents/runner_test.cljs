(ns knoxx.backend.agents.runner-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.extern.agent-runner :as xrunner]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.infra.agent.policy :as agent-policy]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.turn :as agent-turns]))

(defn- event-turn-body
  [run-id]
  {:run-id run-id
   :conversation-id (str "conversation-" run-id)
   :session-id (str "session-" run-id)
   :message (str "translate " run-id)
   :model "gemma4:e2b"
   :mode "direct"
   :agent-spec {:contract-id "publication_translator"
                :trigger-id "translate_on_publication_needed"
                :event-id (str "event-" run-id)
                :model "gemma4:e2b"
                :tools-choice "required-first"}})

(defn- deferred-turn
  []
  (let [resolve* (atom nil)
        promise (js/Promise.
                 (fn [resolve _reject]
                   (reset! resolve* resolve)))]
    {:promise promise
     :resolve! (fn []
                 (when-let [resolve @resolve*]
                   (resolve nil)))}))

(defn- ^:async flush-promises!
  []
  (await (js/Promise.resolve nil))
  (await (js/Promise.resolve nil)))

(defn- remove-test-runs!
  [run-ids]
  (let [run-id-set (set run-ids)]
    (swap! run-state/runs* #(apply dissoc % run-ids))
    (swap! run-state/run-order*
           (fn [order]
             (->> order
                  (remove run-id-set)
                  vec)))))

(deftest direct-start-payload->turn-params-normalizes-direct-start-shape
  (testing "snake_case direct-start payload becomes send-agent-turn! params"
    (is (= {:conversation-id "conversation-1"
            :session-id "session-1"
            :run-id "run-1"
            :message "hello"
            :content-parts [{:type "image" :url "https://example.com/demo.png"}]
            :model "gemma4:31b"
            :mode "direct"
            :agent-spec {:role "knowledge_worker"
                         :system-prompt "sys"}}
           (runner/direct-start-payload->turn-params
            {:conversation_id "conversation-1"
             :session_id "session-1"
             :run_id "run-1"
             :message "hello"
             :content_parts [{:type "image" :url "https://example.com/demo.png"}]
             :model "gemma4:31b"
             :agent_spec {:role "knowledge_worker"
                          :system_prompt "sys"}})))))

(deftest direct-start-payload->turn-params-supports-js-payloads
  (testing "JS direct-start payloads normalize through the runner extern boundary"
    (is (= {:conversation-id "conversation-js"
            :session-id "session-js"
            :run-id "run-js"
            :message "hello from js"
            :content-parts []
            :model "model-js"
            :mode "direct"
            :agent-spec {:role "developer"
                         :tool-policies [{:toolId "discord.read" :effect "allow"}]}}
           (runner/direct-start-payload->turn-params
            (xjs/object
             {:conversation_id "conversation-js"
              :session_id "session-js"
              :run_id "run-js"
              :message "hello from js"
              :model "model-js"
              :agent_spec {:role "developer"
                           :tool_policies [{:toolId "discord.read" :effect "allow"}]}}))))))

(deftest direct-start-payload->turn-params-supports-kebab-keys-too
  (testing "existing CLJ payloads also normalize"
    (is (= {:conversation-id "conversation-2"
            :session-id "session-2"
            :run-id "run-2"
            :message "hi"
            :content-parts []
            :model nil
            :mode "direct"
            :agent-spec {:role "developer"}}
           (runner/direct-start-payload->turn-params
            {:conversation-id "conversation-2"
             :session-id "session-2"
             :run-id "run-2"
             :message "hi"
             :agent-spec {:role "developer"}})))))

(deftest direct-start-payload->turn-params-normalizes-contract-and-actor-ids
  (testing "snake_case agent_spec preserves contract and actor identity"
    (is (= {:contract-id "discord_mention_response"
            :actor-id "discord_automation"
            :role "knowledge_worker"
            :thinking-level "medium"
            :tools-choice "required-first"
            :tool-policies [{:toolId "discord.read" :effect "allow"}]}
           (:agent-spec
            (runner/direct-start-payload->turn-params
             {:message "hi"
              :agent_spec {:contract_id "discord_mention_response"
                           :actor_id "discord_automation"
                           :role "knowledge_worker"
                           :thinking_level "medium"
                           :tools_choice :required-first
                           :tool_policies [{:toolId "discord.read" :effect "allow"}]}}))))))

(deftest direct-start-payload->turn-params-normalizes-trigger-audit-metadata
  (testing "triggered agent runs preserve audit metadata through the runner boundary"
    (is (= {:contract-id "ussyverse_social_creative"
            :actor-id "discord_automation"
            :trigger-id "ussyverse_social_creative_cron"
            :event-type "schedule/ussyverse-social-creative"
            :event-types ["schedule/ussyverse-social-creative"]
            :event-id "evt-1"
            :event-scope-id "ussyverse_social_creative"
            :schedule-id "ussyverse_social_creative"}
           (:agent-spec
            (runner/direct-start-payload->turn-params
             {:message "hi"
              :agent_spec {:contract_id "ussyverse_social_creative"
                           :actor_id "discord_automation"
                           :trigger_id "ussyverse_social_creative_cron"
                           :event_type "schedule/ussyverse-social-creative"
                           :event_types ["schedule/ussyverse-social-creative"]
                           :event_id "evt-1"
                           :event_scope_id "ussyverse_social_creative"
                           :schedule_id "ussyverse_social_creative"}}))))))

(deftest event-trigger-detection-does-not-capture-interactive-chat
  (is (true? (runner/event-triggered-turn?
              {:agent-spec {:trigger-id "publication-translation"}})))
  (is (false? (runner/event-triggered-turn?
               {:agent-spec {:contract-id "knoxx_default"}})))
  (is (false? (runner/event-triggered-turn? {}))))

(deftest ^:async event-turn-queue-is-bounded-and-fifo
  (let [run-ids ["fifo-1" "fifo-2" "fifo-3"]
        bodies (mapv event-turn-body run-ids)
        deferreds (mapv (fn [_] (deferred-turn)) run-ids)
        started* (atom [])
        start-turn (fn [run-id deferred]
                     (fn []
                       (swap! started* conj run-id)
                       (:promise deferred)))
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 8
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (let [responses (mapv (fn [body deferred]
                            (runner/enqueue-event-turn!
                             config body (start-turn (:run-id body) deferred)))
                          bodies deferreds)]
      (testing "only the first provider turn starts and later turns are observable as queued"
        (is (= ["running" "queued" "queued"]
               (mapv #(get-in % [:event_queue :status]) responses)))
        (is (= [0 1 2]
               (mapv #(get-in % [:event_queue :position]) responses)))
        (is (= ["fifo-1"] @started*))
        (is (= {:active 1
                :queued 2
                :concurrency 1
                :queue-limit 8
                :active-run-ids ["fifo-1"]
                :queued-run-ids ["fifo-2" "fifo-3"]
                :restart-aware false}
               (runner/event-turn-queue-snapshot)))
        (is (= "queued" (get-in @run-state/runs* ["fifo-2" :status])))
        (is (= "required-first"
               (get-in @run-state/runs*
                       ["fifo-2" :settings :agentSpec :toolsChoice]))))

      ((:resolve! (nth deferreds 0)))
      (await (flush-promises!))
      (testing "completion releases exactly the oldest pending turn"
        (is (= ["fifo-1" "fifo-2"] @started*))
        (is (= ["fifo-3"] (:queued-run-ids (runner/event-turn-queue-snapshot)))))

      ((:resolve! (nth deferreds 1)))
      (await (flush-promises!))
      (is (= ["fifo-1" "fifo-2" "fifo-3"] @started*))

      ((:resolve! (nth deferreds 2)))
      (await (flush-promises!))
      (is (= 0 (:active (runner/event-turn-queue-snapshot))))
      (is (= 0 (:queued (runner/event-turn-queue-snapshot)))))
    (runner/reset-event-turn-queue!)
    (remove-test-runs! run-ids)))

(deftest ^:async event-turn-queue-rejects-overflow-and-records-the-failure
  (let [run-ids ["full-1" "full-2" "full-3"]
        first-turn (deferred-turn)
        second-turn (deferred-turn)
        third-started* (atom false)
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (with-redefs [xrunner/log-async-spawn-error! (fn [_body _err] nil)]
      (runner/enqueue-event-turn! config (event-turn-body "full-1") #(:promise first-turn))
      (runner/enqueue-event-turn! config (event-turn-body "full-2") #(:promise second-turn))
      (let [message (try
                      (await (runner/enqueue-event-turn!
                              config
                              (event-turn-body "full-3")
                              (fn []
                                (reset! third-started* true)
                                (js/Promise.resolve nil))))
                      nil
                      (catch :default err
                        (ex-message err)))]
        (is (= "event_agent_queue_full: pending queue limit 1 reached" message))
        (is (false? @third-started*))
        (is (= "failed" (get-in @run-state/runs* ["full-3" :status])))
        (is (= ["event_turn_queue_rejected" "async_spawn_failed"]
               (mapv :type (get-in @run-state/runs* ["full-3" :events]))))))
    ((:resolve! first-turn))
    (await (flush-promises!))
    ((:resolve! second-turn))
    (await (flush-promises!))
    (runner/reset-event-turn-queue!)
    (remove-test-runs! run-ids)))

(deftest ^:async event-turn-queue-records-asynchronous-provider-failures
  (let [run-id "async-failure"
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (with-redefs [xrunner/log-async-spawn-error! (fn [_body _err] nil)]
      (runner/enqueue-event-turn!
       config
       (event-turn-body run-id)
       (fn [] (js/Promise.reject (js/Error. "provider unavailable"))))
      (await (flush-promises!)))
    (is (= "failed" (get-in @run-state/runs* [run-id :status])))
    (is (= "provider unavailable" (get-in @run-state/runs* [run-id :error])))
    (is (= "async_spawn_failed"
           (-> @run-state/runs* (get run-id) :events last :type)))
    (is (= 0 (:active (runner/event-turn-queue-snapshot))))
    (runner/reset-event-turn-queue!)
    (remove-test-runs! [run-id])))

(deftest ^:async event-turn-queue-reports-full-turn-rejection-to-its-owner
  (let [run-id "settled-failure"
        settlements* (atom [])
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (await
     (runner/register-event-turn-settler!
      (str "event-" run-id)
      (fn [settlement]
        (swap! settlements* conj settlement)
        (js/Promise.resolve true))))
    (with-redefs [xrunner/log-async-spawn-error! (fn [_body _err] nil)]
      (runner/enqueue-event-turn!
       config
       (event-turn-body run-id)
       (fn [] (js/Promise.reject (js/Error. "provider unavailable"))))
      (await (flush-promises!)))
    (is (= [{:event-turn/status :failed
             :event-turn/detail "provider unavailable"}]
           @settlements*))
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (remove-test-runs! [run-id])))

(deftest ^:async event-turn-queue-treats-an-error-shaped-result-as-failed
  (let [run-id "settled-error-result"
        settlements* (atom [])
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (await
     (runner/register-event-turn-settler!
      (str "event-" run-id)
      (fn [settlement]
        (swap! settlements* conj settlement)
        (js/Promise.resolve true))))
    (runner/enqueue-event-turn!
     config
     (event-turn-body run-id)
     (fn [] (js/Promise.resolve {:error "translation tool failed"})))
    (await (flush-promises!))
    (is (= [{:event-turn/status :failed
             :event-turn/detail "translation tool failed"}]
           @settlements*))
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (remove-test-runs! [run-id])))

(deftest ^:async a-terminal-settlement-is-redelivered-until-accepted
  (let [run-id "settlement-redelivery"
        event-id (str "event-" run-id)
        deliveries* (atom [])
        config {:event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (await
     (runner/register-event-turn-settler!
      event-id
      (fn [settlement]
        (swap! deliveries* conj [:first settlement])
        (js/Promise.reject (js/Error. "transient evidence-store failure")))))
    (with-redefs [xrunner/log-async-spawn-error! (fn [_body _err] nil)]
      (runner/enqueue-event-turn!
       config
       (event-turn-body run-id)
       (fn [] (js/Promise.reject (js/Error. "provider unavailable"))))
      (await (flush-promises!)))

    (testing "callback rejection retains the exact terminal result"
      (is (= [[:first {:event-turn/status :failed
                       :event-turn/detail "provider unavailable"}]]
             @deliveries*)))

    (testing "same-process replay registration redelivers without another turn"
      (await
       (runner/register-event-turn-settler!
        event-id
        (fn [settlement]
          (swap! deliveries* conj [:replay settlement])
          (js/Promise.resolve true))))
      (is (= [[:first {:event-turn/status :failed
                       :event-turn/detail "provider unavailable"}]
              [:replay {:event-turn/status :failed
                        :event-turn/detail "provider unavailable"}]]
             @deliveries*)))

    (testing "acceptance clears the cache"
      (await
       (runner/register-event-turn-settler!
        event-id
        (fn [settlement]
          (swap! deliveries* conj [:unexpected settlement])
          true)))
      (is (= 2 (count @deliveries*))))
    (runner/reset-event-turn-queue!)
    (runner/reset-event-turn-settlers!)
    (remove-test-runs! [run-id])))

(deftest ^:async interactive-direct-turn-bypasses-the-event-fifo
  (let [started* (atom [])]
    (runner/reset-event-turn-queue!)
    (with-redefs [agent-policy/validate-chat-policy!
                  (fn [_auth-context _model]
                    (js/Promise.resolve {:allowed true}))
                  agent-turns/send-agent-turn!
                  (fn [_runtime _config body]
                    (swap! started* conj (:message body))
                    (js/Promise.resolve {:ok true}))]
      (let [response (await (runner/spawn-direct!
                             {:runtime :test}
                             {:llmModel "test-model"}
                             {:message "interactive"}))]
        (await (flush-promises!))
        (is (= ["interactive"] @started*))
        (is (nil? (:event_queue response)))
        (is (= 0 (:active (runner/event-turn-queue-snapshot))))
        (is (= 0 (:queued (runner/event-turn-queue-snapshot))))))))

(deftest ^:async event-turn-timeout-overrides-only-event-triggered-turns
  (let [event-run-id "event-timeout-config"
        observed* (atom [])
        config {:llmModel "test-model"
                :agent-turn-timeout-ms 1200
                :event-agent-turn-timeout-ms 300000
                :event-agent-concurrency 1
                :event-agent-queue-limit 1
                :collection-name "test"}]
    (runner/reset-event-turn-queue!)
    (with-redefs [agent-policy/validate-chat-policy!
                  (fn [_auth-context _model]
                    (js/Promise.resolve {:allowed true}))
                  agent-turns/send-agent-turn!
                  (fn [_runtime turn-config body]
                    (swap! observed* conj
                           {:message (:message body)
                            :timeout-ms (:agent-turn-timeout-ms turn-config)})
                    (js/Promise.resolve {:ok true}))]
      (let [event-response
            (await (runner/spawn-direct!
                    {:runtime :test}
                    config
                    {:run_id event-run-id
                     :message "event"
                     :agent_spec {:trigger_id "translation-needed"}}))
            interactive-response
            (await (runner/spawn-direct!
                    {:runtime :test}
                    config
                    {:run_id "interactive-timeout-config"
                     :message "interactive"}))]
        (await (flush-promises!))
        (is (= "running" (get-in event-response [:event_queue :status])))
        (is (nil? (:event_queue interactive-response)))
        (is (= [{:message "event" :timeout-ms 300000}
                {:message "interactive" :timeout-ms 1200}]
               @observed*))))
    (runner/reset-event-turn-queue!)
    (remove-test-runs! [event-run-id])))
