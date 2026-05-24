(ns knoxx.backend.registry.resource-registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.driver.registry :as driver-registry]
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
    :contract {:trigger/id :trigger/creative-loop}}
   {:id "discord_gateway"
    :contractClass "sources"
    :contract {:source/id :source/discord-gateway}}])

(deftest resource-loader-normalizes-resource-kind
  (testing "resource names are singular even when storage directories are plural"
    (is (= :action (resources/normalize-resource-kind "actions")))
    (is (= :agent (resources/normalize-resource-kind :agent-specs)))
    (is (= "schedules" (resources/resource-class :schedule)))))

(deftest source-drivers-are-code-protocols
  (driver-builtin/register-built-in-drivers!)
  (testing "source resources select events from code-level drivers"
    (is (true? (driver-registry/registered-driver? :driver/discord)))
    (is (= [:discord.message]
           (driver-registry/source-listens {:source/listens [:discord.message]})))
    (is (true? (driver-registry/listened-by-driver?
                {:source/driver :driver/discord
                 :source/listens [:discord.message]}))))
  (testing "driver helper attaches source actor/provenance before dispatch"
    (let [event (driver-registry/source-event
                 (driver-registry/driver :driver/discord)
                 {:source/id :source/discord-gateway
                  :source/actor "discord_automation"}
                 {:event/type :discord.message
                  :event/payload {:content "hello"}})]
      (is (= "discord_automation" (:event/actor event)))
      (is (= {:kind :discord
              :driver :driver/discord
              :source :source/discord-gateway}
             (:event/generator event))))))

(deftest registry-records-advertise-resource-catalogs
  (with-redefs [contract-loader/load-all-contracts-sync (fn [_] sample-records)]
    (testing "each registry has a stable resource kind"
      (is (= :action (registry/registry-resource-kind registry/actions-registry)))
      (is (= :schedule (registry/registry-resource-kind registry/schedules-registry)))
      (is (= :source (registry/registry-resource-kind (registry/registry :source)))))
    (testing "catalogs are maps from resource kind to visible ids"
      (is (= {:catalog/resources {:action ["spawn_session"]}}
             (registry/registry-catalog registry/actions-registry {})))
      (is (= {:catalog/resources {:action ["spawn_session"]
                                  :schedule ["creative_tick"]
                                  :trigger ["creative_loop"]
                                  :source ["discord_gateway"]}}
             (registry/catalog {} [:action :schedule :trigger :source]))))))
