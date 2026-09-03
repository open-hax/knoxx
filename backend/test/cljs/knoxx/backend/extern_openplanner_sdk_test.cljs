(ns knoxx.backend.extern-openplanner-sdk-test
  "Conversion regression tests for the @open-hax/openplanner-sdk extern
   boundary (stubbed module — see test/js/openplanner_sdk_test_stub.mjs) and
   the mode-selecting OpenPlanner client factory."
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
            [cljs.test :refer [deftest is testing]]
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

(deftest ^:async events-can-await-background-indexing
  (testing "read-your-writes callers use the optional arity without leaking the promise"
    (let [result (await (xsdk/events! [{:schema "openplanner.event.v1"
                                        :id "ev-await-index"
                                        :ts "2026-01-01T00:00:00Z"
                                        :source "test"
                                        :kind "docs"
                                        :text "await me"}]
                                      {:await-index? true}))]
      (is (true? (:ok result)))
      (is (= ["ev-await-index"] (:ids result)))
      (is (not (contains? result :backgroundIndexing)))
      (is (= {:ok true
              :event-ids ["ev-await-index"]
              :event-count 1
              :vector-count 1
              :embedding-dimensions 3}
             (await (xsdk/verify-event-vectors! ["ev-await-index"])))))))

(deftest ^:async event-extra-is-the-durable-arbitrary-metadata-shape
  (testing "SDK persistence drops arbitrary meta but retains queryable extra"
    (let [canonical {:source_lang "en"
                     :target_lang "fr"
                     :source_text "hello"
                     :mt_model "gemma4:e2b"
                     :status "in_review"}
          id "ev-durable-translation-extra"]
      (await (xsdk/events! [{:schema "openplanner.event.v1"
                            :id id
                            :ts "2026-01-01T00:00:00Z"
                            :source "mt"
                            :kind "translation.segment"
                            :text "bonjour"
                            :meta canonical
                            :extra canonical}]))
      (let [row (-> (await (xsdk/mongo-query
                            {:collection "events"
                             :filter {:id id}
                             :limit 1}))
                    :rows first)]
        (is (nil? (:meta row)))
        (is (= canonical (select-keys (:extra row) (keys canonical))))))))

(deftest ^:async stable-existing-events-backfill-only-missing-extra-fields
  (let [id "ev-backfill-translation-extra"
        canonical {:source_lang "en"
                   :target_lang "es"
                   :source_text "hello"
                   :mt_model "gemma4:e2b"
                   :status "in_review"}]
    (await (xsdk/events! [{:schema "openplanner.event.v1"
                          :id id
                          :ts "2026-01-01T00:00:00Z"
                          :source "mt"
                          :kind "translation.segment"
                          :text "hola"
                          :meta canonical
                          :extra {:candidate_set_id "candidate-set-1"}}]))
    (let [result (await (xsdk/ensure-event-extra-fields! id canonical))
          row (-> (await (xsdk/mongo-query
                          {:collection "events" :filter {:id id} :limit 1}))
                  :rows first)]
      (is (= (set (keys canonical)) (set (:updated-fields result))))
      (is (= "candidate-set-1" (get-in row [:extra :candidate_set_id])))
      (is (= canonical (select-keys (:extra row) (keys canonical)))))))

(deftest ^:async stable-existing-events-refuse-extra-field-conflicts
  (let [id "ev-conflicting-translation-extra"
        required {:source_lang "en" :target_lang "fr"}]
    (await (xsdk/events! [{:schema "openplanner.event.v1"
                          :id id
                          :ts "2026-01-01T00:00:00Z"
                          :source "mt"
                          :kind "translation.segment"
                          :text "bonjour"
                          :extra {:source_lang "de"}}]))
    (try
      (await (xsdk/ensure-event-extra-fields! id required))
      (is false "immutable existing metadata must not be overwritten")
      (catch :default err
        (is (= "openplanner_event_extra_conflict" (:code (ex-data err))))
        (is (= {:expected "en" :actual "de"}
               (get-in (ex-data err) [:conflicts :source_lang])))))))

(deftest ^:async awaited-event-ingest-repairs-a-missing-vector
  (sdk-mod/__setEventVectorMode "missing")
  (try
    (let [id "ev-missing-vector"
          result (await (xsdk/events! [{:schema "openplanner.event.v1"
                                       :id id
                                       :ts "2026-01-01T00:00:00Z"
                                       :source "test"
                                       :kind "docs"
                                       :text "must be indexed"}]
                                      {:await-index? true}))
          verified (await (xsdk/verify-event-vectors! [id]))
          stored (await (xsdk/mongo-query
                         {:collection "events" :filter {:id id}}))]
      (is (true? (:ok result)))
      (is (= [id] (:event-ids verified)))
      (is (= 1 (:vector-count verified)))
      (is (= 1 (:total stored))
          "vector repair must not reinsert the immutable base event"))
    (finally
      (sdk-mod/__setEventVectorMode "valid"))))

(deftest ^:async existing-event-ensure-replaces-the-wrong-vector-shape
  (sdk-mod/__setEventVectorMode "wrong-dimensions")
  (try
    (let [id "ev-wrong-vector-shape"]
      (await (xsdk/events! [{:schema "openplanner.event.v1"
                            :id id
                            :ts "2026-01-01T00:00:00Z"
                            :source "test"
                            :kind "docs"
                            :text "materialize the wrong shape"}]))
      ;; Await the stub's detached projection through a second ingest-free turn.
      (await (js/Promise.resolve))
      (try
        (await (xsdk/verify-event-vectors! [id]))
        (is false "strict verification must identify the wrong vector shape")
        (catch :default err
          (is (= 3 (:expected-dimensions (ex-data err))))
          (is (= [id] (:invalid-event-ids (ex-data err))))))
      (let [ensured (await (xsdk/ensure-event-vectors! [id]))
            stored (await (xsdk/mongo-query
                           {:collection "events" :filter {:id id}}))]
        (is (= [id] (:repaired-event-ids ensured)))
        (is (= 3 (:embedding-dimensions ensured)))
        (is (= 1 (:vector-count ensured)))
        (is (= 1 (:total stored))
            "shape repair must replace vectors without reinserting the event")))
    (finally
      (sdk-mod/__setEventVectorMode "valid"))))

(deftest ^:async blank-existing-events-fail-vector-repair-clearly
  (let [id "ev-blank-vector-source"]
    (await (xsdk/events! [{:schema "openplanner.event.v1"
                          :id id
                          :ts "2026-01-01T00:00:00Z"
                          :source "test"
                          :kind "docs"
                          :text "  \n  "}]))
    (await (js/Promise.resolve))
    (try
      (await (xsdk/ensure-event-vectors! [id]))
      (is false "a blank immutable event cannot be silently marked indexed")
      (catch :default err
        (is (= "openplanner_event_not_vector_indexable"
               (:code (ex-data err))))
        (is (= id (:event-id (ex-data err))))
        (is (= :blank-text (:reason (ex-data err))))))))

(deftest ^:async sdk-excluded-existing-events-fail-vector-repair-clearly
  (let [id "ev-excluded-vector-source"]
    (await (xsdk/events! [{:schema "openplanner.event.v1"
                          :id id
                          :ts "2026-01-01T00:00:00Z"
                          :source "test"
                          :kind "graph.node"
                          :text "graph projection"}]))
    (await (js/Promise.resolve))
    (try
      (await (xsdk/ensure-event-vectors! [id]))
      (is false "SDK-excluded event kinds cannot be repaired as hot vectors")
      (catch :default err
        (is (= "openplanner_event_not_vector_indexable"
               (:code (ex-data err))))
        (is (= id (:event-id (ex-data err))))
        (is (= :excluded-kind (:reason (ex-data err))))))))

(deftest ^:async event-vector-verification-requires-configured-dimensions
  (let [previous (aget js/process.env "EMBED_PROVIDER_DIMENSIONS")]
    (try
      (aset js/process.env "EMBED_PROVIDER_DIMENSIONS" "not-an-integer")
      (try
        (await (xsdk/verify-event-vectors! ["ev-any-existing-row"]))
        (is false "invalid configured dimensions must fail before querying")
        (catch :default err
          (is (= "openplanner_embedding_dimensions_invalid"
                 (:code (ex-data err))))))
      (finally
        (if (nil? previous)
          (js-delete js/process.env "EMBED_PROVIDER_DIMENSIONS")
          (aset js/process.env "EMBED_PROVIDER_DIMENSIONS" previous))))))

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
