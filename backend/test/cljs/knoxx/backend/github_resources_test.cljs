(ns knoxx.backend.github-resources-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.control.catalog :as control-catalog]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.driver.registry :as driver-registry]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(defn- contract-records
  [relative-path]
  (let [file-path (.join path ".." "contracts" relative-path)
        edn-text (.readFileSync fs file-path "utf8")]
    (contract-loader/parse-contract-file-records! file-path edn-text)))

(defn- contracts-by-kind
  [records kind]
  (->> records
       (keep (fn [{:keys [contract]}]
               (when (= kind (:contract/kind contract))
                 contract)))
       vec))

(deftest github-automation-uses-resolvable-axxium-bound-authority
  (let [actor-records (contract-records "actors/github_automation.edn")
        role-records (contract-records "roles/github_event_observer.edn")
        capability-records
        (contract-records "capabilities/cap_github_event_observer.edn")
        actor (:contract (first actor-records))
        role (:contract (first role-records))
        capability (:contract (first capability-records))]
    (testing "the actor, role, and capability contracts all parse"
      (is (every? :ok? (concat actor-records role-records capability-records)))
      (is (= "github_automation" (:actor/id actor)))
      (is (= :agent (:actor/kind actor)))
      (is (= :service (:actor/type actor))))
    (testing "provider account, installation, and object identities come from Axxium"
      (is (= :axxium (get-in actor [:actor/accounts :github :identity/supplier])))
      (is (= :required (get-in actor [:actor/accounts :github :principal-binding])))
      (is (= :required (get-in actor [:actor/accounts :github :installation-binding])))
      (is (= :required (get-in actor [:actor/accounts :github :object-binding]))))
    (testing "authorization claims resolve through canonical role and capability resources"
      (is (= [:role/github-event-observer] (:actor/roles actor)))
      (is (= [:cap/github-event-observer] (:actor/capabilities actor)))
      (is (= :role/github-event-observer (:role/id role)))
      (is (= [:cap/github-event-observer] (:role/capabilities role)))
      (is (= :cap/github-event-observer (:cap/id capability)))
      (is (= #{:events.status
               :events.dispatch
               :actors.send-message
               :agents.spawn
               :graph_query
               :semantic_read}
             (set (:cap/tools capability)))))))

(deftest github-resources-separate-driver-signal-ledger-and-projection
  (driver-builtin/register-built-in-drivers!)
  (let [records (contract-records "namespaces/github.edn")
        source (first (contracts-by-kind records :source))
        stores (contracts-by-kind records :store)
        stores-by-id (into {} (map (juxt :store/id identity) stores))
        ledger (get stores-by-id :github/event-ledger)
        projection (get stores-by-id :github/object-index)
        selected-types (set (:source/listens source))
        emitted-types (set (driver-registry/emitted-event-types
                            :driver/github-app))]
    (testing "all namespace resources parse through the canonical contract loader"
      (is (= 3 (count records)))
      (is (every? :ok? records)))
    (testing "the code-level driver owns event shapes and the source only selects them"
      (is (true? (driver-registry/registered-driver? :driver/github-app)))
      (is (= :driver/github-app (:source/driver source)))
      (is (nil? (:source/emits source)))
      (is (= emitted-types selected-types))
      (is (= #{:github.delivery-received
               :github.object-observed
               :github.coverage-observed
               :github.ledger-discovered}
             selected-types))
      (is (true? (get-in (control-catalog/catalog records)
                         [:admissibility :ok?]))))
    (testing "webhook delivery remains at-least-once with explicit reconciliation"
      (is (= :github/app-events (:source/id source)))
      (is (false? (:enabled source)))
      (is (= :webhook (get-in source [:source/protocol :message-transport])))
      (is (= :at-least-once (get-in source [:source/protocol :delivery])))
      (is (= :after-signature-verification
             (get-in source [:source/protocol :ack-mode])))
      (is (= :github/deliveries-and-api
             (get-in source [:source/protocol :reconcile])))
      (is (= :explicit (get-in source [:source/protocol :completeness]))))
    (testing "Axxium identity and the Clio record profile cross the interaction boundary"
      (is (= :axxium (get-in source [:source/protocol :identity-supplier])))
      (is (= :clio/event-record-v1
             (get-in source [:source/protocol :record-profile]))))
    (testing "the append-only ledger remains authority and the Knoxx graph is rebuildable"
      (is (= :event-ledger/nd-edn (:store/driver ledger)))
      (is (= ".ημ/ledgers/github" (:store/path ledger)))
      (is (= :immutable-segments (get-in ledger [:store/mirror :mode])))
      (is (false? (:enabled ledger)))
      (is (= :knoxx/projection (:store/driver projection)))
      (is (= :event-ledger (get-in projection [:store/projection :authority])))
      (is (true? (get-in projection [:store/projection :rebuildable?])))
      (is (false? (:enabled projection))))))
