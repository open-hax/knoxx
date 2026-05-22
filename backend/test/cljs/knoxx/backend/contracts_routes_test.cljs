(ns knoxx.backend.contracts-routes-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.contracts :as sut]
            [knoxx.backend.infra.redis-client :as redis]))

(def fixture-config
  {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"})

(defn- capture-json
  []
  (let [captured (atom nil)]
    {:captured captured
     :send! (fn [_status body] (reset! captured body))}))

;; ---------------------------------------------------------------------------
;; Invariant: resource index sync is a no-op (not a crash) when Redis is absent.
;; The old sync-contract-index! name remains as a compatibility alias.
;; ---------------------------------------------------------------------------

(deftest ^:async sync-returns-promise-when-redis-absent
  (testing "sync-resource-index! always returns a Promise even with no Redis client"
    (with-redefs [redis/get-client (fn [] nil)]
      (let [p (sut/sync-resource-index! {})]
        (is (instance? js/Promise p) "must be a Promise")
        (let [result (await p)]
          (is (false? (:ok result)))
          (is (= "Redis not connected" (:error result))))))))

(deftest ^:async sync-does-not-throw-when-redis-absent
  (testing "sync-contract-index! compatibility alias resolves instead of rejecting"
    (with-redefs [redis/get-client (fn [] nil)]
      (try
        (let [result (await (sut/sync-contract-index! {}))]
          (is (map? result) "resolves to a plain map"))
        (catch :default err
          (is false (str "must not reject when Redis absent: " (.-message err))))))))

(deftest sync-no-redis-error-key-is-informative
  (testing "the no-Redis sentinel message names the root cause unambiguously"
    (with-redefs [redis/get-client (fn [] nil)]
      (let [p (sut/sync-contract-index! {})]
        (is (instance? js/Promise p))))))

;; ---------------------------------------------------------------------------
;; Resource route handlers list resources; compatibility contract handlers still
;; return the old {:contracts [...]} shape for existing clients.
;; ---------------------------------------------------------------------------

(deftest ^:async list-resources-handler-returns-resource-shape
  (let [{:keys [captured send!]} (capture-json)]
    (await (sut/handle-list-resources send! fixture-config "agents"))
    (is (seq (:resources @captured)) "expected resources")
    (is (every? #(= "agents" (:resourceClass %)) (:resources @captured))
        "all returned resources must be agents")))

(deftest list-contracts-handler-returns-promise
  (testing "/api/admin/contracts handler produces a Promise"
    (let [{:keys [send!]} (capture-json)
          p (sut/handle-list-contracts send! fixture-config nil)]
      (is (instance? js/Promise p)))))

(deftest ^:async list-contracts-handler-returns-non-empty
  (testing "/api/admin/contracts body has at least one contract"
    (let [{:keys [captured send!]} (capture-json)]
      (await (sut/handle-list-contracts send! fixture-config nil))
      (is (seq (:contracts @captured))
          (str "expected contracts, got: " (pr-str @captured))))))

(deftest ^:async list-contracts-handler-all-have-id-and-class
  (let [{:keys [captured send!]} (capture-json)]
    (await (sut/handle-list-contracts send! fixture-config nil))
    (doseq [c (:contracts @captured)]
      (is (string? (:id c)) (str "missing :id " (pr-str c)))
      (is (string? (:contractClass c))
          (str "missing :contractClass " (pr-str c))))))

(deftest ^:async list-contracts-handler-class-filter
  (let [{:keys [captured send!]} (capture-json)]
    (await (sut/handle-list-contracts send! fixture-config "agents"))
    (is (seq (:contracts @captured)) "expected agents")
    (is (every? #(= "agents" (:contractClass %)) (:contracts @captured))
        "all returned contracts must be class agents")))

(deftest validate-contract-edn-surfaces-agent-shape-warnings
  (let [result (#'sut/validate-contract-edn
                "agents"
                "{:contract/id \"warny\"
                  :contract/kind :agent
                  :trigger-kind :cron
                  :source {:max-messages 2000}
                  :agent {:role :contract_writer}
                  :prompts {:task \"update :data/world_state\"}
                  :data {:filter {:publishChannels [\"123\"]}
                         :source {:max-messages 2000}
                         :plot_log []}}")
        messages (set (map :message (:warnings result)))]
    (is (:ok result))
    (is (some #(str/includes? % ":data/:filter") messages))
    (is (some #(str/includes? % "top-level :source") messages))
    (is (some #(str/includes? % "clamped to 100") messages))
    (is (some #(str/includes? % "mutable runtime state") messages))
    (is (some #(str/includes? % "Role refs should use") messages))
    (is (some #(str/includes? % "Prompt references mutable :data") messages))))
