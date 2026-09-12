(ns knoxx.backend.shape.agent-chat-input-test
  "Portable checks for normalized caller fields at chat admission."
  (:require [knoxx.backend.shape.agent-chat-input :as input]
            #?(:clj [clojure.test :as test]
               :cljs [cljs.test :as test])))

(test/deftest compact-overrides-preserve-explicit-values
  (test/is (= {:enabled false :limit 0 :role "author"}
              (input/compact-agent-spec-overrides
               {:empty nil :blank "  " :sources [] :enabled false :limit 0 :role "author"}))))

(test/deftest requested-policy-precedence-retains-denials
  (let [allow {:toolId "read" :effect "allow"}
        deny {:tool-id "write" :effect "deny"}]
    (test/is (= [deny] (input/requested-tool-policies
                       {:agent-spec {:tool-policies [deny]} :auth-context {:toolPolicies [allow]}})))
    (test/is (= [allow] (input/requested-tool-policies {:auth-context {:tool-policies [allow]}})))
    (test/is (false? (input/allow-policy? deny)))
    (test/is (= "write" (input/tool-policy-id deny)))))

(test/deftest actor-selection-preserves-authority-and-ignores-blank-overrides
  (let [ctx {:org-id "org-a" :permissions ["read"]}]
    (test/is (= ctx (input/auth-context-with-actor ctx " ")))
    (test/is (= (assoc ctx :actorId "actor-a") (input/auth-context-with-actor ctx " actor-a ")))
    (test/is (= "writer" (input/requested-role {:auth-context {:role " writer "}})))))
