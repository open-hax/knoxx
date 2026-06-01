(ns kms-ingestion.api.file-upload
  "Single-file upload endpoint: accept one file over multipart/form-data,
   save it under the configured upload directory with a UUID-based filename,
   register a local source pointing at that directory, and queue an ingestion
   job. Returns {file_id, job_id} for tracking."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.api.common :as common]
   [kms-ingestion.config :as config]
   [kms-ingestion.db :as db]
   [kms-ingestion.jobs.worker :as worker])
  (:import
   [java.io File]))

;; File types supported by the single-file upload endpoint.
(def allowed-extensions #{".md" ".txt" ".pdf" ".docx"})

(defn allowed-extension?
  "True when `filename` carries one of the supported upload extensions."
  [filename]
  (let [lower (str/lower-case (str filename))
        dot (str/last-index-of lower ".")]
    (boolean
     (and dot
          (contains? allowed-extensions (subs lower dot))))))

(defn- file-extension
  "Return the lower-cased extension (including the dot) of `filename`, or \"\"."
  [filename]
  (let [lower (str/lower-case (str filename))
        dot (str/last-index-of lower ".")]
    (if dot (subs lower dot) "")))

(defn- multipart-file
  "Pull the uploaded file descriptor from a ring multipart request.
   Standard ring multipart shape is
   {:filename .. :tempfile <File> :size <long> :content-type ..}."
  [request]
  (let [params (or (:multipart-params request) (:params request) {})]
    (or (get params "file")
        (get params :file))))

(defn- save-upload!
  "Copy the uploaded tempfile into `upload-dir` under a UUID-based filename,
   preserving the original extension. Returns {:file-id .. :saved-file <File>}."
  [^File tempfile filename ^String upload-dir]
  (let [file-id (str (java.util.UUID/randomUUID))
        ext (file-extension filename)
        dir (io/file upload-dir)
        saved-file (io/file dir (str file-id ext))]
    (.mkdirs dir)
    (io/copy tempfile saved-file)
    {:file-id file-id
     :saved-file saved-file}))

(defn file-upload-handler
  "POST /api/ingestion/upload — accept a single multipart file, persist it to
   the configured upload directory under a UUID-based name, register a local
   source pointing at that directory, and queue an ingestion job."
  [request]
  (let [tenant-id (common/get-tenant-id request)
        upload (multipart-file request)
        filename (some-> upload :filename str)
        tempfile (:tempfile upload)
        size (or (:size upload)
                 (when tempfile (.length ^File tempfile))
                 0)
        max-bytes (config/upload-max-bytes)]
    (cond
      (nil? upload)
      {:status 400 :body {:error "multipart field 'file' is required"}}

      (not (allowed-extension? filename))
      {:status 400
       :body {:error "unsupported file type; expected .md, .txt, .pdf, or .docx"
              :filename filename}}

      (> (long size) (long max-bytes))
      {:status 413
       :body {:error "file exceeds maximum upload size"
              :size size
              :max_bytes max-bytes}}

      :else
      (let [{:keys [file-id ^File saved-file]} (save-upload! tempfile filename (config/upload-dir))]
        (try
          (let [source (db/create-source!
                        {:tenant-id tenant-id
                         :driver-type "local"
                         :name (str "upload " (or filename file-id))
                         :config {:root_path (.getAbsolutePath (io/file (config/upload-dir)))
                                  :upload true
                                  :file_id file-id}
                         :collections [tenant-id]
                         :include-patterns [(.getName saved-file)]})
                source-id (common/uuid-str (:source_id source))
                job (db/create-job! source-id tenant-id {:full_scan true})
                job-id (common/uuid-str (:job_id job))]
            (db/mark-source-scanned! source-id)
            (worker/queue-job! job-id source)
            {:status 200
             :body {:file_id file-id
                    :job_id job-id
                    :source_id source-id
                    :tenant_id tenant-id
                    :filename (.getName saved-file)
                    :status "queued"}})
          (catch Exception e
            (.delete saved-file)
            {:status 500
             :body {:error (str "failed to queue upload: " (.getMessage e))
                    :filename filename}}))))))
