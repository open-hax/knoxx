(ns knoxx.backend.e2e.mcp-auth-e2e
  "The authentication contract, exercised over a real socket.

   These assertions are about the door: who gets in, who does not, and whether
   getting in is the same thing as being allowed to act. The tool surface
   itself is mcp-tools-e2e."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            [knoxx.backend.infra.auth.method-config :as method-config]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.law.auth-methods :as law]))

(deftest ^:async trusted-loopback-admits-the-harness-test
  (let [started (await (harness/start!))]
    (try
      (let [init (await (mcp/initialize! (harness/client started)))]
        (is (:ok init) (str "initialize refused: " (pr-str (:error init))))
        (is (= 200 (:status init))))
      (finally (await (harness/stop! started))))))

(deftest ^:async unauthenticated-calls-are-refused-test
  (let [started (await (harness/start!))]
    (try
      (testing "no bearer at all"
        (let [init (await (mcp/initialize! (harness/client started nil)))]
          (is (not (:ok init)))
          (is (= 401 (:status init)))))

      (testing "a bearer that is not the contract's token"
        (let [init (await (mcp/initialize! (harness/client started "not-the-token")))]
          (is (not (:ok init)))
          (is (= 401 (:status init))
              (str "the e2e contract enables OAuth, so an unknown bearer reaches "
                   "that store and misses; a contract without OAuth refuses earlier"))))

      (testing "a bearer under the contract's length floor"
        (let [init (await (mcp/initialize! (harness/client started "short")))]
          (is (not (:ok init)))
          (is (= 401 (:status init)))))
      (finally (await (harness/stop! started))))))

(deftest ^:async grant-is-not-authorization-test
  (let [started (await (harness/start!
                        (harness/policy-context
                         (harness/context-with-tool-policies ["semantic_query"]))))]
    (try
      (let [client (harness/client started)
            _      (await (mcp/initialize! client))
            listed (await (mcp/list-tools! client))
            names  (into #{} (map :name) (get-in listed [:result :tools]))]
        (is (:ok listed) (str "tools/list refused: " (pr-str (:error listed))))
        (is (contains? names "semantic_query"))

        (testing "the contract's :all grant does not override the membership"
          (is (not (contains? names "bluesky_publish"))
              (str "a grant must be intersected with policy, not substituted "
                   "for it; got " (pr-str names)))
          (is (not (contains? names "discord_send")))
          (is (> 5 (count names))
              (str "a membership allowing one tool reached " (count names)
                   ": " (pr-str names))))

        ;; Recorded because it is surprising rather than because it is wrong.
        ;; openplanner-tool-builders authorizes one factory with
        ;; ["graph_query" "semantic_query"] and any-tool-allowed?, so allowing
        ;; either id exposes the tool that factory builds. Anyone narrowing a
        ;; membership to semantic_query should know graph_query comes with it.
        (is (= #{"semantic_query" "graph_query"} names)
            (str "the semantic/graph factory pair changed shape; got "
                 (pr-str names))))
      (finally (await (harness/stop! started))))))

(defn- ^:async exchange-status!
  [base-url content-type code]
  (let [fields {:grant_type "authorization_code"
                :code code
                :client_id "synthetic-client"
                :redirect_uri "http://localhost/callback"
                :code_verifier "synthetic-verifier"}
        body (if (= "application/json" content-type)
               (js/JSON.stringify (clj->js fields))
               (str (js/URLSearchParams. (clj->js fields))))
        response (await (js/fetch (str base-url "/api/mcp/oauth/token")
                                  #js {:method "POST"
                                       :headers #js {:content-type content-type}
                                       :body body}))]
    (await (.text response))
    (.-status response)))

(defn- ^:async mcp-method-status!
  [base-url method bearer]
  (let [response (await (js/fetch (str base-url "/mcp")
                                  #js {:method method
                                       :headers #js {:authorization (str "Bearer " bearer)}}))]
    (await (.text response))
    (.-status response)))

(deftest ^:async private-subscription-credentials-do-not-enter-ordinary-authority-test
  (let [{:keys [base-url] :as started} (await (harness/start!))
        lookups* (atom [])
        db #js {:collection
                (fn [name]
                  #js {:findOne
                       (fn [filter]
                         (swap! lookups* conj [name (js->clj filter)])
                         (js/Promise.resolve nil))})}
        private-bearer "knxsub1_synthetic-private-bearer"
        private-code "knxsubc1_synthetic-private-authorization-code"
        ordinary-bearer "synthetic-ordinary-oauth-bearer"
        ordinary-code "synthetic-ordinary-authorization-code"]
    (try
      (with-redefs [mongo-client/get-db (constantly db)]
        (testing "legacy loopback still initializes"
          (is (= 200 (:status (await (mcp/initialize! (harness/client started)))))))

        (testing "the reserved bearer cannot use loopback even if it matches the configured secret"
          (aset js/process.env "KNOXX_E2E_LOOPBACK_TOKEN" private-bearer)
          (let [init (await (mcp/initialize! (harness/client started private-bearer)))]
            (is (= 401 (:status init)))))

        (testing "a reserved bearer stays out of the legacy OAuth and session paths"
          (let [init (await (mcp/initialize! (harness/client started "KNXSUB1_synthetic-unknown")))]
            (is (= 401 (:status init))))
          (is (empty? @lookups*))
          (doseq [method ["GET" "DELETE"]]
            (is (= 401 (await (mcp-method-status! base-url method private-bearer))) method)
            (is (= 400 (await (mcp-method-status! base-url method ordinary-bearer))) method))
          (is (= 401 (:status (await (mcp/initialize! (harness/client started ordinary-bearer))))))
          (is (= [["knoxx_mcp_tokens" {"access_token" ordinary-bearer}]] @lookups*)))

        (testing "private codes cannot be looked up via either native token exchange encoding"
          (doseq [content-type ["application/json" "application/x-www-form-urlencoded"]]
            (reset! lookups* [])
            (is (= 400 (await (exchange-status! base-url content-type private-code))) content-type)
            (is (empty? @lookups*))
            (is (= 400 (await (exchange-status! base-url content-type ordinary-code))) content-type)
            (is (= [["knoxx_mcp_clients" {"client_id" "synthetic-client"}]
                    ["knoxx_mcp_codes" {"code" ordinary-code}]]
                   @lookups*)))))
      (finally (await (harness/stop! started))))))

;; ── the shipped contract, read as data ──────────────────────────────────────

(defn- shipped-contract
  []
  (method-config/contract-for {:contracts-dir "../contracts"} method-config/mcp-surface))

(defn- shipped-loopback-method
  []
  (some #(when (= :trusted-loopback (:auth-method/id %)) %)
        (law/enabled-methods (shipped-contract) :mcp)))

(deftest shipped-contract-is-loadable-test
  (let [contract (shipped-contract)]
    (is (some? contract) "the shipped authentication contract must be loadable")
    (is (law/method-enabled? contract :mcp :oauth-bearer)
        "OAuth is the shipped path and must stay on")))

(deftest shipped-loopback-method-keeps-its-guards-test
  ;; The shipped contract permits :trusted-loopback so the post-deploy gate can
  ;; verify production. These are the guards that make that safe, and each one
  ;; is a line somebody could delete without obviously breaking anything.
  (let [method (shipped-loopback-method)]
    (is (some? method) "the shipped contract no longer permits :trusted-loopback")
    (is (true? (:auth-method/require-loopback method))
        (str "require-loopback is the load-bearing guard in production, where "
             "require-non-production is deliberately off"))
    (is (<= 16 (:auth-method/min-token-length method))
        "a production shared secret must not be allowed to be short")
    (is (= "KNOXX_MCP_LOOPBACK_TOKEN" (:auth-method/token-env method))
        "the deploy gate and the contract must name the same variable")))

(deftest shipped-loopback-is-inert-without-a-secret-test
  ;; The off switch. An image deployed without KNOXX_MCP_LOOPBACK_TOKEN must
  ;; have no second way in, whatever a caller presents — including the empty
  ;; string, which is what an unset variable reads as.
  (let [loopback {:remote-address "127.0.0.1" :production? true}]
    (is (nil? (law/trusted-loopback-grant
               (shipped-contract) :mcp
               (assoc loopback :configured-token nil :presented-token "anything")))
        "an unconfigured deployment granted access")
    (is (nil? (law/trusted-loopback-grant
               (shipped-contract) :mcp
               (assoc loopback :configured-token "" :presented-token "")))
        "a blank configured secret matched a blank Authorization header")
    (is (nil? (law/trusted-loopback-grant
               (shipped-contract) :mcp
               (assoc loopback :configured-token "short-secret" :presented-token "short-secret")))
        "a secret under the contract's floor was accepted")))

(deftest shipped-loopback-refuses-off-box-callers-test
  (let [configured "a-sufficiently-long-production-secret"]
    (is (some? (law/trusted-loopback-grant
                (shipped-contract) :mcp
                {:configured-token configured :presented-token configured
                 :remote-address "127.0.0.1" :production? true}))
        "the in-container gate must be admitted in production")
    (is (nil? (law/trusted-loopback-grant
               (shipped-contract) :mcp
               {:configured-token configured :presented-token configured
                :remote-address "10.0.0.4" :production? true}))
        (str "an off-box caller holding the production secret was admitted; "
             "require-loopback is the only thing standing between this method "
             "and the public internet"))))

(deftest shipped-grant-names-the-bootstrap-actor-test
  ;; Without an actor the gate cannot reach credential-backed tools, which is
  ;; most of the surface it exists to verify.
  (let [grant (law/grant-of (shipped-loopback-method))]
    (is (some? grant) "the shipped loopback method grants nothing")
    (is (= "system_admin" (:actor-id grant))
        (str "the deploy gate runs as the shipped bootstrap actor; changing this changes "
             "which Discord and Bluesky accounts a verification run touches"))
    (is (= :all (:tools grant)))))
