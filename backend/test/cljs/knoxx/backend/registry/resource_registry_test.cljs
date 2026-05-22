(ns knoxx.backend.registry.resource-registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.registry.resource :as registry]
            [knoxx.backend.domain.resources.loader :as resources]))

(def sample-records
  [{:id "spawn_session"
    :contractClass "actions"
    :contract {:action/id :action/spawn-session}}
   {:id "creative_tick"
    :contractClass "schedules"
    :contract {:schedule/id :schedule/creative-tick}}
   {:id "creative_loop"
    :contractClass "triggers"
    :contract {:trigger/id :trigger/creative-loop}}])

(deftest resource-loader-normalizes-resource-kind
  (testing "resource names are singular even when storage directories are plural"
    (is (= :action (resources/normalize-resource-kind "actions")))
    (is (= :agent (resources/normalize-resource-kind :agent-specs)))
    (is (= "schedules" (resources/resource-class :schedule)))))

(deftest registry-records-advertise-resource-catalogs
  (with-redefs [contract-loader/load-all-contracts-sync (fn [_] sample-records)]
    (testing "each registry has a stable resource kind"
      (is (= :action (registry/registry-resource-kind registry/actions-registry)))
      (is (= :schedule (registry/registry-resource-kind registry/schedules-registry))))
    (testing "catalogs are maps from resource kind to visible ids"
      (is (= {:catalog/resources {:action ["spawn_session"]}}
             (registry/registry-catalog registry/actions-registry {})))
      (is (= {:catalog/resources {:action ["spawn_session"]
                                  :schedule ["creative_tick"]
                                  :trigger ["creative_loop"]}}
             (registry/catalog {} [:action :schedule :trigger]))))))
