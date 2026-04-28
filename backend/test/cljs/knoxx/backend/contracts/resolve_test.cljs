(ns knoxx.backend.contracts.resolve-test
  (:require [cljs.test :as t :refer [deftest is testing]]
            [knoxx.backend.contracts.resolve :as sut]
            [knoxx.backend.contracts.actor-scope]))

(deftest actor-extras-test
  (let [actor-spec {:id "test-actor"
                   :kind :user
                   :system-prompt "You are a test agent"
                   :role-slugs [:developer]
                   :custom-field "extra data"
                   :another-field 42}]
    (testing "extracts unknown fields as extras"
      (let [extras (sut/actor-extras actor-spec)]
        (is (= {:custom-field "extra data" :another-field 42} extras))))))

(deftest actor-extras-no-extras-test
  (let [known-only {:id "known-actor"
                   :kind :user
                   :system-prompt "known"}
        extras (sut/actor-extras known-only)]
    (is (nil? extras))))

(deftest actor-extras-nil-test
  (is (nil? (sut/actor-extras nil))))

(deftest actor-extras-all-known-test
  (let [all-known {:id "actor"
                  :kind :user
                  :default-agent "agent-1"
                  :role-slugs [:dev]
                  :capability-ids [:read]
                  :system-prompt "prompt"
                  :task-prompt "task"
                  :thinking-level "high"
                  :model "gpt-5"
                  :contract-id "contract-1"
                  :model-profile "fast"
                  :tool-policies []}
        extras (sut/actor-extras all-known)]
    (is (nil? extras))))

(deftest known-actor-keys-include-test
  (testing "known-actor-keys is a set"
    (is (set? sut/known-actor-keys)))
  (testing "contains expected keys"
    (is (contains? sut/known-actor-keys :id)))
  (is (contains? sut/known-actor-keys :kind))
  (is (contains? sut/known-actor-keys :system-prompt)))
; ---------------------------------------------------------------------------
; Multi-role composition (agent-role-claims)
; ---------------------------------------------------------------------------

(deftest agent-role-claims-handles-single-role
  (testing "single :role keyword is returned as a one-element vector"
    (let [contract {:agent {:role :knowledge_worker}}
          claims   (knoxx.backend.contracts.actor-scope/agent-role-claims contract)]
      (is (= [:knowledge_worker] claims)))))

(deftest agent-role-claims-handles-roles-vector
  (testing ":agent {:roles [...]} returns all roles"
    (let [contract {:agent {:roles [:creative_catalyst :developer]}}
          claims   (knoxx.backend.contracts.actor-scope/agent-role-claims contract)]
      (is (= [:creative_catalyst :developer] claims)))))

(deftest agent-role-claims-merges-role-and-roles
  (testing ":role and :roles are merged and deduped"
    (let [contract {:agent {:role :knowledge_worker
                            :roles [:developer :knowledge_worker]}}
          claims   (knoxx.backend.contracts.actor-scope/agent-role-claims contract)]
      (is (= 2 (count claims)))
      (is (= (set claims) #{:knowledge_worker :developer})))))

(deftest agent-role-claims-legacy-actor-roles
  (testing ":actor/roles feeds into role claims alongside :agent {:role}"
    (let [contract {:actor/roles [:basic_user]
                    :agent {:role :knowledge_worker}}
          claims   (knoxx.backend.contracts.actor-scope/agent-role-claims contract)]
      (is (contains? (set claims) :basic_user))
      (is (contains? (set claims) :knowledge_worker)))))
