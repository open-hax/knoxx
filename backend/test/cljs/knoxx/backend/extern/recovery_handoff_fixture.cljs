(ns knoxx.backend.extern.recovery-handoff-fixture
  "Actual recovery/turn admission with owned persistence and an inert model boundary."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.voice.turn-control :as control]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.hydration :as hydration]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.recovery :as recovery]
            [knoxx.backend.infra.agent.session :as agents]
            [knoxx.backend.infra.agent.tool-catalog :as catalog]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.shape.agent :as agent]
            [knoxx.backend.shape.session-persistence :as runs]
            [knoxx.backend.shape.thread-store :as threads]))

(defn record "Stable historical pending turn; no caller metadata grants ownership." [id]
  {:run_id (str id "-old-run") :session_id (str id "-session") :conversation_id (str id "-conversation")
   :org_id "owned-org" :user_id "owned-user" :status "running" :startup_token "old-private-token"
   :model "fixture-model" :mode "direct" :created_at "2026-09-20T00:00:00.000Z"
   :updated_at "2026-09-20T00:00:00.000Z" :messages [{:role "user" :content "Resume this pending message"}]})

(defn ^:async seed!
  "Persist through the real provider with the historical process stamp."
  [provider value]
  (with-redefs [instance/current-id (constantly "previous-owned-process")]
    (await (threads/put-thread! provider value))))

(defn- passive! ([_ _ _ _] (passive! nil nil nil nil nil)) ([_ _ _ _ _] nil))
(defn- memory! ([_ _ _] nil) ([_ _ _ _] nil) ([_ _ _ _ _] nil))
(defn- model-session []
  (reify agent/IAgentSession (set-thinking-level! [_ _] nil) (messages [_] [])
    (streaming? [_] false) (current-turn [_] nil)))

(defn ^:async with-runtime!
  "Preserve all installed providers/runtime heaps and replace only the external model boundary."
  [provider run-provider prompt! operation]
  (let [old-provider @sessions/provider* old-runs @registry/session-store*
        old-heap @state/runs* old-order @state/run-order* old-sink @state/event-stream-sink*
        old-agents @agents/sessions* old-access @turn/conversation-access* old-turns @control/active-turns*
        session (model-session)]
    (try
      (sessions/install! provider) (reset! registry/session-store* run-provider) (events/install! run-provider)
      (with-redefs [agents/create-session-manager! (fn ([_ _ _ _ _ _ _] session) ([_ _ _ _ _ _ _ _] session))
                    catalog/visible-session-signature (fn [& _] "recovery-handoff-proof")
                    hydration/passive-hydration! passive! hydration/passive-memory-hydration! memory!
                    titles/maybe-prime-session-title! (fn [& _] nil)
                    policy/enforce-chat-policy! (fn [& _] nil)
                    turn/prompt-and-await! (fixture/prompt-stub prompt!)]
        (await (operation)))
      (finally
        (reset! agents/sessions* old-agents) (reset! turn/conversation-access* old-access)
        (reset! control/active-turns* old-turns) (reset! state/event-stream-sink* old-sink)
        (reset! state/runs* old-heap) (reset! state/run-order* old-order)
        (sessions/install! old-provider) (reset! registry/session-store* old-runs) (events/install! old-runs)))))

(defn ^:async success!
  "Exercise the real recovery entrypoint, real turn hydration and durable initial admission."
  [writer provider run-provider id legacy? scan?]
  (let [value (cond-> (record id) legacy? (dissoc :startup_token)) prompts* (atom 0)]
    (await (seed! writer value))
    (await (runs/put-run! run-provider value))
    (await (with-runtime!
            provider run-provider #(swap! prompts* inc)
            (^:async fn []
              (let [before (await (runs/get-run run-provider (:run_id value)))
                    snapshot (cond (= scan? :cache) (do (await (sessions/get-session (:session_id value)))
                                                       (sessions/get-session-sync (:session_id value)))
                                   scan? (first (filter #(= (:session_id value) (:session_id %))
                                                       (await (sessions/recover-sessions!))))
                                   :else (await (sessions/get-session (:session_id value))))
                    result (await (recovery/resume-recovered-session! {} {} snapshot))
                    after (await (sessions/get-session (:session_id value)))]
                (test/is (true? (:resumed result)) (pr-str result))
                (test/is (= 1 @prompts*))
                (test/is (not= (:run_id value) (:run_id after)))
                (test/is (= (:run_id after) (:run_id result)))
                (test/is (= (:run_id value) (:previous_run_id result) (:recovered_from_run_id after)))
                (test/is (not= (:startup_token value) (:startup_token after)))
                (test/is (= before (await (runs/get-run run-provider (:run_id value)))))
                (test/is (= "run_started" (:type (first (await (runs/events-since run-provider (:run_id after) nil))))))
                (test/is (seq (:messages after)) "The released transcript remains available to actual turn startup")))))))

(defn ^:async refused!
  "Every changed or unreceipted snapshot refuses before model execution or a thread mutation."
  [writer provider run-provider id transform!]
  (let [value (record id) prompts* (atom 0)]
    (await (seed! writer value))
    (await (with-runtime!
            provider run-provider #(swap! prompts* inc)
            (^:async fn []
              (let [original (await (sessions/get-session (:session_id value)))
                    supplied (await (transform! provider original))
                    before (await (threads/read-thread provider (:session_id value)))
                    result (await (recovery/resume-recovered-session! {} {} supplied))]
                (test/is (false? (:resumed result)))
                (test/is (= "thread_recovery_conflict" (:code result)))
                (test/is (zero? @prompts*))
                (test/is (= before (await (threads/read-thread provider (:session_id value)))))))))))
