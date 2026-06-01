(ns kms-ingestion.api.bulk-import
  "Bulk import endpoint: accept a tar/tar.gz/zip archive over multipart,
   unpack it into a temp directory, register a throwaway local source, and
   queue an ingestion job. The temp directory is reaped once the job leaves
   the running/pending state (see `cleanup-temp-source!`)."
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.api.common :as common]
   [kms-ingestion.db :as db]
   [kms-ingestion.jobs.worker :as worker])
  (:import
   [java.io BufferedInputStream File InputStream]
   [org.apache.commons.compress.archivers ArchiveEntry ArchiveInputStream ArchiveStreamFactory]
   [org.apache.commons.compress.compressors CompressorStreamFactory]))

;; Default upload ceiling: 100MB. Overridable via KNOXX_BULK_IMPORT_MAX_BYTES.
(def ^:private default-max-bytes (* 100 1024 1024))

(defn max-upload-bytes
  "Configured maximum bulk-import upload size in bytes."
  []
  (if-let [raw (System/getenv "KNOXX_BULK_IMPORT_MAX_BYTES")]
    (try (Long/parseLong (str/trim raw))
         (catch NumberFormatException _ default-max-bytes))
    default-max-bytes))

(defn archive-format
  "Resolve the archive format from a filename. Returns one of
   :tar.gz, :tgz, :tar, :zip, or nil when unrecognised."
  [filename]
  (let [lower (str/lower-case (str filename))]
    (cond
      (or (str/ends-with? lower ".tar.gz")
          (str/ends-with? lower ".tgz")) :tar.gz
      (str/ends-with? lower ".tar")      :tar
      (str/ends-with? lower ".zip")      :zip
      :else                              nil)))

(defn- normalized-target
  "Resolve an archive entry name against the destination root, rejecting any
   path that would escape the root (zip-slip guard). Returns a File or nil."
  ^File [^File dest-dir ^String entry-name]
  (let [root (.toPath (.getCanonicalFile dest-dir))
        candidate (.toFile (.resolve (.toPath dest-dir) entry-name))
        canonical (.toPath (.getCanonicalFile candidate))]
    (when (.startsWith canonical root)
      (.toFile canonical))))

(defn- open-archive-stream
  "Wrap the raw input stream with a decompressor (for gzip) when the format
   requires it, returning an ArchiveInputStream positioned at the first entry."
  ^ArchiveInputStream [format ^InputStream in]
  (let [buffered (BufferedInputStream. in)
        archive-factory (ArchiveStreamFactory.)]
    (case format
      (:tar.gz :tgz)
      (let [decompressed (.createCompressorInputStream
                          (CompressorStreamFactory.)
                          CompressorStreamFactory/GZIP
                          buffered)]
        (.createArchiveInputStream archive-factory ArchiveStreamFactory/TAR
                                   (BufferedInputStream. decompressed)))

      :tar
      (.createArchiveInputStream archive-factory ArchiveStreamFactory/TAR buffered)

      :zip
      (.createArchiveInputStream archive-factory ArchiveStreamFactory/ZIP buffered))))

(defn extract-archive!
  "Extract `archive-file` (of the given `format`) into `dest-dir`.
   Returns the number of regular files written. Throws on a zip-slip attempt."
  [^File archive-file format ^File dest-dir]
  (.mkdirs dest-dir)
  (with-open [in (io/input-stream archive-file)
              ^ArchiveInputStream ais (open-archive-stream format in)]
    (loop [written 0]
      (if-let [^ArchiveEntry entry (.getNextEntry ais)]
        (let [target (normalized-target dest-dir (.getName entry))]
          (when (nil? target)
            (throw (ex-info "archive entry escapes destination directory"
                            {:entry (.getName entry)})))
          (cond
            (.isDirectory entry)
            (do (.mkdirs target)
                (recur written))

            :else
            (do
              (some-> (.getParentFile target) (.mkdirs))
              (io/copy ais target)
              (recur (inc written)))))
        written))))

(defn- multipart-file
  "Pull the uploaded file descriptor from a ring multipart request.
   Standard ring multipart shape is
   {:filename .. :tempfile <File> :size <long> :content-type ..}."
  [request]
  (let [params (or (:multipart-params request) (:params request) {})]
    (or (get params "file")
        (get params :file))))

(defn bulk-import-handler
  "POST /api/ingestion/bulk — accept a tar/tar.gz/zip upload, extract it to a
   temp directory, register a temporary local source, and queue a job."
  [request]
  (let [tenant-id (common/get-tenant-id request)
        upload (multipart-file request)
        filename (some-> upload :filename str)
        tempfile (:tempfile upload)
        size (or (:size upload)
                 (when tempfile (.length ^File tempfile))
                 0)
        format (archive-format filename)]
    (cond
      (nil? upload)
      {:status 400 :body {:error "multipart field 'file' is required"}}

      (nil? format)
      {:status 400
       :body {:error "unsupported archive format; expected .tar.gz, .tgz, .tar, or .zip"
              :filename filename}}

      (> (long size) (max-upload-bytes))
      {:status 413
       :body {:error "archive exceeds maximum upload size"
              :size size
              :max_bytes (max-upload-bytes)}}

      :else
      (let [temp-dir (.toFile (fs/create-temp-dir {:prefix "knoxx-bulk-import-"}))]
        (try
          (let [file-count (extract-archive! ^File tempfile format temp-dir)
                source (db/create-source!
                        {:tenant-id tenant-id
                         :driver-type "local"
                         :name (str "bulk-import " (or filename "archive"))
                         :config {:root_path (.getAbsolutePath temp-dir)
                                  :bulk_import true
                                  :temp_path (.getAbsolutePath temp-dir)}
                         :collections [tenant-id]})
                source-id (common/uuid-str (:source_id source))
                job (db/create-job! source-id tenant-id {:full_scan true})
                job-id (common/uuid-str (:job_id job))]
            (db/mark-source-scanned! source-id)
            (worker/queue-job! job-id source)
            {:status 200
             :body {:job_id job-id
                    :source_id source-id
                    :tenant_id tenant-id
                    :temp_path (.getAbsolutePath temp-dir)
                    :file_count file-count
                    :status "queued"}})
          (catch Exception e
            (fs/delete-tree temp-dir)
            {:status 400
             :body {:error (str "failed to extract archive: " (.getMessage e))
                    :filename filename}}))))))
