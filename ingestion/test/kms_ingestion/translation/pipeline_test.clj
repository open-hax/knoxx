(ns kms-ingestion.translation.pipeline-test
  "End-to-end tests for the CMS -> publish -> translate pipeline."
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [cheshire.core :as json])
  (:import
   [java.net URL HttpURLConnection]
   [java.io OutputStreamWriter]))

;; Test configuration
(def test-base-url
  (or (System/getenv "TEST_OPENPLANNER_URL")
      "http://localhost:7777"))

(def test-api-key
  (or (System/getenv "TEST_OPENPLANNER_API_KEY")
      "change-me"))

(defn- api-request
  "Make an API request to OpenPlanner."
  [method path & [body]]
  (let [url (str test-base-url "/v1" path)
        conn (HttpURLConnection/openConnection (URL. url))]
    (.setRequestMethod conn method)
    (.setRequestProperty conn "Content-Type" "application/json")
    (.setRequestProperty conn "Authorization" (str "Bearer " test-api-key))
    (when body
      (.setDoOutput conn true)
      (.write (OutputStreamWriter. (.getOutputStream conn))
              (json/generate-string body)))
    (let [code (.getResponseCode conn)
          body-stream (if (>= code 400) (.getErrorStream conn) (.getInputStream conn))
          body-str (when body-stream (slurp body-stream))]
      {:status code
       :body (when body-str (json/parse-string body-str keyword))})))

(defn- cleanup-test-data
  "Cleanup test gardens and documents after each test."
  [f]
  (f))

(use-fixtures :each cleanup-test-data)

(deftest cms-publish-queues-translation-jobs-test
  (testing "Publishing a document to a garden with target_languages queues translation jobs"
    ;; Create a test garden with target languages
    (let [garden-result (api-request :post "/gardens"
                         {:garden_id "test-translation-garden"
                          :title "Test Translation Garden"
                          :description "Garden for translation pipeline tests"
                          :target_languages ["es" "fr"]
                          :status "active"})]
      (is (= 201 (:status garden-result)))
      
      ;; Create a test document
      (let [doc-result (api-request :post "/documents"
                        {:kind "docs"
                         :project "test-translation"
                         :visibility "draft"
                         :extra {:title "Test Document for Translation"
                                 :content "This is a test document. It has multiple sentences. We want to see if translation works correctly."}})]
        (is (= 201 (:status doc-result)))
        (let [doc-id (get-in doc-result [:body :doc_id])]
          
          ;; Publish the document to the garden
          (let [publish-result (api-request :post 
                                (str "/cms/publish/" doc-id "/test-translation-garden"))]
            (is (= 200 (:status publish-result)))
            (let [body (:body publish-result)]
              ;; Should have queued translation jobs
              (is (= "published" (:status body)))
              (is (pos? (count (:translation_jobs body))))
              
              ;; Verify jobs were created for both target languages
              (let [jobs (:translation_jobs body)]
                (is (some #(= "es" (:target_lang %)) jobs))
                (is (some #(= "fr" (:target_lang %)) jobs))
                
                ;; Verify jobs are in queued status
                (doseq [job jobs]
                  (is (= "queued" (:status job))))))))))))

(deftest translation-job-status-endpoints-test
  (testing "Translation job status endpoints work correctly"
    ;; Create a test job directly
    (let [job-result (api-request :post "/translations/jobs"
                      {:document_id "test-doc-123"
                       :garden_id "test-garden"
                       :source_lang "en"
                       :target_language "es"
                       :status "queued"})]
      (is (= 201 (:status job-result)))
      (let [job-id (get-in job-result [:body :job_id])]
        
        ;; Fetch next job
        (let [next-result (api-request :get "/translations/jobs/next")]
          (is (= 200 (:status next-result)))
          (when-let [job (get-in next-result [:body :job])]
            (is (= "queued" (:status job)))
            
            ;; Mark job as processing
            (let [status-result (api-request :post 
                                  (str "/translations/jobs/" job-id "/status")
                                 {:status "processing"})]
              (is (= 200 (:status status-result))))))))))

(deftest translation-segment-crud-test
  (testing "Translation segments can be created and retrieved"
    ;; Create a test segment
    (let [segment-result (api-request :post "/translations/segments"
                          {:source_text "Hello world"
                           :translated_text "Hola mundo"
                           :source_lang "en"
                           :target_lang "es"
                           :document_id "test-doc-456"
                           :garden_id "test-garden"
                           :segment_index 0
                           :status "pending"})]
      (is (= 201 (:status segment-result)))
      
      ;; List segments
      (let [list-result (api-request :get "/translations/segments?target_lang=es")]
        (is (= 200 (:status list-result)))))))

(comment
  ;; Run tests with: clj -M:test
  )
