(ns knoxx.backend.extern-mcp-token-test
  "Regression coverage for the MCP token-record conversion boundary."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.mcp-token :as mcp-token]))

(deftest native-record-converts-cljs-data-once
  (let [record (mcp-token/native-record
                {:accessToken "authentication-contract"
                 :clientId "knoxx-authentication-contract"
                 :userEmail "admin@example.test"
                 :actorId "system_admin"
                 :tools ["semantic_query" "read"]})]
    (testing "the route receives the native field names its integration reads"
      (is (= "admin@example.test" (aget record "userEmail")))
      (is (= "system_admin" (aget record "actorId"))))
    (testing "nested CLJS collections become native arrays at the boundary"
      (is (array? (aget record "tools")))
      (is (= ["semantic_query" "read"]
             (vec (array-seq (aget record "tools"))))))))

(deftest native-record-preserves-absence
  (is (nil? (mcp-token/native-record nil))))

(deftest native-record-refuses-malformed-authorization-data
  (testing "required identity is checked before native conversion"
    (is (thrown-with-msg?
         js/Error
         #"Invalid MCP token record"
         (mcp-token/native-record
          {:accessToken "authentication-contract"
           :clientId "knoxx-authentication-contract"
           :userEmail ""
           :tools ["semantic_query"]}))))
  (testing "tool grants must use the exact vector-of-names contract"
    (is (thrown-with-msg?
         js/Error
         #"Invalid MCP token record"
         (mcp-token/native-record
          {:accessToken "authentication-contract"
           :clientId "knoxx-authentication-contract"
           :userEmail "admin@example.test"
           :tools :all})))))
