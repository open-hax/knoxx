(ns knoxx.backend.triggers.trigger-loading-test
  "Verify trigger resources are loaded correctly from a configured contracts root.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest trigger-resources-are-loadable
  (testing "trigger resources can be loaded from the fixture root"
    (let [all-resources (resources/load-all-resources-sync fixture-config)
          trigger-resources (filter #(= :trigger (:resource/kind %)) all-resources)
          trigger-defs (map :resource/definition trigger-resources)]
      (is (seq all-resources)
          (str "Should load resources from disk. Count: " (count all-resources)))
      (is (seq trigger-resources)
          (str "Should find trigger resources. Found: " (count trigger-resources)
               " kinds: " (pr-str (distinct (map :resource/kind all-resources)))))
      (is (seq trigger-defs)
          "Trigger definitions should not be nil")
      (when (seq trigger-resources)
        (let [trigger-ids (map :resource/id trigger-resources)]
          (is (some #(= "ussyverse_social_replies_event" %) trigger-ids)
              (str "Should find ussyverse trigger. IDs: " (pr-str trigger-ids))))))))

(deftest trigger-loading-cached-vs-uncached
  (testing "cached and uncached loading should both find triggers"
    (contract-loader/invalidate-sync-contract-cache!)
    (let [uncached (resources/load-all-resources-sync fixture-config)]
      (is (seq uncached) "Uncached load should find resources"))
    ;; Second call should use cache
    (let [cached (resources/load-all-resources-sync fixture-config)]
      (is (seq cached) "Cached load should also find resources"))))

(deftest status-snapshot-shows-triggers
  (testing "status-snapshot should list triggers including ussyverse"
    (let [status (event-dispatch/status-snapshot fixture-config)
          trigger-ids (map :id (:triggers status))]
      (is (seq trigger-ids)
          (str "Status snapshot should list triggers. Found: " (count trigger-ids)))
      (is (some #(= "ussyverse_social_replies_event" %) trigger-ids)
          (str "Should include ussyverse trigger. IDs: " (pr-str trigger-ids))))))
