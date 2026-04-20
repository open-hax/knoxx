(ns kms-ingestion.translation.pipeline-test
  "Integration tests for the CMS -> publish -> translate pipeline.
   Requires a live OpenPlanner server.
   Run with: clojure -M:integration"
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [cheshire.core :as json])
  (:import
   [java.net URL]
   [java.io OutputStreamWriter]))

(def ^:private test-base-url
  (or (System/getenv "TEST_OPENPLANNER_URL") "http://localhost:7777"))

(def ^:private test-api-key
  (or (System/getenv "TEST_OPENPLANNER_API_KEY") "change-me"))

(defn- api-request [method path & [body]]
  (let [conn (.openConnection (URL. (str test-base-url "/v1" path)))]
    (.setRequestMethod conn method)
    (.setRequestProperty conn "Content-Type" "application/json")
    (.setRequestProperty conn "Authorization" (str "Bearer " test-api-key))
    (when body
      (.setDoOutput conn true)
      (.write (OutputStreamWriter. (.getOutputStream conn))
              (json/generate-string body)))
    (let [code        (.getResponseCode conn)
          body-stream (if (>= code 400) (.getErrorStream conn) (.getInputStream conn))
          body-str    (when body-stream (slurp body-stream))]
      {:status code
       :body   (when body-str (json/parse-string body-str keyword))})))

(defn- cleanup-test-data [f] (f))

(use-fixtures :each cleanup-test-data)

(deftest cms-publish-queues-translation-jobs-test
  (testing "Publishing a document to a garden with target_languages queues translation jobs"
    (let [garden-result (api-request "POST" "/gardens"
                                     {:garden_id        "test-translation-garden"
                                      :title            "Test Translation Garden"
                                      :description      "Garden for translation pipeline tests"
                                      :target_languages ["es" "fr"]
                                      :status           "active"})]
      (is (= 201 (:status garden-result)))
      (let [doc-result (api-request "POST" "/documents"
                                    {:kind       "docs"
                                     :project    "test-translation"
                                     :visibility "draft"
                                     :extra      {:title   "Test Document for Translation"
                                                  :content "This is a test document."}})]
        (is (= 201 (:status doc-result)))
        (let [doc-id         (get-in doc-result [:body :doc_id])
              publish-result (api-request "POST" (str "/cms/publish/" doc-id "/test-translation-garden"))]
          (is (= 200 (:status publish-result)))
          (let [body (:body publish-result)
                jobs (:translation_jobs body)]
            (is (= "published" (:status body)))
            (is (pos? (count jobs)))
            (is (some #(= "es" (:target_lang %)) jobs))
            (is (some #(= "fr" (:target_lang %)) jobs))
            (doseq [job jobs]
              (is (= "queued" (:status job))))))))))

(deftest translation-job-status-endpoints-test
  (testing "Translation job status endpoints work correctly"
    (let [job-result (api-request "POST" "/translations/jobs"
                                  {:document_id     "test-doc-123"
                                   :garden_id       "test-garden"
                                   :source_lang     "en"
                                   :target_language "es"
                                   :status          "queued"})]
      (is (= 201 (:status job-result)))
      (let [job-id      (get-in job-result [:body :job_id])
            next-result (api-request "GET" "/translations/jobs/next")]
        (is (= 200 (:status next-result)))
        (when-let [job (get-in next-result [:body :job])]
          (is (= "queued" (:status job)))
          (let [status-result (api-request "POST"
                                           (str "/translations/jobs/" job-id "/status")
                                           {:status "processing"})]
            (is (= 200 (:status status-result)))))))))

(deftest translation-segment-crud-test
  (testing "Translation segments can be created and retrieved"
    (let [segment-result (api-request "POST" "/translations/segments"
                                      {:source_text     "Hello world"
                                       :translated_text "Hola mundo"
                                       :source_lang     "en"
                                       :target_lang     "es"
                                       :document_id     "test-doc-456"
                                       :garden_id       "test-garden"
                                       :segment_index   0
                                       :status          "pending"})]
      (is (= 201 (:status segment-result)))
      (let [list-result (api-request "GET" "/translations/segments?target_lang=es")]
        (is (= 200 (:status list-result)))))))
