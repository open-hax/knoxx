(ns knoxx.backend.open-hax-policy-test
  (:require [cljs.test :refer [deftest is testing]]
            [open-hax.contracts.policy.fulfillment :as fulfillment]
            [open-hax.contracts.policy.gate :as gate]))

(deftest gate-policy-matches-tool-params
  (testing "exact and predicate param matches are accepted"
    (let [policy {:policy/action :warn
                  :policy/reason "large request"
                  :policy/match {:tool/name "memory.search"
                                 :tool/params {:workspace "knoxx"
                                               :k #(> % 20)}}}
          result (gate/evaluate-gates [policy]
                                      {:tool/name "memory.search"
                                       :tool/params {:workspace "knoxx" :k 50}})]
      (is (= :warn (:action result)))
      (is (= policy (:policy result)))
      (is (= [policy] (:matches result))))))

(deftest gate-policy-chooses-strongest-active-action
  (testing "expired policies are ignored and strongest active action wins"
    (let [expired {:policy/action :block
                   :policy/ttl-ms 10
                   :policy/match {:tool/name "write"}}
          note {:policy/action :note
                :policy/match {:tool/name "write"}}
          warn {:policy/action :warn
                :policy/reason "review write"
                :policy/match {:tool/name "write"}}
          result (gate/evaluate-gates [expired note warn]
                                      {:tool/name "write" :tool/params {}}
                                      200
                                      0)]
      (is (= :warn (:action result)))
      (is (= warn (:policy result)))
      (is (= [note warn] (:matches result))))))

(deftest fulfillment-matches-tool-params-and-interpolates-message
  (testing "fulfillment params support exact values and predicates"
    (let [contract {:fulfillment/mode :audit
                    :fulfillment/level :info
                    :fulfillment/message "Rendered {file} with {tool/name}"
                    :fulfillment/match {:tool/name "openutau.render"
                                        :tool/params {:file "song.ustx"
                                                      :duration #(>= % 10)}}}
          tool-result {:tool/name "openutau.render"
                       :tool/params {:file "song.ustx" :duration 12}
                       :tool/status :ok}
          actions (fulfillment/evaluate-fulfillments [contract] tool-result)]
      (is (= 1 (count actions)))
      (is (= {:mode :audit
              :message "Rendered song.ustx with openutau.render"
              :level :info
              :fulfill contract}
             (first actions))))))

(deftest fulfillment-rejects-non-matching-error-state
  (testing "error presence can be matched explicitly"
    (let [contract {:fulfillment/match {:tool/name "danger.tool"
                                        :tool/error? true}}
          ok-result {:tool/name "danger.tool" :tool/error nil}]
      (is (empty? (fulfillment/evaluate-fulfillments [contract] ok-result))))))
