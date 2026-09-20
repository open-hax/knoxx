(ns knoxx.backend.extern-fetch-test
  (:require ["node:http" :as http]
            [cljs.test :refer [deftest is testing]]
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

(defn- ^:async listen! [server]
  (await (js/Promise. (fn [resolve reject]
                        (.once server "error" reject)
                        (.listen server 0 "127.0.0.1" resolve))))
  (str "http://127.0.0.1:" (.-port (.address server))))

(defn- ^:async close! [server]
  (await (js/Promise. (fn [resolve] (.close server resolve)))))

(deftest ^:async credential-redirect-policy-reaches-native-fetch
  (let [received (atom 0)
        destination (http/createServer (fn [_ response]
                                         (swap! received inc)
                                         (.end response "{}")))
        destination-url (await (listen! destination))
        status (atom 307)
        origin (http/createServer (fn [_ response]
                                    (.writeHead response @status #js {"Location" destination-url})
                                    (.end response)))
        origin-url (await (listen! origin))]
    (try
      (doseq [code [302 303 307 308] nested? [false true]]
        (reset! status code)
        (let [request {:method "POST" :json {:password "test-only-password"} :redirect "error"}
              request (if nested? {:opts request} request)]
          (is (true? (try (await (xfetch/json! xfetch/default-client (assoc request :url origin-url)))
                          false (catch :default _ true))))))
      (is (zero? @received) "No redirected request or password reaches the second server")
      (finally (await (close! origin)) (await (close! destination))))))
