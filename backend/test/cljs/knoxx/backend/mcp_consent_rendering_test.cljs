(ns knoxx.backend.mcp-consent-rendering-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.routes.mcp.consent :as consent]))

(defn- render-page
  [overrides]
  (consent/page
   (merge {:base (js/URL. "https://knoxx.example")
           :auth-context {:user {:email "person@example.test"}
                          :org {:slug "open-hax"}
                          :actor {:binding "open_hax"}}
           :client-id "client"
           :redirect-uri "https://client.example/callback"
           :state nil
           :code-challenge "challenge"
           :requested-scope ""
           :tools (clj->js [{:name "semantic_query"
                             :label "Semantic query"
                             :description "Read the indexed corpus"}])
           :selected #{"semantic_query"}}
          overrides)))

(deftest preserves-consent-form-contract
  (let [output (render-page {})]
    (is (str/starts-with? output "<!doctype html>"))
    (is (str/includes? output "<form action=\"/api/mcp/oauth/authorize/confirm\" method=\"GET\">"))
    (is (str/includes? output "name=\"state\" type=\"hidden\" value=\"\""))
    (is (str/includes? output "name=\"scope\" type=\"hidden\" value=\"\""))
    (is (str/includes? output "name=\"actor_id\" type=\"hidden\" value=\"open_hax\""))
    (is (str/includes? output "checked name=\"tool\" type=\"checkbox\" value=\"semantic_query\""))
    (is (str/includes? output "<strong>Acting as:</strong> open_hax"))))

(deftest actorless-consent-explains-the-cost
  (let [output (render-page {:auth-context {:user {:email "person@example.test"}
                                             :org {:slug "open-hax"}
                                             :actor {:binding nil}}})]
    (is (str/includes? output "<strong>No actor</strong> is bound to this session"))
    (is (str/includes? output "name=\"actor_id\" type=\"hidden\" value=\"\""))))

(deftest every-dynamic-consent-field-is-escaped
  (let [payload "</span><script data-attack=\"1\">owned</script><span>"
        output (render-page
                {:auth-context {:user {:email payload}
                                :org {:slug payload}
                                :actor {:binding payload}}
                 :client-id payload
                 :redirect-uri (str "https://client.example/\"" payload)
                 :state payload
                 :code-challenge payload
                 :requested-scope payload
                 :tools (clj->js [{:name payload
                                   :label payload
                                   :description payload}])
                 :selected #{payload}})]
    (testing "untrusted values never become elements"
      (is (not (str/includes? output "<script data-attack=\"1\">")))
      (is (not (str/includes? output "</span><script"))))
    (testing "the content remains visible as escaped text and attributes"
      (is (str/includes? output "&lt;script data-attack=\"1\"&gt;owned&lt;/script&gt;"))
      (is (str/includes? output "&quot;&lt;/span&gt;&lt;script")))
    (testing "the hostile tool may still be selected without changing markup"
      (is (str/includes? output "checked name=\"tool\" type=\"checkbox\"")))))
