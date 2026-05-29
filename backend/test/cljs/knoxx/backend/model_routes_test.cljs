(ns knoxx.backend.model-routes-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.routes.models :as model-routes]))

(defn- test-reply
  []
  (let [state (atom {})
        reply #js {}]
    (aset reply "state" state)
    (aset reply "raw" #js {})
    (aset reply "code" (fn [status]
                          (swap! state assoc :status status)
                          reply))
    (aset reply "type" (fn [content-type]
                          (swap! state assoc :type content-type)
                          reply))
    (aset reply "send" (fn [body]
                          (swap! state assoc :body (js->clj body :keywordize-keys true))
                          reply))
    reply))

(defn- reply-state
  [reply]
  @(aget reply "state"))

(defn- model-response
  [ok models body]
  {:ok ok
   :body (or body {:data models})})

(defn- stub-proxx-client
  [models-fn]
  (reify proxx-client/IProxxClient
    (health! [_]
      (js/Promise.reject (js/Error. "unused")))
    (models! [_]
      (models-fn))
    (request-logs! [_ _query-string]
      (js/Promise.reject (js/Error. "unused")))
    (dashboard-overview! [_ _query-string]
      (js/Promise.reject (js/Error. "unused")))
    (provider-model-analytics! [_ _query-string]
      (js/Promise.reject (js/Error. "unused")))
    (websearch! [_ _payload]
      (js/Promise.reject (js/Error. "unused")))
    (chat-completions! [_ _payload]
      (js/Promise.reject (js/Error. "unused")))
    (chat-completions-response! [_ _payload]
      (js/Promise.reject (js/Error. "unused")))
    (embeddings! [_ _payload]
      (js/Promise.reject (js/Error. "unused")))
    (embeddings-response! [_ _payload]
      (js/Promise.reject (js/Error. "unused")))))

(def base-config
  {:proxx-base-url "https://proxx.example"
   :proxx-auth-token "test-token"
   :model-prefix-allowlist ["good" "also"]})

(deftest ^:async send-proxx-models-filters-by-allowlist-and-policy
  (testing "successful Proxx model responses are filtered before sending"
    (let [calls* (atom [])
          reply (test-reply)
          auth-ctx {:toolPolicies [{:toolId "agent.chat"
                                    :constraints {:allowedModels ["good-alpha" "missing-model"]}}]}
          ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply auth-ctx)
                     :proxx-client (stub-proxx-client
                                    (fn []
                                      (swap! calls* conj :models)
                                      (js/Promise.resolve
                                       (model-response true
                                                       [{:id "good-alpha"}
                                                        {:id "good-beta"}
                                                        {:id "blocked-alpha"}
                                                        {:id "also-visible"}]
                                                       nil)))))]
      (await (model-routes/send-proxx-models! ctx))
      (let [state (reply-state reply)]
        (is (= [:models] @calls*))
        (is (= 200 (:status state)))
        (is (= "application/json" (:type state)))
        (is (= ["good-alpha"]
               (mapv :id (:models (:body state)))))))))

(deftest ^:async send-proxx-models-renders-upstream-error
  (testing "non-ok Proxx responses become 502 JSON responses"
    (let [reply (test-reply)
          ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply nil)
                     :proxx-client (stub-proxx-client
                                    (fn []
                                      (js/Promise.resolve
                                       (model-response false nil {:message "upstream nope"})))))]
      (await (model-routes/send-proxx-models! ctx))
      (let [state (reply-state reply)]
        (is (= 502 (:status state)))
        (is (= {:error "Proxx model list failed"
                :details {:message "upstream nope"}}
               (:body state)))))))

(deftest ^:async send-proxx-models-renders-rejection
  (testing "fetch rejections become 502 JSON responses"
    (let [reply (test-reply)
          ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply nil)
                     :proxx-client (stub-proxx-client
                                    (fn []
                                      (js/Promise.reject (js/Error. "network down")))))]
      (await (model-routes/send-proxx-models! ctx))
      (let [state (reply-state reply)]
        (is (= 502 (:status state)))
        (is (str/includes? (:error (:body state)) "network down"))))))
