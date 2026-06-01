(ns knoxx.backend.agents.policy-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.redis-client :as redis]))

(defn- chat-context
  [constraints]
  {:membership {:id "membership-1"}
   :tool-policies [{:tool-id "agent.chat"
                    :constraints constraints}]})

(deftest ^:async enforce-chat-policy-allows-configured-models
  (testing "allowedModels accepts the requested model"
    (with-redefs [redis/get-client (fn [] nil)]
      (let [result (await (policy/enforce-chat-policy!
                           (chat-context {:allowedModels ["glm-5" "gpt-5"]})
                           "glm-5"))]
        (is (nil? result))))))

(deftest ^:async enforce-chat-policy-rejects-disallowed-models
  (testing "model allow-list failures carry API status metadata"
    (with-redefs [redis/get-client (fn [] nil)]
      (try
        (await (policy/enforce-chat-policy!
                (chat-context {:allowedModels ["glm-5" "gpt-5"]})
                "claude"))
        (is false "expected policy rejection")
        (catch :default err
          (is (= 403 (aget err "statusCode")))
          (is (= "model_not_allowed" (aget err "code")))
          (is (re-find #"glm-5" (.-message err))))))))

(deftest ^:async enforce-chat-policy-applies-redis-rate-limit
  (testing "rate limit increments per membership and rejects after maxRequests"
    (let [counts* (atom {})
          expirations* (atom [])
          fake-redis #js {:incr (fn [key]
                                  (let [n (inc (get @counts* key 0))]
                                    (swap! counts* assoc key n)
                                    (js/Promise.resolve n)))
                           :expire (fn [key seconds]
                                     (swap! expirations* conj [key seconds])
                                     (js/Promise.resolve true))}]
      (with-redefs [redis/get-client (fn [] fake-redis)]
        (let [ctx (chat-context {:maxRequests 2 :windowSeconds 60})]
          (is (nil? (await (policy/enforce-chat-policy! ctx "glm-5"))))
          (is (nil? (await (policy/enforce-chat-policy! ctx "glm-5"))))
          (try
            (await (policy/enforce-chat-policy! ctx "glm-5"))
            (is false "expected rate-limit rejection")
            (catch :default err
              (is (= 429 (aget err "statusCode")))
              (is (= "chat_rate_limited" (aget err "code"))))))
        (is (= [["knoxx:chat-rate:membership-1:60" 60]] @expirations*))))))

(deftest ^:async enforce-chat-policy-fails-open-when-redis-errors
  (testing "redis operational failures should not block chat"
    (let [fake-redis #js {:incr (fn [_]
                                  (js/Promise.reject (js/Error. "redis down")))
                           :expire (fn [_ _]
                                     (js/Promise.resolve true))}]
      (with-redefs [redis/get-client (fn [] fake-redis)]
        (let [result (await (policy/enforce-chat-policy!
                             (chat-context {:maxRequests 1 :windowSeconds 60})
                             "glm-5"))]
          (is (nil? result)))))))
