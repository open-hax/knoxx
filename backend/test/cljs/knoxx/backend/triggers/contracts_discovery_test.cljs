(ns knoxx.backend.triggers.contracts-discovery-test
  "Verify contract roots are discoverable from a configured contracts dir.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned
   and its contents must never be asserted on by the test suite."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.resources.loader :as resources]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest contracts-directory-is-discoverable
  (testing "a configured contracts dir resolves to a single existing root"
    (let [roots (contract-loader/contract-root-paths fixture-config)]
      (is (= 1 (count roots))
          "A non-default :contracts-dir should resolve to exactly one root")
      (is (every? #(.existsSync (js/require "node:fs") %) roots)
          (str "Configured contract root should exist. Roots: " (pr-str roots))))))

(deftest generated-contract-root-is-lower-priority-and-opt-in
  (let [config (assoc fixture-config
                      :generated-contracts-dir "test/fixtures/empty-contracts")
        roots (contract-loader/contract-root-paths config)]
    (is (= 2 (count roots)))
    (is (.endsWith (first roots) "test/fixtures/trigger-contracts"))
    (is (.endsWith (second roots) "test/fixtures/empty-contracts"))))

(deftest trigger-files-are-discoverable
  (testing "trigger files can be discovered under the configured root"
    (let [roots (contract-loader/contract-root-paths fixture-config)
          trigger-files (mapcat #(try
                                   (.readdirSync (js/require "node:fs")
                                                 (.join (js/require "node:path") % "triggers")
                                                 #js {:withFileTypes true :recursive true})
                                   (catch :default _ []))
                                roots)
          edn-files (filter #(and (.isFile %)
                                  (.endsWith (.-name %) ".edn"))
                            trigger-files)]
      (is (seq edn-files)
          (str "Should discover trigger .edn files. Found: " (count edn-files))))))

(deftest ussyverse-trigger-is-loadable-from-disk
  (testing "the fixture trigger can be loaded synchronously from disk"
    (let [trigger (resources/resource-sync fixture-config :trigger "ussyverse_social_replies_event")]
      (is (some? trigger)
          "Trigger should be loadable from the configured fixture root"))))

(deftest all-triggers-loadable
  (testing "all trigger resources under the fixture root load without errors"
    (let [trigger-ids (resources/list-resource-ids-sync fixture-config :trigger)]
      (is (seq trigger-ids)
          (str "Should find trigger IDs under the fixture root. Found: " (count trigger-ids)))
      (doseq [id trigger-ids]
        (let [trigger (resources/resource-sync fixture-config :trigger id)]
          (is (some? trigger)
              (str "Trigger " id " should be loadable")))))))
