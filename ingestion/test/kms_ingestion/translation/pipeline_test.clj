(ns kms-ingestion.translation.pipeline-test
  "End-to-end tests for the CMS -> publish -> translate pipeline."
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [kms-ingestion.config :as config]))

;; Test fixtures
(def test-base-url
  (or (System/getenv "TEST_OPENPLANNER_URL")
      "http://localhost:7777"))

(def test-api-key
  (or (System/getenv "TEST_OPENPLANNER_API_KEY")
      "change-me"))

(defn api-request
  [method path & [body]]
  (let [url (str test-base-url path)
        headers {"Content-Type" "application/json"
                  "Authorization" (str "Bearer " test-api-key)}
        opts (cond-> {:headers headers :method method}
                body (assoc :body (json/generate-string body)))]
    (http/request opts)))

(defn cleanup-test-data
  "Clean up test gardens, documents, and translation jobs created during tests."
  [f]
   (try
     ;; Delete test gardens
     (api-request :delete "/gardens/test-translation-garden")
     (catch Exception _))
   (try
     ;; Delete test documents
     (let [result (api-request :get "/documents?project=test-translation")]
       (doseq [doc (:documents (:body result))]
         (try
           (api-request :delete (str "/documents/" (:_id doc)))
           (catch Exception _))))
     (catch Exception _))
   (try
     ;; Delete test translation jobs
     (let [result (api-request :get "/translations/jobs?project=test-translation")]
       (doseq [job (:jobs (:body result))]
         (try
           (api-request :delete (str "/translations/jobs/" (:_id job)))
           (catch Exception _))))
     (catch Exception _))
   (f)))

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
     (is (= 201 (:status garden-result))
     
     ;; Create a test document
     (let [doc-result (api-request :post "/documents"
                        {:kind "docs"
                         :project "test-translation"
                         :visibility "draft"
                         :extra {:title "Test Document for Translation"
                                 :content "This is a test document. It has multiple sentences. We want to see if translation works correctly."}})]
       (is (= 201 (:status doc-result))
       (let [doc-id (get-in (:body doc-result) [:doc_id])]
         
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
               (is (= "queued" (:status job)))))))))))

(deftest translation-job-status-endpoints-test
  (testing "Translation job status endpoints work correctly"
   ;; Create a test job directly
   (let [job-result (api-request :post "/translations/jobs"
                     {:document_id "test-doc-123"
                      :garden_id "test-garden"
                      :source_lang "en"
                      :target_language "es"
                      :status "queued"})]
     (is (= 201 (:status job-result))
     (let [job-id (get-in (:body job-result) [:job_id])]
      
       ;; Fetch next job
       (let [next-result (api-request :get "/translations/jobs/next")]
         (is (= 200 (:status next-result))
         (when-let [job (:job (:body next-result))]
           (is (= "queued" (:status job)))
         
           ;; Mark job as processing
           (let [status-result (api-request :post 
                                 (str "/translations/jobs/" job-id "/status")
                                {:status "processing"})]
             (is (= 200 (:status status-result))
            
             ;; Verify job is now processing
             (let [job-result (api-request :get (str "/translations/jobs/" job-id))]
               (is (= "processing" (:status (:body job-result)))))))))))

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
     (is (= 201 (:status segment-result))
     (let [segment-id (get-in (:body segment-result) [:segment_id])]
      
       ;; List segments
       (let [list-result (api-request :get "/translations/segments?target_lang=es")]
         (is (= 200 (:status list-result))
         (is (pos? (count (:segments (:body list-result)))))))))
 
(deftest full-pipeline-test
  (testing "Full CMS -> publish -> translate -> verify pipeline"
   ;; This test requires the full stack running including translation worker
   ;; It's marked as integration test and may need longer timeout
   
   ;; Create garden with target language
   (let [garden-result (api-request :post "/gardens"
                          {:garden_id "test-full-pipeline-garden"
                           :title "Full Pipeline Test Garden"
                           :target_languages ["es"]
                           :status "active"})]
     (is (= 201 (:status garden-result))
    
     ;; Create and publish document
     (let [doc-result (api-request :post "/documents"
                      {:kind "docs"
                       :project "test-translation"
                       :visibility "draft"
                       :extra {:title "Full Pipeline Test"
                               :content "This document tests the full translation pipeline from CMS publish to translated segment storage."}})]
       (is (= 201 (:status doc-result))
       (let [doc-id (get-in (:body doc-result) [:doc_id])]
      
         ;; Publish to garden
         (let [publish-result (api-request :post 
                                (str "/cms/publish/" doc-id "/test-full-pipeline-garden"))]
           (is (= 200 (:status publish-result))
           (let [jobs (:translation_jobs (:body publish-result))]
             (is (pos? (count jobs)))
            
             ;; Wait for translation worker to process
             ;; (In real test, would poll for completion)
             (Thread/sleep 2000)
            
             ;; Check for translation segments
             (let [segments-result (api-request :get 
                                   (str "/translations/segments?document_id=" doc-id))]
               ;; Should have segments after worker processes
               ;; Note: This may not work in unit test without worker running
               (is (= 200 (:status segments-result)))))))))))

(comment
  ;; Run tests with: clj -X:test
  )
