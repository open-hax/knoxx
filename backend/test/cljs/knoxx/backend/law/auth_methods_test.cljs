(ns knoxx.backend.law.auth-methods-test
  "Every way the authentication contract must refuse.

   The e2e suite proves the rule works over a socket; these prove it refuses,
   one reason per test, without needing a server. A rule that decides whether
   an unauthenticated caller reaches the tool surface deserves both."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.auth-methods :as law]))

(def ^:private token "e2e-loopback-token")

(defn- contract
  "The e2e-shaped contract, with `overrides` merged into its loopback method."
  ([] (contract {}))
  ([overrides]
   {:contract/kind :authentication
    :contract/id "mcp_http"
    :auth/surface :mcp
    :auth/methods
    [{:auth-method/id :oauth-bearer :auth-method/enabled true}
     (merge {:auth-method/id :trusted-loopback
             :auth-method/enabled true
             :auth-method/require-loopback true
             :auth-method/require-non-production true
             :auth-method/token-env "KNOXX_E2E_LOOPBACK_TOKEN"
             :auth-method/min-token-length 8
             :auth-method/grants {:grant/user-email "system-admin@open-hax.local"
                                  :grant/actor-id "e2e_actor"
                                  :grant/tools :all}}
            overrides)]}))

(defn- request
  [overrides]
  (merge {:configured-token token
          :presented-token  token
          :remote-address   "127.0.0.1"
          :production?      false}
         overrides))

(deftest loopback-address?-test
  (testing "every form Node reports for this machine"
    (is (law/loopback-address? "127.0.0.1"))
    (is (law/loopback-address? "::1"))
    (is (law/loopback-address? "::ffff:127.0.0.1"))
    (is (law/loopback-address? "localhost"))
    (is (law/loopback-address? "  127.0.0.1  ") "whitespace is not a distinct address")
    (is (law/loopback-address? "LOCALHOST") "case is not a distinct address"))

  (testing "anything else is a remote peer"
    (is (not (law/loopback-address? "10.0.0.4")))
    (is (not (law/loopback-address? "127.0.0.1.evil.example")))
    (is (not (law/loopback-address? "::ffff:10.0.0.4")))
    (is (not (law/loopback-address? nil)))
    (is (not (law/loopback-address? "")))))

(deftest enabled-methods-test
  (testing "a disabled method is not accepted"
    (let [c (contract {:auth-method/enabled false})]
      (is (law/method-enabled? c :mcp :oauth-bearer))
      (is (not (law/method-enabled? c :mcp :trusted-loopback)))))

  (testing "a method with no :auth-method/enabled key is not accepted"
    (let [c (contract {:auth-method/enabled nil})]
      (is (not (law/method-enabled? c :mcp :trusted-loopback))
          "absence must not read as permission")))

  (testing "a contract for another surface says nothing about this one"
    (let [c (assoc (contract) :auth/surface :something-else)]
      (is (empty? (law/enabled-methods c :mcp)))
      (is (not (law/method-enabled? c :mcp :oauth-bearer)))))

  (testing "no contract at all accepts nothing"
    (is (empty? (law/enabled-methods nil :mcp)))
    (is (not (law/method-enabled? nil :mcp :oauth-bearer)))))

(deftest grant-of-test
  (testing "a grant names an identity and a tool set"
    (let [grant (law/grant-of (second (:auth/methods (contract))))]
      (is (= "system-admin@open-hax.local" (:user-email grant)))
      (is (= "e2e_actor" (:actor-id grant)))
      (is (= :all (:tools grant)))))

  (testing "a method granting no email grants nothing"
    (is (nil? (law/grant-of {:auth-method/grants {:grant/tools :all}}))
        "a blank identity is a valid bearer carrying no authorization"))

  (testing "a method granting no tools grants nothing"
    (is (nil? (law/grant-of {:auth-method/grants {:grant/user-email "a@b.c"}}))))

  (testing "a method with no grants at all grants nothing"
    (is (nil? (law/grant-of {:auth-method/id :trusted-loopback}))))

  (testing "an explicit tool list is normalized, not passed through"
    (is (= ["semantic_query" "graph_query"]
           (:tools (law/grant-of {:auth-method/grants
                                  {:grant/user-email "a@b.c"
                                   :grant/tools ["semantic_query" "  " "graph_query"]}}))))))

(deftest trusted-loopback-admits-a-satisfied-request-test
  (is (some? (law/trusted-loopback-grant (contract) :mcp (request {}))))
  (is (some? (law/trusted-loopback-grant (contract) :mcp (request {:remote-address "::1"}))))
  (is (some? (law/trusted-loopback-grant (contract) :mcp
                                         (request {:presented-token (str "  " token "  ")})))
      "surrounding whitespace does not change the credential"))

(deftest trusted-loopback-refuses-off-loopback-test
  (testing "a remote caller is refused even holding the token"
    (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:remote-address "10.0.0.4"}))))
    (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:remote-address nil}))))
    (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:remote-address ""})))))

  (testing "a method that does not require loopback admits a remote caller"
    (is (some? (law/trusted-loopback-grant (contract {:auth-method/require-loopback false})
                                           :mcp (request {:remote-address "10.0.0.4"})))
        (str "the guard is the contract's to declare — this is why the shipped "
             "contract sets it, and why the e2e one does too"))))

(deftest trusted-loopback-refuses-in-production-test
  (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:production? true}))))
  (is (some? (law/trusted-loopback-grant (contract {:auth-method/require-non-production false})
                                          :mcp (request {:production? true})))
      "a contract may opt out, but must say so explicitly"))

(deftest trusted-loopback-refuses-bad-tokens-test
  (testing "an unconfigured process cannot be admitted"
    (is (nil? (law/trusted-loopback-grant (contract) :mcp
                                          (request {:configured-token nil :presented-token nil}))))
    (is (nil? (law/trusted-loopback-grant (contract) :mcp
                                          (request {:configured-token "" :presented-token ""})))
        "a blank config must not match a blank Authorization header"))

  (testing "a configured token under the contract's floor is refused"
    (is (nil? (law/trusted-loopback-grant (contract) :mcp
                                          (request {:configured-token "short"
                                                    :presented-token "short"})))))

  (testing "a mismatched or partial token is refused"
    (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:presented-token nil}))))
    (is (nil? (law/trusted-loopback-grant (contract) :mcp (request {:presented-token ""}))))
    (is (nil? (law/trusted-loopback-grant (contract) :mcp
                                          (request {:presented-token (subs token 0 8)})))
        "a prefix is not a match")
    (is (nil? (law/trusted-loopback-grant (contract) :mcp
                                          (request {:presented-token (str token "x")}))))))

(deftest trusted-loopback-refuses-a-disabled-or-absent-method-test
  (is (nil? (law/trusted-loopback-grant (contract {:auth-method/enabled false}) :mcp (request {})))
      "disabled is refused even when every guard is satisfied")
  (is (nil? (law/trusted-loopback-grant nil :mcp (request {})))
      "no contract is refused")
  (is (nil? (law/trusted-loopback-grant {:auth/surface :mcp :auth/methods []} :mcp (request {})))
      "a contract declaring no methods is refused")
  (is (nil? (law/trusted-loopback-grant (contract {:auth-method/grants nil}) :mcp (request {})))
      "an enabled method that grants nothing is refused rather than permissive"))
