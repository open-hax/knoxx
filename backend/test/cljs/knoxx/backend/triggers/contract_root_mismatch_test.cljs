(ns knoxx.backend.triggers.contract-root-mismatch-test
  "Test that documents the production contract root mismatch.

   Production logs show contract roots that may not contain triggers.
   This test verifies triggers are discoverable from the workspace.

   This test verifies the mismatch and ensures we don't silently ignore it."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.resources.loader :as resources]))

(deftest production-contract-root-is-not-where-triggers-live
  (testing "the contract root used in production does not contain trigger files"
    (let [roots (contract-loader/contract-root-paths {})
          _ (js/console.log "[test] contract roots: " (pr-str roots))]
      (is (seq roots) "Should have at least one root")
      (doseq [root roots]
        (let [trigger-dir (.join (js/require "node:path") root "triggers")
              has-triggers (try
                             (seq (.readdirSync (js/require "node:fs") trigger-dir))
                             (catch :default _ false))]
          (when has-triggers
            (is true (str "Root " root " has triggers (this is where they should be)")))
          (when-not has-triggers
            (is false (str "Root " root " has NO triggers directory — triggers will not be found!"))))))))

(deftest trigger-files-exist-somewhere-in-workspace
  (testing "trigger files exist somewhere in the workspace"
    (let [           possible-roots [(.resolve (js/require "node:path") (.cwd js/process) "contracts")
                          (.resolve (js/require "node:path") (.cwd js/process) ".." "contracts")
                          (.resolve (js/require "node:path") (.cwd js/process) ".." ".." "contracts")]
          trigger-file (some (fn [root]
                               (let [f (.join (js/require "node:path") root "triggers" "ussyverse_social_replies_event.edn")]
                                 (when (.existsSync (js/require "node:fs") f)
                                   f)))
                             possible-roots)]
      (is (some? trigger-file)
          (str "Trigger file should exist somewhere. Checked: " (pr-str possible-roots))))))

(deftest trigger-not-found-when-roots-mismatch
  (testing "when contract roots point to wrong directory, an error is thrown"
    (let [wrong-config {:contracts-dir "/tmp/nonexistent"}]
      (try
        (resources/resource-sync wrong-config :trigger "ussyverse_social_replies_event")
        (is false "Should have thrown an error")
        (catch :default err
          (is (some? err) "Error should be thrown when contract root is wrong"))))))
