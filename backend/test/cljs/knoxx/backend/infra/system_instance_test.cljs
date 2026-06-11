(ns knoxx.backend.infra.system-instance-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.system-instance :as system-instance]))

(deftest current-id-test
  (testing "current-id is a stable non-blank string for the process lifetime"
    (let [id (system-instance/current-id)]
      (is (string? id))
      (is (seq id))
      (is (= id (system-instance/current-id))))))

(deftest owned-by-current-instance-test
  (testing "documents stamped by this instance are owned"
    (is (system-instance/owned-by-current-instance?
         {:system_instance_id (system-instance/current-id)})))
  (testing "documents stamped by another instance are not owned"
    (is (not (system-instance/owned-by-current-instance?
              {:system_instance_id "00000000-0000-0000-0000-000000000000"}))))
  (testing "legacy documents without the field are never owned"
    (is (not (system-instance/owned-by-current-instance? {})))
    (is (not (system-instance/owned-by-current-instance? nil)))))
