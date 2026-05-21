(ns knoxx.backend.contracts.resolve-test
  (:require [cljs.test :as t :refer [deftest is testing]]
            [knoxx.backend.domain.actor.scope]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.contracts.resolve :as sut]
            [knoxx.backend.domain.contracts.roles :as roles]))

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

(deftest resolve-agent-contract-composes-role-actor-and-agent-task-prompts
  (let [contract {:contract/id "agent-a"
                  :contract/kind :agent
                  :contract/actors ["actor-a"]
                  :trigger-kind :manual
                  :agent {:roles [:role/source-role]}
                  :prompts {:task "agent task"}}]
    (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                     (case class
                                                       "agents" {:id id :contract contract}
                                                       nil))
                  sut/default-actor-id (fn [_] "actor-a")
                  sut/resolve-actor (fn [_ _]
                                      {:id "actor-a"
                                       :role-slugs []
                                       :capability-ids []
                                       :task-prompt "actor task"})
                  roles/role-task-prompt (fn [_ role]
                                           (when (= "source-role" role)
                                             "role task"))
                  roles/role-system-prompt (fn [_ _] nil)
                  roles/role-tool-ids (fn [_ _] [])]
      (let [resolved (sut/resolve-agent-contract {} "agent-a")
            task (:task-prompt resolved)]
        (is (sequential? task))
        (is (= ["role task" "actor task" "agent task"] (nth task 2)))))))

(deftest resolve-agent-contract-composes-runtime-sources
  (let [source-contract {:contract/id "openplanner-memory"
                         :contract/kind :source
                         :source/id :source/openplanner-memory
                         :source/provider :openplanner
                         :source/hydration {:strategy :memory-search
                                            :mode :triggered
                                            :k 6}}
        agent-contract {:contract/id "agent-a"
                        :contract/kind :agent
                        :contract/actors ["actor-a"]
                        :trigger-kind :manual
                        :agent {:roles [:role/source-role]}
                        :sources [{:source/ref :source/openplanner-memory
                                   :hydration {:mode :always}}]}
        role-contract {:role/id :role/source-role
                       :role/sources [{:source/ref :source/openplanner-memory
                                       :hydration {:k 4}}]}]
    (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                     (case class
                                                       "agents" {:id id :contract agent-contract}
                                                       "sources" {:id id :contract source-contract}
                                                       nil))
                  sut/default-actor-id (fn [_] "actor-a")
                  sut/resolve-actor (fn [_ _]
                                      {:id "actor-a"
                                       :role-slugs []
                                       :capability-ids []
                                       :actor {:actor/sources [:source/openplanner-memory]}})
                  roles/role-contract (fn [_ role]
                                        (when (= "source-role" role)
                                          role-contract))
                  roles/role-task-prompt (fn [_ _] nil)
                  roles/role-system-prompt (fn [_ _] nil)
                  roles/role-tool-ids (fn [_ _] [])]
      (let [resolved (sut/resolve-agent-contract {} "agent-a")
            source (first (:sources resolved))]
        (is (= 1 (count (:sources resolved))))
        (is (= :source/openplanner-memory (:source/id source)))
        (is (= {:strategy :memory-search
                :mode :always
                :k 4}
               (:source/hydration source)))))))
; ---------------------------------------------------------------------------
; Multi-role composition (agent-role-claims)
; ---------------------------------------------------------------------------

(deftest agent-role-claims-handles-single-role
  (testing "single :role keyword is returned as a one-element vector"
    (let [contract {:agent {:role :knowledge_worker}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= [:knowledge_worker] claims)))))

(deftest agent-role-claims-handles-roles-vector
  (testing ":agent {:roles [...]} returns all roles"
    (let [contract {:agent {:roles [:creative_catalyst :developer]}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= [:creative_catalyst :developer] claims)))))

(deftest agent-role-claims-merges-role-and-roles
  (testing ":role and :roles are merged and deduped"
    (let [contract {:agent {:role :knowledge_worker
                            :roles [:developer :knowledge_worker]}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= 2 (count claims)))
      (is (= (set claims) #{:knowledge_worker :developer})))))

(deftest agent-role-claims-legacy-actor-roles
  (testing ":actor/roles feeds into role claims alongside :agent {:role}"
    (let [contract {:actor/roles [:basic_user]
                    :agent {:role :knowledge_worker}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (contains? (set claims) :basic_user))
      (is (contains? (set claims) :knowledge_worker)))))
