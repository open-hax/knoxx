(ns knoxx.backend.extern.wiki-error-response-test
  "Wiki failure responses complete through real Fastify with bounded status codes."
  (:require ["fastify" :default Fastify]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.wiki :as wiki]))

(defn- ^:async response! [error]
  (let [app (Fastify #js {:logger false})]
    (try
      (with-redefs [fastify/log-unclassified-failure! (fn [_surface _error] nil)]
        (.get app "/failure"
              (fn [_request reply]
                (#'wiki/respond! reply #(throw error))))
        (let [response (await (.inject app #js {:method "GET" :url "/failure"}))]
          {:status (.-statusCode response)
           :body (js->clj (.json response) :keywordize-keys true)}))
      (finally (await (.close app))))))

(deftest ^:async malformed-or-non-error-statuses-send-an-opaque-500
  (doseq [status [nil false true "401" "invalid" {} [] 200 399 600 401.5 js/NaN js/Infinity]]
    (is (= {:status 500 :body {:detail "internal error"}}
           (await (response! (ex-info "Private failure detail" {:status status :private "hidden"})))))))

(deftest ^:async valid-statuses-and-ex-data-precedence-are-preserved
  (doseq [status [400 403 429 500 599]]
    (is (= status (:status (await (response! (ex-info "Refused" {:status status})))))))
  (is (= 429 (:status (await (response! (js-obj "statusCode" 429 "message" "Rate limited"))))))
  (let [error (ex-info "Scoped refusal" {:status 403})]
    (aset error "statusCode" 429)
    (is (= 403 (:status (await (response! error))))))
  (let [error (ex-info "Malformed refusal" {:status "bad"})]
    (aset error "statusCode" 429)
    (is (= 500 (:status (await (response! error)))))))
