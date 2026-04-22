(ns knoxx.backend.agent-turns-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agent-turns :as agent-turns]))

(deftest ensure-session-id-preserves-provided-value
  (testing "existing session ids are kept intact"
    (let [crypto #js {:randomUUID (fn [] "generated-session-id")}]
      (is (= "existing-session-id"
             (agent-turns/ensure-session-id crypto "existing-session-id"))))))

(deftest ensure-session-id-generates-missing-value
  (testing "missing session ids are generated before provider requests"
    (let [crypto #js {:randomUUID (fn [] "generated-session-id")}]
      (is (= "generated-session-id"
             (agent-turns/ensure-session-id crypto nil))))))