(ns knoxx.backend.extern-fetch-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fetch :as xfetch]))

(defn- response
  ([body] (response body 200))
  ([body status]
   (js/Response. body #js {:status status
                           :headers #js {"Content-Type" "application/json"}})))

(deftest ^:async json-request-encodes-cljs-body-at-fetch-boundary
  (testing "CLJS JSON payload is stringified once and content-type is defaulted"
    (let [seen* (atom nil)
          client (xfetch/native-client
                  {:fetch-fn (fn [_url opts]
                               (reset! seen* opts)
                               (js/Promise.resolve (response "{\"ok\":true}")))})
          result (await (xfetch/json! client {:url "http://example.test/json"
                                              :method "POST"
                                              :headers {"Accept" "application/json"}
                                              :json {:hello "world"}}))
          opts @seen*]
      (is (= {:ok true} (:body result)))
      (is (= "POST" (aget opts "method")))
      (is (= "application/json" (aget opts "headers" "Content-Type")))
      (is (= "application/json" (aget opts "headers" "Accept")))
      (is (= "{\"hello\":\"world\"}" (aget opts "body"))))))

(deftest ^:async nil-json-request-omits-body
  (testing "nil JSON request body does not create a RequestInit body"
    (let [seen* (atom nil)
          client (xfetch/native-client
                  {:fetch-fn (fn [_url opts]
                               (reset! seen* opts)
                               (js/Promise.resolve (response "{}")))})]
      (await (xfetch/json! client {:url "http://example.test/nil"
                                   :method "POST"
                                   :json nil}))
      (is (nil? (aget @seen* "body"))))))

(deftest ^:async opaque-body-is-not-json-stringified
  (testing "binary/native bodies are carried to fetch without JSON encoding"
    (let [buffer (.from js/Buffer "abc")
          seen* (atom nil)
          client (xfetch/native-client
                  {:fetch-fn (fn [_url opts]
                               (reset! seen* opts)
                               (js/Promise.resolve (response "ok")))})]
      (await (xfetch/response! client {:url "http://example.test/binary"
                                       :method "POST"
                                       :headers {"Content-Type" "application/octet-stream"}
                                       :body buffer}))
      (is (identical? buffer (aget @seen* "body")))
      (is (= "application/octet-stream" (aget @seen* "headers" "Content-Type"))))))

(deftest ^:async non-2xx-json-response-still-decodes-body
  (testing "transport returns status and decoded JSON body for failed upstream responses"
    (let [client (xfetch/native-client
                  {:fetch-fn (fn [_url _opts]
                               (js/Promise.resolve (response "{\"error\":\"denied\"}" 403)))})
          result (await (xfetch/json! client {:url "http://example.test/denied"
                                              :method "GET"}))]
      (is (false? (:ok result)))
      (is (= 403 (:status result)))
      (is (= {:error "denied"} (:body result))))))
