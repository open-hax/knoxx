(ns kms-ingestion.api.bulk-import-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.api.bulk-import :as bulk-import]
   [kms-ingestion.db :as db]
   [kms-ingestion.jobs.worker :as worker])
  (:import
   [java.io File]
   [org.apache.commons.compress.archivers.tar TarArchiveEntry TarArchiveOutputStream]
   [org.apache.commons.compress.compressors.gzip GzipCompressorOutputStream]))

(defn- write-tar-gz!
  "Create a .tar.gz at `dest` containing the given {entry-name -> content} map."
  ^File [^File dest entries]
  (with-open [gz (GzipCompressorOutputStream. (io/output-stream dest))
              tar (TarArchiveOutputStream. gz)]
    (doseq [[name content] entries]
      (let [bytes (.getBytes ^String content "UTF-8")
            entry (doto (TarArchiveEntry. ^String name)
                    (.setSize (alength bytes)))]
        (.putArchiveEntry tar entry)
        (.write tar bytes)
        (.closeArchiveEntry tar))))
  dest)

(deftest archive-format-detection
  (testing "format is resolved from the filename extension"
    (is (= :tar.gz (bulk-import/archive-format "bundle.tar.gz")))
    (is (= :tar.gz (bulk-import/archive-format "BUNDLE.TGZ")))
    (is (= :tar (bulk-import/archive-format "bundle.tar")))
    (is (= :zip (bulk-import/archive-format "bundle.zip")))
    (is (nil? (bulk-import/archive-format "notes.txt")))
    (is (nil? (bulk-import/archive-format nil)))))

(deftest extract-archive-writes-files
  (testing "a tar.gz is unpacked into the destination directory"
    (let [archive (File/createTempFile "bulk-test" ".tar.gz")
          dest (.toFile (fs/create-temp-dir {:prefix "bulk-extract-"}))]
      (try
        (write-tar-gz! archive {"a.md" "alpha"
                                "nested/b.md" "beta"})
        (let [count (bulk-import/extract-archive! archive :tar.gz dest)]
          (is (= 2 count))
          (is (= "alpha" (slurp (io/file dest "a.md"))))
          (is (= "beta" (slurp (io/file dest "nested" "b.md")))))
        (finally
          (.delete archive)
          (fs/delete-tree dest))))))

(deftest extract-archive-rejects-zip-slip
  (testing "entries that escape the destination are rejected"
    (let [archive (File/createTempFile "bulk-slip" ".tar.gz")
          dest (.toFile (fs/create-temp-dir {:prefix "bulk-slip-"}))]
      (try
        (write-tar-gz! archive {"../escape.md" "nope"})
        (is (thrown? clojure.lang.ExceptionInfo
                     (bulk-import/extract-archive! archive :tar.gz dest)))
        (finally
          (.delete archive)
          (fs/delete-tree dest))))))

(deftest bulk-import-handler-missing-file
  (testing "a request without a file field returns 400"
    (let [response (bulk-import/bulk-import-handler {:multipart-params {}})]
      (is (= 400 (:status response)))
      (is (re-find #"file" (get-in response [:body :error]))))))

(deftest bulk-import-handler-unsupported-format
  (testing "an unsupported extension returns 400"
    (let [response (bulk-import/bulk-import-handler
                    {:multipart-params {"file" {:filename "notes.txt"
                                                :tempfile (File/createTempFile "bulk-fmt" ".txt")
                                                :size 10}}})]
      (is (= 400 (:status response)))
      (is (re-find #"unsupported" (get-in response [:body :error]))))))

(deftest bulk-import-handler-rejects-oversize
  (testing "an upload above the size ceiling returns 413"
    (let [response (bulk-import/bulk-import-handler
                    {:multipart-params {"file" {:filename "big.tar.gz"
                                                :tempfile (File/createTempFile "bulk-big" ".tar.gz")
                                                :size (* 200 1024 1024)}}})]
      (is (= 413 (:status response)))
      (is (= (* 200 1024 1024) (get-in response [:body :size]))))))

(deftest bulk-import-handler-extracts-and-queues
  (testing "a valid archive creates a temp source + job and returns queued"
    (let [archive (File/createTempFile "bulk-ok" ".tar.gz")
          queued (atom nil)
          created-source (atom nil)
          created-job (atom nil)]
      (try
        (write-tar-gz! archive {"doc.md" "hello world"})
        (with-redefs [db/create-source! (fn [args]
                                          (reset! created-source args)
                                          {:source_id "11111111-1111-1111-1111-111111111111"
                                           :tenant_id (:tenant-id args)})
                      db/create-job! (fn [source-id tenant-id config]
                                       (reset! created-job {:source-id source-id
                                                            :tenant-id tenant-id
                                                            :config config})
                                       {:job_id "22222222-2222-2222-2222-222222222222"
                                        :source_id source-id})
                      db/mark-source-scanned! (constantly nil)
                      worker/queue-job! (fn [job-id source] (reset! queued {:job-id job-id :source source}))]
          (let [response (bulk-import/bulk-import-handler
                          {:multipart-params {"file" {:filename "bundle.tar.gz"
                                                      :tempfile archive
                                                      :size (.length archive)}}
                           :query-params {:tenant_id "devel"}})
                temp-path (get-in response [:body :temp_path])]
            (try
              (is (= 200 (:status response)))
              (is (= "queued" (get-in response [:body :status])))
              (is (= "22222222-2222-2222-2222-222222222222" (get-in response [:body :job_id])))
              (is (= "11111111-1111-1111-1111-111111111111" (get-in response [:body :source_id])))
              (is (= 1 (get-in response [:body :file_count])))
              (is (= "local" (:driver-type @created-source)))
              (is (true? (get-in @created-source [:config :bulk_import])))
              (is (= temp-path (get-in @created-source [:config :root_path])))
              (is (= "hello world" (slurp (io/file temp-path "doc.md"))))
              (is (some? @queued))
              (finally
                (when temp-path (fs/delete-tree temp-path))))))
        (finally
          (.delete archive))))))

(deftest cleanup-temp-source-removes-marked-dir
  (testing "cleanup-temp-source! reaps a bulk-import temp directory"
    (let [temp-dir (.toFile (fs/create-temp-dir {:prefix "bulk-cleanup-"}))]
      (spit (io/file temp-dir "x.md") "data")
      (worker/cleanup-temp-source!
       "job-1"
       {:config (str "{\"bulk_import\":true,\"temp_path\":\"" (.getAbsolutePath temp-dir) "\"}")})
      (is (not (.exists temp-dir))))))

(deftest cleanup-temp-source-ignores-unmarked
  (testing "cleanup-temp-source! leaves non-bulk-import sources untouched"
    (let [temp-dir (.toFile (fs/create-temp-dir {:prefix "bulk-keep-"}))]
      (try
        (worker/cleanup-temp-source!
         "job-2"
         {:config (str "{\"root_path\":\"" (.getAbsolutePath temp-dir) "\"}")})
        (is (.exists temp-dir))
        (finally
          (fs/delete-tree temp-dir))))))
