(ns knoxx.backend.security.event-policy-authority-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.agent.event-policy-authority :as authority]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.publication-draft-tool :as draft-tool]
            [knoxx.backend.infra.routes.tools :as tool-routes]
            [knoxx.backend.infra.tooling :as tooling]))

(def ^:private policies
  {:publication-draft? true
   :source-document-id :docs/source
   :source-revision "sha256-source"
   :source-locale :en
   :gardens [{:garden/id :gardens/main :garden/locales [:en :es]}]
   :org-id "org-a"
   :membership-id "membership-a"})

(def ^:private resolved-agent
  {:actor-id "pi"
   :role "publication-drafter"
   :model "gemma4:e2b"
   :thinking-level :off
   :system-prompt "Craft a post."
   :tool-ids ["save_publication_draft"]
   :tool-policies [{:toolId "save_publication_draft" :effect "allow"}]})

(defn- draft-action
  []
  {:action/kind :actions/start-agent-session
   :action/with {:agent-id "publication_post_drafter"
                 :resource-policies-from-event true
                 :task "Craft one draft."}})

(defn- draft-context
  [trusted?]
  {:config {}
   :event/trusted? trusted?
   :trigger {:trigger/id "publication/craft-post-from-indexed-document"
             :trigger/emitter "knoxx-publication"}
   :event {:event/id "indexed-event"
           :event/type :publication/document-indexed
           :event/actor "knoxx-publication"
           :event/payload {:content "source"
                           :resource-policies policies}}})

(deftest untrusted-event-policy-never-spawns-an-agent
  (let [spawn-count* (atom 0)
        error (with-redefs [tooling/resolve-agent-contract
                            (fn
                              ([_config _agent-id] resolved-agent)
                              ([_config _agent-id _actor-id] resolved-agent))
                            agent-runner/spawn-direct!
                            (fn
                              ([_config _payload] (swap! spawn-count* inc))
                              ([_runtime _config _payload]
                               (swap! spawn-count* inc)))]
                (try
                  (action-registry/run-action! (draft-context false)
                                               (draft-action))
                  nil
                  (catch :default cause cause)))]
    (is (= "untrusted_event_policy_overlay" (:code (ex-data error))))
    (is (= 403 (:status (ex-data error))))
    (is (zero? @spawn-count*))))

(deftest mismatched-emitter-never-spawns-an-agent
  (let [spawn-count* (atom 0)
        error (with-redefs [tooling/resolve-agent-contract
                            (fn
                              ([_config _agent-id] resolved-agent)
                              ([_config _agent-id _actor-id] resolved-agent))
                            agent-runner/spawn-direct!
                            (fn
                              ([_config _payload] (swap! spawn-count* inc))
                              ([_runtime _config _payload]
                               (swap! spawn-count* inc)))]
                (try
                  (action-registry/run-action!
                   (assoc-in (draft-context true) [:event :event/actor]
                             "caller-controlled-source")
                   (draft-action))
                  nil
                  (catch :default cause cause)))]
    (is (= "untrusted_event_policy_overlay" (:code (ex-data error))))
    (is (zero? @spawn-count*))))

(deftest trusted-trigger-mints-non-json-event-authority
  (let [spawned* (atom nil)]
    (with-redefs [tooling/resolve-agent-contract
                  (fn
                    ([_config _agent-id] resolved-agent)
                    ([_config _agent-id _actor-id] resolved-agent))
                  agent-runner/spawn-direct!
                  (fn
                    ([_config payload]
                     (reset! spawned* payload)
                     {:ok true})
                    ([_runtime _config payload]
                     (reset! spawned* payload)
                     {:ok true}))]
      (action-registry/run-action! (draft-context true) (draft-action)))
    (let [auth-context (:auth_context @spawned*)
          forged (authority/authorized-context
                  policies "pi" "publication-drafter" [])]
      (is (authority/authorized? auth-context))
      (is (= policies (:resourcePolicies auth-context)))
      (testing "a separately created JSON-shaped map cannot copy object identity"
        (is (authority/authorized? forged))
        (is (not (authority/authorized?
                  (assoc (dissoc forged
                                 :knoxx.backend.infra.agent.event-policy-authority/authority)
                         :knoxx.backend.infra.agent.event-policy-authority/authority
                         (js/Object.)))))))))

(deftest ^:async direct-draft-tool-policy-without-authority-is-refused
  (let [execute (draft-tool/make-save-draft-execute
                 {:resourcePolicies policies})
        error (try
                (await (execute nil {} "tool-call" #js {:content "# Draft"}
                                nil nil nil))
                nil
                (catch :default cause cause))]
    (is (= :publication-draft-authority-required (:code (ex-data error))))
    (is (= 403 (:status (ex-data error))))))

(deftest ^:async dispatch-provenance-is-not-an-event-field
  (let [seen* (atom [])
        trigger {:resource/id "capture"
                 :resource/kind :trigger
                 :resource/definition
                 {:contract/id "capture"
                  :trigger/kind :event
                  :trigger/events [:test/provenance]
                  :trigger/action :test/capture
                  :action/fn (fn [ctx _]
                               (swap! seen* conj (:event/trusted? ctx))
                               (js/Promise.resolve {:ok true}))
                  :enabled true}}]
    (with-redefs [resources/load-all-resources-sync (fn [_] [trigger])]
      (event-dispatch/reset-dedup!)
      (await (event-dispatch/dispatch-external!
              {} {:event/id "external" :event/type :test/provenance
                  :event/trusted? true}))
      (await (event-dispatch/dispatch!
              {} {:event/id "internal" :event/type :test/provenance
                  :event/trusted? false})))
    (is (= [false true] @seen*))))

(deftest operator-ingress-rebinds-every-actor-alias
  (let [event (tool-routes/operator-dispatch-event
               {:actorId "authenticated-actor"}
               {:event/actor "spoof-a"
                :actorId "spoof-b"
                :actor-id "spoof-c"
                :event/type :manual/test})]
    (is (= "authenticated-actor" (:event/actor event)))
    (is (not (contains? event :actorId)))
    (is (not (contains? event :actor-id)))))
