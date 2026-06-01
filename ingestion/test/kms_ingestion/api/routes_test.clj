(ns kms-ingestion.api.routes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.java.io :as io]
   [clj-http.client :as http]
   [kms-ingestion.api.file-upload :as file-upload]
   [kms-ingestion.api.routes :as routes]
   [kms-ingestion.config :as config]
   [kms-ingestion.db :as db]
   [kms-ingestion.jobs.worker :as worker]))

(deftest call-proxx-chat-uses-configured-timeouts
  (testing "Proxx chat requests honor the configured timeout budget"
    (let [captured (atom nil)]
      (with-redefs [config/proxx-url (constantly "http://proxx.test")
                    config/proxx-auth-token (constantly "secret-token")
                    config/proxx-default-model (constantly "glm-5")
                    config/proxx-connection-timeout-ms (constantly 1500)
                    config/proxx-socket-timeout-ms (constantly 3200)
                    http/post (fn [url opts]
                                (reset! captured {:url url :opts opts})
                                {:status 503
                                 :body {:error {:message "upstream timeout"}}})]
        (let [result (routes/call-proxx-chat {:messages [{:role "user" :content "hello"}]})]
          (is (= false (:ok result)))
          (is (= 503 (:status result)))
          (is (= "http://proxx.test/v1/chat/completions" (get @captured :url)))
          (is (= 1500 (get-in @captured [:opts :connection-timeout])))
          (is (= 3200 (get-in @captured [:opts :socket-timeout])))
          (is (= "Bearer secret-token" (get-in @captured [:opts :headers "Authorization"]))))))))

(deftest answer-handler-surfaces-proxx-failure
  (testing "The chat answer endpoint returns a clear upstream error when Proxx is unavailable"
    (with-redefs [routes/federated-fts (fn [_]
                                         {:projects ["devel-docs"]
                                          :count 1
                                          :rows [{:project "devel-docs"
                                                  :source "kms-ingestion"
                                                  :kind "docs"
                                                  :ts "2026-04-03T20:00:00Z"
                                                  :message "docs/reference/chat.md"
                                                  :snippet "Chat fallback context"}]})
                  routes/call-proxx-chat (fn [_]
                                           {:ok false
                                            :status 0
                                            :error "timed out waiting for Proxx"})]
      (let [response (routes/answer-handler {:params {}
                                             :query-params {}
                                             :body "{\"q\":\"why is chat slow?\",\"role\":\"workspace\"}"})]
        (is (= 504 (:status response)))
        (is (= "proxx_timeout" (get-in response [:body :error_code])))
        (is (= "timed out waiting for Proxx" (get-in response [:body :model_error])))
        (is (= ["devel-docs"] (get-in response [:body :projects])))
        (is (= 1 (get-in response [:body :count])))
        (is (re-find #"timed out" (get-in response [:body :error])))))))

(deftest devel-answer-system-prompt-pushes-synthesis-over-counting
  (let [prompt (routes/devel-answer-system-prompt {:projects ["devel-docs" "devel-code"]
                                                   :context-found? true})]
    (is (re-find #"Synthesize across snippets" prompt))
    (is (re-find #"Do not default to frequency-counting" prompt))
    (is (re-find #"## Answer, ## Why it matters, ## Evidence" prompt))
    (is (re-find #"devel-docs, devel-code" prompt))))

(deftest build-answer-user-prompt-enforces-structured-grounded-output
  (let [prompt (routes/build-answer-user-prompt
                {:q "What should we do next?"
                 :projects ["devel-docs" "devel-code"]
                 :rows [{:project "devel-docs"
                         :kind "docs"
                         :source_path "docs/plan.md"
                         :snippet "Prioritize grounded synthesis before architecture churn."}
                        {:project "devel-code"
                         :kind "code"
                         :source_path "src/app.ts"
                         :snippet "Prompt builder currently emits raw context."}]})]
    (is (re-find #"## Answer" prompt))
    (is (re-find #"## Why it matters" prompt))
    (is (re-find #"Do not pad with counts" prompt))
    (is (re-find #"docs/plan.md" prompt))
    (is (re-find #"src/app.ts" prompt))))

;; ============================================================
;; File upload endpoint
;; ============================================================

(defn- temp-upload-file
  "Write `content` to a temp file with the given extension and return it."
  ^java.io.File [ext content]
  (let [f (java.io.File/createTempFile "kms-upload-test" ext)]
    (.deleteOnExit f)
    (spit f content)
    f))

(defn- multipart-request
  "Build a ring multipart request whose 'file' field carries the descriptor."
  [descriptor]
  {:multipart-params {"file" descriptor}})

(defn- with-upload-stubs
  "Run `f` with db/worker stubbed so the upload handler runs without a live DB.
   The `calls` atom records the source/job creation arguments."
  [calls f]
  (with-redefs [db/create-source! (fn [args]
                                    (swap! calls assoc :source-args args)
                                    {:source_id (java.util.UUID/fromString
                                                 "11111111-1111-1111-1111-111111111111")})
                db/create-job! (fn [source-id tenant-id cfg]
                                 (swap! calls assoc :job-args {:source-id source-id
                                                               :tenant-id tenant-id
                                                               :config cfg})
                                 {:job_id (java.util.UUID/fromString
                                           "22222222-2222-2222-2222-222222222222")
                                  :status "pending"})
                db/mark-source-scanned! (fn [source-id]
                                          (swap! calls assoc :scanned source-id))
                worker/queue-job! (fn [job-id source]
                                    (swap! calls assoc :queued {:job-id job-id
                                                                :source source}))]
    (f)))

(deftest allowed-extension?-accepts-supported-types-and-rejects-others
  (testing "Supported document extensions are accepted (case-insensitive)"
    (is (true? (file-upload/allowed-extension? "notes.md")))
    (is (true? (file-upload/allowed-extension? "plan.TXT")))
    (is (true? (file-upload/allowed-extension? "report.pdf")))
    (is (true? (file-upload/allowed-extension? "brief.docx"))))
  (testing "Unsupported or extension-less filenames are rejected"
    (is (false? (file-upload/allowed-extension? "archive.zip")))
    (is (false? (file-upload/allowed-extension? "image.png")))
    (is (false? (file-upload/allowed-extension? "Makefile")))
    (is (false? (file-upload/allowed-extension? nil)))))

(deftest file-upload-handler-rejects-missing-file-field
  (testing "A request with no 'file' multipart field returns 400"
    (let [response (file-upload/file-upload-handler {:multipart-params {}})]
      (is (= 400 (:status response)))
      (is (re-find #"file" (get-in response [:body :error]))))))

(deftest file-upload-handler-rejects-unsupported-file-type
  (testing "An unsupported extension returns 400 without saving or queuing"
    (let [calls (atom {})
          tempfile (temp-upload-file ".zip" "junk")
          response (with-upload-stubs
                     calls
                     #(file-upload/file-upload-handler
                       (multipart-request {:filename "evil.zip"
                                           :tempfile tempfile
                                           :size 4})))]
      (is (= 400 (:status response)))
      (is (re-find #"unsupported file type" (get-in response [:body :error])))
      (is (nil? (:source-args @calls)))
      (is (nil? (:job-args @calls))))))

(deftest file-upload-handler-enforces-size-limit
  (testing "A file larger than the configured limit returns 413"
    (let [calls (atom {})
          tempfile (temp-upload-file ".txt" "x")]
      (with-redefs [config/upload-max-bytes (constantly 10)]
        (let [response (with-upload-stubs
                         calls
                         #(file-upload/file-upload-handler
                           (multipart-request {:filename "big.txt"
                                               :tempfile tempfile
                                               :size 1000})))]
          (is (= 413 (:status response)))
          (is (= 10 (get-in response [:body :max_bytes])))
          (is (nil? (:source-args @calls))))))))

(deftest file-upload-handler-saves-creates-source-and-queues-job
  (testing "A valid upload is saved, a local source is created, a job queued, and {file_id, job_id} returned"
    (let [calls (atom {})
          upload-dir (str (System/getProperty "java.io.tmpdir")
                          "/kms-upload-test-" (System/currentTimeMillis))
          tempfile (temp-upload-file ".md" "# Hello upload")]
      (with-redefs [config/upload-dir (constantly upload-dir)
                    config/upload-max-bytes (constantly (* 50 1024 1024))]
        (let [response (with-upload-stubs
                         calls
                         #(file-upload/file-upload-handler
                           (assoc (multipart-request {:filename "hello.md"
                                                      :tempfile tempfile
                                                      :size (.length tempfile)})
                                  :params {"tenant_id" "devel"})))]
          (testing "response format"
            (is (= 200 (:status response)))
            (is (string? (get-in response [:body :file_id])))
            (is (= "22222222-2222-2222-2222-222222222222" (get-in response [:body :job_id])))
            (is (= "queued" (get-in response [:body :status]))))
          (testing "file is saved under the upload dir with a UUID-based name"
            (let [file-id (get-in response [:body :file_id])
                  saved (io/file upload-dir (str file-id ".md"))]
              (is (.exists saved))
              (is (= "# Hello upload" (slurp saved)))
              (.delete saved)))
          (testing "a local source pointing at the upload dir is created"
            (is (= "local" (:driver-type (:source-args @calls))))
            (is (= (.getAbsolutePath (io/file upload-dir))
                   (get-in (:source-args @calls) [:config :root_path]))))
          (testing "an ingestion job is queued"
            (is (= "11111111-1111-1111-1111-111111111111"
                   (get-in @calls [:job-args :source-id])))
            (is (some? (:queued @calls)))))))))
