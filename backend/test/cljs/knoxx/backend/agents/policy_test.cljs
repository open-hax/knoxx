(ns knoxx.backend.agents.policy-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.stores.mongo-rate-limits :as mongo-rate-limits]))

(defn- chat-context
  [constraints]
  {:membership {:id "membership-1"}
   :tool-policies [{:tool-id "agent.chat"
                    :constraints constraints}]})

(deftest ^:async enforce-chat-policy-allows-configured-models
  (testing "allowedModels accepts the requested model"
    (let [result (await (policy/enforce-chat-policy!
                         (chat-context {:allowedModels ["glm-5" "gpt-5"]})
                         "glm-5"))]
      (is (nil? result)))))

(deftest ^:async enforce-chat-policy-rejects-disallowed-models
  (testing "model allow-list failures carry API status metadata"
    (try
      (await (policy/enforce-chat-policy!
              (chat-context {:allowedModels ["glm-5" "gpt-5"]})
              "claude"))
      (is false "expected policy rejection")
      (catch :default err
        (is (= 403 (aget err "statusCode")))
        (is (= "model_not_allowed" (aget err "code")))
        (is (re-find #"glm-5" (.-message err)))))))

(deftest ^:async enforce-chat-policy-applies-rate-limit
  (testing "rate limit increments per membership and rejects after maxRequests"
    (let [counts* (atom {})]
      (try
        (mongo-rate-limits/set-increment-fn!
         (fn [key _window-seconds]
           (let [n (inc (get @counts* key 0))]
             (swap! counts* assoc key n)
             n)))
        (let [ctx (chat-context {:maxRequests 2 :windowSeconds 60})]
          (is (nil? (await (policy/enforce-chat-policy! ctx "glm-5"))))
          (is (nil? (await (policy/enforce-chat-policy! ctx "glm-5"))))
          (try
            (await (policy/enforce-chat-policy! ctx "glm-5"))
            (is false "expected rate-limit rejection")
            (catch :default err
              (is (= 429 (aget err "statusCode")))
              (is (= "chat_rate_limited" (aget err "code"))))))
        (finally
          (mongo-rate-limits/set-increment-fn! nil))))))

(deftest ^:async enforce-chat-policy-fails-open-when-mongo-errors
  (testing "mongo operational failures should not block chat"
    (try
      (mongo-rate-limits/set-increment-fn!
       (fn [_ _] (throw (js/Error. "mongo down"))))
      (let [result (await (policy/enforce-chat-policy!
                           (chat-context {:maxRequests 1 :windowSeconds 60})
                           "glm-5"))]
        (is (nil? result)))
      (finally
        (mongo-rate-limits/set-increment-fn! nil)))))
