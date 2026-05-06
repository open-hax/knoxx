(ns knoxx.backend.model-routes-test
  (:require [cljs.test :refer [async deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.routes.models :as model-routes]))

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
  #js {:ok ok
       :body (or body #js {:data models})})

(def base-config
  {:proxx-base-url "https://proxx.example"
   :proxx-auth-token "test-token"
   :model-prefix-allowlist ["good" "also"]})

(deftest send-proxx-models-filters-by-allowlist-and-policy
  (async done
    (testing "successful Proxx model responses are filtered before sending"
      (let [calls* (atom [])
            reply (test-reply)
            auth-ctx {:toolPolicies [{:toolId "agent.chat"
                                      :constraints {:allowedModels ["good-alpha" "missing-model"]}}]}
            ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply auth-ctx)
                       :fetch-json! (fn [url opts]
                                      (swap! calls* conj {:url url
                                                          :authorization (aget opts "headers" "Authorization")})
                                      (js/Promise.resolve
                                       (model-response true
                                                       #js [#js {:id "good-alpha"}
                                                            #js {:id "good-beta"}
                                                            #js {:id "blocked-alpha"}
                                                            #js {:id "also-visible"}]
                                                       nil))))]
        (-> (model-routes/send-proxx-models! ctx)
            (.then (fn [_]
                     (let [state (reply-state reply)]
                       (is (= [{:url "https://proxx.example/v1/models"
                                :authorization "Bearer test-token"}]
                              @calls*))
                       (is (= 200 (:status state)))
                       (is (= "application/json" (:type state)))
                       (is (= ["good-alpha"]
                              (mapv :id (:models (:body state))))))))
            (.catch (fn [err]
                      (is nil (str "unexpected rejection: " err))))
            (.finally done))))))

(deftest send-proxx-models-renders-upstream-error
  (async done
    (testing "non-ok Proxx responses become 502 JSON responses"
      (let [reply (test-reply)
            ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply nil)
                       :fetch-json! (fn [_url _opts]
                                      (js/Promise.resolve
                                       (model-response false nil #js {:message "upstream nope"}))))]
        (-> (model-routes/send-proxx-models! ctx)
            (.then (fn [_]
                     (let [state (reply-state reply)]
                       (is (= 502 (:status state)))
                       (is (= {:error "Proxx model list failed"
                               :details {:message "upstream nope"}}
                              (:body state))))))
            (.catch (fn [err]
                      (is nil (str "unexpected rejection: " err))))
            (.finally done))))))

(deftest send-proxx-models-renders-rejection
  (async done
    (testing "fetch rejections become 502 JSON responses"
      (let [reply (test-reply)
            ctx (assoc (model-routes/proxx-models-ctx base-config #js {} reply nil)
                       :fetch-json! (fn [_url _opts]
                                      (js/Promise.reject (js/Error. "network down"))))]
        (-> (model-routes/send-proxx-models! ctx)
            (.then (fn [_]
                     (let [state (reply-state reply)]
                       (is (= 502 (:status state)))
                       (is (str/includes? (:error (:body state)) "network down")))))
            (.catch (fn [err]
                      (is nil (str "unexpected rejection: " err))))
            (.finally done))))))
