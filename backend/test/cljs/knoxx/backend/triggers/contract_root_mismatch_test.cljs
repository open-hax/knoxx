(ns knoxx.backend.triggers.contract-root-mismatch-test
  "Test contract root resolution behavior.

   A configured :contracts-dir must be honored as the only root, and pointing
   at a missing directory must fail loudly instead of silently finding nothing.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.resources.loader :as resources]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest configured-contract-root-contains-triggers
  (testing "the configured contract root resolves and contains trigger files"
    (let [roots (contract-loader/contract-root-paths fixture-config)]
      (is (seq roots) "Should have at least one root")
      (doseq [root roots]
        (let [trigger-dir (.join (js/require "node:path") root "triggers")
              has-triggers (try
                             (seq (.readdirSync (js/require "node:fs") trigger-dir))
                             (catch :default _ false))]
          (is has-triggers
              (str "Root " root " has NO triggers directory — triggers will not be found!")))))))

(deftest fixture-trigger-file-exists
  (testing "the fixture trigger file exists under the configured root"
    (let [root (first (contract-loader/contract-root-paths fixture-config))
          trigger-file (.join (js/require "node:path")
                              root "triggers" "ussyverse_social_replies_event.edn")]
      (is (.existsSync (js/require "node:fs") trigger-file)
          (str "Trigger fixture should exist at: " trigger-file)))))

(deftest trigger-not-found-when-roots-mismatch
  (testing "when contract roots point to wrong directory, an error is thrown"
    (let [wrong-config {:contracts-dir "/tmp/nonexistent"}]
      (try
        (resources/resource-sync wrong-config :trigger "ussyverse_social_replies_event")
        (is false "Should have thrown an error")
        (catch :default err
          (is (some? err) "Error should be thrown when contract root is wrong"))))))
