(ns kms-ingestion.jobs.ingest-support
  "Helpers for file-state updates and downstream ingestion targets."
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.db :as db]
   [kms-ingestion.graph :as graph])
  (:import
   [java.time Instant]
   [java.util UUID]))

(defn parse-jsonish
  [value]
  (cond
    (nil? value) nil
    (string? value) (json/parse-string value keyword)
    (map? value) value
    (vector? value) value
    (instance? org.postgresql.util.PGobject value)
    (let [s (.getValue ^org.postgresql.util.PGobject value)]
      (when-not (str/blank? s)
        (json/parse-string s keyword)))
    :else value))

(defn- stable-event-id
  [source-id file-id]
  (str (UUID/nameUUIDFromBytes (.getBytes (str source-id "|" file-id) "UTF-8"))))

(defn- derive-domain
  [path]
  (let [parts (->> (str/split (str path) #"/") (remove str/blank?) vec)]
    (cond
      (empty? parts) "general"
      (= 1 (count parts)) "general"
      :else (or (first parts) "general"))))

(defn- absolute-file-id
  [root-path rel-path]
  (.getAbsolutePath (java.io.File. (str root-path) (str rel-path))))

(defn apply-deleted-paths!
  [source source-id existing-hashes collections deleted-paths]
  (let [driver-config (or (parse-jsonish (:config source)) {})
        root-path (or (:root-path driver-config) (:root_path driver-config))]
    (doseq [rel-path deleted-paths]
      (db/upsert-file-state!
       {:file-id (absolute-file-id root-path rel-path)
        :source-id source-id
        :tenant-id (:tenant_id source)
        :path rel-path
        :content-hash (get existing-hashes (absolute-file-id root-path rel-path))
        :status "deleted"
        :chunks 0
        :collections collections
        :metadata {:deleted true}}))))

(defn- post-openplanner-events!
  [openplanner-url openplanner-api-key events]
  (let [headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? openplanner-api-key))
                  (assoc "Authorization" (str "Bearer " openplanner-api-key)))
        resp (http/post
              (str openplanner-url "/v1/events")
              {:headers headers
               :body (json/generate-string {:events events})
               :as :json
               :socket-timeout 60000
               :connection-timeout 60000
               :throw-exceptions false})]
    (if (= 200 (:status resp))
      {:status :success :count (count events)}
      {:status :failed
       :error (:body resp)
       :response resp})))

(defn ingest-via-ragussy!
  [ragussy-url collections file]
  (let [resp (http/post
              (str ragussy-url "/api/rag/ingest/text")
              {:headers {"Content-Type" "application/json"}
               :body (json/generate-string
                      {:text (:content file)
                       :source (:path file)
                       :collection (first collections)})
               :as :json
               :socket-timeout 60000
               :connection-timeout 60000})]
    (if (= 200 (:status resp))
      {:status :success
       :chunks (or (-> resp :body :chunks) 0)
       :target :ragussy}
      {:status :failed
       :error (:body resp)
       :target :ragussy})))

(defn ingest-via-openplanner!
  [_job-id tenant-id source-id openplanner-url openplanner-api-key file graph-context]
  (let [node-type (graph/node-type-for-path (:path file))
        project tenant-id
        doc-id (stable-event-id source-id (:id file))
        title (:path file)
        payload {:document {:id doc-id
                            :title title
                            :content (:content file)
                            :project project
                            :kind (name node-type)
                            :visibility "internal"
                            :source "kms-ingestion"
                            :sourcePath (:path file)
                            :domain (derive-domain (:path file))
                            :language "en"
                            :createdBy "kms-ingestion"
                            :metadata {:tenant_id tenant-id
                                       :lake tenant-id
                                       :node_type (name node-type)
                                       :path (:path file)
                                       :content_hash (:content-hash file)
                                       :source_id source-id
                                       :file_id (:id file)}}}
        headers (cond-> {"Content-Type" "application/json"}
                  (not (str/blank? openplanner-api-key))
                  (assoc "Authorization" (str "Bearer " openplanner-api-key)))
        resp (http/post
              (str openplanner-url "/v1/documents")
              {:headers headers
               :body (json/generate-string payload)
               :as :json
               :socket-timeout 60000
               :connection-timeout 60000
               :throw-exceptions false})
        graph-events (graph/collect-devel-graph-events
                      {:tenant-id tenant-id
                       :source-id source-id
                       :file file
                       :context graph-context
                       :ts (str (Instant/now))})]
    (if (= 200 (:status resp))
      (let [graph-result (post-openplanner-events! openplanner-url openplanner-api-key graph-events)]
        (if (= :success (:status graph-result))
          {:status :success
           :chunks 1
           :target :openplanner
           :lake tenant-id
           :doc-id doc-id
           :graph_events (count graph-events)}
          {:status :failed
           :target :openplanner
           :lake tenant-id
           :doc-id doc-id
           :error {:document (:body resp)
                   :graph (:error graph-result)}}))
      {:status :failed
       :error (:body resp)
       :target :openplanner
       :lake tenant-id})))
