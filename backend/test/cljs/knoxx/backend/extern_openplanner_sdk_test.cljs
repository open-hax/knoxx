(ns knoxx.backend.extern-openplanner-sdk-test
  "Conversion regression tests for the @open-hax/openplanner-sdk extern
   boundary (stubbed module — see test/js/openplanner_sdk_test_stub.mjs) and
   the mode-selecting OpenPlanner client factory."
  (:require ["@open-hax/openplanner-sdk" :as sdk-stub]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.clients.openplanner-mongo :as openplanner-mongo]))

(defn- reset-sdk-calls!
  []
  (set! (.-length (.-__calls sdk-stub)) 0))

(defn- sdk-call-names
  []
  (mapv #(aget % "name") (array-seq (.-__calls sdk-stub))))

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

(deftest ^:async mongo-client-keeps-translations-in-process
  (testing "translation calls use the SDK projection, never the wrapped REST client"
    (let [rest-client (openplanner-client/client {:openplanner-base-url "http://intentionally-unused"
                                                  :openplanner-api-key "unused"
                                                  :openplanner-client-mode "rest"})
          built (openplanner-mongo/client {} rest-client)
          result (await (openplanner-client/translation-segments! built {:project "knoxx" :limit 1}))]
      (is (= [] (:segments result)))
      (is (= 0 (:total result))))))

(deftest ^:async translation-sdk-boundary-contracts
  (testing "every translation operation reaches the direct SDK and preserves established response shapes"
    (reset-sdk-calls!)
    (let [segments (await (xsdk/translation-segments! {:project "knoxx" :limit 1}))
          segment (await (xsdk/translation-segment! "segment-1"))
          created (await (xsdk/create-translation-segment! {:source_text "Hello"
                                                            :translated_text "Hola"
                                                            :target_lang "es"
                                                            :document_id "doc-1"
                                                            :segment_index 0}))
          labeled (await (xsdk/label-translation-segment! "segment-1" {:adequacy "good"
                                                                       :fluency "good"
                                                                       :terminology "correct"
                                                                       :risk "safe"
                                                                       :overall "approve"}))
          manifest (await (xsdk/translation-export-manifest! {:project "knoxx" :org_id "org-1"}))
          sft (await (xsdk/translation-export-sft! {:project "knoxx" :org_id "org-1"}))
          ;; Batch imports intentionally permit omitted segment_index; the SDK
          ;; assigns the row position while single-segment creates require it.
          imported (await (xsdk/create-translation-segments-batch! {:segments [{:source_text "Hello"
                                                                                :translated_text "Hola"
                                                                                :target_lang "es"
                                                                                :document_id "doc-1"}]}))
          documents (await (xsdk/translation-documents! {:project "knoxx" :org_id "org-1"}))
          document (await (xsdk/translation-document! "doc-1" "es"))
          reviewed (await (xsdk/review-translation-document! "doc-1" "es" {:overall "approve"}))
          created-batch (await (xsdk/create-translation-batch! {:garden_id "garden-1"
                                                               :target_lang "es"
                                                               :document_ids ["doc-1"]}))
          batches (await (xsdk/translation-batches! {:status "queued"}))
          next-batch (await (xsdk/next-translation-batch!))
          batch (await (xsdk/translation-batch! "mongo-batch-1"))
          updated-batch (await (xsdk/update-translation-batch-status! "mongo-batch-1" {:status "complete"}))]
      (is (= 0 (:total segments)))
      (is (= "segment-1" (:id segment)))
      (is (= "segment-1" (:id created)))
      (is (= "approved" (:new_status labeled)))
      (is (= 1 (get-in manifest [:languages :es :approved])))
      (is (= 1 (get-in manifest [:export_sizes :sft_es :rows])))
      (is (string? sft))
      (is (= 1 (:imported imported)))
      (is (= "fully_approved" (-> documents :documents first :overall_status)))
      (is (= 1 (get-in document [:summary :total_segments])))
      (is (not (contains? document :labels)))
      (is (= 1 (:segments_reviewed reviewed)))
      (is (= "batch-1" (:batch_id created-batch)))
      (is (= "batch-1" (-> batches :batches first :batch_id)))
      (is (= "processing" (get-in next-batch [:batch :status])))
      (is (= "mongo-batch-1" (:id batch)))
      (is (= "complete" (:status updated-batch)))
      (is (= ["translation.listSegments"
              "translation.segment"
              "translation.createSegment"
              "translation.labelSegment"
              "translation.manifest"
              "translation.exportSft"
              "translation.createSegmentsBatch"
              "translation.documents"
              "translation.document"
              "translation.reviewDocument"
              "translation.createBatch"
              "translation.listBatches"
              "translation.nextBatch"
              "translation.batch"
              "translation.updateBatch"]
             (sdk-call-names))))))
