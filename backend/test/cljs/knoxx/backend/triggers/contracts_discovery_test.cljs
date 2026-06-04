(ns knoxx.backend.triggers.contracts-discovery-test
  "Verify contracts are discoverable from the current working directory."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.resources.loader :as resources]))

(deftest contracts-directory-is-discoverable
  (testing "at least one contract root path exists on disk"
    (let [roots (contract-loader/contract-root-paths {})]
      (is (seq roots)
          "Should have at least one contract root candidate")
      (is (every? #(.existsSync (js/require "node:fs") %) roots)
          (str "All contract roots should exist. Roots: " (pr-str roots))))))

(deftest trigger-files-are-discoverable
  (testing "trigger files can be discovered from disk"
    (let [roots (contract-loader/contract-root-paths {})
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
  (testing "the ussyverse trigger can be loaded synchronously from disk"
    (let [trigger (resources/resource-sync {} :trigger "ussyverse_social_replies_event")]
      (is (some? trigger)
          "Trigger should be loadable from disk with default config"))))

(deftest all-triggers-loadable
  (testing "all trigger resources on disk load without errors"
    (let [trigger-ids (resources/list-resource-ids-sync {} :trigger)]
      (is (seq trigger-ids)
          (str "Should find trigger IDs on disk. Found: " (count trigger-ids)))
      (doseq [id trigger-ids]
        (let [trigger (resources/resource-sync {} :trigger id)]
          (is (some? trigger)
              (str "Trigger " id " should be loadable")))))))
