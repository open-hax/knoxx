(ns knoxx.backend.defroute-async-test
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [cljs.test :refer [deftest is]]))

(defroute promise-chain-route!
  []
  "GET" "/test/promise-chain"
  (let [target-url "/upstream"]
    (-> (fetch-json target-url {:method "GET"})
        (.then (fn [resp]
                 (json-response! reply 200 (:body resp))))
        (.catch (fn [err]
                  (json-response! reply 502 {:detail (.-message err)}))))))

(defroute await-route!
  []
  "GET" "/test/await"
  (let [value (await (js/Promise.resolve 41))]
    (json-response! reply 200 {:value (inc value)})))

(defn- capture-route-deps
  [captured-handler sent-response]
  {:route! (fn [_app _method _path handler]
             (reset! captured-handler handler)
             nil)
   :with-request-context! (fn [_runtime request reply f]
                            (f (aget request "ctx")))
   :fetch-json (fn [_url _opts]
                 (js/Promise.resolve {:body {:ok true}}))
   :json-response! (fn [_reply status body]
                     (reset! sent-response {:status status :body body})
                     nil)})

(deftest ^:async defroute-preserves-classic-promise-chains
  (let [captured-handler (atom nil)
        sent-response (atom nil)
        deps (capture-route-deps captured-handler sent-response)]
    (promise-chain-route! nil #js {} {} deps)
    (await (@captured-handler #js {} #js {}))
    (is (= {:status 200 :body {:ok true}} @sent-response))))

(deftest ^:async defroute-keeps-raw-await-handlers-async
  (let [captured-handler (atom nil)
        sent-response (atom nil)
        deps (capture-route-deps captured-handler sent-response)]
    (await-route! nil #js {} {} deps)
    (await (@captured-handler #js {} #js {}))
    (is (= {:status 200 :body {:value 42}} @sent-response))))
