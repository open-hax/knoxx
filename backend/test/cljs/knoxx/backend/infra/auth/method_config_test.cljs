(ns knoxx.backend.infra.auth.method-config-test
  "Grant construction stays in CLJS; extern.mcp-token owns native conversion."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.auth.method-config :as method-config]))

(deftest grant-token-record-is-cljs-data
  (let [record (method-config/grant->token-record
                {:user-email "admin@example.test"
                 :org-slug "open-hax"
                 :actor-id "system_admin"
                 :tools :all}
                ["read" "semantic_query"])]
    (is (map? record))
    (is (= "admin@example.test" (:userEmail record)))
    (is (= "system_admin" (:actorId record)))
    (is (= ["read" "semantic_query"] (:tools record)))))
