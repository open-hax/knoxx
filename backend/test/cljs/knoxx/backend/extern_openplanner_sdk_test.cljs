(ns knoxx.backend.extern-openplanner-sdk-test
  "Conversion regression tests for the @open-hax/openplanner-sdk extern
   boundary (stubbed module — see test/js/openplanner_sdk_test_stub.cjs) and
   the mode-selecting OpenPlanner client factory."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.clients.openplanner-mongo :as openplanner-mongo]))

(deftest ^:async events-conversion
  (testing "ingest result arrives as keywordized CLJS without transport-only keys"
    (let [result (await (xsdk/events! [{:schema "openplanner.event.v1"
                                        :id "ev-1"
                                        :ts "2026-01-01T00:00:00Z"
                                        :source "test"
                                        :kind "knoxx.message"
                                        :text "hello"}]))]
      (is (map? result))
      (is (true? (:ok result)))
      (is (= 1 (:count result)))
      (is (= ["ev-1"] (:ids result)))
      (is (not (contains? result :acceptedEvents)))
      (is (not (contains? result :backgroundIndexing))))))

(deftest ^:async vector-search-conversion
  (testing "vector search result matches the /v1/search/vector body shape"
    (let [result (await (xsdk/vector-search! {:q "hello" :k 5}))]
      (is (true? (:ok result)))
      (is (= [["stub-1"]] (get-in result [:result :ids])))
      (is (= "mongodb" (:storageBackend result))))))

(deftest ^:async sessions-conversion
  (testing "session list rows keywordize like the REST body"
    (let [result (await (xsdk/sessions {:project "p" :limit 10 :offset 0}))]
      (is (true? (:ok result)))
      (is (= "s" (-> result :rows first :session))))))

(deftest ^:async health-shape
  (testing "health resolves the REST client's {:ok :status :body} shape"
    (let [result (await (xsdk/health))]
      (is (true? (:ok result)))
      (is (= 200 (:status result)))
      (is (map? (:body result))))))

(deftest client-factory-mode-selection
  (testing "mongo mode (default) returns the direct-mongo record"
    (let [built (openplanner-client/client {:openplanner-base-url "http://x"
                                            :openplanner-api-key "k"})]
      (is (instance? openplanner-mongo/MongoOpenPlannerClient built))))
  (testing "rest mode returns the fetch client"
    (let [built (openplanner-client/client {:openplanner-base-url "http://x"
                                            :openplanner-api-key "k"
                                            :openplanner-client-mode "rest"})]
      (is (instance? openplanner-client/FetchOpenPlannerClient built))))
  (testing "explicit opts mode overrides config"
    (let [built (openplanner-client/client {:openplanner-client-mode "mongo"}
                                           {:mode "rest"})]
      (is (instance? openplanner-client/FetchOpenPlannerClient built)))))

(deftest ^:async mongo-client-delegates-data-plane
  (testing "the direct-mongo record serves data-plane calls via the SDK"
    (let [rest-client (openplanner-client/client {:openplanner-base-url "http://x"
                                                  :openplanner-api-key "k"
                                                  :openplanner-client-mode "rest"})
          built (openplanner-mongo/client {} rest-client)
          result (await (openplanner-client/vector-search! built {:q "hello" :k 3}))]
      (is (true? (:ok result)))
      (is (= [["stub-1"]] (get-in result [:result :ids]))))))
