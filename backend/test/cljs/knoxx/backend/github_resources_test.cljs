(ns knoxx.backend.github-resources-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
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

(deftest github-automation-uses-axxium-identity
  (let [records (contract-records "actors/github_automation.edn")
        actor (:contract (first records))]
    (testing "the actor contract parses and remains disabled from secret material"
      (is (= 1 (count records)))
      (is (every? :ok? records))
      (is (= "github_automation" (:actor/id actor)))
      (is (= :agent (:actor/kind actor)))
      (is (= :service (:actor/type actor))))
    (testing "provider account, installation, and object identities come from Axxium"
      (is (= :axxium (get-in actor [:actor/accounts :github :identity/supplier])))
      (is (= :required (get-in actor [:actor/accounts :github :principal-binding])))
      (is (= :required (get-in actor [:actor/accounts :github :installation-binding])))
      (is (= :required (get-in actor [:actor/accounts :github :object-binding]))))))

(deftest github-resources-separate-signal-ledger-and-projection
  (let [records (contract-records "namespaces/github.edn")
        source (first (contracts-by-kind records :source))
        stores (contracts-by-kind records :store)
        stores-by-id (into {} (map (juxt :store/id identity) stores))
        ledger (get stores-by-id :github/event-ledger)
        projection (get stores-by-id :github/object-index)
        emitted-types (set (map :event/type (:source/emits source)))]
    (testing "all namespace resources parse through the canonical contract loader"
      (is (= 3 (count records)))
      (is (every? :ok? records)))
    (testing "webhook delivery is an at-least-once signal with explicit reconciliation"
      (is (= :github/app-events (:source/id source)))
      (is (false? (:source/enabled? source)))
      (is (= :webhook (get-in source [:source/protocol :message-transport])))
      (is (= :at-least-once (get-in source [:source/protocol :delivery])))
      (is (= :after-signature-verification
             (get-in source [:source/protocol :ack-mode])))
      (is (= :github/deliveries-and-api
             (get-in source [:source/protocol :reconcile])))
      (is (= :explicit (get-in source [:source/protocol :completeness])))
      (is (contains? emitted-types :github/delivery-received))
      (is (contains? emitted-types :github/object-observed))
      (is (contains? emitted-types :github/coverage-observed))
      (is (contains? emitted-types :github/ledger-discovered)))
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
