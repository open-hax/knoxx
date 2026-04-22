(ns knoxx.backend.contract-validator-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.contract-loader :as loader]
            [knoxx.backend.runtime.contract-validator :as validator]))

(deftest actor-contract-kind-is-agent-or-user
  (testing "user actors validate"
    (is (:ok (validator/validate "actors"
                                 {:actor/id "workspace_user"
                                  :actor/kind :user
                                  :actor/roles [:role/knowledge-worker]}))))
  (testing "legacy human actor kinds are rejected"
    (is (false? (:ok (validator/validate "actors"
                                         {:actor/id "legacy_human"
                                          :actor/kind :human
                                          :actor/roles []}))))))

(deftest policy-contract-schema-accepts-user-supplied-shape
  (let [result (validator/validate "policies"
                                   {:contract/id "actor_capability_surface_parity"
                                    :contract/kind :policy
                                    :contract/doc "Actor-centric parity policy"
                                    :contract/scope "actors roles capabilities"
                                    :contract/uses ["actors" "roles" "capabilities"]
                                    :policy/invariants [{:id "actor-kind"
                                                         :severity :block
                                                         :message "Actors must be :agent or :user."
                                                         :check {:rule :actor-kind-known}}]
                                    :policy/required [{:id :capability-surface-tool-parity
                                                       :severity :block
                                                       :message "Capabilities need both tool and human surfaces."
                                                       :check {:expr '(and (:cap/tools capability)
                                                                           (:cap/user-surfaces capability))}}]
                                    :policy/checked-by :contract-validator})]
    (is (:ok result) (pr-str result))))

(deftest capability-contract-schema-accepts-user-surface-mappings
  (let [result (validator/validate "capabilities"
                                   {:cap/id :cap/read
                                    :cap/tools [:read]
                                    :cap/user-surfaces [{:surface/id :workspace/file-browser
                                                         :surface/label "Workspace file browser"
                                                         :surface/kind :reader
                                                         :surface/routes ["/"]
                                                         :surface/endpoints ["/api/documents/content/:path"]
                                                         :surface/description "Humans browse files; agents call read."}]})]
    (is (:ok result) (pr-str result))))

(deftest loader-normalizes-policy-class
  (is (= "policies" (loader/normalize-contract-class "policy")))
  (is (= "policies" (loader/normalize-contract-class "policies"))))
