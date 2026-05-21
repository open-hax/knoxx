(ns knoxx.backend.row-extra-codecs-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.row-extra :as row-extra]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.stores.session-titles :as session-titles]))

(deftest row-extra-codecs-preserve-current-shapes
  (testing "CLJS maps pass through unchanged"
    (is (= {:title "Launch"}
           (row-extra/parse-session-title-extra {:title "Launch"})))
    (is (= {:org_id "org-1"}
           (row-extra/parse-core-memory-extra {:org_id "org-1"}))))
  (testing "JSON object strings decode to keyword maps while preserving key spelling"
    (is (= {:title "Launch" :titleModel "model-a"}
           (session-titles/parse-json-object "{\"title\":\"Launch\",\"titleModel\":\"model-a\"}")))
    (is (= {:org_id "org-1" :membership-id "mem-1" :actorId "actor-1"}
           (core-memory/parse-json-object "{\"org_id\":\"org-1\",\"membership-id\":\"mem-1\",\"actorId\":\"actor-1\"}"))))
  (testing "invalid and non-object JSON remain rejected"
    (is (nil? (row-extra/parse-session-title-extra "{")))
    (is (nil? (row-extra/parse-core-memory-extra "[1,2,3]")))
    (is (nil? (session-titles/parse-json-object nil)))
    (is (= {} (core-memory/row-extra-map {:extra "[1,2,3]"})))))

(deftest session-extra-compatibility-accepts-snake-kebab-and-camel-keys
  (testing "semantic lookups retain legacy key compatibility"
    (is (= "contract-snake"
           (core-memory/session-contract-id-from-rows
            [{:extra "{\"contract_id\":\"contract-snake\"}"}])))
    (is (= "actor-kebab"
           (core-memory/session-actor-id-from-rows
            [{:extra {:actor-id "actor-kebab"}}])))
    (is (= "sub-camel"
           (core-memory/session-sub-agent-id-from-rows
            [{:extra "{\"subAgentId\":\"sub-camel\"}"}])))))

(deftest ^:async authorized-session-ids-aggregates-to-cljs-set
  (testing "promise aggregation returns only allowed session ids as a CLJS set"
    (let [ctx {:orgId "org-1"
               :membershipId "mem-1"
               :userId "user-1"
               :permissions []}]
      (with-redefs [core-memory/fetch-openplanner-session-rows!
                    (fn [_config session-id]
                      (js/Promise.resolve
                       (case session-id
                         "allowed" [{:extra {:org_id "org-1"
                                             :membership_id "mem-1"}}]
                         "denied" [{:extra {:org_id "org-2"
                                            :membership_id "mem-2"}}]
                         (throw (js/Error. "unexpected session")))))]
        (let [allowed (await (core-memory/authorized-session-ids! {} ctx ["allowed" "denied" "allowed"]))]
          (is (set? allowed))
          (is (= #{"allowed"} allowed)))))))
