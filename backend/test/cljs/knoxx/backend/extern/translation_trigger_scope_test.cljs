(ns knoxx.backend.extern.translation-trigger-scope-test
  "Admitted translation scope through the real action, queue, turn and Clio stores."
  (:require [cljs.test :as t]
            [knoxx.backend.domain.action.registry :as actions]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.event.normalize :as normalize]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.infra.agent.event-policy-authority :as authority]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.turn :as turns]
            [knoxx.backend.infra.openplanner.scope :as planner-scope]
            [knoxx.backend.infra.run-events :as run-events]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.infra.tooling :as tooling]
            [knoxx.backend.infra.translation-agent-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as evidence]
            [knoxx.backend.infra.translation-split-store :as splits]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.shape.session-persistence :as runs]))

(def verified-scope
  "Only these admitted coordinates may reach the triggered run."
  {:org-id "verified-org" :membership-id "verified-member" :project "verified-project"})

(def resolved-agent
  "Finite model and tool policy resolved by the fixture's contract adapter."
  {:actor-id "translator" :role "translator" :model "test-model"
   :thinking-level "off" :system-prompt "Translate." :tool-ids []
   :tool-policies []})

(def action
  "Explicitly scoped, emitter-bound trigger action."
  {:action/kind :actions/start-agent-session
   :action/with {:agent-id "publication_translator" :task "Translate the admitted source."
                 :resource-policies-from-event true :execution-snapshot-from-event true
                 :scope-from-event true}})

(defn- digest [value] (str "h" (hash value)))

(defn- context []
  {:dispatch/garden "example.gardens/site" :dispatch/document-wire-id "example.documents/post"
   :dispatch/source-locale :en :dispatch/org-id (:org-id verified-scope)
   :dispatch/membership-id (:membership-id verified-scope)
   :dispatch/project (:project verified-scope) :dispatch/source-digest "sha256-source"})

(defn- dispatch-dependencies [emit!]
  {:evidence-store (evidence/memory-store)
   :split-store (splits/memory-store digest)
   :translation-execution
   (split-law/execution-snapshot digest
                                  {:agent-id "publication_translator" :model "test-model"
                                   :thinking :off :system-prompt "Translate." :tool-ids []})
   :clock (constantly "2026-09-12T00:00:00.000Z") :digest-hex digest :emit! emit!})

(defn- deferred []
  (let [deliver* (atom nil)
        promise (js/Promise. (fn [resolve! _reject!] (reset! deliver* resolve!)))]
    {:promise promise :deliver! (fn [value] (@deliver* value))}))

(defn- ^:async await-settlement [promise]
  (let [timer* (atom nil)
        deadline (js/Promise.
                  (fn [_resolve! reject!]
                    (reset! timer* (js/setTimeout
                                    #(reject! (js/Error. "Scoped turn did not settle within 10 seconds"))
                                    10000))))]
    (try (await (js/Promise.race #js [promise deadline]))
         (finally (js/clearTimeout @timer*)))))

(defn- emitter [config recorded deliver!]
  (^:async fn [raw]
    (let [event (normalize/normalize-event raw)]
      (swap! recorded assoc :event event)
      (await (runner/register-event-turn-settler! (:event/id event) deliver!))
      (await (actions/run-action!
              {:config config :event event :event/trusted? true
               :trigger {:trigger/id "scope-proof" :trigger/emitter "knoxx-publication"}}
              action))
      {:matchedTriggers [:scope-proof]})))

(defn- checked-hydration [recorded config ctx]
  (let [scope (planner-scope/session-options config)]
    (swap! recorded assoc :hydration-scope scope :context (:auth-context ctx)))
  [nil nil [] nil])

(t/deftest ^:async admission-scope-reaches-an-executed-durable-turn
  (await
   (fixture/with-run!
    {:run_id "scope-seed" :session_id "scope-seed" :conversation_id "scope-seed"}
    (^:async fn []
      (let [recorded (atom {})
            {:keys [promise deliver!]} (deferred)
            config {:openplanner-org-id "ambient-org" :session-project-name "ambient-project"}]
        (runner/reset-event-turn-queue!)
        (runner/reset-event-turn-settlers!)
        (try
          (with-redefs [tooling/resolve-agent-contract (fn ([_ _] resolved-agent) ([_ _ _] resolved-agent))
                        runner/current-runtime (constantly {})
                        policy/validate-chat-policy! (fn [_ _] true)
                        policy/enforce-chat-policy! (fn [_ _] true)
                        titles/maybe-prime-session-title! (fn [& _] nil)
                        turns/hydrate-and-materialize! (fn [_ cfg ctx _] (checked-hydration recorded cfg ctx))
                        turns/prompt-and-await!
                        (fn [cfg session-id run-id & _]
                          (swap! recorded assoc :model-config cfg :run-id run-id :session-id session-id)
                          {:run_id run-id :answer "finite model response"})]
            (let [result (await (dispatch/dispatch-work!
                                 (dispatch-dependencies (emitter config recorded deliver!))
                                 {:document :example.documents/post :locale :de
                                  :revision "sha256-source" :replace-stale? false}
                                 (context) "One source paragraph."))
                  settlement (await (await-settlement promise))
                  run-id (:run-id @recorded)]
              (t/is (= "verified-member" (get-in result [:dispatch/record :dispatch/membership-id])))
              (t/is (= verified-scope (get-in @recorded [:event :event/payload :scope])))
              (t/is (= {:org_id "verified-org" :project "verified-project"} (:hydration-scope @recorded)))
              (t/is (= verified-scope (select-keys (:context @recorded) [:org-id :membership-id :project])))
              (t/is (authority/authorized? (:context @recorded)))
              (t/is (= :completed (:event-turn/status settlement)))
              (t/is (some? run-id) "The real turn reached its finite model boundary")
              (when run-id
                (await (run-events/flush! run-id))
                (let [stored (await (runs/get-run @registry/session-store* run-id))
                      thread (await (sessions/get-session (:session-id @recorded)))]
                  (t/is (= "verified-org" (:org_id stored)))
                  (t/is (= "verified-member" (:membership_id stored)))
                  (t/is (= run-id (:run_id thread)))))))
          (finally
            (runner/reset-event-turn-queue!)
            (runner/reset-event-turn-settlers!))))))))

(t/deftest untrusted-or-unbound-scope-never-reaches-spawn
  (let [calls (atom 0)
        resolutions (atom 0)
        scoped-action {:action/kind :actions/start-agent-session
                       :action/with {:agent-id "publication_translator" :scope-from-event true}}
        base {:config {} :event/trusted? true
              :trigger {:trigger/emitter "knoxx-publication"}
              :event {:event/actor "knoxx-publication" :event/payload {:scope verified-scope}}}]
    (with-redefs [tooling/resolve-agent-contract (fn ([_ _] (swap! resolutions inc) resolved-agent)
                                                ([_ _ _] (swap! resolutions inc) resolved-agent))
                  runner/spawn-direct! (fn ([_ _] (swap! calls inc)) ([_ _ _] (swap! calls inc)))]
      (doseq [ctx [(assoc base :event/trusted? false)
                   (assoc-in base [:trigger :trigger/emitter] "other")
                   (update-in base [:event :event/payload] dissoc :scope)
                   (assoc-in base [:event :event/payload :scope :roleSlugs] ["system_admin"])]]
        (let [error (try (actions/run-action! ctx scoped-action) nil
                         (catch :default caught caught))]
          (t/is (contains? #{"untrusted_event_policy_overlay" "invalid_event_scope"}
                           (:code (ex-data error))) (str error))))
      (t/is (zero? @calls))
      (t/is (zero? @resolutions)))))

(t/deftest event-scope-is-opt-in-and-cannot-mint-extra-role-authority
  (let [spawned (atom nil)
        base {:config {:openplanner-org-id "ambient-org" :session-project-name "ambient-project"}
              :event/trusted? true :trigger {:trigger/emitter "knoxx-publication"}
              :event {:event/actor "knoxx-publication"
                      :event/payload {:scope verified-scope :roleSlugs ["system_admin"]}}}
        scoped-action {:action/kind :actions/start-agent-session
                       :action/with {:agent-id "publication_translator" :scope-from-event true}}]
    (with-redefs [tooling/resolve-agent-contract (fn ([_ _] resolved-agent) ([_ _ _] resolved-agent))
                  runner/spawn-direct! (fn ([cfg payload] (reset! spawned [cfg payload]))
                                          ([_ cfg payload] (reset! spawned [cfg payload])))]
      (actions/run-action! base scoped-action)
      (let [[cfg payload] @spawned
            ctx (:auth_context payload)]
        (t/is (= "verified-org" (:openplanner-org-id cfg)))
        (t/is (= "verified-project" (:session-project-name cfg)))
        (t/is (= ["translator"] (:roleSlugs ctx)))
        (t/is (= verified-scope (select-keys ctx [:org-id :membership-id :project])))
        (t/is (authority/authorized? ctx))
        (t/is (not (authority/authorized? (assoc verified-scope ::authority/authority "forged")))))
      (actions/run-action! (assoc base :event/trusted? false)
                           (update scoped-action :action/with dissoc :scope-from-event))
      (t/is (= (:config base) (first @spawned)))
      (t/is (nil? (:auth_context (second @spawned)))))))

(t/deftest ^:async historical-claim-without-membership-requires-a-new-admission
  (let [emitted (atom [])
        deps (dispatch-dependencies (fn [event] (swap! emitted conj event)
                                       {:matchedTriggers [:scope-proof]}))
        work {:document :example.documents/post :locale :de
              :revision "sha256-source" :replace-stale? false}
        historical (dissoc (dispatch-law/dispatch-record
                            work (context) :dispatch/accepted "2026-09-12T00:00:00.000Z"
                            :attempt-id "historical-attempt")
                           :dispatch/membership-id)]
    (t/is (= historical (dispatch-law/assert-record! historical)))
    (await (evidence/reserve-dispatch! (:evidence-store deps) historical))
    (let [refused (await (dispatch/dispatch-work! deps work (context) "One source paragraph."))]
      (t/is (= :dispatch/failed (:dispatch/outcome refused)))
      (t/is (empty? @emitted)))
    (let [retry (await (dispatch/dispatch-work! deps work (context) "One source paragraph."))]
      (t/is (= :dispatch/accepted (:dispatch/outcome retry)))
      (t/is (= "verified-member" (get-in retry [:dispatch/record :dispatch/membership-id])))
      (t/is (not= "historical-attempt" (get-in retry [:dispatch/record :dispatch/attempt-id])))
      (t/is (= [verified-scope] (mapv #(get-in % [:event/payload :scope]) @emitted))))))
