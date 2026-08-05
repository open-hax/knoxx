(ns knoxx.backend.mcp-params-test
  "Request parsing for the MCP OAuth endpoints.

   Malli's {:optional true} governs whether a key must be *present*, not what a
   present value may be — so a nil under a `string?` entry fails validation. The
   register-client parser built its map with `some->`, which yields nil when the
   field is absent, so a dynamic client registration that simply omitted
   client_name — which the spec permits — was rejected as invalid_client_metadata
   and could never obtain a token."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.mcp.params :as params]))

(defn- request
  "A Fastify-shaped request carrying this body."
  [body]
  #js {:body (clj->js body)})

(deftest register-client-accepts-a-body-without-a-client-name
  (testing "client_name is optional in dynamic client registration"
    (let [parsed (params/parse-register-client-body
                  (request {:redirect_uris ["https://chatgpt.com/connector_platform_oauth_redirect"]}))]
      (is (= ["https://chatgpt.com/connector_platform_oauth_redirect"]
             (:redirect-uris parsed)))
      (is (not (contains? parsed :client-name))
          "absent must mean absent, not present-and-nil"))))

(deftest register-client-keeps-a-client-name-when-given
  (let [parsed (params/parse-register-client-body
                (request {:redirect_uris ["https://x.test/cb"] :client_name "ChatGPT"}))]
    (is (= "ChatGPT" (:client-name parsed)))))

(deftest register-client-treats-a-blank-name-as-absent
  (testing "a blank name is not a name, and must not fail the schema either"
    (let [parsed (params/parse-register-client-body
                  (request {:redirect_uris ["https://x.test/cb"] :client_name "   "}))]
      (is (not (contains? parsed :client-name))))))

(deftest register-client-still-requires-a-redirect-uri
  (testing "the refusal that matters is kept"
    (is (thrown? js/Error (params/parse-register-client-body (request {}))))
    (is (thrown? js/Error (params/parse-register-client-body
                           (request {:redirect_uris []}))))))
